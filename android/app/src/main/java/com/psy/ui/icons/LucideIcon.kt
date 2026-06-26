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
        "arrow-right-left" to Lucide.ArrowRightLeft,
        "list" to Lucide.List,
        // expansion
        "pizza" to Lucide.Pizza,
        "sandwich" to Lucide.Sandwich,
        "ice-cream-cone" to Lucide.IceCreamCone,
        "cake" to Lucide.Cake,
        "wine" to Lucide.Wine,
        "milk" to Lucide.Milk,
        "soup" to Lucide.Soup,
        "salad" to Lucide.Salad,
        "cookie" to Lucide.Cookie,
        "apple" to Lucide.Apple,
        "fish" to Lucide.Fish,
        "egg" to Lucide.Egg,
        "ham" to Lucide.Ham,
        "drumstick" to Lucide.Drumstick,
        "candy" to Lucide.Candy,
        "ship" to Lucide.Ship,
        "truck" to Lucide.Truck,
        "footprints" to Lucide.Footprints,
        "plane-takeoff" to Lucide.PlaneTakeoff,
        "store" to Lucide.Store,
        "shopping-basket" to Lucide.ShoppingBasket,
        "tag" to Lucide.Tag,
        "tags" to Lucide.Tags,
        "gem" to Lucide.Gem,
        "watch" to Lucide.Watch,
        "glasses" to Lucide.Glasses,
        "baby" to Lucide.Baby,
        "zap" to Lucide.Zap,
        "droplet" to Lucide.Droplet,
        "flame" to Lucide.Flame,
        "wifi" to Lucide.Wifi,
        "phone" to Lucide.Phone,
        "plug" to Lucide.Plug,
        "recycle" to Lucide.Recycle,
        "stethoscope" to Lucide.Stethoscope,
        "heart-pulse" to Lucide.HeartPulse,
        "activity" to Lucide.Activity,
        "syringe" to Lucide.Syringe,
        "brain" to Lucide.Brain,
        "film" to Lucide.Film,
        "tv" to Lucide.Tv,
        "headphones" to Lucide.Headphones,
        "mic" to Lucide.Mic,
        "camera" to Lucide.Camera,
        "ticket" to Lucide.Ticket,
        "party-popper" to Lucide.PartyPopper,
        "dices" to Lucide.Dices,
        "book" to Lucide.Book,
        "book-open" to Lucide.BookOpen,
        "star" to Lucide.Star,
        "briefcase" to Lucide.Briefcase,
        "building" to Lucide.Building,
        "building-2" to Lucide.Building2,
        "coins" to Lucide.Coins,
        "piggy-bank" to Lucide.PiggyBank,
        "hand-coins" to Lucide.HandCoins,
        "calculator" to Lucide.Calculator,
        "chart-pie" to Lucide.ChartPie,
        "percent" to Lucide.Percent,
        "dollar-sign" to Lucide.DollarSign,
        "users" to Lucide.Users,
        "heart" to Lucide.Heart,
        "cat" to Lucide.Cat,
        "bone" to Lucide.Bone,
        "paw-print" to Lucide.PawPrint,
        "map-pin" to Lucide.MapPin,
        "map" to Lucide.Map,
        "luggage" to Lucide.Luggage,
        "tent" to Lucide.Tent,
        "mountain" to Lucide.Mountain,
        "hotel" to Lucide.Hotel,
        "compass" to Lucide.Compass,
        "bed" to Lucide.Bed,
        "sofa" to Lucide.Sofa,
        "lamp" to Lucide.Lamp,
        "key" to Lucide.Key,
    )

    /** Icons offered in the picker (ordered). Keep in sync with iOS pickerSet. */
    val pickerSet: List<String> = listOf(
        "wallet", "landmark", "utensils", "shopping-cart", "coffee", "cup-soda", "bus", "bike",
        "fuel", "train-front", "square-parking", "car", "shopping-bag", "shirt", "package",
        "receipt", "lightbulb", "globe", "gamepad-2", "banknote", "gift", "circle-dollar-sign",
        "house", "pill", "hospital", "smartphone", "plane", "graduation-cap", "dog", "credit-card",
        "trending-up", "dumbbell", "music", "umbrella", "beer", "clapperboard",
        // expansion (keep in sync with iOS)
        "pizza", "sandwich", "ice-cream-cone", "cake", "wine", "milk", "soup", "salad", "cookie",
        "apple", "fish", "egg", "ham", "drumstick", "candy", "ship", "truck", "footprints",
        "plane-takeoff", "store", "shopping-basket", "tag", "tags", "gem", "watch", "glasses",
        "baby", "zap", "droplet", "flame", "wifi", "phone", "plug", "recycle", "stethoscope",
        "heart-pulse", "activity", "syringe", "brain", "film", "tv", "headphones", "mic", "camera",
        "ticket", "party-popper", "dices", "book", "book-open", "star", "briefcase", "building",
        "building-2", "coins", "piggy-bank", "hand-coins", "calculator", "chart-pie", "percent",
        "dollar-sign", "users", "heart", "cat", "bone", "paw-print", "map-pin", "map", "luggage",
        "tent", "mountain", "hotel", "compass", "bed", "sofa", "lamp", "key",
    )

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
