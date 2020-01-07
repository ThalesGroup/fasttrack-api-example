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

enum OtpType: Equatable {
    case TOTP
    case OCRA
    case CAP
}

class ProtectorExample {
    
    var otpType: OtpType = OtpType.TOTP
    var oathProtector: EMOathMobileProtector
    var capProtector: EMCapMobileProtector
    var fastTrack: EMFastTrack
    var oathTokenDevice: EMProtectorOathTokenDevice?
    var capTokenDevice: EMProtectorCapTokenDevice?
    weak var statusLogger:StatusLoggerDelegateProtocol?
    
    var tokenDeviceName:String? {
        didSet {
            guard let name = tokenDeviceName else { return  }
            do {
                switch otpType {
                case .TOTP:
                    // 4.1 Get Token Device to be used to generate OTP
                    oathTokenDevice = try oathProtector.tokenDevice(name: name, fingerpintCustomData: nil)
                case .OCRA:
                    // 4.1 Get Token Device to be used to generate OTP
                    oathTokenDevice = try oathProtector.tokenDevice(name: name, fingerpintCustomData: nil)
                case .CAP:
                    // 4.1 Get Token Device to be used to generate OTP
                    capTokenDevice = try capProtector.tokenDevice(name: name, fingerpintCustomData: nil)
                }
            } catch let error as NSError {
                statusLogger?.updateStatus("Error: \(error)")
            }
        }
    }
    
    init() {
        // 2 Initialization of Mobile Protector which can be either OATH or CAP
        // Normally we only need to choose either one of them
        fastTrack = EMFastTrack.sharedInstance()!

        // 2.a Initialization for Oath Mobile Protector
        oathProtector = fastTrack.oathMobileProtector(url:ProtectorConfigurations.epsUrl(),
                                                         domain:ProtectorConfigurations.domain(),
                                                         provisioningProtocol:.version5,
                                                         rsaKeyId: ProtectorConfigurations.rsaKeyId(),
                                                         rsaExponent: ProtectorConfigurations.rsaKeyExponent(),
                                                         rsaModulus: ProtectorConfigurations.rsaKeyModulus(),
                                                         optionalParameters: nil)
        
        // 2.b Initialization for CAP Mobile Protector
        capProtector = fastTrack.capMobileProtector(url:ProtectorConfigurations.epsUrl(),
                                                       domain:ProtectorConfigurations.domain(),
                                                       provisioningProtocol:.version5,
                                                       rsaKeyId: ProtectorConfigurations.rsaKeyId(),
                                                       rsaExponent: ProtectorConfigurations.rsaKeyExponent(),
                                                       rsaModulus: ProtectorConfigurations.rsaKeyModulus(),
                                                       optionalParameters: nil)
        
    }
    
