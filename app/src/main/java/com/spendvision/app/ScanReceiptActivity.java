package com.spendvision.app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.spendvision.app.database.DatabaseHelper;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScanReceiptActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_CODE = 100;

    private ImageView receiptPreview;
    private TextView extractedTextView;

    private Uri cameraImageUri;

    private String lastExtractedText = "";
    private String lastDetectedTotal = "";
    private String lastDetectedCategory = "";
    private String lastDetectedStore = "";
    private String lastDetectedDate = "";

    private final ActivityResultLauncher<Uri> cameraLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.TakePicture(),
                    success -> {
                        if (success && cameraImageUri != null) {
                            receiptPreview.setImageURI(cameraImageUri);
                            extractTextFromUri(cameraImageUri);
                        } else {
                            Toast.makeText(this, "No photo captured", Toast.LENGTH_SHORT).show();
                        }
                    }
            );

    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.GetContent(),
                    uri -> {
                        if (uri != null) {
                            receiptPreview.setImageURI(uri);
                            extractTextFromUri(uri);
                        } else {
                            Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show();
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_receipt);

        Button openCameraBtn = findViewById(R.id.openCameraBtn);
        Button choosePhotoBtn = findViewById(R.id.choosePhotoBtn);
        Button saveExpenseBtn = findViewById(R.id.saveExpenseBtn);

        receiptPreview = findViewById(R.id.receiptPreview);
        extractedTextView = findViewById(R.id.extractedTextView);

        openCameraBtn.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                openFullCamera();
            } else {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.CAMERA},
                        CAMERA_PERMISSION_CODE
                );
            }
        });

        choosePhotoBtn.setOnClickListener(v -> galleryLauncher.launch("image/*"));

        saveExpenseBtn.setOnClickListener(v -> {

            if (lastExtractedText.isEmpty()) {
                Toast.makeText(
                        this,
                        "Scan or choose a receipt first",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            DatabaseHelper db = new DatabaseHelper(this);

            boolean saved = db.addExpense(
                    lastDetectedStore,
                    lastDetectedDate,
                    lastDetectedTotal,
                    lastDetectedCategory,
                    lastExtractedText
            );

            if (saved) {
                Toast.makeText(
                        this,
                        "Expense saved successfully",
                        Toast.LENGTH_LONG
                ).show();
            } else {
                Toast.makeText(
                        this,
                        "Failed to save expense",
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    private void openFullCamera() {
        try {
            File imageFile = createImageFile();

            cameraImageUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    imageFile
            );

            cameraLauncher.launch(cameraImageUri);

        } catch (Exception e) {
            Toast.makeText(this, "Camera error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private File createImageFile() throws Exception {
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        if (storageDir == null) {
            throw new Exception("Storage folder not available");
        }

        return File.createTempFile(
                "receipt_",
                ".jpg",
                storageDir
        );
    }

    private void extractTextFromUri(Uri uri) {
        extractedTextView.setText("Reading receipt...");

        try {
            InputImage image = InputImage.fromFilePath(this, uri);
            runTextRecognition(image);
        } catch (Exception e) {
            extractedTextView.setText("Could not read image: " + e.getMessage());
        }
    }

    private void runTextRecognition(InputImage image) {
        TextRecognizer recognizer =
                TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        recognizer.process(image)
                .addOnSuccessListener(result -> {
                    String extractedText = result.getText();

                    if (extractedText.trim().isEmpty()) {
                        lastExtractedText = "";
                        lastDetectedTotal = "";
                        lastDetectedCategory = "";
                        lastDetectedStore = "";
                        lastDetectedDate = "";
                        extractedTextView.setText("No text found. Try a clearer photo.");
                    } else {
                        String total = findTotal(extractedText);
                        String category = detectCategory(extractedText);
                        String store = detectStore(extractedText);
                        String date = detectDate(extractedText);

                        lastExtractedText = extractedText;
                        lastDetectedTotal = total;
                        lastDetectedCategory = category;
                        lastDetectedStore = store;
                        lastDetectedDate = date;

                        extractedTextView.setText(
                                "Store: " + store +
                                        "\nDate: " + date +
                                        "\nDetected Total: " + total +
                                        "\nCategory: " + category +
                                        "\n\nFull Receipt:\n\n" +
                                        extractedText
                        );
                    }
                })
                .addOnFailureListener(e -> {
                    lastExtractedText = "";
                    lastDetectedTotal = "";
                    lastDetectedCategory = "";
                    lastDetectedStore = "";
                    lastDetectedDate = "";
                    extractedTextView.setText("OCR failed: " + e.getMessage());
                });
    }

    private String findTotal(String text) {
        String[] lines = text.split("\\n");

        for (int i = 0; i < lines.length; i++) {
            String upperLine = lines[i].toUpperCase();

            if (upperLine.contains("TOTAL")
                    || upperLine.contains("AMOUNT")
                    || upperLine.contains("BALANCE")) {

                String amountInSameLine = findMoneyInLine(lines[i]);
                if (!amountInSameLine.equals("")) {
                    return amountInSameLine;
                }

                if (i + 1 < lines.length) {
                    String amountInNextLine = findMoneyInLine(lines[i + 1]);
                    if (!amountInNextLine.equals("")) {
                        return amountInNextLine;
                    }
                }
            }
        }

        String lastAmount = "";

        for (String line : lines) {
            String amount = findMoneyInLine(line);
            if (!amount.equals("")) {
                lastAmount = amount;
            }
        }

        if (!lastAmount.equals("")) {
            return lastAmount;
        }

        return "Total not found";
    }

    private String findMoneyInLine(String line) {
        Pattern pattern = Pattern.compile("£?\\s?\\d+\\.\\d{2}");
        Matcher matcher = pattern.matcher(line);

        if (matcher.find()) {
            return matcher.group().replace(" ", "");
        }

        return "";
    }

    private String detectCategory(String text) {
        String upperText = text.toUpperCase();

        if (upperText.contains("TESCO")
                || upperText.contains("ASDA")
                || upperText.contains("LIDL")
                || upperText.contains("ALDI")
                || upperText.contains("MORRISONS")
                || upperText.contains("SAINSBURY")
                || upperText.contains("ICELAND")
                || upperText.contains("CO-OP")
                || upperText.contains("COOP")) {
            return "Groceries";
        }

        if (upperText.contains("SHELL")
                || upperText.contains("BP")
                || upperText.contains("ESSO")
                || upperText.contains("FUEL")
                || upperText.contains("PETROL")
                || upperText.contains("DIESEL")) {
            return "Fuel";
        }

        if (upperText.contains("MCDONALD")
                || upperText.contains("KFC")
                || upperText.contains("BURGER KING")
                || upperText.contains("SUBWAY")
                || upperText.contains("RESTAURANT")
                || upperText.contains("PIZZA")) {
            return "Restaurant";
        }

        if (upperText.contains("BOOTS")
                || upperText.contains("PHARMACY")
                || upperText.contains("SUPERDRUG")) {
            return "Health";
        }

        if (upperText.contains("PRIMARK")
                || upperText.contains("NEXT")
                || upperText.contains("H&M")
                || upperText.contains("ZARA")) {
            return "Clothing";
        }

        return "Other";
    }

    private String detectStore(String text) {
        String upperText = text.toUpperCase();

        if (upperText.contains("TESCO")) return "Tesco";
        if (upperText.contains("LIDL")) return "Lidl";
        if (upperText.contains("ALDI")) return "Aldi";
        if (upperText.contains("ASDA")) return "Asda";
        if (upperText.contains("MORRISONS")) return "Morrisons";
        if (upperText.contains("SAINSBURY")) return "Sainsbury's";
        if (upperText.contains("ICELAND")) return "Iceland";
        if (upperText.contains("CO-OP") || upperText.contains("COOP")) return "Co-op";
        if (upperText.contains("SHELL")) return "Shell";
        if (upperText.contains("BP")) return "BP";
        if (upperText.contains("ESSO")) return "Esso";
        if (upperText.contains("BOOTS")) return "Boots";
        if (upperText.contains("SUPERDRUG")) return "Superdrug";
        if (upperText.contains("MCDONALD")) return "McDonald's";
        if (upperText.contains("KFC")) return "KFC";
        if (upperText.contains("BURGER KING")) return "Burger King";
        if (upperText.contains("SUBWAY")) return "Subway";

        return "Unknown Store";
    }

    private String detectDate(String text) {
        Pattern pattern =
                Pattern.compile("\\b\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}\\b");

        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            return matcher.group();
        }

        return "Unknown Date";
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openFullCamera();
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}