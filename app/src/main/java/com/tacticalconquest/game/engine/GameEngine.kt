package com.tacticalconquest.game.engine

import com.tacticalconquest.game.models.*

class GameEngine(private val level: Level, private val difficulty: Difficulty = Difficulty.MEDIUM) {
    
    enum class Player { PLAYER, AI, NEUTRAL }
    enum class GameState { PLAYER_TURN, AI_TURN, VICTORY, DEFEAT }
    enum class Difficulty { EASY, MEDIUM, HARD }
    
    interface GameListener {
        fun onGameStateChanged(state: GameState)
        fun onUnitMoved(unit: Unit, from: HexCoordinate, to: HexCoordinate)
        fun onUnitAttacked(attacker: Unit, defender: Unit, damage: Int)
        fun onTerritoryChanged(hex: HexCoordinate, newOwner: Player)
    }
    
    data class GameStateData(
        val currentTurn: Int,
        val playerUnits: List<Unit>,
        val aiUnits: List<Unit>,
        val playerResources: Resources,
        val aiResources: Resources,
        val map: HexMap
    )
    
    data class Resources(var population: Int = 5, var territories: Int = 1)
    
    // TODO: Copy full implementation from Claude artifacts
    
    fun startGame() {}
    fun getGameState(): GameStateData? = null
}
