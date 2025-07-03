package com.tacticalconquest.game

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.billingclient.api.*
import com.google.android.material.tabs.TabLayout
import com.tacticalconquest.game.databinding.ActivityShopBinding
import com.tacticalconquest.game.databinding.ItemShopBinding
import com.tacticalconquest.game.engine.UnitType
import com.tacticalconquest.game.managers.GameManager
import com.tacticalconquest.game.managers.PreferencesManager
import com.tacticalconquest.game.managers.SoundManager
import com.tacticalconquest.game.models.ShopItem
import com.tacticalconquest.game.models.ShopItemType
import kotlinx.coroutines.*

class ShopActivity : AppCompatActivity(), PurchasesUpdatedListener {
    
    private lateinit var binding: ActivityShopBinding
    private lateinit var soundManager: SoundManager
    private lateinit var prefsManager: PreferencesManager
    private lateinit var gameManager: GameManager
    
    private lateinit var billingClient: BillingClient
    private val skuList = mutableListOf<String>()
    private val skuDetailsMap = mutableMapOf<String, SkuDetails>()
    
    private lateinit var adapter: ShopAdapter
    private val shopItems = mutableListOf<ShopItem>()
    
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityShopBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        soundManager = SoundManager.getInstance(this)
        prefsManager = PreferencesManager.getInstance(this)
        gameManager = GameManager.getInstance(this)
        
