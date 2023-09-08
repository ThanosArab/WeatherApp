# WeatherApp

WeatherApp is a Kotlin Application in which you can access live data for the weather forecast based on your current location.

Consists from:
An API integration with site https://openweathermap.org/, by using an API key and obtaining the weather data by using JSON data format and Retrofit Library for turning the API interface into an object.
All the obtained data are stored with SharedPrefereces and showed in MainActivity layout file.

![weather](https://github.com/ThanosArab/WeatherApp/assets/75016979/7f7a5e60-96aa-4dd9-9235-ee3aa74782d8)


## Dependencies
   
    implementation 'androidx.core:core-ktx:1.12.0'
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.9.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    testImplementation 'junit:junit:4.13.2'
    androidTestImplementation 'androidx.test.ext:junit:1.1.5'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'

    implementation "com.google.android.gms:play-services-location:21.0.1"
    implementation 'com.squareup.retrofit2:retrofit:2.7.2'
    implementation 'com.squareup.retrofit2:converter-gson:2.7.2'

## Launch

Download the zip file or launch the application via Github.
