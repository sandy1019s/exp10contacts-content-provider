package com.example.exp10.repository

import android.content.Context
import android.provider.ContactsContract
import com.example.exp10.model.Contact

object ContactRepository {

    fun fetchContacts(context: Context): List<Contact> {
        val contactsList = mutableListOf<Contact>()
        val uniquePhoneSet = mutableSetOf<String>()

        val contentResolver = context.contentResolver
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI
        )

        val sortOrder = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY} ASC"

        try {
            contentResolver.query(uri, projection, null, null, sortOrder)?.use { cursor ->
                val idIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val lookupIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY)
                val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY)
                val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val photoIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI)

                while (cursor.moveToNext()) {
                    val id = if (idIndex != -1) cursor.getString(idIndex) ?: "" else ""
                    val lookupKey = if (lookupIndex != -1) cursor.getString(lookupIndex) ?: "" else ""
                    val rawName = if (nameIndex != -1) cursor.getString(nameIndex) else null
                    val rawNumber = if (numberIndex != -1) cursor.getString(numberIndex) else null
                    val photoUri = if (photoIndex != -1) cursor.getString(photoIndex) else null

                    val name = if (!rawName.isNullOrBlank()) rawName.trim() else "Unknown Contact"
                    val number = rawNumber?.trim() ?: ""

                    if (number.isNotEmpty()) {
                        val normalizedNumber = number.replace(Regex("[^0-9+]"), "")
                        val uniqueKey = "$id-$normalizedNumber"

                        if (uniquePhoneSet.add(uniqueKey)) {
                            contactsList.add(
                                Contact(
                                    id = id,
                                    lookupKey = lookupKey,
                                    name = name,
                                    phoneNumber = number,
                                    photoUri = photoUri
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return contactsList
    }

    fun fetchEmailForContact(context: Context, contactId: String): String? {
        if (contactId.isBlank()) return null

        val contentResolver = context.contentResolver
        val uri = ContactsContract.CommonDataKinds.Email.CONTENT_URI

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Email.ADDRESS
        )

        val selection = "${ContactsContract.CommonDataKinds.Email.CONTACT_ID} = ?"
        val selectionArgs = arrayOf(contactId)

        try {
            contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                val addressIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
                if (cursor.moveToFirst() && addressIndex != -1) {
                    val email = cursor.getString(addressIndex)
                    if (!email.isNullOrBlank()) {
                        return email.trim()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }
}
