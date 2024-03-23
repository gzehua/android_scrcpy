//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.termux.shared.logger;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.suda.androidscrcpy.R.string;
import com.termux.shared.data.DataUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Logger {
    private static String DEFAULT_LOG_TAG = "Logger";
    public static final int LOG_LEVEL_OFF = 0;
    public static final int LOG_LEVEL_NORMAL = 1;
    public static final int LOG_LEVEL_DEBUG = 2;
    public static final int LOG_LEVEL_VERBOSE = 3;
    public static final int DEFAULT_LOG_LEVEL = 1;
    public static final int MAX_LOG_LEVEL = 3;
    private static int CURRENT_LOG_LEVEL = DEFAULT_LOG_LEVEL;
    public static final int LOGGER_ENTRY_MAX_PAYLOAD = 4068;
    public static final int LOGGER_ENTRY_MAX_SAFE_PAYLOAD = 4000;

    public Logger() {
    }

    public static void logMessage(int logPriority, String tag, String message) {
        if (logPriority == 6 && CURRENT_LOG_LEVEL >= 1) {
            Log.e(getFullTag(tag), message);
        } else if (logPriority == 5 && CURRENT_LOG_LEVEL >= 1) {
            Log.w(getFullTag(tag), message);
        } else if (logPriority == 4 && CURRENT_LOG_LEVEL >= 1) {
            Log.i(getFullTag(tag), message);
        } else if (logPriority == 3 && CURRENT_LOG_LEVEL >= 2) {
            Log.d(getFullTag(tag), message);
        } else if (logPriority == 2 && CURRENT_LOG_LEVEL >= 3) {
            Log.v(getFullTag(tag), message);
        }

    }

    public static void logExtendedMessage(int logLevel, String tag, String message) {
        if (message != null) {
            String prefix = "";
            int maxEntrySize = 4060 - getFullTag(tag).length() - 4;

            int cutOffIndex;
            ArrayList messagesList;
            for (messagesList = new ArrayList(); !message.isEmpty(); message = message.substring(cutOffIndex)) {
                if (message.length() <= maxEntrySize) {
                    messagesList.add(message);
                    break;
                }

                cutOffIndex = maxEntrySize;
                int nextNewlineIndex = message.lastIndexOf(10, maxEntrySize);
                if (nextNewlineIndex != -1) {
                    cutOffIndex = nextNewlineIndex + 1;
                }

                messagesList.add(message.substring(0, cutOffIndex));
            }

            for (int i = 0; i < messagesList.size(); ++i) {
                if (messagesList.size() > 1) {
                    prefix = "(" + (i + 1) + "/" + messagesList.size() + ")\n";
                }

                logMessage(logLevel, tag, prefix + (String) messagesList.get(i));
            }

        }
    }

    public static void logError(String tag, String message) {
        logMessage(6, tag, message);
    }

    public static void logError(String message) {
        logMessage(6, DEFAULT_LOG_TAG, message);
    }

    public static void logErrorExtended(String tag, String message) {
        logExtendedMessage(6, tag, message);
    }

    public static void logErrorExtended(String message) {
        logExtendedMessage(6, DEFAULT_LOG_TAG, message);
    }

    public static void logErrorPrivate(String tag, String message) {
        if (CURRENT_LOG_LEVEL >= 2) {
            logMessage(6, tag, message);
        }

    }

    public static void logErrorPrivate(String message) {
        if (CURRENT_LOG_LEVEL >= 2) {
            logMessage(6, DEFAULT_LOG_TAG, message);
        }

    }

    public static void logErrorPrivateExtended(String tag, String message) {
        if (CURRENT_LOG_LEVEL >= 2) {
            logExtendedMessage(6, tag, message);
        }

    }

    public static void logErrorPrivateExtended(String message) {
        if (CURRENT_LOG_LEVEL >= 2) {
            logExtendedMessage(6, DEFAULT_LOG_TAG, message);
        }

    }

    public static void logWarn(String tag, String message) {
        logMessage(5, tag, message);
    }

    public static void logWarn(String message) {
        logMessage(5, DEFAULT_LOG_TAG, message);
    }

    public static void logWarnExtended(String tag, String message) {
        logExtendedMessage(5, tag, message);
    }

    public static void logWarnExtended(String message) {
        logExtendedMessage(5, DEFAULT_LOG_TAG, message);
    }

    public static void logInfo(String tag, String message) {
        logMessage(4, tag, message);
    }

    public static void logInfo(String message) {
        logMessage(4, DEFAULT_LOG_TAG, message);
    }

    public static void logInfoExtended(String tag, String message) {
        logExtendedMessage(4, tag, message);
    }

    public static void logInfoExtended(String message) {
        logExtendedMessage(4, DEFAULT_LOG_TAG, message);
    }

    public static void logDebug(String tag, String message) {
        logMessage(3, tag, message);
    }

    public static void logDebug(String message) {
        logMessage(3, DEFAULT_LOG_TAG, message);
    }

    public static void logDebugExtended(String tag, String message) {
        logExtendedMessage(3, tag, message);
    }

    public static void logDebugExtended(String message) {
        logExtendedMessage(3, DEFAULT_LOG_TAG, message);
    }

    public static void logVerbose(String tag, String message) {
        logMessage(2, tag, message);
    }

    public static void logVerbose(String message) {
        logMessage(2, DEFAULT_LOG_TAG, message);
    }

    public static void logVerboseExtended(String tag, String message) {
        logExtendedMessage(2, tag, message);
    }

    public static void logVerboseExtended(String message) {
        logExtendedMessage(2, DEFAULT_LOG_TAG, message);
    }

    public static void logVerboseForce(String tag, String message) {
        Log.v(tag, message);
    }

    public static void logInfoAndShowToast(Context context, String tag, String message) {
        if (CURRENT_LOG_LEVEL >= 1) {
            logInfo(tag, message);
            showToast(context, message, true);
        }

    }

    public static void logInfoAndShowToast(Context context, String message) {
        logInfoAndShowToast(context, DEFAULT_LOG_TAG, message);
    }

    public static void logErrorAndShowToast(Context context, String tag, String message) {
        if (CURRENT_LOG_LEVEL >= 1) {
            logError(tag, message);
            showToast(context, message, true);
        }

    }

    public static void logErrorAndShowToast(Context context, String message) {
        logErrorAndShowToast(context, DEFAULT_LOG_TAG, message);
    }

    public static void logDebugAndShowToast(Context context, String tag, String message) {
        if (CURRENT_LOG_LEVEL >= 2) {
            logDebug(tag, message);
            showToast(context, message, true);
        }

    }

    public static void logDebugAndShowToast(Context context, String message) {
        logDebugAndShowToast(context, DEFAULT_LOG_TAG, message);
    }

    public static void logStackTraceWithMessage(String tag, String message, Throwable throwable) {
        logErrorExtended(tag, getMessageAndStackTraceString(message, throwable));
    }

    public static void logStackTraceWithMessage(String message, Throwable throwable) {
        logStackTraceWithMessage(DEFAULT_LOG_TAG, message, throwable);
    }

    public static void logStackTrace(String tag, Throwable throwable) {
        logStackTraceWithMessage(tag, (String) null, throwable);
    }

    public static void logStackTrace(Throwable throwable) {
        logStackTraceWithMessage(DEFAULT_LOG_TAG, (String) null, throwable);
    }

    public static void logStackTracesWithMessage(String tag, String message, List<Throwable> throwablesList) {
        logErrorExtended(tag, getMessageAndStackTracesString(message, throwablesList));
    }

    public static String getMessageAndStackTraceString(String message, Throwable throwable) {
        if (message == null && throwable == null) {
            return null;
        } else if (message != null && throwable != null) {
            return message + ":\n" + getStackTraceString(throwable);
        } else {
            return throwable == null ? message : getStackTraceString(throwable);
        }
    }

    public static String getMessageAndStackTracesString(String message, List<Throwable> throwablesList) {
        if (message != null || throwablesList != null && throwablesList.size() != 0) {
            if (message != null && throwablesList != null && throwablesList.size() != 0) {
                return message + ":\n" + getStackTracesString((String) null, getStackTracesStringArray(throwablesList));
            } else {
                return throwablesList != null && throwablesList.size() != 0 ? getStackTracesString((String) null, getStackTracesStringArray(throwablesList)) : message;
            }
        } else {
            return null;
        }
    }

    public static String getStackTraceString(Throwable throwable) {
        if (throwable == null) {
            return null;
        } else {
            String stackTraceString = null;

            try {
                StringWriter errors = new StringWriter();
                PrintWriter pw = new PrintWriter(errors);
                throwable.printStackTrace(pw);
                pw.close();
                stackTraceString = errors.toString();
                errors.close();
            } catch (IOException var4) {
                var4.printStackTrace();
            }

            return stackTraceString;
        }
    }

    public static String[] getStackTracesStringArray(Throwable throwable) {
        return getStackTracesStringArray(Collections.singletonList(throwable));
    }

    public static String[] getStackTracesStringArray(List<Throwable> throwablesList) {
        if (throwablesList == null) {
            return null;
        } else {
            String[] stackTraceStringArray = new String[throwablesList.size()];

            for (int i = 0; i < throwablesList.size(); ++i) {
                stackTraceStringArray[i] = getStackTraceString((Throwable) throwablesList.get(i));
            }

            return stackTraceStringArray;
        }
    }

    public static String getStackTracesString(String label, String[] stackTraceStringArray) {
        if (label == null) {
            label = "StackTraces:";
        }

        StringBuilder stackTracesString = new StringBuilder(label);
        if (stackTraceStringArray != null && stackTraceStringArray.length != 0) {
            for (int i = 0; i != stackTraceStringArray.length; ++i) {
                if (stackTraceStringArray.length > 1) {
                    stackTracesString.append("\n\nStacktrace ").append(i + 1);
                }

                stackTracesString.append("\n```\n").append(stackTraceStringArray[i]).append("\n```\n");
            }
        } else {
            stackTracesString.append(" -");
        }

        return stackTracesString.toString();
    }

    public static String getStackTracesMarkdownString(String label, String[] stackTraceStringArray) {
        if (label == null) {
            label = "StackTraces";
        }

        StringBuilder stackTracesString = new StringBuilder("### " + label);
        if (stackTraceStringArray != null && stackTraceStringArray.length != 0) {
            for (int i = 0; i != stackTraceStringArray.length; ++i) {
                if (stackTraceStringArray.length > 1) {
                    stackTracesString.append("\n\n\n#### Stacktrace ").append(i + 1);
                }

                stackTracesString.append("\n\n```\n").append(stackTraceStringArray[i]).append("\n```");
            }
        } else {
            stackTracesString.append("\n\n`-`");
        }

        stackTracesString.append("\n##\n");
        return stackTracesString.toString();
    }

    public static String getSingleLineLogStringEntry(String label, Object object, String def) {
        return object != null ? label + ": `" + object + "`" : label + ": " + def;
    }

    public static String getMultiLineLogStringEntry(String label, Object object, String def) {
        return object != null ? label + ":\n```\n" + object + "\n```\n" : label + ": " + def;
    }

    public static void showToast(final Context context, final String toastText, boolean longDuration) {
        if (context != null && !DataUtils.isNullOrEmpty(toastText)) {
            (new Handler(Looper.getMainLooper())).post(() -> {
                Toast.makeText(context, toastText, longDuration ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show();
            });
        }
    }

    public static CharSequence[] getLogLevelsArray() {
        return new CharSequence[]{String.valueOf(0), String.valueOf(1), String.valueOf(2), String.valueOf(3)};
    }

    public static CharSequence[] getLogLevelLabelsArray(Context context, CharSequence[] logLevels, boolean addDefaultTag) {
        if (logLevels == null) {
            return null;
        } else {
            CharSequence[] logLevelLabels = new CharSequence[logLevels.length];

            for (int i = 0; i < logLevels.length; ++i) {
                logLevelLabels[i] = getLogLevelLabel(context, Integer.parseInt(logLevels[i].toString()), addDefaultTag);
            }

            return logLevelLabels;
        }
    }

    public static String getLogLevelLabel(final Context context, final int logLevel, final boolean addDefaultTag) {
        String logLabel;
        switch (logLevel) {
            case 0:
                logLabel = context.getString(string.log_level_off);
                break;
            case 1:
                logLabel = context.getString(string.log_level_normal);
                break;
            case 2:
                logLabel = context.getString(string.log_level_debug);
                break;
            case 3:
                logLabel = context.getString(string.log_level_verbose);
                break;
            default:
                logLabel = context.getString(string.log_level_unknown);
        }

        return addDefaultTag && logLevel == 1 ? logLabel + " (default)" : logLabel;
    }

    @NonNull
    public static String getDefaultLogTag() {
        return DEFAULT_LOG_TAG;
    }

    public static void setDefaultLogTag(@NonNull String defaultLogTag) {
        DEFAULT_LOG_TAG = defaultLogTag.length() >= 23 ? defaultLogTag.substring(0, 22) : defaultLogTag;
    }

    public static int getLogLevel() {
        return CURRENT_LOG_LEVEL;
    }

    public static int setLogLevel(Context context, int logLevel) {
        if (isLogLevelValid(logLevel)) {
            CURRENT_LOG_LEVEL = logLevel;
        } else {
            CURRENT_LOG_LEVEL = 1;
        }

        if (context != null) {
            showToast(context, context.getString(string.log_level_value, new Object[]{getLogLevelLabel(context, CURRENT_LOG_LEVEL, false)}), true);
        }

        return CURRENT_LOG_LEVEL;
    }

    public static String getFullTag(String tag) {
        return DEFAULT_LOG_TAG.equals(tag) ? tag : DEFAULT_LOG_TAG + "." + tag;
    }

    public static boolean isLogLevelValid(Integer logLevel) {
        return logLevel != null && logLevel >= 0 && logLevel <= 3;
    }

    public static boolean shouldEnableLoggingForCustomLogLevel(Integer customLogLevel) {
        if (CURRENT_LOG_LEVEL <= 0) {
            return false;
        } else if (customLogLevel == null) {
            return CURRENT_LOG_LEVEL >= 3;
        } else if (customLogLevel <= 0) {
            return false;
        } else {
            customLogLevel = isLogLevelValid(customLogLevel) ? customLogLevel : 3;
            return customLogLevel >= CURRENT_LOG_LEVEL;
        }
    }
}
