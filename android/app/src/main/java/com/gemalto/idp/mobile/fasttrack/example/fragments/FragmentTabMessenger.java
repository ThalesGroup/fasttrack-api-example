package com.gemalto.idp.mobile.fasttrack.example.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.gemalto.idp.mobile.fasttrack.FastTrack;
import com.gemalto.idp.mobile.fasttrack.FastTrackException;
import com.gemalto.idp.mobile.fasttrack.example.MessengerConfigurations;
import com.gemalto.idp.mobile.fasttrack.example.R;
import com.gemalto.idp.mobile.fasttrack.example.Utils;
import com.gemalto.idp.mobile.fasttrack.messenger.MobileMessageManager;
import com.gemalto.idp.mobile.fasttrack.messenger.MobileMessenger;
import com.gemalto.idp.mobile.fasttrack.messenger.callback.AcknowledgeCallback;
import com.gemalto.idp.mobile.fasttrack.messenger.callback.FetchMessageCallback;
import com.gemalto.idp.mobile.fasttrack.messenger.callback.FetchResponse;
import com.gemalto.idp.mobile.fasttrack.messenger.callback.MobileMessengerCallback;
import com.gemalto.idp.mobile.fasttrack.messenger.callback.RegistrationCallback;
import com.gemalto.idp.mobile.fasttrack.messenger.callback.RegistrationResponse;
import com.gemalto.idp.mobile.fasttrack.messenger.callback.SendMessageCallback;
import com.gemalto.idp.mobile.fasttrack.messenger.callback.SendResponse;
import com.gemalto.idp.mobile.fasttrack.messenger.message.OutgoingMessage;
import com.gemalto.idp.mobile.fasttrack.messenger.message.TransactionSigningRequest;
import com.gemalto.idp.mobile.fasttrack.messenger.message.TransactionSigningResponse;
import com.gemalto.idp.mobile.fasttrack.messenger.message.TransactionVerifyRequest;
import com.gemalto.idp.mobile.fasttrack.messenger.message.TransactionVerifyResponse;
import com.gemalto.idp.mobile.fasttrack.protector.oath.OathMobileProtector;
import com.gemalto.idp.mobile.fasttrack.protector.oath.OathTokenDevice;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import static com.gemalto.idp.mobile.fasttrack.messenger.message.IncomingMessage.Type.UNSUPPORTED;

/**
 * This example for Messenger requires OCRA token for Transaction Signing use case.
 * Please provision OCRA first on {@link FragmentTabMessenger}
 */
public class FragmentTabMessenger extends Fragment {

    private FastTrack fastTrack;
    private MobileMessenger mobileMessenger;
    private MobileMessageManager messageManager;

    private Button registerButton;
    private Button unregisterButton;
    private Button fetchMessageButton;
    private EditText registrationCodeEditText;
    private TextView logTextView;

    private Utils.MyLogger mMyLogger = Utils.MyLogger.getsInstance();

