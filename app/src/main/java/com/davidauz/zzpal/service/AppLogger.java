package com.davidauz.zzpal.service;

import android.content.Context;
import android.os.Environment;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;

public class AppLogger {
    private static final String LOG_DIR = "zzPalLogs"
    ,   LOG_FILE = "zzz_log.txt"
    ,   TAG="zzzPal.tag"
    ;
    private static AppLogger instance;
    private TextView logTextView;
    private StringBuilder logBuffer;
    private static final int MAX_LOG_LINES = 100; // to prevent memory issues
    private Context context;

    private AppLogger(Context con) {
        logBuffer = new StringBuilder();
        context=con;
    }

//to be called in mainActivity.onStart because needs context
    public static synchronized void init(Context con) {
        instance = new AppLogger(con);
        instance.logBuffer=instance.readLogs();
        instance.log("********** AppLogger init ********** ");
    }

    public static synchronized AppLogger getInstance() {
        if(null==instance)
            instance = new AppLogger(null);
        return instance;
    }

    public void setLogTextView(TextView textView) {
        logTextView = textView;
        logTextView.setMovementMethod(new ScrollingMovementMethod());
        if (logTextView != null && logBuffer.length() > 0) {
            logTextView.setText(logBuffer.toString());
        }
    }



    public void log(String message) {
        String timestamp = android.text.format.DateFormat.format("HH:mm:ss", new java.util.Date()).toString();
        String logEntry = "[" + timestamp + "] " + message + "\n";
        writeToFileSync(logEntry);
        logBuffer.append(logEntry);
        limitBufferSize();
        if (logTextView != null) {
// Must run on UI thread
            logTextView.post(() -> {
                if (logTextView != null) {
                    logTextView.setText(logBuffer.toString());
// Auto-scroll to bottom
                    logTextView.post(() -> {
                        int scrollAmount = logTextView.getLayout().getLineTop(logTextView.getLineCount()) - logTextView.getHeight();
                        if (scrollAmount > 0) {
                            logTextView.scrollTo(0, scrollAmount);
                        } else {
                            logTextView.scrollTo(0, 0);
                        }
                    });
                }
            });
        }
    }

    // Overloaded method for logging exceptions
    public void log(Exception e) {
        log("Exception: " + e.getMessage() + "\nStack trace: " + android.util.Log.getStackTraceString(e));
    }

// Remove reference when logs layout is hidden (prevent memory leaks)
    public void clearLogTextView(boolean bDeleteFileToo) {
        logBuffer.setLength(0);

        if(bDeleteFileToo)
            try {
                File logFile = getLogFile();
                if (logFile.exists()) {
                    logFile.delete();
                }
            } catch (IOException e) {
                Log.e(TAG, "Error clearing logs", e);
            }

        if (logTextView != null) {
            logTextView.post(() -> {
                if (logTextView != null) {
                    logTextView.setText("");
                }
            });
        }
    }

    private void limitBufferSize() {
        String[] lines = logBuffer.toString().split("\n");
        if (lines.length > MAX_LOG_LINES) {
            StringBuilder newBuffer = new StringBuilder();
            for (int i = lines.length - MAX_LOG_LINES; i < lines.length; i++) {
                newBuffer.append(lines[i]).append("\n");
            }
            logBuffer = newBuffer;
        }
    }

    private File getLogFile() throws IOException {
        File logDir;
        if(null==context)
            return null;

        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            logDir = new File(context.getExternalFilesDir(null), LOG_DIR);
        } else {
            logDir = new File(context.getFilesDir(), LOG_DIR);
        }

        if (!logDir.exists()) {
            logDir.mkdirs();
        }

        return new File(logDir, LOG_FILE);
    }

    private synchronized void writeToFileSync(String logMessage) {
        FileOutputStream fos = null;
        try {
            File logFile = getLogFile();
            if(null==logFile)
                return;
            fos = new FileOutputStream(logFile, true);
            fos.write(logMessage.getBytes());
        } catch (IOException e) {
            Log.e(TAG, "Error writing to log file", e);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing log file", e);
                }
            }
        }
    }



    public StringBuilder readLogs() {
        StringBuilder sb = new StringBuilder();
        try {
            File logFile = getLogFile();
            if (null==logFile||!logFile.exists())
                return sb;

            BufferedReader reader = new BufferedReader(new FileReader(logFile));
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
            return sb;
        } catch (IOException e) {
            Log.e(TAG, "Error reading log file", e);
            sb.append("Error reading logs: " + e.getMessage());
            return sb;
        }
    }

}
