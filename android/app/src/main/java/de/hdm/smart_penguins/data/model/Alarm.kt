package de.hdm.smart_penguins.data.model

class Alarm(
    val nearestBlackIceNode: Int,
    val nearestRescueLaneNodeId: Int,
    val nearestTrafficJamNodeId: Int,
    val currentNode: Int)