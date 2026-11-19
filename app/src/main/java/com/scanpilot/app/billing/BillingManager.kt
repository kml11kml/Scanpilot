package com.scanpilot.app.billing

import android.content.Context
import com.scanpilot.app.data.SettingsStore
import kotlinx.coroutines.flow.Flow

/**
 * Payment gateway for the Pro version.
 *
 * Current status: STUB — no real payment code is connected and no price
 * is hardcoded in the code. The Pro price is defined only in Google Play
 * Console, and the developer manages it there.
 *
 * The correct path to connect real payments:
 *  1. In Google Play Console, create the in-app products / subscriptions
 *     (monthly, yearly, lifetime) for ScanPilot Pro.
 *  2. Add the Google Play Billing Library to the project
 *     (com.android.billingclient:billing) following Google's guide.
 *  3. Complete the PlayBillingManager class below with real BillingClient
 *     calls and use it in the ViewModels instead of LocalBillingManager.
 * Until then, the free version works fully and a purchase shows a
 * "coming soon" message.
 */
interface BillingManager {
    /** Has the user bought the Pro version? */
    val isPro: Flow<Boolean>

    /** Attempt to buy the Pro version. In the stub it always fails and explains why. */
    suspend fun purchasePro(): Result<Unit>

    /** Restore previous purchases (after reinstalling). */
    suspend fun restorePurchases(): Result<Boolean>
}

/**
 * Local payment manager — for development and testing only.
 * It does not make a real purchase; purchasePro says Google Play Billing
 * is not connected yet. The Pro flag only changes via setProLocal
 * (for your own testing).
 */
class LocalBillingManager(context: Context) : BillingManager {

    private val store = SettingsStore(context)

    override val isPro: Flow<Boolean> = store.isPro

    override suspend fun purchasePro(): Result<Unit> {
        return Result.failure(
            IllegalStateException(
                "Buying the Pro version will be available soon. " +
                    "Google Play Billing has not been connected yet."
            )
        )
    }

    override suspend fun restorePurchases(): Result<Boolean> {
        return Result.success(false)
    }

    /** For developer testing only. */
    suspend fun setProLocal(value: Boolean) = store.setPro(value)
}

/**
 * TODO — Google Play Billing connection point.
 * This class is intentionally empty so nobody thinks payments are connected.
 * When the Play Billing Library is added, complete the methods with its real calls:
 *  - purchasePro: launch the billing flow for the Pro product defined in Play Console
 *  - restorePurchases: query the user's previous purchases and acknowledge them
 *  - isPro: purchase status from the local cache + verification with Google Play
 */
class PlayBillingManager : BillingManager {
    override val isPro: Flow<Boolean> = kotlinx.coroutines.flow.flowOf(false)

    override suspend fun purchasePro(): Result<Unit> {
        return Result.failure(
            IllegalStateException("TODO: The Google Play Billing Library has not been added to the project yet.")
        )
    }

    override suspend fun restorePurchases(): Result<Boolean> {
        return Result.failure(
            IllegalStateException("TODO: The Google Play Billing Library has not been added to the project yet.")
        )
    }
}
