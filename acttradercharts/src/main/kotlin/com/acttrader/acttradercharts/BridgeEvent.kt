package com.acttrader.acttradercharts

import org.json.JSONObject

/**
 * Events emitted from the chart WebView back to native Android code.
 */
sealed class BridgeEvent {

    /** Chart engine is initialised and ready to receive commands. */
    object Ready : BridgeEvent()

    /** Crosshair moved; contains the bar data at the cursor position. */
    data class Crosshair(
        val time: Long,
        val open: Double,
        val high: Double,
        val low: Double,
        val close: Double,
        val volume: Double,
        val x: Double,
        val y: Double,
    ) : BridgeEvent()

    /** User tapped/clicked a bar. */
    data class BarClick(
        val time: Long,
        val open: Double,
        val high: Double,
        val low: Double,
        val close: Double,
        val volume: Double,
    ) : BridgeEvent()

    /** Viewport scroll or zoom changed. */
    data class ViewportChange(
        val startIndex: Int,
        val endIndex: Int,
        val barWidth: Double,
    ) : BridgeEvent()

    /** Active chart series type changed. */
    data class SeriesChange(val series: String) : BridgeEvent()

    /** User picked a price source (`"bid"`, `"ask"` or `"ltp"`) from the header
     *  dropdown (`priceSourceSelector`) — persist it host-side if desired. */
    data class PriceSourceChange(val source: String) : BridgeEvent()

    /** Active timeframe changed. */
    data class TimeframeChange(val timeframe: String) : BridgeEvent()

    /** Active duration changed. */
    data class DurationChange(val duration: String) : BridgeEvent()

    /** Any aspect of chart state changed (generic). */
    data class StateChange(val stateJson: String) : BridgeEvent()

    /** Response to a `GetState` command; contains the full serialised state. */
    data class StateSnapshot(val stateJson: String) : BridgeEvent()

    /**
     * The native-UI catalog: everything the app needs to build its own header, chart-type list,
     * drawing-tools sheet, indicators list and chart-settings screen — ids, labels, defaults,
     * indicator settings fields, and `available` per entry (true = works in this chart version).
     * Sent once after every init and on [BridgeCommand.GetCatalog].
     *
     * @param catalogVersion the chart library version the catalog belongs to — cache the JSON
     *                       and rebuild your UI only when this changes.
     * @param catalogJson    the whole catalog as a raw JSON string (store it as-is, parse with
     *                       your own models / Gson / kotlinx.serialization).
     */
    data class Catalog(val catalogVersion: String, val catalogJson: String) : BridgeEvent()

    /** `loadData` command completed successfully. */
    data class DataLoaded(val barCount: Int) : BridgeEvent()

    /** A new bar was appended (live edge shift). */
    data class NewBar(
        val time: Long,
        val open: Double,
        val high: Double,
        val low: Double,
        val close: Double,
        val volume: Double,
    ) : BridgeEvent()

    /** Stream connection status changed. */
    data class StreamStatus(val status: String) : BridgeEvent()

    /** User submitted an order via the floating trade button. */
    data class PlaceOrder(
        val price: Double,
        val side: String,
        val orderType: String,
    ) : BridgeEvent()

    /**
     * Chart engine is requesting data for a time range.
     * Respond by calling [ActtraderChartsView.resolveDataRequest] with the fetched bars.
     *
     * @param requestId Correlation ID — must be passed back to [ActtraderChartsView.resolveDataRequest].
     * @param timeframe Current chart timeframe (e.g. `"1h"`).
     * @param interval  Base interval string passed to the data loader (e.g. `"1hour"`).
     * @param start     Range start in milliseconds since epoch.
     * @param end       Range end in milliseconds since epoch.
     */
    data class DataRequest(
        val requestId: String,
        val timeframe: String,
        val interval: String,
        val start: Long,
        val end: Long,
    ) : BridgeEvent()

    /** User tapped × to close or cancel a trade level or remove a bracket. */
    data class TradeLevelClose(
        val label: String,
        val type: String,
        /** `"CLOSED"`, `"CANCELLED"`, `"REMOVE_SL"`, or `"REMOVE_TP"`. */
        val action: String,
        /** Opaque level data serialised as a raw JSON string. */
        val data: String,
        /** `"SL"` or `"TP"` when the × removes a bracket; `null` otherwise. */
        val bracketType: String?,
        val isFullscreen: Boolean,
    ) : BridgeEvent()

    /** Live drag position — fires on every pointer move while a level or bracket is dragged. */
    data class TradeLevelDrag(
        val label: String,
        val newPrice: Double,
        /** Opaque level data serialised as a raw JSON string. */
        val data: String,
        /** `"stopLoss"` or `"takeProfit"` when a bracket is being dragged; `null` for main level. */
        val bracketType: String?,
        val isFullscreen: Boolean,
    ) : BridgeEvent()

