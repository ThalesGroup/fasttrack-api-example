package com.gemalto.idp.mobile.fasttrack.example.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.gemalto.idp.mobile.authentication.mode.pin.PinAuthInput;
import com.gemalto.idp.mobile.fasttrack.FastTrack;
import com.gemalto.idp.mobile.fasttrack.FastTrackException;
import com.gemalto.idp.mobile.fasttrack.example.MainActivity;
import com.gemalto.idp.mobile.fasttrack.example.ProtectorConfigurations;
import com.gemalto.idp.mobile.fasttrack.example.R;
import com.gemalto.idp.mobile.fasttrack.example.Utils;
import com.gemalto.idp.mobile.fasttrack.protector.BioFingerprintAuthenticationCallbacks;
import com.gemalto.idp.mobile.fasttrack.protector.BioFingerprintAuthenticationStatus;
import com.gemalto.idp.mobile.fasttrack.protector.BiometricAuthenticationCallbacks;
import com.gemalto.idp.mobile.fasttrack.protector.BiometricAuthenticationStatus;
import com.gemalto.idp.mobile.fasttrack.protector.MobileProtector;
import com.gemalto.idp.mobile.fasttrack.protector.ProtectorAuthInput;
import com.gemalto.idp.mobile.fasttrack.protector.TokenDevice;
import com.gemalto.idp.mobile.fasttrack.protector.cap.CapMobileProtector;
import com.gemalto.idp.mobile.fasttrack.protector.cap.CapSettings;
import com.gemalto.idp.mobile.fasttrack.protector.cap.CapTokenDevice;
import com.gemalto.idp.mobile.fasttrack.protector.cap.CapTokenDeviceCreationCallback;
import com.gemalto.idp.mobile.fasttrack.protector.oath.OathMobileProtector;
import com.gemalto.idp.mobile.fasttrack.protector.oath.OathTokenDevice;
import com.gemalto.idp.mobile.fasttrack.protector.oath.OathTokenDeviceCreationCallback;
import com.gemalto.idp.mobile.fasttrack.protector.oath.OcraSettings;
import com.gemalto.idp.mobile.fasttrack.protector.oath.TotpSettings;
import com.gemalto.idp.mobile.ui.UiModule;
import com.gemalto.idp.mobile.ui.secureinput.SecureInputBuilder;
import com.gemalto.idp.mobile.ui.secureinput.SecureInputService;
import com.gemalto.idp.mobile.ui.secureinput.SecureInputUi;
import com.gemalto.idp.mobile.ui.secureinput.SecureKeypadListener;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Currency;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.gemalto.idp.mobile.fasttrack.example.ProtectorConfigurations.ENU_OTP_TYPE.CAP;
import static com.gemalto.idp.mobile.fasttrack.example.ProtectorConfigurations.ENU_OTP_TYPE.OATH_OCRA;
import static com.gemalto.idp.mobile.fasttrack.example.ProtectorConfigurations.ENU_OTP_TYPE.OATH_TOTP;
import static com.gemalto.idp.mobile.fasttrack.example.ProtectorConfigurations.PinUsage.ACTIVATE_BIO_FINGERPRINT;
import static com.gemalto.idp.mobile.fasttrack.example.ProtectorConfigurations.PinUsage.CHANGE_PIN;
import static com.gemalto.idp.mobile.fasttrack.example.ProtectorConfigurations.PinUsage.GET_OTP;
import static com.gemalto.idp.mobile.fasttrack.example.ProtectorConfigurations.getEpsDomain;
import static com.gemalto.idp.mobile.fasttrack.example.ProtectorConfigurations.getEpsRsaKeyExponent;
import static com.gemalto.idp.mobile.fasttrack.example.ProtectorConfigurations.getEpsRsaKeyId;
import static com.gemalto.idp.mobile.fasttrack.example.ProtectorConfigurations.getEpsRsaKeyModulus;
import static com.gemalto.idp.mobile.fasttrack.example.ProtectorConfigurations.getEpsUrl;

