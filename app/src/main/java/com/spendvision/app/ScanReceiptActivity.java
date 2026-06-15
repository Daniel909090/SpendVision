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
                        String items = buildItemsList(extractedText);

                        lastExtractedText = extractedText;
                        lastDetectedTotal = total;
                        lastDetectedCategory = category;
                        lastDetectedStore = store;
                        lastDetectedDate = date;

                        extractedTextView.setText(
                                "Receipt Summary\n\n" +
                                        "Store: " + store +
                                        "\nDate: " + date +
                                        "\nCategory: " + category +
                                        "\n\nTOTAL\n" +
                                        total +
                                        "\n\nProducts\n\n" +
                                        items +
                                        "\n\nRaw OCR Text\n\n" +
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
            String upperLine = lines[i].trim().toUpperCase();

            boolean isTotalLine =
                    upperLine.equals("TOTAL")
                            || upperLine.equals("TOTAL:")
                            || upperLine.contains("TOTAL TO PAY")
                            || upperLine.contains("AMOUNT DUE")
                            || upperLine.contains("BALANCE DUE")
                            || upperLine.contains("GRAND TOTAL");

            boolean ignoreLine =
                    upperLine.contains("SAVING")
                            || upperLine.contains("DISCOUNT")
                            || upperLine.contains("VAT")
                            || upperLine.contains("SUBTOTAL")
                            || upperLine.contains("ITEM")
                            || upperLine.contains("QTY")
                            || upperLine.contains("CHANGE");

            if (isTotalLine && !ignoreLine) {

                for (int j = i; j <= i + 5 && j < lines.length; j++) {
                    String amount = findMoneyInLine(lines[j]);

                    if (!amount.equals("")) {
                        return amount;
                    }
                }
            }
        }

        for (int i = 0; i < lines.length; i++) {
            String upperLine = lines[i].trim().toUpperCase();

            if (upperLine.contains("VISA")
                    || upperLine.contains("MASTERCARD")
                    || upperLine.contains("CARD")
                    || upperLine.contains("CONTACTLESS")) {

                for (int j = i; j <= i + 4 && j < lines.length; j++) {
                    String amount = findMoneyInLine(lines[j]);

                    if (!amount.equals("")) {
                        return amount;
                    }
                }
            }
        }

        return "Total not found";
    }

    private String findLargestAmount(String text) {
        String[] lines = text.split("\\n");

        double largest = 0;
        String largestText = "";

        for (String line : lines) {
            String upperLine = line.toUpperCase();

            if (upperLine.contains("SAVING")
                    || upperLine.contains("DISCOUNT")
                    || upperLine.contains("CHANGE")
                    || upperLine.contains("VAT")
                    || upperLine.contains("SUBTOTAL")) {
                continue;
            }

            Pattern pattern = Pattern.compile("£?\\s?\\d+\\.\\d{2}");
            Matcher matcher = pattern.matcher(line);

            while (matcher.find()) {
                String amountText = matcher.group()
                        .replace("£", "")
                        .replace(" ", "");

                try {
                    double amount = Double.parseDouble(amountText);

                    if (amount > largest) {
                        largest = amount;
                        largestText = "£" + String.format("%.2f", amount);
                    }

                } catch (Exception ignored) {
                }
            }
        }

        if (!largestText.equals("")) {
            return largestText;
        }

        return "Total not found";
    }


    private String findMoneyInLine(String line) {
        Pattern pattern = Pattern.compile("£?\\s?\\d{1,4}[.,]\\d{2}");
        Matcher matcher = pattern.matcher(line);

        String lastAmount = "";

        while (matcher.find()) {
            String amount = matcher.group()
                    .replace(" ", "")
                    .replace(",", ".");

            if (!amount.startsWith("£")) {
                amount = "£" + amount;
            }

            lastAmount = amount;
        }

        return lastAmount;
    }

    private String buildItemsList(String text) {
        StringBuilder items = new StringBuilder();
        String[] lines = text.split("\\n");

        Pattern pricePattern =
                Pattern.compile("£?\\s?\\d+\\.\\d{2}");

        for (String line : lines) {
            String cleanLine = line.trim();
            Matcher matcher = pricePattern.matcher(cleanLine);

            if (matcher.find()) {
                String rawPrice = matcher.group();
                String price = rawPrice.replace(" ", "");

                if (!price.startsWith("£")) {
                    price = "£" + price;
                }

                String product =
                        cleanLine.replace(rawPrice, "")
                                .replace("£", "")
                                .replace("GBP", "")
                                .replace("gbp", "")
                                .trim();

                String upperProduct = product.toUpperCase();

                if (!product.isEmpty()
                        && product.length() > 2
                        && !upperProduct.contains("TOTAL")
                        && !upperProduct.contains("BALANCE")
                        && !upperProduct.contains("AMOUNT")
                        && !upperProduct.contains("CARD")
                        && !upperProduct.contains("CHANGE")
                        && !upperProduct.contains("VAT")
                        && !upperProduct.contains("SAVING")
                        && !upperProduct.contains("DISCOUNT")
                        && !upperProduct.contains("VISA")
                        && !upperProduct.contains("MASTERCARD")
                        && !upperProduct.contains("CONTACTLESS")) {

                    items.append("• ")
                            .append(product)
                            .append("    ")
                            .append(price)
                            .append("\n");
                }
            }
        }

        if (items.length() == 0) {
            return "No item-price pairs found. Try a clearer, straighter photo.";
        }

        return items.toString();
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
                || upperText.contains("COOP")
                || upperText.contains("WAITROSE")
                || upperText.contains("MARKS AND SPENCER")
                || upperText.contains("M&S")
                || upperText.contains("FOOD WAREHOUSE")) {
            return "Groceries";
        }

        if (upperText.contains("SHELL")
                || upperText.contains("BP")
                || upperText.contains("ESSO")
                || upperText.contains("TEXACO")
                || upperText.contains("JET")
                || upperText.contains("GULF")
                || upperText.contains("FUEL")
                || upperText.contains("PETROL")
                || upperText.contains("DIESEL")) {
            return "Fuel";
        }

        if (upperText.contains("MCDONALD")
                || upperText.contains("KFC")
                || upperText.contains("BURGER KING")
                || upperText.contains("SUBWAY")
                || upperText.contains("NANDOS")
                || upperText.contains("DOMINO")
                || upperText.contains("PIZZA HUT")
                || upperText.contains("GREGGS")
                || upperText.contains("COSTA")
                || upperText.contains("STARBUCKS")
                || upperText.contains("RESTAURANT")
                || upperText.contains("PIZZA")) {
            return "Restaurant";
        }

        if (upperText.contains("BOOTS")
                || upperText.contains("PHARMACY")
                || upperText.contains("SUPERDRUG")
                || upperText.contains("HOLLAND & BARRETT")
                || upperText.contains("HOLLAND AND BARRETT")) {
            return "Health";
        }

        if (upperText.contains("TK MAXX")
                || upperText.contains("TKMAXX")
                || upperText.contains("PRIMARK")
                || upperText.contains("NEXT")
                || upperText.contains("H&M")
                || upperText.contains("ZARA")
                || upperText.contains("NEW LOOK")
                || upperText.contains("RIVER ISLAND")
                || upperText.contains("SPORTS DIRECT")
                || upperText.contains("JD SPORTS")
                || upperText.contains("SCHUH")
                || upperText.contains("CLARKS")) {
            return "Clothing";
        }

        if (upperText.contains("SMYTHS")
                || upperText.contains("SMYTHS TOYS")
                || upperText.contains("THE ENTERTAINER")
                || upperText.contains("TOY")
                || upperText.contains("GAME")) {
            return "Toys";
        }

        if (upperText.contains("B&Q")
                || upperText.contains("B AND Q")
                || upperText.contains("WICKES")
                || upperText.contains("SCREWFIX")
                || upperText.contains("TOOLSTATION")
                || upperText.contains("HOMEBASE")
                || upperText.contains("GARDEN CENTRE")
                || upperText.contains("DOBBIES")) {
            return "Home & DIY";
        }

        if (upperText.contains("ARGOS")
                || upperText.contains("CURRYS")
                || upperText.contains("CURRY'S")
                || upperText.contains("AMAZON")
                || upperText.contains("APPLE")
                || upperText.contains("SAMSUNG")
                || upperText.contains("CE X")
                || upperText.contains("CEX")) {
            return "Electronics";
        }

        if (upperText.contains("POUNDLAND")
                || upperText.contains("B&M")
                || upperText.contains("B AND M")
                || upperText.contains("HOME BARGAINS")
                || upperText.contains("THE RANGE")
                || upperText.contains("DUNELM")
                || upperText.contains("WILKO")) {
            return "Household";
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
        if (upperText.contains("WAITROSE")) return "Waitrose";
        if (upperText.contains("MARKS AND SPENCER") || upperText.contains("M&S")) return "M&S";
        if (upperText.contains("CO-OP") || upperText.contains("COOP")) return "Co-op";
        if (upperText.contains("FOOD WAREHOUSE")) return "Food Warehouse";

        if (upperText.contains("SHELL")) return "Shell";
        if (upperText.contains("BP")) return "BP";
        if (upperText.contains("ESSO")) return "Esso";
        if (upperText.contains("TEXACO")) return "Texaco";
        if (upperText.contains("JET")) return "Jet";
        if (upperText.contains("GULF")) return "Gulf";

        if (upperText.contains("MCDONALD")) return "McDonald's";
        if (upperText.contains("KFC")) return "KFC";
        if (upperText.contains("BURGER KING")) return "Burger King";
        if (upperText.contains("SUBWAY")) return "Subway";
        if (upperText.contains("NANDOS")) return "Nando's";
        if (upperText.contains("DOMINO")) return "Domino's";
        if (upperText.contains("PIZZA HUT")) return "Pizza Hut";
        if (upperText.contains("GREGGS")) return "Greggs";
        if (upperText.contains("COSTA")) return "Costa";
        if (upperText.contains("STARBUCKS")) return "Starbucks";

        if (upperText.contains("BOOTS")) return "Boots";
        if (upperText.contains("SUPERDRUG")) return "Superdrug";
        if (upperText.contains("HOLLAND & BARRETT") || upperText.contains("HOLLAND AND BARRETT")) return "Holland & Barrett";

        if (upperText.contains("TK MAXX") || upperText.contains("TKMAXX")) return "TK Maxx";
        if (upperText.contains("PRIMARK")) return "Primark";
        if (upperText.contains("NEXT")) return "Next";
        if (upperText.contains("H&M")) return "H&M";
        if (upperText.contains("ZARA")) return "Zara";
        if (upperText.contains("NEW LOOK")) return "New Look";
        if (upperText.contains("RIVER ISLAND")) return "River Island";
        if (upperText.contains("SPORTS DIRECT")) return "Sports Direct";
        if (upperText.contains("JD SPORTS")) return "JD Sports";
        if (upperText.contains("SCHUH")) return "Schuh";
        if (upperText.contains("CLARKS")) return "Clarks";

        if (upperText.contains("SMYTHS")) return "Smyths";
        if (upperText.contains("THE ENTERTAINER")) return "The Entertainer";
        if (upperText.contains("GAME")) return "GAME";

        if (upperText.contains("B&Q") || upperText.contains("B AND Q")) return "B&Q";
        if (upperText.contains("WICKES")) return "Wickes";
        if (upperText.contains("SCREWFIX")) return "Screwfix";
        if (upperText.contains("TOOLSTATION")) return "Toolstation";
        if (upperText.contains("HOMEBASE")) return "Homebase";
        if (upperText.contains("DOBBIES")) return "Dobbies";

        if (upperText.contains("ARGOS")) return "Argos";
        if (upperText.contains("CURRYS") || upperText.contains("CURRY'S")) return "Currys";
        if (upperText.contains("AMAZON")) return "Amazon";
        if (upperText.contains("APPLE")) return "Apple";
        if (upperText.contains("SAMSUNG")) return "Samsung";
        if (upperText.contains("CE X") || upperText.contains("CEX")) return "CeX";

        if (upperText.contains("POUNDLAND")) return "Poundland";
        if (upperText.contains("B&M") || upperText.contains("B AND M")) return "B&M";
        if (upperText.contains("HOME BARGAINS")) return "Home Bargains";
        if (upperText.contains("THE RANGE")) return "The Range";
        if (upperText.contains("DUNELM")) return "Dunelm";
        if (upperText.contains("WILKO")) return "Wilko";

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