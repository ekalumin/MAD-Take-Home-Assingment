package com.e2145071.madtakehomeassingment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.SettingsClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    EditText txtCity;
    TextView result, sysTime;
    private final String  url = "http://api.openweathermap.org/data/2.5/weather/";
    private final String appid = "2993e8025b7403d13cf42b6a8da14d5c";
    DecimalFormat df = new DecimalFormat("#.##");

    String TAG = "location ";
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;
    private SettingsClient settingsClient;
    private LocationRequest locationRequest;
    private LocationSettingsRequest locationSettingsRequest;
    private Location location;

    String fetchedAddress = "";

    Context context;

    TextView latitude, longitude, address;
    Double d_lat, d_long;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtCity = findViewById(R.id.txtCity);
        result = findViewById(R.id.result);
        sysTime = findViewById(R.id.sysTime);

//        Date currentTime = Calendar.getInstance().getTime();
//        sysTime.setText(currentTime.toString().trim());

        Runnable runnable = new CountDownRunner();
        Thread myThread = new Thread(runnable);
        myThread.start();


        latitude = findViewById(R.id.latitude);
        longitude = findViewById(R.id.longitude);
        address = findViewById(R.id.redidence);

        context = getApplicationContext();

        checkLocationPermission();
        init();
    }

    public void doWork() {
        runOnUiThread(() -> {
            try{
                TextView txtCurrentTime= (TextView)findViewById(R.id.sysTime);
                Date currentTime = Calendar.getInstance().getTime();
                txtCurrentTime.setText(currentTime.toString());
            }catch (Exception e) {}
        });
    }

    class CountDownRunner implements Runnable{
        // @Override
        public void run() {
            while(!Thread.currentThread().isInterrupted()){
                try {
                    doWork();
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }catch(Exception e){
                }
            }
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopLocationUpdates();
    }

    public void init(){
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        settingsClient = LocationServices.getSettingsClient(this);
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                receiveLocation(locationResult);
            }
        };
        locationRequest = LocationRequest
                .create()
                .setInterval(5000)
                .setFastestInterval(500)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setMaxWaitTime(100);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(locationRequest);
        locationSettingsRequest = builder.build();
        startLocationUpdates();
    }


    private void receiveLocation(LocationResult locationResult){
        location = locationResult.getLastLocation();
        Log.d(TAG, "Latitude: " + location.getLatitude() );
        Log.d(TAG, "Longitude: " + location.getLongitude() );
        Log.d(TAG, "Altitude: " + location.getAltitude() );

        String s_lat = String.format(Locale.ROOT, "%.2f", location.getLatitude());
        String s_long = String.format(Locale.ROOT, "%.2f", location.getLongitude());

        d_lat = location.getLatitude();
        d_long = location.getLongitude();

        latitude.setText(String.valueOf(d_lat));
        longitude.setText(String.valueOf(d_long));

        try{
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> add = geocoder.getFromLocation(d_lat, d_long, 1);
            fetchedAddress = add.get(0).getAddressLine(0);
            String [] split = fetchedAddress.split(",");
            Log.d(TAG, "Fetched Address -> " + fetchedAddress );
            address.setText(String.valueOf(split[2]));
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void stopLocationUpdates(){
        fusedLocationProviderClient.removeLocationUpdates(locationCallback).addOnCompleteListener(task -> { Log.d(TAG, "STOP location update."); });
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates(){
        settingsClient.checkLocationSettings(locationSettingsRequest)
                .addOnSuccessListener(locationSettingsResponse -> {
                    Log.d(TAG, "Location settings are OK");
                    fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
                }).addOnFailureListener( e -> {
                    //int status = ((ApiException) e).getStatusCode();
                    //Log.d(TAG, "inside error -> " + status );
                    Log.d(TAG, "inside error -> " + e.getMessage() );
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                    {
                        Toast.makeText(this, "PERMISSION_GRANTED", Toast.LENGTH_SHORT).show();
                        init();
                    }
                }
                return;
        }
    }

    private void checkLocationPermission() {
        Log.d(TAG, "inside check permssion");
        if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION)){
                ActivityCompat.requestPermissions(MainActivity.this, new String[] {android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }else{
                ActivityCompat.requestPermissions(MainActivity.this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }
    }

    public void getWeatherDetails(View view) {
        String tempURL = "";
        String city = txtCity.getText().toString().trim();
        if (city.equals("")){
            result.setText("City field can not be empty.");
        }else{
            tempURL = url + "?q=" + city + "&appid=" + appid;
            StringRequest stringRequest = new StringRequest(Request.Method.POST, tempURL, response -> {
                Log.d("response", response);
                String output = "";
                try {
                    JSONObject jsonResponse = new JSONObject(response);
                    JSONArray jsonArray = jsonResponse.getJSONArray("weather");
                    JSONObject jsonObjectWeather = jsonArray.getJSONObject(0);
                    String description = jsonObjectWeather.getString("description");
                    JSONObject jsonObjectMain = jsonResponse.getJSONObject("main");
                    double temp = jsonObjectMain.getDouble("temp") - 273.15;
                    double feelsLike = jsonObjectMain.getDouble("feels_like") - 273.15;
                    float pressure = jsonObjectMain.getInt("pressure");
                    float humidity = jsonObjectMain.getInt("humidity");
                    JSONObject jsonObjectWind = jsonResponse.getJSONObject("wind");
                    String wind = jsonObjectWind.getString("speed");
                    JSONObject jsonObjectClouds = jsonResponse.getJSONObject("clouds");
                    String clouds = jsonObjectClouds.getString("all");
                    JSONObject jsonObjectSys = jsonResponse.getJSONObject("sys");
                    String countryName = jsonObjectSys.getString("country");
                    String cityName =  jsonResponse.getString("name");
                    result.setTextColor(Color.rgb(240, 243, 244 ));
                    output += " Current weather of " + cityName + " (" + countryName + ")"
                            + "\n Temp: " + df.format(temp) + " °C"
                            + "\n Feels Like: " + df.format(feelsLike) + " °C"
                            + "\n Humidity: " + humidity + "%"
                            + "\n Description: " + description
                            + "\n Wind Speed: " + wind + "m/s (meters per second)"
                            + "\n Cloudiness: " + clouds + "%"
                            + "\n Pressure: " + pressure + " hPa";
                    result.setText(output);

                }catch (JSONException e){
                    e.printStackTrace();
                }
            }, error -> {
                Toast.makeText(getApplicationContext(), error.toString().trim(), Toast.LENGTH_SHORT).show();
            });
            RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
            requestQueue.add(stringRequest);
        }
    }
}