package android.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Vibrator;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.example.daniel.butterbot.R;
import com.example.daniel.butterbot.SplashActivity;

import static android.content.Context.VIBRATOR_SERVICE;

public class VerticalSeekBar extends SeekBar {
    private Context context;
    private Vibrator vib;
    public VerticalSeekBar(Context context) {
        super(context);
        this.context = context;
        vib = (Vibrator)context.getSystemService(VIBRATOR_SERVICE);

    }

    public VerticalSeekBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;
        vib = (Vibrator)context.getSystemService(VIBRATOR_SERVICE);

    }

    public VerticalSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        vib = (Vibrator)context.getSystemService(VIBRATOR_SERVICE);
    }

    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(h, w, oldh, oldw);
    }

    @Override
    protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(heightMeasureSpec, widthMeasureSpec);
        setMeasuredDimension(getMeasuredHeight(), getMeasuredWidth());
    }

    protected void onDraw(Canvas c) {
        c.rotate(-90);
        c.translate(-getHeight(), 0);

        super.onDraw(c);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            return false;
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                vib.vibrate(20);

                setThumb(ContextCompat.getDrawable(context, R.drawable.thumb_down));
                setProgress(getMax() - (int) (getMax() * event.getY() / getHeight()));
                onSizeChanged(getWidth(), getHeight(), 0, 0);
                break;

            case MotionEvent.ACTION_MOVE:
                setProgress(getMax() - (int) (getMax() * event.getY() / getHeight()));
                onSizeChanged(getWidth(), getHeight(), 0, 0);
                break;
            case MotionEvent.ACTION_UP:
                setThumb(ContextCompat.getDrawable(context, R.drawable.thumb_up));
                setProgress(getMax() - (int) (getMax() * event.getY() / getHeight()));
                onSizeChanged(getWidth(), getHeight(), 0, 0);
                break;

            case MotionEvent.ACTION_CANCEL:
                break;
        }
        return true;
    }
}