package com.gitlab.sckzw.aawidget;

import android.appwidget.AppWidgetHostView;
import android.content.Context;
import android.graphics.Canvas;
import android.view.MotionEvent;

public class AAWidgetHostView extends AppWidgetHostView {
    private float mScale = 1.0f;

    public AAWidgetHostView( Context context ) {
        super( context );
        //setClipChildren( false );
        //setClipToPadding( false );
    }

    @Override
    protected void dispatchDraw( Canvas canvas ) {
        canvas.save();
        float pivotX = getWidth() / 2.0f;
        float pivotY = getHeight() / 2.0f;
        //canvas.scale( mScale, mScale, pivotX, pivotY );
        canvas.scale( mScale, mScale, 0, 0 );
        super.dispatchDraw( canvas );
        canvas.restore();
    }

    @Override
    protected void onMeasure( int widthMeasureSpec, int heightMeasureSpec ) {
        int parentWidth = MeasureSpec.getSize( widthMeasureSpec );
        int parentHeight = MeasureSpec.getSize( heightMeasureSpec );

        int childWidth = (int)( parentWidth / mScale );
        int childHeight = (int)( parentHeight / mScale );

        int childWidthSpec = MeasureSpec.makeMeasureSpec( childWidth, MeasureSpec.EXACTLY );
        int childHeightSpec = MeasureSpec.makeMeasureSpec( childHeight, MeasureSpec.EXACTLY );

        super.onMeasure( childWidthSpec, childHeightSpec );

        setMeasuredDimension( parentWidth, parentHeight );
    }

    @Override
    protected void onLayout( boolean changed, int left, int top, int right, int bottom ) {
        int scaledWidth = (int)( ( right - left ) / mScale );
        int scaledHeight = (int)( ( bottom - top ) / mScale );

        super.onLayout( changed, 0, 0, scaledWidth, scaledHeight );
    }

    @Override
    public boolean dispatchTouchEvent( MotionEvent ev ) {
        ev.setLocation( ev.getX() / mScale, ev.getY() / mScale );
        return super.dispatchTouchEvent( ev );
    }

    public void setScaleFactor( float scale ) {
        this.mScale = scale;
        requestLayout();
        invalidate();
    }
}
