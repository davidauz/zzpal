package com.davidauz.zzpal.service;

import android.text.method.ScrollingMovementMethod;
import android.widget.TextView;

public class AppLogger {
    private static AppLogger instance;
    private TextView logTextView;
    private StringBuilder logBuffer;
    private static final int MAX_LOG_LINES = 100; // Prevent memory issues

    private AppLogger() {
        logBuffer = new StringBuilder();
    }

    public static synchronized AppLogger getInstance() {
        if (instance == null) {
            instance = new AppLogger();
        }
        return instance;
    }

    // Call this from your MainActivity when the logs layout is shown
    public void setLogTextView(TextView textView) {
        logTextView = textView;
        logTextView.setMovementMethod(new ScrollingMovementMethod());
        // Display existing logs when TextView becomes available
        if (logTextView != null && logBuffer.length() > 0) {
            logTextView.setText(logBuffer.toString());
        }
    }



    // Main logging method - can be called from any class
    public void log(String message) {
        String timestamp = android.text.format.DateFormat.format("HH:mm:ss", new java.util.Date()).toString();
        String logEntry = "[" + timestamp + "] " + message + "\n";

        // Add to buffer
        logBuffer.append(logEntry);

        // Limit buffer size to prevent memory issues
        limitBufferSize();

        // Update UI if TextView is available
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
    public void clearLogTextView() {
        logBuffer.setLength(0);
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
}