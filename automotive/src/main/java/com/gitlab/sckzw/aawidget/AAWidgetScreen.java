package com.gitlab.sckzw.aawidget;

import android.app.Presentation;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.car.app.AppManager;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.SurfaceCallback;
import androidx.car.app.SurfaceContainer;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.OnClickListener;
import androidx.car.app.model.Template;
import androidx.car.app.navigation.model.NavigationTemplate;
import androidx.core.graphics.drawable.IconCompat;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.PreferenceManager;

public class AAWidgetScreen extends Screen implements SurfaceCallback, DefaultLifecycleObserver {
    private final static String TAG = AAWidgetScreen.class.getSimpleName();
    private final Context mAppContext;
    private final CarContext mCarContext;
    private final AppWidgetHost mAppWidgetHost;
    private final AppWidgetManager mAppWidgetManager;
    private final SharedPreferences mSharedPreferences;

    private Surface mSurface;
    private SurfaceContainer mSurfaceContainer;
    private VirtualDisplay mVirtualDisplay;
    private Presentation mPresentation;
    private AppWidgetHostView mHostView;

    private int mSurfaceWidth;
    private int mSurfaceHeight;
    private Rect mVisibleArea = new Rect();
    private int mZoomRatio = 100;

    protected AAWidgetScreen( @NonNull CarContext carContext ) {
        super( carContext );
        Log.i( TAG, "AAWidgetScreen constructor" );

        mAppContext = carContext.getApplicationContext();
        mCarContext = carContext;
        mCarContext.getCarService( AppManager.class ).setSurfaceCallback( this );

        mAppWidgetHost = AAWidgetApplication.getAppWidgetHost(); // new AppWidgetHost( mAppContext, 0 );
        mAppWidgetManager = AAWidgetApplication.getAppWidgetManager(); // AppWidgetManager.getInstance( mAppContext );

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences( mAppContext );

        getLifecycle().addObserver( this );
    }

    @Override
    public void onSurfaceAvailable( @NonNull SurfaceContainer surfaceContainer ) {
        Log.i( TAG, "AAWidgetScreen onSurfaceAvailable" );
        mSurfaceContainer = surfaceContainer;
        mSurface = surfaceContainer.getSurface();
        if ( mSurface == null ) {
            return;
        }

        mSurfaceWidth = surfaceContainer.getWidth();
        mSurfaceHeight = surfaceContainer.getHeight();
        int dpi = surfaceContainer.getDpi();
        if ( mSurfaceWidth == 0 || mSurfaceHeight == 0 || dpi == 0 ) {
            return;
        }

        int appWidgetId = mSharedPreferences.getInt( "widget_id", AppWidgetManager.INVALID_APPWIDGET_ID );
        String backgroundColor = mSharedPreferences.getString( "background_color", "" );

        mVirtualDisplay = mCarContext.getSystemService( DisplayManager.class )
                .createVirtualDisplay(
                        mCarContext.getString( R.string.app_name ),
                        mSurfaceWidth,
                        mSurfaceHeight,
                        dpi,
                        mSurface,
                        DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION );

        mPresentation = new Presentation( mCarContext, mVirtualDisplay.getDisplay() );

        if ( appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID ) {
            AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo( appWidgetId );
            mHostView = mAppWidgetHost.createView( mAppContext, appWidgetId, appWidgetInfo );

            try {
                mHostView.setBackgroundColor( Color.parseColor( backgroundColor ) );
            }
            catch ( Exception ignored ) {
            }

            /*
            float density = Resources.getSystem().getDisplayMetrics().density;
            int width = (int)( mSurfaceWidth / density * mScaleRatio );
            int height = (int)( mSurfaceHeight / density * mScaleRatio );

            mHostView.updateAppWidgetSize( null, width, height, width, height );
            Log.i( TAG, "updateAppWidgetSize " + width + ", " + height + ", " + density );
            */

            mPresentation.setContentView( mHostView );
        }

        mPresentation.show();
    }

