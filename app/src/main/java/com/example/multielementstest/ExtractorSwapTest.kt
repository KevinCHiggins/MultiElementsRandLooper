package com.example.multielementstest


import android.animation.TimeAnimator
import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.net.Uri
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.widget.TextView
import kotlin.concurrent.thread


class ExtractorSwapTest(_statsView: TextView, _layers: MutableList<Surface>, _invisibleLayers: MutableList<Surface>): TimeAnimator.TimeListener, Element.OnInitialisationListener {
    final val TAG = "ExtractorSwapTest"

    //private val GL_TEXTURE_EXTERNAL_OES = 0x8D65
    private lateinit var readyListener: MultiElementsRandLooperInitialisedListener
    private lateinit var switchListener: SwitchListener
    val statsView = _statsView
    var duration: Long = 0
    var timer = TimeAnimator()
    var ticksCount = 0 // for debugging/stats, total times onTimeUpdate was called
    var totalTimeBetweenTicksThisSecond = 0 // debug stats
    var secondsCount = 0 // debug stats - this is used to update stats view once a second
    var lastTickTime: Long = 0 // debug stats
    var goodFrames = 0 // debug stats = count of frames rendered with synchronised times
    var catchingUpTicksThisSecond = 0 // debug stats - count of ticks in which the last-decoded frame is too early to present
    var elements = mutableListOf<Element>() // our video elements
    var layers = _layers // the Surfaces that will display each layer of the composite
    var invisibleLayers = _invisibleLayers
    // TEST HARD-CODING ****************
    var elementsA = mutableListOf<SimpleElement>()
    var elementsB = mutableListOf<SimpleElement>()
    var playingA: Boolean = true
    // END TEST HARD-CODING ****************
    init {
        timer.setTimeListener(this)
    }

    override fun onTimeUpdate(animation: TimeAnimator?, totalTime: Long, deltaTime: Long) {
        // as the result of the expression can't be larger than 1000 (milliseconds in a second), int is fine
        totalTimeBetweenTicksThisSecond = (totalTimeBetweenTicksThisSecond + (totalTime - lastTickTime)).toInt()
        lastTickTime = totalTime

        var allOutputBuffersReady = true // used to render frames only when all are available
        for (i in 0..(elementsA.size - 1)) {
            val e = elementsA[i]
            if (e.extractorAdvanced) {
                if (e.extractor.sampleFlags == -1) {
                    /*
                    Log.d(TAG, "Extractor returned to start in element " + e.toString())
                    e.extractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

                     */
                    if (playingA) {
                        // put it back to start before we put it away
                        e.extractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                        val temp = e.extractor
                        e.extractor = elementsB[i].extractor
                        elementsA[i].extractor = temp
                        val temp2 = e.durationMillis
                        e.durationMillis = elementsB[i].durationMillis
                        elementsA[i].durationMillis = temp2
                        playingA = false
                        Log.d(TAG, "Switching to B's extractor and duration")
                    }
                    else {
                        e.extractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                        val temp = e.extractor
                        e.extractor = elementsA[i].extractor
                        elementsB[i].extractor = temp
                        val temp2 = e.durationMillis
                        e.durationMillis = elementsA[i].durationMillis
                        elementsB[i].durationMillis = temp2
                        playingA = true
                        Log.d(TAG, "Switching to A's extractor and duration")

                    }

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
                catchingUpTicksThisSecond++
                Log.d(TAG, "outputBuffer " + e.outputBufferIndex + " bad to render, time " + (e.bufferInfo.presentationTimeUs / 1000) + ", animationTime " + animationTime)

                return false
            }(totalTime % duration) && allOutputBuffersReady;



        }
        // if all elements have a dequeued buffer that should be rendered, then render them all
        if (allOutputBuffersReady) {
            Log.d(TAG, "Rendering frame")
            for (e in elementsA) {

                e.codec.releaseOutputBuffer(e.outputBufferIndex, true)

                e.outputBufferIndex = -1

            }
            goodFrames++;
        }
        // stats
        ticksCount++
        // if another second has rolled over...
        if (secondsCount < totalTime / 1000) { // totalTime is in ms
            // update counter
            secondsCount++

            statsView.text = "Seconds elapsed: " + secondsCount +
                    "\nFrames per second: " + goodFrames +
                    "\nTicks waiting for time to catch up: " + catchingUpTicksThisSecond +
                    "\nTicks missed: " + (60 - ticksCount) +
                    "\nAverage time between ticks: " + totalTimeBetweenTicksThisSecond / ticksCount +
                    "\nPlaying A: " + playingA

            catchingUpTicksThisSecond = 0
            totalTimeBetweenTicksThisSecond = 0
            goodFrames = 0
            ticksCount = 0
        }

    }
    // okay, this does not present a nice interface at all, but I'm gonna
    // take in the details of SimpleElements one by one and fill up elementsA
    // with active, ready-to-play SimpleElements, and elementsB with
    // dummy SimpleElements whose Surface is an invisible GL Texture
    fun addSimpleElement(context: Context, uri: Uri) {
        if (elementsA.size < 3) {
            var newEl = SimpleElement(context, uri, layers[elementsA.size])
            duration = newEl.durationMillis.toLong()
            //. leaving out the safety checks of duration being equal and stuff
            elementsA.add(newEl)
        }
        else {
            var newEl = SimpleElement(context, uri, invisibleLayers[elementsB.size])
            duration = newEl.durationMillis.toLong()
            //. leaving out the safety checks of duration being equal and stuff
            elementsB.add(newEl)
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
        readyListener = _listener
    }
    interface MultiElementsRandLooperInitialisedListener {
        abstract fun onMultiElementsRandLooperReady(loop: MultiElementsRandLooper)
    }
    fun setSwitchListener(_listener: SwitchListener) {
        switchListener = _listener
    }
    interface SwitchListener {
        abstract fun onSwitch(loop: MultiElementsRandLooper)
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
            if (readyListener != null) {
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