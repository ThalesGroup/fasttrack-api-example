package com.gemalto.idp.mobile.fasttrack.example;

import android.util.Base64;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

public class MessengerConfigurations {

    /**
     * Replace this URL with your Mobile Messenger Server URL.
     */
    public static URL getMessengerServerUrl() {
        try {
            return new URL("https://localhost/oobs-messenger/api/client/v1/action");
        } catch (MalformedURLException e) {
            return null;
        }
    }

    /**
     * Get applicationId
     */
    public static String getApplicationId() {
        return "your-application-id";
    }

    /**
     * Get userId
     */
    public static String getUserId() {
        return "your-user-id";
    }

    /**
     * Get userAlias
     */
    public static String getUserAlias() {
        return "your-user-alias";
    }

    /**
     * Get domain
     */
    public static String getDomain() {
        return "your-domain";
    }

    /**
     * Get providerId
     */
    public static String getProviderId() {
        return "your-provide-id";
    }

    // Get the public key object
    public static RSAPublicKey getPublicKey() {

        byte[] publicKey = Base64.decode(
                "000000000000000000000000000000" +
                    "000000000000000000000000000000" +
                    "000000000000000000000000000000" +
                    "000000000000000000000000000000" +
                    "000000000000000000000000000000" +
                    "000000000000000000000000000000" +
                    "000000000000000000000000000000" +
                    "000000000000000000000000000000" +
                    "000000000000000000000000000000" +
                    "000000000000000000000000000000" +
                    "000000000000000000000000000000" +
                    "000000000000000000000000000000" +
                    "000000000000000000000000000000" +
                    "000000000000000000000000000000" +
                    "000000000000000000000000000000" +
                    "000000000000000000000000000000" +
                    "000000000000000000000000000000" +
                    "0000",
                Base64.NO_PADDING | Base64.NO_WRAP);

        try {
            KeyFactory fact = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(publicKey);
            return (RSAPublicKey) fact.generatePublic(x509KeySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e1) {
            return null;
        }
    }

    /**
     * Get obfuscation key, replace this value accordingly.
     */
    public static byte[] getObfuscationKey() {
        return null;
    }

    public static String getSignaturePublicKeyPem() {
    String base64encoded = "-----BEGIN PUBLIC KEY-----\n";
    return base64encoded;
}

}
