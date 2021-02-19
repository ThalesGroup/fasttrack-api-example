package com.gemalto.idp.mobile.fasttrack.example;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;

import com.gemalto.idp.mobile.fasttrack.example.fragments.FragmentTabMessenger;
import com.gemalto.idp.mobile.fasttrack.example.fragments.FragmentTabProtector;

public class MainActivity extends AppCompatActivity {
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

    @Override
    public void onBackPressed() {
        boolean processed = false;

        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = fm.findFragmentById(R.id.fragment_container);
        if (fragment instanceof OnBackPressed)
            processed = ((OnBackPressed) fragment).onBackPressed();

        if (!processed)
            finish();
    }

    /**
     * Initializes the GUI of the application.
     */
    @SuppressLint("NonConstantResourceId")
    private void initGui() {
        setContentView(R.layout.activity_main);

        final Button btnProtector = findViewById(R.id.tab_protector);
        final Button btnMessenger = findViewById(R.id.tab_messenger);

        btnProtector.setOnClickListener(view -> {
            loadFragment(new FragmentTabProtector());

            btnProtector.setEnabled(false);
            btnMessenger.setEnabled(true);
        });

        btnMessenger.setOnClickListener(view -> {
            loadFragment(new FragmentTabMessenger());

            btnMessenger.setEnabled(false);
            btnProtector.setEnabled(true);
        });

        // Display PROTECTOR by default
        btnProtector.performClick();
    }

    /**
     * Load the selected fragment
     *
     * @param fragment The fragment
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

    public interface OnBackPressed {
        boolean onBackPressed();
    }
}
