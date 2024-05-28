package com.example.leaflinkappv3

import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ExperimentalGetImage
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.leaflinkappv3.database.SensorScanDatabase
import com.example.leaflinkappv3.databinding.ActivityLocalDatabaseBinding
import com.example.leaflinkappv3.model.sensorScan
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

class LocalDatabaseActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLocalDatabaseBinding
    private lateinit var adapter: LocalDatabaseAdapter
    private lateinit var sensorScans: List<sensorScan>

    @OptIn(ExperimentalGetImage::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLocalDatabaseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val database = SensorScanDatabase.getDatabase(applicationContext)

        updateRecyclerView(database)

        lifecycleScope.launch {
            val sensorScans = withContext(Dispatchers.IO) {
                database.sensorScanDao().getAll()
            }

            binding.localDatabaseView.apply {
                layoutManager = LinearLayoutManager(this@LocalDatabaseActivity)
                adapter = LocalDatabaseAdapter(sensorScans)
            }
        }

        val popupView = layoutInflater.inflate(R.layout.menu_popup_window, null)
        val popupWindow = PopupWindow(popupView, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)

        val menuButton = findViewById<Button>(R.id.menubutton)
        menuButton.setOnClickListener {
            // Inflate the popup window layout
            val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val popupView = inflater.inflate(R.layout.menu_popup_window, null)

            // Create the PopupWindow
            val width = LinearLayout.LayoutParams.MATCH_PARENT
            val height = LinearLayout.LayoutParams.WRAP_CONTENT
            val focusable = true // lets taps outside the popup also dismiss it
            val popupWindow = PopupWindow(popupView, width, height, focusable)

            // Create the data for the RecyclerView
            val items = listOf(
                PopupMenuItem(R.drawable.scanbutton_icon, "SCAN", R.id.scanPage),
                PopupMenuItem(R.drawable.database_icon, "LOCAL DATABASE", R.id.localDatabasePage),
                PopupMenuItem(R.drawable.map_icon, "MAP", R.id.page3),
            )

            // Set up the RecyclerView
            val recyclerView = popupView.findViewById<RecyclerView>(R.id.popup_recycler_view)
            recyclerView.layoutManager = LinearLayoutManager(this)
            recyclerView.adapter = PopupMenuAdapter(items)

            val parentView = menuButton.rootView

            // Show the PopupWindow
            popupWindow.showAtLocation(parentView, Gravity.TOP, 0, 0)
        }



        val uploadButton = findViewById<Button>(R.id.UploadLocalDatabaseButton)
        uploadButton.setOnClickListener {
            lifecycleScope.launch {
                val sensorScans = withContext(Dispatchers.IO) {
                    val database = SensorScanDatabase.getDatabase(applicationContext)
                    database.sensorScanDao().getAll()
                }

                // Convert the sensorScans to JSON format
                val gson = Gson()
                val sensorScansJson = gson.toJson(sensorScans)

                // Create a logging interceptor
                val loggingInterceptor = HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }

                // Create an OkHttpClient with the logging interceptor
                val client = OkHttpClient.Builder()
                    .addInterceptor(loggingInterceptor)
                    .build()

                // Send the JSON data to server
                val retrofit = Retrofit.Builder()
                    .baseUrl("http://100.120.218.87:5432")
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build()

                val api = retrofit.create(ServerApi::class.java)
                val call = api.uploadData(sensorScansJson)

                call.enqueue(object : Callback<Void> {
                    override fun onResponse(call: Call<Void>, response: Response<Void>) {
                        if (response.isSuccessful) {
                            runOnUiThread {
                                Toast.makeText(this@LocalDatabaseActivity, "Response Successful", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            runOnUiThread {
                                Toast.makeText(this@LocalDatabaseActivity, "Else", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                    override fun onFailure(call: Call<Void>, t: Throwable) {
                        runOnUiThread {
                            Toast.makeText(this@LocalDatabaseActivity, "On Failure", Toast.LENGTH_SHORT).show()
                        }
                    }
                })
            }
        }

        val deleteButton = findViewById<Button>(R.id.deleteLocalDatabaseButton)
        deleteButton.setOnClickListener {
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    database.sensorScanDao().deleteAll()
                }
                updateRecyclerView(database)
            }
        }
    }

    private fun updateRecyclerView(database: SensorScanDatabase) {
        lifecycleScope.launch {
            sensorScans = withContext(Dispatchers.IO) {
                database.sensorScanDao().getAll()
            }

            binding.localDatabaseView.apply {
                layoutManager = LinearLayoutManager(this@LocalDatabaseActivity)
                adapter = LocalDatabaseAdapter(sensorScans).also { this@LocalDatabaseActivity.adapter = it }
            }
        }
    }

    interface ServerApi {
        @POST("api/applications")
        fun uploadData(@Body data: String): Call<Void>
    }

}