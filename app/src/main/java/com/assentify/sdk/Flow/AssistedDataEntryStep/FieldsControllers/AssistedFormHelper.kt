import com.assentify.sdk.AssistedDataEntry.Models.InputTypes
import com.assentify.sdk.AssistedDataEntryPagesObject
import com.assentify.sdk.Flow.FlowController.FlowController
import com.assentify.sdk.Flow.Models.DataSourceAttribute
import com.assentify.sdk.Flow.Models.DataSourceRequestBody
import com.assentify.sdk.Flow.Models.DataSourceResponse
import com.assentify.sdk.LanguageTransformation.Models.LanguageTransformationModel
import com.assentify.sdk.LanguageTransformation.Models.TransformationModel
import com.assentify.sdk.RemoteClient.Models.ConfigModel
import com.assentify.sdk.RemoteClient.RemoteClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


object AssistedFormHelper {


    fun getDefaultValueValue(key: String, page: Int): String? {
        val model = AssistedDataEntryPagesObject.getAssistedDataEntryModelObject()
        val pages = model!!.assistedDataEntryPages
        val field = pages[page].dataEntryPageElements
            .firstOrNull { it.inputKey == key }

        if (field!!.value.isNullOrEmpty()) {
            if (field.inputPropertyIdentifierList!!.isEmpty()) {
                return "";
            } else {
                var defaultValue = "";
                val doneList = FlowController.getAllDoneSteps();
                doneList.forEach { step ->
                    field.inputPropertyIdentifierList.forEach { keyID ->
                        for (outputProperty in step.stepDefinition!!.customization.outputProperties) {
                            if (outputProperty.keyIdentifier == keyID) {
                                if (defaultValue.isEmpty()) {
                                    defaultValue =
                                        step.submitRequestModel!!.extractedInformation.getValue(
                                            outputProperty.key
                                        )
                                } else {
                                    defaultValue += ",${
                                        step.submitRequestModel!!.extractedInformation.getValue(
                                            outputProperty.key
                                        )
                                    }"
                                }
                            }
                        }

                    }
                }

                changeValue(key, defaultValue, page);
                return defaultValue;


            }
        } else {
            return field.value;
        }

    }

    fun changeValue(key: String, value: String, page: Int) {
        val model = AssistedDataEntryPagesObject.getAssistedDataEntryModelObject()
        val pages = model!!.assistedDataEntryPages
        val field = pages[page].dataEntryPageElements
            .firstOrNull { it.inputKey == key }

        /** Check IF Has Children **/
        if (field!!.children!!.isNotEmpty()) {
            field.children?.forEach { (key, list) ->
                if (key == value && list.isNotEmpty()) {
                    val pageList =
                        model.assistedDataEntryPages[page].dataEntryPageElements.toMutableList()
                    list.forEach { element ->
                        if (!pageList.contains(element)) {
                            pageList.add(element)
                        }
                    }
                    model.assistedDataEntryPages[page].dataEntryPageElements = pageList
                } else {
                    model.assistedDataEntryPages[page].dataEntryPageElements =
                        model.assistedDataEntryPages[page].dataEntryPageElements
                            .toMutableList()
                            .apply {
                                removeAll(list)
                            }
                }
            }

        }
        /** **/

        field.value = value
        AssistedDataEntryPagesObject.setAssistedDataEntryModelObject(model)
    }

    fun changeValueSecureDropdownWithDataSource(
        key: String,
        dataSourceAttribute: List<DataSourceAttribute>,
        outputKeys: Map<String, String>,
        page: Int
    ) {
        val model = AssistedDataEntryPagesObject.getAssistedDataEntryModelObject()
        val pages = model!!.assistedDataEntryPages
        val field = pages[page].dataEntryPageElements
            .firstOrNull { it.inputKey == key }

        /** Check IF Has Children **/
        if (field!!.children!!.isNotEmpty()) {
            field.children?.forEach { (key, list) ->
                if (dataSourceAttribute.isNotEmpty() && key == dataSourceAttribute.first{i->i.mappedKey =="Display Value"}.value && list.isNotEmpty()) {
                    val pageList =
                        model.assistedDataEntryPages[page].dataEntryPageElements.toMutableList()
                    list.forEach { element ->
                        if (!pageList.contains(element)) {
                            pageList.add(element)
                        }
                    }
                    model.assistedDataEntryPages[page].dataEntryPageElements = pageList
                } else {
                    model.assistedDataEntryPages[page].dataEntryPageElements =
                        model.assistedDataEntryPages[page].dataEntryPageElements
                            .toMutableList()
                            .apply {
                                removeAll(list)
                            }
                }
            }

        }
        /** **/

        if (dataSourceAttribute.isNotEmpty()) {
            field.value = dataSourceAttribute.first{i->i.mappedKey =="Display Value"}.value
            field.dataSourceValues = mutableMapOf();
            dataSourceAttribute.forEach { item ->
                field.dataSourceValues!!.put(outputKeys.entries.first { it.key == item.id.toString() }.value,item.value)
            }
            AssistedDataEntryPagesObject.setAssistedDataEntryModelObject(model)
        }

    }


    fun changeRegex(key: String, regex: String, defaultDial: String, page: Int) {
        val model = AssistedDataEntryPagesObject.getAssistedDataEntryModelObject()
        val pages = model!!.assistedDataEntryPages
        val field = pages[page].dataEntryPageElements
            .firstOrNull { it.inputKey == key }

        field!!.applyRegex = true
        field!!.regexDescriptor = regex
        field!!.defaultCountryCode = defaultDial


        AssistedDataEntryPagesObject.setAssistedDataEntryModelObject(model)
    }

    fun validateField(key: String, page: Int): String? {
        val model = AssistedDataEntryPagesObject.getAssistedDataEntryModelObject()
        val pages = model!!.assistedDataEntryPages
        val field = pages[page].dataEntryPageElements
            .firstOrNull { it.inputKey == key }

        val fieldValue = field!!.value ?: ""

        val fieldType = InputTypes.fromString(field.inputType)

        /** Mandatory **/
        if (field.mandatory == true && fieldValue.isEmpty())
            return "This field is required"

        if (fieldValue.isEmpty()) {
            return null
        }

        /**  MaxLength - MinLength **/
        field.minLength?.let { min ->
            if (fieldValue.length < min) return "Minimum $min characters required"
        }
        field.maxLength?.let { max ->
            if (fieldValue.length > max) return "Maximum $max characters allowed"
        }

        /**  Email Regex **/
        if (fieldType == InputTypes.Email && fieldValue.isNotBlank()) {
            val emailRegex = Regex(
                "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$",
                RegexOption.IGNORE_CASE
            )
            if (!emailRegex.matches(fieldValue)) {
                return field.regexErrorMessage.takeUnless { it.isNullOrEmpty() }
                    ?: "Please enter a valid email address"

            }
        }

        /**  Regex **/
        if (field.applyRegex!!) {
            val emailRegex = Regex(
                field.regexDescriptor!!,
                RegexOption.IGNORE_CASE
            )
            val finalValue = fieldValue;
            if (!emailRegex.matches(finalValue)) {
                return field.regexErrorMessage.takeUnless { it.isNullOrEmpty() }
                    ?: "Please enter a valid value"

            }
        }

        return null;
    }


    fun changeLocalOtpValid(key: String, value: Boolean, page: Int) {
        val model = AssistedDataEntryPagesObject.getAssistedDataEntryModelObject()
        val pages = model!!.assistedDataEntryPages
        val field = pages[page].dataEntryPageElements
            .firstOrNull { it.inputKey == key }

        field!!.isLocalOtpValid = value

        AssistedDataEntryPagesObject.setAssistedDataEntryModelObject(model)
    }

    fun validatePage(
        page: Int
    ): Boolean {
        val model = AssistedDataEntryPagesObject.getAssistedDataEntryModelObject()
        val pages = model!!.assistedDataEntryPages
        val fields = pages[page].dataEntryPageElements
        for (f in fields) {
            val err = validateField(f.inputKey!!, page)
            if (!err.isNullOrEmpty()) return false
        }
        /// SDK TODO
        /* for (f in fields) {
             val fieldType = InputTypes.fromString(f.inputType)
             if (fieldType == InputTypes.EmailWithOTP || fieldType == InputTypes.PhoneNumberWithOTP) {
                 if (!f.isLocalOtpValid) return false
             }
         }*/
        return true
    }

    fun valueTransformation(
        language: String,
        transformationModel: TransformationModel,
        onResult: (LanguageTransformationModel?) -> Unit
    ) {

        val call = RemoteClient.remoteLanguageTransform.transformData(
            "Api",
            language,
            transformationModel
        )
        call.enqueue(object : Callback<List<LanguageTransformationModel>> {
            override fun onResponse(
                call: Call<List<LanguageTransformationModel>>,
                response: Response<List<LanguageTransformationModel>>
            ) {
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    if (responseBody != null) {
                        onResult(responseBody.firstOrNull());
                    }
                } else {
                    onResult(null)
                }
            }

            override fun onFailure(call: Call<List<LanguageTransformationModel>>, t: Throwable) {
                onResult(null)
            }
        })

    }


    fun getDataSourceValues(
        configModel: ConfigModel,
        elementIdentifier: String,
        stepId: Int,
        endpointId: Int,
        onResult: (DataSourceResponse?) -> Unit
    ) {

        val call = RemoteClient.remoteGatewayService.getDataSourceValues(
            "", "Android SDK",
            configModel.flowInstanceId,
            configModel.tenantIdentifier,
            configModel.blockIdentifier,
            configModel.instanceId,
            configModel.flowIdentifier,
            configModel.instanceHash,
            elementIdentifier,
            stepId,
            endpointId,
            DataSourceRequestBody(
                filterKeyValues = emptyMap(),
                inputKeyValues = emptyMap()
            )
        )
        call.enqueue(object : Callback<DataSourceResponse> {
            override fun onResponse(
                call: Call<DataSourceResponse>,
                response: Response<DataSourceResponse>
            ) {
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    if (responseBody != null) {
                        onResult(responseBody);
                    }
                } else {
                    onResult(null)
                }
            }

            override fun onFailure(call: Call<DataSourceResponse>, t: Throwable) {
                onResult(null)
            }
        })

    }

}




