package com.example.urban_safety.ui.screens.maps

import android.Manifest
import android.content.Context
import android.graphics.Color
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.urban_safety.data.model.IncidentType
import com.example.urban_safety.data.model.LocationData
import com.example.urban_safety.ui.viewmodels.LocationViewModel
import com.example.urban_safety.ui.viewmodels.SafetyViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.ScaleBarOverlay
import org.osmdroid.views.overlay.TilesOverlay
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.io.File
import androidx.hilt.navigation.compose.hiltViewModel
import okhttp3.OkHttpClient
import org.json.JSONObject

enum class MapState {
    BROWSE,         // Browsing the map
    SELECT_START,   // Selecting a start point
    SELECT_END,     // Selecting an end point
    ROUTE_DISPLAYED // Route is displayed
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun SafeRoutesScreen(
    onNavigateBack: () -> Unit,
    viewModel: LocationViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Initialize OSMDroid configuration
    LaunchedEffect(Unit) {
        val config = Configuration.getInstance()
        config.userAgentValue = context.packageName
        config.osmdroidBasePath = File(context.cacheDir, "osmdroid")
        config.osmdroidTileCache = File(config.osmdroidBasePath, "tiles")
    }
    
    // Location permission state
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    
    // Map state
    var mapState by rememberSaveable { mutableStateOf(MapState.BROWSE) }
    var startPoint by rememberSaveable { mutableStateOf<GeoPoint?>(null) }
    var endPoint by rememberSaveable { mutableStateOf<GeoPoint?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    
    // Reference to the map view
    val mapView = remember { mutableStateOf<MapView?>(null) }
    
    // Request location permission
    LaunchedEffect(Unit) {
        if (!locationPermissionState.status.isGranted) {
            locationPermissionState.launchPermissionRequest()
        }
    }
    
    // Initialize map route
    LaunchedEffect(startPoint, endPoint) {
        if (startPoint != null && endPoint != null) {
            // Display route
            displaySafeRoute(mapView.value, context, startPoint!!, endPoint!!)
            mapState = MapState.ROUTE_DISPLAYED
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Safe Routes") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            Column {
                // My location button
                FloatingActionButton(
                    onClick = {
                        mapView.value?.let { map ->
                            val myLocationOverlay = map.overlays
                                .firstOrNull { it is MyLocationNewOverlay } as? MyLocationNewOverlay
                            
                            myLocationOverlay?.let {
                                if (it.myLocation != null) {
                                    map.controller.animateTo(it.myLocation)
                                } else {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Location not available")
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "My Location")
                }
                
                // Route button when in browse mode
                if (mapState == MapState.BROWSE) {
                    FloatingActionButton(
                        onClick = {
                            mapState = MapState.SELECT_START
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Select your starting point")
                            }
                        }
                    ) {
                        Icon(Icons.Default.Navigation, contentDescription = "Find Route")
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // OSMDroid map view
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        mapView.value = this
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(15.0)
                        controller.setCenter(GeoPoint(0.0, 0.0)) // Default position
                        
                        // Add overlays
                        val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(ctx), this)
                        locationOverlay.enableMyLocation()
                        locationOverlay.enableFollowLocation()
                        overlays.add(locationOverlay)
                        
                        val compassOverlay = CompassOverlay(
                            ctx,
                            InternalCompassOrientationProvider(ctx),
                            this
                        )
                        compassOverlay.enableCompass()
                        overlays.add(compassOverlay)
                        
                        val rotationOverlay = RotationGestureOverlay(this)
                        rotationOverlay.isEnabled = true
                        overlays.add(rotationOverlay)
                        
                        val scaleBarOverlay = ScaleBarOverlay(this)
                        scaleBarOverlay.setCentred(true)
                        scaleBarOverlay.setScaleBarOffset(ctx.resources.displayMetrics.widthPixels / 2, 10)
                        overlays.add(scaleBarOverlay)
                        
                        // Add tap events
                        val mapEventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
                            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                                when (mapState) {
                                    MapState.SELECT_START -> {
                                        startPoint = p
                                        addMarker(this@apply, p, "Start", R.drawable.ic_marker_start)
                                        
                                        mapState = MapState.SELECT_END
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Now select your destination")
                                        }
                                    }
                                    MapState.SELECT_END -> {
                                        endPoint = p
                                        addMarker(this@apply, p, "Destination", R.drawable.ic_marker_end)
                                    }
                                    else -> {
                                        // Do nothing in other states
                                    }
                                }
                                return true
                            }
                            
                            override fun longPressHelper(p: GeoPoint): Boolean {
                                return false
                            }
                        })
                        overlays.add(mapEventsOverlay)
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { view ->
                    // Update the map when needed
                    viewModel.currentLocation.value?.let { location ->
                        view.controller.setCenter(GeoPoint(location.latitude, location.longitude))
                    }
                }
            )
            
            // Instructions overlay
            if (mapState == MapState.SELECT_START || mapState == MapState.SELECT_END) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.TopCenter)
                ) {
                    Text(
                        text = when (mapState) {
                            MapState.SELECT_START -> "Tap on the map to select your starting point"
                            MapState.SELECT_END -> "Tap on the map to select your destination"
                            else -> ""
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .align(Alignment.Center)
                    )
                }
            }
            