    /** A single change entry within a [TradeLevelEdit] event. */
    data class TradeLevelChange(
        /** `"MAIN"`, `"SL"`, `"TP"`, `"ADD_SL"`, `"ADD_TP"`, `"REMOVE_SL"`, or `"REMOVE_TP"`. */
        val field: String,
        val newPrice: Double,
        /**
         * Opaque change data serialised as a raw JSON string. On the `MAIN` change, the embedded
         * `lots` field is overridden with the newly-edited qty when the user changed it this session.
         */
        val data: String,
        /** Present on the `MAIN` change when the user edited the lot size this session. */
        val newLots: Double?,
        val bracketOrderLabel: String?,
    )

    /** User confirmed edits to a trade level — main price, SL, TP changes batched together. */
    data class TradeLevelEdit(
        val label: String,
        val type: String,
        /**
         * Opaque level data serialised as a raw JSON string. When the user edited the lot size
         * during this session, the embedded `lots` field is overridden with the new value.
         */
        val data: String,
        val isFullscreen: Boolean,
        /** Present when the user changed the lot size via the QTY pill flyout during this edit session. */
        val newLots: Double?,
        val changes: List<TradeLevelChange>,
    ) : BridgeEvent()

    /**
     * Live qty edit via the QTY pill flyout — fires before the level edit is confirmed,
     * so hosts can refresh Estimated PNL on SL/TP brackets in real time instead of
     * waiting for [TradeLevelEdit] at ✓ confirm. [type] is `"draft"` for the in-progress
     * draft order, otherwise the parent level's type. [previousLots] is the qty at
     * edit-session start (useful for revert-aware previews).
     */
    data class TradeLevelQtyChange(
        val label: String,
        val type: String,
        val newLots: Double,
        val previousLots: Double,
        val isFullscreen: Boolean,
    ) : BridgeEvent()

    /** Chart ✓ button confirmed an edit (including draft orders). */
    data class TradeLevelConfirmed(
        val label: String,
        val type: String,
        val isFullscreen: Boolean,
    ) : BridgeEvent()

    /**
     * An in-progress level edit was cancelled from the chart (ESC key or inline ✕ cancel button).
     * Mirrors [TradeLevelConfirmed] for the revert path. Not fired for draft orders
     * (those emit [DraftCancelled]). Hosts listen to reset their external modify-order panel.
     */
    data class TradeLevelEditCancelled(
        val label: String,
        val type: String,
        val isFullscreen: Boolean,
    ) : BridgeEvent()

    /** User tapped the pencil/edit button to open the order panel for a level. */
    data class TradeLevelEditOpen(
        val label: String,
        val type: String,
        /** Opaque level data serialised as a raw JSON string. */
        val data: String,
        val price: Double,
        val side: String?,
        val stopLossPrice: Double?,
        val takeProfitPrice: Double?,
        val isFullscreen: Boolean,
    ) : BridgeEvent()

    /** Emitted after addLevelBracket() / addBracket() auto-places a SL/TP bracket; use the price to populate the form's input. label is an empty string when the bracket was placed on a draft order (no external ID yet) — check label.isEmpty() to detect the draft case. */
    data class TradeLevelBracketActivated(
        val label: String,
        /** `"sl"` or `"tp"`. */
        val bracketType: String,
        val price: Double,
        val isFullscreen: Boolean,
    ) : BridgeEvent()

    /** Emitted when a new draft order is shown on the chart (market, limit, or stop).
     *  Native layer should open the buy/sell form. */
    data class DraftInitiated(
        val side: String,
        val price: Double,
        val orderType: String,
        val isFullscreen: Boolean,
    ) : BridgeEvent()

    /** Emitted when a draft order is cancelled (Escape, ✕ button, or external revert). */
    data class DraftCancelled(
        val label: String,
        val isFullscreen: Boolean,
    ) : BridgeEvent()

    /** TFC (Trade from Charts) was toggled on or off via the top bar button or API. */
    data class TfcToggle(val enabled: Boolean) : BridgeEvent()

    /**
     * The crosshair was switched on or off — via the header switch (`enableCrossHairHeader`)
     * or [BridgeCommand.SetCrosshairEnabled]. Persist [enabled] and seed it back through
     * `crosshairEnabled` on the next init.
     */
    data class CrosshairToggle(val enabled: Boolean) : BridgeEvent()

    /**
     * A horizontal (time-axis) badge drag started — before any movement. Requires
     * `orderLineTimeDrag` and a level flagged `timeDraggable`.
     */
    data class OrderLineMoveStart(
        val label: String,
        /** Unix-ms time anchor the badge started from. */
        val fromTimestamp: Long,
        val fromBarIndex: Int,
        val isFullscreen: Boolean,
    ) : BridgeEvent()

