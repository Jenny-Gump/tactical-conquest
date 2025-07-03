package com.tacticalconquest.game.engine

import com.tacticalconquest.game.models.TerrainType

class HexMap(val width: Int, val height: Int) {
    // TODO: Copy full implementation from Claude artifacts
    
    fun getTerrain(hex: HexCoordinate): TerrainType? = null
    fun getHexOwner(hex: HexCoordinate): GameEngine.Player? = null
}
