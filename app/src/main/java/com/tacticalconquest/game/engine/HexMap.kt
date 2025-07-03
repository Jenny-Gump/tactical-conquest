package com.tacticalconquest.game.engine

import com.tacticalconquest.game.models.TerrainType
import java.util.*
import kotlin.math.abs

/**
 * Класс для управления гексагональной картой
 */
class HexMap(val width: Int, val height: Int) {
    
    private data class HexTile(
        var terrain: TerrainType = TerrainType.PLAINS,
        var owner: GameEngine.Player = GameEngine.Player.NEUTRAL
    )
    
    private val tiles = Array(height) { Array(width) { HexTile() } }
    
    fun setTerrain(hex: HexCoordinate, terrain: TerrainType) {
        if (hex.isInBounds(width, height)) {
            tiles[hex.row][hex.col].terrain = terrain
        }
    }
    
    fun getTerrain(hex: HexCoordinate): TerrainType? {
        return if (hex.isInBounds(width, height)) {
            tiles[hex.row][hex.col].terrain
        } else null
    }
    
    fun setHexOwner(hex: HexCoordinate, owner: GameEngine.Player) {
        if (hex.isInBounds(width, height)) {
            tiles[hex.row][hex.col].owner = owner
        }
    }
    
    fun getHexOwner(hex: HexCoordinate): GameEngine.Player? {
        return if (hex.isInBounds(width, height)) {
            tiles[hex.row][hex.col].owner
        } else null
    }
    
    fun getNeighbors(hex: HexCoordinate): List<HexCoordinate> {
        return hex.getNeighbors().filter { it.isInBounds(width, height) }
    }
    
    fun getDistance(from: HexCoordinate, to: HexCoordinate): Int {
        return from.distanceTo(to)
    }
    
    fun getMovementCost(from: HexCoordinate, to: HexCoordinate): Int {
        val path = findPath(from, to)
        if (path.isEmpty()) return Int.MAX_VALUE
        
        var totalCost = 0
        for (i in 1 until path.size) {
            val terrain = getTerrain(path[i]) ?: continue
            totalCost += when (terrain) {
                TerrainType.PLAINS -> 1
                TerrainType.HILLS -> 2
                TerrainType.FOREST -> 2
                TerrainType.RIVER -> 3
                TerrainType.CITY -> 1
            }
        }
        
        return totalCost
    }
    
    fun getTerrainDefenseBonus(hex: HexCoordinate): Int {
        val terrain = getTerrain(hex) ?: return 0
        return when (terrain) {
            TerrainType.PLAINS -> 0
            TerrainType.HILLS -> 20
            TerrainType.FOREST -> 10
            TerrainType.RIVER -> 0
            TerrainType.CITY -> 15
        }
    }
    
    fun getCitiesOwnedBy(player: GameEngine.Player): List<HexCoordinate> {
        val cities = mutableListOf<HexCoordinate>()
        for (row in 0 until height) {
            for (col in 0 until width) {
                val hex = HexCoordinate(col, row)
                if (getTerrain(hex) == TerrainType.CITY && getHexOwner(hex) == player) {
                    cities.add(hex)
                }
            }
        }
        return cities
    }
    
    fun getTerritoriesOwnedBy(player: GameEngine.Player): List<HexCoordinate> {
        val territories = mutableListOf<HexCoordinate>()
        for (row in 0 until height) {
            for (col in 0 until width) {
                val hex = HexCoordinate(col, row)
                if (getHexOwner(hex) == player) {
                    territories.add(hex)
                }
            }
        }
        return territories
    }
    
