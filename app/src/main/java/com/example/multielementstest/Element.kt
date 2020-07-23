package com.example.multielementstest

import android.content.Context
import android.graphics.SurfaceTexture
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import android.view.Surface
import android.view.TextureView
import java.io.IOException

class Element(_context: Context, _uri: Uri, textureView: TextureView): TextureView.SurfaceTextureListener {
    var listeners = mutableListOf<OnInitialisationListener>()
    val TAG = "Element"
    val context = _context
    val uri = _uri
    val formatType: MediaFormat
    val durationMillis: Int
    // set true after a SurfaceTexture has been made available and used to configure the codec
    var initialised = false
    // will hold the index of a dequeued output buffer ready to be rendered, or else a negative
    // value signifying no output buffer was been made available yet: INFO_TRY_AGAIN_LATER, INFO_OUTPUT_FORMAT_CHANGED, or INFO_OUTPUT_BUFFERS_CHANGED
    var outputBufferIndex: Int = -1
    // will be set to false during asynchronous call to advance in other thread, but no need to advance now, at zero
    var extractorAdvanced = true
    var codec: MediaCodec
    // will be sent into each call to codec.dequeueOutputBuffer, to be filled therein with metadata re:
    // the buffered sample whose index the call returns
    var bufferInfo = MediaCodec.BufferInfo()
    var extractor: MediaExtractor
    init {
        Log.d(TAG, "Setting Element " + this.toString() + " to listen for TextureView " + textureView.toString() + " which is currently available? " + textureView.isAvailable)
        textureView.surfaceTextureListener = this

        extractor = MediaExtractor()

        try { // NOT SURE WHETHER TO BOTHER WITH THIS
            extractor.setDataSource(context,
                uri,
                null)

            extractor.selectTrack(0)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        formatType = extractor.getTrackFormat(0)
        if (!formatType.getString(MediaFormat.KEY_MIME).contains("video/")) {
            throw IOException("Not a video file")
        }
        val durationChecker = MediaMetadataRetriever()
        durationChecker.setDataSource(context, uri)
        durationMillis = Integer.parseInt(durationChecker.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION))
        codec = MediaCodec.createDecoderByType(formatType.getString(MediaFormat.KEY_MIME))


    }
    fun updateOutputBufferIndex() {
        if (outputBufferIndex < 0) {
            outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 100)
        }
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {
        TODO("Not yet implemented")
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {
        Log.d(TAG,"Update!!!")
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
        TODO("Not yet implemented")
    }

    override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture?, width: Int, height: Int) {
        Log.d(TAG, "SurfaceTexture now available, configuring codec " + codec.toString())
        codec.configure(formatType, Surface(surfaceTexture), null, 0)
        Log.d(TAG, "starting codec " + codec.codecInfo)
        codec.start()
        initialised = true
        notifyInitialisationListeners()
    }

    fun notifyInitialisationListeners() {
        for (listener in listeners) {
            listener.onElementInitialised(this)
        }
    }
    fun addOnInitialisationListener(listener: OnInitialisationListener) {
        listeners.add(listener)
    }
    fun removeOnInitialisationListenerListener(listener: OnInitialisationListener) {
        listeners.remove(listener)
    }
    interface OnInitialisationListener{
        abstract fun onElementInitialised(e: Element)
    }


}