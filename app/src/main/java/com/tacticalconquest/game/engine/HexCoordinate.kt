package com.tacticalconquest.game.engine

import kotlin.math.abs
import kotlin.math.max

data class HexCoordinate(val col: Int, val row: Int) {
    
    fun toCubic(): CubicCoordinate {
        val x = col - (row - (row and 1)) / 2
        val z = row
        val y = -x - z
        return CubicCoordinate(x, y, z)
    }
    
    fun getNeighbors(): List<HexCoordinate> {
        val neighbors = mutableListOf<HexCoordinate>()
        val parity = row and 1
        
        val directions = if (parity == 0) {
            listOf(
                HexCoordinate(col + 1, row),
                HexCoordinate(col, row - 1),
                HexCoordinate(col - 1, row - 1),
                HexCoordinate(col - 1, row),
                HexCoordinate(col - 1, row + 1),
                HexCoordinate(col, row + 1)
            )
        } else {
            listOf(
                HexCoordinate(col + 1, row),
                HexCoordinate(col + 1, row - 1),
                HexCoordinate(col, row - 1),
                HexCoordinate(col - 1, row),
                HexCoordinate(col, row + 1),
                HexCoordinate(col + 1, row + 1)
            )
        }
        
        neighbors.addAll(directions)
        return neighbors
    }
    
    fun distanceTo(other: HexCoordinate): Int {
        val a = this.toCubic()
        val b = other.toCubic()
        return (abs(a.x - b.x) + abs(a.y - b.y) + abs(a.z - b.z)) / 2
    }
    
    fun isInBounds(width: Int, height: Int): Boolean {
        return col >= 0 && col < width && row >= 0 && row < height
    }
    
    fun getHexesInRadius(radius: Int): List<HexCoordinate> {
        val results = mutableListOf<HexCoordinate>()
        val center = this.toCubic()
        
        for (x in -radius..radius) {
            for (y in max(-radius, -x - radius)..minOf(radius, -x + radius)) {
                val z = -x - y
                val cubic = CubicCoordinate(center.x + x, center.y + y, center.z + z)
                results.add(cubic.toOffset())
            }
        }
        
        return results
    }
}

data class CubicCoordinate(val x: Int, val y: Int, val z: Int) {
    init {
        require(x + y + z == 0) { "Invalid cubic coordinates: x + y + z must equal 0" }
    }
    
    fun toOffset(): HexCoordinate {
        val col = x + (z - (z and 1)) / 2
        val row = z
        return HexCoordinate(col, row)
    }
}
