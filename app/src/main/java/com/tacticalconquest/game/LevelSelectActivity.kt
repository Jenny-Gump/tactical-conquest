package com.tacticalconquest.game

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.tacticalconquest.game.databinding.ActivityLevelSelectBinding
import com.tacticalconquest.game.managers.GameManager
import com.tacticalconquest.game.managers.SoundManager
import com.tacticalconquest.game.models.Level
import com.tacticalconquest.game.models.LevelProgress
import com.tacticalconquest.game.utils.Constants

class LevelSelectActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityLevelSelectBinding
    private lateinit var gameManager: GameManager
    private lateinit var soundManager: SoundManager
    private lateinit var adapter: LevelAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityLevelSelectBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        gameManager = GameManager.getInstance(this)
        soundManager = SoundManager.getInstance(this)
        
        setupUI()
        loadLevels()
    }
    
    private fun setupUI() {
        // Кнопка назад
        binding.btnBack.setOnClickListener {
            soundManager.playSound(SoundManager.SOUND_CLICK)
            onBackPressed()
        }
        
        // Настройка RecyclerView
        binding.rvLevels.layoutManager = GridLayoutManager(this, 2)
        adapter = LevelAdapter { level, progress ->
            if (progress.isUnlocked) {
                soundManager.playSound(SoundManager.SOUND_CLICK)
                startLevel(level.id)
            } else {
                soundManager.playSound(SoundManager.SOUND_ERROR)
            }
        }
        binding.rvLevels.adapter = adapter
    }
    
    private fun loadLevels() {
        val levels = gameManager.getAllLevels()
        val levelData = levels.map { level ->
            val progress = gameManager.getLevelProgress(level.id)
            LevelWithProgress(level, progress)
        }
        adapter.submitList(levelData)
    }
    
    private fun startLevel(levelId: Int) {
        val intent = Intent(this, GameActivity::class.java).apply {
            putExtra(Constants.EXTRA_LEVEL_ID, levelId)
        }
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }
    
    override fun onResume() {
        super.onResume()
        // Обновляем прогресс при возврате
        loadLevels()
    }
    
    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
    
    // Адаптер для списка уровней
    private class LevelAdapter(
        private val onLevelClick: (Level, LevelProgress) -> Unit
    ) : RecyclerView.Adapter<LevelAdapter.LevelViewHolder>() {
        
        private var levels = listOf<LevelWithProgress>()
        
        fun submitList(newLevels: List<LevelWithProgress>) {
            levels = newLevels
            notifyDataSetChanged()
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LevelViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_level, parent, false)
            return LevelViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: LevelViewHolder, position: Int) {
            holder.bind(levels[position], onLevelClick)
        }
        
        override fun getItemCount() = levels.size
        
        class LevelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val card: MaterialCardView = itemView.findViewById(R.id.cardLevel)
            private val tvLevelNumber: TextView = itemView.findViewById(R.id.tvLevelNumber)
            private val tvLevelName: TextView = itemView.findViewById(R.id.tvLevelName)
            private val ivLock: ImageView = itemView.findViewById(R.id.ivLock)
            private val llStars: View = itemView.findViewById(R.id.llStars)
            private val ivStar1: ImageView = itemView.findViewById(R.id.ivStar1)
            private val ivStar2: ImageView = itemView.findViewById(R.id.ivStar2)
            private val ivStar3: ImageView = itemView.findViewById(R.id.ivStar3)
            
            fun bind(data: LevelWithProgress, onClick: (Level, LevelProgress) -> Unit) {
                val level = data.level
                val progress = data.progress
                
                tvLevelNumber.text = level.id.toString()
                tvLevelName.text = level.name
                
                // Состояние блокировки
                if (progress.isUnlocked) {
                    card.alpha = 1.0f
                    ivLock.visibility = View.GONE
                    card.isClickable = true
                    
                    // Показываем звезды для пройденных уровней
                    if (progress.isCompleted) {
                        llStars.visibility = View.VISIBLE
                        ivStar1.setImageResource(if (progress.stars >= 1) R.drawable.ic_star_filled else R.drawable.ic_star_empty)
                        ivStar2.setImageResource(if (progress.stars >= 2) R.drawable.ic_star_filled else R.drawable.ic_star_empty)
                        ivStar3.setImageResource(if (progress.stars >= 3) R.drawable.ic_star_filled else R.drawable.ic_star_empty)
                    } else {
                        llStars.visibility = View.GONE
                    }
                } else {
                    card.alpha = 0.5f
                    ivLock.visibility = View.VISIBLE
                    llStars.visibility = View.GONE
                    card.isClickable = false
                }
                
                card.setOnClickListener {
                    onClick(level, progress)
                }
            }
        }
    }
    
    data class LevelWithProgress(
        val level: Level,
        val progress: LevelProgress
    )
}