    func provision(_ registrationCode:String, completion: @escaping (_ tokenName: String?,_ error: Error?) -> Void) {
        switch otpType {
        case .CAP:
            // 3.1 Apply settings for this particular Token
            let settings = EMProtectorCapSettings()
            let tokenDeviceName = ProtectorConfigurations.tokenDeviceName(OtpType.CAP)
            // 3.2 Call provision API to start provisioning,this API call will save the settings locally
            capProtector.provision(tokenDeviceName: tokenDeviceName,
                                    registrationCode: registrationCode,
                                    capSettings: settings,
                                    optionalParameters: nil,
                                    completionHandler:
                { (capTokenDevice:EMProtectorCapTokenDevice?,
                    extensions:[AnyHashable : Any]?,
                    error:Error?) in
                    
                    if(error != nil) {
                        completion(nil, error)
                    }
                    else
                    {
                        self.capTokenDevice = capTokenDevice
                        completion(tokenDeviceName,nil)
                    }
            })
            break
        case .TOTP:
            // 3.1 Apply settings for this particular Token
            let settings = EMProtectorTotpSettings()
            let tokenDeviceName = ProtectorConfigurations.tokenDeviceName(OtpType.TOTP)
            // 3.2 Call Provision API to start provisioning,this API call will save the settings locally
            oathProtector.provision(tokenDeviceName: tokenDeviceName,
                                     registrationCode: registrationCode,
                                     totpSettings: settings,
                                     optionalParameters:nil,
                                     completionHandler:
                { (oathTokenDevice:EMProtectorOathTokenDevice?,
                    extensions:[AnyHashable : Any]?,
                    error:Error?) in
                    
                    if(error != nil) {
                        completion(nil, error)
                    }
                    else
                    {
                        self.oathTokenDevice = oathTokenDevice
                        completion(tokenDeviceName,nil)
                    }
            })
        case .OCRA:
            // 3.1 Apply settings for this particular Token
            let ocraSettings = EMProtectorOcraSettings()
            ocraSettings.startTime = 0
            ocraSettings.ocraSuite = "OCRA-1:HOTP-SHA256-8:C-QA09-PSHA1-S064-T30S"
            let tokenDeviceName = ProtectorConfigurations.tokenDeviceName(OtpType.OCRA)
            // 3.2 Call Provision API to start provisioning ,this API call will save the settings locally
            oathProtector.provision(tokenDeviceName: tokenDeviceName,
                                     registrationCode: registrationCode,
                                     ocraSettings: ocraSettings,
                                     optionalParameters:nil,
                                     completionHandler:
                { (oathTokenDevice:EMProtectorOathTokenDevice?,
                    extensions:[AnyHashable : Any]?,
                    error:Error?) in
                    
                    if(error != nil) {
                        completion(nil, error)
                    }
                    else
                    {
                        self.oathTokenDevice = oathTokenDevice
                        completion(tokenDeviceName,nil)
                    }
            })
            break
        }
    }
    
    func otp(_ pin: String) -> String? {
        var otp:String?
        do {
            switch otpType {
            case .TOTP:
                // 4.2 Generate OTP by passing the authentication: PIN
                otp = try oathTokenDevice?.otp(pin: pin)
            case .OCRA:
                let serverChallenge = "000000003"
                let clientChallenge = "000000003"
                let password = "password"
                let session = "\u{20ac}" + "10"
                let passwordHash = try oathTokenDevice?.ocraPasswordHash(password)
                // 4.2 Generate OTP by passing the authentication: PIN
                otp = try oathTokenDevice?.ocra(pin: pin,
                                                serverChallengeQuestion: serverChallenge,
                                                clientChallengeQuestion: clientChallenge,
                                                passwordHash: passwordHash,
                                                session: session)
            case .CAP:
                otp = generateCapOtpByPin(pin)
            }
        } catch let error as NSError {
            statusLogger?.updateStatus("Error: \(error)")
        }
        return otp
    }
    
    private func generateCapOtpByPin(_ pin: String) ->String? {
        
        guard let capTokenDevice = capTokenDevice else { return nil }
        var CAP_otps_result:String? 

        let challenge = "12345678"
        let amount = "1000"
        let currencyCode = "USD"
        let defaultTds:NSMutableArray = []
        defaultTds.add("1111")
        defaultTds.add("1111")
        defaultTds.add("222")
        defaultTds.add("33")
        defaultTds.add("4")

        // Mode 1
        // 4.2 Generate OTP by passing the authentication: PIN
        do {
            var otp:String?
            otp = try capTokenDevice.otpMode1(withPin: pin, challenge: challenge, amount: amount, currencyCode: currencyCode)
            if CAP_otps_result == nil {
                CAP_otps_result = ""
            }
            CAP_otps_result?.append("otpMode1: \(otp ?? "nil") \n")
        }
        catch let error as NSError {
            statusLogger?.updateStatus("Error: \(error)")
        }
        
        // Mode 2
        // 4.2 Generate OTP by passing the authentication: PIN
        do {
            var otp:String?
            otp = try capTokenDevice.otpMode2(withPin: pin)
            if CAP_otps_result == nil {
                CAP_otps_result = ""
            }
            CAP_otps_result?.append("otpMode2: \(otp ?? "nil") \n")
        }
        catch let error as NSError {
            statusLogger?.updateStatus("Error: \(error)")
        }
        
        // Mode 2 tds
        // 4.2 Generate OTP by passing the authentication: PIN
        do {
            var otp:String?
            otp = try capTokenDevice.otpMode2Tds(withPin: pin, dataToSign: defaultTds)
            if CAP_otps_result == nil {
                CAP_otps_result = ""
            }
            CAP_otps_result?.append("otpMode2Tds: \(otp ?? "nil") \n")
        }
        catch let error as NSError {
            statusLogger?.updateStatus("Error: \(error)")
        }
        
        // Mode 3
        // 4.2 Generate OTP by passing the authentication: PIN
        do {
            var otp:String?
            otp = try capTokenDevice.otpMode3(withPin: pin, challenge: challenge)
            if CAP_otps_result == nil {
                CAP_otps_result = ""
            }
            CAP_otps_result?.append("otpMode3: \(otp ?? "nil") \n")
        }
        catch let error as NSError {
            statusLogger?.updateStatus("Error: \(error)")
        }
        return CAP_otps_result
    }
    
