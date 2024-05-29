package   com.assentify.sdk.ContextAware

import CreateUserDocumentResponseModel
import DocumentTokensModel
import SignatureResponseModel


interface ContextAwareSigningCallback {
    fun onHasTokens(documentTokens: List<DocumentTokensModel>);
    fun onCreateUserDocumentInstance(userDocumentResponseModel: CreateUserDocumentResponseModel);
    fun onSignature(signatureResponseModel :SignatureResponseModel);
    fun onError(message :String);

}
