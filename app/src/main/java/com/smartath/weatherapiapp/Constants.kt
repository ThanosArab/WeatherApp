package com.smartath.weatherapiapp

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

object Constants {

    const val APP_ID: String = "Current Weather Data API Key" //https://openweathermap.org/api for obtaining API Key
    const val BASE_URL: String = "https://api.openweathermap.org/data/"

    const val METRIC_UNIT: String = "metric"


    const val PREFERENCE_NAME: String = "WeatherAppPreference"
    const val WEATHER_RESPONSE_DATA: String = "weather_response_data"

    fun isNetworkAvailable(context: Context): Boolean{
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            val network = connectivityManager.activeNetwork ?: return false
            val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

            return when{
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                else -> false
            }
        }
        else{
            val networkInfo = connectivityManager.activeNetworkInfo
            return (networkInfo!=null) && networkInfo.isConnectedOrConnecting
        }
    }
}
