package com.itheamc.mapboxmapcompose.map

import com.mapbox.geojson.Point
import com.mapbox.maps.ScreenCoordinate
import com.mapbox.maps.plugin.animation.MapAnimationOptions

class CameraPosition(
    val center: Point,
    val zoom: Double = 13.0,
    val bearing: Double? = null,
    val pitch: Double? = null,
    val anchor: ScreenCoordinate? = null,
    val animationOptions: MapAnimationOptions? =
        MapAnimationOptions
            .mapAnimationOptions {
                startDelay(300L)
                duration(1000L)
            }
)