    /**
     * To perform change pin:
     * 1 Call changePin API with current token device by providing current and new PIN
     */
    func changePin(_ oldPin: String, _ newPin: String) ->Bool {
        var retValue: Bool = false
        do {
            switch otpType {
            case .TOTP:
                guard let oathTokenDevice = oathTokenDevice else { return false }
                try oathTokenDevice.changePin(oldPin: oldPin, newPin: newPin)
                retValue = true
            case .OCRA:
                guard let oathTokenDevice = oathTokenDevice else { return false }
                try oathTokenDevice.changePin(oldPin: oldPin, newPin: newPin)
                retValue = true
            case .CAP:
                guard let capTokenDevice = capTokenDevice else { return false }
                try capTokenDevice.changePin(oldPin: oldPin, newPin: newPin)
                retValue = true
            }
        } catch let error as NSError {
            statusLogger?.updateStatus("Error: \(error)")
        }
        return retValue
    }
    
    func getTokenNames() -> Set<String> {
        var retTokenNames = Set<String>()
        do {
            switch otpType {
            case .TOTP:
                let tokenNames = try oathProtector.tokenDeviceNames()
                for tokenName in tokenNames {
                    if (tokenName.hasPrefix("TOKEN_TOTP")) {
                        retTokenNames.insert(tokenName)
                    }
                }
                break
            case .OCRA:
                let tokenNames = try oathProtector.tokenDeviceNames()
                for tokenName in tokenNames {
                    if (tokenName.hasPrefix("TOKEN_OCRA")) {
                        retTokenNames.insert(tokenName)
                    }
                }
                break
            case .CAP:
                let tokenNames = try capProtector.tokenDeviceNames()
                for tokenName in tokenNames {
                    if (tokenName.hasPrefix("TOKEN_CAP")) {
                        retTokenNames.insert(tokenName)
                    }
                }
                break
            }
        }
        catch let error as NSError {
            statusLogger?.updateStatus("Error: \(error)")
        }
        return retTokenNames
    }
    
    func getAllTokenDeviceNames()->Set<String> {
        var retTokenNames = Set<String>()
        do {
            let oathTokenNames = try oathProtector.tokenDeviceNames()
            retTokenNames.formUnion(oathTokenNames)
            
            let capTokenNames = try capProtector.tokenDeviceNames()
            retTokenNames.formUnion(capTokenNames)
        }
        catch let error as NSError {
            statusLogger?.updateStatus("Error: \(error)")
        }
        return retTokenNames
    }
    
    func deleteTokenDevices()->Set<String> {
        let tokenNamesSet = getAllTokenDeviceNames()
        do {
            for tokenName in tokenNamesSet {
                try oathProtector.removeTokenDevice(name: tokenName)
            }
        }
        catch let error as NSError {
            statusLogger?.updateStatus("Error: \(error)")
        }
        return tokenNamesSet
    }
    
    func activateSystemBiometric(_ pin:String) ->Bool{
        
        var isActivated = false
        
        do {
            switch otpType {
            case .TOTP, .OCRA:
                guard let oathTokenDevice = oathTokenDevice else { return false }

                // Make sure the current PIN provided is correct by verifying the OTP value against the Server
                // before calling this API
                try oathTokenDevice.activateSystemBiometricMode(pin: pin)
                isActivated = true
                break

            case .CAP:
                guard let capTokenDevice = capTokenDevice else { return false }

                // Make sure the current PIN provided is correct by verifying the OTP value against the Server
                // before calling this API
                try capTokenDevice.activateSystemBiometricMode(pin: pin)
                isActivated = true
                break
            }
        }
        catch let error as NSError {
            statusLogger?.updateStatus("Error: \(error)")
        }
        
        return isActivated
    }
    
