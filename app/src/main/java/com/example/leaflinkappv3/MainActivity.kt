package com.example.leaflinkappv3

import android.Manifest
import android.animation.ObjectAnimator
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.example.leaflinkappv3.database.SensorScanDatabase
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.function.Consumer


@androidx.camera.core.ExperimentalGetImage
class MainActivity : AppCompatActivity() {

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var locationManager: LocationManager
    private val locationPermissionCode = 2
    private lateinit var viewFinder: PreviewView
    private lateinit var imageAnalysis: ImageAnalysis
    private var isImageAnalyzed = false
    private lateinit var db: SensorScanDatabase
    private var latitude: Double? = null
    private var longitude: Double? = null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        viewFinder = findViewById(R.id.viewFinder)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        db = Room.databaseBuilder(
            applicationContext,
            SensorScanDatabase::class.java, "sensor_scan_database"
        ).build()

        val scanButton = findViewById<Button>(R.id.scanbutton)
        scanButton.setOnClickListener {
            isImageAnalyzed = false
            startScanning()
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

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermission()
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()

                val camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview)

                // Attempt at setting focus to closest possible distance
                val factory = SurfaceOrientedMeteringPointFactory(
                    viewFinder.width.toFloat(), viewFinder.height.toFloat()
                )
                val centerPoint = factory.createPoint(0.1f, 0.1f)
                val action = FocusMeteringAction.Builder(centerPoint, FocusMeteringAction.FLAG_AF)
                    //.disableAutoCancel() // Prevents the focus from resetting after 3 seconds
                    .build()

                camera.cameraControl.startFocusAndMetering(action)

            } catch(exc: Exception) {
                Toast.makeText(this, "Use case binding failed", Toast.LENGTH_SHORT).show()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        requestPermissionLauncher.launch(REQUIRED_PERMISSIONS)
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions.values.all { it }) {
                startCamera()
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

    companion object {
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private fun startScanning() {
        getLocation()
        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        imageAnalysis.setAnalyzer(cameraExecutor, ImageAnalysis.Analyzer { imageProxy ->
            if (isImageAnalyzed) {
                imageProxy.close()
                return@Analyzer
            }

            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                val scanner = BarcodeScanning.getClient()

                scanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        for (barcode in barcodes) {
                            if (barcode.format == Barcode.FORMAT_QR_CODE) {
                                // Create a new sensorScan object if a QR code is detected
                                val scanEntry = com.example.leaflinkappv3.model.sensorScan(
                                    0,
                                    System.currentTimeMillis(),
                                    latitude,
                                    longitude,
                                    barcode.rawValue
                                )
                                if (longitude !=null && latitude != null){
                                    showGoodImagePopUp(R.id.scanCompleteIcon)
                                }
                                else{
                                    showBadImagePopUp(R.id.scanErrorIcon)
                                }
                                // Get an instance of sensorScanDao from SensorScanDatabase
                                val sensorScanDao = db.sensorScanDao()
                                // Use a coroutine to call the insert method
                                lifecycleScope.launch(Dispatchers.IO) {
                                    sensorScanDao.insert(scanEntry)
                                }
                                //Toast.makeText(this, "QR Code: ${barcode.rawValue}", Toast.LENGTH_LONG).show()
                            }
                        }
                        isImageAnalyzed = true
                    }
                    .addOnFailureListener {
                        // Handle any errors
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            }
        })

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(viewFinder.surfaceProvider)
                    }

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)
            } catch (exc: Exception) {
                Toast.makeText(this, "Use case binding failed", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun getLocation() {
        Log.d("MainActivity", "getLocation() called")
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                locationPermissionCode
            )
            return
        }

        val cancellationSignal = CancellationSignal()
        val executor = ContextCompat.getMainExecutor(this)
        val consumer = Consumer<Location> { location ->
            if (location != null) {
                latitude = location.latitude
                longitude = location.longitude
                Log.d("MainActivity", "Location received: $location")
                //Toast.makeText(applicationContext, coordinates, Toast.LENGTH_LONG).show()
            } else {
                latitude = 1.0000
                longitude = 1.0000
                Log.d("MainActivity", "Location received is null")
                //Toast.makeText(applicationContext, "Location not available", Toast.LENGTH_LONG).show()
            }
        }

        Log.d("MainActivity", "Requesting location")
        locationManager.getCurrentLocation(LocationManager.GPS_PROVIDER, cancellationSignal, executor, consumer)
    }

    private fun showGoodImagePopUp(imageViewId: Int) {
        val imageView = findViewById<ImageView>(imageViewId)
        imageView.visibility = View.VISIBLE

        // Create an ObjectAnimator instance for the "alpha" property
        val fadeIn = ObjectAnimator.ofFloat(imageView, "alpha", 0f, 1f)
        fadeIn.duration = 500 // duration of the animation in milliseconds

        // Create an ObjectAnimator instance for the "rotation" property
        val rotateIn = ObjectAnimator.ofFloat(imageView, "rotation", 0f, 360f)
        rotateIn.duration = 500 // duration of the animation in milliseconds

        // Start the fade-in and rotation animations
        fadeIn.start()
        rotateIn.start()

        // Use a Handler to delay the start of the fade-out animation
        Handler(Looper.getMainLooper()).postDelayed({
            // Create an ObjectAnimator instance for the "alpha" property
            val fadeOut = ObjectAnimator.ofFloat(imageView, "alpha", 1f, 0f)
            fadeOut.duration = 500 // duration of the animation in milliseconds

            // Start the fade-out and rotate out animation
            fadeOut.start()
            imageView.visibility = View.INVISIBLE
        }, 1000) // delay of 1 second
    }

    private fun showBadImagePopUp(imageViewId: Int) {
        val imageView = findViewById<ImageView>(imageViewId)
        imageView.visibility = View.VISIBLE

        // Create an ObjectAnimator instance for the "alpha" property
        val fadeIn = ObjectAnimator.ofFloat(imageView, "alpha", 0f, 1f)
        fadeIn.duration = 500 // duration of the animation in milliseconds


        val scaleUpX = ObjectAnimator.ofFloat(imageView, "scaleX", 1f, 1.5f)
        val scaleUpY = ObjectAnimator.ofFloat(imageView, "scaleY", 1f, 1.5f)
        scaleUpX.duration = 500 // duration of the animation in milliseconds
        scaleUpY.duration = 500 // duration of the animation in milliseconds

        // Start the fade-in, rotation and scale animations
        fadeIn.start()
        //rotateIn.start()
        scaleUpX.start()
        scaleUpY.start()
        // Use a Handler to delay the start of the fade-out animation
        Handler(Looper.getMainLooper()).postDelayed({
            // Create an ObjectAnimator instance for the "alpha" property
            val fadeOut = ObjectAnimator.ofFloat(imageView, "alpha", 1f, 0f)
            fadeOut.duration = 500 // duration of the animation in milliseconds

            // Start the fade-out and rotate out animation
            fadeOut.start()
            imageView.visibility = View.INVISIBLE
        }, 1000) // delay of 1 second
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == locationPermissionCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permission denied.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

}