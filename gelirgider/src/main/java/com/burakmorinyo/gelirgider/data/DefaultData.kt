package com.burakmorinyo.gelirgider.data

object DefaultData {

    val expenseCategories = listOf(
        "Market" to ("shopping_cart" to 0xFF22C55E),
        "Yemek & Kafe" to ("restaurant" to 0xFFF97316),
        "Kira" to ("home" to 0xFF6366F1),
        "Faturalar" to ("bolt" to 0xFFEAB308),
        "Telefon & İnternet" to ("wifi" to 0xFF06B6D4),
        "Ulaşım" to ("directions_bus" to 0xFF3B82F6),
        "Akaryakıt" to ("gas" to 0xFFF59E0B),
        "Sağlık" to ("health" to 0xFFEF4444),
        "Giyim" to ("checkroom" to 0xFFEC4899),
        "Eğlence" to ("movie" to 0xFFA855F7),
        "Abonelikler" to ("subscriptions" to 0xFF8B5CF6),
        "Eğitim" to ("school" to 0xFF0EA5E9),
        "Ev & Yaşam" to ("chair" to 0xFF84CC16),
        "Elektronik" to ("devices" to 0xFF64748B),
        "Kişisel Bakım" to ("spa" to 0xFFF472B6),
        "Hediye" to ("gift" to 0xFFE11D48),
        "Evcil Hayvan" to ("pets" to 0xFFB45309),
        "Seyahat" to ("flight" to 0xFF14B8A6),
        "Vergi & Harç" to ("gavel" to 0xFF78716C),
        "Kredi & Faiz" to ("percent" to 0xFFDC2626),
        "Diğer" to ("more" to 0xFF94A3B8)
    )

    val incomeCategories = listOf(
        "Maaş" to ("work" to 0xFF16A34A),
        "Ek Gelir" to ("laptop" to 0xFF0891B2),
        "Prim & İkramiye" to ("star" to 0xFFF59E0B),
        "Kira Geliri" to ("apartment" to 0xFF7C3AED),
        "Yatırım Getirisi" to ("trending_up" to 0xFF059669),
        "Hediye" to ("gift" to 0xFFDB2777),
        "İade" to ("undo" to 0xFF2563EB),
        "Diğer" to ("more" to 0xFF94A3B8)
    )

    fun categories(): List<Category> =
        expenseCategories.mapIndexed { i, (name, v) -> Category(name = name, type = TxType.EXPENSE, icon = v.first, color = v.second, sortOrder = i) } +
            incomeCategories.mapIndexed { i, (name, v) -> Category(name = name, type = TxType.INCOME, icon = v.first, color = v.second, sortOrder = i) }

    fun accounts(): List<Account> = listOf(
        Account(name = "Nakit", type = AccountType.CASH, color = 0xFF22C55E, icon = "wallet", sortOrder = 0),
        Account(name = "Banka Hesabı", type = AccountType.BANK, color = 0xFF3B82F6, icon = "bank", sortOrder = 1)
    )

    val palette = listOf(
        0xFF6366F1, 0xFF8B5CF6, 0xFFA855F7, 0xFFEC4899, 0xFFE11D48, 0xFFEF4444,
        0xFFF97316, 0xFFF59E0B, 0xFFEAB308, 0xFF84CC16, 0xFF22C55E, 0xFF10B981,
        0xFF14B8A6, 0xFF06B6D4, 0xFF0EA5E9, 0xFF3B82F6, 0xFF64748B, 0xFF78716C
    )
}
