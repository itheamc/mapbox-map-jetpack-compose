package com.itheamc.mapboxmapcompose.map.controller


import android.graphics.Bitmap
import android.util.Log
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.bindgen.None
import com.mapbox.geojson.Feature
import com.mapbox.geojson.Point
import com.mapbox.maps.*
import com.mapbox.maps.extension.observable.eventdata.MapLoadingErrorEventData
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.*
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.VectorSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.generated.vectorSource
import com.mapbox.maps.plugin.delegates.listeners.OnMapLoadErrorListener
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.mapbox.maps.plugin.gestures.addOnMapLongClickListener

private const val TAG = "MapboxMapController"

class MapboxMapController(private val mapView: MapView) {
    /**
     * Init Block
     */
    init {
        loadStyleUri()
    }

    /**
     * MapBoxMap Getter
     */
    private val mapboxMap: MapboxMap
        get() = mapView.getMapboxMap()

    /**
     * Listeners
     */
    val onStyleLoadedCallbacks = mutableListOf<(style: Style) -> Unit>()

    /**
     * List of layers added by the user to the style
     */
    private val _interactiveLayers = mutableListOf<StyleObjectInfo>()
    private val interactiveLayers: List<StyleObjectInfo> = _interactiveLayers

    /**
     * List of layer sources added by the user
     * that might need interaction
     */
    private val _interactiveLayerSources = mutableListOf<StyleObjectInfo>()
    val interactiveLayerSources: List<StyleObjectInfo> = _interactiveLayerSources

    /**
     * Getter for style
     */
    private val style: Style?
        get() = mapboxMap.getStyle()

    /**
     * Getter for styleLayers
     */
    private val styleLayers: List<StyleObjectInfo>?
        get() = style?.styleLayers


    /**
     * Getter for styleSources
     */
    val styleSources: List<StyleObjectInfo>?
        get() = style?.styleSources

    /**
     * Getter for style default camera options
     */
    val styleDefaultCamera: CameraOptions?
        get() = style?.styleDefaultCamera


    /**
     * Method to load the style uri
     */
    private fun loadStyleUri() {
        Log.d(TAG, "loadStyleUri: Controller")
        mapboxMap.loadStyleUri(
            Style.LIGHT,
            styleTransitionOptions = TransitionOptions.Builder()
                .duration(0)
                .delay(0)
                .enablePlacementTransitions(false)
                .build(),
            onStyleLoaded = { sty ->
                Log.d(TAG, "onStyleLoadedListener: Loaded")
                if (onStyleLoadedCallbacks.isNotEmpty()) {
                    onStyleLoadedCallbacks.forEach {
                        it(sty)
                    }
                }
            },
            onMapLoadErrorListener = object : OnMapLoadErrorListener {
                override fun onMapLoadError(eventData: MapLoadingErrorEventData) {
                    Log.d(TAG, "onMapLoadError: ${eventData.message}")
                }
            }
        )
        Log.d(TAG, "loadStyleUri: Ended")
    }

    /**
     * Method to switch the map style
     */
    fun toggleSatelliteMode() {
        mapboxMap.getStyle()?.let {
            mapboxMap.loadStyleUri(
                if (it.styleURI == Style.SATELLITE) Style.LIGHT else Style.SATELLITE,
                onStyleLoaded = { sty ->
                    Log.d(TAG, "onStyleLoadedListener: Loaded")
                    _interactiveLayerSources.clear()
                    _interactiveLayers.clear()
                    if (onStyleLoadedCallbacks.isNotEmpty()) {
                        onStyleLoadedCallbacks.forEach {
                            it(sty)
                        }
                    }
                },
                onMapLoadErrorListener = object : OnMapLoadErrorListener {
                    override fun onMapLoadError(eventData: MapLoadingErrorEventData) {
                        Log.d(TAG, "onMapLoadError: ${eventData.message}")
                    }
                }
            )
        }
    }


