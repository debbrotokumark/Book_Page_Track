package com.debkk.bookpagetrack;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

public class Db extends SQLiteOpenHelper {
    private static Db inst;

    public static synchronized Db get(Context c) {
        if (inst == null) inst = new Db(c.getApplicationContext());
        return inst;
    }

    private Db(Context c) { super(c, "pagenest.db", null, 1); }

    public static class Book {
        public long id;
        public String name = "", author = "", cat = "", start = "", target = "", notes = "";
        public int total = 1;
        public List<int[]> r = new ArrayList<>();
        public int read() { return Ranges.count(r); }
    }

    public static class Hist { public long id; public String date; public List<int[]> q, nw; }

    public static class Item { public long id; public String kind, t = "", d = ""; public int p; public boolean done; }

    @Override public void onCreate(SQLiteDatabase d) {
        d.execSQL("CREATE TABLE books(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT,author TEXT,cat TEXT,total INTEGER,start TEXT,target TEXT,notes TEXT,ranges TEXT)");
        d.execSQL("CREATE TABLE hist(id INTEGER PRIMARY KEY AUTOINCREMENT,book_id INTEGER,date TEXT,q TEXT,nw TEXT)");
        d.execSQL("CREATE TABLE items(id INTEGER PRIMARY KEY AUTOINCREMENT,book_id INTEGER,kind TEXT,t TEXT,p INTEGER,d TEXT,done INTEGER DEFAULT 0)");
    }

    @Override public void onUpgrade(SQLiteDatabase d, int o, int n) { }

    private static String s(Cursor c, int i) { String x = c.getString(i); return x == null ? "" : x; }

    private Book book(Cursor c) {
        Book b = new Book();
        b.id = c.getLong(0); b.name = s(c, 1); b.author = s(c, 2); b.cat = s(c, 3);
        b.total = c.getInt(4); b.start = s(c, 5); b.target = s(c, 6); b.notes = s(c, 7);
        b.r = Ranges.parseStored(s(c, 8));
        return b;
    }

    public List<Book> getBooks() {
        List<Book> l = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery("SELECT * FROM books ORDER BY id DESC", null);
        while (c.moveToNext()) l.add(book(c));
        c.close();
        return l;
    }

    public Book getBook(long id) {
        Cursor c = getReadableDatabase().rawQuery("SELECT * FROM books WHERE id=?", new String[]{"" + id});
        Book b = c.moveToFirst() ? book(c) : null;
        c.close();
        return b;
    }

    private ContentValues cv(Book b) {
        ContentValues v = new ContentValues();
        v.put("name", b.name); v.put("author", b.author); v.put("cat", b.cat); v.put("total", b.total);
        v.put("start", b.start); v.put("target", b.target); v.put("notes", b.notes);
        v.put("ranges", Ranges.store(b.r));
        return v;
    }

    public long saveBook(Book b) {
        SQLiteDatabase d = getWritableDatabase();
        if (b.id == 0) {
            b.id = d.insert("books", null, cv(b));
        } else {
            b.r = Ranges.norm(Ranges.sub(b.r, b.total + 1, Integer.MAX_VALUE));
            d.update("books", cv(b), "id=?", new String[]{"" + b.id});
            d.delete("items", "book_id=? AND kind='bm' AND p>?", new String[]{"" + b.id, "" + b.total});
        }
        return b.id;
    }

    public void deleteBook(long id) {
        SQLiteDatabase d = getWritableDatabase();
        String[] a = {"" + id};
        d.delete("hist", "book_id=?", a);
        d.delete("items", "book_id=?", a);
        d.delete("books", "id=?", a);
    }

    private void setRanges(long id, List<int[]> r) {
        ContentValues v = new ContentValues();
        v.put("ranges", Ranges.store(r));
        getWritableDatabase().update("books", v, "id=?", new String[]{"" + id});
    }

    public void resetBook(long id) {
        getWritableDatabase().delete("hist", "book_id=?", new String[]{"" + id});
        setRanges(id, new ArrayList<>());
    }

    // ---------- reading log ----------
    public List<Hist> getHist(long bookId) {
        List<Hist> l = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery("SELECT id,date,q,nw FROM hist WHERE book_id=? ORDER BY date DESC,id DESC", new String[]{"" + bookId});
        while (c.moveToNext()) {
            Hist h = new Hist();
            h.id = c.getLong(0); h.date = s(c, 1);
            h.q = Ranges.parseStored(s(c, 2)); h.nw = Ranges.parseStored(s(c, 3));
            l.add(h);
        }
        c.close();
        return l;
    }

