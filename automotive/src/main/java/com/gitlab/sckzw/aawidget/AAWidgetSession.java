package com.gitlab.sckzw.aawidget;

import android.content.Intent;
import android.content.res.Configuration;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.car.app.CarAppService;
import androidx.car.app.CarContext;
import androidx.car.app.CarToast;
import androidx.car.app.Screen;
import androidx.car.app.Session;

import java.util.Objects;

public class AAWidgetSession extends Session {
    private final Class< ? extends CarAppService > mCarAppServiceClass;

    public AAWidgetSession( Class< ? extends CarAppService > carAppServiceClass ) {
        super();
        mCarAppServiceClass = carAppServiceClass;
    }

    @NonNull
    @Override
    public Screen onCreateScreen( @NonNull Intent intent ) {
        return new AAWidgetScreen( getCarContext(), mCarAppServiceClass );
    }

    @Override
    public void onCarConfigurationChanged( @NonNull Configuration newConfiguration ) {
        if ( getCarContext().isDarkMode() ) {
            AppCompatDelegate.setDefaultNightMode( AppCompatDelegate.MODE_NIGHT_YES );
        }
        else {
            AppCompatDelegate.setDefaultNightMode( AppCompatDelegate.MODE_NIGHT_NO );
        }
    }

    @Override
    public void onNewIntent( @NonNull Intent intent ) {
        super.onNewIntent( intent );
        if ( Objects.equals( intent.getAction(), CarContext.ACTION_NAVIGATE ) ) {
            CarToast.makeText( getCarContext(), R.string.switch_to_google_maps_to_navigate, CarToast.LENGTH_LONG ).show();
        }
    }
}
