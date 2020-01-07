package com.gemalto.idp.mobile.fasttrack.example;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.gemalto.idp.mobile.fasttrack.example.fragments.FragmentTabMessenger;
import com.gemalto.idp.mobile.fasttrack.example.fragments.FragmentTabProtector;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    // UI Elements
    private BottomNavigationView mTabBar = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Security Guideline: AND01. Sensitive data leaks
        //
        // Prevents screenshots of the app
        disableScreenShot();

        // Load basic ui components like tab bar etc...
        initGui();
    }

    /**
     * Initializes the GUI of the application.
     */
    private void initGui() {
        setContentView(R.layout.activity_main);

        mTabBar = findViewById(R.id.navigation);

        // Load MobileProtector fragment by default

        loadFragment(new FragmentTabProtector());

        mTabBar.setOnNavigationItemSelectedListener(item -> {
            Fragment fragment = null;
            switch (item.getItemId()) {
                case R.id.navigation_protector:
                    fragment = new FragmentTabProtector();
                    break;
                case R.id.navigation_messenger:
                    fragment = new FragmentTabMessenger();
                    break;
            }

            loadFragment(fragment);
            return true;
        });
    }

    /**
     * Load the selected fragment
     * @param fragment
     */
    private void loadFragment(Fragment fragment) {
        // load fragment
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.setCustomAnimations(R.anim.fade_in, R.anim.fade_out);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    /**
     * Disables screenshots.
     */
    private void disableScreenShot() {
        getWindow().setFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE,
                android.view.WindowManager.LayoutParams.FLAG_SECURE);
    }
}