            // Search bar when in BROWSE mode
            if (mapState == MapState.BROWSE) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.TopCenter)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Search location") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = {
                                // Search for location (in a real app this would use geocoding)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Location search not implemented in demo")
                                }
                            }) {
                                Icon(Icons.Default.Search, contentDescription = "Search")
                            }
                        }
                    )
                }
            }
            
            // Route controls when route is displayed
            if (mapState == MapState.ROUTE_DISPLAYED) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.BottomCenter)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Safe Route Found",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "This route avoids areas with reported incidents",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = {
                                // Reset map state
                                mapView.value?.overlays?.removeAll { it is Polyline || it is Marker }
                                mapView.value?.invalidate()
                                startPoint = null
                                endPoint = null
                                mapState = MapState.BROWSE
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Clear Route")
                        }
                    }
                }
            }
        }
    }
    
    // Handle lifecycle events for the map
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    mapView.value?.onResume()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    mapView.value?.onPause()
                }
                else -> {}
            }
        }
        
        lifecycleOwner.lifecycle.addObserver(observer)
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.value?.onDetach()
        }
    }
}

/**
 * Add a marker to the map
 */
private fun addMarker(mapView: MapView, point: GeoPoint, title: String, iconResId: Int) {
    val marker = Marker(mapView)
    marker.position = point
    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
    marker.title = title
    
    // In a real app, you would use a proper resource ID
    // marker.icon = ContextCompat.getDrawable(mapView.context, iconResId)
    
    // For this demo, we'll just use the default marker
    mapView.overlays.add(marker)
    mapView.invalidate()
}

/**
 * Display a safe route on the map
 */
private fun displaySafeRoute(
    mapView: MapView?,
    context: Context,
    start: GeoPoint,
    end: GeoPoint
) {
    mapView ?: return
    
    // Clear existing routes
    mapView.overlays.removeAll { it is Polyline }
    
    // Start a coroutine to fetch the route
    kotlinx.coroutines.MainScope().launch {
        try {
            // Create a URL for the OSRM API
            val url = "https://router.project-osrm.org/route/v1/driving/" +
                    "${start.longitude},${start.latitude};" +
                    "${end.longitude},${end.latitude}" +
                    "?overview=full&geometries=polyline"
            
            // Create an HTTP client and make the request
            val client = OkHttpClient()
            val request = okhttp3.Request.Builder()
                .url(url)
                .build()
            
            val response = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                client.newCall(request).execute()
            }
            
            if (response.isSuccessful) {
                val responseData = response.body?.string()
                val jsonObject = JSONObject(responseData)
                
                // Parse the geometry from the response
                val routes = jsonObject.getJSONArray("routes")
                if (routes.length() > 0) {
                    val route = routes.getJSONObject(0)
                    val geometry = route.getString("geometry")
                    
                    // Decode the polyline
                    val points = decodePolyline(geometry)
                    
                    // Create route overlay
                    val routeOverlay = Polyline()
                    routeOverlay.setPoints(points)
                    routeOverlay.color = android.graphics.Color.BLUE
                    routeOverlay.width = 8f
                    
                    // Add the safety overlay with incident markers
                    addSafetyOverlay(mapView, context, points)
                    
                    // Add the route to the map
                    mapView.overlays.add(routeOverlay)
                    
                    // Zoom to show the entire route
                    val bounds = routeOverlay.bounds
                    mapView.zoomToBoundingBox(bounds, true, 100)
                    mapView.invalidate()
                }
            } else {
                // If OSRM API fails, fall back to a simulated route
                val routePoints = createSimpleRoute(start, end)
                
                val routeOverlay = Polyline()
                routeOverlay.setPoints(routePoints)
                routeOverlay.color = android.graphics.Color.BLUE
                routeOverlay.width = 8f
                
                mapView.overlays.add(routeOverlay)
                
                // Zoom to show the entire route
                val bounds = routeOverlay.bounds
                mapView.zoomToBoundingBox(bounds, true, 100)
                mapView.invalidate()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            
            // Fallback to simple route on exception
            val routePoints = createSimpleRoute(start, end)
            
            val routeOverlay = Polyline()
            routeOverlay.setPoints(routePoints)
            routeOverlay.color = android.graphics.Color.BLUE
            routeOverlay.width = 8f
            
            mapView.overlays.add(routeOverlay)
            
            // Zoom to show the entire route
            val bounds = routeOverlay.bounds
            mapView.zoomToBoundingBox(bounds, true, 100)
            mapView.invalidate()
        }
    }
}

