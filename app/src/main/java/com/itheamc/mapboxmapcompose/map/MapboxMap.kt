package com.itheamc.mapboxmapcompose.map

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.viewinterop.AndroidView
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxLifecycleObserver
import com.mapbox.maps.plugin.animation.flyTo
import com.mapbox.maps.plugin.lifecycle.lifecycle
import com.mapbox.maps.plugin.scalebar.scalebar

@Composable
fun MapboxMap(
    modifier: Modifier = Modifier,
    initialCameraPosition: CameraPosition?,
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
    val mapView: MapView =
        remember(context, mapInitOptions, onMapCreated, showScale, initialCameraPosition) {
            MapView(
                context = context,
                mapInitOptions = mapInitOptions(context)
            )
                .apply {
                    scalebar.enabled = showScale
                }.also {
                    onMapCreated(MapboxMapController(it))
                }.also {
                    initialCameraPosition?.let { position ->
                        it.getMapboxMap().flyTo(
                            cameraOptions = CameraOptions.Builder()
                                .center(position.center)
                                .zoom(position.zoom)
                                .bearing(position.bearing)
                                .pitch(position.pitch)
                                .anchor(position.anchor)
                                .build(),
                            animationOptions = position.animationOptions
                        )
                    }
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