    @Override
    public void onSurfaceDestroyed( @NonNull SurfaceContainer surfaceContainer ) {
        Log.i( TAG, "AAWidgetScreen onSurfaceDestroyed" );
        mSurface = null;
        mPresentation.dismiss();
        mVirtualDisplay.release();
    }

    @Override
    public void onStableAreaChanged( @NonNull Rect stableArea ) {
        Log.i( TAG, "AAWidgetScreen onStableAreaChanged: " + stableArea.left + ", " + stableArea.top + ", " + stableArea.right + ", " + stableArea.bottom );
    }

    @Override
    public void onVisibleAreaChanged( @NonNull Rect visibleArea ) {
        Log.i( TAG, "AAWidgetScreen onVisibleAreaChanged: " + visibleArea.left + ", " + visibleArea.top + ", " + visibleArea.right + ", " + visibleArea.bottom );

        if ( mHostView == null ) {
            return;
        }

        mVisibleArea = visibleArea;
        int width = visibleArea.width();
        int height = visibleArea.height();

        float density = Resources.getSystem().getDisplayMetrics().density;
        width = (int)( width / density * ( mZoomRatio / 100.0f ) );
        height = (int)( height / density * ( mZoomRatio / 100.0f ) );

        // onSurfaceDestroyed( mSurfaceContainer );
        // onSurfaceAvailable( mSurfaceContainer );

        mHostView.setPadding( visibleArea.left, visibleArea.top, mSurfaceWidth - visibleArea.right, mSurfaceHeight - visibleArea.bottom );
        mHostView.updateAppWidgetSize( null, width, height, width, height );
        Log.i( TAG, "updateAppWidgetSize " + width + ", " + height + ", " + density );
    }

    @NonNull
    @Override
    public Template onGetTemplate() {
        NavigationTemplate.Builder builder = new NavigationTemplate.Builder();

        builder.setActionStrip( new ActionStrip.Builder()
                .addAction( new Action.Builder()
                        .setIcon( new CarIcon.Builder(
                                IconCompat.createWithResource(
                                        getCarContext(),
                                        R.drawable.ic_refresh ) )
                                .build() )
                        .setOnClickListener( new OnClickListener() {
                            @Override
                            public void onClick() {
                                mZoomRatio = 100;
                                onVisibleAreaChanged( mVisibleArea );
                            }
                        } )
                        .build() )
                .build() );

        builder.setMapActionStrip( new ActionStrip.Builder()
                .addAction( new Action.Builder()
                        .setIcon( new CarIcon.Builder(
                                IconCompat.createWithResource(
                                        getCarContext(),
                                        R.drawable.ic_zoom_out ) )
                                .build() )
                        .setOnClickListener( new OnClickListener() {
                            @Override
                            public void onClick() {
                                mZoomRatio -= 10;
                                if ( mZoomRatio < 10 ) {
                                    mZoomRatio = 10;
                                }
                                onVisibleAreaChanged( mVisibleArea );                            }
                        } )
                        .build() )
                .addAction( new Action.Builder()
                        .setIcon( new CarIcon.Builder(
                                IconCompat.createWithResource(
                                        getCarContext(),
                                        R.drawable.ic_zoom_in ) )
                                .build() )
                        .setOnClickListener( new OnClickListener() {
                            @Override
                            public void onClick() {
                                mZoomRatio += 10;
                                if ( mZoomRatio > 200 ) {
                                    mZoomRatio = 200;
                                }
                                onVisibleAreaChanged( mVisibleArea );                            }
                        } )
                        .build() )
                .build() );

        return builder.build();
    }

    @Override
    public void onStart( @NonNull LifecycleOwner owner ) {
        DefaultLifecycleObserver.super.onStart( owner );
        Log.i( TAG, "AAWidgetScreen onStart" );
        // mAppWidgetHost.startListening();
    }

    @Override
    public void onStop( @NonNull LifecycleOwner owner ) {
        DefaultLifecycleObserver.super.onStop( owner );
        Log.i( TAG, "AAWidgetScreen onStop" );
        // mAppWidgetHost.stopListening();
    }
}
