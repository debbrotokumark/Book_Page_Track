package com.debkk.bookpagetrack;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private TextView top;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_main);
        top = findViewById(R.id.top);
        BottomNavigationView nav = findViewById(R.id.nav);
        nav.setOnItemSelectedListener(i -> {
            int id = i.getItemId();
            Fragment f;
            if (id == R.id.nav_cal) f = new CalendarFragment();
            else if (id == R.id.nav_ana) f = new AnalyticsFragment();
            else if (id == R.id.nav_set) f = new SettingsFragment();
            else f = new LibraryFragment();
            getSupportFragmentManager().beginTransaction().replace(R.id.container, f).commit();
            return true;
        });
        if (s == null) nav.setSelectedItemId(R.id.nav_lib);
    }

    @Override
    protected void onResume() {
        super.onResume();
        header();
    }

    public void header() {
        Map<String, Integer> m = Db.get(this).daily();
        int t = m.getOrDefault(Util.today(), 0), g = Util.goal(this);
        top.setText("PageNest   •   আজ " + Util.bn(t) + "/" + Util.bn(g) + " পৃষ্ঠা" + (t >= g ? " ✓" : "")
                + "   🔥 " + Util.bn(Util.streak(m)) + " দিন");
    }
}
