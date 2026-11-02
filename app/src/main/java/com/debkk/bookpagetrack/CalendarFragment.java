package com.debkk.bookpagetrack;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;
import androidx.fragment.app.Fragment;

import com.google.android.material.color.MaterialColors;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;

public class CalendarFragment extends Fragment {
    private final Calendar cm = Calendar.getInstance();
    private GridLayout grid;
    private TextView month, sum;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle s) {
        View v = inf.inflate(R.layout.frag_calendar, c, false);
        grid = v.findViewById(R.id.grid); month = v.findViewById(R.id.month); sum = v.findViewById(R.id.sum);
        cm.set(Calendar.DAY_OF_MONTH, 1);
        v.findViewById(R.id.prev).setOnClickListener(x -> { cm.add(Calendar.MONTH, -1); draw(); });
        v.findViewById(R.id.next).setOnClickListener(x -> { cm.add(Calendar.MONTH, 1); draw(); });
        return v;
    }

    @Override public void onResume() { super.onResume(); draw(); }

    private TextView cell(Context ctx, String t, int h) {
        TextView tv = new TextView(ctx);
        tv.setText(t);
        tv.setGravity(Gravity.CENTER);
        tv.setTextSize(12);
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams(GridLayout.spec(GridLayout.UNDEFINED), GridLayout.spec(GridLayout.UNDEFINED, 1f));
        lp.width = 0;
        lp.height = h;
        lp.setMargins(2, 2, 2, 2);
        tv.setLayoutParams(lp);
        return tv;
    }

    private void draw() {
        Context ctx = requireContext();
        Map<String, Integer> m = Db.get(ctx).daily();
        int goal = Util.goal(ctx);
        grid.removeAllViews();
        month.setText(new SimpleDateFormat("MMMM yyyy", Util.BN).format(cm.getTime()));
        for (String d : new String[]{"রবি", "সোম", "মঙ্গল", "বুধ", "বৃহ", "শুক্র", "শনি"})
            grid.addView(cell(ctx, d, ViewGroup.LayoutParams.WRAP_CONTENT));

        int primary = MaterialColors.getColor(grid, com.google.android.material.R.attr.colorPrimary, 0xFF1E6B73);
        int onSurface = MaterialColors.getColor(grid, com.google.android.material.R.attr.colorOnSurface, Color.BLACK);
        int outline = MaterialColors.getColor(grid, com.google.android.material.R.attr.colorOutline, Color.GRAY);
        int first = cm.get(Calendar.DAY_OF_WEEK) - 1, n = cm.getActualMaximum(Calendar.DAY_OF_MONTH);
        for (int i = 0; i < first; i++) grid.addView(cell(ctx, "", Util.dp(ctx, 56)));

        int tot = 0, days = 0;
        String today = Util.today();
        for (int i = 1; i <= n; i++) {
            Calendar c = (Calendar) cm.clone();
            c.set(Calendar.DAY_OF_MONTH, i);
            String k = Util.iso(c);
            int v = m.getOrDefault(k, 0);
            tot += v;
            if (v > 0) days++;
            TextView tv = cell(ctx, Util.bn(i) + "\n" + (v > 0 ? Util.bn(v) : ""), Util.dp(ctx, 56));
            int alpha = v > 0 ? (int) (255 * Math.min(1f, .25f + v / (float) goal * .75f)) : 0;
            GradientDrawable g = new GradientDrawable();
            g.setCornerRadius(Util.dp(ctx, 8));
            g.setColor(v > 0 ? ColorUtils.setAlphaComponent(primary, alpha) : ColorUtils.setAlphaComponent(outline, 40));
            if (k.equals(today)) g.setStroke(Util.dp(ctx, 2), 0xFFB8861F);
            tv.setBackground(g);
            tv.setTextColor(alpha > 150 ? Color.WHITE : onSurface);
            grid.addView(tv);
        }
        sum.setText("এই মাসে " + Util.bn(tot) + " পৃষ্ঠা, " + Util.bn(days) + " দিন পড়েছেন। ঘর যত গাঢ়, দৈনিক লক্ষ্যের তত কাছে।");
    }
}
