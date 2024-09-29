package com.gitlab.sckzw.aawidget;

import android.annotation.SuppressLint;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WidgetListActivity extends AppCompatActivity {
    private static final String PREF_KEY_AVAILABLE_WIDGET_LIST = "available_widget_list";
    private final List< WidgetInfo > mWidgetInfoList = new ArrayList<>();
    private final WidgetListAdapter mWidgetListAdapter = new WidgetListAdapter();
    private final HashMap< String, Boolean > mAvailableWidgetList = new HashMap<>();
    private PackageManager mPackageManager;
    private AppWidgetManager mAppWidgetManager;
    private SharedPreferences mSharedPreferences;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_widget_list );

        mPackageManager = getApplicationContext().getPackageManager();
        mAppWidgetManager = AppWidgetManager.getInstance( getApplicationContext() );
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences( getApplicationContext() );

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit( new LoadAppListRunnable() );
    }

    @Override
    public void onPause() {
        super.onPause();

        String availableAppList = String.join( ";", mAvailableWidgetList.keySet() );

        mSharedPreferences.edit().putString( PREF_KEY_AVAILABLE_WIDGET_LIST, availableAppList ).apply();

        /*
        Intent intent = new Intent( MessagingService.INTENT_ACTION_SET_PREF );
        intent.putExtra( "key", PREF_KEY_AVAILABLE_WIDGET_LIST );
        intent.putExtra( "value", availableAppList );
        LocalBroadcastManager.getInstance( getApplicationContext() ).sendBroadcast( intent );
         */
    }

    private static class WidgetInfo {
        String appName;
        String pkgName;
        String label;
        String description;
        AppWidgetProviderInfo providerInfo;
        Drawable appIcon;
        boolean isAvailable;

        WidgetInfo( String pkgName, String appName, String label, String description, AppWidgetProviderInfo providerInfo, Drawable appIcon, boolean isAvailable ) {
            this.pkgName = pkgName;
            this.appName = appName;
            this.label = label;
            this.description = description;
            this.providerInfo = providerInfo;
            this.appIcon = appIcon;
            this.isAvailable = isAvailable;
        }
    }

    private class WidgetListAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return mWidgetInfoList.size();
        }

        @Override
        public Object getItem( int position ) {
            return mWidgetInfoList.get( position );
        }

        @Override
        public long getItemId( int position ) {
            return position;
        }

        @SuppressLint("ResourceType")
        @Override
        public View getView( int position, View convertView, ViewGroup parent ) {
            WidgetInfo widgetInfo = (WidgetInfo)getItem( position );

            if ( widgetInfo.appName == null ) {
                try {
                    ApplicationInfo applicationInfo = mPackageManager.getApplicationInfo( widgetInfo.pkgName, 0 );
                    widgetInfo.appName = mPackageManager.getApplicationLabel( applicationInfo ).toString();
                } catch ( PackageManager.NameNotFoundException ex ) {
                    widgetInfo.appName = "";
                }
            }

            if ( widgetInfo.appIcon == null ) {
                try {
                    widgetInfo.appIcon = mPackageManager.getApplicationIcon( widgetInfo.pkgName );
                } catch ( PackageManager.NameNotFoundException ex ) {
                    widgetInfo.appIcon = ResourcesCompat.getDrawable( getResources(), android.R.drawable.sym_def_app_icon, null );
                }
            }

            AppWidgetProviderInfo providerInfo = widgetInfo.providerInfo;
            Context context = getApplicationContext();
            View previewView = null;
            Drawable previewImage = null;

            if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && providerInfo.previewLayout != 0 ) {
                try {
                    Context providerContext = context.createPackageContext( widgetInfo.pkgName, 0 );
                    previewView = LayoutInflater.from( providerContext ).inflate( providerInfo.previewLayout, null );
                }
                catch ( PackageManager.NameNotFoundException ignored ) {
                }
            }

            if ( previewView == null ) {
                int densityDpi = context.getResources().getDisplayMetrics().densityDpi;
                previewImage = providerInfo.loadPreviewImage( context, densityDpi );

                if ( previewImage == null ) {
                    previewImage = providerInfo.loadIcon( context, densityDpi );
                }

                if ( previewImage == null ) {
                    previewImage = widgetInfo.appIcon;
                }

                ImageView imageView = new ImageView( context );
                imageView.setImageDrawable( previewImage );
                previewView = imageView;
            }

            View listItemView = convertView;
            if ( listItemView == null ) {
                listItemView = ( (LayoutInflater)getSystemService( Context.LAYOUT_INFLATER_SERVICE ) ).inflate( R.layout.widget_list_item, null );
            }

            ImageView imageAppIcon = listItemView.findViewById( R.id.image_app_icon );
            TextView textAppName = listItemView.findViewById( R.id.text_app_name );
            TextView textPkgName = listItemView.findViewById( R.id.text_pkg_name );
            TextView textWidgetLabel = listItemView.findViewById( R.id.text_widget_label );
            TextView textWidgetDescription = listItemView.findViewById( R.id.text_widget_description );

            imageAppIcon.setImageDrawable( widgetInfo.appIcon );
            textAppName.setText( widgetInfo.appName );
            textPkgName.setText( widgetInfo.pkgName );
            textWidgetLabel.setText( widgetInfo.label );
            textWidgetDescription.setText( widgetInfo.description );

            FrameLayout layoutWidgetPreview = listItemView.findViewById( R.id.layout_widget_preview );
            layoutWidgetPreview.removeAllViews();
            layoutWidgetPreview.addView( previewView );

            return listItemView;
        }
    }

    private class LoadAppListRunnable implements Runnable {
        @Override
        public void run() {
            ProgressBar progressBar = findViewById( R.id.progress_bar );
            List< AppWidgetProviderInfo > widgetProviderInfoList = mAppWidgetManager.getInstalledProviders();
            String availableAppList = ";" + mSharedPreferences.getString( PREF_KEY_AVAILABLE_WIDGET_LIST, "" ) + ";";

            int widgetNum = widgetProviderInfoList.size();
            int widgetCnt = 0;

            for ( AppWidgetProviderInfo widgetProviderInfo : widgetProviderInfoList ) {
                String pkgName = widgetProviderInfo.provider.getPackageName();
                String appName = "";
                try {
                    ApplicationInfo appInfo = mPackageManager.getApplicationInfo( pkgName, 0 );
                    appName = appInfo.loadLabel( mPackageManager ).toString();
                }
                catch ( Exception ignored ) {
                }

                String label = widgetProviderInfo.loadLabel( mPackageManager );
                if ( label == null ) {
                    label = widgetProviderInfo.label;
                }

                String description = "";
                if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ) {
                    CharSequence descriptionCharSeq = widgetProviderInfo.loadDescription( getApplicationContext() );
                    if ( descriptionCharSeq != null ) {
                        description = descriptionCharSeq.toString();
                    }
                }

                mWidgetInfoList.add( new WidgetInfo(
                        pkgName,
                        appName,
                        label,
                        description,
                        widgetProviderInfo,
                        null, // appInfo.loadIcon( mPackageManager )
                        true
                ) );

                progressBar.setProgress( 100 * ( ++widgetCnt ) / widgetNum );
            }

            mWidgetInfoList.sort( new Comparator< WidgetInfo >() {
                @Override
                public int compare( WidgetInfo widgetInfo1, WidgetInfo widgetInfo2 ) {
                    if ( widgetInfo1.isAvailable == widgetInfo2.isAvailable ) {
                        return widgetInfo1.appName.compareTo( widgetInfo2.appName );
                    }
                    else {
                        return widgetInfo1.isAvailable ? -1 : 1;
                    }
                }
            } );

            progressBar.setVisibility( android.widget.ProgressBar.INVISIBLE );

            ListView listView = findViewById( R.id.widget_list_view );
            listView.setOnItemClickListener( new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick( AdapterView< ? > adapterView, View view, int i, long l ) {
                    WidgetInfo widgetInfo = mWidgetInfoList.get( i );
                    widgetInfo.isAvailable = !widgetInfo.isAvailable;
                    mWidgetListAdapter.notifyDataSetChanged();

                    if ( widgetInfo.isAvailable ) {
                        mAvailableWidgetList.put( widgetInfo.pkgName, true );
                    }
                    else {
                        mAvailableWidgetList.remove( widgetInfo.pkgName );
                    }
                }
            } );

            Handler handler = new Handler( Looper.getMainLooper() );
            handler.post( new Runnable() {
                @Override
                public void run() {
                    ListView listView = findViewById( R.id.widget_list_view );
                    listView.setAdapter( mWidgetListAdapter );
                }
            } );
        }
    }
}