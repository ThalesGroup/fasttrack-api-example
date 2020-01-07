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

import Foundation

/**
 * FastTrack - messenger configurations
 */
class MessengerConfigurations {

    /**
     * Replace this URL with your Mobile Messenger Server URL.
     */
    class func messengerServerUrl() -> URL {
        let clientUrl = "https://localhost/oobs-messenger/api/client/v1/action"
        
        return URL(string: clientUrl)!
    }

    /**
     * Get applicationId
     */
    class func applicationId() -> String {
        return "your-application-id"
    }

    /**
     * Get userId
     */
    class func userId() -> String {
        return "your-user-id"
    }

    /**
     * Get userAlias
     */
    class func userAlias() -> String {
        return "your-user-alias"
    }

    /**
     * Get domain
     */
    class func domain() -> String {
        return "your-domain"
    }

    /**
     * Get providerId
     */
    class func providerId() -> String {
        return "your-provide-id"
    }

    /**
     * Get the public key - exponent
     */
    class func rsaExponent() -> Data {
        let exponent : Array<CUnsignedChar> = [0x00]
        return Data(bytes: exponent)
    }

    /**
     * Get the public key - modulus
     */
    class func rsaModulus() -> Data {
        let modulus : Array<CUnsignedChar> = [0x00]
        return Data(bytes: modulus)
    }

    /**
     * Get obfuscation key
     */
    class func obfuscationKey() -> Data? {
        return nil
    }

    /**
     * Get public key for signature
     */
    class func signaturePublicKeyPem() -> String? {
        return nil
    }
}
