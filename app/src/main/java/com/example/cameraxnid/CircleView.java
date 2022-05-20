package com.example.cameraxnid;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class CircleView extends View {

    private static final String TAG = "CircleView";
    private Paint srcPaint;

    public CircleView(Context context) {
        this(context, null, 0);
    }

    public CircleView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CircleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();

        //int ratioConstraint = 20;
        int ratioConstraint = getWidth()/36;

        int widthRatio = 16 * ratioConstraint;
        int heightRatio = 9 * ratioConstraint;

        int leftX1 = getWidth()/2 - widthRatio;
        int topY1 = getHeight()/2 - heightRatio;
        int rightX2 = getWidth()/2 + widthRatio;;
        int bottomY2 = getHeight()/2 + heightRatio;

        //float centerLeft = leftX1 / 2;
        //float centerTop = topY1 / 2;
        //float centerRight = rightX2 / 2;
        //float centerBottom = bottomY2 / 2;

        Bitmap background = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        Canvas layer = new Canvas(background);
       // layer.drawColor(getResources().getColor(R.color.camera_rect_bg_color));
        layer.drawColor(getResources().getColor(R.color.camera_rect_bg_color));
        RectF rectF = new RectF(leftX1,topY1,rightX2,bottomY2);
        layer.drawRect(rectF, srcPaint);

        canvas.drawBitmap(background, 0, 0, null);
    }

    private void init() {
        srcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        srcPaint.setColor(Color.WHITE);
        srcPaint.setStyle(Paint.Style.FILL);
        srcPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
    }
}