    func checkSystemBiometricActivation() ->Bool{
        
        var isActivated = false
        
        switch otpType {
        case .TOTP, .OCRA:
            
            guard let oathTokenDevice = oathTokenDevice else { return false }

            if (!oathProtector.isSystemBiometricModeSupported()) {
                statusLogger?.updateStatus("SystemBiometric not supported on this device!")
            }
            else if (!oathProtector.isSystemBiometricModeConfigured()) {
                statusLogger?.updateStatus("SystemBiometric not configured on this device!")
            } else {
                if (oathTokenDevice.isSystemBiometricModeActivated()) {
                    isActivated = true
                    break
                }
            }
            break
        case .CAP:
            guard let capTokenDevice = capTokenDevice else { return false }

            if (!capProtector.isSystemBiometricModeSupported()) {
                statusLogger?.updateStatus("SystemBiometric not supported on this device!")
            }
            else if (!capProtector.isSystemBiometricModeConfigured()) {
                statusLogger?.updateStatus("SystemBiometric not configured on this device!")
            } else {
                if (capTokenDevice.isSystemBiometricModeActivated()) {
                    isActivated = true
                    break
                }
            }
            break
        }
        return isActivated
    }
    
    
    /**
     * To perform OTP generation with Biometric Authentication:
     * 1 Get Token Device to be used to generate OTP
     * 2 Call authenticate API
     * 3 Upon successful authentication, generate OTP by passing the authentication
     */
    func otpWithSystemBiometric(completion: @escaping (_ otp: String?,_ error: Error?) -> Void) {
        var otp:String?
        do {
            switch otpType {
            case .TOTP:
                guard let oathTokenDevice = oathTokenDevice else { return }
                oathTokenDevice.authenticateWithMessage(localizedMessage: "Authenticate to get totp", fallbackTitle: "Enter Pin", completionHandler: { (authInput, data, error) in
                    if (error == nil) {
                        do {
                            let otp = try self.oathTokenDevice?.otp(authInput: authInput!)
                            completion(otp, nil)
                        } catch let error as NSError {
                            self.statusLogger?.updateStatus("Error: \(error)")
                            completion(nil,error)
                        }
                    }
                    else
                    {
                        completion(nil,error)
                    }
                })
            case .OCRA:
                guard let oathTokenDevice = oathTokenDevice else { return }
                let serverChallenge = "000000003"
                let clientChallenge = "000000003"
                let password = "password"
                let session = "\u{20ac}" + "10"
                let passwordHash = try oathTokenDevice.ocraPasswordHash(password)
                oathTokenDevice.authenticateWithMessage(localizedMessage: "Authenticate to get ocra", fallbackTitle: "Enter Pin", completionHandler: { (authInput, data, error) in
                    if (error == nil) {
                        do {
                            otp = try oathTokenDevice.ocra(authInput: authInput!,
                                                                 serverChallengeQuestion: serverChallenge,
                                                                 clientChallengeQuestion: clientChallenge,
                                                                 passwordHash: passwordHash,
                                                                 session: session)
                            completion(otp, nil)
                        } catch let error as NSError {
                            self.statusLogger?.updateStatus("Error: \(error)")
                            completion(nil,error)
                        }
                    }
                    else
                    {
                        completion(nil,error)
                    }
                })
            case .CAP:
                guard let capTokenDevice = capTokenDevice else { return }
                capTokenDevice.authenticateWithMessage(localizedMessage: "Authenticate to get cap", fallbackTitle: "Enter Pin", completionHandler: { (authInput, data, error) in
                    if (error == nil) {
                        
                        let otp = self.generateCapOtpByAuthInput(authInput!)
                        completion(otp, nil)
                    }
                    else
                    {
                        completion(nil,error)
                    }
                })
            }
        } catch let error as NSError {
            statusLogger?.updateStatus("Error: \(error)")
        }
    }
    
