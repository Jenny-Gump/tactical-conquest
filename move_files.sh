#!/bin/bash

# Simple script to ONLY move files - NO creation!

echo "📁 Creating folder structure..."
mkdir -p app/src/main/java/com/tacticalconquest/game/{engine,managers,models,ui,utils}
mkdir -p app/src/main/res/{layout,drawable,values,anim,animator}
mkdir -p app

echo "🚀 Moving files..."

# Kotlin files (.kt extension)
mv main_activity.kt app/src/main/java/com/tacticalconquest/game/MainActivity.kt 2>/dev/null
mv game_activity.kt app/src/main/java/com/tacticalconquest/game/GameActivity.kt 2>/dev/null
mv level_select_activity.kt app/src/main/java/com/tacticalconquest/game/LevelSelectActivity.kt 2>/dev/null
mv shop_activity.kt app/src/main/java/com/tacticalconquest/game/ShopActivity.kt 2>/dev/null
mv settings_activity.kt app/src/main/java/com/tacticalconquest/game/SettingsActivity.kt 2>/dev/null

# Engine
mv game_engine.kt app/src/main/java/com/tacticalconquest/game/engine/GameEngine.kt 2>/dev/null
mv unit_class.kt app/src/main/java/com/tacticalconquest/game/engine/Unit.kt 2>/dev/null
mv hex_coordinate.kt app/src/main/java/com/tacticalconquest/game/engine/HexCoordinate.kt 2>/dev/null
mv hex_map.kt app/src/main/java/com/tacticalconquest/game/engine/HexMap.kt 2>/dev/null

# Managers
mv game_manager.kt app/src/main/java/com/tacticalconquest/game/managers/GameManager.kt 2>/dev/null
mv preferences_manager.kt app/src/main/java/com/tacticalconquest/game/managers/PreferencesManager.kt 2>/dev/null
mv sound_manager.kt app/src/main/java/com/tacticalconquest/game/managers/SoundManager.kt 2>/dev/null

# UI
mv game_view.kt app/src/main/java/com/tacticalconquest/game/ui/GameView.kt 2>/dev/null

# Layouts (remove .txt extension)
mv activity_main_layout.txt app/src/main/res/layout/activity_main.xml 2>/dev/null
mv activity_game_layout.txt app/src/main/res/layout/activity_game.xml 2>/dev/null
mv activity_level_select_layout.txt app/src/main/res/layout/activity_level_select.xml 2>/dev/null
mv activity_shop_layout.txt app/src/main/res/layout/activity_shop.xml 2>/dev/null
mv activity_settings_layout.txt app/src/main/res/layout/activity_settings.xml 2>/dev/null
mv item_level_layout.txt app/src/main/res/layout/item_level.xml 2>/dev/null
mv item_shop_layout.txt app/src/main/res/layout/item_shop.xml 2>/dev/null
mv dialog_unit_info_layout.txt app/src/main/res/layout/dialog_unit_info.xml 2>/dev/null

# Values
mv strings_xml.txt app/src/main/res/values/strings.xml 2>/dev/null
mv colors_xml.txt app/src/main/res/values/colors.xml 2>/dev/null
mv themes_xml.txt app/src/main/res/values/themes.xml 2>/dev/null

# Drawables - all icons and backgrounds
mv *_drawable.txt app/src/main/res/drawable/ 2>/dev/null
# Rename them
cd app/src/main/res/drawable/
for file in *_drawable.txt; do
    [ -f "$file" ] && mv "$file" "${file%_drawable.txt}.xml"
done
cd ../../../../../

# Animations
mv *_anim.txt app/src/main/res/anim/ 2>/dev/null
# Rename them
cd app/src/main/res/anim/
for file in *_anim.txt; do
    [ -f "$file" ] && mv "$file" "${file%_anim.txt}.xml"
done
cd ../../../../

# Animator
mv button_state_animator.txt app/src/main/res/animator/button_state_animator.xml 2>/dev/null

# Root files
mv project_build_gradle.txt build.gradle 2>/dev/null
mv app_build_gradle.txt app/build.gradle 2>/dev/null
mv android_manifest.txt app/src/main/AndroidManifest.xml 2>/dev/null
mv proguard_rules.txt app/proguard-rules.pro 2>/dev/null

echo "✅ Done! Files moved to proper locations."
echo ""
echo "⚠️  Missing files:"
echo "   - Constants.kt"
echo "   - GameModels.kt"
echo "   - settings.gradle"
echo "   - gradle.properties"
echo ""
echo "You need to find and download these from Claude chat."