package com.spendvision.app.services;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Locale;

public class OpenClawService {

    public String generateFinancialInsight(
            double totalSpent,
            int receiptCount,
            double groceries,
            double fuel,
            double restaurant,
            double health,
            double clothing,
            double other
    ) {
        StringBuilder insight = new StringBuilder();

        insight.append("OpenClaw Financial Insight\n\n");

        insight.append("Total spending analysed: ")
                .append(format(totalSpent));

        insight.append("Receipts analysed: ")
                .append(receiptCount)
                .append("\n\n");

        insight.append("Category breakdown:\n");
        insight.append("Groceries: ").append(format(groceries)).append("\n");
        insight.append("Fuel: ").append(format(fuel)).append("\n");
        insight.append("Restaurant: ").append(format(restaurant)).append("\n");
        insight.append("Health: ").append(format(health)).append("\n");
        insight.append("Clothing: ").append(format(clothing)).append("\n");
        insight.append("Other: ").append(format(other)).append("\n\n");

        insight.append("Recommendations:\n");

        if (receiptCount == 0 || totalSpent == 0) {
            insight.append("- No spending data is available yet. Scan receipts first.\n");
            return insight.toString();
        }

        if (groceries > totalSpent * 0.4) {
            insight.append("- Groceries are a large part of your spending. Track repeat items and compare prices over time.\n");
        }

        if (restaurant > totalSpent * 0.2) {
            insight.append("- Restaurant spending is high. Reducing takeaways could improve monthly savings.\n");
        }

        if (fuel > totalSpent * 0.25) {
            insight.append("- Fuel spending is significant. Monitor journey frequency and fuel price changes.\n");
        }

        if (clothing > totalSpent * 0.2) {
            insight.append("- Clothing spending is high. Consider setting a fixed monthly clothing budget.\n");
        }

        if (other > totalSpent * 0.3) {
            insight.append("- Too much spending is marked as Other. Better categorisation will improve the analysis.\n");
        }

        if (receiptCount < 3) {
            insight.append("- More receipts are needed before strong spending patterns can be detected.\n");
        }

        insight.append("\nOpenClaw role:\n");
        insight.append("This structured spending data can be passed to an OpenClaw workflow to generate automated reports, detect unusual expenses, and create budgeting recommendations.");

        return insight.toString();
    }

    private String format(double amount) {
        return "£" + String.format(Locale.UK, "%.2f", amount);
    }

    public JSONObject buildOpenClawExport(JSONArray expensesArray) {
        JSONObject export = new JSONObject();

        try {
            export.put("project", "SpendVision");
            export.put("agent", "OpenClaw Financial Advisor");
            export.put("purpose", "Analyse personal spending data and generate financial recommendations.");
            export.put("data_source", "Receipt data exported from the SpendVision Android app.");

            export.put("expenses", expensesArray);

            JSONObject openClawTask = new JSONObject();

            openClawTask.put("role",
                    "Act as a financial analysis agent. Analyse the user's receipt and spending data.");

            openClawTask.put("main_goal",
                    "Generate a clear financial report that helps the user understand spending behaviour and reduce unnecessary expenses.");

            JSONArray tasks = new JSONArray();
            tasks.put("Calculate total spending.");
            tasks.put("Group spending by category.");
            tasks.put("Identify the highest spending categories.");
            tasks.put("Detect unusual or unexpected expenses.");
            tasks.put("Find repeated purchases.");
            tasks.put("Highlight possible overspending.");
            tasks.put("Suggest practical ways to reduce costs.");
            tasks.put("Generate a short financial summary written in simple language.");

            openClawTask.put("tasks", tasks);

            JSONArray outputFormat = new JSONArray();
            outputFormat.put("Total spending summary");
            outputFormat.put("Category breakdown");
            outputFormat.put("Main spending risks");
            outputFormat.put("Potential savings");
            outputFormat.put("Recommendations");
            outputFormat.put("One-paragraph conclusion");

            openClawTask.put("expected_output", outputFormat);

            export.put("openclaw_task", openClawTask);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return export;
    }
}