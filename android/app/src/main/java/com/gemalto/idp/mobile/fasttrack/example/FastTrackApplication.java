package com.gemalto.idp.mobile.fasttrack.example;

import android.app.Application;
import android.content.Context;

import com.gemalto.idp.mobile.fasttrack.FastTrack;
import com.gemalto.idp.mobile.fasttrack.messenger.TransactionSignatureKey;

import java.util.Collections;

/**
 * This class will help to keep the instance of the main activity without having it as a singleton to avoid any memory
 * leaks.
 */
public class FastTrackApplication extends Application {

    public static String FULL_ACTIVATION_CODE = "YOUR_ACTIVATION_CODE";

    @Override
    public void onCreate() {
        super.onCreate();

        // 1.1 required for Dexguard decryption
        System.setProperty("java.io.tmpdir", getDir("files", Context.MODE_PRIVATE).getPath());

        if (!FastTrack.isConfigured()) {
            TransactionSignatureKey signKey = new TransactionSignatureKey(
                    MessengerConfigurations.getSignaturePublicKeyPem()
            );

            // 1.2 This is entry point for initialization the FastTrack API
            new FastTrack.Builder(this.getApplicationContext())
                    .withActivationCode(FULL_ACTIVATION_CODE)
                    .withMspObfuscationKeys(Collections.singletonList(MessengerConfigurations.getObfuscationKey()))
                    .withMspVerificationKeys(Collections.singletonList(signKey))
                    .build();
        }
    }
}
