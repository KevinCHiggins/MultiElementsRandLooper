package com.example.multielementstest

import android.graphics.SurfaceTexture
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.view.TextureView
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), MultiElementsRandLooper.MultiElementsRandLooperInitialisedListener {

    //
    val TAG = "MainActivity"
    lateinit var loop: MultiElementsRandLooper
    // called when loop is ready (we set MainActivity to be its listener in onCreate)
    override fun onMultiElementsRandLooperReady(loop: MultiElementsRandLooper) {
        Log.d(TAG, "Starting loop")
        loop.start()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val flameUri = Uri.parse("android.resource://" + packageName + "/" + R.raw.candle)
        val thicknessUri = Uri.parse("android.resource://" + packageName + "/" + R.raw.candle)
        val blurUri = Uri.parse("android.resource://" + packageName + "/" + R.raw.blur7)
        loop = MultiElementsRandLooper(stats)
        loop.addElement(this, flameUri, topLeftTex)
        loop.addElement(this, thicknessUri, bottomLeftTex)
        loop.addElement(this, blurUri, bottomRightTex)
        loop.setReadyListener(this)

    }




    override fun onStart() {
        super.onStart()

    }


}