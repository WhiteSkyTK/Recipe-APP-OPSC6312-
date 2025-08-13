package com.rst.recipeappopsc6312

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class CountrySelectionActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var searchEditText: EditText
    private lateinit var continueButton: Button
    private lateinit var backButton: ImageView
    private lateinit var countryAdapter: CountryAdapter
    private var countryList: List<Country> = emptyList()
    private var selectedCountry: Country? = null

    // This will hold the registration data passed between screens
    private lateinit var registrationData: RegistrationData

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_country_selection)

        // Initialize registrationData, creating a new object if one isn't passed
        registrationData = intent.getParcelableExtra("REGISTRATION_DATA") ?: RegistrationData()

        recyclerView = findViewById(R.id.recyclerViewCountries)
        searchEditText = findViewById(R.id.editTextSearch)
        continueButton = findViewById(R.id.buttonContinue)
        backButton = findViewById(R.id.imageViewBack)
        recyclerView.layoutManager = LinearLayoutManager(this)

        setupContinueButton()
        loadCountries()

        backButton.setOnClickListener {
            finish() // This will close the current activity and go back to WelcomeActivity
        }
    }

    private fun loadCountries() {
        val cachedCountries = getCountriesFromCache()
        if (cachedCountries.isNotEmpty()) {
            // If we have cached data, use it immediately
            countryList = sortCountries(cachedCountries)
            setupRecyclerViewAndSearch()
        } else {
            // Otherwise, fetch from the network
            fetchCountriesFromApi()
        }
    }

    private fun sortCountries(countries: List<Country>): List<Country> {
        val mutableCountries = countries.toMutableList()
        // Find South Africa in the list
        val southAfrica = mutableCountries.find { it.nameInfo.common == "South Africa" }

        // If found, remove it to be added back later
        southAfrica?.let {
            mutableCountries.remove(it)
        }

        // Sort the rest of the list alphabetically
        mutableCountries.sortBy { it.nameInfo.common }

        // Add South Africa back to the very beginning of the list
        southAfrica?.let {
            mutableCountries.add(0, it)
        }

        return mutableCountries
    }

    private fun fetchCountriesFromApi() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://restcountries.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val service = retrofit.create(CountryApiService::class.java)
        val call = service.getAllCountries()

        call.enqueue(object : Callback<List<Country>> {
            override fun onResponse(call: Call<List<Country>>, response: Response<List<Country>>) {
                if (response.isSuccessful) {
                    countryList = response.body() ?: emptyList()
                    saveCountriesToCache(countryList) // Save the new data
                    setupRecyclerViewAndSearch()
                } else {
                    Toast.makeText(this@CountrySelectionActivity, "Failed to load countries", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<Country>>, t: Throwable) {
                Toast.makeText(this@CountrySelectionActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupRecyclerViewAndSearch() {
        countryAdapter = CountryAdapter(countryList) { country ->
            // This lambda is called when a country is clicked in the adapter
            selectedCountry = country
            continueButton.isEnabled = true // Enable the continue button
        }
        recyclerView.adapter = countryAdapter

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                countryAdapter.filter.filter(s)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupContinueButton() {
        continueButton.isEnabled = false // Disabled by default
        continueButton.setOnClickListener {
            if (selectedCountry != null) {
                // Update the registration data object
                registrationData.country = selectedCountry!!.nameInfo.common

                // Navigate to the next screen
                val intent = Intent(this, CuisineSelectionActivity::class.java)
                intent.putExtra("REGISTRATION_DATA", registrationData)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Please select a country", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // --- Caching Logic ---
    private fun saveCountriesToCache(countries: List<Country>) {
        val sharedPreferences = getSharedPreferences("AppCache", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val gson = Gson()
        val json = gson.toJson(countries)
        editor.putString("country_list", json)
        editor.apply()
    }

    private fun getCountriesFromCache(): List<Country> {
        val sharedPreferences = getSharedPreferences("AppCache", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPreferences.getString("country_list", null)
        return if (json != null) {
            val type = object : TypeToken<List<Country>>() {}.type
            gson.fromJson(json, type)
        } else {
            emptyList()
        }
    }
}