public class FragmentTabProtector extends Fragment implements
        AdapterView.OnItemSelectedListener,
        MainActivity.OnBackPressed {

    private final Utils.MyLogger mMyLogger = Utils.MyLogger.getsInstance();

    /**
     * TODO
     * Set this flag to true to enable the SecureKeyPad.
     * <p>
     * NOTE: Need to have proper activation code to use this feature.
     */
    private static final boolean USE_SECURE_KEYPAD = true;

    //region Member - UI elements
    private EditText mRegistrationCode;
    private Button mProvisionButton;
    private Button mGetOtpPinButton;
    private Button mChangePinButton;
    private Button mActivateBiometricBtn;
    private Button mDeactivateBiometricBtn;
    private Button mGetOtpBiometricBtn;
    private Button mRemoveTokenDevice;
    private Button mListTokenDevice;
    private TextView mLogTextView;
    private RadioGroup mRadioGroup;
    private Spinner mTokenSpinner;

    private View mOtpTabContainer;
    private View mSecureKeypadContainer;

    private Activity mActivity;
    private BioMetricFragment bioMetricFragment;
    private final CancellationSignal mCancellationSignal = new CancellationSignal();
    private ProtectorConfigurations.ENU_OTP_TYPE mCurrentOtpType;
    //endregion

    //region Member
    private String challenge, amount;
    private Currency currency;
    private List<String> defaultTds;

    private CapMobileProtector mCapMobileProtector;

    // OATH-TOTP and OATH-OCRA use the same oath protector
    private OathMobileProtector mOathMobileProtector;
    //endregion

    //region UI Callback
    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        final View retValue = initGui(inflater);

        mActivity = getActivity();

        try {
            initMobileProtectors();
            try {
                updateTokenSpinner(mCurrentOtpType, getCurrentTokenDevice());
            } catch (FastTrackException e) {
                // Print the error
                mMyLogger.updateLogMessage(mLogTextView, "An error happen : "
                        + e.getClass().getName() + " | Reason : " + e.getMessage() + "\n", false);
            }
        } catch (MalformedURLException e) {
            // Print the error
            mMyLogger.updateLogMessage(mLogTextView, "An error happen : "
                    + e.getClass().getName() + " | Reason : " + e.getMessage() + "\n", false);
        }

        return retValue;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        bioMetricFragment = BioMetricFragment.newInstance(bioFpFragmentCallback, "Generate OTP");
    }

    @Override
    public void onPause() {
        super.onPause();

        // Dismiss the SecureKeypad for security reason
        onBackPressed();
        mMyLogger.cleanLog(mLogTextView);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        // On selecting a spinner item
        String item = parent.getItemAtPosition(position).toString();

        // Showing selected spinner item
        Toast.makeText(parent.getContext(), "Selected: " + item, Toast.LENGTH_LONG).show();
    }

    public void onNothingSelected(AdapterView<?> arg0) { }

    @Override
    public boolean onBackPressed() {
        // Clean any cached PIN input
        if (mCurrentPinInput != null) {
            mCurrentPinInput.wipe();
            mCurrentPinInput = null;
        }

        return dismissSecureKeypad();
    }
    //endregion

    @SuppressLint("InflateParams")
    private View initGui(final LayoutInflater inflater) {
        View mainView = inflater.inflate(R.layout.fragment_tab_protector, null);

        mRegistrationCode = mainView.findViewById(R.id.protector_registration_code);
        mProvisionButton = mainView.findViewById(R.id.provision);
        mGetOtpPinButton = mainView.findViewById(R.id.getOtpByPin);
        mChangePinButton = mainView.findViewById(R.id.changePin);
        mActivateBiometricBtn = mainView.findViewById(R.id.activateBiometric);
        mDeactivateBiometricBtn = mainView.findViewById(R.id.deactivateBiometric);
        mGetOtpBiometricBtn = mainView.findViewById(R.id.getOtpByBiometric);
        mRemoveTokenDevice = mainView.findViewById(R.id.removeTokenDevice);
        mListTokenDevice = mainView.findViewById(R.id.listTokenDevices);
        mLogTextView = mainView.findViewById(R.id.log);
        mRadioGroup = mainView.findViewById(R.id.radio_otp_type);

        mTokenSpinner = mainView.findViewById(R.id.tokenSpinner);

        mOtpTabContainer = mainView.findViewById(R.id.otp_tab_container);
        mSecureKeypadContainer = mainView.findViewById(R.id.secure_keypad_container);

        mRadioGroup.check(R.id.radioOathTotp);
        mCurrentOtpType = OATH_TOTP;

        setListeners();

        return mainView;
    }

    private void updateTokenSpinner(
            ProtectorConfigurations.ENU_OTP_TYPE otp_type,
            String selectedToken
    ) throws FastTrackException {

        Set<String> tokenDevices = getTokenNames(otp_type);
        // Spinner click listener
        mTokenSpinner.setOnItemSelectedListener(this);

        // Creating adapter for spinner initialising with totp token devices.
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(mActivity,
                android.R.layout.simple_spinner_item, new ArrayList<>(tokenDevices));

        // Drop down layout style - list view with radio button
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // attaching data adapter to spinner
        mTokenSpinner.setAdapter(dataAdapter);

        // Update selected token
        if (selectedToken != null) {
            int spinnerPosition = dataAdapter.getPosition(selectedToken);
            mTokenSpinner.setSelection(spinnerPosition);
        }
    }

    private void initMobileProtectors() throws MalformedURLException {

        // 2. Initialization of Mobile Protector which can be either OATH or CAP
        // Normally we only need to choose either one of them

        // 2.a Initialization for Oath Mobile Protector
        mOathMobileProtector = FastTrack.getInstance().getOathMobileProtectorBuilder(
                new URL(getEpsUrl()),
                getEpsDomain(),
                MobileProtector.ProvisioningProtocol.PROTOCOL_V5,
                getEpsRsaKeyId(),
                getEpsRsaKeyExponent(),
                getEpsRsaKeyModulus()
        )
                .build();

        // 2.b Initialization for CAP Mobile Protector
        mCapMobileProtector = FastTrack.getInstance().getCapMobileProtectorBuilder(
                new URL(getEpsUrl()),
                getEpsDomain(),
                MobileProtector.ProvisioningProtocol.PROTOCOL_V5,
                getEpsRsaKeyId(),
                getEpsRsaKeyExponent(),
                getEpsRsaKeyModulus()
        )
                .build();
    }

    /**
     * Example fake data
     */
    private void initCapValue() {
        challenge = "12345678";
        amount = "1000";
        currency = Currency.getInstance("USD");
        defaultTds = new ArrayList<>();
        defaultTds.clear();
        defaultTds.add("1111");
        defaultTds.add("222");
        defaultTds.add("33");
        defaultTds.add("4");
    }

    @SuppressLint("NonConstantResourceId")
    private void setListeners() {
        mProvisionButton.setOnClickListener(v -> {
            Utils.hideKeyboard(mActivity, mRegistrationCode);

            mMyLogger.updateLogTitle(mLogTextView, "Provisioning " + mCurrentOtpType + " Token");

            switch (mCurrentOtpType) {
                case OATH_TOTP:
                    provisionOathTotpToken();
                    break;

                case OATH_OCRA:
                    provisionOathOcraToken();
                    break;

                case CAP:
                    provisionCapToken();
                    break;
            }
        });

        mGetOtpPinButton.setOnClickListener(v -> {
            if (USE_SECURE_KEYPAD)
                showSecureKeyPad(GET_OTP);
            else
                displayPinDialog(GET_OTP);
        });

        mChangePinButton.setOnClickListener(v -> {
            if (USE_SECURE_KEYPAD)
                showSecureKeyPad(CHANGE_PIN);
            else
                displayPinDialog(CHANGE_PIN);
        });

        mActivateBiometricBtn.setOnClickListener(v -> {
            if (USE_SECURE_KEYPAD)
                showSecureKeyPad(ACTIVATE_BIO_FINGERPRINT);
            else
                displayPinDialog(ACTIVATE_BIO_FINGERPRINT);
        });

        mDeactivateBiometricBtn.setOnClickListener(v -> {
            try {
                deactivateBiometric();
            } catch (FastTrackException e) {
                // Print the error
                mMyLogger.updateLogMessage(mLogTextView, "An error happen : "
                        + e.getClass().getName() + " | Reason : " + e.getMessage() + "\n", false);
            }
        });

        mGetOtpBiometricBtn.setOnClickListener(v -> {
            try {
                getOtpByBiometric();
            } catch (FastTrackException e) {
                // Print the error
                mMyLogger.updateLogMessage(mLogTextView, "An error happen : "
                        + e.getClass().getName() + " | Reason : " + e.getMessage() + "\n", false);
            }
        });

        mListTokenDevice.setOnClickListener(v -> {
            try {
                mMyLogger.updateLogTitle(mLogTextView, "TokenDevices");
                Set<String> remainingTokens = getTokenNames(mCurrentOtpType);
                for (String tokenName : remainingTokens)
                    mMyLogger.updateLogMessage(mLogTextView, tokenName);
            } catch (FastTrackException e) {
                // Print the error
                mMyLogger.updateLogMessage(mLogTextView, "An error happen : "
                        + e.getClass().getName() + " | Reason : " + e.getMessage() + "\n", false);
            }
        });

        mRemoveTokenDevice.setOnClickListener(v -> {
            try {
                removeTokenDevice();
            } catch (FastTrackException e) {
                // Print the error
                mMyLogger.updateLogMessage(mLogTextView, "An error happen : "
                        + e.getClass().getName() + " | Reason : " + e.getMessage() + "\n", false);
            }
        });

        mRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            switch (checkedId) {
                case R.id.radioOathTotp:
                    mCurrentOtpType = OATH_TOTP;
                    break;

                case R.id.radioOathOcra:
                    mCurrentOtpType = OATH_OCRA;
                    break;

                case R.id.radioCap:
                    mCurrentOtpType = CAP;
                    break;
            }

            try {
                updateTokenSpinner(mCurrentOtpType, getCurrentTokenDevice());
            } catch (FastTrackException e) {
                // Print the error
                mMyLogger.updateLogMessage(mLogTextView, "An error happen : "
                        + e.getClass().getName() + " | Reason : " + e.getMessage() + "\n", false);
            }
        });
    }

    private String getCurrentTokenDevice() {
        String deviceName = null;

        if (mTokenSpinner.getSelectedItem() != null)
            deviceName = mTokenSpinner.getSelectedItem().toString();

        return deviceName;
    }

    private Set<String> getTokenNames(ProtectorConfigurations.ENU_OTP_TYPE otpType)
            throws FastTrackException {

        Set<String> retTokenNames = new HashSet<>();
        Set<String> tokenNames;

        switch (otpType) {
            case OATH_TOTP: {
                tokenNames = mOathMobileProtector.getTokenDeviceNames();
                for (String tokenName : tokenNames) { // pick up the first token.
                    if (tokenName.startsWith("TOKEN_TOTP_")) {
                        retTokenNames.add(tokenName);
                        break;
                    }
                }
                break;
            }

            case OATH_OCRA: {
                tokenNames = mOathMobileProtector.getTokenDeviceNames();
                for (String tokenName : tokenNames) { // pick up the first token.
                    if (tokenName.startsWith("TOKEN_OCRA_")) {
                        retTokenNames.add(tokenName);
                        break;
                    }
                }
                break;
            }

            case CAP: {
                tokenNames = mCapMobileProtector.getTokenDeviceNames();
                for (String tokenName : tokenNames) { // pick up the first token.
                    if (tokenName.startsWith("TOKEN_CAP_")) {
                        retTokenNames.add(tokenName);
                        break;
                    }
                }
                break;
            }
        }

        return retTokenNames;
    }

    //region Provision Tokens
    private void provisionOathTotpToken() {

        // 3.1 Apply settings for this particular Token
        TotpSettings totpSettings = new TotpSettings();

        try {
            final String oathTokenName = ProtectorConfigurations.getRandomTokenName(OATH_TOTP);

            // 3.2 Provisioning the token will require Callback as it requires Network
            OathTokenDeviceCreationCallback callback = new OathTokenDeviceCreationCallback() {
                @Override
                public void onSuccess(OathTokenDevice oathTokenDevice, Map<String, String> extension) {
                    mMyLogger.updateLog(mLogTextView, "Provision OATH_TOTP Success",
                            "Token " + oathTokenName + " created.");

                    try {
                        updateTokenSpinner(OATH_TOTP, oathTokenName);
                    } catch (FastTrackException e) {
                        // Print the error
                        mMyLogger.updateLogMessage(mLogTextView, "An error happen : "
                                + e.getClass().getName() + " | Reason : " + e.getMessage() + "\n", false);
                    }
                }

                @Override
                public void onError(FastTrackException e) {
                    // Print the error
                    mMyLogger.updateLogMessage(mLogTextView, "An error happen : "
                            + e.getClass().getName() + " | Reason : " + e.getMessage() + "\n", false);
                }
            };

            // 3.3 Call Provision API to start provisioning
            // This API call will save the settings locally
            mOathMobileProtector.provision(oathTokenName, mRegistrationCode.getText().toString(), totpSettings, callback);

        } catch (Exception e) {
            // Print the error
            mMyLogger.updateLogMessage(mLogTextView, "An error happen : "
                    + e.getClass().getName() + " | Reason : " + e.getMessage() + "\n", false);
        }
    }

    private void provisionOathOcraToken() {

        // 3.1 Apply settings for this particular Token
        OcraSettings ocraSettings = new OcraSettings();

        ocraSettings.setStartTime(0);
        ocraSettings.setOcraSuite("OCRA-1:HOTP-SHA256-8:C-QA09-PSHA1-S064-T30S");

        try {
            final String oathTokenName = ProtectorConfigurations.getRandomTokenName(OATH_OCRA);

            // 3.2 Provisioning the token will require Callback as it requires Network
            OathTokenDeviceCreationCallback callback = new OathTokenDeviceCreationCallback() {
                @Override
                public void onSuccess(OathTokenDevice oathTokenDevice, Map<String, String> extension) {
                    mMyLogger.updateLog(mLogTextView, "Provision OATH_OCRA Success",
                            "Token " + oathTokenName + " created.");

                    try {
                        updateTokenSpinner(OATH_OCRA, oathTokenName);
                    } catch (FastTrackException e) {
                        // Print the error
                        mMyLogger.updateLogMessage(mLogTextView, "An error happen : "
                                + e.getClass().getName() + " | Reason : " + e.getMessage() + "\n", false);
                    }
                }

                @Override
                public void onError(FastTrackException e) {
                    mMyLogger.updateLogMessage(mLogTextView, "An error happen : "
                            + e.getClass().getName() + " | Reason : " + e.getMessage() + "\n", false);
                }
            };

            // 3.3 Call Provision API to start provisioning
            // This API call will save the settings locally
            mOathMobileProtector.provision(oathTokenName, mRegistrationCode.getText().toString(), ocraSettings, callback);

        } catch (Exception e) {
            // Print the error
            mMyLogger.updateLogMessage(mLogTextView, "An error happen : "
                    + e.getClass().getName() + " | Reason : " + e.getMessage() + "\n", false);
        }
    }

    private void provisionCapToken() {
        // 3.1 Apply settings for this particular Token
        CapSettings capSettings = new CapSettings();

        try {
            final String capTokenName = ProtectorConfigurations.getRandomTokenName(CAP);

            // 3.2 Provisioning the token will require Callback as it requires Network
            CapTokenDeviceCreationCallback callback = new CapTokenDeviceCreationCallback() {
                @Override
                public void onSuccess(CapTokenDevice capTokenDevice, Map<String, String> extension) {
                    mMyLogger.updateLog(mLogTextView, "Provision CAP Success",
                            "Token " + capTokenName + " created.");

                    try {
                        updateTokenSpinner(CAP, capTokenName);
                    } catch (FastTrackException e) {
                        // Print the error
                        mMyLogger.updateLogMessage(mLogTextView, "An error happen : "
                                + e.getClass().getName() + " | Reason : " + e.getMessage() + "\n", false);
                    }
                }

                @Override
                public void onError(FastTrackException e) {
                    // Print the error
                    mMyLogger.updateLogMessage(mLogTextView, "An error happen : "
                            + e.getClass().getName() + " | Reason : " + e.getMessage() + "\n", false);
                }
            };

            // 3.3 Call Provision API to start provisioning
            // This API call will save the settings locally
            mCapMobileProtector.provision(capTokenName,
                    mRegistrationCode.getText().toString(),
                    capSettings,
                    callback);

        } catch (Exception e) {
            // Print the error
            mMyLogger.updateLogMessage(mLogTextView, "An error happen : "
                    + e.getClass().getName() + " | Reason : " + e.getMessage() + "\n", false);
        }

    }
    //endregion Provision Tokens

    //region SecureKeyPad
    private SecureInputBuilder mKeypadBuilder;

    private ProtectorAuthInput mCurrentPinInput = null;

    private static final String TAG_SKP = "SecureKeyPad";

    /**
     * Display the SecureKeypad for PIN input
     *
     * @param pinUsage The purpose of showing keypad, for PIN value or changing PIN
     */
    private void showSecureKeyPad(ProtectorConfigurations.PinUsage pinUsage) {
        DialogFragment keypadFragment = null;

        if (getCurrentTokenDevice() == null) {
            mMyLogger.updateLogMessage(mLogTextView, "No TokenDevice selected.");
            return;
        }

        do {
            // Configure the secure keypad
            // For more information on configure the SecureKeyPad, check the EzioMobileExample application

            SecureKeypadListener listener = new SecureKeypadListener() {
                @Override
                public void onKeyPressedCountChanged(int i, int i1) { }

                @Override
                public void onInputFieldSelected(int i) { }

                @Override
                public void onOkButtonPressed() { }

                @Override
                public void onDeleteButtonPressed() { }

                @Override
                public void onFinish(PinAuthInput pin1, PinAuthInput pin2) {
                    // 0. Dismiss and clean up the current keypad first
                    dismissSecureKeypad();

                    // 1. Process the pin result

                    //region 1.1 Switch the data type
                    ProtectorAuthInput pinInput = null;
                    switch (mCurrentOtpType) {
                        case CAP:
                            pinInput = mCapMobileProtector.getProtectorAuthInput(pin1);
                            break;

                        case OATH_OCRA:
                        case OATH_TOTP:
                            pinInput = mOathMobileProtector.getProtectorAuthInput(pin1);
                            break;
                    }
                    //endregion

                    //region 1.2 Process the PIN input based on the use-case
                    switch (pinUsage) {
                        case GET_OTP: {
                            // Generate the OTP using the PIN input
                            try {
                                getOtpUsingPin(null, pinInput);
                            } catch (FastTrackException ex) {
                                mMyLogger.updateLogMessage(mLogTextView, "An error happen : "
                                        + ex.getClass().getName() + " | Reason : " + ex.getMessage() + "\n", false);
                            } finally {
                                // Always wipe the pin after usage
                                pinInput.wipe();
                            }
                            break;
                        }

                        case CHANGE_PIN: {
                            //region Cache the current pin input?
                            if (mCurrentPinInput == null) {
                                mMyLogger.updateLogTitle(mLogTextView, "CHANGE PIN");

                                // Save the current PIN
                                // NOTE: The application should verify again that the current PIN is correct,
                                // since the SDK can not check it. If this PIN is wrong, the later generated OTP will not be verified.
                                mCurrentPinInput = pinInput;

                                // Show the SecureKeypad again with doubled-input
                                showSecureKeyPad(pinUsage);
                                break;
                            }
                            //endregion

                            //region Process the change PIN
                            // Verify the the pin inputs are identical
                            if (!pin1.equals(pin2)) {
                                mMyLogger.updateLogMessage(mLogTextView, "The two new PINs are not same!");
                                mCurrentPinInput = null;
                                break;
                            }

                            try {
                                changePin(null, null, mCurrentPinInput, pinInput);
                                mMyLogger.updateLogMessage(mLogTextView, "Change PIN successfully");
                            } catch (FastTrackException ex) {
                                mMyLogger.updateLogMessage(mLogTextView, "An error happen : "
                                        + ex.getClass().getName() + " | Reason : " + ex.getMessage() + "\n", false);
                            } finally {
                                // Always wipe the pin after usage
                                pinInput.wipe();
                                mCurrentPinInput.wipe();
                                mCurrentPinInput = null;
                            }
                            //endregion
                            break;
                        }

                        case ACTIVATE_BIO_FINGERPRINT: {
                            mMyLogger.updateLogTitle(mLogTextView, "ACTIVATE BioFingerprint/Biometric");

                            try {

                                activateBiometric(null, pinInput);
                                mMyLogger.updateLogMessage(mLogTextView, "BioFingerprint/Biometric activated");

                            } catch (FastTrackException ex) {
                                mMyLogger.updateLogMessage(mLogTextView, "An error happen : "
                                        + ex.getClass().getName() + " | Reason : " + ex.getMessage() + "\n", false);
                            } finally {
                                // Always wipe the pin after usage
                                pinInput.wipe();
                            }

                            break;
                        }
                    }
                    //endregion
                }

                @Override
                public void onError(String error) {
                    mMyLogger.updateLogMessage(mLogTextView,
                            "SecureKeyPad error: " + error + "\n", false);
                }
            };

            try {
                if (mKeypadBuilder != null) {
                    mKeypadBuilder.wipe();
                    mKeypadBuilder = null;
                }

                SecureInputService siService = SecureInputService.create(UiModule.create());
                mKeypadBuilder = siService.getSecureInputBuilder();

                mKeypadBuilder.setKeypadMatrix(4, 4);
                mKeypadBuilder.setMaximumAndMinimumInputLength(PIN_LENGTH, PIN_LENGTH);

                mKeypadBuilder.setKeys(Arrays.asList('1', '2', '3', '4', '5', '6', '7', '8', '9', '0'),
                        null);
                mKeypadBuilder.setShiftKeys(Arrays.asList('A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J'),
                        null);

                boolean isDoubledInput = (pinUsage == CHANGE_PIN && mCurrentPinInput != null);
                if (isDoubledInput) {
                    mKeypadBuilder.setFirstLabel("Enter new PIN");
                    mKeypadBuilder.setSecondLabel("Confirm new PIN");
                }

                SecureInputUi siUI = mKeypadBuilder.buildKeypad(true, isDoubledInput, false, listener);
                keypadFragment = siUI.getDialogFragment();
            } catch (Exception ex) {
                mMyLogger.updateLogMessage(mLogTextView, "An error happen : "
                        + ex.getClass().getName() + " | Reason : " + ex.getMessage() + "\n", false);
            }

            if (keypadFragment == null) {
                // Failed to initialise SecureKeyPad - Fallback to raw pin input
                displayPinDialog(pinUsage);
                break;
            }

            // Show the SecureKeypad
            mOtpTabContainer.setVisibility(View.GONE);
            mSecureKeypadContainer.setVisibility(View.VISIBLE);

            FragmentManager fm = getFragmentManager();
            fm.beginTransaction()
                    .add(mSecureKeypadContainer.getId(), keypadFragment, TAG_SKP)
                    .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                    .addToBackStack(null)
                    .commit();
        } while (false);
    }

    private boolean dismissSecureKeypad() {
        boolean processed = false;

        FragmentManager fm = getFragmentManager();
        Fragment skpFragment = fm.findFragmentByTag(TAG_SKP);
        if (skpFragment != null) {
            fm.popBackStack();

            mOtpTabContainer.setVisibility(View.VISIBLE);
            mSecureKeypadContainer.setVisibility(View.GONE);
            processed = true;
        }

        if (mKeypadBuilder != null) {
            mKeypadBuilder.wipe();
            mKeypadBuilder = null;
        }

        return processed;
    }
    //endregion

    //region Get OTPs - PIN

    private static final int PIN_LENGTH = 8;

    private void displayPinDialog(ProtectorConfigurations.PinUsage pinUsage) {

        String tkDeviceName = getCurrentTokenDevice();
        if (tkDeviceName == null) {
            mMyLogger.updateLogMessage(mLogTextView, "No TokenDevice selected.");
            return;
        }

        LayoutInflater li = LayoutInflater.from(mActivity);
        View promptsView = li.inflate(R.layout.pin_input_dialog, null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mActivity);

        // set prompts.xml to alert dialog builder
        alertDialogBuilder.setView(promptsView);

        TextView userInputHint = promptsView.findViewById(R.id.pinUsageHint);
        switch (pinUsage) {
            case GET_OTP:
                userInputHint.setText(getString(R.string.enter_pin_to_get_otp));
                break;

            case CHANGE_PIN:
                userInputHint.setText(getString(R.string.txt_raw_pin_enter_old_pin_hint_label));
                break;

            case ACTIVATE_BIO_FINGERPRINT:
                userInputHint.setText(getString(R.string.enter_pin_to_activate_biofingerprint));
                break;
        }

        final EditText userInput = promptsView.findViewById(R.id.editTextPin);
        userInput.setFilters(new InputFilter[]{
                new InputFilter.LengthFilter(PIN_LENGTH)
        });

        // set dialog message
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK",
                        (dialog, id) -> {
                            String pinText = userInput.getText().toString();

                            switch (pinUsage) {
                                case GET_OTP: {
                                    try {
                                        getOtpUsingPin(pinText, null);
                                    } catch (FastTrackException e) {
                                        // Print the error
                                        mMyLogger.updateLogMessage(mLogTextView,
                                                "An error happen : " + e.getClass().getName() +
                                                        " | Reason : " + e.getMessage() + "\n", false);
                                    }
                                    break;
                                }

                                case CHANGE_PIN:
                                    displayChangePinDialog(pinText);
                                    break;

                                case ACTIVATE_BIO_FINGERPRINT: {
                                    try {
                                        activateBiometric(pinText, null);
                                    } catch (FastTrackException e) {
                                        // Print the error
                                        mMyLogger.updateLogMessage(mLogTextView,
                                                "An error happen : " + e.getClass().getName() + " | Reason : " + e.getMessage() + "\n", false);
                                    }
                                    break;
                                }
                            }
                        })
                .setNegativeButton("Cancel",
                        (dialog, id) -> dialog.cancel());

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();
        // show it
        alertDialog.show();
    }

    private void getOtpUsingPin(
            String pinText,
            ProtectorAuthInput pinInput
    ) throws FastTrackException {
        String tkDeviceName = getCurrentTokenDevice();
        if (tkDeviceName == null) {
            mMyLogger.updateLogMessage(mLogTextView, "No TokenDevice selected.");
            return;
        }

        switch (mCurrentOtpType) {
            case OATH_TOTP:
                generateOathTotpByPin(tkDeviceName, pinText, pinInput);
                break;

            case OATH_OCRA:
                generateOathOcraByPin(tkDeviceName, pinText, pinInput);
                break;

            case CAP:
                generateCapOtpByPin(tkDeviceName, pinText, pinInput);
                break;
        }
    }

    private void generateOathTotpByPin(
            String tkDeviceName,
            String pinText,
            ProtectorAuthInput pinInput
    ) throws FastTrackException {
        // 4.1 Get Token Device to be used to generate OTP
        OathTokenDevice tokenDevice = mOathMobileProtector.getTokenDevice(tkDeviceName, null);
        if (tokenDevice == null)
            return;

        // 4.2 Generate OTP by passing the authentication: PIN
        String otp = null;

        if (pinText != null) {
            // 4.2.a Generate OTP by text PIN
            otp = tokenDevice.getOtp(pinText);
        } else if (pinInput != null) {
            // 4.2.b Generate OTP by PIN input
            otp = tokenDevice.getOtp(pinInput);
        }

        mMyLogger.updateLog(mLogTextView, "Get OTP PIN", "Token: "
                + getCurrentTokenDevice() + "\n" + "OTP: " + otp);
    }

    private void generateOathOcraByPin(
            String tkDeviceName,
            String pinText,
            ProtectorAuthInput pinInput
    ) throws FastTrackException {

        // 4.1 Get Token Device to be used to generate OTP
        OathTokenDevice oathTokenDevice = mOathMobileProtector.getTokenDevice(tkDeviceName, null);
        if (oathTokenDevice == null)
            return;

        // Generate the OTP
        String serverChallenge = "000000003";
        String clientChallenge = "000000003";
        String password = "password";
        String session = "\u20ac" + "10"; // (Euro) E2 82 AC + 31 + 30
        byte[] passwordHash = oathTokenDevice.getOcraPasswordHash(password);

        // 4.2 Generate OTP by passing the authentication: PIN
        String ocraOtp = null;

        if (pinText != null) {
            // 4.2.a Generate OTP by text PIN
            ocraOtp = oathTokenDevice.getOcraOtp(pinText, serverChallenge, clientChallenge, passwordHash, session);
        } else if (pinInput != null) {
            // 4.2.b Generate OTP by PIN input
            ocraOtp = oathTokenDevice.getOcraOtp(pinInput, serverChallenge, clientChallenge, passwordHash, session);
        }

        mMyLogger.updateLog(mLogTextView, "Get OTP PIN", "Token: "
                + getCurrentTokenDevice() + "\n" + "OCRA OTP: " + ocraOtp);
    }

    private void generateCapOtpByPin(
            String tkDeviceName,
            String pinText,
            ProtectorAuthInput pinInput
    ) throws FastTrackException {
        // 4.1 Get Token Device to be used to generate OTP
        CapTokenDevice tokenDevice = mCapMobileProtector.getTokenDevice(tkDeviceName, null);
        if (tokenDevice == null)
            return;

        mMyLogger.updateLog(mLogTextView, "Get OTP PIN", "Current Token: " + tkDeviceName);

        initCapValue();

        // 4.2 Generate OTP
        String otpMode1 = null;
        String otpMode2 = null;
        String otpMode2Tds = null;
        String otpMode3 = null;

        if (pinText != null) {
            // 4.2.a Generate OTP by text PIN

            otpMode1 = tokenDevice.getOtpMode1(pinText, challenge, amount, currency);

            otpMode2 = tokenDevice.getOtpMode2(pinText);

            otpMode2Tds = tokenDevice.getOtpMode2Tds(pinText, defaultTds);

            otpMode3 = tokenDevice.getOtpMode3(pinText, challenge);
        } else if (pinInput != null) {
            // 4.2.b Generate OTP by PIN input

            otpMode1 = tokenDevice.getOtpMode1(pinInput, challenge, amount, currency);

            otpMode2 = tokenDevice.getOtpMode2(pinInput);

            otpMode2Tds = tokenDevice.getOtpMode2Tds(pinInput, defaultTds);

            otpMode3 = tokenDevice.getOtpMode3(pinInput, challenge);
        }

        mMyLogger.updateLogMessage(mLogTextView, "OTP Mode1: " + otpMode1);
        mMyLogger.updateLogMessage(mLogTextView, "OTP Mode2: " + otpMode2);
        mMyLogger.updateLogMessage(mLogTextView, "OTP Mode2Tds: " + otpMode2Tds);
        mMyLogger.updateLogMessage(mLogTextView, "OTP Mode3: " + otpMode3);
    }
    //endregion

    //region Change PIN

    /**
     * Displaying the change pin ***NOT*** using SecurePinPad
     */
    @SuppressWarnings("InflateParams")
    private void displayChangePinDialog(String currentPin) {
        String tkDeviceName = getCurrentTokenDevice();
        if (tkDeviceName == null) {
            mMyLogger.updateLogMessage(mLogTextView, "No TokenDevice selected.");
            return;
        }

        mMyLogger.updateLogTitle(mLogTextView, "Change PIN");

        LayoutInflater inflater = LayoutInflater.from(mActivity);
        View dialogLayout = inflater.inflate(R.layout.verifycode_input_dialog, null);

        InputFilter[] pinFilters = new InputFilter[]{
                new InputFilter.LengthFilter(PIN_LENGTH)
        };

        final EditText pinCodeTxt = dialogLayout.findViewById(R.id.txt_code);
        pinCodeTxt.setHint(R.string.txt_raw_pin_enter_pin_hint_label);
        pinCodeTxt.setFilters(pinFilters);

        final EditText confirmPinCodeTxt = dialogLayout.findViewById(R.id.txt_confirm_code);
        confirmPinCodeTxt.setVisibility(View.VISIBLE);
        confirmPinCodeTxt.setFilters(pinFilters);

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(mActivity);
        builder.setView(dialogLayout);
        builder.setTitle(R.string.please_provide_your_pin);
        builder.setPositiveButton(android.R.string.ok,
                (dialog, which) -> {
                    dialog.dismiss();

                    // Construct the PIN1 object
                    String pin1 = pinCodeTxt.getText().toString();
                    // Construct the PIN2 object
                    String pin2 = confirmPinCodeTxt.getText().toString();

                    // Process to next step
                    Utils.hideKeyboard(mActivity, pinCodeTxt);

                    if (!pin1.equals(pin2)) {
                        mMyLogger.updateLogMessage(mLogTextView, "The two PINs are not same.");
                        return;
                    }

                    try {
                        changePin(currentPin, pin1, null, null);
                        mMyLogger.updateLogMessage(mLogTextView, "Change PIN done!");
                    } catch (FastTrackException e) {
                        // Print the error
                        mMyLogger.updateLogMessage(mLogTextView, "An error happen : "
                                + e.getClass().getName() + " | Reason : " + e.getMessage() + "\n", false);
                    }
                });
        builder.setNegativeButton(android.R.string.cancel,
                (dialog, which) -> dialog.cancel());

        android.app.AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dialog1 -> Utils.showKeyboard(mActivity, pinCodeTxt));
        dialog.show();
    }

    /**
     * To perform change pin:
     * 1. Get current Token Device
     * 2. Call changePin API by providing current and new PIN
     *
     * @param oldPin      The old PIN text
     * @param newPin      The new PIN text
     * @param oldPinInput The old PIN input
     * @param newPinInput The new PIN input
     */
    private void changePin(
            String oldPin, String newPin,
            ProtectorAuthInput oldPinInput, ProtectorAuthInput newPinInput
    ) throws FastTrackException {

        String tkDeviceName = getCurrentTokenDevice();

        switch (mCurrentOtpType) {
            case OATH_TOTP:
            case OATH_OCRA: {
                OathTokenDevice tokenDevice = mOathMobileProtector.getTokenDevice(tkDeviceName, null);
                if (tokenDevice == null)
                    break;

                if (oldPin != null && newPin != null)
                    tokenDevice.changePin(oldPin, newPin);
                else if (oldPinInput != null && newPinInput != null)
                    tokenDevice.changePin(oldPinInput, newPinInput);
                break;
            }

            case CAP: {
                CapTokenDevice tokenDevice = mCapMobileProtector.getTokenDevice(tkDeviceName, null);
                if (tokenDevice == null)
                    break;

                if (oldPin != null && newPin != null)
                    tokenDevice.changePin(oldPin, newPin);
                else if (oldPinInput != null && newPinInput != null)
                    tokenDevice.changePin(oldPinInput, newPinInput);
                break;
            }
        }
    }
    //endregion

    //region Activate Biometric

    /**
     * To activate Biometric Authentication:
     * 1. Get current Token Device
     * 2. Check if it has been activated previously
     * 3. Make sure the PIN provided is correct by Authenticating to the Server
     * 4. Call activate API by providing PIN value
     *
     * @param pinText  The PIN text value
     * @param pinInput The PIN input
     */
    private void activateBiometric(
            String pinText,
            ProtectorAuthInput pinInput
    ) throws FastTrackException {

        String tkDeviceName = getCurrentTokenDevice();
        if (tkDeviceName == null) {
            mMyLogger.updateLogMessage(mLogTextView, "No TokenDevice selected.");
            return;
        }

        mMyLogger.updateLogTitle(mLogTextView, "Activate Biometric");
        switch (mCurrentOtpType) {
            case OATH_TOTP:
            case OATH_OCRA: {
                OathTokenDevice tokenDevice = mOathMobileProtector.getTokenDevice(tkDeviceName, null);
                if (tokenDevice == null)
                    break;

                if (!canAuthenticate(mOathMobileProtector)) {
                    mMyLogger.updateLogMessage(mLogTextView,
                            "Biometric not supported&configured on this device!");
                    break;
                }

                if (isBioModeActivated(tokenDevice)) {
                    mMyLogger.updateLogMessage(mLogTextView,
                            "Biometric already activated for token " + tkDeviceName);
                    break;
                }

                activeBioMode(tokenDevice, pinText, pinInput);
                mMyLogger.updateLogMessage(mLogTextView,
                        "Biometric activated for token " + tkDeviceName);
                break;
            }

            case CAP: {
                CapTokenDevice tokenDevice = mCapMobileProtector.getTokenDevice(tkDeviceName, null);
                if (tokenDevice == null)
                    break;

                if (!canAuthenticate(mCapMobileProtector)) {
                    mMyLogger.updateLogMessage(mLogTextView,
                            "Biometric not supported&configured on this device!");
                    break;
                }

                if (isBioModeActivated(tokenDevice)) {
                    mMyLogger.updateLogMessage(mLogTextView,
                            "Biometric already activated for token " + tkDeviceName);
                    break;
                }

                activeBioMode(tokenDevice, pinText, pinInput);
                mMyLogger.updateLogMessage(mLogTextView,
                        "Biometric activated for token " + tkDeviceName);
                break;
            }
        }
    }

    @SuppressWarnings("deprecation")
    private boolean canAuthenticate(MobileProtector protector) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P)
            return protector.isBioFingerprintModeSupported() && protector.isBioFingerprintModeConfigured();

        return protector.canBiometricAuthenticate() == 0;
    }

    @SuppressWarnings("deprecation")
    private boolean isBioModeActivated(TokenDevice tokenDevice) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P)
            return tokenDevice.isBioFingerprintModeActivated();

        return tokenDevice.isBiometricModeActivated();
    }

    @SuppressWarnings("deprecation")
    private void activeBioMode(TokenDevice tokenDevice, String pinText, ProtectorAuthInput pinInput)
            throws FastTrackException {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            if (pinText != null)
                tokenDevice.activateBioFingerprintMode(pinText);
            else
                tokenDevice.activateBioFingerprintMode(pinInput);

            return;
        }

        if (pinText != null)
            tokenDevice.activateBiometricMode(pinText);
        else
            tokenDevice.activateBiometricMode(pinInput);
    }
    //endregion

    //region Deactivate Biometric

    /**
     * To activate Biometric Authentication:
     * 1. Get current Token Device
     * 2. Check if it has been activated previously
     * 3. Call de-activate API by providing PIN value
     */
    private void deactivateBiometric() throws FastTrackException {
        String tkDeviceName = getCurrentTokenDevice();
        if (tkDeviceName == null) {
            mMyLogger.updateLogMessage(mLogTextView, "No TokenDevice selected.");
            return;
        }

        mMyLogger.updateLogTitle(mLogTextView, "Deactivate Biometric");
        switch (mCurrentOtpType) {
            case OATH_TOTP:
            case OATH_OCRA: {
                OathTokenDevice oathTokenDevice = mOathMobileProtector.getTokenDevice(tkDeviceName, null);
                if (oathTokenDevice == null)
                    break;

                if (!isBioModeActivated(oathTokenDevice)) {
                    mMyLogger.updateLogMessage(mLogTextView,
                            "Biometric authentication is not activated for token device " + tkDeviceName);
                    break;
                }

                deActiveBioMode(oathTokenDevice);
                mMyLogger.updateLogMessage(mLogTextView,
                        "Biometric authentication is deactivated for token device " + tkDeviceName);
                break;
            }

            case CAP: {
                CapTokenDevice capTokenDevice = mCapMobileProtector.getTokenDevice(tkDeviceName, null);
                if (capTokenDevice == null)
                    break;

                if (!isBioModeActivated(capTokenDevice)) {
                    mMyLogger.updateLogMessage(mLogTextView,
                            "Biometric authentication is not activated for token device " + tkDeviceName);
                    break;
                }

                deActiveBioMode(capTokenDevice);
                mMyLogger.updateLogMessage(mLogTextView,
                        "Biometric authentication is deactivated for token device " + tkDeviceName);
                break;
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void deActiveBioMode(TokenDevice tokenDevice) throws FastTrackException {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            tokenDevice.deActivateBioFingerprintMode();
        } else {
            tokenDevice.deActivateBiometricMode();
        }
    }

    //endregion

    //region Get OTPs - Biometric
    /**
     * To perform OTP generation with Biometric Authentication:
     * 1. Prepare UI callback
     * 2. Make sure Biometric has been activated previously
     * 3. Get Token Device to be used to generate OTP
     * 4. Call authenticate API
     * 5. Upon successful authentication, generate OTP by passing the authentication
     */
    private final BioMetricFragment.BioFpFragmentCallback bioFpFragmentCallback = this::cancelBioFingerprintPrompt;

    @SuppressWarnings("deprecation")
    private final BioFingerprintAuthenticationCallbacks bioFpCallbacks = new BioFingerprintAuthenticationCallbacks() {
        @Override
        public void onSuccess(ProtectorAuthInput protectorAuthInput) {
            if (bioMetricFragment != null)
                bioMetricFragment.dismiss();

            try {
                getOtpUsingBiometric(protectorAuthInput);
            } catch (FastTrackException e) {
                // Print the error
                mMyLogger.updateLogMessage(mLogTextView, "An error happen : "
                        + e.getClass().getName() + " | Reason : " + e.getMessage() + "\n", false);
            }
        }

        @Override
        public void onStartFPSensor() {
            bioMetricFragment.show(getFragmentManager(), "bioMetricFragment");
        }

        @Override
        @SuppressWarnings("deprecation")
        public void onAuthenticationStatus(int status, String message) {
            if (status != BioFingerprintAuthenticationStatus.BIO_FINGERPRINT_CANCELED) {
                bioMetricFragment.setPromptText(message);
                return;
            }

            if (bioMetricFragment != null
                    && bioMetricFragment.getDialog() != null
                    && bioMetricFragment.getDialog().isShowing()) {
                bioMetricFragment.getDialog().dismiss();
            }

            showPinFallbackDialog();
        }
    };

    private void cancelBioFingerprintPrompt() {
        if (!mCancellationSignal.isCanceled())
            mCancellationSignal.cancel();
    }

    private final BiometricAuthenticationCallbacks bioMpCallbacks =
            new com.gemalto.idp.mobile.fasttrack.protector.BiometricAuthenticationCallbacks() {
                @Override
                public void onSuccess(ProtectorAuthInput protectorAuthInput) {
                    try {
                        getOtpUsingBiometric(protectorAuthInput);
                    } catch (FastTrackException e) {
                        // Print the error
                        mMyLogger.updateLogMessage(mLogTextView, "An error happen : " +
                                e.getClass().getName() + " | Reason : " + e.getMessage() + "\n", false);
                    }
                }

                @Override
                public void onAuthenticationStatus(int status, String message) {
                    if (status == BiometricAuthenticationStatus.BIOMETRIC_CANCELED) {
                        showPinFallbackDialog();
                    } else {
                        mMyLogger.updateLogMessage(mLogTextView, message,
                                BiometricAuthenticationStatus.BIO_AUTHENTICATION_SUCCESS == status);
                    }
                }
            };

    @SuppressWarnings("deprecation")
    private void getOtpByBiometric() throws FastTrackException {
        String tkDeviceName = getCurrentTokenDevice();
        if (tkDeviceName == null) {
            mMyLogger.updateLogMessage(mLogTextView, "No TokenDevice selected.");
            return;
        }

        mMyLogger.updateLogTitle(mLogTextView, "OTP using Biometric");

        CancellationSignal cancellationSignal = new CancellationSignal();
        switch (mCurrentOtpType) {
            case OATH_TOTP:
            case OATH_OCRA: {
                OathTokenDevice tokenDevice = mOathMobileProtector.getTokenDevice(tkDeviceName, null);
                if (tokenDevice == null)
                    break;

                if (!isBioModeActivated(tokenDevice)) {
                    mMyLogger.updateLogMessage(mLogTextView,
                            "Biometric authentication is not activated for token device " + tkDeviceName);
                    break;
                }

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                    tokenDevice.authenticateWithBioFingerprint(mCancellationSignal, bioFpCallbacks);
                    break;
                }

                tokenDevice.authenticateWithBiometric(
                        "Test Biometric",
                        "Login with biometrics",
                        "Please use your biometric to verify your identity",
                        "Cancel",
                        cancellationSignal,
                        bioMpCallbacks);
                break;
            }

            case CAP: {
                CapTokenDevice tokenDevice = mCapMobileProtector.getTokenDevice(tkDeviceName, null);
                if (tokenDevice == null)
                    break;

                if (!isBioModeActivated(tokenDevice)) {
                    mMyLogger.updateLogMessage(mLogTextView,
                            "Biometric authentication is not activated for token device " + tkDeviceName);
                    break;
                }

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                    tokenDevice.authenticateWithBioFingerprint(mCancellationSignal, bioFpCallbacks);
                    break;
                }

                tokenDevice.authenticateWithBiometric(
                        "Test Biometric",
                        "Login with biometrics",
                        "Please use your biometric to verify your identity",
                        "Cancel",
                        cancellationSignal,
                        bioMpCallbacks);
                break;
            }
        }
    }

    private void showPinFallbackDialog() {
        LayoutInflater li = LayoutInflater.from(mActivity);
        View promptsView = li.inflate(R.layout.pin_fallback_dialog, null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mActivity);

        // set prompts.xml to alert dialog builder
        alertDialogBuilder.setView(promptsView);

        // set dialog message
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK",
                        (dialog, id) -> displayPinDialog(GET_OTP))
                .setNegativeButton("Cancel",
                        (dialog, id) -> dialog.cancel());

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();
        // show it
        alertDialog.show();
    }

    private void getOtpUsingBiometric(ProtectorAuthInput protectorAuthInput)
            throws FastTrackException {
        switch (mCurrentOtpType) {
            case OATH_TOTP:
                generateOathTotpByBiometric(protectorAuthInput);
                break;

            case OATH_OCRA:
                generateOathOcraByBiometric(protectorAuthInput);
                break;

            case CAP:
                generateCapOtpByBiometric(protectorAuthInput);
                break;
        }
    }

    private void generateOathTotpByBiometric(ProtectorAuthInput protectorAuthInput)
            throws FastTrackException {
        String currentTokenDevice = getCurrentTokenDevice();
        OathTokenDevice oathTokenDevice = mOathMobileProtector.getTokenDevice(currentTokenDevice, null);
        if (oathTokenDevice == null)
            return;

        // OTP
        String otp = oathTokenDevice.getOtp(protectorAuthInput);
        mMyLogger.updateLog(mLogTextView, "Get OTP Biometric",
                "Token: " + currentTokenDevice + "\n" + "OTP: " + otp);
    }

    private void generateOathOcraByBiometric(ProtectorAuthInput protectorAuthInput)
            throws FastTrackException {
        String currentTokenDevice = getCurrentTokenDevice();

        OathTokenDevice oathTokenDevice = mOathMobileProtector.getTokenDevice(currentTokenDevice, null);
        if (oathTokenDevice == null)
            return;

        // Generate the OTP
        String serverChallenge = "000000003";
        String clientChallenge = "000000003";
        String password = "password";
        String session = "\u20ac" + "10"; // (Euro) E2 82 AC + 31 + 30
        byte[] passwordHash = oathTokenDevice.getOcraPasswordHash(password);
        String ocraOtp = oathTokenDevice.getOcraOtp(protectorAuthInput, serverChallenge, clientChallenge, passwordHash, session);
        mMyLogger.updateLog(mLogTextView, "Get OTP Biometric",
                "Token: " + currentTokenDevice + "\n" + "OCRA OTP: " + ocraOtp);
    }

    private void generateCapOtpByBiometric(ProtectorAuthInput protectorAuthInput)
            throws FastTrackException {
        String tkDeviceName = getCurrentTokenDevice();
        CapTokenDevice tokenDevice = mCapMobileProtector.getTokenDevice(tkDeviceName, null);
        if (tokenDevice == null)
            return;

        mMyLogger.updateLog(mLogTextView, "Get OTP Biometric", "Current Token: " + tkDeviceName);

        initCapValue();

        // Mode 1
        String otpMode1 = tokenDevice.getOtpMode1(protectorAuthInput, challenge, amount, currency);
        mMyLogger.updateLogMessage(mLogTextView, "OTP Mode1: " + otpMode1);

        // Mode 2
        String otpMode2 = tokenDevice.getOtpMode2(protectorAuthInput);
        mMyLogger.updateLogMessage(mLogTextView, "OTP Mode2: " + otpMode2);

        // Mode 2 tds
        String otpMode2Tds = tokenDevice.getOtpMode2Tds(protectorAuthInput, defaultTds);
        mMyLogger.updateLogMessage(mLogTextView, "OTP Mode2Tds: " + otpMode2Tds);

        // Mode 3
        String otpMode3 = tokenDevice.getOtpMode3(protectorAuthInput, challenge);
        mMyLogger.updateLogMessage(mLogTextView, "OTP Mode3: " + otpMode3);
    }
    //endregion

    //region Remove token device
    private void removeTokenDevice() throws FastTrackException {

        Set<String> tokenNames = getTokenNames(mCurrentOtpType);
        mMyLogger.updateLogTitle(mLogTextView, "Removed TokenDevices");
        switch (mCurrentOtpType) {
            case OATH_TOTP:
            case OATH_OCRA: {
                for (String tokenName : tokenNames) {
                    mOathMobileProtector.removeTokenDevice(tokenName);
                    mMyLogger.updateLogMessage(mLogTextView, "Token: " + tokenName);
                }
                break;
            }

            case CAP: {
                for (String tokenName : tokenNames) {
                    mCapMobileProtector.removeTokenDevice(tokenName);
                    mMyLogger.updateLogMessage(mLogTextView, "Token: " + tokenName);
                }
                break;
            }
        }

        mTokenSpinner.setAdapter(null);
    }
    //endregion
}
