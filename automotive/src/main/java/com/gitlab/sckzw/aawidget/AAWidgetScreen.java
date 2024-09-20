package com.gitlab.sckzw.aawidget;

import android.app.Presentation;
import android.content.res.Resources;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.util.Log;
import android.view.Gravity;
import android.view.Surface;
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

public class AAWidgetScreen extends Screen implements SurfaceCallback {
    private final static String TAG = AAWidgetScreen.class.getSimpleName();

    private Surface mSurface;
    private VirtualDisplay mVirtualDisplay;
    private Presentation mPresentation;

    private final CarContext mCarContext;

    protected AAWidgetScreen( @NonNull CarContext carContext ) {
        super( carContext );
        mCarContext = carContext;
        mCarContext.getCarService( AppManager.class ).setSurfaceCallback( this );
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

        float density = Resources.getSystem().getDisplayMetrics().density;
        TextView surfaceInfoTextView = new TextView( mCarContext );
        surfaceInfoTextView.setText( "Width: " + width + ", Height: " + height + ", Dpi: " + dpi + ", Density: " + density );
        surfaceInfoTextView.setGravity( Gravity.CENTER );

        mVirtualDisplay = mCarContext.getSystemService( DisplayManager.class )
                .createVirtualDisplay(
                        mCarContext.getString( R.string.app_name ),
                        width,
                        height,
                        dpi,
                        mSurface,
                        DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY );

        mPresentation = new Presentation( mCarContext, mVirtualDisplay.getDisplay() );
        mPresentation.setContentView( surfaceInfoTextView );
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
        Log.i( TAG, "Visible area changed " + mSurface + " stableArea: " + stableArea );
    }

    @Override
    public void onVisibleAreaChanged( @NonNull Rect visibleArea ) {
        Log.i( TAG, "Visible area changed " + mSurface + " visibleArea: " + visibleArea );
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
