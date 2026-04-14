package com.example.smartwaterchick;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.Collections;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "smartwater.db";
    private static final int DATABASE_VERSION = 3;

    // ======================
    // TABLE USER (LOGIN)
    // ======================
    private static final String TABLE_USER = "users";
    private static final String COLUMN_USER_ID = "id";
    private static final String COLUMN_EMAIL = "email";
    private static final String COLUMN_PASSWORD = "password";

    // ======================
    // TABLE MONITORING
    // ======================
    private static final String TABLE_MONITORING = "monitoring";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_TANGGAL = "tanggal";
    private static final String COLUMN_SUHU = "suhu";
    private static final String COLUMN_KELEMBABAN = "kelembaban";
    private static final String COLUMN_PH = "ph";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        // ======================
        // TABLE USER
        // ======================
        String createUser = "CREATE TABLE " + TABLE_USER + " (" +
                COLUMN_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_EMAIL + " TEXT UNIQUE, " +
                COLUMN_PASSWORD + " TEXT)";
        db.execSQL(createUser);

        // USER DEFAULT
        ContentValues user = new ContentValues();
        user.put(COLUMN_EMAIL, "admin@gmail.com");
        user.put(COLUMN_PASSWORD, "1234");
        db.insert(TABLE_USER, null, user);

        // ======================
        // TABLE MONITORING
        // ======================
        String createMonitoring = "CREATE TABLE " + TABLE_MONITORING + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_TANGGAL + " TEXT, " +
                COLUMN_SUHU + " REAL, " +
                COLUMN_KELEMBABAN + " REAL, " +
                COLUMN_PH + " REAL)";
        db.execSQL(createMonitoring);

        // ======================
        // 20 DATA DUMMY
        // ======================
        insertDummy(db, "2026-03-01", 27, 80, 7);
        insertDummy(db, "2026-03-02", 28, 82, 7);
        insertDummy(db, "2026-03-03", 29, 85, 6.8);
        insertDummy(db, "2026-03-04", 30, 83, 6.9);
        insertDummy(db, "2026-03-05", 28, 81, 7);
        insertDummy(db, "2026-03-06", 27, 79, 7.1);
        insertDummy(db, "2026-03-07", 29, 84, 6.7);
        insertDummy(db, "2026-03-08", 31, 88, 6.5);
        insertDummy(db, "2026-03-09", 30, 86, 6.6);
        insertDummy(db, "2026-03-10", 32, 90, 6.4);

        insertDummy(db, "2026-03-11", 33, 92, 6.3);
        insertDummy(db, "2026-03-12", 31, 89, 6.5);
        insertDummy(db, "2026-03-13", 30, 87, 6.7);
        insertDummy(db, "2026-03-14", 29, 85, 6.8);
        insertDummy(db, "2026-03-15", 28, 83, 7);
        insertDummy(db, "2026-03-16", 27, 80, 7.2);
        insertDummy(db, "2026-03-17", 29, 84, 6.9);
        insertDummy(db, "2026-03-18", 30, 86, 6.7);
        insertDummy(db, "2026-03-19", 31, 88, 6.6);
        insertDummy(db, "2026-03-20", 32, 91, 6.4);
    }

    // ======================
    // INSERT DATA
    // ======================
    private void insertDummy(SQLiteDatabase db, String tanggal, double suhu, double kelembaban, double ph) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_TANGGAL, tanggal);
        values.put(COLUMN_SUHU, suhu);
        values.put(COLUMN_KELEMBABAN, kelembaban);
        values.put(COLUMN_PH, ph);
        db.insert(TABLE_MONITORING, null, values);
    }

    // ======================
    // UPGRADE DATABASE
    // ======================
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USER);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MONITORING);
        onCreate(db);
    }

    // ======================
    // LOGIN
    // ======================
    public boolean checkLogin(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + TABLE_USER + " WHERE email=? AND password=?",
                new String[]{email, password}
        );

        boolean result = cursor.moveToFirst();
        cursor.close();
        return result;
    }

    // ======================
    // AMBIL SEMUA DATA
    // ======================
    public ArrayList<Float> getData(String column) {
        ArrayList<Float> data = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT " + column + " FROM " + TABLE_MONITORING + " ORDER BY tanggal ASC",
                null
        );

        if (cursor.moveToFirst()) {
            do {
                data.add(cursor.getFloat(0));
            } while (cursor.moveToNext());
        }

        cursor.close();
        return data;
    }

    // ======================
    // FILTER DATA (1 HARI / 7 HARI / 1 BULAN)
    // ======================
    public ArrayList<Float> getLimitedData(String column, int limit) {
        ArrayList<Float> data = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT " + column + " FROM " + TABLE_MONITORING +
                        " ORDER BY tanggal DESC LIMIT " + limit,
                null
        );

        if (cursor.moveToFirst()) {
            do {
                data.add(cursor.getFloat(0));
            } while (cursor.moveToNext());
        }

        cursor.close();

        Collections.reverse(data); // supaya urut dari lama → baru
        return data;
    }

    // ======================
    // KHUSUS PH (OPTIONAL BIAR RAPI)
    // ======================
    public ArrayList<Float> getPhData(int limit) {
        return getLimitedData("ph", limit);
    }
}