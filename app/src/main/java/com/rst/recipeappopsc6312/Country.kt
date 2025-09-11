package com.rst.recipeappopsc6312

import com.google.gson.annotations.SerializedName

data class Country(
    @SerializedName("name") val nameInfo: CountryName,
    @SerializedName("cca2") val code: String,
    @SerializedName("flags") val flags: FlagInfo
)

data class CountryName(
    @SerializedName("common") val common: String
)

data class FlagInfo(
    @SerializedName("png") val png: String
)