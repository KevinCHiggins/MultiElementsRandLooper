package com.example.multielementstest

import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class CloserGoingDownUnitTest {

    @Test
    fun hundredCloserNineHundred() {
        assertTrue(closerGoingDown(100, 900, 1000))
    }
    @Test
    fun eightHundredNotCloserNineHundred() {
        assertFalse(closerGoingDown(800, 900, 1000))
    }
    @Test
    fun fourHundredNotCloserFiveHundred() {
        assertFalse(closerGoingDown(400, 500, 1000))
    }
    @Test
    fun fiveHundredCloserFourHundred() {
        assertTrue(closerGoingDown(500, 400, 1000))
    }
    @Test
    fun actualCase() {
        assertFalse(closerGoingDown(1516, 1566, 14533))
    }
    @Test
    fun otherCase() {
        assert(closerGoingDown(2583, 3200, 14533) == true)

    }
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