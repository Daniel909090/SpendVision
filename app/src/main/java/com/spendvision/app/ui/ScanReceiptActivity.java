package com.spendvision.app.ui;

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

import com.spendvision.app.R;
import com.spendvision.app.database.DatabaseHelper;
import com.spendvision.app.services.OCRService;
import com.spendvision.app.services.ReceiptParserService;

import java.io.File;

public class ScanReceiptActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_CODE = 100;

    private ImageView receiptPreview;
    private TextView extractedTextView;

    private Uri cameraImageUri;

    private OCRService ocrService;
    private ReceiptParserService receiptParserService;

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
                            readReceipt(cameraImageUri);
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
                            readReceipt(uri);
                        } else {
                            Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show();
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_receipt);

        ocrService = new OCRService(this);
        receiptParserService = new ReceiptParserService();

        Button openCameraBtn = findViewById(R.id.openCameraBtn);
        Button choosePhotoBtn = findViewById(R.id.choosePhotoBtn);
        Button saveExpenseBtn = findViewById(R.id.saveExpenseBtn);

        receiptPreview = findViewById(R.id.receiptPreview);
        extractedTextView = findViewById(R.id.extractedTextView);

        openCameraBtn.setOnClickListener(v -> checkCameraPermission());

        choosePhotoBtn.setOnClickListener(v -> galleryLauncher.launch("image/*"));

        saveExpenseBtn.setOnClickListener(v -> saveExpense());
    }

    private void checkCameraPermission() {
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

        return File.createTempFile("receipt_", ".jpg", storageDir);
    }

    private void readReceipt(Uri uri) {
        extractedTextView.setText("Reading receipt...");

        ocrService.extractTextFromUri(uri, new OCRService.OCRCallback() {
            @Override
            public void onSuccess(String extractedText) {
                if (extractedText.trim().isEmpty()) {
                    clearLastReceiptData();
                    extractedTextView.setText("No text found. Try a clearer photo.");
                    return;
                }

                ReceiptParserService.ParsedReceipt parsedReceipt =
                        receiptParserService.parseReceipt(extractedText);

                lastExtractedText = extractedText;
                lastDetectedTotal = parsedReceipt.getTotal();
                lastDetectedCategory = parsedReceipt.getCategory();
                lastDetectedStore = parsedReceipt.getStore();
                lastDetectedDate = parsedReceipt.getDate();

                extractedTextView.setText(
                        "Receipt Summary\n\n" +
                                "Store: " + parsedReceipt.getStore() +
                                "\nDate: " + parsedReceipt.getDate() +
                                "\nCategory: " + parsedReceipt.getCategory() +
                                "\n\nTOTAL\n" +
                                parsedReceipt.getTotal() +
                                "\n\nProducts\n\n" +
                                parsedReceipt.getItems() +
                                "\n\nRaw OCR Text\n\n" +
                                extractedText
                );
            }

            @Override
            public void onFailure(String errorMessage) {
                clearLastReceiptData();
                extractedTextView.setText("OCR failed: " + errorMessage);
            }
        });
    }

    private void saveExpense() {
        if (lastExtractedText.isEmpty()) {
            Toast.makeText(this, "Scan or choose a receipt first", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(this, "Expense saved successfully", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Failed to save expense", Toast.LENGTH_LONG).show();
        }
    }

    private void clearLastReceiptData() {
        lastExtractedText = "";
        lastDetectedTotal = "";
        lastDetectedCategory = "";
        lastDetectedStore = "";
        lastDetectedDate = "";
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