    /** Live position during a horizontal badge drag — fires on every move, before release. */
    data class OrderLineMoving(
        val label: String,
        /** Unix-ms time under the badge right now. */
        val toTimestamp: Long,
        val toBarIndex: Int,
        val isFullscreen: Boolean,
    ) : BridgeEvent()

    /**
     * A horizontal badge drag ended on a different candle (after snapping, when enabled). The
     * price is unchanged — only the level's time anchor moved. [data] is the level's original
     * map serialised as a raw JSON string.
     */
    data class OrderLineMoved(
        val label: String,
        val fromTimestamp: Long,
        val toTimestamp: Long,
        val fromBarIndex: Int,
        val toBarIndex: Int,
        val data: String,
        val isFullscreen: Boolean,
    ) : BridgeEvent()

    /**
     * Emitted whenever any dismissible chart UI (flyout, modal, dropdown, popover) opens or closes.
     * Paired with [BridgeCommand.DismissAllUI], this lets the hosting Activity decide whether
     * the hardware back button should dismiss chart UI or propagate to normal back navigation.
     *
     * [ActtraderChartsView] already listens for this event internally and mirrors the state into
     * [ActtraderChartsView.hasOpenUI], so most hosts won't need to subscribe directly.
     */
    data class UiStateChange(val hasOpenUI: Boolean) : BridgeEvent()

    /** User tapped the symbol name; fires when `onSymbolClick` is enabled in the init command. */
    data class SymbolClick(val symbol: String) : BridgeEvent()

    /** User tapped the "Ask AI" (✦) button in the mobile header; fires when
     *  `onAskAiClick` is enabled in the init command. */
    object AskAiClick : BridgeEvent()

    /**
     * User picked a layout preset or toggled a cross-pane sync option in the
     * chart-owned LayoutPopover. Fires only when `enableMultipleLayouts` is set
     * in [BridgeCommand.Init]. The native host should mount / teardown panes to
     * match [presetId] (one of `"1"`, `"2-h"`, `"4-2x2"`, etc. — see the JS
     * library's `LAYOUT_PRESETS` for the full list) and apply the sync flags.
     *
     * @param syncJson Raw JSON of the `LayoutSyncState` object: `{"symbol":bool,"interval":bool,"crosshair":bool,"time":bool,"dateRange":bool}`.
     */
    data class LayoutChange(
        val presetId: String,
        val syncJson: String,
    ) : BridgeEvent()

    /**
     * User picked Download or Copy from the chart-owned SnapshotPopover. Fires
     * only when `enableSnapshot` is set in [BridgeCommand.Init]. The chart will
     * still attempt the native browser action; native hosts can save the PNG
     * via platform APIs (MediaStore, ClipboardManager) using [dataUrl].
     *
     * @param action `"download"` or `"copy"`.
     */
    data class Snapshot(
        val dataUrl: String,
        val action: String,
    ) : BridgeEvent()

    /**
     * Chart engine is requesting bars for a compare symbol. Respond by calling
     * [ActtraderChartsView.resolveCompareDataRequest] with the fetched bars.
     *
     * @param requestId Correlation ID — must be passed back to the resolver.
     * @param symbol    The compare symbol being added or refetched.
     * @param timeframe Current primary timeframe (e.g. `"1D"`).
     * @param interval  Base interval string passed to the loader (e.g. `"1day"`).
     * @param start     Range start in milliseconds since epoch.
     * @param end       Range end in milliseconds since epoch.
     */
    data class CompareDataRequest(
        val requestId: String,
        val symbol: String,
        val timeframe: String,
        val interval: String,
        val start: Long,
        val end: Long,
    ) : BridgeEvent()

    /** A compare symbol was added and assigned an auto-picked palette color. */
    data class CompareAdded(val symbol: String, val color: String) : BridgeEvent()

    /** A compare symbol was removed (×, programmatic remove, or clearCompares). */
    data class CompareRemoved(val symbol: String) : BridgeEvent()

    /** Adding a compare or fetching its bars failed. */
    data class CompareError(val symbol: String, val message: String) : BridgeEvent()

    /**
     * A study instance was added. Multiple instances of the same study can be
     * active at once (e.g. EMA-20, EMA-50). Keep [instanceId] to later remove
     * this specific instance via [ActtraderChartsView.removeIndicator].
     *
     * @param instanceId Unique per-instance id (e.g. `"EMA#3"`).
     * @param shortName  Study short name (e.g. `"EMA"`).
     * @param params     Resolved params (period, color, source, timeframe, …).
     */
    data class IndicatorAdded(
        val instanceId: String,
        val shortName: String,
        val params: Map<String, Any>,
    ) : BridgeEvent()

