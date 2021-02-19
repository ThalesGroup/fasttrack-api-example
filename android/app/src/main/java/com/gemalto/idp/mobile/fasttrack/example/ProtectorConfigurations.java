package com.gemalto.idp.mobile.fasttrack.example;

import java.security.SecureRandom;
import java.util.Random;

public class ProtectorConfigurations {

    public enum PinUsage {
        GET_OTP,
        CHANGE_PIN,
        ACTIVATE_BIO_FINGERPRINT
    }

    /**
     * Targeted OTP Type
     */
    public enum ENU_OTP_TYPE {
        OATH_TOTP,
        OATH_OCRA,
        CAP,
    }

    /**
     * Replace this string with your own EPS key ID.
     * <p>
     * This is specific to the configuration of the bank's system. Therefore
     * other values should be used here.
     */
    public static String getEpsRsaKeyId() {
        return "your-eps-rsa-key-id";
    }

    /**
     * Get domain, replace this value accordingly.
     */
    public static String getEpsDomain() {
        return "your-eps-domain";
    }

    /**
     * Replace this URL with your EPS URL.
     *
     */
    public static String getEpsUrl() {
        return "https://localhost/provision";
    }

    private static String createRandomString(int len) {
        Random rng = new SecureRandom();
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < len; ++i) {
            buf.append((char) ('A' + rng.nextInt('Z' - 'A' + 1)));
        }
        return buf.toString();
    }

    public static String getRandomTokenName(ENU_OTP_TYPE otpType) {
        String tokenName = "UNKNOWN";

        switch (otpType) {
            case OATH_TOTP:
                tokenName = "TOKEN_TOTP_" + createRandomString(3);
                break;
            case OATH_OCRA:
                tokenName = "TOKEN_OCRA_" + createRandomString(3);
                break;
            case CAP:
                tokenName = "TOKEN_CAP_" + createRandomString(3);
                break;
        }

        return tokenName;
    }

    /**
     * Replace this byte array with your own EPS key modulus unless you are
     * using the EPS default key pair.
     * <p>
     * The EPS' RSA modulus. This is specific to the configuration of the
     * bank's system.  Therefore other values should be used here.
     */
    public static byte[] getEpsRsaKeyModulus() {

        // Security Guideline: GEN13. Integrity of public keys
        // Since this example hard codes the key and does not load it from a
        // file, this guideline is skipped.

        // Security Guideline: GEN17. RSA key length 2048 bit key
        return Utils.Hex.compress(
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
                "0000");
    }

    /**
     * Replace this byte array with your own EPS key exponent.
     * <p>
     * The EPS' RSA exponent. This is specific to the configuration of the
     * bank's system.  Therefore other values should be used here.
     */
    public static byte[] getEpsRsaKeyExponent() {
        // Security Guideline: GEN13. Integrity of public keys
        // Since this example hard codes the key and does not load it from a
        // file, this guideline is skipped.
        return new byte[]{ 0x01, 0x00, 0x01 };
    }

}
