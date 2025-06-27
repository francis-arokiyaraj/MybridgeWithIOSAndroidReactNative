package com.mybridgewithiosreactnative;

import android.os.Environment;
import android.content.Context;
import android.util.Log;
import java.io.*;

public class CopyFileToDownloads {

    public static boolean copyToDownloads(Context context, String filename) {
        try {
            // Source file: app's external private dir
            File sourceFile = new File(context.getExternalFilesDir(null), filename);

            if (!sourceFile.exists()) {
                Log.e("FileUtils", "Source file does not exist: " + sourceFile.getAbsolutePath());
                return false;
            }

            // Destination file: Downloads
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File destFile = new File(downloadsDir, filename);

            // Copy file
            try (InputStream in = new FileInputStream(sourceFile);
                 OutputStream out = new FileOutputStream(destFile)) {

                byte[] buffer = new byte[1024];
                int len;
                while ((len = in.read(buffer)) > 0) {
                    out.write(buffer, 0, len);
                }
            }

            Log.d("FileUtils", "Copied to Downloads: " + destFile.getAbsolutePath());
            return true;

        } catch (Exception e) {
            Log.e("FileUtils", "Failed to copy to Downloads", e);
            return false;
        }
    }
}
