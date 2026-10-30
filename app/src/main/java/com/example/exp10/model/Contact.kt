package com.example.exp10.model

import android.os.Parcel
import android.os.Parcelable

data class Contact(
    val id: String,
    val lookupKey: String,
    val name: String,
    val phoneNumber: String,
    val email: String? = null,
    val photoUri: String? = null
) : Parcelable {

    constructor(parcel: Parcel) : this(
        id = parcel.readString() ?: "",
        lookupKey = parcel.readString() ?: "",
        name = parcel.readString() ?: "",
        phoneNumber = parcel.readString() ?: "",
        email = parcel.readString(),
        photoUri = parcel.readString()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(lookupKey)
        parcel.writeString(name)
        parcel.writeString(phoneNumber)
        parcel.writeString(email)
        parcel.writeString(photoUri)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Contact> {
        override fun createFromParcel(parcel: Parcel): Contact {
            return Contact(parcel)
        }

        override fun newArray(size: Int): Array<Contact?> {
            return arrayOfNulls(size)
        }
    }

    fun getInitials(): String {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) return "?"
        
        val parts = trimmedName.split("\\s+".toRegex()).filter { it.isNotEmpty() }
        return when {
            parts.size >= 2 -> {
                val first = parts[0].firstOrNull()?.uppercaseChar() ?: ""
                val second = parts[1].firstOrNull()?.uppercaseChar() ?: ""
                "$first$second"
            }
            parts.isNotEmpty() -> {
                parts[0].firstOrNull()?.uppercaseChar()?.toString() ?: "?"
            }
            else -> "?"
        }
    }
}
