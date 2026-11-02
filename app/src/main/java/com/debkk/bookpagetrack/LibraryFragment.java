package com.debkk.bookpagetrack;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class LibraryFragment extends Fragment {
    private List<Db.Book> all = new ArrayList<>(), shown = new ArrayList<>();
    private List<String> cl = new ArrayList<>();
    private String cat = "";
    private EditText q;
    private Spinner cf;
    private TextView empty;
    private RecyclerView rv;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle s) {
        View v = inf.inflate(R.layout.frag_library, c, false);
        q = v.findViewById(R.id.q); cf = v.findViewById(R.id.cf); empty = v.findViewById(R.id.empty); rv = v.findViewById(R.id.rv);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(new Adapter());
        q.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence a, int b, int c2, int d) { }
            public void onTextChanged(CharSequence a, int b, int c2, int d) { filter(); }
            public void afterTextChanged(Editable e) { }
        });
        cf.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> p, View vw, int pos, long id) {
                cat = pos <= 0 || pos >= cl.size() ? "" : cl.get(pos);
                filter();
            }
            public void onNothingSelected(AdapterView<?> p) { }
        });
        v.findViewById(R.id.fab).setOnClickListener(x -> BookDialog.show(requireContext(), null, () -> {
            load();
            ((MainActivity) requireActivity()).header();
        }));
        return v;
    }

    @Override public void onResume() { super.onResume(); load(); }

    private void load() {
        all = Db.get(requireContext()).getBooks();
        Set<String> cs = new LinkedHashSet<>();
        for (Db.Book b : all) if (!b.cat.isEmpty()) cs.add(b.cat);
        cl = new ArrayList<>();
        cl.add("সব বিভাগ");
        cl.addAll(cs);
        cf.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, cl));
        int idx = cl.indexOf(cat);
        cf.setSelection(idx < 0 ? 0 : idx);
        filter();
    }

    private void filter() {
        String k = q.getText().toString().toLowerCase();
        shown = new ArrayList<>();
        for (Db.Book b : all)
            if ((cat.isEmpty() || cat.equals(b.cat)) && (b.name + " " + b.author).toLowerCase().contains(k)) shown.add(b);
        rv.getAdapter().notifyDataSetChanged();
        empty.setVisibility(shown.isEmpty() ? View.VISIBLE : View.GONE);
        empty.setText(all.isEmpty() ? "এখনো কোনো বই নেই। “নতুন বই” চেপে প্রথম বইটি যোগ করুন।" : "কোনো বই মেলেনি।");
    }

    class Adapter extends RecyclerView.Adapter<Adapter.VH> {
        class VH extends RecyclerView.ViewHolder {
            TextView n, s, t; ProgressBar p;
            VH(View v) {
                super(v);
                n = v.findViewById(R.id.i_name); s = v.findViewById(R.id.i_sub); t = v.findViewById(R.id.i_t); p = v.findViewById(R.id.i_p);
            }
        }

        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup g, int t) {
            return new VH(LayoutInflater.from(g.getContext()).inflate(R.layout.item_book, g, false));
        }

        @Override public void onBindViewHolder(@NonNull VH h, int i) {
            Db.Book b = shown.get(i);
            int rd = b.read(), pc = Math.round(rd * 100f / b.total);
            h.n.setText(b.name);
            h.s.setText(b.author + (b.cat.isEmpty() ? "" : "  •  " + b.cat));
            h.p.setProgress(pc);
            h.t.setText(Util.bn(rd) + "/" + Util.bn(b.total) + " পৃষ্ঠা, " + Util.bn(pc) + "%");
            h.itemView.setOnClickListener(x -> startActivity(new Intent(requireContext(), BookActivity.class).putExtra("id", b.id)));
        }

        @Override public int getItemCount() { return shown.size(); }
    }
}
