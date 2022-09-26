package com.itheamc.mapboxmapcompose

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.itheamc.mapboxmapcompose.map.MapboxMap
import com.itheamc.mapboxmapcompose.ui.theme.MapboxMapComposeTheme
import com.mapbox.maps.extension.style.expressions.dsl.generated.get

private const val TAG = "MainActivity"

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
                    MapboxMap(
                        modifier = Modifier.fillMaxSize(),
                        onMapCreated = { controller ->
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