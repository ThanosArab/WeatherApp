package com.smartath.weatherapiapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.LocationManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.gson.Gson
import com.smartath.weatherapiapp.databinding.ActivityMainBinding
import com.smartath.weatherapiapp.models.WeatherResponse
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private var binding: ActivityMainBinding? = null
    private var customDialog: Dialog? = null
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private lateinit var sharedPreferences: SharedPreferences

    private val locationPermission: ActivityResultLauncher<Array<String>> = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){
        permissions ->
        permissions.entries.forEach {
            val permission = it.key
            val isGranted = it.value

            if(isGranted){
                if(sharedPreferences.getString(Constants.WEATHER_RESPONSE_DATA, "").isNullOrEmpty()) {
                    requestLocation()
                }
            }
            else{
                if (permission == Manifest.permission.ACCESS_FINE_LOCATION){
                    Toast.makeText(this@MainActivity, "Permission denied!", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun locationAccess(){
        if(ActivityCompat.shouldShowRequestPermissionRationale(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION)){
            showPermissionDialog()
        }
        else{
            locationPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    private fun showPermissionDialog(){
        AlertDialog.Builder(this).setMessage("It seems that the requested permission is turned off." +
                " In order to turn it on please go to Application Settings")
            .setPositiveButton("GO TO SETTINGS") { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }.setNegativeButton("CANCEL"){dialogInterface, _ ->
                dialogInterface.dismiss()
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        setUpToolbar()

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        sharedPreferences = getSharedPreferences(Constants.PREFERENCE_NAME, Context.MODE_PRIVATE)

        setUpUI()

        if (!isLocationEnabled()){
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        }
        else{
            locationAccess()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId){
            R.id.action_refresh -> {
                requestLocation()
                true
            }
            else ->
                super.onOptionsItemSelected(item)
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestLocation(){
        val locationRequest= LocationRequest.Builder(1000).setMaxUpdates(1).build()

        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper())
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val lastKnownLocation = locationResult.lastLocation
            val latitude = lastKnownLocation!!.latitude
            val longitude = lastKnownLocation!!.longitude

            getLocationWeatherDetails(latitude, longitude)
        }
    }

    private fun getLocationWeatherDetails(latitude: Double, longitude: Double){
        if(Constants.isNetworkAvailable(this)){
            val retrofit: Retrofit = Retrofit.Builder()
                .baseUrl(Constants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val service : WeatherService = retrofit.create(WeatherService::class.java)

            val call: Call<WeatherResponse> = service.getWeather(latitude, longitude, Constants.METRIC_UNIT, Constants.APP_ID)

            showCustomDialog()

            call.enqueue(object : Callback<WeatherResponse>{
                override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
                    if (response!!.isSuccessful){
                        val weatherList: WeatherResponse? = response.body()

                        val weatherResponseJsonString = Gson().toJson(weatherList)

                        val editor = sharedPreferences.edit()
                        editor.putString(Constants.WEATHER_RESPONSE_DATA, weatherResponseJsonString)
                        editor.apply()

                        setUpUI()
                        cancelCustomDialog()
                    }
                    else{
                        when(response.code()){
                            400 -> {
                                Toast.makeText(this@MainActivity, "Error 400: Bad Connection", Toast.LENGTH_LONG).show()
                            }
                            404 -> {
                                Toast.makeText(this@MainActivity, "Error 404: Not Found", Toast.LENGTH_LONG).show()
                            }
                            else -> {
                                Toast.makeText(this@MainActivity, "Generic Error", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }

                override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                    Toast.makeText(this@MainActivity, "Error loading data", Toast.LENGTH_LONG).show()
                    cancelCustomDialog()
                }
            })
        }
        else{
            Toast.makeText(this@MainActivity, "No Internet access!", Toast.LENGTH_LONG).show()
        }
    }

    private fun setUpUI(){
        val weatherResponseJsonString = sharedPreferences.getString(Constants.WEATHER_RESPONSE_DATA, "")

        if (!weatherResponseJsonString.isNullOrEmpty()){
            val weatherList = Gson().fromJson(weatherResponseJsonString, WeatherResponse::class.java)

            for (i in weatherList.weather.indices) {
                binding?.mainTv?.text = weatherList.weather[i].main
                binding?.mainDescTv?.text = weatherList.weather[i].description

                binding?.degreeTv?.text =
                    weatherList.main.temp.toString() + getUnit(application.resources.configuration.toString())
                binding?.degreeDescTv?.text = weatherList.main.humidity.toString() + " %"

                binding?.tempMinTv?.text = weatherList.main.temp_min.toString() + " min"
                binding?.tempMaxTv?.text = weatherList.main.temp_max.toString() + " max"

                binding?.windTv?.text = weatherList.wind.speed.toString()

                binding?.locationTv?.text = weatherList.name
                binding?.locationDescTv?.text = weatherList.sys.country

                binding?.sunriseTv?.text = unixTime(weatherList.sys.sunrise)
                binding?.sunsetTv?.text = unixTime(weatherList.sys.sunset)

                binding?.timeTv?.text = unixTime(weatherList.dt)

                when (weatherList.weather[i].icon) {
                    "01d" -> binding?.mainIv?.setImageResource(R.drawable.sunny)
                    "02d" -> binding?.mainIv?.setImageResource(R.drawable.cloud)
                    "03d" -> binding?.mainIv?.setImageResource(R.drawable.cloud)
                    "04d" -> binding?.mainIv?.setImageResource(R.drawable.cloud)
                    "04n" -> binding?.mainIv?.setImageResource(R.drawable.cloud)
                    "10d" -> binding?.mainIv?.setImageResource(R.drawable.rain)
                    "11d" -> binding?.mainIv?.setImageResource(R.drawable.storm)
                    "13d" -> binding?.mainIv?.setImageResource(R.drawable.snowflake)
                    "01n" -> binding?.mainIv?.setImageResource(R.drawable.cloud)
                    "02n" -> binding?.mainIv?.setImageResource(R.drawable.cloud)
                    "03n" -> binding?.mainIv?.setImageResource(R.drawable.cloud)
                    "10n" -> binding?.mainIv?.setImageResource(R.drawable.cloud)
                    "11n" -> binding?.mainIv?.setImageResource(R.drawable.rain)
                    "13n" -> binding?.mainIv?.setImageResource(R.drawable.snowflake)
                }
            }
        }
    }

    private fun getUnit(value: String): String {
        var value = "°C"
        if ("US" == value || "LR" == value || "MM" == value) {
            value = "°F"
        }
        return value
    }

    private fun unixTime(time: Long): String {
        val date = Date(time * 1000L)
        @SuppressLint("SimpleDateFormat") val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        sdf.timeZone = TimeZone.getDefault()
        return sdf.format(date)
    }

    private fun isLocationEnabled(): Boolean{
        val locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun setUpToolbar(){
        setSupportActionBar(binding?.toolbar)

        if(supportActionBar!= null){
            supportActionBar?.title = "WeatherApp"
        }
    }

    private fun showCustomDialog(){
        customDialog = Dialog(this)
        customDialog?.setContentView(R.layout.custom_dialog_layout)
        customDialog?.setCancelable(false)
        customDialog?.show()
    }

    private fun cancelCustomDialog(){
        customDialog?.dismiss()
    }
}