    /** A study instance was removed (pill ×, settings dialog, or removeIndicator). */
    data class IndicatorRemoved(
        val instanceId: String,
        val shortName: String,
    ) : BridgeEvent()

    // ── Layouts ───────────────────────────────────────────────────────────────

    /**
     * A workspace was saved as a named layout. **Persist [layoutJson]** — the chart
     * holds it only for the lifetime of the view.
     */
    data class LayoutSaved(val id: String, val name: String, val layoutJson: String) : BridgeEvent()

    /** A saved layout was restored. [layoutJson] carries every pane, for lazy mounting. */
    data class LayoutApplied(
        val id: String,
        val name: String,
        val presetId: String,
        val layoutJson: String,
    ) : BridgeEvent()

    /** A saved layout was deleted. Remove it from your storage too. */
    data class LayoutDeleted(val id: String) : BridgeEvent()

    // ── Indicator templates ───────────────────────────────────────────────────

    /**
     * An indicator set was saved as a named template. **Persist [templateJson]** —
     * the chart holds it only for the lifetime of the view.
     */
    data class IndicatorTemplateSaved(
        val id: String,
        val name: String,
        val templateJson: String,
    ) : BridgeEvent()

    /** A template's indicators replaced the chart's active set. */
    data class IndicatorTemplateApplied(
        val id: String,
        val name: String,
        val count: Int,
    ) : BridgeEvent()

    /** An indicator template was deleted. Remove it from your storage too. */
    data class IndicatorTemplateDeleted(val id: String) : BridgeEvent()

    // ── Chart-settings templates ──────────────────────────────────────────────

    /** A settings template was saved. **Persist [templateJson].** */
    data class SettingsTemplateSaved(
        val id: String,
        val name: String,
        val templateJson: String,
    ) : BridgeEvent()

    /** A settings template was applied to this chart. */
    data class SettingsTemplateApplied(val id: String, val name: String) : BridgeEvent()

    /** A settings template was deleted. Remove it from your storage too. */
    data class SettingsTemplateDeleted(val id: String) : BridgeEvent()

    /**
     * Settings were applied from the Chart Settings dialog.
     *
     * This chart has already applied them. When [applyToAll] is true the user asked
     * for every chart — send [settingsJson] to each of your other panes with
     * [BridgeCommand.ApplyChartSettings].
     */
    data class ChartSettingsApplied(
        val settingsJson: String,
        val applyToAll: Boolean,
    ) : BridgeEvent()

    // ── Quick Search ──────────────────────────────────────────────────────────

    /**
     * A Quick Search command ran. It has already executed — use this for analytics,
     * or to mirror the action into your own chrome.
     */
    data class QuickSearchCommand(
        val id: String,
        val label: String,
        val group: String,
    ) : BridgeEvent()

    // ── Cursors ───────────────────────────────────────────────────────────────

    /**
     * The pointer mode changed — via [BridgeCommand.SetCursorMode] or the Cursors
     * group in the drawing toolbar. Persist it to restore the user's choice.
     */
    data class CursorModeChange(val mode: String) : BridgeEvent()

    // ── Drawing toolbar options ───────────────────────────────────────────────

    /** Magnet mode toggled from the toolbar. Persist it to restore the choice. */
    data class MagnetModeChange(val enabled: Boolean) : BridgeEvent()

    /** Keep-drawing mode toggled from the toolbar. */
    data class KeepDrawingModeChange(val enabled: Boolean) : BridgeEvent()

    /** "Copy to all charts" toggled from the toolbar. */
    data class CopyDrawingsToAllChange(val enabled: Boolean) : BridgeEvent()

    /** The drawing toolbar was shown or hidden. */
    data class DrawingToolbarVisibility(val visible: Boolean) : BridgeEvent()

    /**
     * A drawing was completed.
     *
     * When [copyToAll] is true the user asked for new drawings to appear on every
     * chart in the layout — send [drawingJson] to your other chart views. The
     * chart cannot do it itself; only your app knows which panes exist.
     */
    data class DrawingCreated(
        val type: String,
        val drawingJson: String,
        val copyToAll: Boolean,
    ) : BridgeEvent()

    /** Decimal places for prices changed, whether pinned or re-inferred. */
    data class PricePrecisionChange(val digits: Int) : BridgeEvent()

    /** Bar colouring switched. [source] is `"open"` or `"previousClose"`. */
    data class BarColorSourceChange(val source: String) : BridgeEvent()

    /** The display timezone changed. Always a resolved IANA name, never `"local"`. */
    data class TimezoneChange(val timezone: String) : BridgeEvent()

