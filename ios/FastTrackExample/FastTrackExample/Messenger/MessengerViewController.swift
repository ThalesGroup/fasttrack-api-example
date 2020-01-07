/** ------------------------------------------------------------------------------------------------------
 
 Copyright (c) 2019  -  GEMALTO DEVELOPMENT - R&D
 
 --------------------------------------------------------------------------------------------------------
 GEMALTO MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF
 THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
 TO THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 PARTICULAR PURPOSE, OR NON-INFRINGEMENT. GEMALTO SHALL NOT BE
 LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING,
 MODIFYING OR DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES.
 
 THIS SOFTWARE IS NOT DESIGNED OR INTENDED FOR USE OR RESALE AS ON-LINE
 CONTROL EQUIPMENT IN HAZARDOUS ENVIRONMENTS REQUIRING FAIL-SAFE
 PERFORMANCE, SUCH AS IN THE OPERATION OF NUCLEAR FACILITIES, AIRCRAFT
 NAVIGATION OR COMMUNICATION SYSTEMS, AIR TRAFFIC CONTROL, DIRECT LIFE
 SUPPORT MACHINES, OR WEAPONS SYSTEMS, IN WHICH THE FAILURE OF THE
 SOFTWARE COULD LEAD DIRECTLY TO DEATH, PERSONAL INJURY, OR SEVERE
 PHYSICAL OR ENVIRONMENTAL DAMAGE ("HIGH RISK ACTIVITIES"). GEMALTO
 SPECIFICALLY DISCLAIMS ANY EXPRESS OR IMPLIED WARRANTY OF FITNESS FOR
 HIGH RISK ACTIVITIES.
 
 ----------------------------------------------------------------------------------------------------------
 */

import UIKit

class MessengerViewController: UIViewController {

    @IBOutlet weak var registerButton           : UIButton!
    @IBOutlet weak var unregisterButton         : UIButton!
    @IBOutlet weak var fetchMessageButton       : UIButton!
    @IBOutlet weak var registrationCodeTextField: UITextField!
    @IBOutlet weak var logTextView              : UITextView!

    var fastTrack                             : EMFastTrack!
    var mobileMessenger                         : EMMobileMessenger!
    var messageManager                          : EMMobileMessageManager!

    var clientId                                : String?

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        // Do any additional setup after loading the view.

        // Clear log
        MyLogger.clearLog(logTextView)

        // Retrieved FastTrack instance that was configured on the Application startup
        fastTrack = EMFastTrack.sharedInstance()

