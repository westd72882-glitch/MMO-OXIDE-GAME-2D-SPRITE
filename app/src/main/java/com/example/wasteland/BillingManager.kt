package com.example.wasteland

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryProductDetailsResult
import com.android.billingclient.api.QueryPurchasesParams
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf

/**
 * ===================== ДОНАТ ЧЕРЕЗ GOOGLE PLAY BILLING =====================
 *
 * Это РЕАЛЬНАЯ интеграция с Google Play Billing Library 7 — платежи
 * настоящие, деньги будут реально списываться с карты пользователя
 * и поступать вам через Google Play Console (за вычетом комиссии Google,
 * обычно 15-30%).
 *
 * ЧТО НУЖНО СДЕЛАТЬ ВАМ ПЕРЕД ТЕМ, КАК ЭТО ЗАРАБОТАЕТ:
 *
 * 1. Приложение должно быть загружено в Google Play Console (хотя бы
 *    в закрытое тестирование — не обязательно в продакшн) и подписано
 *    РЕЛИЗНЫМ ключом (не debug-ключом, который использует текущая CI-сборка).
 *    Google Play Billing НЕ РАБОТАЕТ с debug-сборками и неопубликованными
 *    приложениями — это ограничение самого Google, не проекта.
 *
 * 2. В Google Play Console -> ваше приложение -> Monetize -> Products ->
 *    In-app products, создайте товары с ТОЧНО такими же ID, как в
 *    DONATION_PRODUCTS ниже (donation_small, donation_medium, donation_large,
 *    donation_mega). Задайте цену и описание для каждого.
 *
 * 3. Аккаунт разработчика Google Play должен быть привязан к банковскому
 *    счёту (Google Payments merchant account) — иначе платежи не примутся.
 *
 * 4. Приложение должно быть установлено ИМЕННО из Google Play (или через
 *    внутреннее тестирование по ссылке из Play Console) — сборка через
 *    обычный APK-файл (как сейчас, через GitHub Actions -> скачать APK
 *    вручную) Billing API вызовет, но покупка завершится ошибкой
 *    "Item unavailable", пока приложение не установлено через Play.
 *
 * До этого момента код ниже полностью рабочий и готов к использованию —
 * просто покупки будут возвращать ошибку "product not found", пока товары
 * не созданы в консоли и приложение не опубликовано.
 * ============================================================================
 */

data class DonationProduct(
    val productId: String,
    val coinReward: Int,
    val boostSeconds: Double = 0.0
)

/** ID должны СОВПАДАТЬ 1-в-1 с тем, что вы создадите в Play Console. */
val DONATION_PRODUCTS = listOf(
    DonationProduct("donation_small", coinReward = 500),
    DonationProduct("donation_medium", coinReward = 3000, boostSeconds = 1800.0),
    DonationProduct("donation_large", coinReward = 10000, boostSeconds = 7200.0),
    DonationProduct("donation_mega", coinReward = 50000, boostSeconds = 86400.0),
)

class BillingManager(private val context: Context, private val state: GameState) {

    private val TAG = "BillingManager"

    var isReady = mutableStateOf(false)
        private set

    /** Загруженные цены с Google Play, ключ = productId, значение = отформатированная цена ("₽149.00"). */
    val prices = mutableStateMapOf<String, String>()

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.i(TAG, "Покупка отменена пользователем")
        } else {
            Log.w(TAG, "Ошибка покупки: ${billingResult.debugMessage}")
        }
    }

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases()
        .build()

    fun connect() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    isReady.value = true
                    queryProductDetails()
                    queryExistingPurchases()
                } else {
                    Log.w(TAG, "Billing setup не удался: ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                isReady.value = false
                // Billing library сама переподключается при следующем вызове,
                // но можно явно попробовать снова:
                connect()
            }
        })
    }

    private fun queryProductDetails() {
        val products = DONATION_PRODUCTS.map { product ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(product.productId)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        }
        val params = QueryProductDetailsParams.newBuilder().setProductList(products).build()

        billingClient.queryProductDetailsAsync(params) { billingResult, result: QueryProductDetailsResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                result.productDetailsList.forEach { details: ProductDetails ->
                    productDetailsCache[details.productId] = details
                    val price = details.oneTimePurchaseOfferDetails?.formattedPrice
                    if (price != null) {
                        prices[details.productId] = price
                    }
                }
            } else {
                Log.w(TAG, "Не удалось загрузить товары: ${billingResult.debugMessage}. " +
                    "Убедитесь, что товары созданы в Play Console и приложение опубликовано.")
            }
        }
    }

    private val productDetailsCache = mutableMapOf<String, ProductDetails>()

    /** Запускает системное окно оплаты Google Play для указанного доната. */
    fun launchPurchase(activity: Activity, productId: String) {
        val details = productDetailsCache[productId]
        if (details == null) {
            state.let { /* показать тост через UI-слой, см. DonationSection */ }
            Log.w(TAG, "Товар $productId ещё не загружен из Play Console")
            return
        }
        val offerToken = details.oneTimePurchaseOfferDetails?.offerToken ?: return

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details)
                .setOfferToken(offerToken)
                .build()
        )
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        billingClient.launchBillingFlow(activity, flowParams)
    }

    private fun handlePurchase(purchase: com.android.billingclient.api.Purchase) {
        if (purchase.purchaseState != com.android.billingclient.api.Purchase.PurchaseState.PURCHASED) return

        for (productId in purchase.products) {
            val product = DONATION_PRODUCTS.find { it.productId == productId } ?: continue

            // Начисляем награду сразу в игре
            state.grantDonationCoins(product.coinReward)
            if (product.boostSeconds > 0) {
                state.activateIncomeBoost(product.boostSeconds, multiplier = 2.0)
            }

            // Донаты — расходуемые (consumable): подтверждаем и "потребляем",
            // чтобы пользователь мог купить их снова в будущем.
            if (!purchase.isAcknowledged) {
                val consumeParams = ConsumeParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient.consumeAsync(consumeParams) { billingResult, _ ->
                    if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                        Log.w(TAG, "Не удалось подтвердить покупку: ${billingResult.debugMessage}")
                    }
                }
            }
        }
    }

    private fun queryExistingPurchases() {
        // Проверяем незавершённые/непотреблённые покупки (например, если
        // приложение закрылось между оплатой и consumeAsync) и доводим их до конца.
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                purchases.forEach { handlePurchase(it) }
            }
        }
    }

    fun disconnect() {
        billingClient.endConnection()
    }
}
