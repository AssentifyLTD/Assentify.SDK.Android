package  com.assentify.sdk.Core.Constants

import com.assentify.sdk.Core.Constants.HubConnectionTargets
import com.google.common.util.concurrent.ListenableFuture
import io.sentry.Scope
import io.sentry.ScopeCallback
import io.sentry.Sentry
import io.sentry.SentryLevel
import org.checkerframework.checker.initialization.qual.Initialized

object SentryKeys {
    const val Initialized = "Initialized The Sdk"
    const val KeyValidation = "Key Validation"
    const val HasTemplates = "Has Templates"
    const val Passport = "Scan Passport"
    const val ID = "Scan ID"
    const val Other = "Scan Other"
    const val Face = "Face Match"
    const val Submit = "Submit Data"

}

object SentryManager {
    fun registerCallbackEvent(futureName:String,eventName: String, moreInfo: String) {
        if (eventName == HubConnectionTargets.ON_ERROR || eventName == HubConnectionTargets.ON_RETRY || eventName == HubConnectionTargets.ON_NO_FACE_DETECTED || eventName == HubConnectionTargets.ON_NO_MRZ_EXTRACTED || eventName == HubConnectionTargets.ON_UPLOAD_FAILED || eventName == HubConnectionTargets.ON_WRONG_TEMPLATE) {
            Sentry.captureMessage(
                futureName +" , "+ eventName + " : " + moreInfo, ScopeCallback(
                    { scope: Scope? -> scope!!.setLevel(SentryLevel.ERROR) })
            )
        } else {
            Sentry.captureMessage(
                futureName  +" , " + eventName + " : " + moreInfo, ScopeCallback(
                    { scope: Scope? -> scope!!.setLevel(SentryLevel.INFO) })
            )
        }
    }

    fun registerEvent(message: String, sentryLevel: SentryLevel) {
        Sentry.captureMessage(
            message, ScopeCallback(
                { scope: Scope? -> scope!!.setLevel(sentryLevel) })
        );
    }
}