package com.debkk.bookpagetrack;

import java.util.*;
import java.util.regex.*;

public class Ranges {
    public static List<int[]> parseStored(String s) {
        List<int[]> o = new ArrayList<>();
        if (s == null || s.isEmpty()) return o;
        for (String p : s.split(",")) {
            String[] a = p.split("-");
            if (a.length == 2) o.add(new int[]{Integer.parseInt(a[0]), Integer.parseInt(a[1])});
        }
        return o;
    }

    public static String store(List<int[]> r) {
        StringBuilder sb = new StringBuilder();
        for (int[] x : r) {
            if (sb.length() > 0) sb.append(',');
            sb.append(x[0]).append('-').append(x[1]);
        }
        return sb.toString();
    }

    public static List<int[]> norm(List<int[]> in) {
        List<int[]> r = new ArrayList<>();
        for (int[] x : in) r.add(new int[]{x[0], x[1]});
        Collections.sort(r, (a, b) -> Integer.compare(a[0], b[0]));
        List<int[]> o = new ArrayList<>();
        for (int[] x : r) {
            int[] l = o.isEmpty() ? null : o.get(o.size() - 1);
            if (l != null && x[0] <= l[1] + 1) l[1] = Math.max(l[1], x[1]);
            else o.add(x);
        }
        return o;
    }

    public static int count(List<int[]> r) {
        int s = 0;
        for (int[] x : r) s += x[1] - x[0] + 1;
        return s;
    }

    public static List<int[]> gaps(List<int[]> r, int t) {
        List<int[]> o = new ArrayList<>();
        int p = 1;
        for (int[] x : r) {
            if (x[0] > p) o.add(new int[]{p, x[0] - 1});
            p = x[1] + 1;
        }
        if (p <= t) o.add(new int[]{p, t});
        return o;
    }

    public static List<int[]> sub(List<int[]> r, int a, int b) {
        List<int[]> o = new ArrayList<>();
        for (int[] x : r) {
            if (x[1] < a || x[0] > b) o.add(x);
            else {
                if (x[0] < a) o.add(new int[]{x[0], a - 1});
                if (x[1] > b) o.add(new int[]{b + 1, x[1]});
            }
        }
        return o;
    }

    public static List<int[]> diff(List<int[]> a, List<int[]> b) {
        List<int[]> res = a;
        for (int[] y : b) res = sub(res, y[0], y[1]);
        return norm(res);
    }

    public static boolean has(List<int[]> r, int p) {
        for (int[] x : r) if (p >= x[0] && p <= x[1]) return true;
        return false;
    }

    public static String fmt(List<int[]> r) {
        if (r.isEmpty()) return "নেই";
        StringBuilder sb = new StringBuilder();
        for (int[] x : r) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(x[0] == x[1] ? Util.bn(x[0]) : Util.bn(x[0]) + "–" + Util.bn(x[1]));
        }
        return sb.toString();
    }

    /** Parses user input like "25-31, 40". Throws IllegalArgumentException with a Bangla message. */
    public static List<int[]> parseInput(String s, int total) {
        s = Util.en(s).replaceAll("\\s*[-–—~]+\\s*", "-");
        List<int[]> o = new ArrayList<>();
        Pattern pat = Pattern.compile("^(\\d+)(?:-(\\d+))?$");
        for (String p : s.split("[,،\\s]+")) {
            if (p.isEmpty()) continue;
            Matcher m = pat.matcher(p);
            if (!m.matches()) throw new IllegalArgumentException("ফরম্যাট ঠিক নয়। যেমন: ২৫-৩১, ৪০");
            int a, b;
            try {
                a = Integer.parseInt(m.group(1));
                b = m.group(2) != null ? Integer.parseInt(m.group(2)) : a;
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("ফরম্যাট ঠিক নয়। যেমন: ২৫-৩১, ৪০");
            }
            if (a > b) { int t = a; a = b; b = t; }
            if (a < 1 || b > total)
                throw new IllegalArgumentException("পৃষ্ঠা ১ থেকে " + Util.bn(total) + " এর মধ্যে হতে হবে");
            o.add(new int[]{a, b});
        }
        if (o.isEmpty()) throw new IllegalArgumentException("পৃষ্ঠার নম্বর লিখুন");
        return norm(o);
    }
}
