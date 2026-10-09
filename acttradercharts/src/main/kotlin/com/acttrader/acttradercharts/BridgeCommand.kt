package com.acttrader.acttradercharts

import android.util.Log
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

private const val TAG = "ActtraderCharts"

private fun JSONObject.putJsonArray(key: String, json: String) {
    try {
        put(key, JSONArray(json))
    } catch (e: JSONException) {
        Log.w(TAG, "Dropping malformed JSON array for key '$key': ${e.message}")
    }
}

private fun JSONObject.putJson(key: String, json: String) {
    try {
        put(key, JSONObject(json))
    } catch (e: JSONException) {
        Log.w(TAG, "Dropping malformed JSON for key '$key': ${e.message}")
    }
}

/**
 * Cross-pane sync toggles for the chart-owned layout popover (Symbol / Interval
 * / Crosshair / Time / Date range). All fields are nullable — a `null` field is
 * omitted from the payload and keeps its current (or library-default) value on
 * the chart side. Only meaningful when `enableMultipleLayouts = true`.
 */
data class LayoutSync(
    val symbol: Boolean? = null,
    val interval: Boolean? = null,
    val crosshair: Boolean? = null,
    val time: Boolean? = null,
    val dateRange: Boolean? = null,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        symbol?.let { put("symbol", it) }
        interval?.let { put("interval", it) }
        crosshair?.let { put("crosshair", it) }
        time?.let { put("time", it) }
        dateRange?.let { put("dateRange", it) }
    }
}

/**
 * Commands sent from native Android code to the chart WebView.
 * Each subclass serialises itself to the JSON format expected by `window.ChartBridge.send()`.
 *
 * Protocol shape: `{ "type": "<cmd>", "payload": { ...fields } }`
 */
sealed class BridgeCommand {

    abstract fun toJson(): String

    // ── Core ──────────────────────────────────────────────────────────────────

