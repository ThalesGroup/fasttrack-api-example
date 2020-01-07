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
import UIKit

class MyLogger {

    class func clearLog(_ textView: UITextView?) {
        guard let textView = textView else {
            return
        }

        textView.attributedText = NSAttributedString.init(string: "")
    }

    class func updateLog(_ textView: UITextView?, title: String!, message: String!)  {
        MyLogger.updateLogTitle(textView, title: title)
        MyLogger.updateLogMessage(textView, message: message)
    }

    class func updateLogTitle(_ textView: UITextView?, title: String!) {
        guard let textView = textView else {
            return
        }

        MyLogger.appendLogTitle(textView, title: title)
        MyLogger.appendBreakLine(textView)
    }

    class func updateLogMessage(_ textView: UITextView?, message: String!) {
        guard let textView = textView else {
            return
        }
        
        MyLogger.appendNormalLog(textView, message: message)
        MyLogger.appendBreakLine(textView)
    }

    class func updateLogMessage(_ textView: UITextView?, message: String!, status: Bool) {
        guard let textView = textView else {
            return
        }
        
        if status {
            MyLogger.appendNormalLog(textView, message: message)
        } else {
            MyLogger.appendFailureLog(textView, message: message)
        }
        MyLogger.appendBreakLine(textView)
    }

    // MARK: - Helper

    class private func appendLogTitle(_ textView: UITextView!, title: String!) {
        let attrContent : NSMutableAttributedString = NSMutableAttributedString.init(string: title)
        attrContent.addAttribute(.font, value: UIFont.preferredFont(forTextStyle: .title1), range: NSMakeRange(0, attrContent.length))
        attrContent.addAttribute(.foregroundColor, value: UIColor.blue, range: NSMakeRange(0, attrContent.length))

        MyLogger.appendAttributedString(textView, text: attrContent)
    }

    class private func appendFailureLog(_ textView: UITextView!, message: String!) {
        let attrContent : NSMutableAttributedString = NSMutableAttributedString.init(string: message)
        attrContent.addAttribute(.font, value: UIFont.preferredFont(forTextStyle: .body), range: NSMakeRange(0, attrContent.length))
        attrContent.addAttribute(.foregroundColor, value: UIColor.red, range: NSMakeRange(0, attrContent.length))

        MyLogger.appendAttributedString(textView, text: attrContent)
    }

    class private func appendNormalLog(_ textView: UITextView!, message: String!) {
        let attrContent : NSMutableAttributedString = NSMutableAttributedString.init(string: message)
        attrContent.addAttribute(.font, value: UIFont.preferredFont(forTextStyle: .body), range: NSMakeRange(0, attrContent.length))
        attrContent.addAttribute(.foregroundColor, value: UIColor.green, range: NSMakeRange(0, attrContent.length))

        MyLogger.appendAttributedString(textView, text: attrContent)
    }

    class private func appendBreakLine(_ textView: UITextView!) {
        MyLogger.appendNormalLog(textView, message: "\n")
    }

    class private func appendAttributedString(_ textView: UITextView!, text: NSAttributedString!) {
        let string : NSMutableAttributedString = NSMutableAttributedString()
        if let txt = textView.attributedText {
            string.append(txt)
        }
        string.append(text)

        textView.attributedText = string
        textView.scrollRangeToVisible(NSMakeRange(string.length - 1, 1))
    }
}
