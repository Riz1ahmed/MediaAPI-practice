package com.learner.codereducer.utils

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/** @author [Riz1Ahmed](https://fb.com/Riz1Ahmed)
 *
 * Date: 15/12/2021*/
object GSonUtils {
    /**
     * Convert data to json string.
     * @param data it would be any type of data. Would be
     * Int,String,Float or model data class EX: UserInfo,ProductModel etc.
     *
     * You will rollback your data by call fromJson function
     * with parameter of it's returned value.
     */
    fun <T> toJson(data: T): String? = Gson().toJson(data)

    /**
     * @param jsonData jsonString of T type data.
     * T would be anything (data class, Int, String etc).
     *
     * call process: GSonUtils.fromJson<Type>(JsonStrData)
     *
     * Ex:
     *  1. val products = GSonUtils.fromJson<ArrayList<ProductData>>(productsJson)
     *  2. val products:ArrayList<ProductData> = GSonUtils.fromJson(productsJson)
     */
    inline fun <reified T> fromJson(jsonData: String): T {
        return Gson().genericFromJson<T>(jsonData)
    }

    /**
     * Json Generic type converter.
     *
     * Please do not call this method from anywhere.
     * It's actually inner method of this object
     * */
    inline fun <reified T> Gson.genericFromJson(jsonData: String): T {
        return fromJson(jsonData, object : TypeToken<T>() {}.type)
    }

}