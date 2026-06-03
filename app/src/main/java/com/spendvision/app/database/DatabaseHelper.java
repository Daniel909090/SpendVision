package com.spendvision.app.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "spendvision.db";
    private static final int DATABASE_VERSION = 2;

    private static final String TABLE_EXPENSES = "expenses";

    private static final String COL_ID = "id";
    private static final String COL_STORE = "store";
    private static final String COL_DATE = "date";
    private static final String COL_TOTAL = "total";
    private static final String COL_CATEGORY = "category";
    private static final String COL_RECEIPT_TEXT = "receipt_text";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable =
                "CREATE TABLE " + TABLE_EXPENSES + " (" +
                        COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COL_STORE + " TEXT, " +
                        COL_DATE + " TEXT, " +
                        COL_TOTAL + " TEXT, " +
                        COL_CATEGORY + " TEXT, " +
                        COL_RECEIPT_TEXT + " TEXT" +
                        ")";

        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_EXPENSES);
        onCreate(db);
    }

    public boolean addExpense(String store, String date, String total, String category, String receiptText) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COL_STORE, store);
        values.put(COL_DATE, date);
        values.put(COL_TOTAL, total);
        values.put(COL_CATEGORY, category);
        values.put(COL_RECEIPT_TEXT, receiptText);

        long result = db.insert(TABLE_EXPENSES, null, values);
        db.close();

        return result != -1;
    }

    public Cursor getAllExpenses() {
        SQLiteDatabase db = this.getReadableDatabase();

        return db.rawQuery(
                "SELECT * FROM " + TABLE_EXPENSES + " ORDER BY " + COL_ID + " DESC",
                null
        );
    }

    public double getTotalSpent() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT total FROM " + TABLE_EXPENSES, null);

        double totalSpent = 0;

        while (cursor.moveToNext()) {
            totalSpent += cleanAmount(cursor.getString(0));
        }

        cursor.close();
        db.close();

        return totalSpent;
    }

    public int getExpensesCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_EXPENSES, null);

        int count = 0;

        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }

        cursor.close();
        db.close();

        return count;
    }

    public double getTotalByCategory(String category) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT total FROM " + TABLE_EXPENSES + " WHERE category = ?",
                new String[]{category}
        );

        double categoryTotal = 0;

        while (cursor.moveToNext()) {
            categoryTotal += cleanAmount(cursor.getString(0));
        }

        cursor.close();
        db.close();

        return categoryTotal;
    }

    private double cleanAmount(String amountText) {
        try {
            if (amountText == null) {
                return 0;
            }

            amountText = amountText
                    .replace("£", "")
                    .replace(",", "")
                    .trim();

            return Double.parseDouble(amountText);

        } catch (Exception e) {
            return 0;
        }
    }
}