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

protocol StatusLoggerDelegateProtocol: NSObjectProtocol {
    func updateStatus(_ log: String)
}

class ProtectorViewController: UIViewController, UIPickerViewDelegate, UIPickerViewDataSource, UITextFieldDelegate, StatusLoggerDelegateProtocol {
    
    @IBOutlet weak var segmentControlOtpType: UISegmentedControl!
    @IBOutlet weak var textFieldRegCode: UITextField!
    @IBOutlet weak var buttonProvision: UIButton!
    @IBOutlet weak var pickerViewTokenDevice: UIPickerView!
    @IBOutlet weak var buttonOtpWithPin: UIButton!
    @IBOutlet weak var buttonChangePin: UIButton!
    @IBOutlet weak var buttonActivateSystemBiometric: UIButton!
    @IBOutlet weak var buttonOtpWithSystemBiometric: UIButton!
    @IBOutlet weak var buttonDeactiveBiomertic: UIButton!
    @IBOutlet weak var textViewLogger: UITextView!
    
    var protectorExample: ProtectorExample?
    var indicator: UIActivityIndicatorView?
    var tokenDeviceNames: Array<String>? = [String]()
    
    var otpType: OtpType? = OtpType.TOTP {
        didSet {
            guard let otpType = otpType else { return }
            protectorExample?.otpType = otpType
            guard let tokenDeviceNamesSet = protectorExample?.getTokenNames() else {
                protectorExample?.tokenDeviceName = nil
                return
            }
            tokenDeviceNames = Array(tokenDeviceNamesSet)
            self.pickerViewTokenDevice.selectRow(0, inComponent:0, animated:true)
            protectorExample?.tokenDeviceName = tokenDeviceNames?.first
            self.pickerViewTokenDevice.reloadAllComponents()
        }
    }
    
    func reloadTokenDeviceNames(_ selectedTokenDeviceName:String? = nil) {
        guard let tokenDeviceNamesSet = protectorExample?.getTokenNames() else { return  }
        tokenDeviceNames = Array(tokenDeviceNamesSet)
        self.pickerViewTokenDevice.reloadAllComponents()
        guard let selectedTokenDeviceName = selectedTokenDeviceName else { return  }
        guard let selectedRow = tokenDeviceNames?.index(of: selectedTokenDeviceName) else { return  }
        self.pickerViewTokenDevice.selectRow(selectedRow, inComponent: 0, animated: true)
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        segmentControlOtpType.selectedSegmentIndex = 0
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        setupControllers()
        protectorExample = ProtectorExample()
        protectorExample?.statusLogger = self
        guard let tokenDeviceNamesSet = protectorExample?.getTokenNames() else { return  }
        tokenDeviceNames = Array(tokenDeviceNamesSet)
        
        protectorExample?.tokenDeviceName = tokenDeviceNames?.first
    }
    
    fileprivate func setupControllers() {
        
        let otpTypes = ["TOTP","CAP","OCRA"]
        segmentControlOtpType.removeAllSegments()
        for (index, otpType) in otpTypes.enumerated() {
            segmentControlOtpType.insertSegment(withTitle: otpType, at: index, animated: true)
        }
        if segmentControlOtpType.selectedSegmentIndex == UISegmentedControlNoSegment {
            segmentControlOtpType.selectedSegmentIndex = 0
            segmentControlOtpType.setNeedsLayout()
        }
        pickerViewTokenDevice.dataSource = self
        pickerViewTokenDevice.delegate = self
        self.pickerViewTokenDevice.selectRow(0, inComponent:0, animated:true)
        textFieldRegCode.delegate = self
        initActivityInidcator()
    }
    
    func numberOfComponents(in pickerView: UIPickerView) -> Int {
        return 1
    }
    
    func pickerView(_ pickerView: UIPickerView, numberOfRowsInComponent component: Int) -> Int {
        guard let tokenDeviceNames = tokenDeviceNames else { return 0 }
        return tokenDeviceNames.count
    }
    
    func pickerView(_ pickerView: UIPickerView, didSelectRow row: Int, inComponent component: Int) {
        guard let tokenDeviceNames = tokenDeviceNames, tokenDeviceNames.count != 0 else { return  }
        protectorExample?.tokenDeviceName = tokenDeviceNames[row]
    }
    
    func pickerView(_ pickerView: UIPickerView, titleForRow row: Int, forComponent component: Int) -> String? {
        guard let tokenDeviceNames = tokenDeviceNames, tokenDeviceNames.count != 0 else { return  nil }
        return tokenDeviceNames[row]
    }
    
