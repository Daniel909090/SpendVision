package com.spendvision.app;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.spendvision.app.database.DatabaseHelper;

import java.util.ArrayList;

public class StatisticsActivity extends AppCompatActivity {

    private PieChart categoryPieChart;
    private TextView statisticsSummaryText;

    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        categoryPieChart = findViewById(R.id.categoryPieChart);
        statisticsSummaryText = findViewById(R.id.statisticsSummaryText);

        databaseHelper = new DatabaseHelper(this);

        loadStatistics();
    }

    private void loadStatistics() {

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

        double total =
                groceries +
                        fuel +
                        restaurant +
                        health +
                        clothing +
                        other;

        statisticsSummaryText.setText(
                "Total Spent: £" + String.format("%.2f", total)
        );

        ArrayList<PieEntry> entries = new ArrayList<>();

        if (groceries > 0)
            entries.add(new PieEntry((float) groceries, "Groceries"));

        if (fuel > 0)
            entries.add(new PieEntry((float) fuel, "Fuel"));

        if (restaurant > 0)
            entries.add(new PieEntry((float) restaurant, "Restaurant"));

        if (health > 0)
            entries.add(new PieEntry((float) health, "Health"));

        if (clothing > 0)
            entries.add(new PieEntry((float) clothing, "Clothing"));

        if (other > 0)
            entries.add(new PieEntry((float) other, "Other"));

        PieDataSet dataSet =
                new PieDataSet(entries, "Expenses");

        PieData data = new PieData(dataSet);

        categoryPieChart.setData(data);
        categoryPieChart.setCenterText("SpendVision");
        categoryPieChart.getDescription().setEnabled(false);
        categoryPieChart.invalidate();
    }
}