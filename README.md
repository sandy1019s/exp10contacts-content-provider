# Experiment 10: Implement Data Sharing Using Contacts Content Provider

## App Name: Connectly — Smart Contacts

---

## 1. Aim
To design, build, and deploy a modern Android application (**Connectly — Smart Contacts**) demonstrating how an Android application retrieves contact information from the built-in **Android Contacts Content Provider** using `ContentResolver`, `ContactsContract`, and `Cursor`, and shares contact details using explicit intents (`Intent.ACTION_SEND`) and `Intent.createChooser()`.

---

## 2. Software and Hardware Requirements

### Software Requirements
- **Operating System**: Windows 10/11, macOS, or Linux
- **Development Tool**: Android Studio Ladybug / Koala / Hedgehog or newer
- **Programming Language**: Kotlin
- **SDK & Build System**:
  - `minSdk`: 24 (Android 7.0 Nougat)
  - `targetSdk`: 35 (Android 15)
  - `compileSdk`: 37
  - Build System: Gradle 8.x with Kotlin DSL (`build.gradle.kts`)
- **Android Jetpack Libraries**:
  - `androidx.appcompat:appcompat`
  - `androidx.core:core-ktx`
  - `androidx.constraintlayout:constraintlayout`
  - `com.google.android.material:material` (Material 3)
  - `androidx.swiperefreshlayout:swiperefreshlayout`

### Hardware Requirements
- **Processor**: Intel Core i5/i7 or AMD Ryzen 5/7
- **RAM**: Minimum 8 GB (16 GB recommended)
- **Disk Space**: At least 10 GB free space
- **Test Device**: Physical Android Phone or Android Virtual Device (AVD) running Android 7.0+ with sample contacts.

---

## 3. Technologies Used

- **Kotlin**: Core application programming language.
- **XML Layouts & Material Design 3**: UI design, custom cards, shapeable avatars, dark mode styling.
- **Android Contacts Content Provider**: System component managing device contacts store.
- **`ContactsContract`**: Contract class providing URIs and column definitions for contacts data.
- **`ContentResolver`**: Interface used to query system content providers.
- **`Cursor`**: Tabular dataset returned by content provider queries.
- **Runtime Permissions**: Dynamic runtime check and request for `READ_CONTACTS`.
- **ViewBinding**: Type-safe layout view referencing.
- **RecyclerView & ListAdapter with DiffUtil**: Efficient contact list rendering and dynamic search filtering.
- **Android Explicit Intents & Sharesheet**: `Intent.ACTION_SEND`, `Intent.ACTION_DIAL`, `Intent.ACTION_SENDTO`, and `Intent.createChooser()`.

---

## 4. Procedure

1. **Project Setup & Configuration**:
   - Initialize Android Studio project with `com.example.exp10` namespace.
   - Configure `build.gradle.kts` to enable ViewBinding and Kotlin Parcelize.
   - Declare `<uses-permission android:name="android.permission.READ_CONTACTS" />` in `AndroidManifest.xml`.

2. **Data Model Creation**:
   - Create `Contact.kt` implementing `Parcelable` to store `id`, `lookupKey`, `name`, `phoneNumber`, `email`, and `photoUri`.
   - Implement helper functions to generate initial badges for contact avatars.

3. **Content Provider Repository Implementation**:
   - Create `ContactRepository.kt` to encapsulate database operations.
   - Query `ContactsContract.CommonDataKinds.Phone.CONTENT_URI` to fetch contact display names, phone numbers, ID, and photo thumbnail URIs.
   - Safely read cursor columns, iterate records, and handle null or blank values.
   - Deduplicate contacts based on contact ID and normalized phone numbers.
   - Query `ContactsContract.CommonDataKinds.Email.CONTENT_URI` asynchronously to fetch contact email addresses.

4. **UI & Adapter Construction**:
   - Build `activity_main.xml` with top app bar, dashboard card, live search input, `SwipeRefreshLayout`, `RecyclerView`, and state layouts (Loading, Permission Denied, Empty Contacts, Search No Results).
   - Build `item_contact.xml` for RecyclerView row items with circular initial badge and quick share button.
   - Build `activity_contact_detail.xml` for contact detail screen with quick action buttons (Call, SMS, Share).
   - Implement `ContactAdapter.kt` using `ListAdapter` and `DiffUtil` with real-time text filter function.

5. **Activities & Intent Handling**:
   - Implement `MainActivity.kt` with runtime permission request flow using `ActivityResultContracts.RequestPermission()`.
   - Load contacts in background thread via Kotlin Coroutines (`Dispatchers.IO`) when permission is granted.
   - Implement `ContactDetailActivity.kt` to display detailed contact information and handle quick call (`Intent.ACTION_DIAL`), SMS (`Intent.ACTION_SENDTO`), and sharing (`Intent.ACTION_SEND` + `Intent.createChooser()`).

6. **Testing & Verification**:
   - Test on emulator and physical device under granted and denied permission conditions.
   - Verify contact search filtering, contact selection, and intent chooser functionality.

---

## 5. Explanation of Key Android Components

### 1. `ContactsContract`
The framework contract class containing constants and URIs for the contacts database schema. It defines tables such as:
- `ContactsContract.CommonDataKinds.Phone.CONTENT_URI`: URI pointing to phone number entries.
- `ContactsContract.CommonDataKinds.Email.CONTENT_URI`: URI pointing to email address entries.

