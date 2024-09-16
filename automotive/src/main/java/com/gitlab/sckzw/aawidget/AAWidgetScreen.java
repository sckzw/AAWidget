package com.gitlab.sckzw.aawidget;

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.Pane;
import androidx.car.app.model.PaneTemplate;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;

public class AAWidgetScreen extends Screen {
    protected AAWidgetScreen( @NonNull CarContext carContext ) {
        super( carContext );
    }

    @NonNull
    @Override
    public Template onGetTemplate() {
        Row row = new Row.Builder()
                .setTitle( "AAWidget" )
                .build();
        Pane pane = new Pane.Builder()
                .addRow( row )
                .build();
        return new PaneTemplate.Builder( pane )
                .setHeaderAction( Action.APP_ICON )
                .build();
    }
}
