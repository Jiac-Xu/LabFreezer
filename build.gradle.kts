plugins {
    // KSP 2.3.11 要求 AGP >= 8.10.0
    id("com.android.application") version "8.10.0" apply false
    // Kotlin 2.3.21 与 backdrop 2.0.0 / Dagger 2.60 对齐（Hilt 2.55 时代的历史卡点已解除）
    id("org.jetbrains.kotlin.android") version "2.3.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.21" apply false
    // KSP 自 2.3.0 起改用独立号段（对应 Kotlin 2.3 线）
    id("com.google.devtools.ksp") version "2.3.11" apply false
    // Hilt：2.60+ 要求 AGP 9.0，2.59 要求 AGP 9.0；2.58 支持 AGP 8.4+，且已支持 Kotlin 2.3 metadata
    id("com.google.dagger.hilt.android") version "2.58" apply false
}
