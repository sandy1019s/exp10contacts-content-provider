package com.example.exp10

import android.content.Context
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.exp10.databinding.ActivityContactDetailBinding
import com.example.exp10.model.Contact
import com.example.exp10.repository.ContactRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

class ContactDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityContactDetailBinding
    private var contact: Contact? = null
    private var fetchedEmail: String? = null

    private val avatarColors = intArrayOf(
        R.color.avatar_1,
        R.color.avatar_2,
        R.color.avatar_3,
        R.color.avatar_4,
        R.color.avatar_5,
        R.color.avatar_6,
        R.color.avatar_7,
        R.color.avatar_8
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContactDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        contact = getContactFromIntent()

        if (contact == null) {
            Toast.makeText(this, "Unable to load contact details", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupToolbar()
        displayContactData(contact!!)
        loadEmailAddress(contact!!)
        setupClickListeners(contact!!)
    }

    private fun getContactFromIntent(): Contact? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(MainActivity.EXTRA_CONTACT, Contact::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(MainActivity.EXTRA_CONTACT)
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbarDetail)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbarDetail.setNavigationOnClickListener {
            finish()
        }
    }

    private fun displayContactData(contact: Contact) {
        binding.detailTvName.text = contact.name
        binding.detailTvPhone.text = contact.phoneNumber
        binding.detailTvContactId.text = "ID: ${contact.id}"
        binding.detailTvEmail.text = "Loading email address..."

        // Avatar
        binding.detailTvInitials.text = contact.getInitials()

        if (!contact.photoUri.isNullOrBlank()) {
            try {
                val uri = Uri.parse(contact.photoUri)
                binding.detailImgPhoto.setImageURI(uri)
                binding.detailImgPhoto.visibility = View.VISIBLE
                binding.detailTvInitials.visibility = View.GONE
            } catch (e: Exception) {
                showInitialsAvatar(this, contact.name)
            }
        } else {
            showInitialsAvatar(this, contact.name)
        }
    }

    private fun showInitialsAvatar(context: Context, name: String) {
        binding.detailImgPhoto.visibility = View.GONE
        binding.detailTvInitials.visibility = View.VISIBLE

        val colorRes = avatarColors[abs(name.hashCode()) % avatarColors.size]
        val colorInt = ContextCompat.getColor(context, colorRes)

        val background = binding.detailAvatarContainer.background
        if (background is GradientDrawable) {
            background.setColor(colorInt)
        } else {
            val shape = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(colorInt)
            }
            binding.detailAvatarContainer.background = shape
        }
    }

    private fun loadEmailAddress(contact: Contact) {
        if (!contact.email.isNullOrBlank()) {
            fetchedEmail = contact.email
            binding.detailTvEmail.text = contact.email
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val email = ContactRepository.fetchEmailForContact(this@ContactDetailActivity, contact.id)
            withContext(Dispatchers.Main) {
                fetchedEmail = email
                if (!email.isNullOrBlank()) {
                    binding.detailTvEmail.text = email
                } else {
                    binding.detailTvEmail.text = getString(R.string.no_email_available)
                }
            }
        }
    }

    private fun setupClickListeners(contact: Contact) {
        binding.btnMainShare.setOnClickListener {
            shareContact(contact)
        }

        binding.btnCall.setOnClickListener {
            callContact(contact.phoneNumber)
        }

        binding.btnSms.setOnClickListener {
            messageContact(contact.phoneNumber)
        }
    }

    private fun shareContact(contact: Contact) {
        val shareText = if (!fetchedEmail.isNullOrBlank()) {
            getString(R.string.sharing_text_with_email, contact.name, contact.phoneNumber, fetchedEmail)
        } else {
            getString(R.string.sharing_text, contact.name, contact.phoneNumber)
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Contact: ${contact.name}")
            putExtra(Intent.EXTRA_TEXT, shareText)
        }

        val chooser = Intent.createChooser(shareIntent, getString(R.string.share_via))
        startActivity(chooser)
    }

    private fun callContact(phoneNumber: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$phoneNumber")
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Unable to launch dialer", Toast.LENGTH_SHORT).show()
        }
    }

    private fun messageContact(phoneNumber: String) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$phoneNumber")
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Unable to launch messaging app", Toast.LENGTH_SHORT).show()
        }
    }
}
