package com.psy.ui.icons

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import com.composables.icons.lucide.*

/**
 * Resolves a stored Lucide icon-name string (e.g. "shopping-bag") to an ImageVector.
 * The map covers the icons the app seeds + offers in the picker; unknown names fall
 * back to [Lucide.CircleDollarSign]. The kebab-case keys are the portable ids written
 * into the cross-platform snapshot, so iOS must resolve the same strings.
 */
object LucideIcons {
    val byName: Map<String, ImageVector> = mapOf(
        "wallet" to Lucide.Wallet,
        "landmark" to Lucide.Landmark,
        "utensils" to Lucide.Utensils,
        "shopping-cart" to Lucide.ShoppingCart,
        "coffee" to Lucide.Coffee,
        "cup-soda" to Lucide.CupSoda,
        "bus" to Lucide.Bus,
        "bike" to Lucide.Bike,
        "fuel" to Lucide.Fuel,
        "train-front" to Lucide.TrainFront,
        "square-parking" to Lucide.SquareParking,
        "car" to Lucide.Car,
        "shopping-bag" to Lucide.ShoppingBag,
        "shirt" to Lucide.Shirt,
        "package" to Lucide.Package,
        "receipt" to Lucide.Receipt,
        "lightbulb" to Lucide.Lightbulb,
        "globe" to Lucide.Globe,
        "gamepad-2" to Lucide.Gamepad2,
        "banknote" to Lucide.Banknote,
        "gift" to Lucide.Gift,
        "circle-dollar-sign" to Lucide.CircleDollarSign,
        "house" to Lucide.House,
        "pill" to Lucide.Pill,
        "hospital" to Lucide.Hospital,
        "smartphone" to Lucide.Smartphone,
        "plane" to Lucide.Plane,
        "graduation-cap" to Lucide.GraduationCap,
        "dog" to Lucide.Dog,
        "credit-card" to Lucide.CreditCard,
        "trending-up" to Lucide.TrendingUp,
        "dumbbell" to Lucide.Dumbbell,
        "music" to Lucide.Music,
        "umbrella" to Lucide.Umbrella,
        "beer" to Lucide.Beer,
        "clapperboard" to Lucide.Clapperboard,
        "chart-column" to Lucide.ChartColumn,
    )

    /** Icons offered in the picker (ordered). Extend freely. */
    val pickerSet: List<String> = byName.keys.toList()

    fun resolve(name: String): ImageVector = byName[name] ?: Lucide.CircleDollarSign
}

@Composable
fun LucideIcon(
    name: String,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    size: Dp = 24.dp,
) {
    Icon(
        imageVector = LucideIcons.resolve(name),
        contentDescription = null,
        tint = tint,
        modifier = modifier.size(size),
    )
}
