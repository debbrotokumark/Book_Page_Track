package com.debkk.bookpagetrack;

import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import java.util.*;

public class BookActivity extends AppCompatActivity {
    private long id;
    private Db db;
    private Db.Book b;
    private TextView title, sub, stats, unread, readTv, notes;
    private ProgressBar prog;
    private PageMapView map;
    private EditText rg, dt, bmP, bmT, tgT, tgD, qtT, qtP;
    private LinearLayout histBox, bmBox, tgBox, qtBox;
    private Button resetHist;

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_book);
        id = getIntent().getLongExtra("id", 0);
        db = Db.get(this);
        title = findViewById(R.id.title); sub = findViewById(R.id.sub); stats = findViewById(R.id.stats);
        unread = findViewById(R.id.unread); readTv = findViewById(R.id.readTv); notes = findViewById(R.id.notes);
        prog = findViewById(R.id.prog); map = findViewById(R.id.map);
        rg = findViewById(R.id.rg); dt = findViewById(R.id.dt);
        bmP = findViewById(R.id.bm_p); bmT = findViewById(R.id.bm_t);
        tgT = findViewById(R.id.tg_t); tgD = findViewById(R.id.tg_d);
        qtT = findViewById(R.id.qt_t); qtP = findViewById(R.id.qt_p);
        histBox = findViewById(R.id.hist_box); bmBox = findViewById(R.id.bm_box);
        tgBox = findViewById(R.id.tg_box); qtBox = findViewById(R.id.qt_box);
        resetHist = findViewById(R.id.reset_hist);

        dt.setText(Util.today());
        dt.setOnClickListener(v -> Util.pickDate(this, dt));
        tgD.setOnClickListener(v -> Util.pickDate(this, tgD));

        findViewById(R.id.back).setOnClickListener(v -> finish());
        findViewById(R.id.edit).setOnClickListener(v -> BookDialog.show(this, b, this::reload));
        findViewById(R.id.del).setOnClickListener(v ->
                Util.confirm(this, "“" + b.name + "” মুছে ফেলবেন? এটি ফেরানো যাবে না।", () -> { db.deleteBook(id); finish(); }));
        findViewById(R.id.read).setOnClickListener(v -> log(true));
        findViewById(R.id.unmark).setOnClickListener(v -> log(false));
        resetHist.setOnClickListener(v ->
                Util.confirm(this, "এই বইয়ের সব পড়ার হিসাব মুছে যাবে। নিশ্চিত?", () -> { db.resetBook(id); reload(); }));

        map.cb = (p, was) -> {
            List<int[]> q = new ArrayList<>();
            q.add(new int[]{p, p});
            if (was) db.unmark(id, q); else db.addRead(id, q, Util.today());
            reload();
        };

        findViewById(R.id.bm_add).setOnClickListener(v -> {
            int p = Util.num(bmP.getText().toString());
            if (p < 1 || p > b.total) { Util.toast(this, "পৃষ্ঠা ১ থেকে " + Util.bn(b.total) + " এর মধ্যে দিন"); return; }
            db.addItem(id, "bm", bmT.getText().toString().trim(), p, "");
            bmP.setText(""); bmT.setText("");
            reload();
        });
        findViewById(R.id.tg_add).setOnClickListener(v -> {
            String t = tgT.getText().toString().trim();
            if (t.isEmpty()) { Util.toast(this, "টার্গেট লিখুন"); return; }
            db.addItem(id, "tg", t, 0, tgD.getText().toString().trim());
            tgT.setText(""); tgD.setText("");
            reload();
        });
        findViewById(R.id.qt_add).setOnClickListener(v -> {
            String t = qtT.getText().toString().trim();
            if (t.isEmpty()) { Util.toast(this, "উক্তি লিখুন"); return; }
            db.addItem(id, "qt", t, Util.num(qtP.getText().toString()), "");
            qtT.setText(""); qtP.setText("");
            reload();
        });
    }

    @Override protected void onResume() { super.onResume(); reload(); }

    private void log(boolean add) {
        try {
            List<int[]> q = Ranges.parseInput(rg.getText().toString(), b.total);
            if (add) {
                String d = dt.getText().toString().trim();
                if (d.isEmpty()) d = Util.today();
                int n = db.addRead(id, q, d);
                Util.toast(this, n > 0 ? Util.bn(n) + " নতুন পৃষ্ঠা যোগ হয়েছে" : "এই পৃষ্ঠাগুলো আগেই পড়া ছিল");
            } else {
                Util.toast(this, Util.bn(db.unmark(id, q)) + " পৃষ্ঠা না-পড়া করা হয়েছে");
            }
            rg.setText("");
            reload();
        } catch (IllegalArgumentException e) {
            Util.toast(this, e.getMessage());
        }
    }

    private void row(LinearLayout box, String text, Boolean chk, CompoundButton.OnCheckedChangeListener cl, Runnable ed, Runnable del) {
        View v = getLayoutInflater().inflate(R.layout.item_row, box, false);
        TextView t = v.findViewById(R.id.r_txt);
        t.setText(text);
        CheckBox c = v.findViewById(R.id.r_chk);
        if (chk != null) {
            c.setVisibility(View.VISIBLE);
            c.setChecked(chk);
            c.setOnCheckedChangeListener(cl);
            if (chk) t.setPaintFlags(t.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        }
        v.findViewById(R.id.r_edit).setOnClickListener(x -> ed.run());
        v.findViewById(R.id.r_del).setOnClickListener(x -> del.run());
        box.addView(v);
    }

    private void reload() {
        b = db.getBook(id);
        if (b == null) { finish(); return; }
        List<Db.Hist> hs = db.getHist(id);
        int rd = b.read(), pc = Math.round(rd * 100f / b.total);
        String today = Util.today();

        title.setText(b.name);
        sub.setText(b.author + (b.cat.isEmpty() ? "" : "  •  " + b.cat));
        prog.setProgress(pc);

        String first = null;
        for (Db.Hist h : hs) if (first == null || h.date.compareTo(first) < 0) first = h.date;
        String avg = "—", fin = "—";
        if (rd >= b.total) fin = "শেষ হয়েছে";
        else if (first != null) {
            long span = Math.max(1, Util.days(first, today) + 1);
            double a = rd / (double) span;
            avg = Util.bn(String.format(Locale.US, "%.1f", a));
            if (a > 0) {
                Calendar c = Calendar.getInstance();
                c.add(Calendar.DAY_OF_MONTH, (int) Math.ceil((b.total - rd) / a));
                fin = Util.fd(Util.iso(c));
            }
        }
        boolean late = !b.target.isEmpty() && b.target.compareTo(today) < 0 && rd < b.total;
        stats.setText("পড়া হয়েছে: " + Util.bn(rd) + " পৃষ্ঠা   |   বাকি: " + Util.bn(b.total - rd) + " পৃষ্ঠা\n"
                + "অগ্রগতি: " + Util.bn(pc) + "%   |   গড় প্রতিদিন: " + avg + "\n"
                + "সম্ভাব্য শেষ: " + fin
                + (b.start.isEmpty() ? "" : "\nশুরু: " + Util.fd(b.start))
                + (b.target.isEmpty() ? "" : "\nটার্গেট: " + Util.fd(b.target) + (late ? " (সময় পেরিয়েছে)" : "")));
        stats.setTextColor(late ? Color.parseColor("#B3433B") : unread.getTextColors().getDefaultColor());

        List<Db.Item> bms = db.items(id, "bm"), tgs = db.items(id, "tg"), qts = db.items(id, "qt");
        Set<Integer> bmSet = new HashSet<>();
        for (Db.Item i : bms) bmSet.add(i.p);
        map.set(b.total, b.r, bmSet);

        unread.setText("না-পড়া: " + Ranges.fmt(Ranges.gaps(b.r, b.total)));
        readTv.setText("পড়া: " + Ranges.fmt(b.r));
        notes.setText(b.notes.isEmpty() ? "" : "নোট: " + b.notes);
        notes.setVisibility(b.notes.isEmpty() ? View.GONE : View.VISIBLE);

        // history
        histBox.removeAllViews();
        resetHist.setVisibility(hs.isEmpty() ? View.GONE : View.VISIBLE);
        if (hs.isEmpty()) {
            TextView e = new TextView(this);
            e.setText("এখনো কোনো এন্ট্রি নেই।");
            histBox.addView(e);
        }
        for (Db.Hist h : hs) {
            row(histBox, Util.fd(h.date) + "  পৃ. " + Ranges.fmt(h.q) + "  (" + Util.bn(Ranges.count(h.nw)) + " নতুন)", null, null,
                    () -> Util.prompt(this, "পড়ার এন্ট্রি সম্পাদনা", new String[]{"তারিখ", "পৃষ্ঠা (যেমন ২৫-৩১, ৪০)"},
                            new String[]{h.date, Ranges.fmt(h.q).replace("–", "-")}, new boolean[]{true, false}, v -> {
                                try {
                                    List<int[]> q = Ranges.parseInput(v[1], b.total);
                                    db.deleteHist(h.id);
                                    db.addRead(id, q, v[0].isEmpty() ? Util.today() : v[0]);
                                    reload();
                                } catch (IllegalArgumentException e) { Util.toast(this, e.getMessage()); }
                            }),
                    () -> Util.confirm(this, "এই এন্ট্রি মুছলে এর পৃষ্ঠাগুলো আবার না-পড়া হবে। নিশ্চিত?", () -> { db.deleteHist(h.id); reload(); }));
        }

        // bookmarks
        bmBox.removeAllViews();
        for (Db.Item i : bms)
            row(bmBox, "পৃ. " + Util.bn(i.p) + "  " + i.t, null, null,
                    () -> Util.prompt(this, "বুকমার্ক সম্পাদনা", new String[]{"পৃষ্ঠা", "নোট"},
                            new String[]{Util.bn(i.p), i.t}, new boolean[]{false, false}, v -> {
                                int p = Util.num(v[0]);
                                if (p < 1 || p > b.total) { Util.toast(this, "পৃষ্ঠা ১ থেকে " + Util.bn(b.total) + " এর মধ্যে দিন"); return; }
                                db.updateItem(i.id, v[1], p, "");
                                reload();
                            }),
                    () -> { db.delItem(i.id); reload(); });

        // targets
        tgBox.removeAllViews();
        for (Db.Item i : tgs) {
            boolean l = !i.d.isEmpty() && i.d.compareTo(today) < 0 && !i.done;
            row(tgBox, i.t + (i.d.isEmpty() ? "" : "  •  " + Util.fd(i.d) + (l ? " (সময় পেরিয়েছে)" : "")), i.done,
                    (cb, on) -> { db.setDone(i.id, on); reload(); },
                    () -> Util.prompt(this, "টার্গেট সম্পাদনা", new String[]{"টার্গেট", "তারিখ"},
                            new String[]{i.t, i.d}, new boolean[]{false, true}, v -> {
                                if (v[0].isEmpty()) return;
                                db.updateItem(i.id, v[0], 0, v[1]);
                                reload();
                            }),
                    () -> { db.delItem(i.id); reload(); });
        }

        // quotes
        qtBox.removeAllViews();
        for (Db.Item i : qts)
            row(qtBox, i.t + (i.p > 0 ? "  (পৃ. " + Util.bn(i.p) + ")" : ""), null, null,
                    () -> Util.prompt(this, "উক্তি সম্পাদনা", new String[]{"উক্তি / চিন্তা", "পৃষ্ঠা"},
                            new String[]{i.t, i.p > 0 ? Util.bn(i.p) : ""}, new boolean[]{false, false}, v -> {
                                if (v[0].isEmpty()) return;
                                db.updateItem(i.id, v[0], Util.num(v[1]), "");
                                reload();
                            }),
                    () -> { db.delItem(i.id); reload(); });
    }
}
