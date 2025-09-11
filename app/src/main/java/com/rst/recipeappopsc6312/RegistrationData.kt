package com.rst.recipeappopsc6312

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
//
@Parcelize
data class RegistrationData(
    var country: String? = null,
    var selectedCuisines: List<String>? = null,
    var selectedDiets: List<String>? = null,
    var fullName: String? = null,
    var phoneNumber: String? = null,
    var gender: String? = null,
    var dateOfBirth: String? = null,
    var username: String? = null,
    var email: String? = null
) : Parcelable