        setupBilling()
        setupUI()
        loadShopItems()
    }
    
    private fun setupBilling() {
        billingClient = BillingClient.newBuilder(this)
            .setListener(this)
            .enablePendingPurchases()
            .build()
        
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    querySkuDetails()
                    queryPurchases()
                }
            }
            
            override fun onBillingServiceDisconnected() {
                // Попробовать переподключиться
            }
        })
    }
    
    private fun setupUI() {
        // Кнопка назад
        binding.btnBack.setOnClickListener {
            soundManager.playSound(SoundManager.SOUND_CLICK)
            onBackPressed()
        }
        
        // Обновление очков славы
        updateGloryPoints()
        
        // Настройка RecyclerView
        adapter = ShopAdapter(
            onPurchaseClick = { shopItem ->
                purchaseItem(shopItem)
            },
            onGloryPurchaseClick = { shopItem ->
                purchaseWithGlory(shopItem)
            }
        )
        binding.rvShopItems.layoutManager = LinearLayoutManager(this)
        binding.rvShopItems.adapter = adapter
        
        // Вкладки
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                soundManager.playSound(SoundManager.SOUND_CLICK)
                when (tab.position) {
                    0 -> filterItems(null) // Все
                    1 -> filterItems(ShopItemType.UNIT_SKIN) // Скины
                    2 -> filterItems(ShopItemType.PREMIUM_PACK) // Премиум
                    3 -> filterItems(ShopItemType.UI_THEME) // Темы
                }
            }
            
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }
    
    private fun loadShopItems() {
        shopItems.clear()
        
        // Премиум пакет
        shopItems.add(ShopItem(
            id = "premium_pack",
            name = "Премиум пакет",
            description = "Удвоенные очки славы + эксклюзивные скины!",
            price = "$2.99",
            type = ShopItemType.PREMIUM_PACK,
            icon = "👑"
        ))
        
        // Скины юнитов
        val skins = listOf(
            Triple("roman_skin", "Римские легионеры", "🏛️"),
            Triple("greek_skin", "Греческие гоплиты", "⚔️"),
            Triple("barbarian_skin", "Варвары", "🪓"),
            Triple("egyptian_skin", "Египтяне", "🏺"),
            Triple("persian_skin", "Персы", "🏹")
        )
        
        skins.forEach { (id, name, icon) ->
            shopItems.add(ShopItem(
                id = id,
                name = name,
                description = "Уникальный скин для пехоты",
                price = "$0.99",
                gloryPrice = 500,
                type = ShopItemType.UNIT_SKIN,
                icon = icon
            ))
        }
        
        // UI темы
        val themes = listOf(
            Triple("dark_theme", "Темная тема", "🌙"),
            Triple("gold_theme", "Золотая тема", "✨"),
            Triple("classic_theme", "Классическая тема", "📜")
        )
        
        themes.forEach { (id, name, icon) ->
            shopItems.add(ShopItem(
                id = id,
                name = name,
                description = "Измените внешний вид интерфейса",
                price = "$0.99",
                gloryPrice = 300,
                type = ShopItemType.UI_THEME,
                icon = icon
            ))
        }
        
        // Обновляем статус покупок
        updatePurchaseStatus()
        
        // Показываем все товары
        filterItems(null)
    }
    
    private fun updatePurchaseStatus() {
        val purchasedItems = prefsManager.getPurchasedItems()
        shopItems.forEach { item ->
            item.isPurchased = item.id in purchasedItems
        }
    }
    
    private fun filterItems(type: ShopItemType?) {
        val filteredItems = if (type == null) {
            shopItems
        } else {
            shopItems.filter { it.type == type }
        }
        adapter.submitList(filteredItems)
    }
    
    private fun updateGloryPoints() {
        binding.tvGloryPoints.text = getString(R.string.glory_points, prefsManager.getGloryPoints())
    }
    
    private fun purchaseItem(shopItem: ShopItem) {
        if (shopItem.isPurchased) {
            Toast.makeText(this, "Уже куплено!", Toast.LENGTH_SHORT).show()
            return
        }
        
        soundManager.playSound(SoundManager.SOUND_CLICK)
        
        // Проверяем, подключен ли billing client
        if (!billingClient.isReady) {
            Toast.makeText(this, "Магазин недоступен", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Получаем детали SKU
        val skuDetails = skuDetailsMap[shopItem.id]
        if (skuDetails != null) {
            val flowParams = BillingFlowParams.newBuilder()
                .setSkuDetails(skuDetails)
                .build()
            
            billingClient.launchBillingFlow(this, flowParams)
        } else {
            // Для тестирования без Google Play
            showTestPurchaseDialog(shopItem)
        }
    }
    
    private fun purchaseWithGlory(shopItem: ShopItem) {
        if (shopItem.isPurchased) {
            Toast.makeText(this, "Уже куплено!", Toast.LENGTH_SHORT).show()
            return
        }
        
        val gloryPrice = shopItem.gloryPrice ?: return
        
        if (prefsManager.getGloryPoints() < gloryPrice) {
            soundManager.playSound(SoundManager.SOUND_ERROR)
            Toast.makeText(this, "Недостаточно очков славы!", Toast.LENGTH_SHORT).show()
            return
        }
        
        AlertDialog.Builder(this)
            .setTitle("Покупка за очки славы")
            .setMessage("Купить ${shopItem.name} за $gloryPrice очков славы?")
            .setPositiveButton("Купить") { _, _ ->
                if (prefsManager.spendGloryPoints(gloryPrice)) {
                    completePurchase(shopItem)
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
    
    private fun showTestPurchaseDialog(shopItem: ShopItem) {
        AlertDialog.Builder(this)
            .setTitle("Тестовая покупка")
            .setMessage("Купить ${shopItem.name} за ${shopItem.price}?\n(Это тестовый режим)")
            .setPositiveButton("Купить") { _, _ ->
                completePurchase(shopItem)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
    
    private fun completePurchase(shopItem: ShopItem) {
        soundManager.playSound(SoundManager.SOUND_COIN)
        
        // Сохраняем покупку
        prefsManager.addPurchasedItem(shopItem.id)
        
        // Обрабатываем специфичные покупки
        when (shopItem.type) {
            ShopItemType.PREMIUM_PACK -> {
                // Активируем премиум функции
                prefsManager.addPurchasedItem("premium_active")
                Toast.makeText(this, "Премиум активирован! Очки славы x2", Toast.LENGTH_LONG).show()
            }
            ShopItemType.UNIT_SKIN -> {
                // Добавляем скин
                prefsManager.addPurchasedSkin(shopItem.id)
                Toast.makeText(this, "Скин добавлен! Выберите его в настройках", Toast.LENGTH_LONG).show()
            }
            ShopItemType.UI_THEME -> {
                // Применяем тему
                prefsManager.setUITheme(shopItem.id)
                Toast.makeText(this, "Тема применена!", Toast.LENGTH_SHORT).show()
                // TODO: Перезагрузить UI с новой темой
            }
            else -> {}
        }
        
        // Обновляем статус и UI
        updatePurchaseStatus()
        updateGloryPoints()
        filterItems(null)
        
        Toast.makeText(this, getString(R.string.purchase_success), Toast.LENGTH_SHORT).show()
    }
    
    private fun querySkuDetails() {
        skuList.clear()
        skuList.addAll(shopItems.map { it.id })
        
        val params = SkuDetailsParams.newBuilder()
            .setSkusList(skuList)
            .setType(BillingClient.SkuType.INAPP)
            .build()
        
        billingClient.querySkuDetailsAsync(params) { billingResult, skuDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && skuDetailsList != null) {
                for (skuDetails in skuDetailsList) {
                    skuDetailsMap[skuDetails.sku] = skuDetails
                    // Обновляем цены из Google Play
                    shopItems.find { it.id == skuDetails.sku }?.price = skuDetails.price
                }
                adapter.notifyDataSetChanged()
            }
        }
    }
    
    private fun queryPurchases() {
        billingClient.queryPurchasesAsync(BillingClient.SkuType.INAPP) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                for (purchase in purchases) {
                    handlePurchase(purchase)
                }
            }
        }
    }
    
    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Toast.makeText(this, "Покупка отменена", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, getString(R.string.purchase_failed), Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            // Подтверждаем покупку
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                
                billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        // Обрабатываем покупку
                        purchase.skus.forEach { sku ->
                            shopItems.find { it.id == sku }?.let { shopItem ->
                                completePurchase(shopItem)
                            }
                        }
                    }
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        if (::billingClient.isInitialized) {
            billingClient.endConnection()
        }
    }
    
    // Адаптер для магазина
    private class ShopAdapter(
        private val onPurchaseClick: (ShopItem) -> Unit,
        private val onGloryPurchaseClick: (ShopItem) -> Unit
    ) : RecyclerView.Adapter<ShopAdapter.ShopViewHolder>() {
        
        private var items = listOf<ShopItem>()
        
        fun submitList(newItems: List<ShopItem>) {
            items = newItems
            notifyDataSetChanged()
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShopViewHolder {
            val binding = ItemShopBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return ShopViewHolder(binding)
        }
        
        override fun onBindViewHolder(holder: ShopViewHolder, position: Int) {
            holder.bind(items[position], onPurchaseClick, onGloryPurchaseClick)
        }
        
        override fun getItemCount() = items.size
        
        class ShopViewHolder(private val binding: ItemShopBinding) : 
            RecyclerView.ViewHolder(binding.root) {
            
            fun bind(
                item: ShopItem,
                onPurchaseClick: (ShopItem) -> Unit,
                onGloryPurchaseClick: (ShopItem) -> Unit
            ) {
                binding.tvItemIcon.text = item.icon
                binding.tvItemName.text = item.name
                binding.tvItemDescription.text = item.description
                
                if (item.isPurchased) {
                    binding.btnBuy.text = binding.root.context.getString(R.string.bought)
                    binding.btnBuy.isEnabled = false
                    binding.btnBuyWithGlory.visibility = View.GONE
                } else {
                    binding.btnBuy.text = item.price
                    binding.btnBuy.isEnabled = true
                    
                    // Кнопка покупки за очки славы
                    if (item.gloryPrice != null) {
                        binding.btnBuyWithGlory.visibility = View.VISIBLE
                        binding.btnBuyWithGlory.text = "${item.gloryPrice} ⚔"
                        binding.btnBuyWithGlory.setOnClickListener {
                            onGloryPurchaseClick(item)
                        }
                    } else {
                        binding.btnBuyWithGlory.visibility = View.GONE
                    }
                }
                
                binding.btnBuy.setOnClickListener {
                    if (!item.isPurchased) {
                        onPurchaseClick(item)
                    }
                }
            }
        }
    }
}