package com.spendvision.app.ui;

import android.database.Cursor;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.spendvision.app.R;
import com.spendvision.app.database.DatabaseHelper;

public class ViewExpensesActivity extends AppCompatActivity {

    private LinearLayout expensesContainer;
    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_expenses);

        expensesContainer = findViewById(R.id.expensesContainer);
        databaseHelper = new DatabaseHelper(this);

        loadExpenses();
    }

    private void loadExpenses() {
        Cursor cursor = databaseHelper.getAllExpenses();

        if (cursor.getCount() == 0) {
            addExpenseText("No expenses saved yet.");
            cursor.close();
            return;
        }

        while (cursor.moveToNext()) {
            String store = cursor.getString(cursor.getColumnIndexOrThrow("store"));
            String total = cursor.getString(cursor.getColumnIndexOrThrow("total"));
            String category = cursor.getString(cursor.getColumnIndexOrThrow("category"));

            String expenseText =
                    "Store: " + store +
                            "\nTotal: " + total +
                            "\nCategory: " + category;

            addExpenseText(expenseText);
        }

        cursor.close();
    }

    private void addExpenseText(String text) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextSize(18);
        textView.setTextColor(0xFF333333);
        textView.setPadding(0, 24, 0, 24);

        expensesContainer.addView(textView);
    }
}