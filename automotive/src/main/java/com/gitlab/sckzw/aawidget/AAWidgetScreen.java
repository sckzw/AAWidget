package com.gitlab.sckzw.aawidget;

import android.app.Presentation;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.util.Log;
import android.view.Surface;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.car.app.AppManager;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.SurfaceCallback;
import androidx.car.app.SurfaceContainer;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.Template;
import androidx.car.app.navigation.model.NavigationTemplate;
import androidx.preference.PreferenceManager;

public class AAWidgetScreen extends Screen implements SurfaceCallback {
    private final static String TAG = AAWidgetScreen.class.getSimpleName();

    private Surface mSurface;
    private VirtualDisplay mVirtualDisplay;
    private Presentation mPresentation;
    private AppWidgetHost mAppWidgetHost;
    private AppWidgetProviderInfo mAppWidgetInfo;

    private AppWidgetManager mAppWidgetManager;
    private AppWidgetHostView mHostView;
    private SharedPreferences mSharedPreferences;
    private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    private final CarContext mCarContext;
    private final Context mAppContext;

    protected AAWidgetScreen( @NonNull CarContext carContext ) {
        super( carContext );
        Log.i( TAG, "AAWidgetScreen constructor" );

        mAppContext = carContext.getApplicationContext();
        mCarContext = carContext;
        mCarContext.getCarService( AppManager.class ).setSurfaceCallback( this );

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences( mAppContext );
        mAppWidgetId = mSharedPreferences.getInt( "widget_id", AppWidgetManager.INVALID_APPWIDGET_ID );

        mAppWidgetHost = new AppWidgetHost( mAppContext, 0 );
        mAppWidgetManager = AppWidgetManager.getInstance( mAppContext );
        mAppWidgetInfo = mAppWidgetManager.getAppWidgetInfo( mAppWidgetId );
    }

    @Override
    public void onSurfaceAvailable( @NonNull SurfaceContainer surfaceContainer ) {
        mSurface = surfaceContainer.getSurface();
        if ( mSurface == null ) {
            return;
        }

        int width = surfaceContainer.getWidth();
        int height = surfaceContainer.getHeight();
        int dpi = surfaceContainer.getDpi();
        if ( width == 0 || height == 0 || dpi == 0 ) {
            return;
        }

        mVirtualDisplay = mCarContext.getSystemService( DisplayManager.class )
                .createVirtualDisplay(
                        mCarContext.getString( R.string.app_name ),
                        width,
                        height,
                        dpi,
                        mSurface,
                        DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY );

        mPresentation = new Presentation( mCarContext, mVirtualDisplay.getDisplay() );

        if ( mAppWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID ) {
            mHostView = mAppWidgetHost.createView( mAppContext, mAppWidgetId, mAppWidgetInfo );
            // mHostView.setLayoutParams( new ViewGroup.LayoutParams( width, height ) );
            // mHostView.setAppWidget( mAppWidgetId, mAppWidgetInfo );

            // mHostView.setDescendantFocusability( ViewGroup.FOCUS_BEFORE_DESCENDANTS );
            // mHostView.setClickable( false );
            // mHostView.setFocusable( false );

            float density = Resources.getSystem().getDisplayMetrics().density;
            width = (int)( width / density );
            height = (int)( height / density );

            mHostView.updateAppWidgetSize( null, width, height, width, height );

            mPresentation.setContentView( mHostView );
        }

        mPresentation.show();
    }

    @Override
    public void onSurfaceDestroyed( @NonNull SurfaceContainer surfaceContainer ) {
        mSurface = null;
        mPresentation.dismiss();
        mVirtualDisplay.release();
    }

    @Override
    public void onStableAreaChanged( @NonNull Rect stableArea ) {
        Log.i( TAG, "Stable area changed: " + stableArea.left + ", " + stableArea.top + ", " + stableArea.right + ", " + stableArea.bottom );
    }

    @Override
    public void onVisibleAreaChanged( @NonNull Rect visibleArea ) {
        Log.i( TAG, "Visible area changed: " + visibleArea.left + ", " + visibleArea.top + ", " + visibleArea.right + ", " + visibleArea.bottom );
        mHostView.setPadding( visibleArea.left, visibleArea.top, 800 - visibleArea.right, 400 - visibleArea.bottom );

        int width = visibleArea.width();
        int height = visibleArea.height();

        // mHostView.setLayoutParams( new FrameLayout.LayoutParams( width, height ) );

        float density = Resources.getSystem().getDisplayMetrics().density;
        width = (int)( width / density );
        height = (int)( height / density );

        mHostView.updateAppWidgetSize( null, width, height, width, height );
    }

    @NonNull
    @Override
    public Template onGetTemplate() {
        return new NavigationTemplate.Builder()
                .setActionStrip( new ActionStrip.Builder()
                        .addAction( new Action.Builder()
                                .setTitle( "Back" )
                                .build() )
                        .build() )
                .build();
    }
}