    /**
     * Method to add geo json sources
     * Remember: If your want to add line layer or fill layer along with the circle layer
     * then you clustering feature will not working. Turning [cluster = true] will throw an error
     */
    fun addGeoJsonSource(
        sourceId: String,
        layerId: String,
        lineLayer: (LineLayerDsl.() -> Unit)? = null,
        circleLayer: (CircleLayerDsl.() -> Unit)? = null,
        fillLayer: (FillLayerDsl.() -> Unit)? = null,
        symbolLayer: (SymbolLayerDsl.() -> Unit)? = null,
        block: GeoJsonSource.Builder.() -> Unit = {}
    ) {
        style?.let {

            removeLayers(
                LayerType.values()
                    .map { layerType -> suffixedLayerId(layerId = layerId, layerType = layerType) }
            )
                .onValue {
                    removeStyleSourceIfAny(sourceId)
                        .onValue {
                            _interactiveLayerSources.add(StyleObjectInfo(sourceId, "GeoJsonSource"))
                            style!!.addSource(
                                geoJsonSource(sourceId, block)
                            )
                            fillLayer?.let {
                                addFillLayer(layerId, sourceId, it)
                            }
                            lineLayer?.let {
                                addLineLayer(layerId, sourceId, it)
                            }
                            circleLayer?.let {
                                addCircleLayer(layerId, sourceId, it)
                            }
                            symbolLayer?.let {
                                addSymbolLayer(layerId, sourceId, it)
                            }
                        }
                }
        }
    }

    /**
     * Method to add vector sources
     */
    fun addVectorSource(
        sourceId: String,
        layerId: String,
        lineLayer: (LineLayerDsl.() -> Unit)? = null,
        fillLayer: (FillLayerDsl.() -> Unit)? = null,
        block: VectorSource.Builder.() -> Unit
    ) {
        style?.let {

            removeLayers(
                LayerType.values()
                    .map { layerType -> suffixedLayerId(layerId = layerId, layerType = layerType) }
            )
                .onValue {
                    removeStyleSourceIfAny(sourceId = sourceId)
                        .onValue {
                            _interactiveLayerSources.add(StyleObjectInfo(sourceId, "VectorSource"))
                            style!!.addSource(
                                vectorSource(sourceId, block)
                            )

                            fillLayer?.let {
                                addFillLayer(layerId, sourceId, it)
                            }
                            lineLayer?.let {
                                addLineLayer(layerId, sourceId, it)
                            }
                        }
                }
        }
    }


    /**
     * Method to add image
     */
    fun addStyleImage(
        imageId: String,
        bitmap: Bitmap,
        sdf: Boolean = false
    ) {
        style?.let {
            removeStyleSourceIfAny(sourceId = imageId, isImageSource = true)
                .onValue {
                    _interactiveLayerSources.add(StyleObjectInfo(imageId, "ImageSource"))
                    style!!.addImage(imageId, bitmap, sdf)
                }
        }

    }

    /**
     * Method to add line layer
     */
    fun addLineLayer(layerId: String, sourceId: String, block: LineLayerDsl.() -> Unit) {
        style?.let {
            val formattedLayerId = suffixedLayerId(layerId = layerId, layerType = LayerType.LINE)

            removeLayerIfAny(formattedLayerId)
                .onValue {
                    _interactiveLayers.add(StyleObjectInfo(formattedLayerId, "LineLayer"))
                    style!!.addLayer(
                        lineLayer(formattedLayerId, sourceId, block)
                    )
                }
        }
    }

    /**
     * Method to add circle layer
     */
    fun addCircleLayer(layerId: String, sourceId: String, block: CircleLayerDsl.() -> Unit) {
        style?.let {
            val formattedLayerId = suffixedLayerId(layerId = layerId, layerType = LayerType.CIRCLE)

            removeLayerIfAny(formattedLayerId)
                .onValue {
                    _interactiveLayers.add(StyleObjectInfo(formattedLayerId, "CircleLayer"))
                    style!!.addLayer(
                        circleLayer(formattedLayerId, sourceId, block)
                    )
                }
        }
    }

    /**
     * Method to add fill layer
     */
    private fun addFillLayer(layerId: String, sourceId: String, block: FillLayerDsl.() -> Unit) {
        style?.let {
            val formattedLayerId = suffixedLayerId(layerId = layerId, layerType = LayerType.FILL)

            removeLayerIfAny(formattedLayerId)
                .onValue {
                    _interactiveLayers.add(StyleObjectInfo(formattedLayerId, "FillLayer"))
                    style!!.addLayer(
                        fillLayer(formattedLayerId, sourceId, block)
                    )
                }
        }
    }

