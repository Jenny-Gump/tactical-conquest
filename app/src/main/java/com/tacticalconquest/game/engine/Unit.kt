package com.tacticalconquest.game.engine

data class Unit(
    val id: Int,
    val type: UnitType,
    val owner: GameEngine.Player,
    var position: HexCoordinate,
    var skinId: String? = null
) {
    val maxHealth: Int = when (type) {
        UnitType.INFANTRY -> 100
        UnitType.ARCHERS -> 70
        UnitType.CAVALRY -> 120
    }
    
    val attack: Int = when (type) {
        UnitType.INFANTRY -> 20
        UnitType.ARCHERS -> 30
        UnitType.CAVALRY -> 35
    }
    
    val defense: Int = when (type) {
        UnitType.INFANTRY -> 15
        UnitType.ARCHERS -> 5
        UnitType.CAVALRY -> 10
    }
    
    val maxMovement: Int = when (type) {
        UnitType.INFANTRY -> 2
        UnitType.ARCHERS -> 2
        UnitType.CAVALRY -> 4
    }
    
    val attackRange: Int = when (type) {
        UnitType.INFANTRY -> 1
        UnitType.ARCHERS -> 3
        UnitType.CAVALRY -> 1
    }
    
    var currentHealth: Int = maxHealth
        private set
    
    var remainingMovement: Int = maxMovement
        private set
    
    var hasAttacked: Boolean = false
        private set
    
    fun takeDamage(damage: Int) {
        currentHealth = (currentHealth - damage).coerceAtLeast(0)
    }
    
    fun heal(amount: Int) {
        currentHealth = (currentHealth + amount).coerceAtMost(maxHealth)
    }
    
    fun useMovement(cost: Int) {
        remainingMovement = (remainingMovement - cost).coerceAtLeast(0)
    }
    
    fun markAsAttacked() {
        hasAttacked = true
    }
    
    fun resetMovement() {
        remainingMovement = maxMovement
        hasAttacked = false
    }
    
    fun isAlive(): Boolean = currentHealth > 0
    
    fun canAct(): Boolean = isAlive() && (remainingMovement > 0 || !hasAttacked)
    
    fun getHealthPercentage(): Float = currentHealth.toFloat() / maxHealth.toFloat()
    
    fun getDisplayColor(): Int {
        return when (owner) {
            GameEngine.Player.PLAYER -> android.graphics.Color.BLUE
            GameEngine.Player.AI -> android.graphics.Color.RED
            GameEngine.Player.NEUTRAL -> android.graphics.Color.GRAY
        }
    }
    
    fun getIcon(): String {
        return when (type) {
            UnitType.INFANTRY -> "⚔"
            UnitType.ARCHERS -> "🏹"
            UnitType.CAVALRY -> "🐴"
        }
    }
}
