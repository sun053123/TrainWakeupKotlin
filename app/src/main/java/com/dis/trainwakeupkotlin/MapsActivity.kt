package com.dis.trainwakeupkotlin

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.os.Vibrator
import android.util.Log
import android.widget.SearchView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.activity_maps.*
import java.io.IOException
import java.util.*


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap

    lateinit var fusedLocationProviderClient: FusedLocationProviderClient
//    lateinit var locationRequest: LocationRequest

    var onSnooze:Boolean? = false //for checking not active alarm twice.
    var tarLocLat:Double? = null
    var tarLocLng:Double? = null

    var locationCallback =
            object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    super.onLocationResult(locationResult)
                    Log.d(
                        "CallLog",
                        "Latitude : " + locationResult.lastLocation.latitude + " - " + "Longtitude : " + locationResult.lastLocation.longitude
                    )
                    var lat = locationResult.lastLocation.latitude
                    var lng = locationResult.lastLocation.longitude

                    getCheckLocate(lat, lng, tarLocLat, tarLocLng)

                }
            }

    val PERMISSION_ID = 1010

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        getStart.setOnClickListener {
            Log.d("Debug1:", CheckPermission().toString())
            Log.d("Debug2:", isLocationEnabled().toString())
            RequestPermission()
            requestLocation()

        }

//        button.setOnClickListener {
//            Log.d(
//                "temp",
//                "target latitude" + tarLocLat.toString() + "target longtitude" + tarLocLng.toString()
//            )
//        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */

    override fun onMapReady(googleMap: GoogleMap) { //this is map manual callback

        mMap = googleMap
        var addressList: List<Address>? = null

        searchtarget.setOnQueryTextListener(
            object : SearchView.OnQueryTextListener {

                override fun onQueryTextSubmit(query: String?): Boolean {
                    val location: String = searchtarget.getQuery().toString()
                    mMap.clear() // clear another marker because we need only 1 marker
                    if (location != null || location != "") {
                        val geocoder = Geocoder(this@MapsActivity)
                        try {
                            addressList = geocoder.getFromLocationName(location, 1)
                        } catch (e: IOException) {

                            return false
                        }

                        val address: Address = addressList!![0]
                        val targetlatLng = LatLng(address.getLatitude(), address.getLongitude())
                        tarLocLat = address.getLatitude()
                        tarLocLng = address.getLongitude()
                        mMap.addMarker(MarkerOptions().position(targetlatLng).title(location))
                        mMap.moveCamera(CameraUpdateFactory.newLatLng(targetlatLng))
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(targetlatLng, 12f))
                    }
                    return false
                }

                override fun onQueryTextChange(p0: String?): Boolean { //needed when char is changes
                    return false
                }
            })
    }

    private fun CheckPermission():Boolean{
        //this function will return a boolean
        //true: if we have permission
        //false if not
        if(
            ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ){
            println("Access Complete")
            Log.d("Access Status:", "Access Accept")
            return true

        }
        println("Access Incomplete")
        Log.d("Access Status:", "Access Denied")
        return false

    }

    @RequiresApi(Build.VERSION_CODES.Q) //because I use Minimum sdk build @lvl26 but BackgroundLocate need api lvl29 ++
    fun RequestPermission() {
        //this function will allows us to tell the user to requesut the necessary permsiion if they are not garented
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ),
            PERMISSION_ID
        )
    }

    fun isLocationEnabled():Boolean{
        //this function will return to us the state of the location service
        //if the gps or the network provider is enabled then it will return true otherwise it will return false
        var locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == PERMISSION_ID){
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Log.d("Debug:", "You have the Permission")
            }
        }
    }

    @SuppressLint("MissingPermission")

    private fun requestLocation() {
        val locationRequest = LocationRequest()
        locationRequest.interval = 6000 //6 sec becasue
        locationRequest.priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY

        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.myLooper(),
            )
        // It's needed Permission in MainActivity
    }

     private fun snoozeAlarm() {

        val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        val pattern = longArrayOf(0, 100, 500, 1000, 500, 1000, 500, 1000, 500, 1000, 500)
        vibrator.vibrate(pattern, 4)

        val mAlertDialog = AlertDialog.Builder(this)
        mAlertDialog.setIcon(R.mipmap.ic_launcher_round) //set alertdialog icon
        mAlertDialog.setTitle("Wake up !") //set alertdialog title
        mAlertDialog.setMessage("You are close to station please wake up") //set alertdialog message
        mAlertDialog.setPositiveButton("Stop the Alarm") { dialog, id ->

            onSnooze = false
            fusedLocationProviderClient.removeLocationUpdates(locationCallback) //Kill looper
            vibrator.cancel()

            Toast.makeText(this, "YEET", Toast.LENGTH_SHORT).show()
        }
        mAlertDialog.show()
    }

    fun Double.formatDecimal(numberOfDecimals: Int = 2): String = "%.${numberOfDecimals}f".format(this)


    private fun getCheckLocate(lat: Double, long: Double, targetLat: Double?, targetLng: Double?) {
//        var geoCoder = Geocoder(this, Locale.getDefault())
//        var Adress = geoCoder.getFromLocation(lat, long, 3)

        if( lat.formatDecimal(3) == targetLat?.formatDecimal(3) && long.formatDecimal(3) == targetLng?.formatDecimal(3) && onSnooze == false ) {
            Log.d(
                "Debug:",
                "Your Locate: " + lat + " , " + long + " ; Target Locate " + targetLat + " , " + targetLng
            )
            println("Wake Upppppppppppppppppppppppppp You really need to wake up before the times are up!!!!")
            onSnooze = true
            snoozeAlarm()
        }
        else {
            Log.d(
                "Debug:",
                "Your Locate: " + lat + " , " + long + " ; Target Locate " + targetLat + " , " + targetLng
            )
            return println("not yet")
        }
    }
}