        // Initialize Mobile Messenger
        initializeMobileMessenger()
    }

    // MARK: - IBAction

    /**
     * Register button on click action
     */
    @IBAction func onClickRegister(_ sender: Any) {
        registration()
    }

    /**
     * Unregister button on click action
     */
    @IBAction func onClickUnregister(_ sender: Any) {
        unregistration()
    }

    /**
     * Fetch message on click action
     */
    @IBAction func onClickFetchMessage(_ sender: Any) {
        fetchMessage()
    }
    
    // MARK: - Mobile messenger

    /**
     * Initialize mobile messenger
     */
    private func initializeMobileMessenger() {
        do {
            // 2.1 Initialization for Mobile Messenger
            mobileMessenger = try fastTrack.mobileMessenger(url: MessengerConfigurations.messengerServerUrl(), domain: MessengerConfigurations.domain(), applicationId: MessengerConfigurations.applicationId(), rsaExponent: MessengerConfigurations.rsaExponent(), rsaModulus: MessengerConfigurations.rsaModulus(), optionalParameters: nil)

            // 2.2 Check if there is a client id exist in the database
            if (!mobileMessenger.clientIds().isEmpty) {
                MyLogger.updateLogTitle(logTextView, title: "Re-initialization")
                MyLogger.updateLogMessage(logTextView, message: "Registration is not needed\n", status: true)

                // 2.3 Get the first client id
                clientId = mobileMessenger.clientIds().first
                MyLogger.updateLogMessage(logTextView, message: "Client Id : \(clientId!)\n", status: true)

                // 2.4 With the client id, construct the Message Manager
                messageManager = try mobileMessenger.messageManager(clientId: clientId!, providerId: MessengerConfigurations.providerId())
                
                // Update the UI
                updateUIAfterRegistration(true)
            } else {
                // Update the UI
                updateUIAfterRegistration(false)
            }
        } catch let err as NSError {
            // Print the error
            MyLogger.updateLogMessage(logTextView, message: "An error happen : \(err)\n", status: false)
        }
    }

    /**
     * Do Messenger registration
     */
    private func registration() {
        MyLogger.updateLogTitle(logTextView, title: "Registration")

        guard let registrationCode = registrationCodeTextField.text else {
            MyLogger.updateLogMessage(logTextView, message: "Registration code is empty\n", status: false)
            return
        }

        guard !registrationCode.isEmpty else {
            MyLogger.updateLogMessage(logTextView, message: "Registration code is empty\n", status: false)
            return
        }

        // 3.1 Do Mobile Messenger registration by providing Callback
        mobileMessenger.register(userId: MessengerConfigurations.userId(), userAlias: MessengerConfigurations.userAlias(), registrationCode: registrationCode, customHeaders: nil) { [weak self](response: EMRegistrationResponse?, err: Error?) in
            if let err = err {
                // Print the error
                MyLogger.updateLogMessage(self?.logTextView, message: "Registration Error : \(err.localizedDescription)\n", status: false)
                
                // Update UI
                self?.updateUIAfterRegistration(false)
                return
            }

            if let response = response {
                // Get the client id from the response
                self?.clientId = response.clientId
                MyLogger.updateLogMessage(self?.logTextView, message: "Registration is successful\nClient ID : \((self?.clientId)!)\n", status: true)

                do {
                    // With the client id, it is possible now to construct Message Manager
                    self?.messageManager = try self?.mobileMessenger.messageManager(clientId: (self?.clientId)!, providerId: MessengerConfigurations.providerId())

                    // Update UI
                    self?.updateUIAfterRegistration(true)
                    self?.mobileMessenger.refreshToken(clientId: (self?.clientId)!, channel: "SMS", endPoint: "+223456", customHeaders: nil, completionHandler: { (success, err) in
                        MyLogger.updateLogMessage(self?.logTextView, message: "refreshToken sucess : \(success)\n", status: success)
                    })
                } catch let err as NSError {
                    // Print the error
                    MyLogger.updateLogMessage(self?.logTextView, message: "An error happen : \(err)\n", status: false)
                }
            }
        }
    }

    /**
     * Do Messenger Unregistration
     */
    private func unregistration() {
        MyLogger.updateLogTitle(logTextView, title: "Unregistration")

        // 5 Do Mobile Messenger un-registration
        mobileMessenger.unregister(clientId: clientId!, customHeaders: nil) { [weak self](success: Bool, err: Error?) in
            if let err = err {
                // Print the error
                MyLogger.updateLogMessage(self?.logTextView, message: "Unregistration Error : \(err.localizedDescription)\n", status: false)
                return
            }

            MyLogger.updateLogMessage(self?.logTextView, message: "Unregistration of \((self?.clientId)!) is successful\n\n", status: true)

            // Update UI
            self?.updateUIAfterRegistration(false)
        }
    }

    /**
     * Fetch the message from OOB server with timeout
     * Client may check if there is an incoming message need to be retrieved.
     */
    private func fetchMessage() {
        // 4.1 fetch message with timeout
        messageManager.fetchMessage(timeout: 30, customHeaders: nil) { [weak self](response: EMFetchMessageResponse?, err: Error?) in
            if let err = err {
                // Print the error
                MyLogger.updateLogMessage(self?.logTextView, message: "Fetch Message Error : \(err.localizedDescription)\n", status: false)
                return
            }

            if let response = response {
                // Check if there's an incoming message from the server
                if response.hasIncomingMessage {
                    if response.messageType == .unsupported {
                        MyLogger.updateLogMessage(self?.logTextView, message: "Fetch message is successful but message type not supported\n", status: false)
                    } else {
                        MyLogger.updateLogMessage(self?.logTextView, message: "Fetch message is successful\n", status: true)

                        // Show a dialog and react to it the message
                        self?.showMessageDialog(response)
                    }
                } else {
                    MyLogger.updateLogMessage(self?.logTextView, message: "There's no message to fetch\n", status: true)
                }
            }
        }
    }

    /**
     * Send Response to OOB server
     */
    private func sendMessage(_ outgoingMessage: EMOutgoingMessage!, value: Bool) {
        // Start sending the message
        messageManager.send(message: outgoingMessage, customHeaders: nil) { [weak self](response: EMSendMessageResponse?, err: Error?) in
            if let err = err {
                // Print the error
                MyLogger.updateLogMessage(self?.logTextView, message: "Message verification Error : \(err.localizedDescription)\n", status: false)
                return
            }
            
            if let _ = response {
                // Print the message that the message has been send successfully
                MyLogger.updateLogMessage(self?.logTextView, message: "This message is \(value ? "ACCEPTED" : "REJECTED") successfully\n", status: true)
            }
        }
    }

    /**
     * Acknowledge the message
     */
    private func acknowledgeMessage(_ response: EMFetchMessageResponse!) {
        guard let genericIncomingMessage = response.genericIncomingMessage else {
            MyLogger.updateLogMessage(logTextView, message: "Acknowledge Message Error : Message type is not supported\n", status: false)
            return
        }

        // Start aknowledge the message
        messageManager.acknowledge(message: genericIncomingMessage, customHeaders: nil) { [weak self](success: Bool, err: Error?) in
            if let err = err {
                // Print the error
                MyLogger.updateLogMessage(self?.logTextView, message: "Acknowledge Message Error : \(err.localizedDescription)\n", status: false)
                return
            }

            // Print the message that the message has been successfully acknowledge
            MyLogger.updateLogMessage(self?.logTextView, message: "Acknowledge message is successfully\n", status: true)
        }
    }

    /**
     * Verify the transaction message with the response value from UI
     * @param response
     * @param value
     */
    private func verifyTransactionMessage(_ response: EMFetchMessageResponse!, value: EMTransactionVerifyResponseValue) {
        guard let request = response.transactionVerifyRequest else {
            MyLogger.updateLogMessage(logTextView, message: "Verify Transaction Message Error : Message type is not supported\n", status: false)
            return
        }

        // Construct META
        let META = NSMutableDictionary()
        META.setValue("Polo", forKey: "Marco")
        META.setValue("Robin", forKey: "Batman")
        META.setValue("Jerry", forKey: "Tom")

        // Construct the verification response
        let transactionVerifyResponse = request.create(response: value, meta: META as? [String : String])

        // Start sending the message
        sendMessage(transactionVerifyResponse, value: value == EMTransactionVerifyResponseValue.accepted)
    }

    /**
     * Sign the transaction message with the response value from UI
     * This will let the user sign the transaction using an OTP and it is required a token device to be present
     * In this example, OATH OCRA token is being used
     * @param response
     * @param value
     * @param pin
     */
    private func signTransactionMessage(_ response: EMFetchMessageResponse!, value: EMTransactionSigningResponseValue, pin: String!) {
        guard let request = response.transactionSigningRequest else {
            MyLogger.updateLogMessage(logTextView, message: "Signing Transaction Message Error : Message type is not supported\n", status: false)
            return
        }

        let META = NSMutableDictionary()
        var transactionSigningRequest : EMOutgoingMessage?

        var messageValue = false

        switch value {
        case .rejected:
            // Construct META for rejected message
            META.setValue("Rejected from the UI", forKey: "Message")
            transactionSigningRequest = request.create(response: EMTransactionSigningResponseValue.rejected, otp: nil, meta: META as? [AnyHashable : Any])
        case .accepted:
            // Get the oath protector instance
            guard let protector = EMOathMobileProtector.sharedInstance() else {
                return
            }
            // Check if there's OATH token device that could be use to sign the request
            do {
                let tokenDeviceNames = try protector.tokenDeviceNames()
                if tokenDeviceNames.isEmpty {
                    // Print the error
                    MyLogger.updateLogMessage(logTextView, message: "There's no available OATH token that is able to be use to sign the request\nProvisioning with Mobile Protector is required", status: false)
                } else {
                    // Get token device with uti, which get from transaction signing request object
                    let tokenDevice = try protector.tokenDevice(userTokenID: Int32(request.userTokenIdForSigning), fingerpintCustomData: nil)
                    let tokenDeviceName = tokenDeviceNames.first!
                    guard let serverChallengeData = request.ocraServerChallenge else {
                        return
                    }
                    guard let serverChallenge = String.init(data: serverChallengeData, encoding: .utf8) else {
                        return
                    }

                    // Generate OTP using server challenge, password hash and session
                    let otp = try tokenDevice.ocra(pin: pin, serverChallengeQuestion: serverChallenge, clientChallengeQuestion: request.ocraClientChallenge, passwordHash: request.ocraPasswordHash, session: request.ocraSession)

                    MyLogger.updateLogMessage(logTextView, message: "Signing the transaction using :\nToken Device Name : \(tokenDeviceName)\nOTP : \(otp)\nServer Challenge : \(serverChallenge)\nClient Challenge : \(String(describing: request.ocraClientChallenge))\nPassword Hash : \(String(describing: request.ocraPasswordHash))\nSession : \(String(describing: request.ocraSession))\n", status: false)

                    // Construct META for accepted message
                    META.setValue(true, forKey: "shouldVerifyOTP")
                    META.setValue(tokenDeviceName, forKey: "user")
                    META.setValue("oathOcra", forKey: "otpType")

                    // Create the response
                    messageValue = true
                    transactionSigningRequest = request.create(response: EMTransactionSigningResponseValue.accepted, otp: otp, meta: META as? [AnyHashable : Any])
                }
            } catch let err as NSError {
                // Print the error
                MyLogger.updateLogMessage(logTextView, message: "An error happen : \(err)\n", status: false)
            }
        }

        if let transactionSigningRequest = transactionSigningRequest {
            // Start send the message
            sendMessage(transactionSigningRequest, value: messageValue)
        }
    }

    /**
     * Response to the message with action
     * @param response
     * @param value in case of the message is transaction message then this value could true (ACCEPTED) or false (REJECTED)
     */
    private func responseToAction(_ response: EMFetchMessageResponse!, value: Bool) {
        switch response.messageType {
        case .unsupported:
            // If the message is not supported
            MyLogger.updateLogMessage(logTextView, message: "Message type not supported\n", status: false)
        case .generic:
            // If the message is generic then acknowledge the message
            acknowledgeMessage(response)
        case .transactionVerifyRequest:
            // If the message if asking to verify a transaction then user could decide to accept or reject it
            verifyTransactionMessage(response, value: value ? EMTransactionVerifyResponseValue.accepted : EMTransactionVerifyResponseValue.rejected)
        case .transactionSigningRequest:
            // If the message is asking to sign a transaction the user could decide to accept or reject it
            if (value) {
                let title : String = "Please enter the pin"

                let alertController : UIAlertController = UIAlertController.init(title: title, message: nil, preferredStyle: .alert)
                let ok : UIAlertAction = UIAlertAction.init(title: "Ok", style: .default) { (_) in
                    // Sign the transaction with ACCEPTED response
                    self.signTransactionMessage(response, value: EMTransactionSigningResponseValue.accepted, pin: alertController.textFields![0].text!)
                }
                let cancel : UIAlertAction = UIAlertAction.init(title: "Cancel", style: .cancel) { (_) in
                    // If the cancel button is hit then cancel the operation
                    MyLogger.updateLogMessage(self.logTextView, message: "The transaction signing is canceled\n", status: false)
                }
                alertController.addAction(ok)
                alertController.addAction(cancel)
                alertController.addTextField { (textField) in
                    textField.keyboardType = .numberPad
                }
                self.present(alertController, animated: true, completion: nil)
            } else {
                // Reject the transaction
                signTransactionMessage(response, value: EMTransactionSigningResponseValue.rejected, pin: "")
            }
        }
    }

    // MARK: - Helper

    /**
     * Update the UI after registration happen, disable the registration button and edit text
     * @param registrationStatus
     */
    private func updateUIAfterRegistration(_ registrationStatus: Bool) {
        fetchMessageButton.isEnabled = registrationStatus
        registerButton.isEnabled = !registrationStatus
        registrationCodeTextField.isEnabled = !registrationStatus
        unregisterButton.isEnabled = registrationStatus
    }

    /**
     * This function will show a dialog that will give the user of the application react on the message
     * Depending on the type, Client need to react differently
     * @param response
     */
    private func showMessageDialog(_ response: EMFetchMessageResponse!) {
        var title : String!

        var positiveButtonLabel : String!
        var negativeButtonLabel : String!

        // Defined the dialog UI element base on the message type
        switch response.messageType {
        case .unsupported:
            // If the message is not supported
            MyLogger.updateLogMessage(logTextView, message: "Message type not supported\n", status: false)
            return
        case .generic:
            positiveButtonLabel = "Yes"
            negativeButtonLabel = "No"
            title = "Do you want to acknowledge this message?"
        case .transactionSigningRequest:
            positiveButtonLabel = "Accept"
            negativeButtonLabel = "Reject"
            title = "Do you want to accept this message?"
        case .transactionVerifyRequest:
            positiveButtonLabel = "Accept"
            negativeButtonLabel = "Reject"
            title = "Do you want to accept this message?"
        }

        let alertController : UIAlertController = UIAlertController.init(title: title, message: nil, preferredStyle: .alert)
        let positiveButton : UIAlertAction = UIAlertAction.init(title: positiveButtonLabel, style: .default) { (_) in
            self.responseToAction(response, value: true)
        }
        let negativeButton : UIAlertAction = UIAlertAction.init(title: negativeButtonLabel, style: .cancel) { (_) in
            self.responseToAction(response, value: false)
        }
        alertController.addAction(positiveButton)
        alertController.addAction(negativeButton)
        self.present(alertController, animated: true, completion: nil)
    }
}