### 2. `ContentResolver`
The application client object used to communicate with system ContentProviders. Calling `contentResolver.query()` executes a structured query against the system contacts provider database.

### 3. `Cursor`
A pointer object providing read-write access to the result set returned by a content provider database query. Managed safely using Kotlin's `.use { }` extension block to ensure automatic closure and prevent memory leaks.

### 4. `READ_CONTACTS` Permission & Runtime Request
Accessing system contacts requires declaring `<uses-permission android:name="android.permission.READ_CONTACTS" />` in `AndroidManifest.xml` and explicitly requesting permission at runtime on Android 6.0+ (API 23+) using `ActivityResultContracts.RequestPermission()`.

### 5. `Intent.ACTION_SEND` & `Intent.createChooser()`
- `Intent.ACTION_SEND` sends text data (`"text/plain"`) to external applications.
- `Intent.createChooser(intent, "Share Contact Details via")` displays the standard Android Sharesheet, enabling users to choose messaging, email, or social networking apps.

---

## 6. Implementation Code Snippets

### 1. Content Provider Contact Query (`ContactRepository.kt`)
```kotlin
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

    contentResolver.query(uri, projection, null, null, sortOrder)?.use { cursor ->
        val idIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
        val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY)
        val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

        while (cursor.moveToNext()) {
            val id = if (idIndex != -1) cursor.getString(idIndex) ?: "" else ""
            val name = if (nameIndex != -1) cursor.getString(nameIndex) ?: "Unknown" else "Unknown"
            val number = if (numberIndex != -1) cursor.getString(numberIndex) ?: "" else ""

            if (number.isNotEmpty() && uniquePhoneSet.add("$id-$number")) {
                contactsList.add(Contact(id = id, lookupKey = "", name = name, phoneNumber = number))
            }
        }
    }
    return contactsList
}
```

### 2. Runtime Permission Request (`MainActivity.kt`)
```kotlin
private val requestPermissionLauncher = registerForActivityResult(
    ActivityResultContracts.RequestPermission()
) { isGranted ->
    if (isGranted) {
        loadContactsFromProvider()
    } else {
        showPermissionDeniedState()
    }
}

private fun checkPermissionAndLoadContacts() {
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
        loadContactsFromProvider()
    } else {
        requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
    }
}
```

### 3. Contact Sharing via Intent Chooser (`ContactDetailActivity.kt`)
```kotlin
private fun shareContact(contact: Contact) {
    val shareText = "Name: ${contact.name}\nPhone: ${contact.phoneNumber}"
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Contact: ${contact.name}")
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    startActivity(Intent.createChooser(shareIntent, "Share Contact Details via"))
}
```

---

## 7. Screenshots

| Permission Request Screen | Contact Details Screen |
| :---: | :---: |
| ![Permission Request Screen](screenshots/permission_request.png) | ![Contact Details Screen](screenshots/contact_details_screen.png) |

| Main Contacts List | Contact Sharing Intent |
| :---: | :---: |
| ![Main Contacts Screen](screenshots/main_contacts_screen.png) | ![Contact Sharing Screen](screenshots/contact_sharing_screen.png) |

*Note: You can place additional screenshot image files in the `screenshots/` directory using the filenames listed above.*

---

## 8. Output Description

- **Permission Request Screen**: Displays a runtime dialog requesting `READ_CONTACTS` permission upon launching Connectly.
- **Main Contacts Screen**: Displays top app bar with Connectly branding, total contact count badge, search filter field, and an alphabetically ordered list of device contacts with circular initials and quick share icons.
- **Contact Detail Screen**: Displays large contact avatar/photo, full display name, phone number, email address (or loading status), contact ID, and quick action buttons for Call, Message, and Share.
- **Contact Sharing Chooser**: Opens the Android system Sharesheet presenting installed messaging, social, and email applications with pre-filled contact details.

---

## 9. Result
The Android application **Connectly — Smart Contacts** was successfully created, compiled, tested, and verified. It retrieves live contact information from the Android **Contacts Content Provider** using `ContentResolver`, displays them in a clean Material Design 3 interface with search capabilities, handles runtime permissions safely, and shares contact details via `Intent.ACTION_SEND` and `Intent.createChooser()`.

---

## 10. How to Run the Project

1. **Clone the Repository**:
   ```bash
   git clone https://github.com/sandy1019s/exp10contacts-content-provider.git
   cd exp10contacts-content-provider
   ```
2. **Open in Android Studio**:
   - Open Android Studio (Ladybug / Koala / Hedgehog or newer).
   - Select **Open an Existing Project** and navigate to the cloned `exp10contacts-content-provider` directory.
3. **Gradle Sync & Build**:
   - Allow Gradle to sync dependencies automatically.
   - Build the project using `Build -> Make Project` or `./gradlew assembleDebug`.
4. **Run on Device or Emulator**:
   - Connect an Android device (with USB debugging enabled) or start an AVD.
   - Ensure the device/emulator has contacts in its Contacts app.
   - Click **Run 'app'** or press `Shift + F10`.
5. **Grant Permission & Explore**:
   - Tap **Allow** when prompted for contacts permission.
   - Use the search bar to filter contacts, tap any contact to view details, and tap **Share Contact** to open the Android Sharesheet.