    public int addRead(long id, List<int[]> q, String date) {
        Book b = getBook(id);
        if (b == null) return 0;
        List<int[]> nw = Ranges.diff(q, b.r);
        int n = Ranges.count(nw);
        if (n > 0) {
            ContentValues v = new ContentValues();
            v.put("book_id", id); v.put("date", date);
            v.put("q", Ranges.store(q)); v.put("nw", Ranges.store(nw));
            getWritableDatabase().insert("hist", null, v);
            List<int[]> all = new ArrayList<>(b.r);
            all.addAll(q);
            setRanges(id, Ranges.norm(all));
        }
        return n;
    }

    public int unmark(long id, List<int[]> q) {
        Book b = getBook(id);
        if (b == null) return 0;
        int before = Ranges.count(b.r);
        List<int[]> r = Ranges.diff(b.r, q);
        setRanges(id, r);
        SQLiteDatabase d = getWritableDatabase();
        for (Hist h : getHist(id)) {
            List<int[]> nw = Ranges.diff(h.nw, q);
            if (Ranges.count(nw) == Ranges.count(h.nw)) continue;
            if (nw.isEmpty()) d.delete("hist", "id=?", new String[]{"" + h.id});
            else {
                ContentValues v = new ContentValues();
                v.put("nw", Ranges.store(nw));
                d.update("hist", v, "id=?", new String[]{"" + h.id});
            }
        }
        return before - Ranges.count(r);
    }

    public void deleteHist(long hid) {
        SQLiteDatabase d = getWritableDatabase();
        Cursor c = d.rawQuery("SELECT book_id,nw FROM hist WHERE id=?", new String[]{"" + hid});
        if (c.moveToFirst()) {
            long bid = c.getLong(0);
            List<int[]> nw = Ranges.parseStored(s(c, 1));
            c.close();
            Book b = getBook(bid);
            if (b != null) setRanges(bid, Ranges.diff(b.r, nw));
            d.delete("hist", "id=?", new String[]{"" + hid});
        } else c.close();
    }

    public Map<String, Integer> daily() {
        Map<String, Integer> m = new HashMap<>();
        Cursor c = getReadableDatabase().rawQuery("SELECT date,nw FROM hist", null);
        while (c.moveToNext()) m.merge(s(c, 0), Ranges.count(Ranges.parseStored(s(c, 1))), Integer::sum);
        c.close();
        return m;
    }

    /** rows: {bookName, firstDate, lastDate} */
    public List<String[]> spans() {
        List<String[]> l = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery("SELECT b.name,MIN(h.date),MAX(h.date) FROM hist h JOIN books b ON b.id=h.book_id GROUP BY h.book_id", null);
        while (c.moveToNext()) l.add(new String[]{s(c, 0), s(c, 1), s(c, 2)});
        c.close();
        return l;
    }

    // ---------- bookmarks / targets / quotes ----------
    public List<Item> items(long bookId, String kind) {
        List<Item> l = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery("SELECT id,kind,t,p,d,done FROM items WHERE book_id=? AND kind=? ORDER BY " + (kind.equals("bm") ? "p,id" : "id"), new String[]{"" + bookId, kind});
        while (c.moveToNext()) {
            Item i = new Item();
            i.id = c.getLong(0); i.kind = s(c, 1); i.t = s(c, 2); i.p = c.getInt(3); i.d = s(c, 4); i.done = c.getInt(5) == 1;
            l.add(i);
        }
        c.close();
        return l;
    }

    public void addItem(long bookId, String kind, String t, int p, String d) {
        ContentValues v = new ContentValues();
        v.put("book_id", bookId); v.put("kind", kind); v.put("t", t); v.put("p", p); v.put("d", d); v.put("done", 0);
        getWritableDatabase().insert("items", null, v);
    }

    public void updateItem(long id, String t, int p, String d) {
        ContentValues v = new ContentValues();
        v.put("t", t); v.put("p", p); v.put("d", d);
        getWritableDatabase().update("items", v, "id=?", new String[]{"" + id});
    }

