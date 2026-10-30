package com.example.exp10

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.exp10.adapter.ContactAdapter
import com.example.exp10.databinding.ActivityMainBinding
import com.example.exp10.model.Contact
import com.example.exp10.repository.ContactRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var contactAdapter: ContactAdapter
    private var allContacts: List<Contact> = emptyList()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            loadContactsFromProvider()
        } else {
            showPermissionDeniedState()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupSearch()
        setupSwipeRefresh()

        binding.btnGrantPermission.setOnClickListener {
            requestContactsPermission()
        }

        checkPermissionAndLoadContacts()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
    }

    private fun setupRecyclerView() {
        contactAdapter = ContactAdapter(
            onItemClick = { contact ->
                openContactDetails(contact)
            },
            onQuickShareClick = { contact ->
                shareContactDetails(contact)
            }
        )

        binding.rvContacts.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = contactAdapter
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterContacts(s?.toString() ?: "")
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            checkPermissionAndLoadContacts()
        }
    }

    private fun checkPermissionAndLoadContacts() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED -> {
                loadContactsFromProvider()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS) -> {
                showPermissionDeniedState()
            }
            else -> {
                requestContactsPermission()
            }
        }
    }

    private fun requestContactsPermission() {
        requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
    }

    private fun loadContactsFromProvider() {
        showLoadingState()

        lifecycleScope.launch(Dispatchers.IO) {
            val contacts = ContactRepository.fetchContacts(this@MainActivity)
            withContext(Dispatchers.Main) {
                binding.swipeRefresh.isRefreshing = false
                allContacts = contacts
                updateDashboardUI(contacts.size)

                if (contacts.isEmpty()) {
                    showEmptyContactsState()
                } else {
                    showContactsListState()
                    contactAdapter.submitFullList(contacts)
                    
                    // Filter again if search query exists
                    val currentQuery = binding.etSearch.text?.toString() ?: ""
                    if (currentQuery.isNotEmpty()) {
                        filterContacts(currentQuery)
                    }
                }
            }
        }
    }

    private fun filterContacts(query: String) {
        if (allContacts.isEmpty()) return

        contactAdapter.filter(query) { filteredCount ->
            if (filteredCount == 0 && query.isNotEmpty()) {
                binding.stateNoSearchResults.visibility = View.VISIBLE
                binding.tvNoSearchQuery.text = getString(R.string.no_search_results_desc, query)
                binding.swipeRefresh.visibility = View.GONE
            } else {
                binding.stateNoSearchResults.visibility = View.GONE
                binding.swipeRefresh.visibility = View.VISIBLE
            }
        }
    }

    private fun updateDashboardUI(count: Int) {
        binding.tvTotalContactsCount.text = resources.getQuantityString(
            R.plurals.contacts_count, count, count
        )
    }

    private fun showLoadingState() {
        binding.stateLoading.visibility = View.VISIBLE
        binding.statePermissionDenied.visibility = View.GONE
        binding.stateEmptyContacts.visibility = View.GONE
        binding.stateNoSearchResults.visibility = View.GONE
        binding.swipeRefresh.visibility = View.GONE
    }

    private fun showContactsListState() {
        binding.stateLoading.visibility = View.GONE
        binding.statePermissionDenied.visibility = View.GONE
        binding.stateEmptyContacts.visibility = View.GONE
        binding.stateNoSearchResults.visibility = View.GONE
        binding.swipeRefresh.visibility = View.VISIBLE
    }

    private fun showEmptyContactsState() {
        binding.stateLoading.visibility = View.GONE
        binding.statePermissionDenied.visibility = View.GONE
        binding.stateEmptyContacts.visibility = View.VISIBLE
        binding.stateNoSearchResults.visibility = View.GONE
        binding.swipeRefresh.visibility = View.GONE
    }

    private fun showPermissionDeniedState() {
        binding.swipeRefresh.isRefreshing = false
        binding.stateLoading.visibility = View.GONE
        binding.statePermissionDenied.visibility = View.VISIBLE
        binding.stateEmptyContacts.visibility = View.GONE
        binding.stateNoSearchResults.visibility = View.GONE
        binding.swipeRefresh.visibility = View.GONE
        binding.tvTotalContactsCount.text = getString(R.string.dashboard_subtitle, 0)
    }

    private fun openContactDetails(contact: Contact) {
        val intent = Intent(this, ContactDetailActivity::class.java).apply {
            putExtra(EXTRA_CONTACT, contact)
        }
        startActivity(intent)
    }

    private fun shareContactDetails(contact: Contact) {
        val shareText = getString(R.string.sharing_text, contact.name, contact.phoneNumber)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Contact: ${contact.name}")
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        val chooser = Intent.createChooser(shareIntent, getString(R.string.share_via))
        startActivity(chooser)
    }

    companion object {
        const val EXTRA_CONTACT = "extra_contact"
    }
}
