package   com.assentify.sdk.ContextAware

import com.assentify.sdk.RemoteClient.Models.ContextAwareSigningModel
import com.assentify.sdk.RemoteClient.Models.CreateUserDocumentResponseModel
import com.assentify.sdk.RemoteClient.Models.DocumentTokensModel
import com.assentify.sdk.RemoteClient.Models.SignatureResponseModel


interface ContextAwareSigningCallback {
    fun onHasTokens(documentTokens: List<DocumentTokensModel>,contextAwareSigningModel : ContextAwareSigningModel?);
    fun onCreateUserDocumentInstance(userDocumentResponseModel: CreateUserDocumentResponseModel);
    fun onSignature(signatureResponseModel : SignatureResponseModel);
    fun onError(message :String);

}
