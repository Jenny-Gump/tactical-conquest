package com.tacticalconquest.game.engine

/**
 * Типы юнитов в игре
 */
enum class UnitType {
    INFANTRY,   // Пехота
    ARCHERS,    // Лучники
    CAVALRY     // Кавалерия
}

/**
 * Класс, представляющий игровой юнит
 */
data class Unit(
    val id: Int,
    val type: UnitType,
    val owner: GameEngine.Player,
    var position: HexCoordinate,
    var skinId: String? = null
) {
    // Базовые характеристики в зависимости от типа
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
    
    // Текущие значения
    var currentHealth: Int = maxHealth
        private set
    
    var remainingMovement: Int = maxMovement
        private set
    
    var hasAttacked: Boolean = false
        private set
    
    /**
     * Нанести урон юниту
     */
    fun takeDamage(damage: Int) {
        currentHealth = (currentHealth - damage).coerceAtLeast(0)
    }
    
    /**
     * Исцелить юнит
     */
    fun heal(amount: Int) {
        currentHealth = (currentHealth + amount).coerceAtMost(maxHealth)
    }
    
    /**
     * Использовать очки движения
     */
    fun useMovement(cost: Int) {
        remainingMovement = (remainingMovement - cost).coerceAtLeast(0)
    }
    
    /**
     * Отметить, что юнит атаковал
     */
    fun markAsAttacked() {
        hasAttacked = true
    }
    
    /**
     * Сбросить очки движения и статус атаки (новый ход)
     */
    fun resetMovement() {
        remainingMovement = maxMovement
        hasAttacked = false
    }
    
    /**
     * Проверить, жив ли юнит
     */
    fun isAlive(): Boolean = currentHealth > 0
    
    /**
     * Проверить, может ли юнит действовать
     */
    fun canAct(): Boolean = isAlive() && (remainingMovement > 0 || !hasAttacked)
    
    /**
     * Получить процент здоровья
     */
    fun getHealthPercentage(): Float = currentHealth.toFloat() / maxHealth.toFloat()
    
    /**
     * Получить цвет для отображения на основе владельца
     */
    fun getDisplayColor(): Int {
        return when (owner) {
            GameEngine.Player.PLAYER -> android.graphics.Color.BLUE
            GameEngine.Player.AI -> android.graphics.Color.RED
            GameEngine.Player.NEUTRAL -> android.graphics.Color.GRAY
        }
    }
    
    /**
     * Получить иконку юнита
     */
    fun getIcon(): String {
        return when (type) {
            UnitType.INFANTRY -> "⚔"  // Символ меча
            UnitType.ARCHERS -> "🏹"   // Символ лука
            UnitType.CAVALRY -> "🐴"   // Символ лошади
        }
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Unit) return false
        return id == other.id
    }
    
    override fun hashCode(): Int {
        return id
    }
}