    func initActivityInidcator()
    {
        indicator = UIActivityIndicatorView(activityIndicatorStyle: UIActivityIndicatorViewStyle.gray)
        guard let indicator = indicator else {
            return
        }
        indicator.frame = CGRect(x: 0, y: 0, width: 80, height: 80)
        indicator.center = view.center
        indicator.color = UIColor.blue
        indicator.backgroundColor = UIColor(red:0.85, green:0.85, blue:0.89, alpha:0.5)
        indicator.layer.cornerRadius = 3
        indicator.clipsToBounds = true
        
        indicator.layer.masksToBounds = false
        view.addSubview(indicator)
        view.bringSubview(toFront: indicator)
    }
    
    // MARK: Protector Operations
    @IBAction func onChangeOtpType(_ sender: Any) {
        
        switch segmentControlOtpType.selectedSegmentIndex {
        case 0:
            self.otpType = OtpType.TOTP
            break
        case 1:
            self.otpType = OtpType.CAP
            break
        case 2:
            self.otpType = OtpType.OCRA
            break
        default:
            return
        }
    }
    
    @IBAction func onClickProvision(_ sender: UIButton) {
        guard let regCode = textFieldRegCode.text, (6...32).contains(regCode.count) else {
            MyLogger.updateLogMessage(self.textViewLogger, message: "You didn't provide the valid registration code!")
            return
        }
        indicator?.startAnimating()
        protectorExample?.provision(regCode, completion: { (tokenName, error) in
            if let tokenName = tokenName {
                self.reloadTokenDeviceNames(tokenName)
                MyLogger.updateLogMessage(self.textViewLogger, message: "Provision successful with token name \(tokenName)")
                self.protectorExample?.tokenDeviceName = tokenName
            }
            else {
                MyLogger.updateLogMessage(self.textViewLogger, message: "Provision failed with error \(String(describing: error))")
            }
            self.textFieldRegCode.text = ""
            self.indicator?.stopAnimating()
        })
    }
    
    @IBAction func onClickOtpWithPin(_ sender: UIButton) {
        if (protectorExample?.tokenDeviceName == nil) {
            MyLogger.updateLogMessage(self.textViewLogger, message: "No TokenDevice selected.")
            return
        }
        
        let title : String = "Please enter the pin"
        getUserInputPin(title: title) { (pin) in
            let otp = self.protectorExample?.otp(pin)
            MyLogger.updateLog(self.textViewLogger, title: "OTP Value", message: "\(otp ?? "nil")")
        }
    }
    
    @IBAction func onClickChangePin(_ sender: UIButton) {
        if (protectorExample?.tokenDeviceName == nil) {
            MyLogger.updateLogMessage(self.textViewLogger, message: "No TokenDevice selected.")
            return
        }
        
        let title : String = "Please enter the pin"
        getUserInputOldAndNewPin(title: title) { (oldPin, newPin) in
            let isPinChanged = self.protectorExample?.changePin(oldPin, newPin)
            MyLogger.updateLogMessage(self.textViewLogger, message: "Change Pin Status :: \(isPinChanged!)", status: isPinChanged!) 
        }
    }
    
    @IBAction func onClickActivateSystemBiometric(_ sender: UIButton) {

        if (protectorExample?.tokenDeviceName == nil) {
            MyLogger.updateLogMessage(self.textViewLogger, message: "No TokenDevice selected.")
            return
        }
        
        /**
         * To activate Biometric Authentication:
         * 1 Check if current Token Device has been activated previously
         * */
        if (self.protectorExample?.checkSystemBiometricActivation() == true) {
            MyLogger.updateLogMessage(self.textViewLogger, message: "SystemBiometric already activated for token device.")
            return
        }
        //2 Make sure the PIN provided is correct by Authenticating to the Server
        let title : String = "Please enter the pin"
        getUserInputPin(title: title) { (pin) in
            //3 Call activate API by providing PIN value
            let isActivated = self.protectorExample?.activateSystemBiometric(pin)
            MyLogger.updateLogMessage(self.textViewLogger, message: "isSystemBiometric Activated :: \(isActivated!)", status: isActivated!)
        }
    }
    
    @IBAction func onClickDeactivateSystemBiometric(_ sender: UIButton) {
        if (protectorExample?.tokenDeviceName == nil) {
            MyLogger.updateLogMessage(self.textViewLogger, message: "No TokenDevice selected.")
            return
        }
        
        let isDeactivated = protectorExample?.deactivateSystemBiometric()
        MyLogger.updateLogMessage(self.textViewLogger, message: "isSystemBiometric Deactivated :: \(isDeactivated!)", status: isDeactivated!)
    }
    
