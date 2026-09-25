package com.example.data.filters

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorMatrix

enum class FilterCategory(val label: String) {
    ALL("All"),
    BASIC("Basic"),
    BEAUTY("Beauty"),
    FUN("Fun"),
    CUTE("Cute"),
    CINEMATIC("Cinematic")
}

enum class FacePropType {
    NONE,
    CYBER_SHADES,
    RETRO_SUNGLASSES,
    PARTY_CROWN,
    CAT_EARS,
    GENTLEMAN_MOUSTACHE,
    FLOATING_HEARTS,
    SPARKLE_AURA,
    FLORAL_CROWN,
    ANGEL_HALO,
    STAR_BLUSH
}

data class SnaporaFilter(
    val id: String,
    val name: String,
    val category: FilterCategory,
    val iconEmoji: String,
    val propType: FacePropType = FacePropType.NONE,
    val colorOverlay: Color = Color.Transparent,
    val vignetteColor: Color? = null,
    val brightnessBoost: Float = 0f,
    val contrastBoost: Float = 1f,
    val saturationBoost: Float = 1f,
    val colorMatrix: ColorMatrix? = null,
    val beautyGlow: Boolean = false,
    val description: String = ""
)

object FilterRegistry {

    val allFilters: List<SnaporaFilter> = listOf(
        // Basic Category
        SnaporaFilter(
            id = "original",
            name = "Original",
            category = FilterCategory.BASIC,
            iconEmoji = "✨",
            description = "Natural camera view"
        ),
        SnaporaFilter(
            id = "bright",
            name = "Bright",
            category = FilterCategory.BASIC,
            iconEmoji = "☀️",
            brightnessBoost = 0.15f,
            contrastBoost = 1.08f,
            saturationBoost = 1.1f,
            description = "Luminous daylight enhancement"
        ),
        SnaporaFilter(
            id = "warm_sunset",
            name = "Warm Sunset",
            category = FilterCategory.BASIC,
            iconEmoji = "🌅",
            colorOverlay = Color(0xFFFF9436).copy(alpha = 0.18f),
            saturationBoost = 1.15f,
            contrastBoost = 1.05f,
            description = "Golden hour warmth and rich tones"
        ),
        SnaporaFilter(
            id = "cool_cyan",
            name = "Cool Cyan",
            category = FilterCategory.BASIC,
            iconEmoji = "❄️",
            colorOverlay = Color(0xFF00E5FF).copy(alpha = 0.14f),
            contrastBoost = 1.1f,
            description = "Crisp arctic teal tone"
        ),
        SnaporaFilter(
            id = "vintage_90s",
            name = "Vintage 90s",
            category = FilterCategory.BASIC,
            iconEmoji = "📼",
            colorOverlay = Color(0xFFD4A373).copy(alpha = 0.22f),
            contrastBoost = 0.95f,
            saturationBoost = 0.85f,
            vignetteColor = Color.Black.copy(alpha = 0.35f),
            description = "Retro faded film with subtle vignette"
        ),
        SnaporaFilter(
            id = "noir_bw",
            name = "Noir B&W",
            category = FilterCategory.BASIC,
            iconEmoji = "🎬",
            contrastBoost = 1.35f,
            saturationBoost = 0f,
            colorMatrix = ColorMatrix().apply { setToSaturation(0f) },
            description = "Dramatic monochrome cinema look"
        ),

        // Beauty Category
        SnaporaFilter(
            id = "natural_glow",
            name = "Natural Glow",
            category = FilterCategory.BEAUTY,
            iconEmoji = "🌸",
            beautyGlow = true,
            brightnessBoost = 0.10f,
            contrastBoost = 1.04f,
            saturationBoost = 1.08f,
            colorOverlay = Color(0xFFFFE0E6).copy(alpha = 0.10f),
            description = "Soft skin radiance with gentle highlight glow"
        ),
        SnaporaFilter(
            id = "soft_portrait",
            name = "Soft Portrait",
            category = FilterCategory.BEAUTY,
            iconEmoji = "🪄",
            beautyGlow = true,
            brightnessBoost = 0.08f,
            colorOverlay = Color(0xFFFFF0F5).copy(alpha = 0.12f),
            vignetteColor = Color(0xFF3B1E28).copy(alpha = 0.25f),
            description = "Diffused studio portrait lighting"
        ),
        SnaporaFilter(
            id = "golden_radiance",
            name = "Golden Radiance",
            category = FilterCategory.BEAUTY,
            iconEmoji = "👑",
            beautyGlow = true,
            colorOverlay = Color(0xFFFFD700).copy(alpha = 0.15f),
            contrastBoost = 1.08f,
            saturationBoost = 1.15f,
            description = "Sun-kissed bronze aesthetic"
        ),

        // Fun Category (Face Tracking Props)
        SnaporaFilter(
            id = "cyber_shades",
            name = "Cyber Shades",
            category = FilterCategory.FUN,
            iconEmoji = "🕶️",
            propType = FacePropType.CYBER_SHADES,
            colorOverlay = Color(0xFF8B5CF6).copy(alpha = 0.12f),
            contrastBoost = 1.15f,
            description = "Futuristic neon visor locked to your eyes"
        ),
        SnaporaFilter(
            id = "retro_sunglasses",
            name = "Retro Shades",
            category = FilterCategory.FUN,
            iconEmoji = "😎",
            propType = FacePropType.RETRO_SUNGLASSES,
            colorOverlay = Color(0xFFFFB703).copy(alpha = 0.10f),
            description = "Classic dark sunglasses tracking your face"
        ),
        SnaporaFilter(
            id = "cat_ears",
            name = "Cat Ears",
            category = FilterCategory.FUN,
            iconEmoji = "🐱",
            propType = FacePropType.CAT_EARS,
            colorOverlay = Color(0xFFFFB6C1).copy(alpha = 0.08f),
            description = "Cute animated animal ears on your head"
        ),
        SnaporaFilter(
            id = "party_hat",
            name = "Party Hat",
            category = FilterCategory.FUN,
            iconEmoji = "🥳",
            propType = FacePropType.PARTY_CROWN,
            saturationBoost = 1.2f,
            description = "Festive celebration hat tracking your head"
        ),
        SnaporaFilter(
            id = "gentleman_moustache",
            name = "Moustache",
            category = FilterCategory.FUN,
            iconEmoji = "🥸",
            propType = FacePropType.GENTLEMAN_MOUSTACHE,
            contrastBoost = 1.1f,
            description = "Classic vintage moustache under your nose"
        ),

        // Cute Category (Animated Face Elements)
        SnaporaFilter(
            id = "floating_hearts",
            name = "Hearts",
            category = FilterCategory.CUTE,
            iconEmoji = "💖",
            propType = FacePropType.FLOATING_HEARTS,
            colorOverlay = Color(0xFFFF2E93).copy(alpha = 0.10f),
            beautyGlow = true,
            description = "Pulsing pastel hearts floating over your head"
        ),
        SnaporaFilter(
            id = "sparkle_aura",
            name = "Sparkles",
            category = FilterCategory.CUTE,
            iconEmoji = "✨",
            propType = FacePropType.SPARKLE_AURA,
            brightnessBoost = 0.12f,
            description = "Glittering starbursts around your eyes"
        ),
        SnaporaFilter(
            id = "floral_crown",
            name = "Flower Crown",
            category = FilterCategory.CUTE,
            iconEmoji = "🌺",
            propType = FacePropType.FLORAL_CROWN,
            colorOverlay = Color(0xFFFFE4E6).copy(alpha = 0.10f),
            description = "Fresh botanical wreath resting on your head"
        ),
        SnaporaFilter(
            id = "angel_halo",
            name = "Angel Halo",
            category = FilterCategory.CUTE,
            iconEmoji = "😇",
            propType = FacePropType.ANGEL_HALO,
            colorOverlay = Color(0xFFFFFBEA).copy(alpha = 0.12f),
            brightnessBoost = 0.14f,
            description = "Golden glowing halo suspended above head"
        ),
        SnaporaFilter(
            id = "star_blush",
            name = "Star Blush",
            category = FilterCategory.CUTE,
            iconEmoji = "⭐",
            propType = FacePropType.STAR_BLUSH,
            beautyGlow = true,
            colorOverlay = Color(0xFFFFC6D9).copy(alpha = 0.10f),
            description = "Glittering star freckles over both cheeks"
        ),

        // Cinematic Category
        SnaporaFilter(
            id = "cyber_neon",
            name = "Cyber Neon",
            category = FilterCategory.CINEMATIC,
            iconEmoji = "⚡",
            colorOverlay = Color(0xFF9333EA).copy(alpha = 0.22f),
            contrastBoost = 1.3f,
            saturationBoost = 1.35f,
            vignetteColor = Color(0xFF0F051D).copy(alpha = 0.45f),
            description = "Electrifying purple and cyan cyberpunk mood"
        ),
        SnaporaFilter(
            id = "dramatic_film",
            name = "Dramatic Film",
            category = FilterCategory.CINEMATIC,
            iconEmoji = "🎞️",
            contrastBoost = 1.25f,
            saturationBoost = 1.15f,
            vignetteColor = Color.Black.copy(alpha = 0.4f),
            description = "Deep shadows and saturated filmic highlights"
        ),
        SnaporaFilter(
            id = "comic_pop",
            name = "Comic Pop",
            category = FilterCategory.CINEMATIC,
            iconEmoji = "💥",
            contrastBoost = 1.4f,
            saturationBoost = 1.5f,
            brightnessBoost = 0.05f,
            description = "Ultra vibrant comic book aesthetic"
        )
    )

    fun getFiltersByCategory(category: FilterCategory): List<SnaporaFilter> {
        return if (category == FilterCategory.ALL) allFilters
        else allFilters.filter { it.category == category }
    }
}
