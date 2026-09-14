package com.michel.deutschmacht;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.michel.deutschmacht.booklet.BookletFragment;
import com.michel.deutschmacht.coach.CoachFragment;
import com.michel.deutschmacht.data.Speaker;
import com.michel.deutschmacht.placement.PlacementFragment;
import com.michel.deutschmacht.practice.PracticeFragment;
import com.michel.deutschmacht.progress.ProgressFragment;

public class MainActivity extends AppCompatActivity implements NavigationBarView.OnItemSelectedListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Speaker.init(this);
        BottomNavigationView nav = findViewById(R.id.bottom_nav);
        nav.setOnItemSelectedListener(this);
        if (savedInstanceState == null) show(CoachFragment.class, "coach");
    }

    private void show(Class<? extends Fragment> cls, String tag) {
        Fragment f = getSupportFragmentManager().findFragmentByTag(tag);
        if (f == null) {
            try { f = cls.newInstance(); } catch (Exception e) { return; }
        }
        if (f.isVisible()) return;
        FragmentTransaction t = getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, f, tag);
        t.commit();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.tab_coach) show(CoachFragment.class, "coach");
        else if (id == R.id.tab_placement) show(PlacementFragment.class, "placement");
        else if (id == R.id.tab_booklet) show(BookletFragment.class, "booklet");
        else if (id == R.id.tab_practice) show(PracticeFragment.class, "practice");
        else if (id == R.id.tab_progress) show(ProgressFragment.class, "progress");
        else return false;
        return true;
    }

    public BottomNavigationView nav() { return findViewById(R.id.bottom_nav); }
}
