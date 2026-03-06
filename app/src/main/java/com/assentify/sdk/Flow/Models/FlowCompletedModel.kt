import com.assentify.sdk.RemoteClient.Models.SubmitRequestModel


data class FlowCompletedModel(
    var stepData: Map<String, String>,
    var submitRequestModel: SubmitRequestModel?,
)