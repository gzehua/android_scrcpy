package com.termux.api;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;

import com.termux.api.apis.ToastAPI;
import com.termux.api.apis.UsbAPI;
import com.termux.api.apis.VibrateAPI;
import com.termux.api.util.ResultReturner;
import com.termux.shared.logger.Logger;

import java.util.Arrays;
import java.util.Iterator;
//import com.termux.shared.termux.plugins.TermuxPluginUtils;

public class TermuxApiReceiver extends BroadcastReceiver {

    private static final String LOG_TAG = "TermuxApiReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        TermuxAPIApplication.setLogConfig(context, false);
        Logger.logDebug(LOG_TAG, "Intent Received:\n" + getIntentString(intent));

        try {
            doWork(context, intent);
        } catch (Throwable t) {
            String message = "Error in " + LOG_TAG;
            // Make sure never to throw exception from BroadCastReceiver to avoid "process is bad"
            // behaviour from the Android system.
            Logger.logStackTraceWithMessage(LOG_TAG, message, t);

//            TermuxPluginUtils.sendPluginCommandErrorNotification(context, LOG_TAG,
//                    TermuxConstants.TERMUX_API_APP_NAME + " Error", message, t);

            ResultReturner.noteDone(this, intent);
        }
    }

    private void doWork(Context context, Intent intent) {
        String apiMethod = intent.getStringExtra("api_method");
        if (apiMethod == null) {
            Logger.logError(LOG_TAG, "Missing 'api_method' extra");
            return;
        }

        switch (apiMethod) {
            case "Usb":
                UsbAPI.onReceive(this, context, intent);
                break;
            case "Vibrate":
                VibrateAPI.onReceive(this, context, intent);
                break;
            case "Toast":
                ToastAPI.onReceive(context, intent);
                break;
            default:
                Logger.logError(LOG_TAG, "Unrecognized 'api_method' extra: '" + apiMethod + "'");
        }
    }

    public static String getIntentString(Intent intent) {
        return intent == null ? null : intent.toString() + "\n" + getBundleString(intent.getExtras());
    }

    public static String getBundleString(Bundle bundle) {
        if (bundle != null && bundle.size() != 0) {
            StringBuilder bundleString = new StringBuilder("Bundle[\n");
            boolean first = true;

            for (Iterator var3 = bundle.keySet().iterator(); var3.hasNext(); first = false) {
                String key = (String) var3.next();
                if (!first) {
                    bundleString.append("\n");
                }

                bundleString.append(key).append(": `");
                Object value = bundle.get(key);
                if (value instanceof int[]) {
                    bundleString.append(Arrays.toString((int[]) ((int[]) value)));
                } else if (value instanceof byte[]) {
                    bundleString.append(Arrays.toString((byte[]) ((byte[]) value)));
                } else if (value instanceof boolean[]) {
                    bundleString.append(Arrays.toString((boolean[]) ((boolean[]) value)));
                } else if (value instanceof short[]) {
                    bundleString.append(Arrays.toString((short[]) ((short[]) value)));
                } else if (value instanceof long[]) {
                    bundleString.append(Arrays.toString((long[]) ((long[]) value)));
                } else if (value instanceof float[]) {
                    bundleString.append(Arrays.toString((float[]) ((float[]) value)));
                } else if (value instanceof double[]) {
                    bundleString.append(Arrays.toString((double[]) ((double[]) value)));
                } else if (value instanceof String[]) {
                    bundleString.append(Arrays.toString((String[]) ((String[]) value)));
                } else if (value instanceof CharSequence[]) {
                    bundleString.append(Arrays.toString((CharSequence[]) ((CharSequence[]) value)));
                } else if (value instanceof Parcelable[]) {
                    bundleString.append(Arrays.toString((Parcelable[]) ((Parcelable[]) value)));
                } else if (value instanceof Bundle) {
                    bundleString.append(getBundleString((Bundle) value));
                } else {
                    bundleString.append(value);
                }

                bundleString.append("`");
            }

            bundleString.append("\n]");
            return bundleString.toString();
        } else {
            return "Bundle[]";
        }
    }

}
