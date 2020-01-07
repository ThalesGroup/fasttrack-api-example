package com.gemalto.idp.mobile.fasttrack.example.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.os.CancellationSignal;
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

import com.gemalto.idp.mobile.fasttrack.FastTrack;
import com.gemalto.idp.mobile.fasttrack.FastTrackException;
import com.gemalto.idp.mobile.fasttrack.example.ProtectorConfigurations;
import com.gemalto.idp.mobile.fasttrack.example.R;
import com.gemalto.idp.mobile.fasttrack.example.Utils;
import com.gemalto.idp.mobile.fasttrack.protector.BioFingerprintAuthenticationCallbacks;
import com.gemalto.idp.mobile.fasttrack.protector.BioFingerprintAuthenticationStatus;
import com.gemalto.idp.mobile.fasttrack.protector.MobileProtector;
import com.gemalto.idp.mobile.fasttrack.protector.ProtectorAuthInput;
import com.gemalto.idp.mobile.fasttrack.protector.cap.CapMobileProtector;
import com.gemalto.idp.mobile.fasttrack.protector.cap.CapSettings;
import com.gemalto.idp.mobile.fasttrack.protector.cap.CapTokenDevice;
import com.gemalto.idp.mobile.fasttrack.protector.cap.CapTokenDeviceCreationCallback;
import com.gemalto.idp.mobile.fasttrack.protector.oath.OathMobileProtector;
import com.gemalto.idp.mobile.fasttrack.protector.oath.OathTokenDevice;
import com.gemalto.idp.mobile.fasttrack.protector.oath.OathTokenDeviceCreationCallback;
import com.gemalto.idp.mobile.fasttrack.protector.oath.OcraSettings;
import com.gemalto.idp.mobile.fasttrack.protector.oath.TotpSettings;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Currency;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import static com.gemalto.idp.mobile.fasttrack.example.ProtectorConfigurations.ENU_OTP_TYPE.CAP;
import static com.gemalto.idp.mobile.fasttrack.example.ProtectorConfigurations.ENU_OTP_TYPE.OATH_OCRA;
import static com.gemalto.idp.mobile.fasttrack.example.ProtectorConfigurations.ENU_OTP_TYPE.OATH_TOTP;
import static com.gemalto.idp.mobile.fasttrack.example.ProtectorConfigurations.PinUsage.ACTIVATE_BIOFINGERPRINT;
import static com.gemalto.idp.mobile.fasttrack.example.ProtectorConfigurations.PinUsage.CHANGE_PIN;
import static com.gemalto.idp.mobile.fasttrack.example.ProtectorConfigurations.PinUsage.GET_OTP;
import static com.gemalto.idp.mobile.fasttrack.example.ProtectorConfigurations.getEpsDomain;
import static com.gemalto.idp.mobile.fasttrack.example.ProtectorConfigurations.getEpsRsaKeyExponent;
import static com.gemalto.idp.mobile.fasttrack.example.ProtectorConfigurations.getEpsRsaKeyId;
import static com.gemalto.idp.mobile.fasttrack.example.ProtectorConfigurations.getEpsRsaKeyModulus;
import static com.gemalto.idp.mobile.fasttrack.example.ProtectorConfigurations.getEpsUrl;

public class FragmentTabProtector extends Fragment implements AdapterView.OnItemSelectedListener {

    private Utils.MyLogger mMyLogger = Utils.MyLogger.getsInstance();

    //region Member - UI elements
    private EditText mRegistrationCode;
    private Button mProvisionButton;
    private Button mGetOtpPinButton;
    private Button mChangePinButton;
    private Button mActivateBioFingerprintBtn;
    private Button mDeactivateBioFingerprintBtn;
    private Button mGetOtpBioFingerprintBtn;
    private Button mRemoveTokenDevice;
    private Button mListTokenDevice;
    private TextView mLogTextView;
    private RadioGroup mRadioGroup;
    private Spinner mTokenSpinner;

    private Activity mActivity;
    private BioMetricFragment bioMetricFragment;
    private CancellationSignal mCancellationSignal = new CancellationSignal();
    private ProtectorConfigurations.ENU_OTP_TYPE mCurentOtpType;
    //endregion

    //region Member
    private String challenge, amount;
    private Currency currency;
    private List<String> defaultTds;

    private CapMobileProtector mCapMobileProtector;
    private List<String> mCapTokenDevices = new ArrayList<>();

    // OATH-TOTP and OATH-OCRA use the same oath protector
    private OathMobileProtector mOathMobileProtector;
    private List<String> mTotpTokenDevices = new ArrayList<>();
    private List<String> mOcraTokenDevices = new ArrayList<>();

    private String mCurrentPin;
    //endregion

