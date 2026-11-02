package com.debkk.bookpagetrack;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.text.SimpleDateFormat;
import java.util.*;

public class AnalyticsFragment extends Fragment {
    private LinearLayout box;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle s) {
        View v = inf.inflate(R.layout.frag_analytics, c, false);
        box = v.findViewById(R.id.box);
        return v;
    }

    @Override public void onResume() { super.onResume(); build(); }

    private void text(String t, boolean head) {
        Context ctx = requireContext();
        TextView tv = new TextView(ctx);
        tv.setText(t);
        if (head) {
            tv.setTextSize(16);
            tv.setTypeface(null, Typeface.BOLD);
            tv.setPadding(0, Util.dp(ctx, 18), 0, Util.dp(ctx, 6));
        }
        box.addView(tv);
    }

    private void bars(List<Object[]> l, String unit) {
        if (l.isEmpty()) { text("এখনো ডেটা নেই।", false); return; }
        int mx = 1;
        for (Object[] x : l) mx = Math.max(mx, (Integer) x[1]);
        for (Object[] x : l) {
            View v = getLayoutInflater().inflate(R.layout.item_bar, box, false);
            ((TextView) v.findViewById(R.id.b_l)).setText((String) x[0]);
            ProgressBar p = v.findViewById(R.id.b_p);
            p.setMax(mx);
            p.setProgress((Integer) x[1]);
            ((TextView) v.findViewById(R.id.b_v)).setText(Util.bn(x[1]) + " " + unit);
            box.addView(v);
        }
    }

    private int sum(Map<String, Integer> m, int n) {
        int s = 0;
        Calendar c = Calendar.getInstance();
        for (int i = 0; i < n; i++) { s += m.getOrDefault(Util.iso(c), 0); c.add(Calendar.DAY_OF_MONTH, -1); }
        return s;
    }

    private void build() {
        Db db = Db.get(requireContext());
        box.removeAllViews();
        Map<String, Integer> m = db.daily();
        List<Db.Book> bs = db.getBooks();

        int all = 0, fin = 0;
        Map<String, Integer> cat = new HashMap<>();
        for (Db.Book b : bs) {
            int r = b.read();
            all += r;
            if (r >= b.total) fin++;
            String k = b.cat.isEmpty() ? "অন্যান্য" : b.cat;
            cat.put(k, cat.getOrDefault(k, 0) + r);
        }
        text("মোট পড়া: " + Util.bn(all) + " পৃষ্ঠা\nশেষ করা বই: " + Util.bn(fin) + "/" + Util.bn(bs.size())
                + "\nগত ৭ দিন: " + Util.bn(sum(m, 7)) + " পৃষ্ঠা\nগত ৩০ দিন: " + Util.bn(sum(m, 30))
                + " পৃষ্ঠা\nটানা পড়া: " + Util.bn(Util.streak(m)) + " দিন", false);

        text("মাসভিত্তিক পড়া (শেষ ৬ মাস)", true);
        List<Object[]> ms = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            Calendar c = Calendar.getInstance();
            c.set(Calendar.DAY_OF_MONTH, 1);
            c.add(Calendar.MONTH, -i);
            String key = Util.iso(c).substring(0, 7);
            int t = 0;
            for (Map.Entry<String, Integer> e : m.entrySet()) if (e.getKey().startsWith(key)) t += e.getValue();
            ms.add(new Object[]{new SimpleDateFormat("MMM", Util.BN).format(c.getTime()), t});
        }
        bars(ms, "পৃ.");

        text("বিভাগ অনুযায়ী পড়া", true);
        List<Object[]> cs = new ArrayList<>();
        for (Map.Entry<String, Integer> e : cat.entrySet()) cs.add(new Object[]{e.getKey(), e.getValue()});
        Collections.sort(cs, (a, b) -> (Integer) b[1] - (Integer) a[1]);
        bars(cs, "পৃ.");

        text("কোন বইয়ে বেশি সময় লেগেছে", true);
        List<Object[]> tm = new ArrayList<>();
        for (String[] sp : db.spans()) tm.add(new Object[]{sp[0], (int) Util.days(sp[1], sp[2]) + 1});
        Collections.sort(tm, (a, b) -> (Integer) b[1] - (Integer) a[1]);
        bars(tm.size() > 8 ? tm.subList(0, 8) : tm, "দিন");
    }
}
