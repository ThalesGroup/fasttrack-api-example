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

class ProtectorConfigurations {
    /**
     * Replace this URL with your EPS URL.
     */
    class func epsUrl() -> URL {
        return URL(string:"https://localhost/provision")!
    }
    
    /**
     * Replace this string with your own domain.
     *
     * This is specific to the configuration of the bank's system. Therefore
     * other values should be used here.
     */
    class func domain() -> String {
        return "your-eps-domain"
    }
    /**
     * Replace this string with your own EPS key ID.
     *
     * This is specific to the configuration of the bank's system. Therefore
     * other values should be used here.
     */
    class func rsaKeyId() -> String {
        return "your-eps-rsa-key-id"
    }
    
    /**
     * Replace this byte array with your own EPS key modulus.
     *
     * The EPS' RSA modulus. This is specific to the configuration of the
     * bank's system.  Therefore other values should be used here.
     */
    class func rsaKeyModulus() -> Data {
        // Security Guideline: GEN13. Integrity of public keys
        // Since this example hard codes the key and does not load it from a
        // file, this guideline is skipped.
        
        // Security Guideline: GEN17. RSA key length
        // 2048 bit key
        let rawRsa : Array<CUnsignedChar> =
            [0x00]
        
        return Data(bytes: rawRsa)
    }
    
    /**
     * Replace this byte array with your own EPS key exponent.
     *
     * The EPS' RSA exponent. This is specific to the configuration of the
     * bank's system.  Therefore other values should be used here.
     */
    class func rsaKeyExponent() -> Data {
        // Security Guideline: GEN13. Integrity of public keys
        // Since this example hard codes the key and does not load it from a
        // file, this guideline is skipped.
        let raw : Array<CUnsignedChar> = [0x00]
        return Data(bytes: raw)
    }
    
    /**
     * The custom fingerprint data that seals all the token credentials in this
     * example.
     *
     * This data does not need to be modified in order to use this example app.
     */
    class func customFingerprintData() -> Data {
        // This example simply uses the bundle identifier.
        //
        // This is one example of possible data that can be used for the custom
        // data. It provides domain separation so that the data stored by the
        // Ezio Mobile SDK is different for this application than it would be
        // for another bank's application. More data can be appended to
        // further improve the fingerprinting.
        
        let bundleId = "com.gemalto.fasttrackexample.FastTrackExample"
        return bundleId.data(using: .utf8)!
    }
    
    class func tokenDeviceName(_ otpType: OtpType) ->String {
        var tokenName = "UNKNOWN"
        
        switch (otpType) {
        case .TOTP:
            tokenName = "TOKEN_TOTP_" + createRandomString(5)
            break
        case .OCRA:
            tokenName = "TOKEN_OCRA_" + createRandomString(5)
            break
        case .CAP:
            tokenName = "TOKEN_CAP_" + createRandomString(5)
            break
        }
        
        return tokenName
    }
    
    private class func createRandomString(_ n: Int) -> String
    {
        let a = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890"
        
        var s = ""
        
        for _ in 0..<n
        {
            let r = Int(arc4random_uniform(UInt32(a.count)))
            
            s += String(a[a.index(a.startIndex, offsetBy: r)])
        }
        
        return s
    }
}

