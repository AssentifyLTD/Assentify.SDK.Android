package com.assentify.sdk.LanguageTransformation

import LanguageTransformationModel
import TransformationModel
import android.util.Log
import com.assentify.sdk.RemoteClient.RemoteClient
import com.assentify.sdk.LanguageTransformation.LanguageTransformationCallback
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

public class LanguageTransformation(private var apiKey: String) {

    private var callback: LanguageTransformationCallback? = null

    fun setCallback(callback: LanguageTransformationCallback) {
        this.callback = callback
    }

    fun languageTransformation(
        language: String,
        transformationModel: TransformationModel
    ) {

        val call = RemoteClient.remoteLanguageTransform.transformData(
            this.apiKey,
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
                        callback!!.onTranslatedSuccess(mergeKeyValue(responseBody));
                    }
                } else {
                    callback!!.onTranslatedError(mergeKeyValue(transformationModel.LanguageTransformationModels));
                }
            }
            override fun onFailure(call: Call<List<LanguageTransformationModel>>, t: Throwable) {
                callback!!.onTranslatedError(mergeKeyValue(transformationModel.LanguageTransformationModels));
            }
        })

    }

    private fun mergeKeyValue(
        languageTransformationList: List<LanguageTransformationModel>
    ): Map<String, String> {
        val properties: MutableMap<String, String> = mutableMapOf()
        languageTransformationList.forEach {
            properties.put(it.key, it.value)
        }
        return properties
    }

}