package com.example.smartwaterchick;

public class JadwalItem {
    public String id;
    public int jam;
    public int menit;
    public boolean aktif;

    public JadwalItem() {} // Required for Firebase

    public JadwalItem(String id, int jam, int menit, boolean aktif) {
        this.id = id;
        this.jam = jam;
        this.menit = menit;
        this.aktif = aktif;
    }
}