    /** Re-creates the chart engine. Sent once after the WebView finishes loading. */
    data class Init(
        val theme: String = "dark",
        val symbol: String? = null,
        /** Contract specs for [symbol] — see [InstrumentSpec]. */
        val instrument: InstrumentSpec? = null,
        /** Account equity and per-trade risk — see [AccountSpec]. */
        val account: AccountSpec? = null,
        /** Enable the reworked drawing tools as one switch: Long/Short Position in a
         *  new Forecasting group, freehand Brush & Highlighter, and the full Ruler
         *  readout. Drawings only — nothing reaches the broker. Position
         *  quantity/money need [account], pips need [InstrumentSpec]. Default: false. */
        val enableForecasting: Boolean? = null,
        val series: String? = null,
        val timeframe: String? = null,
        val duration: String? = null,
        val enableTrading: Boolean = false,
        val showVolume: Boolean? = null,
        val showUI: Boolean? = null,
        val showDrawingTools: Boolean? = null,
        val showBidAskLines: Boolean? = null,
        /** Show the Ask price line independently. `null` falls back to the
         *  legacy [showBidAskLines] behavior. */
        val showAskLine: Boolean? = null,
        /** Show the Bid price line independently. `null` falls back to the
         *  legacy [showBidAskLines] behavior. */
        val showBidLine: Boolean? = null,
        val showActLogo: Boolean? = null,
        val showCandleCountdown: Boolean? = null,
        val candleCountdownTimeframes: List<String>? = null,
        val disableCountdownOnMobile: Boolean? = null,
        val maxSubPanes: Int? = null,
        val mobileBarDivisor: Int? = null,
        /**
         * Minimum bars expected from the initial fetch before giving up. When the
         * native loader returns fewer bars, the chart engine automatically widens
         * the lookback window and retries — handles weekends, market closures, and
         * sparse instruments gracefully. Default: `10`.
         */
        val minInitialBars: Int? = null,
        /**
         * Hard ceiling (in milliseconds) on fetch-window lookback for auto-widening
         * retries. Default: `365L * 24 * 60 * 60 * 1000` (365 days).
         */
        val maxLookbackMs: Long? = null,
        /** Enable momentum (kinetic) scrolling on drag release. Default: `true`. */
        val momentumScrollEnabled: Boolean? = null,
        /** Per-frame velocity decay factor for momentum, normalised to 60 fps. Clamped [0.80, 0.99]. Default: `0.95`. */
        val momentumDecay: Double? = null,
        /** Minimum release velocity (px/ms) to trigger momentum. Default: `0.3`. */
        val momentumThreshold: Double? = null,
        /** Maximum launch velocity (px/ms) for momentum. Default: `6.0`. */
        val momentumMaxVelocity: Double? = null,
        val targetCandleWidth: Double? = null,
        /** Which price drives live candle close/high/low: `"bid"` (default), `"ask"`,
         *  or `"ltp"` — build candles from the last traded price (exchange/dealing
         *  feeds); ticks without a valid LTP fall back to the bid. */
        val tickClosePriceSource: String? = null,
        /** Show the LTP marker (dashed price line + axis tag). `null` (default):
         *  shown only in `"ltp"` mode. `true`: always shown when the feed supplies
         *  an LTP. `false`: hidden even in `"ltp"` mode. */
        val showLtpPrice: Boolean? = null,
        /** Show a price-source dropdown in the chart header listing these sources,
         *  e.g. `listOf("ltp", "bid")` (dealing feeds). A user pick switches the
         *  live candle source and emits [BridgeEvent.PriceSourceChange]. Hidden
         *  when `null` or empty. */
        val priceSourceSelector: List<String>? = null,
        val tradesThresholdForHorizontalLine: Int? = null,
        val tradeDisplayFilter: String? = null,
        val positionRenderStyle: String? = null,
        val hideLevelConfirmCancel: Boolean? = null,
        /**
         * When `true`, clicking or tapping outside a selected/active trade level dismisses
         * it (reverting any pending edits, mirroring ✗ Cancel). When `false` (default), the
         * active level is preserved across outside clicks — only ✓ / ✗ buttons, tapping the
         * level itself, or removing it via `setLevels` will dismiss it. Recommended `false`
         * so incidental clicks (price-axis resize, taps outside the QTY input) don't drop
         * an in-progress edit. Default: `false`.
         */
        val deselectActiveOnOutsideClick: Boolean? = null,
        /**
         * Always render SL/TP bracket lines + price pills, even when the parent
         * trade level is not hovered/selected. Close (×) buttons remain hover-only.
         * Default: `true`. Set `false` to only show them on hover/selection.
         */
        val showTradeLevelsAlways: Boolean? = true,
        /**
         * Show the candle countdown timer on the right price axis, just below the
         * live price tag. Subject to the same `candleCountdownTimeframes` filter.
         * Default: `false`.
         */
        val showPriceAxisCountdown: Boolean? = null,
        /**
         * Multiplier for trade-level Confirm/Cancel/Edit/Close button radii and gaps.
         * Scales visuals AND hit/drag areas together — useful for touch targets.
         * Clamped to `[1.0, 3.0]`. Default: `1.0`.
         */
        val tradeLevelButtonScale: Double? = null,
        /** `"price"` (default) or `"amount"` — what the SL/TP bracket pills show. */
        val bracketLabelMode: String? = null,
        /** Currency symbol for SL/TP amounts when `bracketLabelMode = "amount"`. Default `"$"`. */
        val currencySymbol: String? = null,
        val levelClusteringEnabled: Boolean? = null,
        val clusterThresholdDistance: Int? = null,
        /** Enable TFC toggle button in the top bar. When `false`, TFC is completely disabled. Default: `true`. */
        val tfcEnabled: Boolean? = null,
        val showSettings: Boolean? = null,
        /** Show the fullscreen toggle button in the top bar. Default: `false` on mobile (hidden). */
        val showFullscreenButton: Boolean = false,
        val hideSymbolAndTick: Boolean? = null,
        val hideOHLCV: Boolean? = null,
        val showBottomBar: Boolean? = null,
        /** Per-timeframe base interval override for client-side aggregation, e.g. `mapOf("1h" to "30m")`. */
        val aggregateFrom: Map<String, String>? = null,
        /** Per-theme canvas background color overrides as a raw JSON string. */
        val canvasColorsJson: String? = null,
        /** Per-theme deep-partial color overrides for the built-in themes as a raw JSON string. */
        val themeOverridesJson: String? = null,
        /** User-visible string overrides for i18n/localisation as a raw JSON string. */
        val labelsJson: String? = null,
        /** Per-component UI configuration overrides as a raw JSON string. */
        val uiConfigJson: String? = null,
        /** Override the default duration → timeframe pairings, e.g. `mapOf("1Y" to "1D")`. */
        val durationTimeframeMap: Map<String, String>? = null,
        /** When true, fires a `symbolClick` bridge event on symbol tap instead of opening the picker modal. */
        val onSymbolClick: Boolean = false,
        /**
         * When true, the `"mobile"` header renders an "Ask AI" (✦) button that fires
         * a [BridgeEvent.AskAiClick] event on tap. No effect in other header layouts.
         * Default: `false` (button hidden).
         */
        val onAskAiClick: Boolean = false,
        /** IANA timezone string for time-axis and crosshair labels. Default: `"UTC"`. */
        val timezone: String? = null,
        /**
         * Top-bar variant. `"simple"` (default) shows the classic TopBar; `"advanced"`
         * uses the compact pill-style AdvancedToolbar; `"compact"` uses the slim
         * per-pane CompactToolbar — recommended only when this chart is one cell
         * of a host-rendered multi-pane grid; `"mobile"` renders the compact mobile
         * header (Tools button · timeframe pills · optional Ask AI button) — gate it
         * yourself, e.g. `headerLayout = if (isMobile) "mobile" else null`.
         */
        val headerLayout: String? = null,
        /**
         * Enables the chart-owned multi-layout popover (Layout button + 26 preset
         * picker + sync toggles). Fires `layoutChange` events; host is responsible
         * for actually mounting N panes.
         */
        val enableMultipleLayouts: Boolean? = null,
        /**
         * Enables the chart-owned snapshot popover (Snapshot button + Download/Copy).
         * Fires `snapshot` events with the PNG data URL; native layers can intercept
         * to save via platform APIs.
         */
        val enableSnapshot: Boolean? = null,
        /**
         * Hides the chart header entirely (whichever variant [headerLayout] would
         * have rendered). Bottom bar, drawing tools, and on-canvas overlays stay on
         * their own flags. Drive the chart from native UI via [BridgeCommand.SetTimeframe],
         * [BridgeCommand.SetSeries], [BridgeCommand.AddIndicatorByName],
         * [BridgeCommand.RemoveIndicator]. Default: `false` (header visible).
         */
        val hideHeader: Boolean? = null,
        /**
         * Compare symbols to add automatically on chart init. Each entry triggers
         * a [BridgeEvent.CompareDataRequest] against the initial primary range —
         * respond via [BridgeCommand.ResolveCompareDataRequest].
         */
        val initialCompares: List<String>? = null,
        /** Maximum concurrent compare symbols. Adding beyond emits [BridgeEvent.CompareError]. Default: `8`. */
        val maxCompares: Int? = null,
        /**
         * Initial state of the chart-owned layout popover's cross-pane sync
         * toggles (only meaningful with [enableMultipleLayouts]). Partial — any
         * `null` field falls back to the library default. Change it later on a
         * live chart via [ActtraderChartsView.setLayoutSync].
         */
        val layoutSync: LayoutSync? = null,
        /**
         * Shows the **Templates** block at the foot of the indicators flyout — named,
         * reusable indicator sets the user saves from the current chart and re-applies
         * later. No new header button: the block lives inside the flyout the Indicators
         * button already opens, so the header is unchanged.
         *
         * The chart **persists nothing**. Saving emits
         * [BridgeEvent.IndicatorTemplateSaved] carrying the template as JSON; store it
         * yourself and pass it back via [indicatorTemplatesJson] or
         * [ActtraderChartsView.setIndicatorTemplates]. Default: `false`.
         */
        val enableIndicatorTemplates: Boolean? = null,
        /** Indicator templates to list at init — a JSON array, typically what you stored. */
        val indicatorTemplatesJson: String? = null,
        /**
         * Shows the **Templates** row and the **Apply to all charts** switch in the Chart
         * Settings dialog — named snapshots of everything that dialog edits. Both live
         * inside the dialog the settings cog already opens.
         *
         * Emits [BridgeEvent.SettingsTemplateSaved] to persist, and
         * [BridgeEvent.ChartSettingsApplied] (with `applyToAll`) when the user applies —
         * forward those settings to your other panes with
         * [ActtraderChartsView.applyChartSettings]. Default: `false`.
         */
        val enableSettingsTemplates: Boolean? = null,
        /** Settings templates to list at init — a JSON array. */
        val settingsTemplatesJson: String? = null,
        /**
         * Shows the **Saved layouts** section in the layout popover — a grid preset plus
         * every pane's full state (symbol, timeframe, series, indicators, drawings,
         * viewport, compares), not just the grid shape the preset picker already offers.
         *
         * Requires [enableMultipleLayouts]; on its own it does nothing. Emits
         * [BridgeEvent.LayoutSaved] to persist. Default: `false`.
         */
        val enableSavedLayouts: Boolean? = null,
        /** Saved layouts to list at init — a JSON array. */
        val savedLayoutsJson: String? = null,
        /**
         * Enables **Quick Search**, the command palette over everything the chart can do:
         * series types, timeframes, durations, indicators, drawing tools, saved templates
         * and layouts, and the header actions.
         *
         * It is keyboard-first on the web (Ctrl/⌘+K, `/`) — on Android there is no such
         * key, so open it from your own toolbar with
         * [ActtraderChartsView.openQuickSearch]. Default: `false`.
         */
        val enableQuickSearch: Boolean? = null,
        /**
         * Adds a search button to the chart header for [enableQuickSearch]. Default
         * `false`, because a new button would change a header this feature otherwise
         * leaves alone — most apps drive the palette from their own chrome instead.
         */
        val quickSearchShowButton: Boolean? = null,
        /**
         * Box/reversal parameters for the price-transform chart types, as a JSON
         * object — e.g. `{"renko":{"boxSize":{"kind":"fixed","size":5}}}`.
         *
         * Omit for the default of **ATR(14)** on every one of them, which is what
         * you want unless the instrument has a meaningful fixed tick: a box of
         * "10" is noise on an index and a lifetime on a forex pair.
         */
        val seriesOptionsJson: String? = null,
        /**
         * Initial pointer behaviour over the plot: `"cross"` (default), `"dot"`,
         * `"arrow"`, `"demonstration"` (a fading laser trail for screen-sharing)
         * or `"eraser"` (a tap deletes the drawing under it).
         *
         * Independent of the drawing tool. Change it live with
         * [ActtraderChartsView.setCursorMode].
         */
        val cursorMode: String? = null,
        /**
         * Shows an OHLCV readout beside a long press, on top of the crosshair it
         * already arms.
         *
         * The OHLC strip at the top of the chart carries the same numbers, but on
         * a phone it is at the far end of the screen from the finger — the user
         * has to look away from what they are pointing at. Default: `false`.
         */
        val valueTooltip: Boolean? = null,
        /**
         * Adds a **Cursors** group to the top of the drawing toolbar (Cross, Dot,
         * Arrow, Demonstration, Eraser).
         *
         * A flag rather than always-on because it adds a category to a toolbar
         * apps have laid out around its current contents. With it off the group is
         * absent and the toolbar is unchanged; the modes stay reachable from
         * [ActtraderChartsView.setCursorMode]. Default: `false`.
         */
        val enableCursorModes: Boolean? = null,
        val enableIconTools: Boolean? = null,
        val enableChartSettings: Boolean? = null,
        /** What the status line shows. Null leaves every field at its default. */
        val statusLineJson: String? = null,
        /** Price/time axis options, including precision and timezone. */
        val scalesJson: String? = null,
        /** Grid, watermark and crosshair options. */
        val canvasJson: String? = null,
        /** `"open"` (default) or `"previousClose"`. */
        val barColorSource: String? = null,
        val enableScaleControls: Boolean? = null,
        /** `"normal"` (default), `"log"` or `"percent"`. */
        val priceScaleMode: String? = null,
        val autoScale: Boolean? = null,
        val enableSidePanels: Boolean? = null,
        /** Bar Replay — the Replay button and its control strip. Default: `false`. */
        val enableReplay: Boolean? = null,
        /** Two-finger measure (mobile): hold two fingers to see the change between two bars. Default: `false`. */
        val enableTwoFingerMeasure: Boolean? = null,
        /** Snap drawing points to the nearest OHLC of the bar under the cursor. Default: `false`. */
        val magnetMode: Boolean? = null,
        /** Keep the drawing tool armed after each completed drawing. Default: `false`. */
        val keepDrawingMode: Boolean? = null,
        /**
         * Announce every new drawing via [BridgeEvent.DrawingCreated] with
         * `copyToAll = true`, so your app can replicate it across the other panes
         * of a layout. The chart cannot do it itself — only you know which other
         * panes exist. Default: `false`.
         */
        val copyDrawingsToAllCharts: Boolean? = null,
        /**
         * Puts a crosshair on/off switch in the chart header. The crosshair itself starts
         * on (see [crosshairEnabled]) so the icon is tinted; tapping it hides the crosshair
         * — and the floating trade button that rides on it — and drops the icon to its plain
         * state; tapping again brings both back. Each tap emits [BridgeEvent.CrosshairToggle]
         * so the host can persist the choice. Default: `false`.
         */
        val enableCrossHairHeader: Boolean? = null,
        /**
         * Whether the crosshair is drawn at all. `false` hides it and the floating trade
         * button, ignores mirrored crosshair positions and keeps the long-press crosshair
         * from arming. Seeds the header switch when [enableCrossHairHeader] is on; change it
         * later via [ActtraderChartsView.setCrosshairEnabled]. Default: `true`.
         */
        val crosshairEnabled: Boolean? = null,
        /**
         * Enables horizontal (time-axis) order-line dragging. A level passed to [SetLevels]
         * with `"timeDraggable" to true` — open positions and pending orders alike — can have
         * its info-box badge dragged left/right to re-anchor it to another candle; a
         * `"timestamp"` (unix ms) says which candle the badge starts over. Sideways drags keep
         * the price locked and end in [BridgeEvent.OrderLineMoved]; a pending order's badge
         * still drags vertically to move its entry price — the first movement picks the axis.
         * Broker-gated: enable it only for the users who should have it. Default: `false`.
         */
        val orderLineTimeDrag: Boolean? = null,
        /** Snap a horizontally dragged badge to the nearest candle on release. Default: `true`. */
        val orderLineDragSnap: Boolean? = null,
        /**
         * Remember where each badge was dropped (WebView `localStorage`, keyed by level label)
         * so it comes back to the same candle after a reload. Default: `true`.
         */
        val orderLineAnchorPersistence: Boolean? = null,
        /**
         * Where an un-dragged `timeDraggable` badge sits: `"timestamp"` (over the candle at
         * the level's `timestamp`) or `"center"` (mid-chart, so a fresh market order does not
         * land on the latest candle at the right edge). Default: `"timestamp"`.
         */
        val orderLineDefaultAnchor: String? = null,
        /**
         * When a level gains a new SL/TP (from [SetLevels] or [UpdateLevelBracket]) whose
         * price sits outside the visible price range, widen the price axis so the new line
         * comes into view with the candles already on screen. Default: `false`.
         */
        val revealNewBrackets: Boolean? = null,
    ) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "init")
            put("payload", JSONObject().apply {
                put("theme", theme)
                symbol?.let { put("symbol", it) }
                instrument?.let { put("instrument", it.toJson()) }
                account?.let { put("account", it.toJson()) }
                enableForecasting?.let { put("enableForecasting", it) }
                series?.let { put("series", it) }
                timeframe?.let { put("timeframe", it) }
                duration?.let { put("duration", it) }
                if (enableTrading) {
                    put("enableTrading", true)
                }
                showVolume?.let { put("showVolume", it) }
                showUI?.let { put("showUI", it) }
                showDrawingTools?.let { put("showDrawingTools", it) }
                showBidAskLines?.let { put("showBidAskLines", it) }
                showAskLine?.let { put("showAskLine", it) }
                showBidLine?.let { put("showBidLine", it) }
                showActLogo?.let { put("showActLogo", it) }
                showCandleCountdown?.let { put("showCandleCountdown", it) }
                candleCountdownTimeframes?.let { put("candleCountdownTimeframes", JSONArray(it)) }
                disableCountdownOnMobile?.let { put("disableCountdownOnMobile", it) }
                maxSubPanes?.let { put("maxSubPanes", it) }
                mobileBarDivisor?.let { put("mobileBarDivisor", it) }
                minInitialBars?.let { put("minInitialBars", it) }
                maxLookbackMs?.let { put("maxLookbackMs", it) }
                momentumScrollEnabled?.let { put("momentumScrollEnabled", it) }
                momentumDecay?.let { put("momentumDecay", it) }
                momentumThreshold?.let { put("momentumThreshold", it) }
                momentumMaxVelocity?.let { put("momentumMaxVelocity", it) }
                targetCandleWidth?.let { put("targetCandleWidth", it) }
                tickClosePriceSource?.let { put("tickClosePriceSource", it) }
                showLtpPrice?.let { put("showLtpPrice", it) }
                priceSourceSelector?.let { put("priceSourceSelector", JSONArray(it)) }
                tradesThresholdForHorizontalLine?.let { put("tradesThresholdForHorizontalLine", it) }
                tradeDisplayFilter?.let { put("tradeDisplayFilter", it) }
                positionRenderStyle?.let { put("positionRenderStyle", it) }
                hideLevelConfirmCancel?.let { put("hideLevelConfirmCancel", it) }
                deselectActiveOnOutsideClick?.let { put("deselectActiveOnOutsideClick", it) }
                showTradeLevelsAlways?.let { put("showTradeLevelsAlways", it) }
                showPriceAxisCountdown?.let { put("showPriceAxisCountdown", it) }
                tradeLevelButtonScale?.let { put("tradeLevelButtonScale", it) }
                bracketLabelMode?.let { put("bracketLabelMode", it) }
                currencySymbol?.let { put("currencySymbol", it) }
                levelClusteringEnabled?.let { put("levelClusteringEnabled", it) }
                clusterThresholdDistance?.let { put("clusterThresholdDistance", it) }
                tfcEnabled?.let { put("tfcEnabled", it) }
                showSettings?.let { put("showSettings", it) }
                put("showFullscreenButton", showFullscreenButton)
                hideSymbolAndTick?.let { put("hideSymbolAndTick", it) }
                hideOHLCV?.let { put("hideOHLCV", it) }
                showBottomBar?.let { put("showBottomBar", it) }
                aggregateFrom?.let { put("aggregateFrom", JSONObject(it)) }
                durationTimeframeMap?.let { put("durationTimeframeMap", JSONObject(it)) }
                canvasColorsJson?.let { putJson("canvasColors", it) }
                themeOverridesJson?.let { putJson("themeOverrides", it) }
                labelsJson?.let { putJson("labels", it) }
                uiConfigJson?.let { putJson("uiConfig", it) }
                if (onSymbolClick) put("onSymbolClick", true)
                if (onAskAiClick) put("onAskAiClick", true)
                timezone?.let { put("timezone", it) }
                headerLayout?.let { put("headerLayout", it) }
                enableMultipleLayouts?.let { put("enableMultipleLayouts", it) }
                enableSnapshot?.let { put("enableSnapshot", it) }
                hideHeader?.let { put("hideHeader", it) }
                initialCompares?.let { put("initialCompares", JSONArray(it)) }
                maxCompares?.let { put("maxCompares", it) }
                layoutSync?.let { put("layoutSync", it.toJson()) }
                enableIndicatorTemplates?.let { put("enableIndicatorTemplates", it) }
                indicatorTemplatesJson?.let { putJsonArray("indicatorTemplates", it) }
                enableSettingsTemplates?.let { put("enableSettingsTemplates", it) }
                settingsTemplatesJson?.let { putJsonArray("settingsTemplates", it) }
                enableSavedLayouts?.let { put("enableSavedLayouts", it) }
                savedLayoutsJson?.let { putJsonArray("savedLayouts", it) }
                enableQuickSearch?.let { put("enableQuickSearch", it) }
                quickSearchShowButton?.let {
                    put("quickSearch", JSONObject().apply { put("showButton", it) })
                }
                seriesOptionsJson?.let { putJson("seriesOptions", it) }
                cursorMode?.let { put("cursorMode", it) }
                valueTooltip?.let { put("valueTooltip", it) }
                enableCursorModes?.let { put("enableCursorModes", it) }
                enableIconTools?.let { put("enableIconTools", it) }
                enableChartSettings?.let { put("enableChartSettings", it) }
                statusLineJson?.let { putJson("statusLine", it) }
                scalesJson?.let { putJson("scales", it) }
                canvasJson?.let { putJson("canvas", it) }
                barColorSource?.let { put("barColorSource", it) }
                enableScaleControls?.let { put("enableScaleControls", it) }
                priceScaleMode?.let { put("priceScaleMode", it) }
                autoScale?.let { put("autoScale", it) }
                enableSidePanels?.let { put("enableSidePanels", it) }
                enableReplay?.let { put("enableReplay", it) }
                enableTwoFingerMeasure?.let { put("enableTwoFingerMeasure", it) }
                magnetMode?.let { put("magnetMode", it) }
                keepDrawingMode?.let { put("keepDrawingMode", it) }
                copyDrawingsToAllCharts?.let { put("copyDrawingsToAllCharts", it) }
                enableCrossHairHeader?.let { put("enableCrossHairHeader", it) }
                crosshairEnabled?.let { put("crosshairEnabled", it) }
                orderLineTimeDrag?.let { put("orderLineTimeDrag", it) }
                orderLineDragSnap?.let { put("orderLineDragSnap", it) }
                orderLineAnchorPersistence?.let { put("orderLineAnchorPersistence", it) }
                orderLineDefaultAnchor?.let { put("orderLineDefaultAnchor", it) }
                revealNewBrackets?.let { put("revealNewBrackets", it) }
            })
        }.toString()
    }

    /** Replaces the full dataset. */
    data class LoadData(
        val bars: List<OHLCVBar>,
        val fitAll: Boolean = false,
    ) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "loadData")
            put("payload", JSONObject().apply {
                put("bars", JSONArray().also { arr ->
                    bars.forEach { bar ->
                        arr.put(JSONObject().apply {
                            put("open", bar.open)
                            put("high", bar.high)
                            put("low", bar.low)
                            put("close", bar.close)
                            put("volume", bar.volume)
                            put("time", bar.time)
                        })
                    }
                })
                put("fitAll", fitAll)
            })
        }.toString()
    }

    /** Pushes a live tick (bid/ask/timestamp) for streaming updates.
     *  [ltp]/[ltpv] (last traded price/volume) are optional — sent by
     *  exchange/dealing feeds and consumed when `tickClosePriceSource = "ltp"`. */
    data class PushTick(
        val bid: Double,
        val ask: Double,
        val timestamp: Long,
        val ltp: Double? = null,
        val ltpv: Double? = null,
    ) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "pushTick")
            put("payload", JSONObject().apply {
                put("B", bid)
                put("A", ask)
                put("T", timestamp)
                ltp?.let { put("LTP", it) }
                ltpv?.let { put("LTPV", it) }
            })
        }.toString()
    }

    /** Shows/hides the LTP price marker at runtime. Pass `null` to restore the
     *  default (marker follows `tickClosePriceSource = "ltp"`). */
    data class SetShowLtpPrice(val show: Boolean?) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "setShowLtpPrice")
            put("payload", JSONObject().apply { show?.let { put("show", it) } })
        }.toString()
    }

    /** Switches which price drives live candle close/high/low at runtime
     *  (`"bid"`, `"ask"` or `"ltp"`). Also syncs the header price-source
     *  dropdown when `priceSourceSelector` is enabled. */
    data class SetTickClosePriceSource(val source: String) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "setTickClosePriceSource")
            put("payload", JSONObject().apply { put("source", source) })
        }.toString()
    }

    // ── Appearance ────────────────────────────────────────────────────────────

    /** Switches between `"dark"` and `"light"` themes. */
    data class SetTheme(val theme: String) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "setTheme")
            put("payload", JSONObject().apply {
                put("theme", theme)
            })
        }.toString()
    }

    /**
     * Changes the display timezone for time-axis and crosshair labels.
     * Accepts any IANA string (e.g. `"America/New_York"`), `"UTC"`, or `"local"`.
     */
    data class SetTimezone(val timezone: String) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "setTimezone")
            put("payload", JSONObject().apply {
                put("timezone", timezone)
            })
        }.toString()
    }

    /**
     * Updates the chart-owned layout popover's cross-pane sync toggles. Partial —
     * `null` fields keep their current value. Only meaningful with
     * `enableMultipleLayouts = true`.
     */
    data class SetLayoutSync(val sync: LayoutSync) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "setLayoutSync")
            put("payload", sync.toJson())
        }.toString()
    }

    /**
     * Shows or hides the crosshair at runtime — together with the floating trade button
     * that rides on it. Same effect as tapping the header switch ([Init.enableCrossHairHeader]);
     * the switch follows, and [BridgeEvent.CrosshairToggle] fires when the state changes.
     */
    data class SetCrosshairEnabled(val enabled: Boolean) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "setCrosshairEnabled")
            put("payload", JSONObject().apply { put("enabled", enabled) })
        }.toString()
    }

    /** Changes the chart series type (e.g. `"candlestick"`, `"line"`, `"area"`). */
    data class SetSeries(val series: String) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "setSeries")
            put("payload", JSONObject().apply {
                put("series", series)
            })
        }.toString()
    }

    /** Changes the active timeframe (e.g. `"1m"`, `"1h"`, `"1D"`). */
    /**
     * Selects a duration and refetches. The timeframe is paired from
     * `durationTimeframeMap` unless [timeframe] is given, and the x-axis
     * rescales from the new bars — no reinitialisation needed.
     */
    data class SetDuration(val duration: String, val timeframe: String? = null) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "setDuration")
            put("payload", JSONObject().apply {
                put("duration", duration)
                timeframe?.let { put("timeframe", it) }
            })
        }.toString()
    }

    /** Switches SL/TP pills between the bracket price and the money it is worth. */
    data class SetBracketLabelMode(val mode: String, val currencySymbol: String? = null) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "setBracketLabelMode")
            put("payload", JSONObject().apply {
                put("mode", mode)
                currencySymbol?.let { put("currencySymbol", it) }
            })
        }.toString()
    }

    data class SetTimeframe(val timeframe: String) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "setTimeframe")
            put("payload", JSONObject().apply {
                put("timeframe", timeframe)
            })
        }.toString()
    }

    /** Updates the displayed symbol name. */
    data class SetSymbol(val symbol: String) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "setSymbol")
            put("payload", JSONObject().apply {
                put("symbol", symbol)
            })
        }.toString()
    }

    /**
     * Replaces the contract specs the measurement tools use to report pips and
     * money. Pair it with [SetSymbol] — specs belong to the instrument, and a
     * stale pip size reports a wrong number rather than failing visibly.
     *
     * Every field is optional; omitted ones fall back (pip size is inferred from
     * the feed's decimal count, which follows the usual FX convention and is
     * wrong for metals, indices and crypto). Pass `null` to clear the specs.
     */
    data class SetInstrument(val instrument: InstrumentSpec?) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "setInstrument")
            put("payload", JSONObject().apply {
                if (instrument != null) put("instrument", instrument.toJson())
            })
        }.toString()
    }

    /**
     * Updates the account figures the Long/Short position tools size against.
     * Push it whenever equity moves — a sketch drawn against a stale balance
     * quietly reports the wrong quantity.
     */
    data class SetAccount(val account: AccountSpec?) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "setAccount")
            put("payload", JSONObject().apply {
                if (account != null) put("account", account.toJson())
            })
        }.toString()
    }

    // ── Studies / Drawings ────────────────────────────────────────────────────

    /**
     * Adds a study overlay or oscillator by short name (e.g. `"SMA"`, `"RSI"`).
     * @param params Optional key-value parameters (e.g. `mapOf("period" to 20)`).
     */
    data class AddIndicator(
        val name: String,
        val params: Map<String, Any>? = null,
    ) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "addIndicator")
            put("payload", JSONObject().apply {
                put("shortName", name)
                params?.let { put("params", JSONObject(it)) }
            })
        }.toString()
    }

    /** Removes a study by name. */
    data class RemoveIndicator(val name: String) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "removeIndicator")
            put("payload", JSONObject().apply {
                put("name", name)
            })
        }.toString()
    }

    /** Activates a drawing tool by ID, or passes `null` to deactivate. */
    data class SetDrawingTool(val tool: String?) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "setDrawingTool")
            put("payload", JSONObject().apply {
                if (tool != null) put("tool", tool) else put("tool", JSONObject.NULL)
            })
        }.toString()
    }

    /** Removes all drawings from the chart. */
    object ClearAllDrawings : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "clearAllDrawings")
            put("payload", JSONObject())
        }.toString()
    }

    // ── State ─────────────────────────────────────────────────────────────────

    /** Requests the current chart state; fires a `stateSnapshot` event in response. */
    /**
     * Asks the chart to send the native-UI catalog again ([BridgeEvent.Catalog]). The chart
     * already sends it once after every init; use this to refresh a cached copy. Works before init.
     */
    object GetCatalog : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "getCatalog")
            put("payload", JSONObject())
        }.toString()
    }

    object GetState : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "getState")
            put("payload", JSONObject())
        }.toString()
    }

    /**
     * Restores a previously captured chart state.
     * @param stateJson Raw JSON string returned by a prior `stateSnapshot` event.
     */
    data class SetState(val stateJson: String) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "setState")
            put("payload", JSONObject(stateJson))
        }.toString()
    }

    /**
     * Resolves a pending dataLoader request with fetched bars.
     * @param requestId The ID received in the `dataRequest` bridge event.
     * @param bars The fetched OHLCV bars to return to the chart engine.
     */
    data class ResolveDataRequest(
        val requestId: String,
        val bars: List<OHLCVBar>,
    ) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "resolveDataRequest")
            put("payload", JSONObject().apply {
                put("requestId", requestId)
                put("bars", JSONArray().also { arr ->
                    bars.forEach { bar ->
                        arr.put(JSONObject().apply {
                            put("open", bar.open)
                            put("high", bar.high)
                            put("low", bar.low)
                            put("close", bar.close)
                            put("volume", bar.volume)
                            put("time", bar.time)
                        })
                    }
                })
            })
        }.toString()
    }

    /** Enables or disables verbose tick/render logging in the chart engine. */
    data class SetDebug(val enabled: Boolean) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "setDebug")
            put("payload", JSONObject().apply {
                put("enabled", enabled)
            })
        }.toString()
    }

    /** Destroys the chart engine and releases resources. */
    object Destroy : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "destroy")
            put("payload", JSONObject())
        }.toString()
    }

    // ── Trade levels ──────────────────────────────────────────────────────────

    /**
     * Replaces all levels of the given [type] with [levels].
     * Each map must contain at least [labelKey] and [priceKey] entries.
     * Optional fields per entry: `side`, `stopLossPrice`, `takeProfitPrice`,
     * `pnl`, `pnlText`, `text`, `lots`, `orderType`, `entryPriceEditable`.
     * @param type `"position"`, `"pending"`, or `"trade"`.
     */
    data class SetLevels(
        val levels: List<Map<String, Any>>,
        val labelKey: String,
        val priceKey: String,
        val type: String,
        val pnlKey: String? = null,
        val pnlTextKey: String? = null,
    ) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "setLevels")
            put("payload", JSONObject().apply {
                put("levels", JSONArray().also { arr ->
                    levels.forEach { arr.put(JSONObject(it)) }
                })
                put("labelKey", labelKey)
                put("priceKey", priceKey)
                put("type", type)
                pnlKey?.let { put("pnlKey", it) }
                pnlTextKey?.let { put("pnlTextKey", it) }
            })
        }.toString()
    }

    /** Removes a single level by its label. No-op if not found. */
    data class RemoveLevelByLabel(val label: String) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "removeLevelByLabel")
            put("payload", JSONObject().apply { put("label", label) })
        }.toString()
    }

    /** Updates the entry price of an existing level. */
    /**
     * Mirrors a quantity change made in the host's own modify panel onto the
     * level's pill. Staged like a chart-side qty edit, so it survives `setLevels`
     * refreshes and is reverted by `cancelCurrentEdit`.
     */
    data class UpdateLevelQty(val label: String, val qty: Double) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "updateLevelQty")
            put("payload", JSONObject().apply {
                put("label", label)
                put("qty", qty)
            })
        }.toString()
    }

    data class UpdateLevelMainPrice(val label: String, val price: Double) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "updateLevelMainPrice")
            put("payload", JSONObject().apply {
                put("label", label)
                put("price", price)
            })
        }.toString()
    }

    /**
     * Updates or removes a SL/TP bracket on an existing level.
     * @param bracketType `"sl"` or `"tp"`.
     * @param price Pass `null` to remove the bracket.
     */
    data class UpdateLevelBracket(
        val label: String,
        val bracketType: String,
        val price: Double?,
    ) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "updateLevelBracket")
            put("payload", JSONObject().apply {
                put("label", label)
                put("bracketType", bracketType.lowercase())
                put("price", if (price != null) price else JSONObject.NULL)
            })
        }.toString()
    }

    /**
     * Adds a SL or TP bracket to an existing level at an auto-computed default price.
     * Listen for [BridgeEvent.TradeLevelBracketActivated] to receive the chosen price.
     * @param bracketType `"sl"` or `"tp"`.
     */
    data class AddLevelBracket(
        val label: String,
        val bracketType: String,
    ) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "addLevelBracket")
            put("payload", JSONObject().apply {
                put("label", label)
                put("bracketType", bracketType.lowercase())
            })
        }.toString()
    }

    /**
     * Unified bracket placement — works for both existing levels and the active draft order.
     * Pass [label] (OrderID/TradeID) for an existing level; omit it (null) for the active draft order.
     * Fires [BridgeEvent.TradeLevelBracketActivated] with the auto-computed price.
     * The event's label is null when the bracket was placed on a draft order.
     * @param bracketType `"sl"` or `"tp"`.
     */
    data class AddBracket(
        val bracketType: String,
        val label: String? = null,
    ) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "addBracket")
            put("payload", JSONObject().apply {
                put("bracketType", bracketType.lowercase())
                if (label != null) put("label", label)
            })
        }.toString()
    }

    /**
     * Unified bracket removal — works for both existing levels and the active draft order.
     * Pass [label] (OrderID/TradeID) for an existing level; omit it (null) for the active draft order.
     * @param bracketType `"sl"` or `"tp"`.
     */
    data class RemoveBracket(
        val bracketType: String,
        val label: String? = null,
    ) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "removeBracket")
            put("payload", JSONObject().apply {
                put("bracketType", bracketType.lowercase())
                if (label != null) put("label", label)
            })
        }.toString()
    }

    /** Cancels an in-progress level edit, reverting to the last confirmed price. */
    data class CancelLevelEdit(val label: String) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "cancelLevelEdit")
            put("payload", JSONObject().apply { put("label", label) })
        }.toString()
    }

    /** Programmatically selects a level, or deselects all when [label] is null. */
    data class SelectLevel(val label: String?) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "selectLevel")
            put("payload", JSONObject().apply {
                put("label", if (label != null) label else JSONObject.NULL)
            })
        }.toString()
    }

    // ── Draft orders ──────────────────────────────────────────────────────────

    /**
     * Shows a draggable limit or stop draft order line on the chart.
     * While the user drags it, `tradeLevelDrag` events fire; confirming emits `tradeLevelConfirmed`.
     * @param side `"buy"` or `"sell"`.
     * @param orderType `"limit"` or `"stop"`.
     */
    data class ShowDraftOrder(
        val price: Double,
        val side: String,
        val orderType: String,
    ) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "showDraftOrder")
            put("payload", JSONObject().apply {
                put("price", price)
                put("side", side)
                put("orderType", orderType)
            })
        }.toString()
    }

    /**
     * Shows a non-draggable market-order preview line.
     * SL/TP brackets can be attached via [UpdateDraftOrderBracket].
     * @param side `"buy"` or `"sell"`.
     */
    data class ShowMarketDraft(val price: Double, val side: String) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "showMarketDraft")
            put("payload", JSONObject().apply {
                put("price", price)
                put("side", side)
            })
        }.toString()
    }

    /** Removes any active draft order from the chart. */
    object ClearDraftOrder : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "clearDraftOrder")
            put("payload", JSONObject())
        }.toString()
    }

    /** Cancels whatever is currently being edited or drafted on the chart (draft order or level edit). */
    object CancelCurrentEdit : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "cancelCurrentEdit")
            put("payload", JSONObject())
        }.toString()
    }

    /** Updates the lot quantity shown on the active draft order chip. */
    data class SetDraftOrderLots(val lots: Double) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "setDraftOrderLots")
            put("payload", JSONObject().apply { put("lots", lots) })
        }.toString()
    }

    /** Moves the draft order price line to a new price. */
    data class UpdateDraftOrderPrice(val price: Double) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "updateDraftOrderPrice")
            put("payload", JSONObject().apply { put("price", price) })
        }.toString()
    }

    /**
     * Updates or removes a SL/TP bracket on the active draft order.
     * @param bracketType `"sl"` or `"tp"`.
     * @param price Pass `null` to remove the bracket.
     */
    data class UpdateDraftOrderBracket(val bracketType: String, val price: Double?) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "updateDraftOrderBracket")
            put("payload", JSONObject().apply {
                put("bracketType", bracketType.lowercase())
                put("price", if (price != null) price else JSONObject.NULL)
            })
        }.toString()
    }

    /**
     * Sets or clears the estimated PNL text shown on a draft order's SL or TP bracket line.
     * Pass `null` for [pnlText] to clear.
     * @param bracketType `"sl"` or `"tp"`.
     */
    data class SetDraftBracketPnl(val bracketType: String, val pnlText: String?) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "setDraftBracketPnl")
            put("payload", JSONObject().apply {
                put("bracketType", bracketType.lowercase())
                put("pnlText", if (pnlText != null) pnlText else JSONObject.NULL)
            })
        }.toString()
    }

    // ── UI controls ───────────────────────────────────────────────────────────

    /** Shows or hides the volume sub-pane. */
    data class SetVolume(val show: Boolean) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "setVolume")
            put("payload", JSONObject().apply { put("show", show) })
        }.toString()
    }

    /** Toggles TFC (Trade from Charts) on or off at runtime. */
    data class SetTfcActive(val enabled: Boolean) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "setTfcActive")
            put("payload", JSONObject().apply { put("enabled", enabled) })
        }.toString()
    }

    /** Updates the symbol list used by the ISIN picker modal after initial setup. */
    data class SetIsins(val isins: List<String>) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "setIsins")
            put("payload", JSONObject().apply { put("isins", JSONArray(isins)) })
        }.toString()
    }

    /** Resets both price and time axes to their default auto-fit state. */
    object ResetView : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "resetView")
            put("payload", JSONObject())
        }.toString()
    }

    /**
     * Dismisses any open flyouts, modals, dropdowns, or popovers in the chart UI.
     * Intended for wiring the Android hardware back button.
     *
     * Prefer calling [ActtraderChartsView.dismissAllUI], which short-circuits the
     * WebView round-trip when nothing is open and returns a boolean so you can
     * decide whether to consume the back event.
     */
    object DismissAllUI : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "dismissAllUI")
            put("payload", JSONObject())
        }.toString()
    }

    /**
     * Completely resets the chart to a blank state — clears all bars, the live
     * price line, and any in-flight fetch.  Call this before switching to a new
     * symbol so that no previous symbol data bleeds into the new chart.
     */
    object ResetData : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "resetData")
            put("payload", JSONObject())
        }.toString()
    }

    /** Shows or hides the loading overlay. */
    data class SetLoading(val loading: Boolean) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "setLoading")
            put("payload", JSONObject().apply { put("loading", loading) })
        }.toString()
    }

    /**
     * Updates per-theme deep-partial color overrides and rebuilds the active theme.
     * @param overridesJson Raw JSON string, e.g. `{"dark":{"background":"#111"},"light":{"background":"#fff"}}`.
     */
    data class SetThemeOverrides(val overridesJson: String) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "setThemeOverrides")
            put("payload", JSONObject().apply {
                putJson("overrides", overridesJson)
            })
        }.toString()
    }

    /**
     * Recolours the **canvas only** at runtime — the plot and its axes — and leaves the
     * chrome (header, bottom bar, drawing toolbar, dialogs, popovers) on the theme from
     * [SetThemeOverrides]. Same per-theme picks as the in-chart Chart Settings dialog.
     * @param colorsJson Raw JSON string, e.g. `{"dark":{"background":"#ff00ff"},"light":{"background":"#fff"}}`;
     *                   `null` clears the picks.
     */
    data class SetCanvasColors(val colorsJson: String?) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "setCanvasColors")
            put("payload", JSONObject().apply {
                if (colorsJson == null) put("colors", JSONObject.NULL) else putJson("colors", colorsJson)
            })
        }.toString()
    }

    /**
     * Replaces a specific bar with authoritative OHLCV data (e.g. a correction from the server).
     * @param barTime Unix millisecond timestamp of the bar to replace.
     */
    data class CorrectBar(val barTime: Long, val bar: OHLCVBar) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "correctBar")
            put("payload", JSONObject().apply {
                put("barTime", barTime)
                put("bar", JSONObject().apply {
                    put("open", bar.open)
                    put("high", bar.high)
                    put("low", bar.low)
                    put("close", bar.close)
                    put("volume", bar.volume)
                    put("time", bar.time)
                })
            })
        }.toString()
    }

    // ── Compare ───────────────────────────────────────────────────────────────

    /**
     * Adds a compare symbol overlay. The chart will fire a
     * [BridgeEvent.CompareDataRequest]; reply via [ResolveCompareDataRequest].
     */
    data class AddCompare(val symbol: String) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "addCompare")
            put("payload", JSONObject().apply { put("symbol", symbol) })
        }.toString()
    }

    /** Removes a compare symbol. No-op when not active. */
    data class RemoveCompare(val symbol: String) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "removeCompare")
            put("payload", JSONObject().apply { put("symbol", symbol) })
        }.toString()
    }

    /** Removes every active compare symbol. */
    object ClearCompares : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "clearCompares")
            put("payload", JSONObject())
        }.toString()
    }

    /**
     * Resolves a pending [BridgeEvent.CompareDataRequest] with fetched bars.
     * @param requestId The ID from the request event.
     * @param bars Historical OHLCV bars covering the requested range.
     */
    data class ResolveCompareDataRequest(
        val requestId: String,
        val bars: List<OHLCVBar>,
    ) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "resolveCompareDataRequest")
            put("payload", JSONObject().apply {
                put("requestId", requestId)
                put("bars", JSONArray().also { arr ->
                    bars.forEach { bar ->
                        arr.put(JSONObject().apply {
                            put("open", bar.open)
                            put("high", bar.high)
                            put("low", bar.low)
                            put("close", bar.close)
                            put("volume", bar.volume)
                            put("time", bar.time)
                        })
                    }
                })
            })
        }.toString()
    }

    // ── Snapshot ──────────────────────────────────────────────────────────────

    /**
     * Captures the chart without the user opening the snapshot popover.
     *
     * Replies with [BridgeEvent.Snapshot] carrying a PNG data URL; decode it and
     * hand it to the platform share sheet or MediaStore. The in-WebView browser
     * download is skipped, since it is blocked or useless inside a WebView.
     * Requires `enableSnapshot` in [Init].
     *
     * @param action Why you are capturing — forwarded back on the event so one
     *   handler can tell a share from a copy. `"download"` or `"copy"`.
     */
    data class RequestSnapshot(val action: String = "download") : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "requestSnapshot")
            put("payload", JSONObject().apply { put("action", action) })
        }.toString()
    }

    // ── Indicator templates ───────────────────────────────────────────────────

    /**
     * Replaces the templates listed in the indicators flyout.
     * @param templatesJson A JSON array of templates, as received on
     *   [BridgeEvent.IndicatorTemplateSaved] and stored by your app.
     */
    data class SetIndicatorTemplates(val templatesJson: String) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "setIndicatorTemplates")
            put("payload", JSONObject().apply { putJsonArray("templates", templatesJson) })
        }.toString()
    }

    /** Saves the chart's current indicators under [name]; emits [BridgeEvent.IndicatorTemplateSaved]. */
    data class CaptureIndicatorTemplate(val name: String) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "captureIndicatorTemplate")
            put("payload", JSONObject().apply { put("name", name) })
        }.toString()
    }

    /**
     * Appends one drawing without disturbing the others — the receiving end of
     * copy-to-all-charts.
     *
     * @param drawingJson The `drawingJson` from a [BridgeEvent.DrawingCreated].
     *   Each chart that receives it gives its copy a fresh id, so dragging the
     *   drawing in one pane leaves the others where they were.
     */
    data class AddDrawing(val drawingJson: String) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "addDrawing")
            put("payload", JSONObject().apply {
                putJson("drawing", drawingJson)
            })
        }.toString()
    }

    /**
     * Chooses what the status line above the chart shows.
     *
     * @param statusLineJson e.g. `{"barChange":true,"volume":false}`. Merges —
     *   fields you leave out keep whatever they are.
     */
    data class SetStatusLineSettings(val statusLineJson: String) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "setStatusLineSettings")
            put("payload", JSONObject().apply { putJson("statusLine", statusLineJson) })
        }.toString()
    }

    /**
     * Price- and time-axis options.
     *
     * @param scalesJson e.g.
     *   `{"highLowLabels":true,"pricePrecision":4,"timezone":"Asia/Tokyo"}`. Merges.
     */
    data class SetScalesSettings(val scalesJson: String) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "setScalesSettings")
            put("payload", JSONObject().apply { putJson("scales", scalesJson) })
        }.toString()
    }

    /**
     * Grid, watermark and crosshair options. Colours stay in [SetCanvasColors].
     *
     * @param canvasJson e.g. `{"gridVertical":false,"watermarkVisible":true}`. Merges.
     */
    data class SetCanvasOptions(val canvasJson: String) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "setCanvasOptions")
            put("payload", JSONObject().apply { putJson("canvas", canvasJson) })
        }.toString()
    }

    /**
     * Whether a bar is "up" against its own open or the previous bar's close.
     *
     * @param source `"open"` or `"previousClose"`.
     */
    data class SetBarColorSource(val source: String) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "setBarColorSource")
            put("payload", JSONObject().apply { put("source", source) })
        }.toString()
    }

    /**
     * Fixes the decimal places for every price the chart writes.
     *
     * @param digits 0–8, or null to infer them from the feed again. Display
     *   only — it does not change pip size, which comes from the instrument.
     */
    data class SetPricePrecision(val digits: Int?) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "setPricePrecision")
            put("payload", JSONObject().apply {
                if (digits == null) put("digits", JSONObject.NULL) else put("digits", digits)
            })
        }.toString()
    }

    /**
     * Switches the price axis between regular, logarithmic and percent.
     *
     * @param mode `"normal"`, `"log"` or `"percent"`. Log maps equal ratios to
     *   equal height; it is unavailable on data that reaches zero or below and
     *   maps linearly there rather than refusing to draw.
     */
    data class SetPriceScaleMode(val mode: String) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "setPriceScaleMode")
            put("payload", JSONObject().apply { put("mode", mode) })
        }.toString()
    }

    /**
     * Turns automatic Y-range fitting on or off. Switching it off freezes
     * whatever is on screen, so the chart does not jump as it stops moving.
     */
    data class SetAutoScale(val enabled: Boolean) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "setAutoScale")
            put("payload", JSONObject().apply { put("enabled", enabled) })
        }.toString()
    }

    /**
     * Scrolls the chart to a date, centring the nearest bar.
     *
     * @param date ISO 8601 (`"2024-03-01"`) or unix ms as a string. Nearest,
     *   not exact: the date asked for is often a weekend or a holiday, and
     *   landing beside it beats not moving.
     */
    data class GoToDate(val date: String) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "goToDate")
            put("payload", JSONObject().apply { put("date", date) })
        }.toString()
    }

    /** Shows or hides the docked Data Window / Objects panel. */
    data class SetSidePanelVisible(val visible: Boolean) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "setSidePanelVisible")
            put("payload", JSONObject().apply { put("visible", visible) })
        }.toString()
    }

    /** Switches the panel. [tab] is `"data"` or `"objects"`. */
    data class SetSidePanelTab(val tab: String) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "setSidePanelTab")
            put("payload", JSONObject().apply { put("tab", tab) })
        }.toString()
    }

    /** Shows or hides one drawing. Hiding the selected one deselects it. */
    data class SetDrawingVisible(val id: String, val visible: Boolean) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "setDrawingVisible")
            put("payload", JSONObject().apply { put("id", id); put("visible", visible) })
        }.toString()
    }

    /** Locks or unlocks one drawing. Locking the selected one deselects it. */
    data class SetDrawingLocked(val id: String, val locked: Boolean) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "setDrawingLocked")
            put("payload", JSONObject().apply { put("id", id); put("locked", locked) })
        }.toString()
    }

    /** Deletes one drawing by id, whether or not it is selected. */
    data class DeleteDrawing(val id: String) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "deleteDrawing")
            put("payload", JSONObject().apply { put("id", id) })
        }.toString()
    }

    /** Selects a drawing by id. Pass null to clear the selection. */
    data class SelectDrawing(val id: String?) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "selectDrawing")
            put("payload", JSONObject().apply {
                if (id == null) put("id", JSONObject.NULL) else put("id", id)
            })
        }.toString()
    }

    /** Replaces the chart's indicators with a template's. [id] accepts the id or the name. */
    data class ApplyIndicatorTemplate(val id: String) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "applyIndicatorTemplate")
            put("payload", JSONObject().apply { put("id", id) })
        }.toString()
    }

    /** Removes a template from the flyout. Also delete it from your own storage. */
    data class DeleteIndicatorTemplate(val id: String) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "deleteIndicatorTemplate")
            put("payload", JSONObject().apply { put("id", id) })
        }.toString()
    }

    // ── Chart-settings templates ──────────────────────────────────────────────

    /** Replaces the templates listed in the Chart Settings dialog. */
    data class SetSettingsTemplates(val templatesJson: String) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "setSettingsTemplates")
            put("payload", JSONObject().apply { putJsonArray("templates", templatesJson) })
        }.toString()
    }

    /** Saves the chart's current settings under [name]; emits [BridgeEvent.SettingsTemplateSaved]. */
    data class CaptureSettingsTemplate(val name: String) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "captureSettingsTemplate")
            put("payload", JSONObject().apply { put("name", name) })
        }.toString()
    }

    /** Applies a saved settings template. [id] accepts the id or the name. */
    data class ApplySettingsTemplate(val id: String) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "applySettingsTemplate")
            put("payload", JSONObject().apply { put("id", id) })
        }.toString()
    }

    /** Removes a settings template. Also delete it from your own storage. */
    data class DeleteSettingsTemplate(val id: String) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "deleteSettingsTemplate")
            put("payload", JSONObject().apply { put("id", id) })
        }.toString()
    }

    /**
     * Applies a settings snapshot straight to this chart — the "Apply to all charts"
     * fan-out, sent once per other pane.
     *
     * @param settingsJson The `settings` object from [BridgeEvent.ChartSettingsApplied].
     */
    data class ApplyChartSettings(val settingsJson: String) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "applyChartSettings")
            put("payload", JSONObject().apply { putJson("settings", settingsJson) })
        }.toString()
    }

    // ── Saved layouts ─────────────────────────────────────────────────────────

    /** Replaces the layouts listed in the layout popover. */
    data class SetSavedLayouts(val layoutsJson: String) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "setSavedLayouts")
            put("payload", JSONObject().apply { putJsonArray("layouts", layoutsJson) })
        }.toString()
    }

    /**
     * Saves the current preset and this chart's state under [name]; emits
     * [BridgeEvent.LayoutSaved].
     *
     * @param paneId Identifies this chart within the layout. Leave at `"main"` for a
     *   single-chart screen; pass a distinct id per pane in a multi-pane grid so each
     *   pane's state is restored into the right place.
     */
    data class CaptureSavedLayout(
        val name: String,
        val paneId: String = "main",
    ) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "captureSavedLayout")
            put("payload", JSONObject().apply {
                put("name", name)
                put("paneId", paneId)
            })
        }.toString()
    }

    /** Restores a saved layout into this chart. [id] accepts the id or the name. */
    data class ApplySavedLayout(
        val id: String,
        val paneId: String = "main",
    ) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "applySavedLayout")
            put("payload", JSONObject().apply {
                put("id", id)
                put("paneId", paneId)
            })
        }.toString()
    }

    /** Removes a saved layout. Also delete it from your own storage. */
    data class DeleteSavedLayout(val id: String) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "deleteSavedLayout")
            put("payload", JSONObject().apply { put("id", id) })
        }.toString()
    }

    /**
     * Selects a grid preset. Emits [BridgeEvent.LayoutChange]; mounting the panes
     * stays your app's job — the chart owns only the picker.
     */
    data class SetLayoutPreset(val presetId: String) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "setLayoutPreset")
            put("payload", JSONObject().apply { put("presetId", presetId) })
        }.toString()
    }

    // ── Quick Search ──────────────────────────────────────────────────────────

    // ── Bar Replay ───────────────────────────────────────────────────────────

    /** Opens the Bar Replay strip and arms "Select bar". Requires `enableReplay` in [Init]. */
    object OpenReplay : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "openReplay")
            put("payload", JSONObject())
        }.toString()
    }

    /** Leaves any running replay and closes the strip. */
    object CloseReplay : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "closeReplay")
            put("payload", JSONObject())
        }.toString()
    }

    /**
     * Starts a replay: bars after the last bar at or before [time] are hidden until
     * revealed. [time] is unix milliseconds.
     */
    data class StartReplay(val time: Long) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "startReplay")
            put("payload", JSONObject().apply { put("time", time) })
        }.toString()
    }

    /** Reveals bars one by one at the replay speed. */
    object PlayReplay : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "playReplay")
            put("payload", JSONObject())
        }.toString()
    }

    /** Pauses playback. */
    object PauseReplay : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "pauseReplay")
            put("payload", JSONObject())
        }.toString()
    }

    /** Reveals the next hidden bar. */
    object ReplayStepForward : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "replayStepForward")
            put("payload", JSONObject())
        }.toString()
    }

    /** Replay speed in bars per second (0.1 – 10). */
    data class SetReplaySpeed(val barsPerSecond: Double) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "setReplaySpeed")
            put("payload", JSONObject().apply { put("barsPerSecond", barsPerSecond) })
        }.toString()
    }

    /** Ends the replay and shows the live chart again; the strip stays open. */
    object ExitReplay : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "exitReplay")
            put("payload", JSONObject())
        }.toString()
    }

    /**
     * Opens the command palette. The usual entry point on Android, where the web's
     * Ctrl/⌘+K and `/` shortcuts don't exist — wire it to a toolbar item.
     * Requires `enableQuickSearch` in [Init].
     */
    object OpenQuickSearch : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "openQuickSearch")
            put("payload", JSONObject())
        }.toString()
    }

    /** Closes the command palette. Useful from a back-press handler. */
    object CloseQuickSearch : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "closeQuickSearch")
            put("payload", JSONObject())
        }.toString()
    }

    // ── Chart types ───────────────────────────────────────────────────────────

    /**
     * Retunes the price-transform chart types — Renko, Line Break, Kagi and
     * Point & Figure.
     *
     * Merged over the current options, so `{"renko":{...}}` leaves Kagi and P&F
     * alone. Takes effect immediately when one of those series is showing.
     *
     * The chart *type* is still chosen with [SetSeries]: `"hlc"`, `"renko"`,
     * `"linebreak"`, `"kagi"` and `"pointfigure"` are simply new values of the
     * same series string.
     *
     * @param optionsJson e.g. `{"renko":{"boxSize":{"kind":"atr","length":20}}}`
     */
    data class SetSeriesOptions(val optionsJson: String) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "setSeriesOptions")
            put("payload", JSONObject().apply { putJson("options", optionsJson) })
        }.toString()
    }

    // ── Cursors ───────────────────────────────────────────────────────────────

    /**
     * Switches pointer behaviour over the plot.
     *
     * Independent of the drawing tool — switching mode never cancels a drawing in
     * progress. Replies with [BridgeEvent.CursorModeChange].
     *
     * @param mode `"cross"`, `"dot"`, `"arrow"`, `"demonstration"` or `"eraser"`.
     */
    data class SetCursorMode(val mode: String) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "setCursorMode")
            put("payload", JSONObject().apply { put("mode", mode) })
        }.toString()
    }

    // ── Drawing toolbar options ───────────────────────────────────────────────

    /** Snaps drawing points to the nearest OHLC of the bar under the cursor. */
    data class SetMagnetMode(val enabled: Boolean) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "setMagnetMode")
            put("payload", JSONObject().apply { put("enabled", enabled) })
        }.toString()
    }

    /** Keeps the active tool armed after each drawing, for placing a series. */
    data class SetKeepDrawingMode(val enabled: Boolean) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "setKeepDrawingMode")
            put("payload", JSONObject().apply { put("enabled", enabled) })
        }.toString()
    }

    /** Announces new drawings via [BridgeEvent.DrawingCreated] for layout-wide replication. */
    data class SetCopyDrawingsToAllCharts(val enabled: Boolean) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "setCopyDrawingsToAllCharts")
            put("payload", JSONObject().apply { put("enabled", enabled) })
        }.toString()
    }

    /** Shows or hides the drawing toolbar at runtime. */
    data class SetDrawingToolbarVisible(val visible: Boolean) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "setDrawingToolbarVisible")
            put("payload", JSONObject().apply { put("visible", visible) })
        }.toString()
    }
}
