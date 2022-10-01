package com.itheamc.mapboxmapcompose

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.itheamc.mapboxmapcompose.map.CameraPosition
import com.itheamc.mapboxmapcompose.map.MapboxMap
import com.itheamc.mapboxmapcompose.map.MapboxMapController
import com.itheamc.mapboxmapcompose.ui.theme.MapboxMapComposeTheme
import com.mapbox.geojson.Point
import com.mapbox.maps.extension.style.expressions.dsl.generated.get

private const val TAG = "MainActivity"

@ExperimentalMaterial3Api
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MapboxMapComposeTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                    var mapController: MapboxMapController? by remember {
                        mutableStateOf(null)
                    }

                    Scaffold(
                        floatingActionButton = {
                            Column {
                                SmallFloatingActionButton(
                                    onClick = {
                                        mapController?.toggleSatelliteMode()
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Refresh,
                                        contentDescription = ""
                                    )
                                }
                                SmallFloatingActionButton(
                                    onClick = {
                                        mapController?.animateCameraPosition(
                                            cameraPosition = CameraPosition(
                                                center = Point.fromLngLat(82.539183, 27.811695),
                                                zoom = 15.0,
                                            )
                                        )
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Place,
                                        contentDescription = ""
                                    )
                                }
                            }
                        }
                    ) {
                        MapboxMap(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(it),
                            initialCameraPosition = CameraPosition(
                                center = Point.fromLngLat(82.539183, 27.811695),
                                zoom = 15.0,
                            ),
                            onMapCreated = { controller ->

                                mapController = controller
                                controller.addOnClickListeners(
                                    onMapClickListener = { point ->
                                        Log.d(TAG, "onMapClicked: $point")
                                    },
                                    onFeatureClickListener = { feature ->
                                        Log.d(TAG, "onFeatureClicked: $feature")
                                    }
                                )

                                controller.onStyleLoadedCallbacks.add {
                                    controller.addGeoJsonSource(
                                        sourceId = "sample_geojson",
                                        layerId = "sample_layer",
                                        circleLayer = {
                                            circleColor("blue")
                                            circleRadius(10.0)
                                            circleStrokeWidth(2.0)
                                            circleStrokeColor("#fff")
                                        },
                                        symbolLayer = {
                                            textField(get {
                                                literal("point_count_abbreviated")
                                                textColor("#fff")
                                                textSize(10.0)
                                            })
                                        }
                                    ) {
                                        url("https://d2ad6b4ur7yvpq.cloudfront.net/naturalearth-3.3.0/ne_50m_populated_places_simple.geojson")
                                        cluster(true)
                                        clusterRadius(50)
                                        clusterMaxZoom(14)
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}