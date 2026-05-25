package com.studyplan.app;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigation = findViewById(R.id.bottom_navigation);

        // Set default fragment
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
        }

        bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                fragment = new HomeFragment();
            } else if (itemId == R.id.nav_subject) {
                fragment = new SubjectFragment();
            } else if (itemId == R.id.nav_schedule) {
                fragment = new ScheduleFragment();
            } else if (itemId == R.id.nav_assignment) {
                fragment = new AssignmentFragment();
            } else if (itemId == R.id.nav_account) {
                fragment = new AccountFragment();
            }

            if (fragment != null) {
                loadFragment(fragment);
            }
            return true;
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    /**
     * Switch to a specific tab from fragments.
     * Call this from fragments to navigate between tabs programmatically.
     */
    public void switchToTab(int navItemId) {
        bottomNavigation.setSelectedItemId(navItemId);
    }
}
