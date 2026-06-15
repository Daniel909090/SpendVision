package com.spendvision.app.services;

import android.content.Context;
import android.net.Uri;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

public class OCRService {

    private final Context context;

    public OCRService(Context context) {
        this.context = context;
    }

    public void extractTextFromUri(Uri uri, OCRCallback callback) {
        try {
            InputImage image = InputImage.fromFilePath(context, uri);

            TextRecognizer recognizer =
                    TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

            recognizer.process(image)
                    .addOnSuccessListener(result -> callback.onSuccess(result.getText()))
                    .addOnFailureListener(e -> callback.onFailure(e.getMessage()));

        } catch (Exception e) {
            callback.onFailure(e.getMessage());
        }
    }

    public interface OCRCallback {
        void onSuccess(String extractedText);

        void onFailure(String errorMessage);
    }
}