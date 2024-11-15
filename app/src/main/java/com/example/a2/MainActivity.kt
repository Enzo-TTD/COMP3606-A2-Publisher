package com.example.a2

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private var client: Mqtt5BlockingClient? = null
    private var isEnabled: Boolean = false
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var locationProvider: FusedLocationProviderClient
    val LOCATION_PERMISSION_REQUEST_CODE = 1
    private var idText: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets

        }

        locationProvider = LocationServices.getFusedLocationProviderClient(this)

        client = Mqtt5Client.builder()
            .identifier(UUID.randomUUID().toString())
            .serverHost("broker-816033593.sundaebytestt.com")
            .serverPort(1883)
            .build()
            .toBlocking()
    }

    private val publishLocationTask = object : Runnable {
        override fun run() {
            if (isEnabled) {
                sendLocationData(idText)
                handler.postDelayed(this, 1000) // Schedule again after 5 seconds
            }
        }
    }

    private fun sendLocationData(id: String) {

        try {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                    LOCATION_PERMISSION_REQUEST_CODE)
                return
            }


            locationProvider.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val latitude = location.latitude
                    val longitude = location.longitude

                    val currentTimeInSeconds = System.currentTimeMillis() / 1000
                    //time could have been assumed by the delay in sending data, but idk what will everyone sending interval be

                    val payload = "ID: $id, Location: $latitude, $longitude, Time(seconds): $currentTimeInSeconds "
                    client?.publishWith()
                        ?.topic("locationTopic")
                        ?.payload(payload.toByteArray())
                        ?.send()
                    Log.d("LOCATION",payload)
                    //Toast.makeText(this, "Location Sent :o", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "An error occurred while sending location data", Toast.LENGTH_SHORT).show()
        }
    }

    fun stopPublish(view: View) {
        if(!isEnabled) {
            val text = "Sensor is already disabled"
            Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
        }

        isEnabled = false
        findViewById<TextView>(R.id.textView2).text = "No data is currently being sent"

        try {
            client?.disconnect()
            handler.removeCallbacks(publishLocationTask)
        } catch (e: Exception) {
            Toast.makeText(this, "An error occurred while disconnecting from the broker", Toast.LENGTH_SHORT).show()
        }

}

    fun startPublish(view: View) {
        if(isEnabled){
            val text = "Sensor already enabled"
            Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
        }

        val editText = findViewById<EditText>(R.id.editTextNumber)
        idText = editText.text.toString()

        try {
            if(idText.isNotBlank()) {
                isEnabled = true
                val text = "data is currently being sent"
                val spannable = SpannableString(text)
                spannable.setSpan(StyleSpan(Typeface.BOLD), 0, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                val color = ContextCompat.getColor(this, com.google.android.material.R.color.abc_btn_colored_borderless_text_material)

                spannable.setSpan(ForegroundColorSpan(color), 0, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                findViewById<TextView>(R.id.textView2).text = spannable

                Log.d("SENSOR","The Sensor has been enabled")
                client?.connect()
                handler.post(publishLocationTask)
            }
        } catch (e:Exception){
            Toast.makeText(this,"An error occurred when connecting to broker", Toast.LENGTH_SHORT).show()
        }
    }
}