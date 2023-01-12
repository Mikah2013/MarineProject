package com.example.marineproject

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.provider.Settings
import android.provider.Telephony
import android.telephony.SmsManager
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.marineproject.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.util.*

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {

    private lateinit var mainBinding: ActivityMainBinding
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private val locationPermissionRequestCode = 2
    private val smsPermissionRequestCode = 1
    private var isTimerRunning = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mainBinding.root)
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mainBinding.myLocationButton.setOnClickListener {

            // Check if there is an internet connection
            checkInternetConnection(this)
        }

        receiveSMS()
    }

    @SuppressLint("MissingPermission", "SetTextI18n")
    private fun getLocation() {
        if (!isTimerRunning) {
            val timer = Timer()
            timer.scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    if (checkLocationPermissions()) {
                        if (isLocationEnabled()) {
                            mFusedLocationClient.lastLocation.addOnCompleteListener() { task ->
                                val location: Location? = task.result
                                if (location != null) {
                                    // Latitude Coordinates
                                    val latitude = location.latitude
                                    // Longitude Coordinates
                                    val longitude = location.longitude
                                    // Message that includes both latitude and longitude coordinates
                                    val message = "My current location is: ($latitude, $longitude)"
                                    // Show the coordinates in a toast
                                    Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT)
                                        .show()
                                    // Send text in sms
                                    try {

                                        // on below line we are initializing sms manager.
                                        //as after android 10 the getDefault function no longer works
                                        //so we have to check that if our android version is greater
                                        //than or equal to android version 6.0 i.e SDK 23
                                        val smsManager: SmsManager
                                        if (checkSmsPermissions()) {
                                            //if SDK is greater that or equal to 23 then
                                            //this is how we will initialize the SmsManager
                                            smsManager = applicationContext.getSystemService(SmsManager::class.java)
                                            // Default number an sms to be sent to
                                            val phoneNumber = "256752784617"

                                            // on below line we are sending text message.
                                            smsManager.sendTextMessage(
                                                phoneNumber,
                                                "ME",
                                                message,
                                                null,
                                                null
                                            )

                                            // on below line we are displaying a toast message for message send,
                                            Toast.makeText(
                                                applicationContext,
                                                "Message Sent",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        } else {
                                            requestSmsPermissions()
                                        }

                                    } catch (e: Exception) {

                                        // on catch block we are displaying toast message for error.
                                        Toast.makeText(
                                            applicationContext,
                                            "Please enter all the data.." + e.message.toString(),
                                            Toast.LENGTH_LONG
                                        )
                                            .show()
                                        Log.d(TAG, "SMS ERROR" + e.message.toString())
                                    }
                                }
                            }
                        } else {
                            Toast.makeText(
                                applicationContext,
                                "Please turn on location",
                                Toast.LENGTH_LONG
                            ).show()
                            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                            startActivity(intent)
                        }
                    } else {
                        requestLocationPermissions()
                    }

                }
            }, 0, 25 * 60 * 1000)

            // Stop the timer after 5 hours
            val stopTime = System.currentTimeMillis() + 5 * 60 * 60 * 1000
            val stopTimer = Timer()
            stopTimer.schedule(object : TimerTask() {
                override fun run() {
                    timer.cancel()
                    isTimerRunning = false
                }
            }, Date(stopTime))

            isTimerRunning = true
        }

    }

    private fun requestSmsPermissions() {
        ActivityCompat.requestPermissions(this, arrayOf( Manifest.permission.RECEIVE_SMS, Manifest.permission.SEND_SMS),
            smsPermissionRequestCode)    }

    private fun checkSmsPermissions(): Boolean {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.SEND_SMS ) == PackageManager.PERMISSION_GRANTED ) {
            return true
        }
        return false

    }

    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager =  getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER)
    }

    private fun checkLocationPermissions(): Boolean {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION ) == PackageManager.PERMISSION_GRANTED ) {
            return true
        }
        return false
    }

    private fun requestLocationPermissions() {
        ActivityCompat.requestPermissions(this, arrayOf( Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
            locationPermissionRequestCode)
    }

    @SuppressLint("MissingSuperCall")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            locationPermissionRequestCode -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Location permission has been granted
                    getLocation()
                }
                return
            }
            smsPermissionRequestCode -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // SMS permission has been granted
                    receiveSMS()
                }
                return
            }
        }

    }

    private fun receiveSMS() {
        var br = object: BroadcastReceiver() {
            override fun onReceive(p0: Context?, p1: Intent?) {
                for (sms in Telephony.Sms.Intents.getMessagesFromIntent(p1)) {
                    Toast.makeText(applicationContext, sms.displayMessageBody, Toast.LENGTH_LONG).show()
                }
            }
        }
        registerReceiver(br, IntentFilter("android.provider.Telephony.SMS_RECEIVED"))

    }

    private fun checkInternetConnection(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (connectivityManager != null) {
            val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            if (capabilities != null) {
                if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    Log.i("Internet", "NetworkCapabilities.TRANSPORT_CELLULAR")
                    getLocation()
                    return true
                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    Log.i("Internet", "NetworkCapabilities.TRANSPORT_WIFI")
                    getLocation()
                    return true
                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                    Log.i("Internet", "NetworkCapabilities.TRANSPORT_ETHERNET")
                    return true
                }
            } else {
                Log.i("Internet", "No Network Connection")
                getLocation()

            }
        }
        return false
    }

}