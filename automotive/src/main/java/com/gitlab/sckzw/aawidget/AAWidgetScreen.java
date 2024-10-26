package com.gitlab.sckzw.aawidget;

import android.app.Presentation;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.net.Uri;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.car.app.AppManager;
import androidx.car.app.CarAppService;
import androidx.car.app.CarContext;
import androidx.car.app.CarToast;
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
    private final Class< ? extends CarAppService > mCarAppServiceClass;
    private final Context mAppContext;
    private final CarContext mCarContext;
    private final AppWidgetHost mAppWidgetHost;
    private final AppWidgetManager mAppWidgetManager;
    private final SharedPreferences mSharedPreferences;

    private Surface mSurface;
    private VirtualDisplay mVirtualDisplay;
    private Presentation mPresentation;
    private AppWidgetHostView mAppWidgetView;

    private Rect mVisibleArea = new Rect();
    private int mSurfaceWidth;
    private int mSurfaceHeight;
    private int mZoomRatio = 100;

    protected AAWidgetScreen( @NonNull CarContext carContext, Class< ? extends CarAppService > carAppServiceClass ) {
        super( carContext );
        mCarAppServiceClass = carAppServiceClass;

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

        mVirtualDisplay = mCarContext.getSystemService( DisplayManager.class )
                .createVirtualDisplay(
                        mCarContext.getString( R.string.app_name ),
                        mSurfaceWidth,
                        mSurfaceHeight,
                        dpi,
                        mSurface,
                        DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION );

        mPresentation = new Presentation( mCarContext, mVirtualDisplay.getDisplay() );

        String backgroundColor = mSharedPreferences.getString( "background_color", "" );
        String wallpaperUri = mSharedPreferences.getString( "wallpaper_uri", "" );
        int appWidgetId = mSharedPreferences.getInt( "widget_id", AppWidgetManager.INVALID_APPWIDGET_ID );

        FrameLayout layoutWidget = new FrameLayout( mCarContext );

        try {
            layoutWidget.setBackgroundColor( Color.parseColor( backgroundColor ) );
        }
        catch ( Exception ignored ) {
        }

        if ( !wallpaperUri.isEmpty() ) {
            Bitmap bitmap = null;

            try {
                Uri uri = Uri.parse( wallpaperUri );
                bitmap = MediaStore.Images.Media.getBitmap( mCarContext.getContentResolver(), uri );
            }
            catch ( Exception ex ) {
                CarToast.makeText( mCarContext, R.string.failed_to_load_wallpaper_image_file, CarToast.LENGTH_LONG ).show();
            }

            ImageView imageWallpaper = new ImageView( mCarContext );
            imageWallpaper.setImageBitmap( bitmap );
            imageWallpaper.setScaleType( ImageView.ScaleType.CENTER_CROP );

            layoutWidget.addView( imageWallpaper );
        }

        if ( appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID ) {
            AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo( appWidgetId );
            mAppWidgetView = mAppWidgetHost.createView( mAppContext, appWidgetId, appWidgetInfo );

            layoutWidget.addView( mAppWidgetView );
        }

        mPresentation.setContentView( layoutWidget );
        mPresentation.show();
    }

    @Override
    public void onSurfaceDestroyed( @NonNull SurfaceContainer surfaceContainer ) {
        mSurface = null;
        mPresentation.dismiss();
        mVirtualDisplay.release();
    }

    @Override
    public void onVisibleAreaChanged( @NonNull Rect visibleArea ) {
        if ( mAppWidgetView == null ) {
            return;
        }

        mVisibleArea = visibleArea;
        int width = visibleArea.width();
        int height = visibleArea.height();

        float density = Resources.getSystem().getDisplayMetrics().density;
        width = (int)( width / density * ( mZoomRatio / 100.0f ) );
        height = (int)( height / density * ( mZoomRatio / 100.0f ) );

        mAppWidgetView.setPadding( visibleArea.left, visibleArea.top, mSurfaceWidth - visibleArea.right, mSurfaceHeight - visibleArea.bottom );
        mAppWidgetView.updateAppWidgetSize( null, width, height, width, height );
    }

    @Override
    public void onClick( float x, float y ) {
        int[] coordinates = new int[2];
        mAppWidgetView.getLocationOnScreen( coordinates );
        int xx = coordinates[0] + (int)x;
        int yy = coordinates[1] + (int)y;
        mAppWidgetView.dispatchTouchEvent( MotionEvent.obtain( SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN, xx, yy, 0 ) );
        mAppWidgetView.dispatchTouchEvent( MotionEvent.obtain( SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, xx, yy, 0 ) );
        Log.i( TAG, "onClick x:" + x + ", y:" + y );
    }

    @NonNull
    @Override
    public Template onGetTemplate() {
        int iconId;

        if ( mCarAppServiceClass.isAssignableFrom( AAWidgetCarAppService.class ) ) {
            iconId = R.drawable.ic_widgets;
        }
        else {
            iconId = R.drawable.ic_wallpaper;
        }

        NavigationTemplate.Builder builder = new NavigationTemplate.Builder();

        builder.setActionStrip( new ActionStrip.Builder()
                .addAction( new Action.Builder()
                        .setIcon( new CarIcon.Builder(
                                IconCompat.createWithResource(
                                        getCarContext(),
                                        iconId ) )
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
                                onVisibleAreaChanged( mVisibleArea );
                            }
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
                                onVisibleAreaChanged( mVisibleArea );
                            }
                        } )
                        .build() )
                .addAction( new Action.Builder( Action.PAN )
                        .build() )
                .build() );

        return builder.build();
    }

    @Override
    public void onStart( @NonNull LifecycleOwner owner ) {
        DefaultLifecycleObserver.super.onStart( owner );
        // mAppWidgetHost.startListening();
    }

    @Override
    public void onStop( @NonNull LifecycleOwner owner ) {
        DefaultLifecycleObserver.super.onStop( owner );
        // mAppWidgetHost.stopListening();
    }
}
