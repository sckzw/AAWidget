package com.gitlab.sckzw.aawidget;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class WidgetListActivity extends AppCompatActivity {
    private static final String PREF_KEY_AVAILABLE_WIDGET_LIST = "available_widget_list";
    private final List< WidgetListItem > mAppList = new ArrayList<>();
    private final HashMap< String, Boolean > mAvailableWidgetList = new HashMap<>();
    private PackageManager mPackageManager;
    private SharedPreferences mSharedPreferences;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_widget_list );

        mPackageManager = getApplicationContext().getPackageManager();
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences( getApplicationContext() );

        new LoadAppListTask().execute();
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

    private static class WidgetListItem {
        String appName;
        String pkgName;
        Drawable appIcon;
        boolean isAvailable;

        WidgetListItem( String pkgName, String appName, Drawable appIcon, boolean isAvailable ) {
            this.pkgName = pkgName;
            this.appName = appName;
            this.appIcon = appIcon;
            this.isAvailable = isAvailable;
        }
    }

    private class AppListAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return mAppList.size();
        }

        @Override
        public Object getItem( int position ) {
            return mAppList.get( position );
        }

        @Override
        public long getItemId( int position ) {
            return position;
        }

        @Override
        public View getView( int position, View convertView, ViewGroup parent ) {
            View listItemView = convertView;
            ImageView imageAppIcon;
            TextView textAppName;
            TextView textPkgName;
            Switch switchIsEnabled;

            if ( listItemView == null ) {
                listItemView = ( (LayoutInflater)getSystemService( Context.LAYOUT_INFLATER_SERVICE ) ).inflate( R.layout.widget_list_item, null );
            }

            WidgetListItem widgetListItem = (WidgetListItem)getItem( position );

            if ( widgetListItem != null ) {
                imageAppIcon    = listItemView.findViewById( R.id.image_app_icon );
                textAppName     = listItemView.findViewById( R.id.text_app_name );
                textPkgName     = listItemView.findViewById( R.id.text_pkg_name );
                switchIsEnabled = listItemView.findViewById( R.id.switch_is_enabled );

                if ( widgetListItem.appName == null ) {
                    try {
                        ApplicationInfo applicationInfo = mPackageManager.getApplicationInfo( widgetListItem.pkgName, 0 );
                        widgetListItem.appName = mPackageManager.getApplicationLabel( applicationInfo ).toString();
                    } catch ( PackageManager.NameNotFoundException ex ) {
                        widgetListItem.appName = "";
                    }
                }

                if ( widgetListItem.appIcon == null ) {
                    try {
                        widgetListItem.appIcon = mPackageManager.getApplicationIcon( widgetListItem.pkgName );
                    } catch ( PackageManager.NameNotFoundException ex ) {
                        widgetListItem.appIcon = ResourcesCompat.getDrawable( getResources(), android.R.drawable.sym_def_app_icon, null );
                    }
                }

                textPkgName.setText( widgetListItem.pkgName );
                textAppName.setText( widgetListItem.appName );
                imageAppIcon.setImageDrawable( widgetListItem.appIcon );
                switchIsEnabled.setChecked( widgetListItem.isAvailable );
            }

            return listItemView;
        }
    }

    private class LoadAppListTask extends AsyncTask< Void, Void, Void > {
        ProgressBar mProgressBar;

        @Override
        protected Void doInBackground( Void... voids ) {
            List< ApplicationInfo > appInfoList = mPackageManager.getInstalledApplications( 0 );
            String availableAppList = ";" + mSharedPreferences.getString( PREF_KEY_AVAILABLE_WIDGET_LIST, "" ) + ";";

            int appNum = appInfoList.size();
            int appCnt = 0;

            for ( ApplicationInfo appInfo : appInfoList ) {
                boolean isAvailable = availableAppList.contains( ";" + appInfo.packageName + ";" );

                mAppList.add( new WidgetListItem(
                        appInfo.packageName,
                        appInfo.loadLabel( mPackageManager ).toString(),
                        null, // appInfo.loadIcon( mPackageManager )
                        isAvailable
                ) );

                if ( isAvailable ) {
                    mAvailableWidgetList.put( appInfo.packageName, true );
                }

                mProgressBar.setProgress( 100 * ( ++appCnt ) / appNum );
            }

            Collections.sort( mAppList, new Comparator<WidgetListItem>() {
                @Override
                public int compare( WidgetListItem widgetListItem1, WidgetListItem widgetListItem2 ) {
                    if ( widgetListItem1.isAvailable == widgetListItem2.isAvailable ) {
                        return widgetListItem1.appName.compareTo( widgetListItem2.appName );
                    }
                    else {
                        return widgetListItem1.isAvailable ? -1: 1;
                    }
                }
            } );

            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressBar = findViewById( R.id.progress_bar );
        }

        @Override
        protected void onPostExecute( Void aVoid ) {
            super.onPostExecute( aVoid );

            final AppListAdapter adapter = new AppListAdapter();
            ListView listView = findViewById( R.id.widget_list_view );
            listView.setAdapter( adapter );
            listView.setOnItemClickListener( new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick( AdapterView< ? > adapterView, View view, int i, long l ) {
                    WidgetListItem widgetListItem = mAppList.get( i );
                    widgetListItem.isAvailable = !widgetListItem.isAvailable;
                    adapter.notifyDataSetChanged();

                    if ( widgetListItem.isAvailable ) {
                        mAvailableWidgetList.put( widgetListItem.pkgName, true );
                    }
                    else {
                        mAvailableWidgetList.remove( widgetListItem.pkgName );
                    }
                }
            } );

            mProgressBar.setVisibility( android.widget.ProgressBar.INVISIBLE );
        }

        @Override
        protected void onProgressUpdate( Void... values ) {
            super.onProgressUpdate( values );
        }
    }
}