    private func generateCapOtpByAuthInput(_ authInput: EMProtectorAuthInput) ->String? {
        
        guard let capTokenDevice = capTokenDevice else { return nil }

        var CAP_otps_result:String?

        let challenge = "12345678"
        let amount = "1000"
        let currencyCode = "USD"
        let defaultTds:NSMutableArray = []
        defaultTds.add("1111")
        defaultTds.add("1111")
        defaultTds.add("222")
        defaultTds.add("33")
        defaultTds.add("4")

        // Mode 1
        // 4.2 Generate OTP by passing the authinput
        do {
            var otp:String?
            otp = try capTokenDevice.otpMode1(with: authInput, challenge: challenge, amount: amount, currencyCode: currencyCode)
            if CAP_otps_result == nil {
                CAP_otps_result = ""
            }
            CAP_otps_result?.append("otpMode1: \(otp ?? "nil") \n")
        }
        catch let error as NSError {
            statusLogger?.updateStatus("Error: \(error)")
        }

        // Mode 2
        // 4.2 Generate OTP by passing the authinput
        do {
            var otp:String?
            otp = try capTokenDevice.otpMode2(with: authInput)
            if CAP_otps_result == nil {
                CAP_otps_result = ""
            }
            CAP_otps_result?.append("otpMode2: \(otp ?? "nil") \n")
        }
        catch let error as NSError {
            statusLogger?.updateStatus("Error: \(error)")
        }

        // Mode 2 tds
        // 4.2 Generate OTP by passing the authinput
        do {
            var otp:String?
            otp = try capTokenDevice.otpMode2Tds(with: authInput, dataToSign: defaultTds)
            if CAP_otps_result == nil {
                CAP_otps_result = ""
            }
            CAP_otps_result?.append("otpMode2Tds: \(otp ?? "nil") \n")
        }
        catch let error as NSError {
            statusLogger?.updateStatus("Error: \(error)")
        }

        // Mode 3
        // 4.2 Generate OTP by passing the authinput
        do {
            var otp:String?
            otp = try capTokenDevice.otpMode3(with: authInput, challenge: challenge)
            if CAP_otps_result == nil {
                CAP_otps_result = ""
            }
            CAP_otps_result?.append("otpMode3: \(otp ?? "nil") \n")
        }
        catch let error as NSError {
            statusLogger?.updateStatus("Error: \(error)")
        }

        return CAP_otps_result
    }

    /**
     * To activate Biometric Authentication:
     * 1 Check if current Token Device has been activated previously
     * 2 Call de-activate API by providing PIN value
     */
    func deactivateSystemBiometric() ->Bool {
        var isDeactivated = false
        do {
            switch otpType {
            case .TOTP, .OCRA:
                guard let oathTokenDevice = oathTokenDevice else { return false }
                
                if (oathTokenDevice.isSystemBiometricModeActivated()) {
                    try oathTokenDevice.deactivateSystemBiometricMode()
                    isDeactivated = true
                }
                else{
                    statusLogger?.updateStatus("Biometric authentication is not activated for the current token device")
                }
                break
                
            case .CAP:
                guard let capTokenDevice = capTokenDevice else { return false }
                if (capTokenDevice.isSystemBiometricModeActivated()) {
                    try capTokenDevice.deactivateSystemBiometricMode()
                    isDeactivated = true
                }
                else{
                    statusLogger?.updateStatus("Biometric authentication is not activated for the current token device")
                }
                break
            }
        }
        catch let error as NSError {
            statusLogger?.updateStatus("Error: \(error)")
        }
        return isDeactivated
    }
    
    func checkIsSystemBiometricActivated() ->Bool{
        
        var isActivated = false
        switch otpType {
        case .TOTP, .OCRA:
            guard let oathTokenDevice = oathTokenDevice else { return false }
            if (oathTokenDevice.isSystemBiometricModeActivated()) {
                    isActivated = true
                    break
            }
            break
        case .CAP:
            guard let capTokenDevice = capTokenDevice else { return false }
            if (capTokenDevice.isSystemBiometricModeActivated()) {
                isActivated = true
                break
            }
            break
        }
        return isActivated
    }
}

