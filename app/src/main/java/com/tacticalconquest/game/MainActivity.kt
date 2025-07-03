package com.tacticalconquest.game

import android.content.Intent
import android.media.AudioManager
import android.media.SoundPool
import android.os.Bundle
import android.os.Vibrator
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.getSystemService
import com.tacticalconquest.game.databinding.ActivityMainBinding
import com.tacticalconquest.game.managers.GameManager
import com.tacticalconquest.game.managers.PreferencesManager
import com.tacticalconquest.game.managers.SoundManager
import com.tacticalconquest.game.utils.Constants

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var gameManager: GameManager
    private lateinit var prefsManager: PreferencesManager
    private lateinit var soundManager: SoundManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Инициализация view binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Инициализация менеджеров
        gameManager = GameManager.getInstance(this)
        prefsManager = PreferencesManager.getInstance(this)
        soundManager = SoundManager.getInstance(this)
        
        // Настройка UI
        setupUI()
        
        // Анимация появления
        animateUI()
        
        // Обновление очков славы
        updateGloryPoints()
    }
    
    private fun setupUI() {
        // Кнопка "Играть"
        binding.btnPlay.setOnClickListener {
            soundManager.playSound(SoundManager.SOUND_CLICK)
            startActivity(Intent(this, LevelSelectActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
        
        // Кнопка "Магазин"
        binding.btnShop.setOnClickListener {
            soundManager.playSound(SoundManager.SOUND_CLICK)
            startActivity(Intent(this, ShopActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
        
        // Кнопка "Настройки"
        binding.btnSettings.setOnClickListener {
            soundManager.playSound(SoundManager.SOUND_CLICK)
            startActivity(Intent(this, SettingsActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
        
        // Кнопка "Достижения"
        binding.btnAchievements.setOnClickListener {
            soundManager.playSound(SoundManager.SOUND_CLICK)
            showAchievementsDialog()
        }
        
        // Кнопка "Выход"
        binding.btnExit.setOnClickListener {
            soundManager.playSound(SoundManager.SOUND_CLICK)
            showExitDialog()
        }
        
        // Версия приложения
        binding.tvVersion.text = "v${BuildConfig.VERSION_NAME}"
    }
    
    private fun animateUI() {
        // Анимация логотипа
        val logoAnim = AnimationUtils.loadAnimation(this, R.anim.fade_in_scale)
        binding.ivLogo.startAnimation(logoAnim)
        
        // Анимация кнопок с задержкой
        val buttonAnim = AnimationUtils.loadAnimation(this, R.anim.slide_in_bottom)
        binding.btnPlay.startAnimation(buttonAnim)
        
        binding.btnShop.postDelayed({
            binding.btnShop.startAnimation(buttonAnim)
        }, 100)
        
        binding.btnSettings.postDelayed({
            binding.btnSettings.startAnimation(buttonAnim)
        }, 200)
        
        binding.btnAchievements.postDelayed({
            binding.btnAchievements.startAnimation(buttonAnim)
        }, 300)
        
        binding.btnExit.postDelayed({
            binding.btnExit.startAnimation(buttonAnim)
        }, 400)
    }
    
    private fun updateGloryPoints() {
        val gloryPoints = prefsManager.getGloryPoints()
        binding.tvGloryPoints.text = getString(R.string.glory_points, gloryPoints)
        
        // Анимация при изменении
        val anim = AnimationUtils.loadAnimation(this, R.anim.pulse)
        binding.tvGloryPoints.startAnimation(anim)
    }
    
    private fun showAchievementsDialog() {
        val achievements = gameManager.getAchievements()
        val unlockedCount = achievements.count { it.isUnlocked }
        
        val message = StringBuilder()
        message.append("Разблокировано: $unlockedCount/${achievements.size}\n\n")
        
        achievements.forEach { achievement ->
            val status = if (achievement.isUnlocked) "✓" else "✗"
            message.append("$status ${achievement.name}\n")
            message.append("   ${achievement.description}\n\n")
        }
        
        AlertDialog.Builder(this, R.style.GameDialog)
            .setTitle(R.string.achievements)
            .setMessage(message.toString())
            .setPositiveButton(R.string.ok) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
    
    private fun showExitDialog() {
        AlertDialog.Builder(this, R.style.GameDialog)
            .setTitle(R.string.exit)
            .setMessage("Вы действительно хотите выйти из игры?")
            .setPositiveButton(R.string.yes) { _, _ ->
                soundManager.release()
                finish()
            }
            .setNegativeButton(R.string.no) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
    
    override fun onResume() {
        super.onResume()
        // Обновляем очки славы при возврате
        updateGloryPoints()
        
        // Воспроизводим фоновую музыку
        if (prefsManager.isMusicEnabled()) {
            soundManager.playMusic(SoundManager.MUSIC_MENU)
        }
    }
    
    override fun onPause() {
        super.onPause()
        soundManager.pauseMusic()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            soundManager.release()
        }
    }
}