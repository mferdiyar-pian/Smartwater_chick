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
    private static final int DATABASE_VERSION = 4;

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

    // ======================
    // TABLE VOLUME AIR (UNTUK DIAGRAM BATANG)
    // ======================
    private static final String TABLE_VOLUME = "volume_air";
    private static final String COLUMN_VOLUME_ID = "id";
    private static final String COLUMN_VOLUME_TANGGAL = "tanggal";
    private static final String COLUMN_VOLUME_LITER = "liter";
    private static final String COLUMN_VOLUME_TIPE = "tipe"; // daily, weekly, monthly

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
        // 20 DATA DUMMY MONITORING
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

        // ======================
        // TABEL VOLUME AIR
        // ======================
        String createVolume = "CREATE TABLE " + TABLE_VOLUME + " (" +
                COLUMN_VOLUME_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_VOLUME_TANGGAL + " TEXT, " +
                COLUMN_VOLUME_LITER + " REAL, " +
                COLUMN_VOLUME_TIPE + " TEXT)";
        db.execSQL(createVolume);

        // ======================
        // 20 DATA DUMMY VOLUME (UNTUK DIAGRAM BATANG)
        // ======================
        insertVolumeDummy(db, "2026-03-01", 85f, "daily");
        insertVolumeDummy(db, "2026-03-02", 92f, "daily");
        insertVolumeDummy(db, "2026-03-03", 78f, "daily");
        insertVolumeDummy(db, "2026-03-04", 105f, "daily");
        insertVolumeDummy(db, "2026-03-05", 120f, "daily");
        insertVolumeDummy(db, "2026-03-06", 95f, "daily");
        insertVolumeDummy(db, "2026-03-07", 140f, "daily");
        insertVolumeDummy(db, "2026-03-08", 88f, "daily");
        insertVolumeDummy(db, "2026-03-09", 112f, "daily");
        insertVolumeDummy(db, "2026-03-10", 135f, "daily");
        insertVolumeDummy(db, "2026-03-11", 76f, "daily");
        insertVolumeDummy(db, "2026-03-12", 98f, "daily");
        insertVolumeDummy(db, "2026-03-13", 125f, "daily");
        insertVolumeDummy(db, "2026-03-14", 82f, "daily");
        insertVolumeDummy(db, "2026-03-15", 110f, "daily");
        insertVolumeDummy(db, "2026-03-16", 145f, "daily");
        insertVolumeDummy(db, "2026-03-17", 90f, "daily");
        insertVolumeDummy(db, "2026-03-18", 118f, "daily");
        insertVolumeDummy(db, "2026-03-19", 132f, "daily");
        insertVolumeDummy(db, "2026-03-20", 160f, "daily");

        // Data mingguan (4 minggu)
        insertVolumeDummy(db, "Minggu 1", 580f, "weekly");
        insertVolumeDummy(db, "Minggu 2", 620f, "weekly");
        insertVolumeDummy(db, "Minggu 3", 590f, "weekly");
        insertVolumeDummy(db, "Minggu 4", 650f, "weekly");

        // Data bulanan (20 hari)
        insertVolumeDummy(db, "1", 85f, "monthly");
        insertVolumeDummy(db, "2", 92f, "monthly");
        insertVolumeDummy(db, "3", 78f, "monthly");
        insertVolumeDummy(db, "4", 105f, "monthly");
        insertVolumeDummy(db, "5", 120f, "monthly");
        insertVolumeDummy(db, "6", 95f, "monthly");
        insertVolumeDummy(db, "7", 140f, "monthly");
        insertVolumeDummy(db, "8", 88f, "monthly");
        insertVolumeDummy(db, "9", 112f, "monthly");
        insertVolumeDummy(db, "10", 135f, "monthly");
        insertVolumeDummy(db, "11", 76f, "monthly");
        insertVolumeDummy(db, "12", 98f, "monthly");
        insertVolumeDummy(db, "13", 125f, "monthly");
        insertVolumeDummy(db, "14", 82f, "monthly");
        insertVolumeDummy(db, "15", 110f, "monthly");
        insertVolumeDummy(db, "16", 145f, "monthly");
        insertVolumeDummy(db, "17", 90f, "monthly");
        insertVolumeDummy(db, "18", 118f, "monthly");
        insertVolumeDummy(db, "19", 132f, "monthly");
        insertVolumeDummy(db, "20", 160f, "monthly");
    }

    // ======================
    // INSERT DATA MONITORING
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
    // INSERT DATA VOLUME
    // ======================
    private void insertVolumeDummy(SQLiteDatabase db, String tanggal, float liter, String tipe) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_VOLUME_TANGGAL, tanggal);
        values.put(COLUMN_VOLUME_LITER, liter);
        values.put(COLUMN_VOLUME_TIPE, tipe);
        db.insert(TABLE_VOLUME, null, values);
    }

    // ======================
    // UPGRADE DATABASE
    // ======================
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USER);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MONITORING);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_VOLUME);
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

    // ======================
    // AMBIL DATA VOLUME AIR (UNTUK DIAGRAM BATANG)
    // ======================
    public ArrayList<Float> getVolumeData(String tipe, int limit) {
        ArrayList<Float> data = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT " + COLUMN_VOLUME_LITER + " FROM " + TABLE_VOLUME +
                        " WHERE " + COLUMN_VOLUME_TIPE + " = ? ORDER BY " + COLUMN_VOLUME_ID + " LIMIT ?",
                new String[]{tipe, String.valueOf(limit)}
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
    // AMBIL LABEL VOLUME AIR
    // ======================
    public ArrayList<String> getVolumeLabels(String tipe, int limit) {
        ArrayList<String> labels = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT " + COLUMN_VOLUME_TANGGAL + " FROM " + TABLE_VOLUME +
                        " WHERE " + COLUMN_VOLUME_TIPE + " = ? ORDER BY " + COLUMN_VOLUME_ID + " LIMIT ?",
                new String[]{tipe, String.valueOf(limit)}
        );

        if (cursor.moveToFirst()) {
            do {
                labels.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }

        cursor.close();
        return labels;
    }
}