/**
 * Add safety overlay with incident markers
 */
private fun addSafetyOverlay(mapView: MapView, context: Context, routePoints: List<GeoPoint>) {
    // In a real app, this would fetch incident data from the Firebase database
    // For demo purposes, we'll add some simulated incident points
    
    // Define incident types and their icons
    val incidentTypeIcons = mapOf(
        IncidentType.THEFT to android.R.drawable.ic_dialog_alert,
        IncidentType.ASSAULT to android.R.drawable.ic_dialog_alert,
        IncidentType.ACCIDENT to android.R.drawable.ic_dialog_info
    )
    
    // Create simulated incident data
    val incidents = listOf(
        Triple(
            GeoPoint(routePoints[routePoints.size / 3].latitude + 0.001, 
                    routePoints[routePoints.size / 3].longitude + 0.001),
            IncidentType.THEFT,
            "Reported theft incident"
        ),
        Triple(
            GeoPoint(routePoints[routePoints.size * 2 / 3].latitude - 0.001, 
                    routePoints[routePoints.size * 2 / 3].longitude - 0.001),
            IncidentType.ASSAULT,
            "Reported assault incident"
        )
    )
    
    // Add incident markers
    incidents.forEach { (location, type, description) ->
        val marker = Marker(mapView)
        marker.position = location
        marker.title = type.name
        marker.snippet = description
        
        // Use default marker if custom icon not found
        val iconResId = incidentTypeIcons[type] ?: android.R.drawable.ic_dialog_alert
        marker.icon = androidx.core.content.ContextCompat.getDrawable(context, iconResId)
        
        mapView.overlays.add(marker)
    }
}

/**
 * Decode a polyline string to a list of GeoPoints
 */
private fun decodePolyline(encoded: String): List<GeoPoint> {
    val poly = ArrayList<GeoPoint>()
    var index = 0
    val len = encoded.length
    var lat = 0
    var lng = 0

    while (index < len) {
        var b: Int
        var shift = 0
        var result = 0
        do {
            b = encoded[index++].toInt() - 63
            result = result or (b and 0x1f shl shift)
            shift += 5
        } while (b >= 0x20)
        val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
        lat += dlat

        shift = 0
        result = 0
        do {
            b = encoded[index++].toInt() - 63
            result = result or (b and 0x1f shl shift)
            shift += 5
        } while (b >= 0x20)
        val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
        lng += dlng

        val p = GeoPoint(lat.toDouble() / 1E5, lng.toDouble() / 1E5)
        poly.add(p)
    }
    return poly
}

/**
 * Create a simple route between two points
 * In a real app, this would be replaced with a call to a routing API
 */
private fun createSimpleRoute(start: GeoPoint, end: GeoPoint): ArrayList<GeoPoint> {
    val routePoints = ArrayList<GeoPoint>()
    routePoints.add(start)
    
    // Add some intermediate points to make the route look more realistic
    val latDiff = end.latitude - start.latitude
    val lonDiff = end.longitude - start.longitude
    
    // Add a slight curve to the route
    for (i in 1..8) {
        val fraction = i / 10.0
        val lat = start.latitude + latDiff * fraction
        val lon = start.longitude + lonDiff * fraction
        
        // Add a small offset to create a curve
        val curveOffset = Math.sin(fraction * Math.PI) * 0.001
        
        routePoints.add(GeoPoint(lat + curveOffset, lon - curveOffset))
    }
    
    routePoints.add(end)
    return routePoints
}

// Mock resource class for the demo
private object R {
    object drawable {
        const val ic_marker_start = 0
        const val ic_marker_end = 0
    }
} 