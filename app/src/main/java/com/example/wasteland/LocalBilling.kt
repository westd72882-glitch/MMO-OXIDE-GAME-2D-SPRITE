package com.example.wasteland

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Даёт доступ к BillingManager из любого composable-экрана (например,
 * DonationSection в Components.kt) без необходимости прокидывать его
 * явным параметром через весь GameScreen -> ShopList -> DonationSection.
 *
 * Установлен в MainActivity через CompositionLocalProvider.
 */
val LocalBillingManager = staticCompositionLocalOf<BillingManager?> { null }
