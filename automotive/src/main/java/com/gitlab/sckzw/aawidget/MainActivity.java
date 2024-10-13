package com.gitlab.sckzw.aawidget;

import android.app.Activity;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.os.UserHandle;
import android.util.Log;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

public class MainActivity extends AppCompatActivity {
    static final String TAG = MainActivity.class.getSimpleName();
    AppWidgetManager mAppWidgetManager;
    AppWidgetHost mAppWidgetHost;
    SharedPreferences mSharedPreferences;
    int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    int mTmpWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    FrameLayout mLayoutWidgetPreview;
    ExtendedFloatingActionButton mFabWidget;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        mAppWidgetManager = AAWidgetApplication.getAppWidgetManager(); // AppWidgetManager.getInstance( getApplicationContext() );
        mAppWidgetHost = AAWidgetApplication.getAppWidgetHost(); // new AppWidgetHost( getApplicationContext(), 0 );
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences( getApplicationContext() );
        mTmpWidgetId = mSharedPreferences.getInt( "widget_id", AppWidgetManager.INVALID_APPWIDGET_ID );

        setContentView( R.layout.activity_main );

        getSupportFragmentManager()
                .beginTransaction()
                .replace( R.id.layout_preference, new PreferenceFragment() )
                .commit();

        mLayoutWidgetPreview = findViewById( R.id.layout_widget_preview );
        mLayoutWidgetPreview.getViewTreeObserver().addOnGlobalLayoutListener( new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mLayoutWidgetPreview.getViewTreeObserver().removeOnGlobalLayoutListener( this );

                if ( mTmpWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID ) {
                    createWidget( mTmpWidgetId );
                }
            }
        } );

        mFabWidget = findViewById( R.id.fab_widget );
        mFabWidget.setOnClickListener( v -> {
            if ( mAppWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID ) {
                deleteWidget();
            }
            else {
                mWidgetListActivityLauncher.launch( new Intent( MainActivity.this, WidgetListActivity.class ) );
            }
        } );
    }

    @Override
    protected void onStart() {
        super.onStart();
        // mAppWidgetHost.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // mAppWidgetHost.stopListening();
    }

    public static class PreferenceFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences( Bundle savedInstanceState, String rootKey ) {
            setPreferencesFromResource( R.xml.root_preferences, rootKey );
        }
    }

    ActivityResultLauncher< Intent > mWidgetListActivityLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Intent data = result.getData();
                int resultCode = result.getResultCode();

                if ( resultCode == Activity.RESULT_OK && data != null ) {
                    ComponentName provider = data.getParcelableExtra( AppWidgetManager.EXTRA_APPWIDGET_PROVIDER );
                    UserHandle profile = data.getParcelableExtra( AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE );

                    bindWidget( provider, profile );
                }
            } );

    private void bindWidget( ComponentName provider, UserHandle profile ) {
        if ( mTmpWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID ) {
            mAppWidgetHost.deleteAppWidgetId( mTmpWidgetId );
        }
        mTmpWidgetId = mAppWidgetHost.allocateAppWidgetId();

        boolean bindAllowed = mAppWidgetManager.bindAppWidgetIdIfAllowed( mTmpWidgetId, profile, provider, null );

        if ( bindAllowed ) {
            configureWidget( mTmpWidgetId );
        }
        else {
            Intent intent = new Intent( AppWidgetManager.ACTION_APPWIDGET_BIND );
            intent.putExtra( AppWidgetManager.EXTRA_APPWIDGET_ID, mTmpWidgetId );
            intent.putExtra( AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider );
            mAppWidgetBindLauncher.launch( intent );
        }
    }

    ActivityResultLauncher< Intent > mAppWidgetBindLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                int resultCode = result.getResultCode();

                if ( resultCode == Activity.RESULT_OK ) {
                    configureWidget( mTmpWidgetId );
                }
                else {
                    mAppWidgetHost.deleteAppWidgetId( mTmpWidgetId );
                    mTmpWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
                }
            } );

    private void configureWidget( int appWidgetId ) {
        AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo( appWidgetId );

        if ( appWidgetInfo.configure == null ) {
            createWidget( appWidgetId );
        }
        else {
            /*
            Intent intent = new Intent( AppWidgetManager.ACTION_APPWIDGET_CONFIGURE );
            intent.setComponent( appWidgetInfo.configure );
            intent.putExtra( AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId );
            mAppWidgetConfigureLauncher.launch( intent );
             */

            mAppWidgetHost.startAppWidgetConfigureActivityForResult( this, appWidgetId, 0, 100, null );
        }
    }

    @Override
    protected void onActivityResult( int requestCode, int resultCode, Intent data ) {
        if ( requestCode == 100 ) {
            if ( resultCode == RESULT_OK ) {
                createWidget( mTmpWidgetId );
            }
            else {
                mAppWidgetHost.deleteAppWidgetId( mTmpWidgetId );
                mTmpWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
            }
        }
        else {
            super.onActivityResult( requestCode, resultCode, data );
        }
    }

    private void createWidget( int appWidgetId ) {
        AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo( appWidgetId );
        if ( appWidgetInfo == null ) {
            mAppWidgetHost.deleteAppWidgetId( appWidgetId );
            mTmpWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
            return;
        }

        AppWidgetHostView hostView = mAppWidgetHost.createView( getApplicationContext(), appWidgetId, appWidgetInfo );
        hostView.setLayoutParams( new ViewGroup.LayoutParams( ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT ) );
        // hostView.setAppWidget( appWidgetId, appWidgetInfo );

        String backgroundColor = mSharedPreferences.getString( "background_color", "" );
        try {
            hostView.setBackgroundColor( Color.parseColor( backgroundColor ) );
        }
        catch ( Exception ignored ) {
        }

        float density = Resources.getSystem().getDisplayMetrics().density;
        int width = mLayoutWidgetPreview.getWidth();
        int height = mLayoutWidgetPreview.getHeight();
        width = (int)( width / density );
        height = (int)( height / density );

        hostView.updateAppWidgetSize( null, width, height, width, height );
        Log.i( TAG, "updateAppWidgetSize " + width + ", " + height + ", " + density );

        mLayoutWidgetPreview.removeAllViews();
        mLayoutWidgetPreview.addView( hostView );

        mFabWidget.setText( R.string.remove_widget );

        mAppWidgetId = appWidgetId;
        mTmpWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
        mSharedPreferences.edit().putInt( "widget_id", mAppWidgetId ).commit();
    }

    private void deleteWidget() {
        mLayoutWidgetPreview.removeAllViews();
        mFabWidget.setText( R.string.add_widget );

        if ( mAppWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID ) {
            mAppWidgetHost.deleteAppWidgetId( mAppWidgetId );
            mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
            mSharedPreferences.edit().putInt( "widget_id", mAppWidgetId ).commit();
        }
    }
}