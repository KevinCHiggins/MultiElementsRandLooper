package com.example.multielementstest

import android.graphics.SurfaceTexture
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.view.TextureView
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), ExtractorSwapTest.MultiElementsRandLooperInitialisedListener, ExtractorSwapTest.SwitchListener, TextureView.SurfaceTextureListener {

    //
    val TAG = "MainActivity"
    // this is used to tot up surfaces as they report their availability to the listener
    var surfacesAvailable = 0
    val numLayers = 3; // once this many surfaces are available (twice over, as also invisible ones), we build the MultiElementsRandLooper
    lateinit var loop: ExtractorSwapTest

    // called when loop is ready (we set MainActivity to be its listener in onCreate)
    override fun onMultiElementsRandLooperReady(loop: MultiElementsRandLooper) {
        Log.d(TAG, "Starting loop")
        loop.start()
    }
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        topLeftTex.surfaceTextureListener = this
        bottomRightTex.surfaceTextureListener = this
        bottomLeftTex.surfaceTextureListener = this
        invisTex1.surfaceTextureListener = this
        invisTex2.surfaceTextureListener = this
        invisTex3.surfaceTextureListener = this




    }




    override fun onStart() {
        super.onStart()

    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {
        TODO("Not yet implemented")
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
        TODO("Not yet implemented")
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
        Log.d(TAG, "Surface available")
        surfacesAvailable++
        // count invisible surfaces too
        if (surfacesAvailable >= numLayers * 2) {
            Log.d(TAG, "Building looper")
            val visibleSurfacesAvailable = mutableListOf<Surface>(
                Surface(bottomLeftTex.surfaceTexture),
                Surface(bottomRightTex.surfaceTexture),
                Surface(topLeftTex.surfaceTexture)
            )
            val invisibleSurfacesAvailable = mutableListOf<Surface>(
                Surface(invisTex1.surfaceTexture),
                Surface(invisTex2.surfaceTexture),
                Surface(invisTex3.surfaceTexture)
            )
            loop = ExtractorSwapTest(stats, visibleSurfacesAvailable, invisibleSurfacesAvailable)
            var flameUri = Uri.parse("android.resource://" + packageName + "/" + R.raw.flame1)
            var thicknessUri = Uri.parse("android.resource://" + packageName + "/" + R.raw.flame1)
            var blurUri = Uri.parse("android.resource://" + packageName + "/" + R.raw.blur1)
            loop.addSimpleElement(this, flameUri)
            loop.addSimpleElement(this, thicknessUri)
            loop.addSimpleElement(this, blurUri)
            // these will be invisible
            flameUri = Uri.parse("android.resource://" + packageName + "/" + R.raw.flame2)
            thicknessUri = Uri.parse("android.resource://" + packageName + "/" + R.raw.flame2)
            blurUri = Uri.parse("android.resource://" + packageName + "/" + R.raw.blur2)
            loop.addSimpleElement(this, flameUri)
            loop.addSimpleElement(this, thicknessUri)
            loop.addSimpleElement(this, blurUri)
            loop.setReadyListener(this)
            loop.setSwitchListener(this)
            loop.start()

        }
    }
    /// wait this whole construct may not be necessary!!!!!!!
    override fun onSwitch(loop: MultiElementsRandLooper) {
        TODO("Not yet implemented")
    }


}