package com.itheamc.mapboxmapcompose.map

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.viewinterop.AndroidView
import com.itheamc.mapboxmapcompose.map.controller.MapboxMapController
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxLifecycleObserver
import com.mapbox.maps.plugin.lifecycle.lifecycle
import com.mapbox.maps.plugin.scalebar.scalebar

@Composable
fun MapboxMap(
    modifier: Modifier = Modifier,
    mapInitOptions: (context: Context) -> MapInitOptions = { MapInitOptions(context = it) },
    onMapCreated: (controller: MapboxMapController) -> Unit = {},
    lifecycleObserver: MapboxLifecycleObserver? = null,
    showScale: Boolean = false
) {

    // if the composition is composed inside a Inspect component.
    // return from here
    if (LocalInspectionMode.current) {
        return
    }

    val context = LocalContext.current
    val mapView: MapView = remember(context, mapInitOptions, onMapCreated, showScale) {
        MapView(context = context, mapInitOptions = mapInitOptions(context))
            .apply {
                scalebar.enabled = showScale
            }.also {
                onMapCreated(MapboxMapController(it))
            }
    }

    DisposableEffect(context, mapView) {
        lifecycleObserver?.let {
            mapView.lifecycle.registerLifecycleObserver(mapView, it)
        }
        onDispose {
            mapView.lifecycle.cleanup()
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier
    )
}