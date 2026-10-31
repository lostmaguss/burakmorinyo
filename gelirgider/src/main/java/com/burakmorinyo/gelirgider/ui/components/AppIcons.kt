package com.burakmorinyo.gelirgider.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Apartment
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.BeachAccess
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.Chair
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.ChildFriendly
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalBar
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.LocalTaxi
import androidx.compose.material.icons.rounded.Laptop
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Percent
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.SportsSoccer
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Subscriptions
import androidx.compose.material.icons.rounded.Train
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material.icons.rounded.CurrencyExchange
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.ui.graphics.vector.ImageVector

/** Kategori/hesap ikonları veritabanında anahtar olarak saklanır */
object AppIcons {
    val all: Map<String, ImageVector> = linkedMapOf(
        "shopping_cart" to Icons.Rounded.ShoppingCart,
        "restaurant" to Icons.Rounded.Restaurant,
        "coffee" to Icons.Rounded.LocalCafe,
        "home" to Icons.Rounded.Home,
        "bolt" to Icons.Rounded.Bolt,
        "water" to Icons.Rounded.WaterDrop,
        "wifi" to Icons.Rounded.Wifi,
        "phone" to Icons.Rounded.PhoneAndroid,
        "directions_bus" to Icons.Rounded.DirectionsBus,
        "car" to Icons.Rounded.DirectionsCar,
        "taxi" to Icons.Rounded.LocalTaxi,
        "train" to Icons.Rounded.Train,
        "gas" to Icons.Rounded.LocalGasStation,
        "health" to Icons.Rounded.LocalHospital,
        "checkroom" to Icons.Rounded.Checkroom,
        "shopping_bag" to Icons.Rounded.ShoppingBag,
        "movie" to Icons.Rounded.Movie,
        "music" to Icons.Rounded.MusicNote,
        "games" to Icons.Rounded.SportsEsports,
        "sports" to Icons.Rounded.SportsSoccer,
        "fitness" to Icons.Rounded.FitnessCenter,
        "subscriptions" to Icons.Rounded.Subscriptions,
        "school" to Icons.Rounded.School,
        "book" to Icons.Rounded.MenuBook,
        "chair" to Icons.Rounded.Chair,
        "build" to Icons.Rounded.Build,
        "devices" to Icons.Rounded.Devices,
        "spa" to Icons.Rounded.Spa,
        "gift" to Icons.Rounded.CardGiftcard,
        "cake" to Icons.Rounded.Cake,
        "child" to Icons.Rounded.ChildFriendly,
        "pets" to Icons.Rounded.Pets,
        "flight" to Icons.Rounded.Flight,
        "beach" to Icons.Rounded.BeachAccess,
        "bar" to Icons.Rounded.LocalBar,
        "gavel" to Icons.Rounded.Gavel,
        "percent" to Icons.Rounded.Percent,
        "work" to Icons.Rounded.Work,
        "laptop" to Icons.Rounded.Laptop,
        "star" to Icons.Rounded.Star,
        "apartment" to Icons.Rounded.Apartment,
        "trending_up" to Icons.AutoMirrored.Rounded.TrendingUp,
        "trending_down" to Icons.AutoMirrored.Rounded.TrendingDown,
        "undo" to Icons.AutoMirrored.Rounded.Undo,
        "wallet" to Icons.Rounded.AccountBalanceWallet,
        "bank" to Icons.Rounded.AccountBalance,
        "credit_card" to Icons.Rounded.CreditCard,
        "savings" to Icons.Rounded.Savings,
        "diamond" to Icons.Rounded.Diamond,
        "exchange" to Icons.Rounded.CurrencyExchange,
        "payments" to Icons.Rounded.Payments,
        "handshake" to Icons.Rounded.Handshake,
        "repeat" to Icons.Rounded.Repeat,
        "warning" to Icons.Rounded.Warning,
        "arrow_upward" to Icons.Rounded.ArrowUpward,
        "arrow_downward" to Icons.Rounded.ArrowDownward,
        "celebration" to Icons.Rounded.Celebration,
        "idea" to Icons.Rounded.Lightbulb,
        "more" to Icons.Rounded.MoreHoriz
    )

    /** Kullanıcının seçebileceği ikonlar (iç kullanım ikonları hariç) */
    val pickable = all.keys.filter { it !in setOf("warning", "arrow_upward", "arrow_downward", "trending_down", "repeat", "idea") }

    fun get(key: String?): ImageVector = all[key] ?: Icons.Rounded.MoreHoriz
}