    //region Override
    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             @Nullable final ViewGroup container,
                             @Nullable final Bundle savedInstanceState) {
        final View retValue = initGui(inflater);

        mActivity = getActivity();

        try {
            initMobileProtectors();
            try {
                updateTokenSpinner(mCurentOtpType, getCurrentTokenDevice());
            } catch (FastTrackException e) {
                // Print the error
                mMyLogger.updateLogMessage(mLogTextView, "An error happen : " + e.getClass().getName() + " | Reason : " + e.getMessage() + "\n", false);
            }
        } catch (MalformedURLException e) {
            // Print the error
            mMyLogger.updateLogMessage(mLogTextView, "An error happen : " + e.getClass().getName() + " | Reason : " + e.getMessage() + "\n", false);
        }

        return retValue;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        bioMetricFragment = BioMetricFragment.newInstance(bioFpFragmentCallback, "Generate OTP");
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        // On selecting a spinner item
        String item = parent.getItemAtPosition(position).toString();

        // Showing selected spinner item
        Toast.makeText(parent.getContext(), "Selected: " + item, Toast.LENGTH_LONG).show();
    }

    public void onNothingSelected(AdapterView<?> arg0) {
    }
    //endregion

    private View initGui(final LayoutInflater inflater) {
        final View retValue = inflater.inflate(R.layout.fragment_tab_protector, null);

        mRegistrationCode = retValue.findViewById(R.id.protector_registration_code);
        mProvisionButton = retValue.findViewById(R.id.provision);
        mGetOtpPinButton = retValue.findViewById(R.id.getOtpByPin);
        mChangePinButton = retValue.findViewById(R.id.changePin);
        mActivateBioFingerprintBtn = retValue.findViewById(R.id.activateBioFingerprint);
        mDeactivateBioFingerprintBtn = retValue.findViewById(R.id.deactivateBioFingerprint);
        mGetOtpBioFingerprintBtn = retValue.findViewById(R.id.getOtpByBioFingerprint);
        mRemoveTokenDevice = retValue.findViewById(R.id.removeTokenDevice);
        mListTokenDevice = retValue.findViewById(R.id.listTokenDevices);
        mLogTextView = retValue.findViewById(R.id.log);
        mRadioGroup = retValue.findViewById(R.id.radio_otp_type);

        mTokenSpinner = retValue.findViewById(R.id.tokenSpinner);

        mRadioGroup.check(R.id.radioOathTotp);
        mCurentOtpType = OATH_TOTP;

        setListeners();

        return retValue;
    }

    private void updateTokenSpinner(ProtectorConfigurations.ENU_OTP_TYPE otp_type, String selectedToken) throws FastTrackException {

        Set<String> tokenDevices = getTokenNames(otp_type);
        // Spinner click listener
        mTokenSpinner.setOnItemSelectedListener(this);

        // Creating adapter for spinner initialising with totp token devices.
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(mActivity, android.R.layout.simple_spinner_item, new ArrayList<>(tokenDevices));

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

        // 2 Initialization of Mobile Protector which can be either OATH or CAP
        // Normally we only need to choose either one of them

        // 2.a Initialization for Oath Mobile Protector
        mOathMobileProtector = FastTrack.getInstance().getOathMobileProtectorBuilder(
                new URL(getEpsUrl()),
                getEpsDomain(),
                MobileProtector.ProvisioningProtocol.PROTOCOL_V5,
                getEpsRsaKeyId(),
                getEpsRsaKeyExponent(),
                getEpsRsaKeyModulus()
        ).build();

        // 2.b Initialization for CAP Mobile Protector
        mCapMobileProtector = FastTrack.getInstance().getCapMobileProtectorBuilder(
                new URL(getEpsUrl()),
                getEpsDomain(),
                MobileProtector.ProvisioningProtocol.PROTOCOL_V5,
                getEpsRsaKeyId(),
                getEpsRsaKeyExponent(),
                getEpsRsaKeyModulus()
        ).build();
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

    private void setListeners() {
        mProvisionButton.setOnClickListener(v -> {

            mMyLogger.updateLogTitle(mLogTextView, "Provisioning " + mCurentOtpType + " Token");

            switch (mCurentOtpType) {
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
            displayPinDialog(GET_OTP);
        });

        mChangePinButton.setOnClickListener(v -> {
            displayPinDialog(CHANGE_PIN);
        });

        mActivateBioFingerprintBtn.setOnClickListener(v -> {
            displayPinDialog(ACTIVATE_BIOFINGERPRINT);
        });

        mDeactivateBioFingerprintBtn.setOnClickListener(v -> {
            try {
                deactivateBioFingerprint();
            } catch (FastTrackException e) {
                // Print the error
                mMyLogger.updateLogMessage(mLogTextView, "An error happen : " + e.getClass().getName() + " | Reason : " + e.getMessage() + "\n", false);
            }
        });

        mGetOtpBioFingerprintBtn.setOnClickListener(v -> {
            try {
                promptBioFingerprint();
            } catch (FastTrackException e) {
                // Print the error
                mMyLogger.updateLogMessage(mLogTextView, "An error happen : " + e.getClass().getName() + " | Reason : " + e.getMessage() + "\n", false);
            }
        });

        mListTokenDevice.setOnClickListener(v -> {
            try {
                mMyLogger.updateLogTitle(mLogTextView, "TokenDevices");
                Set<String> remainingTokens = getTokenNames(mCurentOtpType);
                for (String tokenName : remainingTokens) { // pick up the first token.
                    mMyLogger.updateLogMessage(mLogTextView, tokenName);
                }
            } catch (FastTrackException e) {
                // Print the error
                mMyLogger.updateLogMessage(mLogTextView, "An error happen : " + e.getClass().getName() + " | Reason : " + e.getMessage() + "\n", false);
            }
        });

        mRemoveTokenDevice.setOnClickListener(v -> {
            try {
                removeTokenDevice();
            } catch (FastTrackException e) {
                // Print the error
                mMyLogger.updateLogMessage(mLogTextView, "An error happen : " + e.getClass().getName() + " | Reason : " + e.getMessage() + "\n", false);
            }
        });

        mRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            switch (checkedId) {
                case R.id.radioOathTotp:
                    mCurentOtpType = OATH_TOTP;
                    break;
                case R.id.radioOathOcra:
                    mCurentOtpType = OATH_OCRA;
                    break;
                case R.id.radioCap:
                    mCurentOtpType = CAP;
                    break;
            }

            try {
                updateTokenSpinner(mCurentOtpType, getCurrentTokenDevice());
            } catch (FastTrackException e) {
                // Print the error
                mMyLogger.updateLogMessage(mLogTextView, "An error happen : " + e.getClass().getName() + " | Reason : " + e.getMessage() + "\n", false);
            }
        });
    }

    private String getCurrentTokenDevice() {
        String currentTokenDevice = null;

        if (mTokenSpinner.getSelectedItem() != null) {
            currentTokenDevice = mTokenSpinner.getSelectedItem().toString();
        }

        return currentTokenDevice;
    }

    private Set<String> getTokenNames(ProtectorConfigurations.ENU_OTP_TYPE otpType) throws FastTrackException {

        Set<String> retTokenNames = new HashSet<>();
        Set<String> tokenNames;

        switch (otpType) {
            case OATH_TOTP:
                tokenNames = mOathMobileProtector.getTokenDeviceNames();
                for (String tokenName : tokenNames) { // pick up the first token.
                    if (tokenName.startsWith("TOKEN_TOTP_")) {
                        retTokenNames.add(tokenName);
                    }
                }
                break;
            case OATH_OCRA:
                tokenNames = mOathMobileProtector.getTokenDeviceNames();
                for (String tokenName : tokenNames) { // pick up the first token.
                    if (tokenName.startsWith("TOKEN_OCRA_")) {
                        retTokenNames.add(tokenName);
                    }
                }
                break;
            case CAP:
                tokenNames = mCapMobileProtector.getTokenDeviceNames();
                for (String tokenName : tokenNames) { // pick up the first token.
                    if (tokenName.startsWith("TOKEN_CAP_")) {
                        retTokenNames.add(tokenName);
                    }
                }
                break;
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
                    mMyLogger.updateLog(mLogTextView, "Provision OATH_TOTP Success", "Token " + oathTokenName + " created.");
                    try {
                        updateTokenSpinner(OATH_TOTP, oathTokenName);
                    } catch (FastTrackException e) {
                        // Print the error
                        mMyLogger.updateLogMessage(mLogTextView, "An error happen : " + e.getClass().getName() + " | Reason : " + e.getMessage() + "\n", false);
                    }
                }

                @Override
                public void onError(FastTrackException e) {
                    // Print the error
                    mMyLogger.updateLogMessage(mLogTextView, "An error happen : " + e.getClass().getName() + " | Reason : " + e.getMessage() + "\n", false);
                }
            };

            // 3.3 Call Provision API to start provisioning
            // This API call will save the settings locally
            mOathMobileProtector.provision(oathTokenName, mRegistrationCode.getText().toString(), totpSettings, callback);

        } catch (Exception e) {
            // Print the error
            mMyLogger.updateLogMessage(mLogTextView, "An error happen : " + e.getClass().getName() + " | Reason : " + e.getMessage() + "\n", false);
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
                    mMyLogger.updateLog(mLogTextView, "Provision OATH_OCRA Success", "Token " + oathTokenName + " created.");
                    try {
                        updateTokenSpinner(OATH_OCRA, oathTokenName);
                    } catch (FastTrackException e) {
                        // Print the error
                        mMyLogger.updateLogMessage(mLogTextView, "An error happen : " + e.getClass().getName() + " | Reason : " + e.getMessage() + "\n", false);
                    }
                }

                @Override
                public void onError(FastTrackException e) {
                    mMyLogger.updateLogMessage(mLogTextView, "An error happen : " + e.getClass().getName() + " | Reason : " + e.getMessage() + "\n", false);
                }
            };

            // 3.3 Call Provision API to start provisioning
            // This API call will save the settings locally
            mOathMobileProtector.provision(oathTokenName, mRegistrationCode.getText().toString(), ocraSettings, callback);

        } catch (Exception e) {
            // Print the error
            mMyLogger.updateLogMessage(mLogTextView, "An error happen : " + e.getClass().getName() + " | Reason : " + e.getMessage() + "\n", false);
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
                    mMyLogger.updateLog(mLogTextView, "Provision CAP Success", "Token " + capTokenName + " created.");
                    try {
                        updateTokenSpinner(CAP, capTokenName);
                    } catch (FastTrackException e) {
                        // Print the error
                        mMyLogger.updateLogMessage(mLogTextView, "An error happen : " + e.getClass().getName() + " | Reason : " + e.getMessage() + "\n", false);
                    }
                }

                @Override
                public void onError(FastTrackException e) {
                    // Print the error
                    mMyLogger.updateLogMessage(mLogTextView, "An error happen : " + e.getClass().getName() + " | Reason : " + e.getMessage() + "\n", false);
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
            mMyLogger.updateLogMessage(mLogTextView, "An error happen : " + e.getClass().getName() + " | Reason : " + e.getMessage() + "\n", false);
        }

    }
    //endregion Provision Tokens

    //region Get OTPs - PIN
    private void displayPinDialog(ProtectorConfigurations.PinUsage pinUsage) {

        String currentTokenDevice = getCurrentTokenDevice();
        if (currentTokenDevice == null) {
            mMyLogger.updateLogMessage(mLogTextView, "No TokenDevice selected.");
            return;
        }

        LayoutInflater li = LayoutInflater.from(mActivity);
        View promptsView = li.inflate(R.layout.pin_input_dialog, null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mActivity);

        // set prompts.xml to alertdialog builder
        alertDialogBuilder.setView(promptsView);

        final TextView userInputHint = promptsView.findViewById(R.id.pinUsageHint);
        switch (pinUsage) {
            case GET_OTP:
                userInputHint.setText(getString(R.string.enter_pin_to_get_otp));
                break;
            case CHANGE_PIN:
                userInputHint.setText(getString(R.string.txt_raw_pin_enter_old_pin_hint_label));
                break;
            case ACTIVATE_BIOFINGERPRINT:
                userInputHint.setText(getString(R.string.enter_pin_to_activate_biofingerprint));
                break;
        }

        final EditText userInput = promptsView.findViewById(R.id.editTextPin);

        // set dialog message
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK",
                        (dialog, id) -> {

                            mCurrentPin = userInput.getText().toString();

                            switch (pinUsage) {
                                case GET_OTP:
                                    try {
                                        getOtpUsingPin();
                                    } catch (FastTrackException e) {
                                        // Print the error
                                        mMyLogger.updateLogMessage(mLogTextView, "An error happen : " + e.getClass().getName() + " | Reason : " + e.getMessage() + "\n", false);
                                    }
                                    break;
                                case CHANGE_PIN:
                                    displayChangePinDialog();
                                    break;
                                case ACTIVATE_BIOFINGERPRINT:
                                    try {
                                        activateBioFingerprint();
                                    } catch (FastTrackException e) {
                                        // Print the error
                                        mMyLogger.updateLogMessage(mLogTextView, "An error happen : " + e.getClass().getName() + " | Reason : " + e.getMessage() + "\n", false);
                                    }
                                    break;
                            }
                        })
                .setNegativeButton("Cancel",
                        (dialog, id) -> dialog.cancel());

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();
        // show it
        alertDialog.show();

    }

    private void getOtpUsingPin() throws FastTrackException {
        String currentTokenDevice = getCurrentTokenDevice();
        if (currentTokenDevice == null) {
            mMyLogger.updateLogMessage(mLogTextView, "No TokenDevice selected.");
            return;
        }

        switch (mCurentOtpType) {
            case OATH_TOTP:
                generateOathTotpByPin();
                break;
            case OATH_OCRA:
                generateOathOcraByPin();
                break;
            case CAP:
                generateCapOtpByPin();
                break;
        }
    }

    private void generateOathTotpByPin() throws FastTrackException {
        // 4.1 Get Token Device to be used to generate OTP
        OathTokenDevice oathTokenDevice = mOathMobileProtector.getTokenDevice(getCurrentTokenDevice(), null);

        // 4.2 Generate OTP by passing the authentication: PIN
        String otp = oathTokenDevice.getOtp(mCurrentPin);
        mMyLogger.updateLog(mLogTextView, "Get OTP PIN", "Token: " + getCurrentTokenDevice() + "\n" + "OTP: " + otp);
    }

    private void generateOathOcraByPin() throws FastTrackException {

        // 4.1 Get Token Device to be used to generate OTP
        OathTokenDevice oathTokenDevice = mOathMobileProtector.getTokenDevice(getCurrentTokenDevice(), null);

        // Generate the OTP
        String serverChallenge = "000000003";
        String clientChallenge = "000000003";
        String password = "password";
        String session = "\u20ac" + "10"; // (Euro) E2 82 AC + 31 + 30
        byte[] passwordHash = oathTokenDevice.getOcraPasswordHash(password);

        // 4.2 Generate OTP by passing the authentication: PIN
        String ocraOtp = oathTokenDevice.getOcraOtp(mCurrentPin, serverChallenge, clientChallenge, passwordHash, session);
        mMyLogger.updateLog(mLogTextView, "Get OTP PIN", "Token: " + getCurrentTokenDevice() + "\n" + "OCRA OTP: " + ocraOtp);
    }

    private void generateCapOtpByPin() throws FastTrackException {

        String currentTokenDevice = getCurrentTokenDevice();

        // 4.1 Get Token Device to be used to generate OTP
        CapTokenDevice capTokenDevice = mCapMobileProtector.getTokenDevice(currentTokenDevice, null);

        mMyLogger.updateLog(mLogTextView, "Get OTP PIN", "Current Token: " + currentTokenDevice);

        initCapValue();

        //Mode 1
        // 4.2 Generate OTP by passing the authentication: PIN
        String otpMode1 = capTokenDevice.getOtpMode1(mCurrentPin, challenge, amount, currency);
        mMyLogger.updateLogMessage(mLogTextView, "OTP Mode1: " + otpMode1);

        //Mode 2
        // 4.2 Generate OTP by passing the authentication: PIN
        String otpMode2 = capTokenDevice.getOtpMode2(mCurrentPin);
        mMyLogger.updateLogMessage(mLogTextView, "OTP Mode2: " + otpMode2);

        //Mode 2 tds
        // 4.2 Generate OTP by passing the authentication: PIN
        String otpMode2Tds = capTokenDevice.getOtpMode2Tds(mCurrentPin, defaultTds);
        mMyLogger.updateLogMessage(mLogTextView, "OTP Mode2Tds: " + otpMode2Tds);

        //Mode 3
        // 4.2 Generate OTP by passing the authentication: PIN
        String otpMode3 = capTokenDevice.getOtpMode3(mCurrentPin, challenge);
        mMyLogger.updateLogMessage(mLogTextView, "OTP Mode3: " + otpMode3);
    }
    //endregion

    //region Change PIN

    /**
     * Displaying the change pin ***NOT*** using SecurePinPad
     */
    @SuppressWarnings("InflateParams")
    private void displayChangePinDialog() {
        String currentTokenDevice = getCurrentTokenDevice();
        if (currentTokenDevice == null) {
            mMyLogger.updateLogMessage(mLogTextView, "No TokenDevice selected.");
            return;
        }

        mMyLogger.updateLogTitle(mLogTextView, "Change PIN");

        LayoutInflater inflater = LayoutInflater.from(mActivity);
        View dialogLayout = inflater.inflate(R.layout.verifycode_input_dialog, null);

        final EditText pinCodeTxt = dialogLayout.findViewById(R.id.txt_code);
        pinCodeTxt.setHint(R.string.txt_raw_pin_enter_pin_hint_label);
        pinCodeTxt.setFilters(new InputFilter[]{
                new InputFilter.LengthFilter(8),
        });

        final EditText confirmPinCodeTxt = dialogLayout.findViewById(R.id.txt_confirm_code);
        confirmPinCodeTxt.setVisibility(View.VISIBLE);
        confirmPinCodeTxt.setFilters(new InputFilter[]{
                new InputFilter.LengthFilter(8),
        });

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
                    } else {
                        try {
                            changePin(mCurrentPin, pin1);
                        } catch (FastTrackException e) {
                            // Print the error
                            mMyLogger.updateLogMessage(mLogTextView, "An error happen : " + e.getClass().getName() + " | Reason : " + e.getMessage() + "\n", false);
                        }
                        mMyLogger.updateLogMessage(mLogTextView, "Change PIN done!");
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
     * 1 Get current Token Device
     * 2 Call changePin API by providing current and new PIN
     */
    private void changePin(String oldPin, String newPin) throws FastTrackException {

        String currentTokenDevice = getCurrentTokenDevice();

        switch (mCurentOtpType) {
            case OATH_TOTP:
            case OATH_OCRA:
                OathTokenDevice oathTokenDevice = mOathMobileProtector.getTokenDevice(currentTokenDevice, null);
                oathTokenDevice.changePin(oldPin, newPin);
                break;
            case CAP:
                CapTokenDevice capTokenDevice = mCapMobileProtector.getTokenDevice(currentTokenDevice, null);
                capTokenDevice.changePin(oldPin, newPin);
                break;
        }
    }
    //endregion

    //region Activate BioFingerprint

    /**
     * To activate Biometric Authentication:
     * 1 Get current Token Device
     * 2 Check if it has been activated previously
     * 3 Make sure the PIN provided is correct by Authenticating to the Server
     * 4 Call activate API by providing PIN value
     */
    private void activateBioFingerprint() throws FastTrackException {

        String currentTokenDevice = getCurrentTokenDevice();
        if (currentTokenDevice == null) {
            mMyLogger.updateLogMessage(mLogTextView, "No TokenDevice selected.");
            return;
        }

        mMyLogger.updateLogTitle(mLogTextView, "Activate BioFingerprint");
        switch (mCurentOtpType) {
            case OATH_TOTP:
            case OATH_OCRA:
                OathTokenDevice oathTokenDevice = mOathMobileProtector.getTokenDevice(currentTokenDevice, null);
                if (oathTokenDevice != null) {
                    if (!mOathMobileProtector.isBioFingerprintModeSupported()) {
                        mMyLogger.updateLogMessage(mLogTextView, "BioFingerprint not supported on this device!");
                    } else if (!mOathMobileProtector.isBioFingerprintModeConfigured()) {
                        mMyLogger.updateLogMessage(mLogTextView, "BioFingerprint not configured on this device!");
                    } else {
                        if (!oathTokenDevice.isBioFingerprintModeActivated()) {
                            // Make sure the current PIN provided is correct by verifying the OTP value against the Server
                            // before calling this API
                            oathTokenDevice.activateBioFingerprintMode(mCurrentPin);
                            mMyLogger.updateLogMessage(mLogTextView, "BioFingerprint activated for token " + currentTokenDevice);
                        } else {
                            mMyLogger.updateLogMessage(mLogTextView, "BioFingerprint already activated for token " + currentTokenDevice);
                        }

                    }
                }
                break;
            case CAP:
                CapTokenDevice capTokenDevice = mCapMobileProtector.getTokenDevice(currentTokenDevice, null);
                if (capTokenDevice != null) {
                    if (!mCapMobileProtector.isBioFingerprintModeSupported()) {
                        mMyLogger.updateLogMessage(mLogTextView, "BioFingerprint not supported on this device!");
                    } else if (!mCapMobileProtector.isBioFingerprintModeConfigured()) {
                        mMyLogger.updateLogMessage(mLogTextView, "BioFingerprint not configured on this device!");
                    } else {
                        if (!capTokenDevice.isBioFingerprintModeActivated()) {
                            // Make sure the current PIN provided is correct by verifying the OTP value against the Server
                            // before calling this API
                            capTokenDevice.activateBioFingerprintMode(mCurrentPin);
                            mMyLogger.updateLogMessage(mLogTextView, "BioFingerprint activated for token " + currentTokenDevice);
                        } else {
                            mMyLogger.updateLogMessage(mLogTextView, "BioFingerprint already activated for token " + currentTokenDevice);
                        }
                    }
                }
                break;
        }
    }
    //endregion

    //region Deactivate BioFingerprint

    /**
     * To activate Biometric Authentication:
     * 1 Get current Token Device
     * 2 Check if it has been activated previously
     * 3 Call de-activate API by providing PIN value
     */
    private void deactivateBioFingerprint() throws FastTrackException {
        String currentTokenDevice = getCurrentTokenDevice();
        if (currentTokenDevice == null) {
            mMyLogger.updateLogMessage(mLogTextView, "No TokenDevice selected.");
            return;
        }

        mMyLogger.updateLogTitle(mLogTextView, "Deactivate BioFingerprint");
        switch (mCurentOtpType) {
            case OATH_TOTP:
            case OATH_OCRA:
                OathTokenDevice oathTokenDevice = mOathMobileProtector.getTokenDevice(currentTokenDevice, null);
                if (oathTokenDevice != null) {
                    if (oathTokenDevice.isBioFingerprintModeActivated()) {
                        oathTokenDevice.deActivateBioFingerprintMode();
                        mMyLogger.updateLogMessage(mLogTextView, "BioFingerprint authentication is deactivated for token device " + currentTokenDevice);
                    } else {
                        mMyLogger.updateLogMessage(mLogTextView, "BioFingerprint authentication is not activated for token device " + currentTokenDevice);
                    }
                }
                break;
            case CAP:
                CapTokenDevice capTokenDevice = mCapMobileProtector.getTokenDevice(currentTokenDevice, null);
                if (capTokenDevice != null) {
                    if (capTokenDevice.isBioFingerprintModeActivated()) {
                        capTokenDevice.deActivateBioFingerprintMode();
                        mMyLogger.updateLogMessage(mLogTextView, "BioFingerprint authentication is deactivated for token device " + currentTokenDevice);
                    } else {
                        mMyLogger.updateLogMessage(mLogTextView, "BioFingerprint authentication is not activated for token device " + currentTokenDevice);
                    }
                }
                break;
        }
    }

    //endregion

    //region Get OTPs - BioFingerprint
    /**
     * To perform OTP generation with Biometric Authentication:
     * 1 Prepare UI callback
     * 2 Make sure Biometric has been activated previously
     * 3 Get Token Device to be used to generate OTP
     * 4 Call authenticate API
     * 5 Upon successful authentication, generate OTP by passing the authentication
     */
    private BioMetricFragment.BioFpFragmentCallback bioFpFragmentCallback = new BioMetricFragment.BioFpFragmentCallback() {
        @Override
        public void onCancel() {
            cancelBioFingerprintPrompt();
        }
    };

    private BioFingerprintAuthenticationCallbacks callbacks = new BioFingerprintAuthenticationCallbacks() {
        @Override
        public void onSuccess(ProtectorAuthInput protectorAuthInput) {
            if (bioMetricFragment != null) {
                bioMetricFragment.dismiss();
            }
            try {
                getOtpUsingBioFingerprint(protectorAuthInput);
            } catch (FastTrackException e) {
                // Print the error
                mMyLogger.updateLogMessage(mLogTextView, "An error happen : " + e.getClass().getName() + " | Reason : " + e.getMessage() + "\n", false);
            }
        }

        @Override
        public void onStartFPSensor() {
            bioMetricFragment.show(getFragmentManager(), "bioMetricFragment");
        }

        @Override
        public void onAuthenticationStatus(int i, String s) {
            if (i == BioFingerprintAuthenticationStatus.BIO_FINGERPRINT_CANCELED) {
                if (bioMetricFragment != null
                        && bioMetricFragment.getDialog() != null
                        && bioMetricFragment.getDialog().isShowing()) {
                    bioMetricFragment.getDialog().dismiss();
                }
            } else {
                bioMetricFragment.setPromptText(s);
            }

        }
    };

    private void cancelBioFingerprintPrompt() {
        if (mCancellationSignal != null && !mCancellationSignal.isCanceled()) {
            mCancellationSignal.cancel();
        }
    }

    private void promptBioFingerprint() throws FastTrackException {

        mMyLogger.updateLogTitle(mLogTextView, "OTP using BioFingerprint");

        String currentTokenDevice = getCurrentTokenDevice();
        if (currentTokenDevice == null) {
            mMyLogger.updateLogMessage(mLogTextView, "No TokenDevice selected.");
            return;
        }

        switch (mCurentOtpType) {
            case OATH_TOTP:
            case OATH_OCRA:
                OathTokenDevice oathTokenDevice = mOathMobileProtector.getTokenDevice(currentTokenDevice, null);
                if (oathTokenDevice.isBioFingerprintModeActivated()) {
                    oathTokenDevice.authenticateWithBioFingerprint(mCancellationSignal, callbacks);
                } else {
                    mMyLogger.updateLogMessage(mLogTextView, "BioFingerprint authentication is not activated for token device " + currentTokenDevice);
                }
                break;
            case CAP:
                CapTokenDevice capTokenDevice = mCapMobileProtector.getTokenDevice(currentTokenDevice, null);
                if (capTokenDevice.isBioFingerprintModeActivated()) {
                    capTokenDevice.authenticateWithBioFingerprint(mCancellationSignal, callbacks);
                } else {
                    mMyLogger.updateLogMessage(mLogTextView, "BioFingerprint authentication is not activated for token device " + currentTokenDevice);
                }
                break;
        }
    }

    private void getOtpUsingBioFingerprint(ProtectorAuthInput protectorAuthInput) throws FastTrackException {
        switch (mCurentOtpType) {
            case OATH_TOTP:
                generateOathTotpByBioFingerprint(protectorAuthInput);
                break;
            case OATH_OCRA:
                generateOathOcraByBioFingerprint(protectorAuthInput);
                break;
            case CAP:
                generateCapOtpByBioFingerprint(protectorAuthInput);
                break;
        }
    }

    private void generateOathTotpByBioFingerprint(ProtectorAuthInput protectorAuthInput) throws FastTrackException {
        String currentTokenDevice = getCurrentTokenDevice();
        OathTokenDevice oathTokenDevice = mOathMobileProtector.getTokenDevice(currentTokenDevice, null);

        // OTP
        String otp = oathTokenDevice.getOtp(protectorAuthInput);
        mMyLogger.updateLog(mLogTextView, "Get OTP BioFingerprint", "Token: " + currentTokenDevice + "\n" + "OTP: " + otp);
    }

    private void generateOathOcraByBioFingerprint(ProtectorAuthInput protectorAuthInput) throws FastTrackException {
        String currentTokenDevice = getCurrentTokenDevice();

        OathTokenDevice oathTokenDevice = mOathMobileProtector.getTokenDevice(currentTokenDevice, null);

        // Generate the OTP
        String serverChallenge = "000000003";
        String clientChallenge = "000000003";
        String password = "password";
        String session = "\u20ac" + "10"; // (Euro) E2 82 AC + 31 + 30
        byte[] passwordHash = oathTokenDevice.getOcraPasswordHash(password);
        String ocraOtp = oathTokenDevice.getOcraOtp(protectorAuthInput, serverChallenge, clientChallenge, passwordHash, session);
        mMyLogger.updateLog(mLogTextView, "Get OTP BioFingerprint", "Token: " + currentTokenDevice + "\n" + "OCRA OTP: " + ocraOtp);
    }

    private void generateCapOtpByBioFingerprint(ProtectorAuthInput protectorAuthInput) throws FastTrackException {
        String currentTokenDevice = getCurrentTokenDevice();
        CapTokenDevice capTokenDevice = mCapMobileProtector.getTokenDevice(currentTokenDevice, null);

        mMyLogger.updateLog(mLogTextView, "Get OTP BioFingerprint", "Current Token: " + currentTokenDevice);

        initCapValue();

        //Mode 1
        String otpMode1 = capTokenDevice.getOtpMode1(protectorAuthInput, challenge, amount, currency);
        mMyLogger.updateLogMessage(mLogTextView, "OTP Mode1: " + otpMode1);

        //Mode 2
        String otpMode2 = capTokenDevice.getOtpMode2(protectorAuthInput);
        mMyLogger.updateLogMessage(mLogTextView, "OTP Mode2: " + otpMode2);

        //Mode 2 tds
        String otpMode2Tds = capTokenDevice.getOtpMode2Tds(protectorAuthInput, defaultTds);
        mMyLogger.updateLogMessage(mLogTextView, "OTP Mode2Tds: " + otpMode2Tds);

        //Mode 3
        String otpMode3 = capTokenDevice.getOtpMode3(protectorAuthInput, challenge);
        mMyLogger.updateLogMessage(mLogTextView, "OTP Mode3: " + otpMode3);
    }
    //endregion

    //region Remove token device
    private void removeTokenDevice() throws FastTrackException {

        Set<String> tokenNames = getTokenNames(mCurentOtpType);
        mMyLogger.updateLogTitle(mLogTextView, "Removed TokenDevices");
        switch (mCurentOtpType) {
            case OATH_TOTP:
                for (String tokenName : tokenNames) { // pick up the first token.
                    mOathMobileProtector.removeTokenDevice(tokenName);
                    mMyLogger.updateLogMessage(mLogTextView, "Token: " + tokenName);
                }
                mTotpTokenDevices.clear();
                break;
            case OATH_OCRA:
                for (String tokenName : tokenNames) { // pick up the first token.
                    mOathMobileProtector.removeTokenDevice(tokenName);
                    mMyLogger.updateLogMessage(mLogTextView, "Token: " + tokenName);
                }
                mOcraTokenDevices.clear();
                break;
            case CAP:
                for (String tokenName : tokenNames) { // pick up the first token.
                    mCapMobileProtector.removeTokenDevice(tokenName);
                    mMyLogger.updateLogMessage(mLogTextView, "Token: " + tokenName);
                }
                mCapTokenDevices.clear();
                break;
        }
        mTokenSpinner.setAdapter(null);
    }
    //endregion


}
