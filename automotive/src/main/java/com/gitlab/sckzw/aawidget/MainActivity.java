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
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import java.util.Objects;

public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    static final String TAG = MainActivity.class.getSimpleName();
    AppWidgetManager mAppWidgetManager;
    AppWidgetHost mAppWidgetHost;
    AppWidgetHostView mAppWidgetView;
    FrameLayout mLayoutWidgetPreview;
    ImageView mImageWallpaper;
    BottomNavigationView mNavWidgetMenu;
    SharedPreferences mSharedPreferences;
    int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    int mTmpWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    String mWallpaperUri;
    String mBackgroundColor;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setContentView( R.layout.activity_main );

        getSupportFragmentManager()
                .beginTransaction()
                .replace( R.id.layout_preference, new PreferenceFragment() )
                .commit();

        mLayoutWidgetPreview = findViewById( R.id.layout_widget_preview );
        mImageWallpaper = findViewById( R.id.image_wallpaper );
        mNavWidgetMenu = findViewById( R.id.nav_widget_menu );

        mAppWidgetManager = AAWidgetApplication.getAppWidgetManager(); // AppWidgetManager.getInstance( getApplicationContext() );
        mAppWidgetHost = AAWidgetApplication.getAppWidgetHost(); // new AppWidgetHost( getApplicationContext(), 0 );

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences( getApplicationContext() );
        mBackgroundColor = mSharedPreferences.getString( "background_color", "" );
        mWallpaperUri = mSharedPreferences.getString( "wallpaper_uri", "" );
        mTmpWidgetId = mSharedPreferences.getInt( "widget_id", AppWidgetManager.INVALID_APPWIDGET_ID );

        if ( !mBackgroundColor.isEmpty() ) {
            setBackgroundColor( mBackgroundColor );
        }

        if ( !mWallpaperUri.isEmpty() ) {
            addWallpaper( Uri.parse( mWallpaperUri ) );
        }

        mLayoutWidgetPreview.getViewTreeObserver().addOnGlobalLayoutListener( new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mLayoutWidgetPreview.getViewTreeObserver().removeOnGlobalLayoutListener( this );

                if ( mTmpWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID ) {
                    createWidget( mTmpWidgetId );
                }
            }
        } );

        mNavWidgetMenu.setOnItemSelectedListener( new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected( @NonNull MenuItem item ) {
                if ( item.getItemId() == R.id.nav_widget_add ) {
                    if ( mAppWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID ) {
                        deleteWidget();
                    }
                    else {
                        mWidgetListActivityLauncher.launch( new Intent( MainActivity.this, WidgetListActivity.class ) );
                    }

                    return true;
                }
                if ( item.getItemId() == R.id.nav_widget_wallpaper ) {
                    if ( !mWallpaperUri.isEmpty() ) {
                        deleteWallpaper();
                    }
                    else {
                        mWallpaperPickerLauncher.launch( new PickVisualMediaRequest.Builder()
                                .setMediaType( ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE )
                                .build() );
                    }

                    return true;
                }

                return false;
            }
        } );
    }

    @Override
    protected void onStart() {
        super.onStart();
        mSharedPreferences.registerOnSharedPreferenceChangeListener( this );
        // mAppWidgetHost.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mSharedPreferences.unregisterOnSharedPreferenceChangeListener( this );
        // mAppWidgetHost.stopListening();
    }

    public static class PreferenceFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences( Bundle savedInstanceState, String rootKey ) {
            setPreferencesFromResource( R.xml.root_preferences, rootKey );
        }
    }

    @Override
    public void onSharedPreferenceChanged( SharedPreferences pref, @Nullable String key ) {
        if ( Objects.equals( key, "background_color" ) ) {
            String backgroundColor = pref.getString( key, "" );
            setBackgroundColor( backgroundColor );
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

    ActivityResultLauncher< PickVisualMediaRequest > mWallpaperPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.PickVisualMedia(),
            uri -> {
                if ( uri != null ) {
                    addWallpaper( uri );
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

        float density = Resources.getSystem().getDisplayMetrics().density;
        int width = mLayoutWidgetPreview.getWidth();
        int height = mLayoutWidgetPreview.getHeight();
        width = (int)( width / density );
        height = (int)( height / density );

        mAppWidgetView = mAppWidgetHost.createView( getApplicationContext(), appWidgetId, appWidgetInfo );
        mAppWidgetView.setBackgroundColor( Color.alpha( 0 ) );
        mAppWidgetView.setLayoutParams( new ViewGroup.LayoutParams( ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT ) );
        mAppWidgetView.updateAppWidgetSize( null, width, height, width, height );

        mLayoutWidgetPreview.addView( mAppWidgetView );

        mNavWidgetMenu.getMenu().findItem( R.id.nav_widget_add ).setTitle( R.string.remove_widget );

        mAppWidgetId = appWidgetId;
        mTmpWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
        mSharedPreferences.edit().putInt( "widget_id", mAppWidgetId ).commit();
    }

    private void deleteWidget() {
        mLayoutWidgetPreview.removeView( mAppWidgetView );
        mAppWidgetView = null;

        mNavWidgetMenu.getMenu().findItem( R.id.nav_widget_add ).setTitle( R.string.add_widget );

        if ( mAppWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID ) {
            mAppWidgetHost.deleteAppWidgetId( mAppWidgetId );
            mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
            mSharedPreferences.edit().putInt( "widget_id", mAppWidgetId ).commit();

            int[] appWidgetIds = mAppWidgetHost.getAppWidgetIds();
            for ( int appWidgetId: appWidgetIds ) {
                mAppWidgetHost.deleteAppWidgetId( appWidgetId );
            }
        }
    }

    private void addWallpaper( Uri uri ) {
        Bitmap bitmap = null;

        try {
            getContentResolver().takePersistableUriPermission( uri, Intent.FLAG_GRANT_READ_URI_PERMISSION );
            bitmap = MediaStore.Images.Media.getBitmap( getContentResolver(), uri );
        }
        catch ( Exception ex ) {
            Toast.makeText( this, R.string.failed_to_load_wallpaper_image_file, Toast.LENGTH_LONG ).show();
        }

        mImageWallpaper.setImageBitmap( bitmap );
        mNavWidgetMenu.getMenu().findItem( R.id.nav_widget_wallpaper ).setTitle( R.string.remove_wallpaper );

        mWallpaperUri = uri.toString();
        mSharedPreferences.edit().putString( "wallpaper_uri", mWallpaperUri ).commit();
    }

    private void deleteWallpaper() {
        mImageWallpaper.setImageBitmap( null );
        mNavWidgetMenu.getMenu().findItem( R.id.nav_widget_wallpaper ).setTitle( R.string.add_wallpaper );

        mWallpaperUri = "";
        mSharedPreferences.edit().putString( "wallpaper_uri", mWallpaperUri ).commit();
    }

    private void setBackgroundColor( String colorString ) {
        try {
            mLayoutWidgetPreview.setBackgroundColor( Color.parseColor( colorString ) );
        } catch ( Exception ex ) {
            Toast.makeText( this, R.string.enter_background_color_code, Toast.LENGTH_LONG ).show();
        }
    }
}