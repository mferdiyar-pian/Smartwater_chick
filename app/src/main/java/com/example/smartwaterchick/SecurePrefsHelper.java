package com.example.smartwaterchick;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Helper untuk mengakses SharedPreferences dengan enkripsi AES256.
 * Menggantikan Context.getSharedPreferences() di seluruh aplikasi
 * agar data preferensi tidak tersimpan dalam plaintext.
 */
public class SecurePrefsHelper {

    private SecurePrefsHelper() {}

    /**
     * Dapatkan SharedPreferences yang terenkripsi.
     * Fallback ke SharedPreferences biasa jika enkripsi tidak tersedia.
     *
     * @param context  context aplikasi
     * @param fileName nama file preferensi
     * @return SharedPreferences (terenkripsi bila memungkinkan)
     */
    public static SharedPreferences getPrefs(Context context, String fileName) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context.getApplicationContext())
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            return EncryptedSharedPreferences.create(
                    context.getApplicationContext(),
                    fileName,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            // Fallback: gunakan SharedPreferences biasa
            return context.getSharedPreferences(fileName, Context.MODE_PRIVATE);
        }
    }
}
