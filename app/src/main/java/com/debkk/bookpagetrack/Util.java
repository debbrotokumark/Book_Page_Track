package com.debkk.bookpagetrack;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Consumer;

public class Util {
    static final String D = "০১২৩৪৫৬৭৮৯";
    static final SimpleDateFormat F = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    public static final Locale BN = new Locale("bn", "BD");

    public static String bn(Object n) {
        String s = String.valueOf(n);
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) sb.append(c >= '0' && c <= '9' ? D.charAt(c - '0') : c);
        return sb.toString();
    }

    public static String en(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            int i = D.indexOf(c);
            sb.append(i >= 0 ? (char) ('0' + i) : c);
        }
        return sb.toString();
    }

    public static int num(String s) {
        try { return Integer.parseInt(en(s.trim())); } catch (Exception e) { return 0; }
    }

    public static String today() { return F.format(new Date()); }
    public static String iso(Calendar c) { return F.format(c.getTime()); }

    public static String fd(String d) {
        if (d == null || d.isEmpty()) return "—";
        try { return new SimpleDateFormat("d MMM yyyy", BN).format(F.parse(d)); } catch (Exception e) { return d; }
    }

    public static long days(String a, String b) {
        try { return Math.round((F.parse(b).getTime() - F.parse(a).getTime()) / 86400000.0); } catch (Exception e) { return 0; }
    }

    public static int dp(Context c, int v) { return (int) (v * c.getResources().getDisplayMetrics().density); }

    public static int goal(Context c) { return c.getSharedPreferences("pn", 0).getInt("goal", 20); }
    public static void setGoal(Context c, int g) { c.getSharedPreferences("pn", 0).edit().putInt("goal", g).apply(); }

    public static int streak(Map<String, Integer> m) {
        Calendar c = Calendar.getInstance();
        if (m.getOrDefault(iso(c), 0) == 0) c.add(Calendar.DAY_OF_MONTH, -1);
        int n = 0;
        while (m.getOrDefault(iso(c), 0) > 0) { n++; c.add(Calendar.DAY_OF_MONTH, -1); }
        return n;
    }

    public static void toast(Context c, String m) { Toast.makeText(c, m, Toast.LENGTH_SHORT).show(); }

    public static void pickDate(Context c, EditText e) {
        Calendar cal = Calendar.getInstance();
        try { cal.setTime(F.parse(e.getText().toString())); } catch (Exception ignored) { }
        new DatePickerDialog(c, (v, y, m, d) -> {
            Calendar x = Calendar.getInstance();
            x.set(y, m, d);
            e.setText(iso(x));
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    public static void confirm(Context c, String msg, Runnable yes) {
        new AlertDialog.Builder(c).setMessage(msg)
                .setPositiveButton("হ্যাঁ", (d, w) -> yes.run())
                .setNegativeButton("না", null).show();
    }

    public static void prompt(Context c, String title, String[] hints, String[] init, boolean[] date, Consumer<String[]> ok) {
        LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(c, 20), dp(c, 8), dp(c, 20), 0);
        EditText[] ed = new EditText[hints.length];
        for (int i = 0; i < hints.length; i++) {
            final EditText e = new EditText(c);
            e.setHint(hints[i]);
            e.setText(init[i]);
            if (date[i]) { e.setFocusable(false); e.setOnClickListener(v -> pickDate(c, e)); }
            box.addView(e, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            ed[i] = e;
        }
        new AlertDialog.Builder(c).setTitle(title).setView(box)
                .setPositiveButton("আপডেট", (d, w) -> {
                    String[] v = new String[ed.length];
                    for (int i = 0; i < v.length; i++) v[i] = ed[i].getText().toString().trim();
                    ok.accept(v);
                })
                .setNegativeButton("বাতিল", null).show();
    }

    public static String readAll(InputStream in) throws IOException {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
        return bo.toString("UTF-8");
    }
}
