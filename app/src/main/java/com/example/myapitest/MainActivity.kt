package com.example.myapitest

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapitest.databinding.ActivityMainBinding
import com.example.myapitest.service.RetrofitClient
import com.example.myapitest.service.safeApiCall
import com.example.myapitest.adapter.VehicleAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.myapitest.service.Result
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var vehicleAdapter: VehicleAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupView()
    }

    override fun onResume() {
        super.onResume()
        fetchItems()
    }

    private fun setupView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        vehicleAdapter = VehicleAdapter(itemClickListener = { vehicle ->
            startActivity(VehicleDetailActivity.newIntent(
                context = this@MainActivity,
                vehicleId = vehicle.id
            ))
        })
        binding.recyclerView.adapter = vehicleAdapter

        binding.addCta.setOnClickListener {
            startActivity(NewVehicleActivity.newIntent(this))
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            fetchItems()
        }

        binding.logoutCta.setOnClickListener {
            onLogout()
        }
    }

    private fun fetchItems(){
        binding.swipeRefreshLayout.isRefreshing = true
        CoroutineScope(Dispatchers.IO).launch {
            val result = safeApiCall { RetrofitClient.apiService.getVehicles() }

            withContext(Dispatchers.Main) {
                binding.swipeRefreshLayout.isRefreshing = false
                when (result) {
                    is Result.Success -> {
                        vehicleAdapter.updateVehicles(result.data)
                    }
                    is Result.Error -> {
                        Toast.makeText(this@MainActivity, R.string.unknown_error, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }


    private fun onLogout() {
        FirebaseAuth.getInstance().signOut()
        val intent = LoginActivity.newIntent(this)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    companion object {
        fun newIntent(context: Context) = Intent(context, MainActivity::class.java)
    }
}