    /** A status-line field was shown or hidden. [statusLineJson] carries the whole set. */
    data class StatusLineChange(val statusLineJson: String) : BridgeEvent()

    /** A scales option changed. [scalesJson] carries the whole set. */
    data class ScalesChange(val scalesJson: String) : BridgeEvent()

    /** A canvas option changed. [canvasJson] carries the whole set. */
    data class CanvasOptionsChange(val canvasJson: String) : BridgeEvent()

    /** The price axis switched. [mode] is `"normal"`, `"log"` or `"percent"`. */
    data class PriceScaleModeChange(val mode: String) : BridgeEvent()

    /** Automatic Y-range fitting was turned on or off. */
    data class AutoScaleChange(val enabled: Boolean) : BridgeEvent()

    /** The chart scrolled to a date. Carries the bar it settled on. */
    data class GoToDate(val time: Long, val barIndex: Int) : BridgeEvent()

    /** Bar Replay started (or restarted) at the bar with this open time (unix ms). */
    data class ReplayStart(val time: Long) : BridgeEvent()

    /** Bar Replay revealed the next bar. */
    data class ReplayStep(val time: Long) : BridgeEvent()

    /** Bar Replay revealed the last hidden bar. */
    data class ReplayEnd(val time: Long) : BridgeEvent()

    /** Bar Replay ended and the live chart is back. */
    object ReplayExit : BridgeEvent()

    /**
     * Two-finger measure (mobile): the user is holding two fingers on the chart.
     * [phase] is `"start"`, `"update"` or `"end"`; times are unix ms; [change] is
     * close-to-close, [changePercent] relative to the start close.
     */
    data class TwoFingerMeasure(
        val phase: String,
        val startTime: Long,
        val endTime: Long,
        val startClose: Double,
        val endClose: Double,
        val change: Double,
        val changePercent: Double,
        val bars: Int,
    ) : BridgeEvent()

    /** The side panel was shown or hidden. [tab] is `"data"` or `"objects"`. */
    data class SidePanelVisibility(val visible: Boolean, val tab: String) : BridgeEvent()

    /** An error occurred inside the chart engine. */
    data class Error(val message: String, val code: String? = null) : BridgeEvent()
}

/** Parses a raw JSON string from the WebView into a [BridgeEvent].
 *
 *  The bridge sends `{ "type": "...", "payload": { ... } }`.  All fields
 *  are read from the nested `payload` object.
 */
object BridgeEventParser {

