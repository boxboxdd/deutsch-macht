package com.michel.deutschmacht

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarView
import com.michel.deutschmacht.booklet.BookletFragment
import com.michel.deutschmacht.coach.CoachFragment
import com.michel.deutschmacht.data.Speaker
import com.michel.deutschmacht.placement.PlacementFragment
import com.michel.deutschmacht.practice.PracticeFragment
import com.michel.deutschmacht.progress.ProgressFragment

class MainActivity : AppCompatActivity(), NavigationBarView.OnItemSelectedListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Speaker.init(this)
        findViewById<BottomNavigationView>(R.id.bottom_nav).apply {
            setOnItemSelectedListener(this@MainActivity)
        }
        if (savedInstanceState == null) show(CoachFragment(), "coach")
    }

    private fun show(fragment: Fragment, tag: String) {
        val existing = supportFragmentManager.findFragmentByTag(tag)
        val f = existing ?: fragment
        if (f.isVisible) return
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, f, tag)
            .commit()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.tab_coach -> { show(CoachFragment(), "coach"); true }
        R.id.tab_placement -> { show(PlacementFragment(), "placement"); true }
        R.id.tab_booklet -> { show(BookletFragment(), "booklet"); true }
        R.id.tab_practice -> { show(PracticeFragment(), "practice"); true }
        R.id.tab_progress -> { show(ProgressFragment(), "progress"); true }
        else -> false
    }
}
