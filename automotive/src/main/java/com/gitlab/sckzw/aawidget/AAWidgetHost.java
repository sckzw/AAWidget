package com.gitlab.sckzw.aawidget;

import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;

public class AAWidgetHost extends AppWidgetHost {
    public AAWidgetHost( Context context, int hostId ) {
        super( context, hostId );
    }

    @Override
    protected AppWidgetHostView onCreateView( Context context, int appWidgetId, AppWidgetProviderInfo appWidget ) {
        return new AAWidgetHostView( context );
    }
}
