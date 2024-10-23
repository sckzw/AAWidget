package com.gitlab.sckzw.aawidget;

import android.annotation.SuppressLint;
import android.content.pm.ApplicationInfo;

import androidx.annotation.NonNull;
import androidx.car.app.CarAppService;
import androidx.car.app.Session;
import androidx.car.app.SessionInfo;
import androidx.car.app.validation.HostValidator;

public class AAWidgetCarAppService2 extends CarAppService {
    @NonNull
    @Override
    public Session onCreateSession( @NonNull SessionInfo sessionInfo ) {
        return new AAWidgetSession( AAWidgetCarAppService2.class );
    }

    @SuppressLint("PrivateResource")
    @NonNull
    @Override
    public HostValidator createHostValidator() {
        if ( ( getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0 ) {
            return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR;
        }
        else {
            return new HostValidator.Builder( getApplicationContext() )
                    .addAllowedHosts( androidx.car.app.R.array.hosts_allowlist_sample )
                    .build();
        }
    }
}
