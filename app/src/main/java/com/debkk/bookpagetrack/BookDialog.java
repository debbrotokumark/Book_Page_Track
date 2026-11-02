package com.debkk.bookpagetrack;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class BookDialog {
    public static void show(Context c, Db.Book existing, Runnable done) {
        Db db = Db.get(c);
        View v = LayoutInflater.from(c).inflate(R.layout.dialog_book, null);
        EditText name = v.findViewById(R.id.f_name), author = v.findViewById(R.id.f_author), total = v.findViewById(R.id.f_total),
                start = v.findViewById(R.id.f_start), target = v.findViewById(R.id.f_target), notes = v.findViewById(R.id.f_notes);
        AutoCompleteTextView cat = v.findViewById(R.id.f_cat);
        Set<String> cs = new LinkedHashSet<>();
        for (Db.Book b : db.getBooks()) if (!b.cat.isEmpty()) cs.add(b.cat);
        cat.setAdapter(new ArrayAdapter<>(c, android.R.layout.simple_dropdown_item_1line, new ArrayList<>(cs)));
        start.setOnClickListener(x -> Util.pickDate(c, start));
        target.setOnClickListener(x -> Util.pickDate(c, target));
        if (existing != null) {
            name.setText(existing.name); author.setText(existing.author); cat.setText(existing.cat);
            total.setText(Util.bn(existing.total)); start.setText(existing.start); target.setText(existing.target);
            notes.setText(existing.notes);
        } else start.setText(Util.today());

        AlertDialog dlg = new AlertDialog.Builder(c).setTitle(existing == null ? "নতুন বই" : "বই সম্পাদনা")
                .setView(v).setPositiveButton("সেভ করুন", null).setNegativeButton("বাতিল", null).create();
        dlg.setOnShowListener(x -> dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(y -> {
            int t = Util.num(total.getText().toString());
            if (name.getText().toString().trim().isEmpty() || t < 1) {
                Util.toast(c, "বইয়ের নাম ও মোট পৃষ্ঠা দিন");
                return;
            }
            Db.Book b = existing != null ? existing : new Db.Book();
            b.name = name.getText().toString().trim();
            b.author = author.getText().toString().trim();
            b.cat = cat.getText().toString().trim();
            b.total = t;
            b.start = start.getText().toString().trim();
            b.target = target.getText().toString().trim();
            b.notes = notes.getText().toString().trim();
            db.saveBook(b);
            dlg.dismiss();
            Util.toast(c, "বই সেভ হয়েছে");
            if (done != null) done.run();
        }));
        dlg.show();
    }
}
