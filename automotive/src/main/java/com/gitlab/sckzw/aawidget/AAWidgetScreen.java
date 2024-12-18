package com.gitlab.sckzw.aawidget;

import android.app.Presentation;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.Intent;
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
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
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
import androidx.car.app.hardware.CarHardwareManager;
import androidx.car.app.hardware.common.CarValue;
import androidx.car.app.hardware.common.OnCarDataAvailableListener;
import androidx.car.app.hardware.info.CarInfo;
import androidx.car.app.hardware.info.Speed;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.Template;
import androidx.car.app.navigation.model.NavigationTemplate;
import androidx.car.app.navigation.model.PanModeListener;
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
    private ImageView mPointerImageView;

    private Rect mVisibleArea = new Rect();
    private int mSurfaceWidth;
    private int mSurfaceHeight;
    private long mLastScrollTime = -1;
    private int mLastScrollX = -1;
    private int mLastScrollY = -1;
    private int mPointerX;
    private int mPointerY;
    private int mSpeed = -1;
    private boolean mIsInPanMode = false;

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
                mCarContext.getContentResolver().takePersistableUriPermission( uri, Intent.FLAG_GRANT_READ_URI_PERMISSION );
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

        mPointerImageView = new ImageView( mCarContext );
        mPointerImageView.setImageResource( R.drawable.ic_pointer );
        mPointerImageView.setVisibility( View.INVISIBLE );

        float density = Resources.getSystem().getDisplayMetrics().density;
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams( (int)( 12 * density ), (int)( 12 * density ) );
        layoutParams.leftMargin = mPointerX = mSurfaceWidth / 2;
        layoutParams.topMargin = mPointerY = mSurfaceHeight / 2;
        layoutWidget.addView( mPointerImageView, layoutParams );

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
        width = (int)( width / density );
        height = (int)( height / density );

        mAppWidgetView.setPadding( visibleArea.left, visibleArea.top, mSurfaceWidth - visibleArea.right, mSurfaceHeight - visibleArea.bottom );
        mAppWidgetView.updateAppWidgetSize( null, width, height, width, height );

        if ( mPointerX < visibleArea.left ) {
            mPointerX = visibleArea.left;
        }
        if ( mPointerX > visibleArea.right ) {
            mPointerX = visibleArea.right;
        }
        if ( mPointerY < visibleArea.top ) {
            mPointerY = visibleArea.top;
        }
        if ( mPointerY > visibleArea.bottom ) {
            mPointerY = visibleArea.bottom;
        }

        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams)mPointerImageView.getLayoutParams();
        layoutParams.leftMargin = mPointerX;
        layoutParams.topMargin = mPointerY;
        mPointerImageView.setLayoutParams( layoutParams );
    }

    @Override
    public void onClick( float x, float y ) {
        if ( mAppWidgetView == null ) {
            return;
        }

        if ( mSpeed != 0 ) {
            CarToast.makeText( mCarContext, R.string.touch_operations_are_disabled, CarToast.LENGTH_LONG ).show();
            return;
        }

        int clickX = (int)x;
        int clickY = (int)y;

        if ( !mIsInPanMode ) {
            clickX += mVisibleArea.left;
        }

        mAppWidgetView.dispatchTouchEvent( MotionEvent.obtain( SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN, clickX, clickY, 0 ) );
        mAppWidgetView.dispatchTouchEvent( MotionEvent.obtain( SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, clickX, clickY, 0 ) );
    }

    @Override
    public void onScroll( float distanceX, float distanceY ) {
        if ( mAppWidgetView == null ) {
            return;
        }

        if ( mIsInPanMode ) {
            if ( mSpeed != 0 ) {
                CarToast.makeText( mCarContext, R.string.touch_operations_are_disabled, CarToast.LENGTH_LONG ).show();
                return;
            }

            mPointerX += (int)distanceX / 2;
            mPointerY += (int)distanceY / 2;

            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams)mPointerImageView.getLayoutParams();
            layoutParams.leftMargin = mPointerX;
            layoutParams.topMargin = mPointerY;
            mPointerImageView.setLayoutParams( layoutParams );
        }
        else {
            if ( mSpeed != 0 ) {
                return;
            }

            if ( mLastScrollTime < 0 || SystemClock.uptimeMillis() - mLastScrollTime > 500 ) {
                mLastScrollX = mVisibleArea.centerX();
                mLastScrollY = mVisibleArea.centerY();

                mLastScrollTime = SystemClock.uptimeMillis();
                mAppWidgetView.dispatchTouchEvent( MotionEvent.obtain( mLastScrollTime, mLastScrollTime, MotionEvent.ACTION_UP, mLastScrollX, mLastScrollY, 0 ) );
                mLastScrollTime = SystemClock.uptimeMillis();
                mAppWidgetView.dispatchTouchEvent( MotionEvent.obtain( mLastScrollTime, mLastScrollTime, MotionEvent.ACTION_DOWN, mLastScrollX, mLastScrollY, 0 ) );
            }

            mLastScrollX -= (int)distanceX;
            mLastScrollY -= (int)distanceY;

            mLastScrollTime = SystemClock.uptimeMillis();
            mAppWidgetView.dispatchTouchEvent( MotionEvent.obtain( mLastScrollTime, mLastScrollTime, MotionEvent.ACTION_MOVE, mLastScrollX, mLastScrollY, 0 ) );
        }
    }

    private void scroll( int distanceY ) {
        int scrollX = mPointerX;
        int scrollY = mPointerY;
        long scrollTime;

        if ( mSpeed != 0 ) {
            CarToast.makeText( mCarContext, R.string.touch_operations_are_disabled, CarToast.LENGTH_LONG ).show();
            return;
        }

        scrollTime = SystemClock.uptimeMillis();
        mAppWidgetView.dispatchTouchEvent( MotionEvent.obtain( scrollTime, scrollTime, MotionEvent.ACTION_DOWN, scrollX, scrollY, 0 ) );

        for ( int i = 0; i < 5; i ++ ) {
            scrollY += distanceY / 5;

            scrollTime = SystemClock.uptimeMillis();
            mAppWidgetView.dispatchTouchEvent( MotionEvent.obtain( scrollTime, scrollTime, MotionEvent.ACTION_MOVE, scrollX, scrollY, 0 ) );
        }
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
                        .setIcon( new CarIcon.Builder( IconCompat.createWithResource( mCarContext, iconId ) ).build() )
                        .build() )
                .build() );

        builder.setMapActionStrip( new ActionStrip.Builder()
                .addAction( new Action.Builder( Action.PAN )
                        .build() )
                .addAction( new Action.Builder()
                        .setIcon( new CarIcon.Builder( IconCompat.createWithResource( mCarContext, R.drawable.ic_click ) ).build() )
                        .setOnClickListener( () -> AAWidgetScreen.this.onClick( (float)mPointerX, (float)mPointerY ) )
                        .build() )
                .addAction( new Action.Builder()
                        .setIcon( new CarIcon.Builder( IconCompat.createWithResource( mCarContext, R.drawable.ic_scroll_up ) ).build() )
                        .setOnClickListener( () -> scroll( 50 ) )
                        .build() )
                .addAction( new Action.Builder()
                        .setIcon( new CarIcon.Builder( IconCompat.createWithResource( mCarContext, R.drawable.ic_scroll_down ) ).build() )
                        .setOnClickListener( () -> scroll( -50 ) )
                        .build() )
                .build() );

        builder.setPanModeListener( new PanModeListener() {
            @Override
            public void onPanModeChanged( boolean isInPanMode ) {
                mIsInPanMode = isInPanMode;

                if ( isInPanMode ) {
                    mPointerImageView.setVisibility( View.VISIBLE );
                }
                else {
                    mPointerImageView.setVisibility( View.INVISIBLE );
                }
            }
        } );

        return builder.build();
    }

    private final OnCarDataAvailableListener< Speed > mSpeedListener = data -> {
        CarValue< Float > speed = data.getDisplaySpeedMetersPerSecond();

        if ( speed.getStatus() == CarValue.STATUS_SUCCESS && speed.getValue() != null ) {
            mSpeed = speed.getValue().intValue();
        }
        else {
            mSpeed = -1;
        }
    };

    @Override
    public void onResume( @NonNull LifecycleOwner owner ) {
        CarInfo carInfo = mCarContext.getCarService( CarHardwareManager.class ).getCarInfo();
        try {
            carInfo.addSpeedListener( mCarContext.getMainExecutor(), mSpeedListener );
        }
        catch ( SecurityException ignored ) {
        }
    }

    @Override
    public void onPause( @NonNull LifecycleOwner owner ) {
        CarInfo carInfo = mCarContext.getCarService( CarHardwareManager.class ).getCarInfo();
        try {
            carInfo.removeSpeedListener( mSpeedListener );
        }
        catch ( SecurityException ignored ) {
        }
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
