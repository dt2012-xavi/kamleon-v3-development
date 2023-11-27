package com.dynatech2012.kamleonuserapp.models

import android.util.Log
import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import com.google.common.reflect.TypeToken
import com.google.gson.Gson

@ProvidedTypeConverter
class IntVectorTypeConverter {
    @TypeConverter
    fun fromString(value: String?): IntArray? {
        //val type = object : TypeToken<IntArray>() {}.type
        val type = IntArray::class.java
        Log.d("TAG", "fromString: $value")
        if (value == null)
            return null
        return Gson().fromJson(value, type)
    }

    @TypeConverter
    fun fromIntVector(vector: IntArray?): String? {
        val gson = Gson()
        return gson.toJson(vector)
    }
}