    /**
     * Method to add symbol layer
     */
    fun addSymbolLayer(layerId: String, sourceId: String, block: SymbolLayerDsl.() -> Unit) {
        style?.let {
            val formattedLayerId = suffixedLayerId(layerId = layerId, layerType = LayerType.SYMBOL)

            removeLayerIfAny(formattedLayerId)
                .onValue {
                    _interactiveLayers.add(StyleObjectInfo(formattedLayerId, "SymbolLayer"))
                    style!!.addLayer(
                        symbolLayer(formattedLayerId, sourceId, block)
                    )
                }
        }
    }

    /**
     * Method to add on click listeners
     * --------------------------------------------------------------------------------------
     */
    fun addOnClickListeners(
        onMapClickListener: (p: Point) -> Unit = {},
        onMapLongClickListener: (p: Point) -> Unit = {},
        onFeatureClickListener: (feature: Feature) -> Unit = {},
        onFeatureLongClickListener: (feature: Feature) -> Unit = {},
    ) {
        mapboxMap.addOnMapClickListener {
            mapboxMap.queryRenderedFeatures(
                geometry = RenderedQueryGeometry(mapboxMap.pixelForCoordinate(it)),
                options = RenderedQueryOptions(interactiveLayers.map { l -> l.id }, null),
                callback = { expectedValue ->
                    expectedValue.onValue { features ->
                        if (features.isNotEmpty()) {
                            onFeatureClickListener(features[0].feature)
                        } else {
                            onMapClickListener(it)
                        }
                    }

                    expectedValue.onError {
                        Log.d(TAG, "addOnClickListener: Error")
                    }
                }
            )
            true
        }

        /**
         * On Long Click listener
         */
        mapboxMap.addOnMapLongClickListener {
            mapboxMap.queryRenderedFeatures(
                geometry = RenderedQueryGeometry(mapboxMap.pixelForCoordinate(it)),
                options = RenderedQueryOptions(interactiveLayers.map { l -> l.id }, null),
                callback = { expectedValue ->
                    expectedValue.onValue { features ->
                        if (features.isNotEmpty()) {
                            onFeatureLongClickListener(features[0].feature)
                        } else {
                            onMapLongClickListener(it)
                        }
                    }

                    expectedValue.onError {
                        Log.d(TAG, "addOnLongClickListener: Error")
                    }
                }
            )
            true
        }
    }

    /**
     * Method to remove layer if already added
     */
    private fun removeLayerIfAny(formattedLayerId: String): Expected<String, None> {
        return if (_interactiveLayers.any { soi -> soi.id == formattedLayerId }) {
            _interactiveLayers.removeIf { soi -> soi.id == formattedLayerId }
            style!!.removeStyleLayer(formattedLayerId)
        } else {
            ExpectedFactory.createValue(None.getInstance())
        }
    }

    /**
     * Method to remove style image if already added with given id
     */
    /**
     * Method to remove layer if already added
     */
    private fun removeStyleSourceIfAny(
        sourceId: String,
        isImageSource: Boolean = false
    ): Expected<String, None> {
        return if (_interactiveLayerSources.any { soi -> soi.id == sourceId }) {
            _interactiveLayerSources.removeIf { soi -> soi.id == sourceId }
            if (isImageSource) {
                style!!.removeStyleImage(sourceId)
            } else {
                style!!.removeStyleSource(sourceId)
            }
        } else {
            ExpectedFactory.createValue(None.getInstance())
        }
    }

    /**
     * Method to remove layers
     */
    private fun removeLayers(layersId: List<String>): Expected<String, None> {
        layersId.forEach { id ->
            style.let {
                val expectedResult = style!!.removeStyleLayer(id)
                expectedResult.onValue {
                    _interactiveLayers.removeIf { soi -> soi.id == id }
                }
            }
        }.also {
            return ExpectedFactory.createValue(None.getInstance())
        }
    }

    /**
     * Method to get suffix for layer id
     */
    private fun suffixedLayerId(layerId: String, layerType: LayerType = LayerType.CIRCLE): String {
        return "${layerId}_${layerType.name.lowercase()}_layer"
    }

}

private enum class LayerType {
    FILL,
    LINE,
    CIRCLE,
    SYMBOL,
    VECTOR
}