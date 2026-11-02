package com.debkk.bookpagetrack;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class SettingsFragment extends Fragment {
    private final ActivityResultLauncher<String> createDoc =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("application/json"), this::doExport);
    private final ActivityResultLauncher<String[]> openDoc =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::doImport);

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle s) {
        View v = inf.inflate(R.layout.frag_settings, c, false);
        EditText goal = v.findViewById(R.id.goal);
        goal.setText(Util.bn(Util.goal(requireContext())));
        v.findViewById(R.id.goal_save).setOnClickListener(x -> {
            Util.setGoal(requireContext(), Math.max(1, Util.num(goal.getText().toString()) == 0 ? 20 : Util.num(goal.getText().toString())));
            Util.toast(requireContext(), "লক্ষ্য সেভ হয়েছে");
            ((MainActivity) requireActivity()).header();
        });
        v.findViewById(R.id.exp).setOnClickListener(x -> createDoc.launch("pagenest-backup-" + Util.today() + ".json"));
        v.findViewById(R.id.imp).setOnClickListener(x -> openDoc.launch(new String[]{"application/json", "*/*"}));
        v.findViewById(R.id.reset).setOnClickListener(x ->
                Util.confirm(requireContext(), "সব বই ও হিসাব মুছে যাবে। নিশ্চিত?", () ->
                        Util.confirm(requireContext(), "সত্যিই মুছবেন? আগে ব্যাকআপ নিয়েছেন তো?", () -> {
                            Db.get(requireContext()).resetAll();
                            ((MainActivity) requireActivity()).header();
                            Util.toast(requireContext(), "সব ডেটা মুছে ফেলা হয়েছে");
                        })));
        return v;
    }

    private void doExport(Uri uri) {
        if (uri == null) return;
        try (OutputStream os = requireContext().getContentResolver().openOutputStream(uri)) {
            os.write(Db.get(requireContext()).export(requireContext()).getBytes(StandardCharsets.UTF_8));
            Util.toast(requireContext(), "ব্যাকআপ সেভ হয়েছে");
        } catch (Exception e) {
            Util.toast(requireContext(), "ব্যাকআপ সেভ করা যায়নি");
        }
    }

    private void doImport(Uri uri) {
        if (uri == null) return;
        try (InputStream in = requireContext().getContentResolver().openInputStream(uri)) {
            int[] r = Db.get(requireContext()).importJson(Util.readAll(in));
            Util.toast(requireContext(), Util.bn(r[0]) + "টি নতুন বই যোগ, " + Util.bn(r[1]) + "টি বই মিলিয়ে আপডেট");
            ((MainActivity) requireActivity()).header();
        } catch (Exception e) {
            Util.toast(requireContext(), "ফাইলটি পড়া যায়নি — সঠিক ব্যাকআপ ফাইল দিন");
        }
    }
}
