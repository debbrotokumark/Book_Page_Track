package com.debkk.bookpagetrack;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.google.android.material.color.MaterialColors;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PageMapView extends View {
    public interface OnPage { void on(int page, boolean wasRead); }

    private int total = 1, c = 10, cols = 1;
    private boolean[] read = new boolean[2];
    private Set<Integer> bm = new HashSet<>();
    private final Paint paint = new Paint();
    public OnPage cb;

    public PageMapView(Context ctx, AttributeSet a) { super(ctx, a); }

    public void set(int total, List<int[]> r, Set<Integer> bm) {
        this.total = Math.max(1, total);
        read = new boolean[this.total + 1];
        for (int[] x : r) for (int i = x[0]; i <= x[1] && i <= this.total; i++) read[i] = true;
        this.bm = bm;
        requestLayout();
        invalidate();
    }

    @Override protected void onMeasure(int ws, int hs) {
        int w = MeasureSpec.getSize(ws);
        float d = getResources().getDisplayMetrics().density;
        double cd = Math.max(6, Math.min(18, Math.floor(Math.sqrt((w / d) * 140.0 / total))));
        c = Math.max(1, (int) (cd * d));
        cols = Math.max(1, w / c);
        int rows = (int) Math.ceil(total / (double) cols);
        setMeasuredDimension(w, rows * c);
    }

    @Override protected void onDraw(Canvas cv) {
        int on = MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimary, 0xFF1E6B73);
        int off = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOutlineVariant, 0xFFD3DAD5);
        int gold = 0xFFB8861F;
        for (int i = 1; i <= total; i++) {
            int x = ((i - 1) % cols) * c, y = ((i - 1) / cols) * c;
            paint.setColor(bm.contains(i) ? gold : read[i] ? on : off);
            cv.drawRect(x + 1, y + 1, x + c - 1, y + c - 1, paint);
        }
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_DOWN) return true;
        if (e.getAction() == MotionEvent.ACTION_UP) {
            int p = (int) (e.getY() / c) * cols + (int) (e.getX() / c) + 1;
            if (p >= 1 && p <= total && cb != null) cb.on(p, read[p]);
            performClick();
            return true;
        }
        return super.onTouchEvent(e);
    }

    @Override public boolean performClick() { return super.performClick(); }
}
