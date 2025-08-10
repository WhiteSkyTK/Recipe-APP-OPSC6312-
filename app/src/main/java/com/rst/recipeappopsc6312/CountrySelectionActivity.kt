package com.rst.recipeappopsc6312

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class CountrySelectionActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var searchEditText: EditText
    private lateinit var countryAdapter: CountryAdapter
    private var countryList: List<Country> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_country_selection)

        recyclerView = findViewById(R.id.recyclerViewCountries)
        searchEditText = findViewById(R.id.editTextSearch)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Fetch the data from the API
        fetchCountries()
    }

    private fun fetchCountries() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://restcountries.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(CountryApiService::class.java)
        val call = service.getAllCountries()

        call.enqueue(object : Callback<List<Country>> {
            override fun onResponse(call: Call<List<Country>>, response: Response<List<Country>>) {
                if (response.isSuccessful) {
                    // FIXED: Assign the API response to our list
                    countryList = response.body() ?: emptyList()

                    // FIXED: Setup the adapter and search listener AFTER data is loaded
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
        countryAdapter = CountryAdapter(countryList)
        recyclerView.adapter = countryAdapter

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                countryAdapter.filter.filter(s)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }
}