package com.spendvision.app.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.spendvision.app.R;
import com.spendvision.app.database.DatabaseHelper;
import com.spendvision.app.services.OpenClawService;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.io.File;
//import java.io.FileWriter;
import java.io.BufferedReader;

import java.io.FileReader;
public class OpenClawInsightActivity extends AppCompatActivity {

    private TextView openClawInsightText;
    private Button exportOpenClawJsonBtn;
    private DatabaseHelper databaseHelper;
    private OpenClawService openClawService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_openclaw_insight);

        exportOpenClawJsonBtn = findViewById(R.id.exportOpenClawJsonBtn);

        exportOpenClawJsonBtn.setOnClickListener(v -> exportDataForOpenClaw());
        openClawInsightText = findViewById(R.id.openClawInsightText);

        databaseHelper = new DatabaseHelper(this);
        openClawService = new OpenClawService();

        loadOpenClawReport();
    }
    private void loadOpenClawReport() {
        try {
            File file = new File(getExternalFilesDir(null), "financial_report.md");

            if (!file.exists()) {
                openClawInsightText.setText(
                        "No OpenClaw report found yet.\n\n" +
                                "Export the spending data first, run OpenClaw, then save the generated report as:\n\n" +
                                file.getAbsolutePath()
                );
                return;
            }

            StringBuilder report = new StringBuilder();
            BufferedReader reader = new BufferedReader(new FileReader(file));

            String line;
            while ((line = reader.readLine()) != null) {
                report.append(line).append("\n");
            }

            reader.close();

            openClawInsightText.setText(report.toString());

        } catch (Exception e) {
            openClawInsightText.setText("Failed to load OpenClaw report: " + e.getMessage());
        }
    }
    private void loadOpenClawInsight() {
        double totalSpent = databaseHelper.getTotalSpent();
        int receiptCount = databaseHelper.getExpensesCount();

        double groceries = databaseHelper.getTotalByCategory("Groceries");
        double fuel = databaseHelper.getTotalByCategory("Fuel");
        double restaurant = databaseHelper.getTotalByCategory("Restaurant");
        double health = databaseHelper.getTotalByCategory("Health");
        double clothing = databaseHelper.getTotalByCategory("Clothing");
        double other = databaseHelper.getTotalByCategory("Other");

        String insight = openClawService.generateFinancialInsight(
                totalSpent,
                receiptCount,
                groceries,
                fuel,
                restaurant,
                health,
                clothing,
                other
        );

        openClawInsightText.setText(insight);
    }

    private void exportDataForOpenClaw() {
        try {
            JSONArray expensesArray = databaseHelper.getExpensesAsJsonArray();

            JSONObject openClawExport =
                    openClawService.buildOpenClawExport(expensesArray);

            String fileName = "spendvision_openclaw_export.json";

            File file = new File(getExternalFilesDir(null), fileName);

            FileWriter writer = new FileWriter(file);

            openClawInsightText.setText(
                    "OpenClaw JSON export created successfully:\n\n" +
                            file.getAbsolutePath() +
                            "\n\nNext step: this file will be read by the OpenClaw Financial Advisor workflow to generate AI spending analysis."
            );

            exportOpenClawJsonBtn.setVisibility(View.GONE);

        } catch (Exception e) {
            openClawInsightText.setText("Export failed: " + e.getMessage());
        }
    }
}