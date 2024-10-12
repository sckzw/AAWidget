package com.gitlab.sckzw.aawidget;

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.car.app.CarToast;
import androidx.car.app.Screen;
import androidx.car.app.Session;

import java.util.Objects;

public class AAWidgetSession extends Session {
    @NonNull
    @Override
    public Screen onCreateScreen( @NonNull Intent intent ) {
        return new AAWidgetScreen( getCarContext() );
    }

    @Override
    public void onNewIntent( @NonNull Intent intent ) {
        super.onNewIntent( intent );
        if ( Objects.equals( intent.getAction(), CarContext.ACTION_NAVIGATE ) ) {
            CarToast.makeText( getCarContext(), R.string.switch_to_google_maps_to_navigate, CarToast.LENGTH_LONG ).show();
        }
    }
}