    @IBAction func onClickOtpWithSystemBiometric(_ sender: UIButton) {
        if (protectorExample?.tokenDeviceName == nil) {
            MyLogger.updateLogMessage(self.textViewLogger, message: "No TokenDevice selected.")
            return
        }
        
        //To perform OTP generation with Biometric Authentication:
        //1 Make sure Biometric has been activated previously
        if (self.protectorExample?.checkIsSystemBiometricActivated() == false) {
            MyLogger.updateLogMessage(self.textViewLogger, message: "SystemBiometric authentication is not activated for token device")
            return
        }
        //2 Perfrom authentication with system biometric and generate OTP
        protectorExample?.otpWithSystemBiometric(completion: { (otp, error) in
            DispatchQueue.main.async {
                if let otp = otp {
                    MyLogger.updateLog(self.textViewLogger, title: "OTP Value", message: "\(otp)")
                } else if let error = error as NSError?, error.code == EMFastTrackError.Code.systemBiometricCancelledUserFallback.rawValue {
                    let title : String = "Please enter the pin"
                    self.getUserInputPin(title: title) { (pin) in
                        let otp = self.protectorExample?.otp(pin)
                        MyLogger.updateLog(self.textViewLogger, title: "OTP Value", message: "\(otp ?? "nil")")
                    }
                }
                else {
                    MyLogger.updateLog(self.textViewLogger, title: "OTP with system biometric", message: "Error: \(error!)")
                }
            }
        })
    }
    
    @IBAction func onClickListTokenDevices(_ sender: UIButton) {
        guard let allTokenDeviceNames =  protectorExample?.getAllTokenDeviceNames() else {
            MyLogger.updateLog(self.textViewLogger, title: "TokenDevices", message: "nil")
            return
        }
        MyLogger.updateLog(self.textViewLogger, title: "TokenDevices", message: "\(allTokenDeviceNames)")
    }
    
    @IBAction func onClickDeleteTokenDevices(_ sender: Any) {
        guard let deletedTokenDeviceNames = protectorExample?.deleteTokenDevices() else {
            MyLogger.updateLog(self.textViewLogger, title: "Removed TokenDevices", message: "nil")
            return
        }
        MyLogger.updateLog(self.textViewLogger, title: "Removed TokenDevices", message: "\(deletedTokenDeviceNames)")
    }
    
    func getUserInputPin(title: String, completion: @escaping (String) -> Void) {
        let alertController : UIAlertController = UIAlertController.init(title: title, message: nil, preferredStyle: .alert)
        let ok : UIAlertAction = UIAlertAction.init(title: "OK", style: .default) { (_) in
            guard let pin = alertController.textFields?[0].text, alertController.textFields?[0].text?.count != 0 else {
                DispatchQueue.main.async {
                    MyLogger.updateLogMessage(self.textViewLogger, message: "User didn't enter the pin!")
                }
                return
            }
            completion(pin)
        }
        let cancel : UIAlertAction = UIAlertAction.init(title: "Cancel", style: .cancel) { (_) in
            DispatchQueue.main.async {
                MyLogger.updateLogMessage(self.textViewLogger, message: "User cancelled the operation")
            }
        }
        alertController.addAction(ok)
        alertController.addAction(cancel)
        alertController.addTextField { (textField) in
            textField.keyboardType = .numberPad
        }
         
        self.present(alertController, animated: true, completion: nil)
        
    }
    
    func getUserInputOldAndNewPin(title: String!, completion: @escaping (String, String) -> Void) {
        let alertController : UIAlertController = UIAlertController.init(title: title, message: nil, preferredStyle: .alert)
        let ok : UIAlertAction = UIAlertAction.init(title: "OK", style: .default) { (_) in
            guard let oldPin = alertController.textFields?[0].text,
                let newPin = alertController.textFields?[1].text
                else {
                    return
            }
            completion(oldPin, newPin)
        }
        let cancel : UIAlertAction = UIAlertAction.init(title: "Cancel", style: .cancel) { (_) in
            MyLogger.updateLogMessage(self.textViewLogger, message: "User cancelled the operation")
        }
        alertController.addAction(ok)
        alertController.addAction(cancel)
        alertController.addTextField { (textField) in
            textField.placeholder = "PIN"
            textField.isSecureTextEntry = true
        }
        alertController.addTextField { (textField) in
            textField.placeholder = "New PIN"
            textField.isSecureTextEntry = true
        }
        self.present(alertController, animated: true, completion: nil)
    }
    
    func textFieldShouldReturn(_ textField: UITextField) -> Bool {
        textField.resignFirstResponder()
        return true
    }
    
    override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
        view.endEditing(true)
    }
    
    func updateStatus(_ log: String) {
        print("Update the status ")
        DispatchQueue.main.async {
            MyLogger.updateLogMessage(self.textViewLogger, message: log)
        }
        
    }
    
}



