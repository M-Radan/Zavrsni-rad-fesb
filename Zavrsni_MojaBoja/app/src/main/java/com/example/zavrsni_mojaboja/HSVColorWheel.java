package com.example.zavrsni_mojaboja;

//importi potrebni za HSVColorWheel klasu
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class HSVColorWheel extends View {
    private Paint paint;
    private Paint dot_paint;
    private float[] hsv = {0, 1, 1};
    private OnColorSelectedListener listener;
    private float dot_X = -1, dot_Y = -1;

    public interface OnColorSelectedListener {
        void onColorSelected(int color);
    }

    public HSVColorWheel(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);

        //dio koda koji se tiče točke koja se pokazuje nakon što odaberemo boju
        dot_paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dot_paint.setColor(Color.WHITE);
        dot_paint.setStrokeWidth(5);
    }

    public void setOnColorSelectedListener(OnColorSelectedListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        //float radius = Math.min(width, height) / 2f;

        // crtanje boja u krugu
        for (int i = 0; i < 360; i++) {
            hsv[0] = i;
            paint.setColor(Color.HSVToColor(hsv));
            canvas.drawArc(0, 0, width, height, i, 1, true, paint);
        }

        // crtamo točku ako je boja odabrana
        if (dot_X != -1 && dot_Y != -1) {
            canvas.drawCircle(dot_X, dot_Y, 10, dot_paint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            float x = event.getX();
            float y = event.getY();
            float centerX = getWidth() / 2f;
            float centerY = getHeight() / 2f;
            float radius = Math.min(centerX, centerY);

            float dx = x - centerX;
            float dy = y - centerY;
            float distance = (float) Math.sqrt(dx * dx + dy * dy);

            if (distance <= radius) {
                float angle = (float) Math.toDegrees(Math.atan2(dy, dx));
                if (angle < 0) {
                    angle=angle+360;
                }
                hsv[0] = angle;
                if (listener != null) {
                    listener.onColorSelected(Color.HSVToColor(hsv));
                }

                dot_X = x;
                dot_Y = y;
                invalidate();
                return true;
            }
        }
        return super.onTouchEvent(event);
    }
}


