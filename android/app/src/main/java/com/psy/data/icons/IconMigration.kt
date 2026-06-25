package com.psy.data.icons

import com.psy.domain.repository.AccountRepository
import com.psy.domain.repository.CategoryGroupRepository
import com.psy.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** Maps the legacy seed emoji to portable Lucide names. Unknown → "circle-dollar-sign". */
object IconMap {
    val emojiToLucide: Map<String, String> = mapOf(
        "💵" to "wallet", "🏦" to "landmark", "🍜" to "utensils", "🛒" to "shopping-cart",
        "🍽️" to "utensils", "🍴" to "utensils", "☕" to "coffee", "🧋" to "cup-soda",
        "🥤" to "cup-soda", "🚌" to "bus", "🛵" to "bike", "⛽" to "fuel", "🅿️" to "square-parking",
        "🚇" to "train-front", "🚗" to "car", "🛍️" to "shopping-bag", "👕" to "shirt",
        "🧴" to "package", "📦" to "package", "🧾" to "receipt", "💡" to "lightbulb",
        "🌐" to "globe", "🎮" to "gamepad-2", "💰" to "banknote", "🎁" to "gift",
        "🏠" to "house", "💊" to "pill", "🏥" to "hospital", "📱" to "smartphone",
        "✈️" to "plane", "🎓" to "graduation-cap", "🐶" to "dog", "💳" to "credit-card",
        "🪙" to "banknote", "📈" to "trending-up", "🏋️" to "dumbbell", "🎵" to "music",
        "☂️" to "umbrella", "🍺" to "beer", "🎬" to "clapperboard", "🍔" to "utensils",
        "🍰" to "utensils", "🚕" to "car", "🐱" to "dog", "🎀" to "gift", "🎈" to "gift",
    )

    /** Already a Lucide name (starts with an ASCII letter) → pass through; emoji → mapped; else fallback. */
    fun toLucide(icon: String): String {
        emojiToLucide[icon]?.let { return it }
        val first = icon.firstOrNull() ?: return "circle-dollar-sign"
        return if (first in 'a'..'z' || first in 'A'..'Z') icon else "circle-dollar-sign"
    }
}

/** One-time, idempotent: rewrites any emoji icon strings to Lucide names. */
class IconMigration @Inject constructor(
    private val accountRepo: AccountRepository,
    private val categoryGroupRepo: CategoryGroupRepository,
    private val categoryRepo: CategoryRepository,
) {
    suspend fun run() {
        accountRepo.observeAll().first().forEach { a ->
            val mapped = IconMap.toLucide(a.icon)
            if (mapped != a.icon) accountRepo.upsert(a.copy(icon = mapped))
        }
        categoryGroupRepo.observeAll().first().forEach { g ->
            val mapped = IconMap.toLucide(g.icon)
            if (mapped != g.icon) categoryGroupRepo.upsert(g.copy(icon = mapped))
        }
        categoryRepo.observeAll().first().forEach { c ->
            val mapped = IconMap.toLucide(c.icon)
            if (mapped != c.icon) categoryRepo.upsert(c.copy(icon = mapped))
        }
    }
}