    fun parse(json: String): BridgeEvent? = runCatching {
        val obj = JSONObject(json)
        val type = obj.getString("type")

        // Every event except "ready" carries a payload object.
        val p: JSONObject = obj.optJSONObject("payload") ?: JSONObject()

        when (type) {
            "ready" -> BridgeEvent.Ready

            "crosshair" -> {
                val bar = p.getJSONObject("bar")
                val pos = p.optJSONObject("position")
                BridgeEvent.Crosshair(
                    time = bar.getLong("time"),
                    open = bar.getDouble("open"),
                    high = bar.getDouble("high"),
                    low = bar.getDouble("low"),
                    close = bar.getDouble("close"),
                    volume = bar.getDouble("volume"),
                    x = pos?.optDouble("x") ?: 0.0,
                    y = pos?.optDouble("y") ?: 0.0,
                )
            }

            "barClick" -> {
                val bar = p.getJSONObject("bar")
                BridgeEvent.BarClick(
                    time = bar.getLong("time"),
                    open = bar.getDouble("open"),
                    high = bar.getDouble("high"),
                    low = bar.getDouble("low"),
                    close = bar.getDouble("close"),
                    volume = bar.getDouble("volume"),
                )
            }

            "viewportChange" -> {
                val vp = p.getJSONObject("viewport")
                BridgeEvent.ViewportChange(
                    startIndex = vp.getInt("startIndex"),
                    endIndex = vp.getInt("endIndex"),
                    barWidth = vp.getDouble("barWidth"),
                )
            }

            "seriesChange" -> BridgeEvent.SeriesChange(p.getString("series"))

            "priceSourceChange" -> BridgeEvent.PriceSourceChange(p.getString("source"))

            "timeframeChange" -> BridgeEvent.TimeframeChange(p.getString("timeframe"))

            "durationChange" -> BridgeEvent.DurationChange(p.getString("duration"))

            "stateChange" -> BridgeEvent.StateChange(p.getJSONObject("state").toString())

            "stateSnapshot" -> BridgeEvent.StateSnapshot(p.toString())

            "catalog" -> BridgeEvent.Catalog(
                catalogVersion = p.optString("catalogVersion", ""),
                catalogJson    = p.toString(),
            )

            "dataLoaded" -> BridgeEvent.DataLoaded(p.optInt("barCount", 0))

            "newBar" -> {
                val bar = p.getJSONObject("completedBar")
                BridgeEvent.NewBar(
                    time = bar.getLong("time"),
                    open = bar.getDouble("open"),
                    high = bar.getDouble("high"),
                    low = bar.getDouble("low"),
                    close = bar.getDouble("close"),
                    volume = bar.getDouble("volume"),
                )
            }

            "streamStatus" -> BridgeEvent.StreamStatus(p.getString("status"))

            "placeOrder" -> BridgeEvent.PlaceOrder(
                price = p.getDouble("price"),
                side = p.getString("side"),
                orderType = p.optString("orderType", "limit"),
            )

            "dataRequest" -> BridgeEvent.DataRequest(
                requestId = p.getString("requestId"),
                timeframe = p.getString("timeframe"),
                interval  = p.getString("interval"),
                start     = p.getLong("start"),
                end       = p.getLong("end"),
            )

            "tradeLevelClose" -> BridgeEvent.TradeLevelClose(
                label        = p.getString("label"),
                type         = p.getString("type"),
                action       = p.getString("action"),
                data         = p.optJSONObject("data")?.toString() ?: p.optString("data", "{}"),
                bracketType  = p.optString("bracketType").takeIf { it.isNotEmpty() },
                isFullscreen = p.optBoolean("isFullscreen", false),
            )

            "tradeLevelDrag" -> BridgeEvent.TradeLevelDrag(
                label        = p.getString("label"),
                newPrice     = p.getDouble("newPrice"),
                data         = p.optJSONObject("data")?.toString() ?: p.optString("data", "{}"),
                bracketType  = p.optString("bracketType").takeIf { it.isNotEmpty() },
                isFullscreen = p.optBoolean("isFullscreen", false),
            )

            "tradeLevelEdit" -> {
                val rawChanges = p.optJSONArray("changes")
                val changes = if (rawChanges != null) {
                    (0 until rawChanges.length()).map { i ->
                        val c = rawChanges.getJSONObject(i)
                        BridgeEvent.TradeLevelChange(
                            field             = c.getString("field"),
                            newPrice          = c.getDouble("newPrice"),
                            data              = c.optJSONObject("data")?.toString() ?: c.optString("data", "{}"),
                            newLots           = if (c.has("newLots") && !c.isNull("newLots")) c.getDouble("newLots") else null,
                            bracketOrderLabel = c.optString("bracketOrderLabel").takeIf { it.isNotEmpty() },
                        )
                    }
                } else emptyList()
                BridgeEvent.TradeLevelEdit(
                    label        = p.getString("label"),
                    type         = p.getString("type"),
                    data         = p.optJSONObject("data")?.toString() ?: p.optString("data", "{}"),
                    isFullscreen = p.optBoolean("isFullscreen", false),
                    newLots      = if (p.has("newLots") && !p.isNull("newLots")) p.getDouble("newLots") else null,
                    changes      = changes,
                )
            }

            "tradeLevelQtyChange" -> BridgeEvent.TradeLevelQtyChange(
                label        = p.getString("label"),
                type         = p.getString("type"),
                newLots      = p.getDouble("newLots"),
                previousLots = p.getDouble("previousLots"),
                isFullscreen = p.optBoolean("isFullscreen", false),
            )

            "tradeLevelConfirmed" -> BridgeEvent.TradeLevelConfirmed(
                label        = p.getString("label"),
                type         = p.getString("type"),
                isFullscreen = p.optBoolean("isFullscreen", false),
            )

            "tradeLevelEditCancelled" -> BridgeEvent.TradeLevelEditCancelled(
                label        = p.getString("label"),
                type         = p.getString("type"),
                isFullscreen = p.optBoolean("isFullscreen", false),
            )

            "tradeLevelEditOpen" -> BridgeEvent.TradeLevelEditOpen(
                label           = p.getString("label"),
                type            = p.getString("type"),
                data            = p.optJSONObject("data")?.toString() ?: p.optString("data", "{}"),
                price           = p.getDouble("price"),
                side            = p.optString("side").takeIf { it.isNotEmpty() },
                stopLossPrice   = if (p.has("stopLossPrice"))   p.getDouble("stopLossPrice")   else null,
                takeProfitPrice = if (p.has("takeProfitPrice")) p.getDouble("takeProfitPrice") else null,
                isFullscreen    = p.optBoolean("isFullscreen", false),
            )

            "tradeLevelBracketActivated" -> BridgeEvent.TradeLevelBracketActivated(
                label        = p.optString("label", ""),
                bracketType  = p.getString("bracketType"),
                price        = p.getDouble("price"),
                isFullscreen = p.optBoolean("isFullscreen", false),
            )

            "draftInitiated" -> BridgeEvent.DraftInitiated(
                side         = p.getString("side"),
                price        = p.getDouble("price"),
                orderType    = p.getString("orderType"),
                isFullscreen = p.optBoolean("isFullscreen", false),
            )

            "draftCancelled" -> BridgeEvent.DraftCancelled(
                label        = p.getString("label"),
                isFullscreen = p.optBoolean("isFullscreen", false),
            )

            "tfcToggle" -> BridgeEvent.TfcToggle(p.getBoolean("enabled"))

            "crosshairToggle" -> BridgeEvent.CrosshairToggle(p.getBoolean("enabled"))

            "orderLineMoveStart" -> BridgeEvent.OrderLineMoveStart(
                label         = p.getString("label"),
                fromTimestamp = p.getLong("fromTimestamp"),
                fromBarIndex  = p.getInt("fromBarIndex"),
                isFullscreen  = p.optBoolean("isFullscreen", false),
            )

            "orderLineMoving" -> BridgeEvent.OrderLineMoving(
                label        = p.getString("label"),
                toTimestamp  = p.getLong("toTimestamp"),
                toBarIndex   = p.getInt("toBarIndex"),
                isFullscreen = p.optBoolean("isFullscreen", false),
            )

            "orderLineMoved" -> BridgeEvent.OrderLineMoved(
                label         = p.getString("label"),
                fromTimestamp = p.getLong("fromTimestamp"),
                toTimestamp   = p.getLong("toTimestamp"),
                fromBarIndex  = p.getInt("fromBarIndex"),
                toBarIndex    = p.getInt("toBarIndex"),
                data          = p.optJSONObject("data")?.toString() ?: p.optString("data", "{}"),
                isFullscreen  = p.optBoolean("isFullscreen", false),
            )

            "uiStateChange" -> BridgeEvent.UiStateChange(p.optBoolean("hasOpenUI", false))

            "symbolClick" -> BridgeEvent.SymbolClick(p.optString("symbol", ""))

            "askAiClick" -> BridgeEvent.AskAiClick

            "layoutChange" -> BridgeEvent.LayoutChange(
                presetId = p.getString("presetId"),
                syncJson = p.optJSONObject("sync")?.toString() ?: "{}",
            )

            "snapshot" -> BridgeEvent.Snapshot(
                dataUrl = p.getString("dataUrl"),
                action  = p.optString("action", "download"),
            )

            "compareDataRequest" -> BridgeEvent.CompareDataRequest(
                requestId = p.getString("requestId"),
                symbol    = p.getString("symbol"),
                timeframe = p.getString("timeframe"),
                interval  = p.getString("interval"),
                start     = p.getLong("start"),
                end       = p.getLong("end"),
            )

            "compareAdded" -> BridgeEvent.CompareAdded(
                symbol = p.getString("symbol"),
                color  = p.getString("color"),
            )

            "compareRemoved" -> BridgeEvent.CompareRemoved(p.getString("symbol"))

            "compareError" -> BridgeEvent.CompareError(
                symbol  = p.getString("symbol"),
                message = p.optString("message", ""),
            )

            "indicatorAdded" -> {
                val paramsObj = p.optJSONObject("params") ?: JSONObject()
                BridgeEvent.IndicatorAdded(
                    instanceId = p.getString("instanceId"),
                    shortName  = p.getString("shortName"),
                    params     = paramsObj.keys().asSequence().associateWith { paramsObj.get(it) },
                )
            }

            "indicatorRemoved" -> BridgeEvent.IndicatorRemoved(
                instanceId = p.getString("instanceId"),
                shortName  = p.getString("shortName"),
            )

            "layoutSaved" -> {
                val layout = p.getJSONObject("layout")
                BridgeEvent.LayoutSaved(
                    id         = layout.getString("id"),
                    name       = layout.getString("name"),
                    layoutJson = layout.toString(),
                )
            }

            "layoutApplied" -> BridgeEvent.LayoutApplied(
                id         = p.getString("id"),
                name       = p.getString("name"),
                presetId   = p.getString("presetId"),
                layoutJson = (p.optJSONObject("layout") ?: JSONObject()).toString(),
            )

            "layoutDeleted" -> BridgeEvent.LayoutDeleted(id = p.getString("id"))

            "indicatorTemplateSaved" -> {
                val tpl = p.getJSONObject("template")
                BridgeEvent.IndicatorTemplateSaved(
                    id           = tpl.getString("id"),
                    name         = tpl.getString("name"),
                    templateJson = tpl.toString(),
                )
            }

            "indicatorTemplateApplied" -> BridgeEvent.IndicatorTemplateApplied(
                id    = p.getString("id"),
                name  = p.getString("name"),
                count = p.optInt("count", 0),
            )

            "indicatorTemplateDeleted" -> BridgeEvent.IndicatorTemplateDeleted(id = p.getString("id"))

            "settingsTemplateSaved" -> {
                val tpl = p.getJSONObject("template")
                BridgeEvent.SettingsTemplateSaved(
                    id           = tpl.getString("id"),
                    name         = tpl.getString("name"),
                    templateJson = tpl.toString(),
                )
            }

            "settingsTemplateApplied" -> BridgeEvent.SettingsTemplateApplied(
                id   = p.getString("id"),
                name = p.getString("name"),
            )

            "settingsTemplateDeleted" -> BridgeEvent.SettingsTemplateDeleted(id = p.getString("id"))

            "chartSettingsApplied" -> BridgeEvent.ChartSettingsApplied(
                settingsJson = (p.optJSONObject("settings") ?: JSONObject()).toString(),
                applyToAll   = p.optBoolean("applyToAll", false),
            )

            "quickSearchCommand" -> BridgeEvent.QuickSearchCommand(
                id    = p.getString("id"),
                label = p.optString("label"),
                group = p.optString("group"),
            )

            "cursorModeChange" -> BridgeEvent.CursorModeChange(mode = p.getString("mode"))

            "magnetModeChange" -> BridgeEvent.MagnetModeChange(p.getBoolean("enabled"))
            "keepDrawingModeChange" -> BridgeEvent.KeepDrawingModeChange(p.getBoolean("enabled"))
            "copyDrawingsToAllChange" -> BridgeEvent.CopyDrawingsToAllChange(p.getBoolean("enabled"))
            "drawingToolbarVisibility" -> BridgeEvent.DrawingToolbarVisibility(p.getBoolean("visible"))

            "drawingCreated" -> {
                val drawing = p.getJSONObject("drawing")
                BridgeEvent.DrawingCreated(
                    type = drawing.optString("type"),
                    drawingJson = drawing.toString(),
                    copyToAll = p.optBoolean("copyToAll", false),
                )
            }

            "pricePrecisionChange" -> BridgeEvent.PricePrecisionChange(
                digits = p.optInt("digits", 2),
            )

            "barColorSourceChange" -> BridgeEvent.BarColorSourceChange(
                source = p.optString("source", "open"),
            )

            "timezoneChange" -> BridgeEvent.TimezoneChange(
                timezone = p.optString("timezone", "UTC"),
            )

            "statusLineChange" -> BridgeEvent.StatusLineChange(
                statusLineJson = p.optJSONObject("statusLine")?.toString() ?: "{}",
            )

            "scalesChange" -> BridgeEvent.ScalesChange(
                scalesJson = p.optJSONObject("scales")?.toString() ?: "{}",
            )

            "canvasOptionsChange" -> BridgeEvent.CanvasOptionsChange(
                canvasJson = p.optJSONObject("canvas")?.toString() ?: "{}",
            )

            "priceScaleModeChange" -> BridgeEvent.PriceScaleModeChange(
                mode = p.optString("mode", "normal"),
            )

            "autoScaleChange" -> BridgeEvent.AutoScaleChange(
                enabled = p.optBoolean("enabled", true),
            )

            "goToDate" -> BridgeEvent.GoToDate(
                time = p.optLong("time", 0L),
                barIndex = p.optInt("barIndex", 0),
            )

            "replayStart" -> BridgeEvent.ReplayStart(time = p.optLong("time", 0L))
            "replayStep" -> BridgeEvent.ReplayStep(time = p.optLong("time", 0L))
            "replayEnd" -> BridgeEvent.ReplayEnd(time = p.optLong("time", 0L))
            "replayExit" -> BridgeEvent.ReplayExit

            "twoFingerMeasure" -> BridgeEvent.TwoFingerMeasure(
                phase = p.optString("phase", "update"),
                startTime = p.optLong("startTime", 0L),
                endTime = p.optLong("endTime", 0L),
                startClose = p.optDouble("startClose", 0.0),
                endClose = p.optDouble("endClose", 0.0),
                change = p.optDouble("change", 0.0),
                changePercent = p.optDouble("changePercent", 0.0),
                bars = p.optInt("bars", 0),
            )

            "sidePanelVisibility" -> BridgeEvent.SidePanelVisibility(
                visible = p.optBoolean("visible", false),
                tab = p.optString("tab", "data"),
            )

            "error" -> BridgeEvent.Error(
                message = p.optString("message", "Unknown error"),
                code = p.optString("code").takeIf { it.isNotEmpty() },
            )

            else -> null
        }
    }.getOrNull()
}