    /**
     * Поиск пути между двумя гексами используя A*
     */
    fun findPath(start: HexCoordinate, goal: HexCoordinate): List<HexCoordinate> {
        if (!start.isInBounds(width, height) || !goal.isInBounds(width, height)) {
            return emptyList()
        }
        
        val openSet = PriorityQueue<PathNode>(compareBy { it.f })
        val closedSet = mutableSetOf<HexCoordinate>()
        val cameFrom = mutableMapOf<HexCoordinate, HexCoordinate>()
        val gScore = mutableMapOf<HexCoordinate, Int>().withDefault { Int.MAX_VALUE }
        
        gScore[start] = 0
        openSet.add(PathNode(start, 0, heuristic(start, goal)))
        
        while (openSet.isNotEmpty()) {
            val current = openSet.poll().hex
            
            if (current == goal) {
                return reconstructPath(cameFrom, current)
            }
            
            closedSet.add(current)
            
            for (neighbor in getNeighbors(current)) {
                if (neighbor in closedSet) continue
                
                val terrain = getTerrain(neighbor) ?: continue
                val movementCost = when (terrain) {
                    TerrainType.PLAINS -> 1
                    TerrainType.HILLS -> 2
                    TerrainType.FOREST -> 2
                    TerrainType.RIVER -> 3
                    TerrainType.CITY -> 1
                }
                
                val tentativeGScore = gScore.getValue(current) + movementCost
                
                if (tentativeGScore < gScore.getValue(neighbor)) {
                    cameFrom[neighbor] = current
                    gScore[neighbor] = tentativeGScore
                    val fScore = tentativeGScore + heuristic(neighbor, goal)
                    openSet.add(PathNode(neighbor, tentativeGScore, fScore))
                }
            }
        }
        
        return emptyList()
    }
    
    private fun heuristic(a: HexCoordinate, b: HexCoordinate): Int {
        return a.distanceTo(b)
    }
    
    private fun reconstructPath(
        cameFrom: Map<HexCoordinate, HexCoordinate>,
        current: HexCoordinate
    ): List<HexCoordinate> {
        val path = mutableListOf(current)
        var curr = current
        
        while (curr in cameFrom) {
            curr = cameFrom[curr]!!
            path.add(0, curr)
        }
        
        return path
    }
    
    private data class PathNode(
        val hex: HexCoordinate,
        val g: Int,
        val f: Int
    )
    
    /**
     * Получить все гексы, видимые из данной точки
     */
    fun getVisibleHexes(from: HexCoordinate, range: Int): List<HexCoordinate> {
        return from.getHexesInRadius(range).filter { hex ->
            hex.isInBounds(width, height) && hasLineOfSight(from, hex)
        }
    }
    
    /**
     * Проверка прямой видимости между гексами
     */
    private fun hasLineOfSight(from: HexCoordinate, to: HexCoordinate): Boolean {
        // Упрощенная проверка - холмы и леса блокируют видимость
        val path = getLineHexes(from, to)
        
        for (hex in path) {
            if (hex == from || hex == to) continue
            val terrain = getTerrain(hex) ?: continue
            if (terrain == TerrainType.HILLS || terrain == TerrainType.FOREST) {
                return false
            }
        }
        
        return true
    }
    
    /**
     * Получить все гексы на линии между двумя точками
     */
    private fun getLineHexes(from: HexCoordinate, to: HexCoordinate): List<HexCoordinate> {
        val results = mutableListOf<HexCoordinate>()
        val fromCubic = from.toCubic()
        val toCubic = to.toCubic()
        
        val distance = from.distanceTo(to)
        if (distance == 0) return listOf(from)
        
        for (i in 0..distance) {
            val t = i.toFloat() / distance
            val x = lerp(fromCubic.x.toFloat(), toCubic.x.toFloat(), t)
            val y = lerp(fromCubic.y.toFloat(), toCubic.y.toFloat(), t)
            val z = lerp(fromCubic.z.toFloat(), toCubic.z.toFloat(), t)
            
            val rounded = roundCubic(x, y, z)
            results.add(rounded.toOffset())
        }
        
        return results.distinct()
    }
    
    private fun lerp(a: Float, b: Float, t: Float): Float {
        return a + (b - a) * t
    }
    
    private fun roundCubic(x: Float, y: Float, z: Float): CubicCoordinate {
        var rx = kotlin.math.round(x).toInt()
        var ry = kotlin.math.round(y).toInt()
        var rz = kotlin.math.round(z).toInt()
        
        val xDiff = abs(rx - x)
        val yDiff = abs(ry - y)
        val zDiff = abs(rz - z)
        
        if (xDiff > yDiff && xDiff > zDiff) {
            rx = -ry - rz
        } else if (yDiff > zDiff) {
            ry = -rx - rz
        } else {
            rz = -rx - ry
        }
        
        return CubicCoordinate(rx, ry, rz)
    }
}