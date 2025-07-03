package com.tacticalconquest.game.engine

import kotlin.math.abs
import kotlin.math.max

/**
 * Координаты гексагональной клетки в системе offset coordinates (odd-r)
 */
data class HexCoordinate(val col: Int, val row: Int) {
    
    /**
     * Преобразование в кубические координаты для упрощения расчетов
     */
    fun toCubic(): CubicCoordinate {
        val x = col - (row - (row and 1)) / 2
        val z = row
        val y = -x - z
        return CubicCoordinate(x, y, z)
    }
    
    /**
     * Получение соседних гексов
     */
    fun getNeighbors(): List<HexCoordinate> {
        val neighbors = mutableListOf<HexCoordinate>()
        val parity = row and 1
        
        val directions = if (parity == 0) {
            // Четная строка
            listOf(
                HexCoordinate(col + 1, row),     // Восток
                HexCoordinate(col, row - 1),      // Северо-восток
                HexCoordinate(col - 1, row - 1),  // Северо-запад
                HexCoordinate(col - 1, row),      // Запад
                HexCoordinate(col - 1, row + 1),  // Юго-запад
                HexCoordinate(col, row + 1)       // Юго-восток
            )
        } else {
            // Нечетная строка
            listOf(
                HexCoordinate(col + 1, row),      // Восток
                HexCoordinate(col + 1, row - 1),  // Северо-восток
                HexCoordinate(col, row - 1),       // Северо-запад
                HexCoordinate(col - 1, row),       // Запад
                HexCoordinate(col, row + 1),       // Юго-запад
                HexCoordinate(col + 1, row + 1)   // Юго-восток
            )
        }
        
        neighbors.addAll(directions)
        return neighbors
    }
    
    /**
     * Расстояние до другого гекса
     */
    fun distanceTo(other: HexCoordinate): Int {
        val a = this.toCubic()
        val b = other.toCubic()
        return (abs(a.x - b.x) + abs(a.y - b.y) + abs(a.z - b.z)) / 2
    }
    
    /**
     * Проверка, находится ли координата в пределах карты
     */
    fun isInBounds(width: Int, height: Int): Boolean {
        return col >= 0 && col < width && row >= 0 && row < height
    }
    
    /**
     * Получение всех гексов в радиусе
     */
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

/**
 * Кубические координаты для упрощения математических операций с гексами
 */
data class CubicCoordinate(val x: Int, val y: Int, val z: Int) {
    init {
        require(x + y + z == 0) { "Invalid cubic coordinates: x + y + z must equal 0" }
    }
    
    /**
     * Преобразование обратно в offset координаты
     */
    fun toOffset(): HexCoordinate {
        val col = x + (z - (z and 1)) / 2
        val row = z
        return HexCoordinate(col, row)
    }
}