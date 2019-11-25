package de.hdm.smart_penguins.utils

class DistanceEstimator {
    var a = kDefaultA
    var n = kDefaultN

    fun getDistanceInMetres(rssi: Float, txPower: Float): Float {
        return rssiToDistanceWithA(rssi, txPower)
    }

    fun rssiToDistance(rssi: Float): Float {
        return rssiToDistanceWithNAndA(rssi, n, a)
    }

    fun rssiToDistanceWithN(rssi: Float, n: Float): Float {
        return rssiToDistanceWithNAndA(rssi, n, a)
    }

    fun rssiToDistanceWithA(rssi: Float, A: Float): Float {
        return rssiToDistanceWithNAndA(rssi, n, A)
    }

    fun rssiToDistanceWithNAndA(rssi: Float, n: Float, A: Float): Float {
        return Math.pow(10.0, ((A - rssi) / (10 * n)).toDouble()).toFloat()
    }

    fun distanceToRssi(distanceInMetre: Float): Float {
        return distanceToRssiWithNAndA(distanceInMetre, n, a)
    }

    fun distanceToRssiWithN(distanceInMetre: Float, n: Float): Float {
        return distanceToRssiWithNAndA(distanceInMetre, n, a)
    }

    fun distanceToRssiWithA(distanceInMetre: Float, A: Float): Float {
        return distanceToRssiWithNAndA(distanceInMetre, n, A)
    }

    fun distanceToRssiWithNAndA(distanceInMetre: Float, n: Float, A: Float): Float {
        return (-10.0 * n.toDouble() * Math.log10(distanceInMetre.toDouble()) + A).toFloat()
    }

    fun getPropagationConstantFromRSSIAndDistance(rssi: Float, distance: Float): Float {
        return ((rssi - a) / (-10 * Math.log10(distance.toDouble()))).toFloat()
    }


    companion object {

        // A is the received signal strength in dBm at 1 metre.
        // We set A as described in:
        // http://stackoverflow.com/questions/30177965/rssi-to-distance-with-beacons
        val kDefaultA = -54f

        // n is the propagation constant or path-loss exponent
        // (Free space has n =2 for reference)
        // Typically n is in the range of [0,2].
        val kDefaultN = 2f
    }
}
