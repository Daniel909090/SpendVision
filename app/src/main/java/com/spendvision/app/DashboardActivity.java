package com.spendvision.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.spendvision.app.database.DatabaseHelper;

public class DashboardActivity extends AppCompatActivity {

    private TextView totalSpentText;
    private TextView receiptsCountText;
    private TextView categoryBreakdownText;

    private Button scanReceiptBtn;
    private Button viewExpensesBtn;
    private Button statisticsBtn;

    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        databaseHelper = new DatabaseHelper(this);

        totalSpentText = findViewById(R.id.totalSpentText);
        receiptsCountText = findViewById(R.id.receiptsCountText);
        categoryBreakdownText = findViewById(R.id.categoryBreakdownText);

        scanReceiptBtn = findViewById(R.id.scanReceiptBtn);
        viewExpensesBtn = findViewById(R.id.viewExpensesBtn);
        statisticsBtn = findViewById(R.id.statisticsBtn);

        scanReceiptBtn.setOnClickListener(v -> {
            Intent intent = new Intent(
                    DashboardActivity.this,
                    ScanReceiptActivity.class
            );
            startActivity(intent);
        });

        viewExpensesBtn.setOnClickListener(v -> {
            Intent intent = new Intent(
                    DashboardActivity.this,
                    ViewExpensesActivity.class
            );
            startActivity(intent);
        });

        statisticsBtn.setOnClickListener(v -> {
            Intent intent =
                    new Intent(
                            DashboardActivity.this,
                            StatisticsActivity.class
                    );

            startActivity(intent);
        });

        loadDashboardData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDashboardData();
    }

    private void loadDashboardData() {

        double totalSpent = databaseHelper.getTotalSpent();
        int receiptsCount = databaseHelper.getExpensesCount();

        double groceries =
                databaseHelper.getTotalByCategory("Groceries");

        double fuel =
                databaseHelper.getTotalByCategory("Fuel");

        double restaurant =
                databaseHelper.getTotalByCategory("Restaurant");

        double health =
                databaseHelper.getTotalByCategory("Health");

        double clothing =
                databaseHelper.getTotalByCategory("Clothing");

        double other =
                databaseHelper.getTotalByCategory("Other");

        totalSpentText.setText(
                String.format("Total spent: £%.2f", totalSpent)
        );

        receiptsCountText.setText(
                "Receipts saved: " + receiptsCount
        );

        categoryBreakdownText.setText(
                "Categories\n\n" +
                        "Groceries: £" + String.format("%.2f", groceries) +
                        "\nFuel: £" + String.format("%.2f", fuel) +
                        "\nRestaurant: £" + String.format("%.2f", restaurant) +
                        "\nHealth: £" + String.format("%.2f", health) +
                        "\nClothing: £" + String.format("%.2f", clothing) +
                        "\nOther: £" + String.format("%.2f", other)
        );
    }
}