    private String clientId;

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             @Nullable final ViewGroup container,
                             @Nullable final Bundle savedInstanceState) {

        final View retValue = inflater.inflate(R.layout.fragment_tab_messenger, null);

        // Retrieved FastTrack instance that was configured on the Application startup
        fastTrack = FastTrack.getInstance();

        // Load the view
        this.loadView(retValue);

        // Initialize Mobile Messenger
        this.initializeMobileMessenger();

        return retValue;
    }

    /**
     * Load the views and listeners
     *
     * @param messengerView
     */
    private void loadView(@NonNull final View messengerView) {

        registerButton = messengerView.findViewById(R.id.register);
        registerButton.setOnClickListener(view -> this.registration());

        unregisterButton = messengerView.findViewById(R.id.unregister);
        unregisterButton.setOnClickListener(view -> this.unregistration());

        fetchMessageButton = messengerView.findViewById(R.id.fetch_message);
        fetchMessageButton.setOnClickListener(view -> this.fetchMessage());

        registrationCodeEditText = messengerView.findViewById(R.id.messenger_reg_code);
        logTextView = messengerView.findViewById(R.id.message_log);
    }

    private void initializeMobileMessenger() {

        // 2.1 Initialization for Mobile Messenger
        mobileMessenger = fastTrack.getMobileMessengerBuilder(MessengerConfigurations.getMessengerServerUrl(),
                MessengerConfigurations.getDomain(),
                MessengerConfigurations.getApplicationId(),
                MessengerConfigurations.getPublicKey().getPublicExponent().toByteArray(),
                MessengerConfigurations.getPublicKey().getModulus().toByteArray())
                .build();

        // 2.2 Check if there is a client id exist in the database
        if (!mobileMessenger.listClients().isEmpty()) {
            mMyLogger.updateLogTitle(logTextView, "Re-initialization");
            mMyLogger.updateLogMessage(logTextView, "Registration is not needed\n", true);

            // 2.3 Get the first client id
            clientId = mobileMessenger.listClients().toArray()[0].toString();
            mMyLogger.updateLogMessage(logTextView, "Client Id : " + clientId + "\n", true);

            // 2.4 With the client id, construct the Message Manager
            messageManager = mobileMessenger.getMessageManager(clientId, MessengerConfigurations.getProviderId());

            // Update the UI
            updateUIAfterRegistration(true);
        } else {
            // Update the UI
            updateUIAfterRegistration(false);
        }
    }

    /**
     * Do Messenger registration
     */
    private void registration() {
        String registrationCode = registrationCodeEditText.getText().toString();
        mMyLogger.updateLogTitle(logTextView, "Registration");

        // Validate the registration code
        if (registrationCode == null || registrationCode.isEmpty()) {
            mMyLogger.updateLogMessage(logTextView, "Registration code is empty\n", false);
        }

        // 3.1 Do Mobile Messenger registration by providing Callback
        mobileMessenger.register(MessengerConfigurations.getUserId(),
                MessengerConfigurations.getUserAlias(),
                registrationCode,
                null,
                new RegistrationCallback() {
                    @Override
                    public void onRegistrationResponse(@NonNull RegistrationResponse registrationResponse) {

                        // Get the client id from the response
                        String clientId = registrationResponse.getClientId();
                        mMyLogger.updateLogMessage(logTextView, "Registration is successful \n" + "Client ID : " + clientId + "\n", true);

                        // With the client id, it is possible now to construct Message Manager
                        messageManager = mobileMessenger.getMessageManager(clientId, MessengerConfigurations.getProviderId());

                        // Update UI
                        updateUIAfterRegistration(true);
                        mobileMessenger.refreshToken(clientId, "SMS", "+23455", null, new MobileMessengerCallback() {
                            @Override
                            public void onSuccess() {
                                mMyLogger.updateLogMessage(logTextView, "refreshToken is successful \n", true);
                            }

                            @Override
                            public void onError(FastTrackException e) {
                                mMyLogger.updateLogMessage(logTextView, "refreshToken Error : " + e.getMessage() + "\n", false);
                            }
                        });
                    }

                    @Override
                    public void onError(@NonNull FastTrackException e) {

                        // Print the error
                        mMyLogger.updateLogMessage(logTextView, "Registration Error : " + e.getMessage() + "\n", false);

                        // Update UI
                        updateUIAfterRegistration(false);
                    }
                });
    }

    /**
     * Do Messenger Unregistration
     */
    private void unregistration() {
        mMyLogger.updateLogTitle(logTextView, "Unregistration");

        // 5 Do Mobile Messenger un-registration
        mobileMessenger.unregister(clientId,
                null,
                new MobileMessengerCallback() {

                    @Override
                    public void onSuccess() {
                        mMyLogger.updateLogMessage(logTextView, "Unregistration of " + clientId + " is successful \n" + "\n", true);

                        // Update UI
                        updateUIAfterRegistration(false);
                    }

                    @Override
                    public void onError(FastTrackException e) {

                        // Print the error
                        mMyLogger.updateLogMessage(logTextView, "Unregistration Error : " + e.getMessage() + "\n", false);
                    }
                });
    }

    /**
     * Fetch the message from OOB server with timeout
     * Client may check if there is an incoming message need to be retrieved.
     */
    private void fetchMessage() {
        // 4.1 fetch message with timeout
        messageManager.fetchMessage(30, null, new FetchMessageCallback() {
            @Override
            public void onFetchResponse(@NonNull FetchResponse fetchResponse) {

                // Check if there's an incoming message from the server
                if (fetchResponse.hasIncomingMessage()) {
                    if (fetchResponse.getMessageType() == UNSUPPORTED) {
                        mMyLogger.updateLogMessage(logTextView, "Fetch message is successful but message type not supported \n", false);
                    } else {
                        mMyLogger.updateLogMessage(logTextView, "Fetch message is successful \n", true);

                        // Show a dialog and react to it the message
                        showMessageDialog(fetchResponse);
                    }
                } else {
                    mMyLogger.updateLogMessage(logTextView, "There's no message to fetch \n", true);
                }
            }

            @Override
            public void onError(@NonNull FastTrackException e) {
                // Print the error
                mMyLogger.updateLogMessage(logTextView, "Fetch Message Error : " + e.getMessage() + "\n", false);
            }
        });
    }

    /**
     * Send Response to OOB server
     */
    private void sendMessage(OutgoingMessage message, Boolean value) {
        // Start sending the message
        messageManager.sendMessage(message, null, new SendMessageCallback() {
            @Override
            public void onSendMessageResponse(SendResponse sendResponse) {

                // Print the message that the message has been send successfully
                mMyLogger.updateLogMessage(logTextView, "The message is " + (value ? "ACCEPTED" : "REJECTED") + " successfully \n", true);
            }

            @Override
            public void onError(FastTrackException e) {

                // Print the error
                mMyLogger.updateLogMessage(logTextView, "Message verification Error : " + e.getMessage() + "\n", false);
            }
        });
    }

    /**
     * Acknowledge the message
     */
    private void acknowledgeMessage(FetchResponse response) {
        messageManager.acknowledgeMessage(response.getGenericIncomingMessage(), null, new AcknowledgeCallback() {
            @Override
            public void onAcknowledgeSuccess() {
                // Print the message that the message has been successfully acknowledge
                mMyLogger.updateLogMessage(logTextView, "Acknowledge message is successful \n", true);
            }

            @Override
            public void onError(@NonNull FastTrackException e) {

                // Print the error
                mMyLogger.updateLogMessage(logTextView, "Acknowledge Message Error : " + e.getMessage() + "\n", false);
            }
        });
    }

    /**
     * Verify the transaction message with the response value from UI
     *
     * @param response
     * @param value
     */
    private void verifyTransactionMessage(FetchResponse response, TransactionVerifyResponse.TransactionVerifyResponseValue value) {
        TransactionVerifyRequest request = response.getTransactionVerifyRequest();

        // Construct META
        Map<String, String> META = new HashMap<>();
        META.put("Marco", "Polo");
        META.put("Batman", "Robin");
        META.put("Tom", "Jerry");

        // Construct the verification response
        OutgoingMessage transactionVerifyResponse = request.createResponse(value, META);

        // Start sending the message
        sendMessage(transactionVerifyResponse, value == TransactionVerifyResponse.TransactionVerifyResponseValue.ACCEPTED);
    }

    /**
     * Sign the transaction message with the response value from UI
     * This will let the user sign the transaction using an OTP and it is required a token device to be present
     * In this example, OATH OCRA token is being used
     *
     * @param response
     * @param value
     */
    private void signTransactionMessage(FetchResponse response,
                                        TransactionSigningResponse.TransactionSigningResponseValue value,
                                        String pin) {
        try {

            // Get the transaction signing request object
            TransactionSigningRequest request = response.getTransactionSigningRequest();

            Map<String, String> META = new HashMap<>();
            OutgoingMessage transactionSigningRequest = null;

            Boolean messageValue = false;

            switch (value) {
                case REJECTED:

                    // Construct META for rejected message
                    META.put("Message", "Rejected from the UI");
                    transactionSigningRequest = request.createResponse(TransactionSigningResponse.TransactionSigningResponseValue.REJECTED,
                            null,
                            META);

                    break;
                case ACCEPTED:

                    // Get the oath protector instance
                    OathMobileProtector protector = fastTrack.getOathMobileProtectorInstance();

                    // Check if there's OATH token device that could be use to sign the request
                    if (protector.getTokenDeviceNames().isEmpty()) {

                        // Print the error
                        mMyLogger.updateLogMessage(logTextView, "There's no available OATH token that is able to be use to sign the request\n" +
                                "Provisioning with Mobile Protector is required", false);
                        return;
                    } else {

                        // Get token device with uti, which get from transaction signing request object
                        OathTokenDevice tokenDevice = protector.getTokenDeviceWithUserTokenId(request.getUserTokenIdForSigning(), null);
                        String tokenDeviceName = protector.getTokenDeviceNames().iterator().next();
                        String serverChallenge = new String(request.getOcraServerChallenge(), StandardCharsets.UTF_8);

                        // Generate OTP using server challenge, password hash and session
                        String otp = tokenDevice.getOcraOtp(pin,
                                serverChallenge,
                                request.getOcraClientChallenge(),
                                request.getOcraPasswordHash(),
                                request.getOcraSession());

                        mMyLogger.updateLogMessage(logTextView, "Signing the transaction using :\n" +
                                "Token Device Name : " + tokenDeviceName + "\n" +
                                "OTP : " + otp + "\n" +
                                "Server Challenge : " + serverChallenge + "\n" +
                                "Client Challenge : " + request.getOcraClientChallenge() + "\n" +
                                "Password Hash : " + Utils.Hex.expand(request.getOcraPasswordHash()) + "\n" +
                                "Session : " + request.getOcraSession() + "\n", false);

                        // Construct META for accepted message
                        META.put("shouldVerifyOTP", "true");
                        META.put("user", tokenDeviceName);
                        META.put("otpType", tokenDevice.getType().name());

                        // Create the response
                        messageValue = true;
                        transactionSigningRequest = request.createResponse(TransactionSigningResponse.TransactionSigningResponseValue.ACCEPTED,
                                otp,
                                META);
                    }
                    break;
            }

            // Start send the message
            sendMessage(transactionSigningRequest, messageValue);

        } catch (FastTrackException e) {
            // Print the error
            mMyLogger.updateLogMessage(logTextView, "An error happen : " + e.getMessage() + "\n", false);
        }
    }

    /**
     * This function will show a dialog that will give the user of the application react on the message
     * Depending on the type, Client need to react differently
     */
    private void showMessageDialog(FetchResponse response) {

        int positiveButtonLabel = 0;
        int negativeButtonLabel = 0;
        int messageLabel = 0;

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        // Defined the dialog UI element base on the message type
        switch (response.getMessageType()) {
            case UNSUPPORTED:
                // If the message is not supported
                mMyLogger.updateLogMessage(logTextView,"Message type not supported\n", false);
                return;
            case GENERIC:
                positiveButtonLabel = R.string.ui_dialog_yes;
                negativeButtonLabel = R.string.ui_dialog_no;
                messageLabel = R.string.ui_dialog_acknowledge_message;
                break;
            case TRANSACTION_SIGNING_REQUEST:
            case TRANSACTION_VERIFY_REQUEST:
                positiveButtonLabel = R.string.ui_dialog_accept;
                negativeButtonLabel = R.string.ui_dialog_reject;
                messageLabel = R.string.ui_dialog_transaction_message;
        }

        builder.setMessage(messageLabel);
        builder.setPositiveButton(positiveButtonLabel, (dialog, id) -> responseToAction(response, true));
        builder.setNegativeButton(negativeButtonLabel, (dialog, id) -> responseToAction(response, false));

        // Finally, create the alert dialog and show
        builder.create().show();
    }

    /**
     * Response to the message with action
     *
     * @param response
     * @param value    in case of the message is transaction message then this value could true (ACCEPTED) or false (REJECTED)
     */
    private void responseToAction(FetchResponse response, Boolean value) {
        switch (response.getMessageType()) {
            case UNSUPPORTED:
                // If the message is not supported
                mMyLogger.updateLogMessage(logTextView,"Message type not supported\n", false);
                break;
            case GENERIC:
                // If the message is generic then acknowledge the message
                acknowledgeMessage(response);
                break;
            case TRANSACTION_VERIFY_REQUEST:
                // If the message if asking to verify a transaction then user could decide to accept or reject it
                verifyTransactionMessage(response, value ? TransactionVerifyResponse.TransactionVerifyResponseValue.ACCEPTED :
                        TransactionVerifyResponse.TransactionVerifyResponseValue.REJECTED);
                break;
            case TRANSACTION_SIGNING_REQUEST:

                // If the message is asking to sign a transaction the user could decide to accept or reject it
                if (value) {

                    // Show another dialog to get the pin
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

                    // Set the view to edit text so user can enter their pin
                    final EditText editText = new EditText(getActivity());
                    builder.setView(editText);

                    builder.setMessage(R.string.ui_dialog_transaction_signing_enter_pin);

                    builder.setPositiveButton(R.string.ui_dialog_ok, (dialog, i) -> {

                        // Sign the transaction with ACCEPTED response
                        signTransactionMessage(response, TransactionSigningResponse.TransactionSigningResponseValue.ACCEPTED, editText.getText().toString());
                    });

                    builder.setNegativeButton(R.string.ui_dialog_cancel, (dialogInterface, i) -> {

                        // If the cancel button is hit then cancel the operation
                        mMyLogger.updateLogMessage(logTextView, "The transaction signing is canceled\n", false);
                        dialogInterface.dismiss();
                    });

                    builder.show();

                } else {

                    // Reject the transaction
                    signTransactionMessage(response, TransactionSigningResponse.TransactionSigningResponseValue.REJECTED, "");
                }

                break;
        }
    }

    /**
     * Update the UI after registration happen, disable the registration button and edit text
     *
     * @param registrationStatus
     */
    private void updateUIAfterRegistration(Boolean registrationStatus) {
        fetchMessageButton.setEnabled(registrationStatus);
        registerButton.setEnabled(!registrationStatus);
        registrationCodeEditText.setEnabled(!registrationStatus);
        unregisterButton.setEnabled(registrationStatus);
    }
}
