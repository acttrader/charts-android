package com.acttrader.acttradercharts

import org.json.JSONObject

/**
 * Account figures the Long/Short position tools size themselves against.
 *
 * TradingView asks the user to type these because it has no broker connection.
 * An ActTrader host does know the live account, so pass real equity here and
 * keep it current — a sketch drawn against a stale balance quietly reports the
 * wrong quantity rather than failing visibly.
 *
 * Omit it and the position tools still draw, showing price, percent, pips and
 * risk/reward but no quantity or money amounts.
 *
 * ```kotlin
 * chart.setAccount(AccountSpec(size = 10_000.0, riskPercent = 1.0))
 * ```
 *
 * @property size Account equity in the account currency.
 * @property riskPercent Percent of the account risked per trade. Defaults to `1.0`.
 */
data class AccountSpec(
    val size: Double? = null,
    val riskPercent: Double? = null,
) {
    internal fun toJson(): JSONObject = JSONObject().apply {
        size?.let { put("size", it) }
        riskPercent?.let { put("riskPercent", it) }
    }
}