    public void setDone(long id, boolean done) {
        ContentValues v = new ContentValues();
        v.put("done", done ? 1 : 0);
        getWritableDatabase().update("items", v, "id=?", new String[]{"" + id});
    }

    public void delItem(long id) { getWritableDatabase().delete("items", "id=?", new String[]{"" + id}); }

    // ---------- backup ----------
    public void resetAll() {
        SQLiteDatabase d = getWritableDatabase();
        d.delete("hist", null, null); d.delete("items", null, null); d.delete("books", null, null);
    }

    public String export(Context ctx) throws JSONException {
        JSONObject root = new JSONObject();
        JSONArray bs = new JSONArray();
        for (Book b : getBooks()) {
            JSONObject o = new JSONObject();
            o.put("id", b.id); o.put("name", b.name); o.put("author", b.author); o.put("cat", b.cat);
            o.put("total", b.total); o.put("start", b.start); o.put("target", b.target); o.put("notes", b.notes);
            o.put("r", Ranges.store(b.r));
            JSONArray hs = new JSONArray();
            for (Hist h : getHist(b.id)) {
                JSONObject j = new JSONObject();
                j.put("id", h.id); j.put("date", h.date); j.put("q", Ranges.store(h.q)); j.put("nw", Ranges.store(h.nw));
                hs.put(j);
            }
            o.put("hist", hs);
            JSONArray its = new JSONArray();
            for (String k : new String[]{"bm", "tg", "qt"})
                for (Item i : items(b.id, k)) {
                    JSONObject j = new JSONObject();
                    j.put("id", i.id); j.put("kind", i.kind); j.put("t", i.t); j.put("p", i.p); j.put("d", i.d); j.put("done", i.done ? 1 : 0);
                    its.put(j);
                }
            o.put("items", its);
            bs.put(o);
        }
        root.put("books", bs);
        root.put("goal", Util.goal(ctx));
        return root.toString(1);
    }

    /** Merge import. Returns {added, updated}. */
    public int[] importJson(String s) throws JSONException {
        JSONObject root = new JSONObject(s);
        JSONArray bs = root.getJSONArray("books");
        int added = 0, upd = 0;
        SQLiteDatabase d = getWritableDatabase();
        for (int i = 0; i < bs.length(); i++) {
            JSONObject o = bs.getJSONObject(i);
            long id = o.getLong("id");
            ContentValues v = new ContentValues();
            v.put("id", id); v.put("name", o.optString("name", "নামহীন")); v.put("author", o.optString("author"));
            v.put("cat", o.optString("cat")); v.put("total", Math.max(1, o.optInt("total", 1)));
            v.put("start", o.optString("start")); v.put("target", o.optString("target")); v.put("notes", o.optString("notes"));
            v.put("ranges", o.optString("r"));
            long rid = d.insertWithOnConflict("books", null, v, SQLiteDatabase.CONFLICT_IGNORE);
            if (rid == -1) {
                Book b = getBook(id);
                List<int[]> all = new ArrayList<>(b.r);
                all.addAll(Ranges.parseStored(o.optString("r")));
                setRanges(id, Ranges.norm(all));
                upd++;
            } else added++;
            JSONArray hs = o.optJSONArray("hist");
            if (hs != null) for (int k = 0; k < hs.length(); k++) {
                JSONObject j = hs.getJSONObject(k);
                ContentValues h = new ContentValues();
                h.put("id", j.getLong("id")); h.put("book_id", id); h.put("date", j.optString("date"));
                h.put("q", j.optString("q")); h.put("nw", j.optString("nw"));
                d.insertWithOnConflict("hist", null, h, SQLiteDatabase.CONFLICT_IGNORE);
            }
            JSONArray its = o.optJSONArray("items");
            if (its != null) for (int k = 0; k < its.length(); k++) {
                JSONObject j = its.getJSONObject(k);
                ContentValues h = new ContentValues();
                h.put("id", j.getLong("id")); h.put("book_id", id); h.put("kind", j.optString("kind"));
                h.put("t", j.optString("t")); h.put("p", j.optInt("p")); h.put("d", j.optString("d")); h.put("done", j.optInt("done"));
                d.insertWithOnConflict("items", null, h, SQLiteDatabase.CONFLICT_IGNORE);
            }
        }
        return new int[]{added, upd};
    }
}
