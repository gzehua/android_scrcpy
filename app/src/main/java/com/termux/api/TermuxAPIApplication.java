package com.termux.api;

import android.app.Application;
import android.content.Context;

import com.termux.api.util.ResultReturner;
import com.termux.shared.logger.Logger;
import com.termux.shared.termux.TermuxConstants;


public class TermuxAPIApplication extends Application {

    public void onCreate() {
        super.onCreate();
        Logger.setLogLevel(this, Logger.MAX_LOG_LEVEL);
        ResultReturner.setContext(this);
        // Set log config for the app
//        setLogConfig(getApplicationContext(), true);
        Logger.logDebug("Starting Application");
        SocketListener.createSocketListener(this);
    }

    public static void setLogConfig(Context context, boolean commitToFile) {
        Logger.setDefaultLogTag(TermuxConstants.TERMUX_API_APP_NAME.replaceAll(":", ""));
    }

}
