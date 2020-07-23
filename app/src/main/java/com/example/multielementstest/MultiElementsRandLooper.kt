package com.example.multielementstest

import android.animation.TimeAnimator
import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.net.Uri
import android.util.Log
import android.view.TextureView
import kotlin.concurrent.thread


class MultiElementsRandLooper(): TimeAnimator.TimeListener, Element.OnInitialisationListener {
    final val TAG = "MultiElementsRandLooper"
    lateinit var listener: MultiElementsRandLooperInitialisedListener

    var duration: Long = 0
    var timer = TimeAnimator()
    var ticksCount = 0 // for debugging/stats, total times onTimeUpdate was called
    var goodFrames = 0 // count of frames rendered with synchronised times
    var badFrames = 0 // count of frames rendered that were erroneous (if needed?)
    var elements = mutableListOf<Element>() // our video elements
    init {
        timer.setTimeListener(this)
    }

    override fun onTimeUpdate(animation: TimeAnimator?, totalTime: Long, deltaTime: Long) {
        ticksCount++
        if (goodFrames + badFrames == 30) {
            // I want to do this on the GUI but for now just log:
            Log.d(TAG, "Rendered " + goodFrames + " good and " + badFrames + " bad, time " + totalTime)
            goodFrames = 0
            badFrames = 0
        }
        var allOutputBuffersReady = true // used to render frames only when all are available
        for (e in elements) {
            if (e.extractorAdvanced) {
                if (e.extractor.sampleFlags == -1) {
                    Log.d(TAG, "Extractor returned to start in element " + e.toString())
                    e.extractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                }

                val index = e.codec.dequeueInputBuffer(100)
                Log.d(TAG, "Got index " + index + " from codec " + e.codec.toString())
                // if a buffer index was returned (rather than a negative flag)
                if (index >= 0) {
                    // fill that buffer with data and save the size of the data
                    val size = e.extractor.readSampleData(e.codec.getInputBuffer(index)!!, 0)





                    // queue the buffer to be decoded
                    Log.d(TAG, e.toString() + "Queuing input buffer size " + size + ", time " + (e.extractor.sampleTime / 1000) + ", flags " + e.extractor.sampleFlags.toString())
                    e.codec.queueInputBuffer(index, 0, size, e.extractor.sampleTime, e.extractor.sampleFlags)
                    e.extractorAdvanced = false
                    // whoa... concise! This, I hope, advances the extractor (which I've read
                    // is a blocking operation) in another thread, then sets the flag when done
                    thread {
                        e.extractor.advance()


                        e.extractorAdvanced = true
                    }
                }

            }
            allOutputBuffersReady = fun(animationTime: Long): Boolean {
                // the element's bufferInfo field will be filled with buffer metadata by a successful call
                //
                e.updateOutputBufferIndex()
                // if the presentation time for the contents of the buffer has elapsed
                if (e.outputBufferIndex >= 0 && closerGoingDown(animationTime, e.bufferInfo.presentationTimeUs / 1000, duration)) {
                    Log.d(TAG, "outputBuffer " + e.outputBufferIndex + " good to render, time " + (e.bufferInfo.presentationTimeUs / 1000) + ", animationTime " + animationTime + ", duration " + duration)

                    Log.d(TAG, "Closer down is " + closerGoingDown(animationTime, e.bufferInfo.presentationTimeUs / 1000, duration))
                    Log.d(TAG, "Closer down other way is " + closerGoingDown(e.bufferInfo.presentationTimeUs / 1000, animationTime, duration))
                    return true
                }
                Log.d(TAG, "outputBuffer " + e.outputBufferIndex + " bad to render, time " + (e.bufferInfo.presentationTimeUs / 1000) + ", animationTime " + animationTime)

                return false
            }(totalTime % duration) && allOutputBuffersReady;



        }
        // if all elements have a dequeued buffer that should be rendered, then render them all
        if (allOutputBuffersReady) {
            Log.d(TAG, "Rendering frame")
            for (e in elements) {

                e.codec.releaseOutputBuffer(e.outputBufferIndex, true)

                e.outputBufferIndex = -1

            }
            goodFrames++;
        }

    }
    fun addElement(context: Context, uri:Uri, textureView: TextureView): Boolean {
        var newEl = Element(context, uri, textureView)
        // can't add elements after combined animation has started
        if (timer.isStarted) {
            return false
        }
        // can't add elements of different duration to already-added elements
        // to avoid having to decide which duration is used for the overall looping
        if (elements.size > 0 && elements.get(0).durationMillis != newEl.durationMillis) {
            return false
        }
        Log.d(TAG, "Setting duration to " + newEl.durationMillis)
        duration = newEl.durationMillis.toLong()
        // this containing class will listen for the initialisation of each element
        newEl.addOnInitialisationListener(this)
        elements.add(newEl)
        Log.d(TAG, "Added element, there are now " + elements.size)
        return true
    }



    fun start(){
        var startTimer = true
        for (e in elements) {
            if (!e.initialised) {
                Log.d(TAG, "Not all elements are initialised.")
                startTimer = false
            }
        }
        if (startTimer) {
            Log.d(TAG, "Timer started.")
            timer.start()

        }
    }
    fun setReadyListener(_listener: MultiElementsRandLooperInitialisedListener) {
        listener = _listener
    }
    interface MultiElementsRandLooperInitialisedListener {
        abstract fun onMultiElementsRandLooperReady(loop: MultiElementsRandLooper)
    }

    override fun onElementInitialised(e: Element) {
        Log.d(TAG, "Element " + e.toString() + " initialised!")
        var allInitialised = true
        for (element in elements) {
            if (!element.initialised) allInitialised = false
            Log.d(TAG, "Checking them all... Element " + e.toString() + " is initialised " + allInitialised)
        }
        if (allInitialised) {
            Log.d(TAG, "onElementInitialised reports that all have now been initialised!")
            if (listener != null) {
                start()
                //listener.onMultiElementsRandLooperReady(this)
            }
        }
    }
    // function to calculate whether, if setting out from a start position and targeting
    // a target position, in a circular number line of positive numbers from 0 to (max - 1) inclusive
    // so instead of counting up to max one wraps to 0 and instead of -1 to (max - 1),
    // the target is closer counting upwards than counting downwards
    fun closerGoingDown(start: Long, target: Long, max: Long): Boolean {
        if (start > target) {
            return (start - target) < (max - start) + target
        }
        else if (start == target) {
            return true // don't miss waste a tick if you're bang on!
        }
        else {
            return (start + (max - target)) < target - start
        }
    }
}