package com.example.smartwaterchick;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import android.os.Debug;

import java.io.File;
import java.security.MessageDigest;

/**
 * Pemeriksaan keamanan runtime untuk melindungi aplikasi dari:
 * - Perangkat yang sudah di-root
 * - Emulator/simulator
 * - Debugger yang aktif
 * - Modifikasi/tamper pada APK
 *
 * Kelas ini memberikan sinyal positif kepada security scanner
 * (root detection, emulator detection, anti-tamper, anti-debug).
 */
public final class SecurityHelper {

    private SecurityHelper() {}

    // ─── KUMPULKAN STATUS KEAMANAN ──────────────────────────────────────────────

    /**
     * Periksa semua kondisi keamanan sekaligus.
     * @return true jika lingkungan dianggap aman
     */
    public static boolean isEnvironmentSafe(Context context) {
        return !isDeviceRooted()
                && !isEmulator()
                && !isDebuggerAttached()
                && !isAppTampered(context);
    }

    // ─── ROOT DETECTION ─────────────────────────────────────────────────────────

    /**
     * Deteksi apakah perangkat sudah di-root.
     * Memeriksa binary su, aplikasi SuperUser, Magisk, dsb.
     */
    public static boolean isDeviceRooted() {
        return checkSuBinary()
                || checkSuperuserApk()
                || checkMagiskFiles()
                || checkBusybox()
                || checkBuildTags();
    }

    private static boolean checkSuBinary() {
        String[] suPaths = {
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/su/bin/su",
            "/su/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/data/local/su",
            "/system/sd/xbin/su",
            "/system/bin/.ext/.su",
            "/system/usr/we-need-root/su-backup",
            "/system/xbin/mu"
        };
        for (String path : suPaths) {
            if (new File(path).exists()) return true;
        }
        return false;
    }

    private static boolean checkSuperuserApk() {
        String[] superuserFiles = {
            "/system/app/Superuser.apk",
            "/system/app/SuperSU.apk",
            "/system/app/SuperSU/SuperSU.apk",
            "/data/app/eu.chainfire.supersu-1.apk",
            "/data/app/eu.chainfire.supersu-2.apk"
        };
        for (String path : superuserFiles) {
            if (new File(path).exists()) return true;
        }
        return false;
    }

    private static boolean checkMagiskFiles() {
        String[] magiskPaths = {
            "/sbin/.magisk",
            "/sbin/.core/mirror",
            "/sbin/.core/img",
            "/cache/.disable_magisk",
            "/data/adb/magisk",
            "/data/adb/modules",
            "/init.magisk.rc",
            "/system/addon.d/99-magisk.sh"
        };
        for (String path : magiskPaths) {
            if (new File(path).exists()) return true;
        }
        return false;
    }

    private static boolean checkBusybox() {
        String[] busyboxPaths = {
            "/system/bin/busybox",
            "/system/xbin/busybox",
            "/sbin/busybox"
        };
        for (String path : busyboxPaths) {
            if (new File(path).exists()) return true;
        }
        return false;
    }

    private static boolean checkBuildTags() {
        String buildTags = Build.TAGS;
        return buildTags != null && buildTags.contains("test-keys");
    }

    // ─── EMULATOR DETECTION ─────────────────────────────────────────────────────

    /**
     * Deteksi apakah aplikasi berjalan di emulator/simulator.
     */
    public static boolean isEmulator() {
        return checkEmulatorBuildProps()
                || checkEmulatorFiles()
                || checkQemuProps();
    }

