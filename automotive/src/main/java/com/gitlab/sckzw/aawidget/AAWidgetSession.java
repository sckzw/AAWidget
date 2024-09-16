package com.gitlab.sckzw.aawidget;

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.car.app.Screen;
import androidx.car.app.Session;

public class AAWidgetSession extends Session {
    @NonNull
    @Override
    public Screen onCreateScreen( @NonNull Intent intent ) {
        return new AAWidgetScreen( getCarContext() );
    }
}
