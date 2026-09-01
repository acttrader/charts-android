package com.acttrader.acttradercharts

import org.json.JSONObject

/**
 * What a price *means* for the current instrument.
 *
 * The chart reads prices, never contract specs — so a tool that reports a
 * distance in pips, or converts one to money, has to be told how. The ruler uses
 * this to show a pip count alongside the price delta; without it, the readout
 * stops after the percentage.
 *
 * Every field is optional and every one degrades to a documented fallback, so an
 * app that omits the whole block simply gets price-only readouts.
 *
 * Pass it at [ActtraderChartsView.init] and swap it with
 * [ActtraderChartsView.setInstrument] whenever the symbol changes — specs belong
 * to the instrument, and a stale pip size reports a wrong number rather than
 * failing visibly.
 *
 * ```kotlin
 * chart.setSymbol("USDJPY")
 * chart.setInstrument(InstrumentSpec(pipSize = 0.01, contractSize = 100_000.0))
 * ```
 *
 * @property pipSize Price distance counted as one pip — `0.0001` for most FX
 *   pairs, `0.01` for JPY crosses. When omitted it is inferred from how many
 *   decimals the feed quotes, which follows the usual FX convention and is
 *   **wrong for metals, indices and crypto**. Pass it explicitly if pips matter.
 * @property contractSize Units per lot (`100.0` for XAUUSD, `100000.0` for most
 *   FX pairs). Defaults to `1.0`.
 * @property valuePerPoint Account-currency value of one price unit per contract
 *   unit. Defaults to `1.0`.
 * @property currencySymbol Prefixed to money figures. Defaults to `"$"`.
 */
data class InstrumentSpec(
    val pipSize: Double? = null,
    val contractSize: Double? = null,
    val valuePerPoint: Double? = null,
    val currencySymbol: String? = null,
) {
    internal fun toJson(): JSONObject = JSONObject().apply {
        pipSize?.let { put("pipSize", it) }
        contractSize?.let { put("contractSize", it) }
        valuePerPoint?.let { put("valuePerPoint", it) }
        currencySymbol?.let { put("currencySymbol", it) }
    }
}