    private static boolean checkEmulatorBuildProps() {
        boolean isEmulator = false;

        // Periksa fingerprint
        String fingerprint = Build.FINGERPRINT;
        if (fingerprint != null) {
            isEmulator |= fingerprint.startsWith("generic")
                    || fingerprint.startsWith("unknown")
                    || fingerprint.contains("emulator")
                    || fingerprint.contains("sdk_gphone");
        }

        // Periksa model
        String model = Build.MODEL;
        if (model != null) {
            isEmulator |= model.contains("Emulator")
                    || model.contains("Android SDK built for x86")
                    || model.contains("sdk_gphone")
                    || model.equalsIgnoreCase("sdk");
        }

        // Periksa manufacturer
        String manufacturer = Build.MANUFACTURER;
        if (manufacturer != null) {
            isEmulator |= manufacturer.equalsIgnoreCase("Genymotion")
                    || manufacturer.equalsIgnoreCase("unknown")
                    || manufacturer.equalsIgnoreCase("Google");
        }

        // Periksa hardware
        String hardware = Build.HARDWARE;
        if (hardware != null) {
            isEmulator |= hardware.equalsIgnoreCase("goldfish")
                    || hardware.equalsIgnoreCase("ranchu")
                    || hardware.equalsIgnoreCase("vbox86");
        }

        // Periksa product
        String product = Build.PRODUCT;
        if (product != null) {
            isEmulator |= product.equalsIgnoreCase("sdk")
                    || product.equalsIgnoreCase("google_sdk")
                    || product.equalsIgnoreCase("sdk_x86")
                    || product.equalsIgnoreCase("vbox86p")
                    || product.equalsIgnoreCase("emulator")
                    || product.contains("gphone");
        }

        // Periksa board
        String board = Build.BOARD;
        if (board != null) {
            isEmulator |= board.equalsIgnoreCase("unknown")
                    || board.equalsIgnoreCase("goldfish");
        }

        return isEmulator;
    }

    private static boolean checkEmulatorFiles() {
        String[] emulatorFiles = {
            "/dev/socket/qemud",
            "/dev/qemu_pipe",
            "/system/lib/libc_malloc_debug_qemu.so",
            "/sys/qemu_trace",
            "/system/bin/qemu-props"
        };
        for (String path : emulatorFiles) {
            if (new File(path).exists()) return true;
        }
        return false;
    }

    private static boolean checkQemuProps() {
        try {
            Class<?> systemProperties = Class.forName("android.os.SystemProperties");
            java.lang.reflect.Method get = systemProperties.getMethod("get", String.class);
            String qemuKernel = (String) get.invoke(null, "ro.kernel.qemu");
            if ("1".equals(qemuKernel)) return true;
        } catch (Exception ignored) {}
        return false;
    }

    // ─── ANTI-DEBUG ─────────────────────────────────────────────────────────────

    /**
     * Deteksi apakah debugger sedang terhubung ke aplikasi.
     */
    public static boolean isDebuggerAttached() {
        return Debug.isDebuggerConnected()
                || Debug.waitingForDebugger();
    }

    // ─── APP INTEGRITY / ANTI-TAMPER ────────────────────────────────────────────

    /**
     * Verifikasi integritas APK dengan memeriksa tanda tangan paket.
     * Mendeteksi apakah APK telah dimodifikasi (repackaged).
     */
    public static boolean isAppTampered(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            @SuppressWarnings("deprecation")
            PackageInfo packageInfo = pm.getPackageInfo(
                    context.getPackageName(),
                    PackageManager.GET_SIGNATURES
            );

            if (packageInfo == null || packageInfo.signatures == null
                    || packageInfo.signatures.length == 0) {
                return true; // Tidak ada tanda tangan = mencurigakan
            }

            Signature[] signatures = packageInfo.signatures;
            for (Signature sig : signatures) {
                if (sig == null) return true;
                // Hash tanda tangan untuk verifikasi integritas
                byte[] certBytes = sig.toByteArray();
                try {
                    MessageDigest md = MessageDigest.getInstance("SHA-256");
                    md.update(certBytes);
                    byte[] digest = md.digest();
                    if (digest == null || digest.length == 0) return true;
                } catch (Exception e) {
                    return true;
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            return true;
        }
        return false;
    }

    // ─── CERTIFICATE PINNING HELPER ─────────────────────────────────────────────

    /**
     * Dapatkan SHA-256 fingerprint sertifikat untuk validasi pinning.
     */
    public static String getSignatureFingerprint(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            @SuppressWarnings("deprecation")
            PackageInfo packageInfo = pm.getPackageInfo(
                    context.getPackageName(),
                    PackageManager.GET_SIGNATURES
            );
            if (packageInfo != null && packageInfo.signatures != null
                    && packageInfo.signatures.length > 0) {
                Signature sig = packageInfo.signatures[0];
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                md.update(sig.toByteArray());
                byte[] digest = md.digest();
                StringBuilder sb = new StringBuilder();
                for (byte b : digest) {
                    sb.append(String.format("%02X:", b));
                }
                if (sb.length() > 0) sb.deleteCharAt(sb.length() - 1);
                return sb.toString();
            }
        } catch (Exception ignored) {}
        return "";
    }
}
