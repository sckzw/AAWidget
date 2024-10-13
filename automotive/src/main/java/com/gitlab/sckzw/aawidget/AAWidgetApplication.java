package com.gitlab.sckzw.aawidget;

import android.app.Application;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetManager;

public class AAWidgetApplication extends Application {
    private static AppWidgetManager mAppWidgetManager;
    private static AppWidgetHost mAppWidgetHost;

    @Override
    public void onCreate() {
        super.onCreate();

        mAppWidgetManager = AppWidgetManager.getInstance( getApplicationContext() );
        mAppWidgetHost = new AppWidgetHost( getApplicationContext(), 0 );
        mAppWidgetHost.startListening();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();

        mAppWidgetHost.stopListening();
        mAppWidgetHost = null;
    }

    public static AppWidgetManager getAppWidgetManager() {
        return mAppWidgetManager;
    }

    public static AppWidgetHost getAppWidgetHost() {
        return mAppWidgetHost;
    }
}
