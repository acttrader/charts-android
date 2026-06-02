# charts-android

Android Kotlin library that renders [`ActCharts`](https://github.com/acttrader/ActCharts) inside a `WebView`.

## Requirements

- minSdk 24 (Android 7.0)
- Kotlin 1.9+

## Installation

Add GitHub Packages to your project's `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        maven {
            url = uri("https://maven.pkg.github.com/acttrader/charts-android")
        }
    }
}
```

Then add the dependency:

```kotlin
dependencies {
    implementation("com.acttrader:acttrader-charts-android:0.1.0")
}
```

### Beta releases

Pre-release builds are published to the same GitHub Packages repository under prerelease versions (`X.Y.Z-beta.N`). They coexist with stable releases — pin the exact beta version to opt in:

```kotlin
implementation("com.acttrader:acttrader-charts-android:1.1.0-beta.1")
```

If you use Gradle dynamic versions (`1.+`, `latest.release`), prerelease versions **may** be picked up — pin exact versions in production to avoid surprises.

## OHLCVBar

```kotlin
data class OHLCVBar(
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double,
    val time: Long,   // Unix timestamp in milliseconds (UTC)
)
```

## Basic usage

```kotlin
// In your Activity or Fragment
val chart = ActtraderChartsView(context)

chart.onReady = {
    chart.init(theme = "dark", symbol = "AAPL", enableTrading = false)
    chart.loadData(bars)           // List<OHLCVBar>
}

chart.onCrosshair = { event ->
    // event.open, .high, .low, .close, .volume, .time
}

chart.onError = { err ->
    Log.e("Chart", err.message)
}

parentLayout.addView(chart)
```

> **Note:** `init(...)` is how you apply a non-default configuration (theme, timeframe,
> symbol, `themeOverrides`, etc.). If you skip it, the chart auto-initialises with the
> built-in defaults (dark theme, `1D` timeframe) the moment the WebView is ready, so
> runtime methods like `setTheme` and `loadData` always have an engine to talk to.

### XML layout

```xml
<com.acttrader.acttradercharts.ActtraderChartsView
    android:id="@+id/chart"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

## Commands

| Method | Description |
|--------|-------------|
| `init(...)` | Initialise the chart engine — call from `onReady` before `loadData` (see params below) |
| `loadData(bars, fitAll)` | Replace full dataset |
| `pushTick(bid, ask, timestamp)` | Stream a live tick |
| `setTheme("dark" \| "light")` | Switch theme |
| `setTimeframe(timeframe)` | `"1m"` `"5m"` `"15m"` `"30m"` `"1h"` `"4h"` `"1D"` `"1W"` `"1M"` `"1Y"` |
| `setSeries(type)` | Change chart type (`"candlestick"`, `"hollow_candle"`, `"line"`, `"area"`, `"ohlc"`) |
| `setSymbol(symbol)` | Update displayed symbol name |
| `addIndicator(name, params?)` | Add study (e.g. `"SMA"`, `"EMA"`, `"RSI"`, `"BB"`, `"MACD"`). Parameterized studies add a **new instance** per call (multiple EMAs etc.); listen to `onIndicatorAdded` for its `instanceId` |
| `removeIndicator(name)` | Remove a study — pass an `instanceId` (e.g. `"EMA#3"`) for one instance, or a short name (e.g. `"EMA"`) for all instances of that study |
| `setDrawingTool(tool?)` | Activate drawing tool (e.g. `"trend_line"`, `"horizontal_line"`), `null` to deactivate |
| `clearAllDrawings()` | Remove all drawings |
| `getState()` | Request state snapshot — result delivered via `onStateSnapshot` |
| `setState(stateJson)` | Restore a prior state from `onStateSnapshot` JSON |
| `resolveDataRequest(requestId, bars)` | Resolves a pending `onDataRequest` with fetched bars |
| `setDebug(enabled)` | Enable or disable verbose logging in the browser console |
| `destroy()` | Release WebView resources |
| **TFC — Trade Levels** | |
| `setLevels(levels, labelKey, priceKey, type, pnlKey?, pnlTextKey?)` | Replace all levels of a given type; pass empty list to clear |
| `removeLevelByLabel(label)` | Remove a single level by label |
| `updateLevelMainPrice(label, price)` | Update the entry price of an existing level. Stages the edit in the chart's pending-edit buffer so it survives subsequent `setLevels` refreshes (e.g. per-tick PnL updates) until the server echoes the new price or `cancelLevelEdit` / `cancelCurrentEdit` is called. Call `cancelLevelEdit(label)` when your modify panel closes without submitting, otherwise the staged edit keeps overriding server state on the chart |
| `updateLevelBracket(label, bracketType, price?)` | Update or remove a SL/TP bracket on an existing level; pass `null` price to remove. Same staging semantics as `updateLevelMainPrice` |
| `addLevelBracket(label, bracketType)` | Auto-place a SL or TP bracket at a default price offset; fires `onTradeLevelBracketActivated` with the computed price |
| `addBracket(bracketType, label?)` | Unified auto-price bracket placement — pass `label` for an existing order/position, omit it for the active draft order; fires `onTradeLevelBracketActivated` (`label` is `""` for drafts — check `label.isEmpty()`) |
| `removeBracket(bracketType, label?)` | Unified bracket removal — pass `label` for an existing order/position, omit it for the active draft order |
| `cancelLevelEdit(label)` | Cancel an in-progress level edit, reverting to last confirmed price |
| `selectLevel(label?)` | Programmatically highlight a level; pass `null` to deselect all |
| | **Off-viewport indicators:** When a level's entry/SL/TP is outside the visible price range, a `▲ N` / `▼ N` pill appears near the chart's right edge. Tapping the pill smooth-scrolls the nearest off-screen marker to center. This is automatic — no configuration needed. |
| | **Trade level visuals:** Pending orders and ES/EL entry working orders render as **dashed** lines tinted by side (`pendingBuyLine` green / `pendingSellLine` red). True open positions render as **solid** lines — green/red when `pnl` is set, otherwise `positionLine` (purple/indigo). Each true open position shows a colored entry-price tag on the right-side price axis (same style as the Bid/Ask tag). |
| | **Brackets follow entry on drag:** dragging the entry line of a pending order, draft order, or an entry-editable open position translates any existing SL/TP brackets by the same price delta. The distance is whatever the user currently sees; if they manually adjust SL or TP, the new distance anchors subsequent entry drags. Missing brackets are not auto-created. On confirm, `onTradeLevelEdit` carries all translated fields together in one `changes` list; with `hideLevelConfirmCancel = true` the three changes arrive as a single atomic event. |
| | **Bracket pill auto-offset:** when an SL/TP price sits within about one pill-height of the entry price, the bracket's label pill is pushed vertically away from the entry pill and connected back to its real price line by a dashed leader. The horizontal bracket line stays at the true price; only the pill and its `×` button move, and drag/tap targets follow the displaced pill — so the bracket pill and entry pill never share a touch area. Works for both buy and sell orders, automatic (no configuration). |
| **TFC — Draft Orders** | |
| `showDraftOrder(price, side, orderType)` | Show a draggable limit or stop draft order line |
| `showMarketDraft(price, side)` | Show a non-draggable market-order preview line |
| `clearDraftOrder()` | Remove the active draft order |
| `cancelCurrentEdit()` | Cancel whatever is currently being edited or drafted (draft order or level edit); no-op when nothing is active |
| `setDraftOrderLots(lots)` | Update the lot quantity on the active draft order chip |
| `updateDraftOrderPrice(price)` | Move the draft order price line to a new price |
| `updateDraftOrderBracket(bracketType, price?)` | Update or remove a SL/TP bracket on the draft order; pass `null` to remove |
| `setDraftBracketPnl(bracketType, pnlText)` | Display estimated P&L text next to the active bracket host's SL or TP line — a draft order while drafting, or the currently selected existing pending order / position while modifying; pass `null` to clear |
| **UI / Utility** | |
| `setTfcActive(enabled)` | Toggle TFC on/off at runtime (only when `tfcEnabled = true` at init) |
| `setVolume(show)` | Show or hide the volume sub-pane |
| `setIsins(isins)` | Update the symbol list used by the ISIN picker |
| `setMinLots(lots)` | Update the minimum lot size in the trade popover |
| `resetView()` | Reset price and time axes to auto-fit. The built-in bottom-center reset button invokes this — it is hidden while the chart is at its default view and fades in only after the user pans, zooms, or price-scales |
| `resetData()` | Clear all bars, the live price line, any in-flight fetch, **all user drawings, and all trade/position levels** (including pending draft orders). Call before switching to a new symbol to prevent previous symbol state from bleeding in (see example below). For a same-symbol data refresh that should preserve drawings, call `loadData(emptyList())` directly instead |
| `setLoading(loading)` | Show or hide the loading overlay |
| `setTimezone(timezone)` | Change display timezone at runtime — IANA string (`"America/New_York"`) or `"local"` |
| `setThemeOverrides(overrides)` | Update per-theme color overrides at runtime — accepts typed `ThemeOverrides` or raw JSON string |
| `correctBar(barTime, bar)` | Replace a specific bar with authoritative OHLCV data (e.g. server correction) |
| **Compare** | |
| `addCompare(symbol)` | Add a compare symbol overlay. Fires `onCompareDataRequest` — reply via `resolveCompareDataRequest` |
| `removeCompare(symbol)` | Remove a compare symbol. No-op if not active |
| `clearCompares()` | Remove every active compare symbol |
| `resolveCompareDataRequest(requestId, bars)` | Resolve a pending `onCompareDataRequest` with fetched bars |

#### Symbol switch pattern

Always call `resetData()` before loading bars for a new symbol. This prevents
the previous symbol's candles, live price line, drawings, and trade levels
from bleeding into the new chart during the data-fetch window.

```kotlin
chart.setSymbol("GBPUSD")
chart.resetData()
// … fetch new bars for GBPUSD …
chart.loadData(bars)
```

### `init()` parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `theme` | `String` | `"dark"` | `"dark"` or `"light"` |
| `symbol` | `String?` | `null` | Symbol name shown in the top bar (e.g. `"AAPL"`) |
| `series` | `String?` | `null` | Initial chart type (e.g. `"candlestick"`, `"line"`, `"area"`, `"ohlc"`, `"hollow_candle"`) |
| `timeframe` | `String?` | `null` | Initial timeframe (e.g. `"1m"`, `"5m"`, `"1h"`, `"1D"`) |
| `duration` | `String?` | `null` | Initial duration button (e.g. `"1D"`, `"1M"`, `"1Y"`, `"All"`) |
| `showVolume` | `Boolean?` | `null` | Show volume bars |
| `showUI` | `Boolean?` | `null` | Show top / bottom bars. When `false`, the loading overlay is also suppressed |
| `showDrawingTools` | `Boolean?` | `null` | Show drawing toolbar and pencil button |
| `showBidAskLines` | `Boolean?` | `null` | Show bid and ask as dashed lines during a live stream |
| `showActLogo` | `Boolean?` | `null` | Show ACT watermark logo |
| `showCandleCountdown` | `Boolean?` | `null` | Show countdown timer on the live candle (time axis) |
| `candleCountdownTimeframes` | `List<String>?` / `"all"` | `null` | Timeframes where the countdown appears |
| `showPriceAxisCountdown` | `Boolean?` | `null` (`false`) | Show candle countdown on the right price axis just below the live price tag. Honours `candleCountdownTimeframes`. Toggleable from the in-chart Settings dialog. |
| `disableCountdownOnMobile` | `Boolean?` | `null` | Hide the countdown on small screens |
| `enableTrading` | `Boolean` | `false` | Show floating buy/sell order button |
| `minLots` | `Int` | `1` | Minimum lot size for order entry (requires `enableTrading = true`) |
| `maxSubPanes` | `Int?` | `null` | Max simultaneous oscillator sub-panes |
| `prefetchThreshold` | `Int?` | `null` | Bars from start of data at which historical fetch triggers (min 20, default 80) |
| `mobileBarDivisor` | `Int?` | `null` | Divide desktop bar count on touch devices (`2`, `3`, or `4`) |
| `minInitialBars` | `Int?` | `null` | If `onDataRequest` returns fewer bars, the fetch window auto-widens and retries. Default: `10` |
| `maxLookbackMs` | `Long?` | `null` | Hard ceiling (ms) for auto-widening retries. Default: 365 days |
| `momentumScrollEnabled` | `Boolean?` | `null` | Enable momentum (kinetic) scrolling — chart coasts after a fast flick. Default: `true` |
| `momentumDecay` | `Double?` | `null` | Per-frame velocity decay, normalised to 60 fps. Clamped `[0.80, 0.99]`. Default: `0.95` |
| `momentumThreshold` | `Double?` | `null` | Min release velocity (px/ms) to launch momentum. Default: `0.3` |
| `momentumMaxVelocity` | `Double?` | `null` | Max launch velocity (px/ms). Default: `6.0` |
| `targetCandleWidth` | `Double?` | `null` | Target px width per candle for auto-calculating initial bar count |
| `tickClosePriceSource` | `String?` | `null` | `"bid"` or `"ask"` for live tick close/high/low |
| `tradesThresholdForHorizontalLine` | `Int?` | `null` | Level count above which render auto-switches to dot mode |
| `tradeDisplayFilter` | `String?` | `null` | Which TFC levels are visible: `"all"` · `"positions"` · `"orders"` · `"none"` |
| `positionRenderStyle` | `String?` | `null` | Force position render style: `"line"` or `"dot"` |
| `hideLevelConfirmCancel` | `Boolean?` | `null` | Hide on-canvas ✓/✗ confirm/cancel buttons for TFC level edits |
| `deselectActiveOnOutsideClick` | `Boolean?` | `null` (`false`) | When `true`, clicking/tapping outside a selected trade level dismisses it (reverts pending edits). Default `false` preserves the active level across outside clicks so incidental taps (price-axis resize, taps outside the QTY input) don't drop an in-progress edit. Set `true` to restore the legacy outside-click-to-cancel behavior |
| `showTradeLevelsAlways` | `Boolean?` | `null` (`false`) | Always render SL/TP bracket lines + price pills, even when the parent level isn't hovered or selected. Close (×) buttons stay hover-only. Toggleable from the in-chart Settings dialog (Trading tab). |
| `tradeLevelButtonScale` | `Double?` | `null` (`1.0`) | Multiplier for trade-level Confirm/Cancel/Edit/Close button radii and gaps. Scales visuals **and** hit/drag areas together — raise it on touch devices for easier tapping. Clamped to `[1.0, 3.0]` |
| `levelClusteringEnabled` | `Boolean?` | `true` | Enable trade-level fan-out clustering; overlapping levels group into expandable badges |
| `clusterThresholdDistance` | `Int?` | `20` | Pixel proximity threshold for clustering (only when `levelClusteringEnabled` is `true`) |
| `tfcEnabled` | `Boolean?` | `true` | Enable TFC toggle button in the top bar; when `false`, TFC is completely disabled and the toggle button is hidden |
| `hideQtyButton` | `Boolean?` | `null` | Hide the floating Qty input overlay on draft orders |
| `showQuantityField` | `Boolean?` | `null` (`false`) | Render an editable QTY pill at the left of the draft order info box. Tapping opens a flyout input to edit the quantity before submitting |
| `quantityFieldMinLots` | `Double?` | `null` (`1.0`) | Minimum lot size, step size, and initial quantity for the QTY flyout (only used when `showQuantityField = true`) |
| `quantityFieldMaxLots` | `Double?` | `null` (`100.0`) | Maximum lot size for the QTY flyout (only used when `showQuantityField = true`) |
| `showSettings` | `Boolean?` | `null` | Show the settings gear button in the top bar; set to `false` to hide it entirely |
| `showFullscreenButton` | `Boolean` | `false` | Show the fullscreen toggle button in the top bar. Hidden by default on mobile; set to `true` to surface it |
| `hideSymbolAndTick` | `Boolean?` | `null` | Hide the symbol name and tick-activity (streaming) dot in the top-left overlay. Does **not** affect the OHLC(V) strip — use `hideOHLCV` for that |
| `hideOHLCV` | `Boolean?` | `null` | Hide the OHLC(V) data strip (`O: H: L: C: V:`) in the top-left overlay. Independent of `hideSymbolAndTick` — set both to `true` to hide the entire overlay |
| `showBottomBar` | `Boolean?` | `null` | Show the bottom duration-selector bar (hidden by default) |
| `hideHeader` | `Boolean?` | `null` (`false`) | Hide the chart header entirely (whichever `headerLayout` variant would have rendered). Bottom bar, drawing tools, and on-canvas overlays remain on their own flags. Drive the chart from native UI via `setTimeframe(...)`, `setSeries(...)`, `addIndicatorByName(...)`, `removeIndicator(...)` |
| `timezone` | `String?` | `null` (`"UTC"`) | IANA timezone string for time-axis and crosshair labels. `"UTC"` (default), `"local"` (device timezone), or any IANA string (`"America/New_York"`, `"Europe/London"`, etc.) |
| `uiConfigJson` | `String?` | `null` | Per-component UI configuration overrides (font sizes, icon sizes, spacing) as a raw JSON string. See *Mobile icon sizing* below. |
| `themeOverrides` | `ThemeOverrides?` | `null` | Typed per-theme color overrides. See *Theme overrides* below. |
| `initialCompares` | `List<String>?` | `null` | Compare symbols to add automatically on init. Each one fires `onCompareDataRequest` against the initial primary range — respond via `resolveCompareDataRequest` |
| `maxCompares` | `Int?` | `null` (`8`) | Maximum concurrent compare symbols. Adding beyond fires `onCompareError` |
| `stateJson` | `String?` | `null` | Raw JSON from a prior `onStateSnapshot` to restore atomically at init (timeframe, series, indicators, drawings). See *Restoring state without a flash* below. |

### Restoring state without a flash

When you need to restore a previously saved chart state (e.g. the user re-opens the chart screen), pass the snapshot JSON as `stateJson` in `init()` instead of calling `setState()` separately:

```kotlin
// ✅ Correct — init + setState are evaluated in a single evaluateJavascript call;
//    the engine never renders a frame with the default "1D" timeframe.
chart.onReady = {
    chart.init(stateJson = savedStateJson)
    chart.loadData(bars)
}

// ❌ Avoid — setState fires after the chart has already rendered once with "1D".
chart.onReady = {
    chart.init()
    chart.setState(savedStateJson)
}
```

For simple cases where you only need to set a specific timeframe (without full state restore), use the `timeframe` parameter in `init()` directly — no `stateJson` required.

### Theme overrides

Use `themeOverrides` (in `init()`) or `setThemeOverrides(overrides)` to selectively override colors for each theme mode. Only the keys you supply are merged on top of the built-in dark/light themes.

```kotlin
// At init time
chart.init(
    theme = "dark",
    symbol = "EURUSD",
    themeOverrides = ThemeOverrides(
        dark = ChartThemeOverride(
            background = "#0a0a0a",
            candle = CandleColors(up = "#00e676", down = "#ff1744"),
            topBar = TopBarColors(btnColor = "#cccccc"),
        )
    ),
)

// Or update at runtime
chart.setThemeOverrides(ThemeOverrides(
    dark = ChartThemeOverride(background = "#111111"),
    light = ChartThemeOverride(background = "#fafafa"),
))
```

All properties at every level are optional — only supply the ones you want to change. Available nested types: `TooltipColors`, `CandleColors`, `VolumeColors`, `UiColors`, `StreamColors`, `DrawingToolbarColors`, `TopBarColors`, `BottomBarColors`, `IndicatorOverlayColors`, `TradeLevelColors`, `TradePanelColors`.

> Raw JSON strings are still supported via `themeOverridesJson` / `setThemeOverrides(jsonString)` for backward compatibility.

### Mobile icon sizing

The chart automatically bumps top-bar icon buttons (settings, fullscreen, drawing toggle) and the floating trade ⊕ button to larger sizes when the container width drops below `uiConfig.drawingToolbar.mobileBreakpoint` (default `480px`). Defaults:

| Element | Desktop | Mobile |
|---------|---------|--------|
| Top-bar icon button container | 26px | 28px |
| Top-bar icon SVG | 14–15px | 16–17px |
| Trade ⊕ button container | 22px | 24px |
| Trade ⊕ icon SVG | 14px | 16px |

Override via `uiConfigJson`:

```kotlin
chart.init(
    theme = "dark",
    symbol = "AAPL",
    enableTrading = true,
    uiConfigJson = """
        {
          "topBar": {
            "mobileIconBtnSize": "30px",
            "mobileDrawBtnIconSize": "18px"
          },
          "tradeButton": {
            "mobileSize": 26,
            "mobileIconSize": 18
          }
        }
    """.trimIndent(),
)
```

## Events

| Callback | Type | Description |
|----------|------|-------------|
| `onReady` | `() -> Unit` | Engine ready to receive commands |
| `onCrosshair` | `BridgeEvent.Crosshair` | Crosshair moved — `.open`, `.high`, `.low`, `.close`, `.volume`, `.time`, `.x`, `.y` |
| `onBarClick` | `BridgeEvent.BarClick` | Bar tapped — `.open`, `.high`, `.low`, `.close`, `.volume`, `.time` |
| `onViewportChange` | `BridgeEvent.ViewportChange` | Pan / zoom — `.startIndex`, `.endIndex`, `.barWidth` |
| `onSeriesChange` | `BridgeEvent.SeriesChange` | Series type changed — `.series` |
| `onTimeframeChange` | `BridgeEvent.TimeframeChange` | Timeframe changed — `.timeframe` |
| `onDurationChange` | `BridgeEvent.DurationChange` | Duration selector changed — `.duration` |
| `onStateChange` | `BridgeEvent.StateChange` | Any chart state mutation — `.stateJson` |
| `onDataLoaded` | `BridgeEvent.DataLoaded` | `loadData` complete — `.barCount` |
| `onNewBar` | `BridgeEvent.NewBar` | New bar appended at live edge — `.open`, `.high`, `.low`, `.close`, `.volume`, `.time` |
| `onStreamStatus` | `BridgeEvent.StreamStatus` | Stream connection status changed — `.status` |
| `onPlaceOrder` | `BridgeEvent.PlaceOrder` | User submitted order (requires `enableTrading`) — `.price`, `.side`, `.orderType` |
| `onTradeLevelEdit` | `BridgeEvent.TradeLevelEdit` | User confirmed a TFC edit — `.label`, `.type`, `.data`, `.newLots?`, `.changes[]` (each with `.newLots?` on the `MAIN` change), `.isFullscreen`. When qty was edited this session, the `lots` field embedded in `.data` (and in the `MAIN` change's `data`) is overridden with the new value for convenience. |
| `onTradeLevelQtyChange` | `BridgeEvent.TradeLevelQtyChange` | Live qty edit via the QTY pill flyout — fires before the edit is confirmed, so hosts can refresh Estimated PNL on SL/TP brackets in real time — `.label`, `.type` (`"draft"` for draft orders, otherwise parent level's type), `.newLots`, `.previousLots`, `.isFullscreen` |
| `onTradeLevelClose` | `BridgeEvent.TradeLevelClose` | User tapped × on a level — `.label`, `.type`, `.action`, `.data`, `.isFullscreen` |
| `onTradeLevelDrag` | `BridgeEvent.TradeLevelDrag` | Live price during drag, fires on every move — `.label`, `.newPrice`, `.bracketType?`, `.data`, `.isFullscreen` |
| `onTradeLevelEditOpen` | `BridgeEvent.TradeLevelEditOpen` | User tapped the pencil button **or** (when `hideLevelConfirmCancel=true`) tapped a trade level line — `.label`, `.type`, `.price`, `.side?`, `.stopLossPrice?`, `.takeProfitPrice?`, `.data`, `.isFullscreen` |
| `onTradeLevelBracketActivated` | `BridgeEvent.TradeLevelBracketActivated` | SL/TP bracket auto-placed via `addLevelBracket` or `addBracket` — use `.price` to pre-populate your bracket price input — `.label` (`""` for draft orders, OrderID string for existing levels), `.bracketType`, `.price`, `.isFullscreen` |
| `onTradeLevelConfirmed` | `BridgeEvent.TradeLevelConfirmed` | Chart ✓ button confirmed an edit — `.label`, `.type`, `.isFullscreen` |
| `onTradeLevelEditCancelled` | `BridgeEvent.TradeLevelEditCancelled` | In-progress level edit aborted from the chart (ESC key or inline ✕ cancel button). Not fired for draft orders (see `onDraftCancelled`). Hosts listen to reset an external modify-order panel — `.label`, `.type`, `.isFullscreen` |
| `onDraftInitiated` | `BridgeEvent.DraftInitiated` | New draft order shown — `.side`, `.price`, `.orderType`, `.isFullscreen` |
| `onDraftCancelled` | `BridgeEvent.DraftCancelled` | Draft order cancelled — `.label`, `.isFullscreen` |
| `onTfcToggle` | `BridgeEvent.TfcToggle` | TFC toggled on/off via top bar button or `setTfcActive()` — `.enabled` |
| `onUiStateChange` | `BridgeEvent.UiStateChange` | Fires whenever any chart flyout/modal/dropdown opens or closes — `.hasOpenUI`. Most hosts don't need this directly; `ActtraderChartsView.hasOpenUI` mirrors the state automatically and `dismissAllUI()` is the usual integration point. |
| `onDataRequest` | `BridgeEvent.DataRequest` | Chart requests data for a time range — `.requestId`, `.from`, `.to`, `.timeframe`; call `resolveDataRequest` to respond |
| `onSymbolClick` | `BridgeEvent.SymbolClick` | User tapped the symbol name (requires `onSymbolClick = true` in `init`) |
| `onStateSnapshot` | `BridgeEvent.StateSnapshot` | Response to `getState()` — `.stateJson` |
| `onCompareDataRequest` | `BridgeEvent.CompareDataRequest` | Chart needs bars for a compare symbol — `.requestId`, `.symbol`, `.timeframe`, `.interval`, `.start`, `.end`; reply via `resolveCompareDataRequest` |
| `onCompareAdded` | `BridgeEvent.CompareAdded` | Compare symbol added — `.symbol`, `.color` |
| `onCompareRemoved` | `BridgeEvent.CompareRemoved` | Compare symbol removed — `.symbol` |
| `onCompareError` | `BridgeEvent.CompareError` | Compare fetch / add failed — `.symbol`, `.message` |
| `onIndicatorAdded` | `BridgeEvent.IndicatorAdded` | Study instance added — `.instanceId`, `.shortName`, `.params`; keep `instanceId` to remove that instance later |
| `onIndicatorRemoved` | `BridgeEvent.IndicatorRemoved` | Study instance removed — `.instanceId`, `.shortName` |
| `onError` | `BridgeEvent.Error` | Engine error — `.message`, `.code` |
| `onBridgeEvent` | `BridgeEvent` | Generic — fires for every event including those with typed callbacks |

> **`isFullscreen`** is `true` when the chart is in fullscreen mode at the time of the TFC action. Use it to gate toast notifications so they only appear while the chart is covering the full screen.

## Multi-pane layouts & snapshot

The WebView bundle includes the chart-owned multi-layout popover (26 grid
presets + 5 cross-pane sync toggles) and a snapshot popover (Download / Copy
PNG). Both are opt-in and dispatch bridge events back to the host.

### Enabling the layout & snapshot UI

```kotlin
chart.init(
    theme                 = "dark",
    symbol                = "EURUSD",
    timeframe             = "1h",
    headerLayout          = "advanced",   // "simple" (default) | "advanced" | "compact"
    enableMultipleLayouts = true,         // Layout button + preset picker
    enableSnapshot        = true,         // Snapshot button + Download/Copy
)

chart.onLayoutChange = { evt ->
    // evt.presetId is one of "1", "2-h", "2-v", "4-2x2", "6-2x3", "8-4x2", … —
    // see the JS library's LAYOUT_PRESETS for the full catalogue.
    // evt.syncJson is the raw LayoutSyncState JSON: {symbol, interval, crosshair, time, dateRange}.
    Log.d("Chart", "preset=${evt.presetId} sync=${evt.syncJson}")
    // The host owns the actual N-pane grid — mount/teardown sibling ActtraderChartsView
    // instances to match preset.count and apply the sync flags.
}

chart.onSnapshot = { evt ->
    // evt.action is "download" or "copy"; evt.dataUrl is a base64 PNG.
    // Intercept here to save to MediaStore / clipboard via platform APIs.
}
```

### `headerLayout`

| Value        | Use case
|--------------|----------------------------------------------------------------
| `"simple"`   | Classic TopBar (default) — symbol, type, timeframe, studies, drawings
| `"advanced"` | Compact pill-style toolbar — recommended above multi-pane grids
| `"compact"`  | Slim per-pane toolbar — recommended for individual cells of a grid

> **Sync flags are intent, not action.** The native host is responsible for
> mirroring symbol/timeframe/viewport across the sibling chart views — the
> JS-side `ChartGroup` only operates inside one WebView. On mobile each pane
> is its own `ActtraderChartsView`, so coordinate from native code (e.g. set
> the same timeframe on all panes when `syncJson["interval"]` is `true`).

## Compare symbols

Overlay one or more comparison instruments on the main chart, normalized to
percent change from the leftmost visible bar. Historical-only — no live
streaming. The chart owns the picker UI (filtered by the symbols you supplied
to `init` via `isins`) and refetches every active compare automatically when
the primary timeframe / symbol changes or the user pans back into history.

```kotlin
chart.init(
    symbol           = "AAPL",
    headerLayout     = "advanced",   // Compare button lives in this toolbar
    initialCompares  = listOf("SPY"),
    maxCompares      = 8,
)

// Serve the chart's compare data requests.
chart.onCompareDataRequest = { req ->
    lifecycleScope.launch {
        val bars = api.fetchBars(req.symbol, req.interval, req.start, req.end)
        chart.resolveCompareDataRequest(req.requestId, bars)
    }
}

chart.onCompareAdded   = { evt -> Log.d("chart", "+ ${evt.symbol} ${evt.color}") }
chart.onCompareRemoved = { evt -> Log.d("chart", "- ${evt.symbol}") }
chart.onCompareError   = { evt -> Log.w("chart", "${evt.symbol}: ${evt.message}") }

// Programmatic control.
chart.addCompare("MSFT")
chart.removeCompare("MSFT")
chart.clearCompares()
```

When at least one compare is active the Y-axis switches to percent
(`+12.34%` / `-5.67%`); removing every compare returns it to absolute prices.

## Multiple instances of the same study

Parameterized studies (EMA, SMA, RSI, BB, …) support multiple simultaneous
instances, each with an auto-cycled color:

```kotlin
chart.addIndicator("EMA", mapOf("period" to 20))
chart.addIndicator("EMA", mapOf("period" to 50))   // a 2nd EMA, distinct color
chart.addIndicator("EMA", mapOf("period" to 200))  // a 3rd

// Track instance ids so you can remove a specific one.
val emaIds = mutableListOf<String>()
chart.onIndicatorAdded   = { evt -> if (evt.shortName == "EMA") emaIds.add(evt.instanceId) }
chart.onIndicatorRemoved = { evt -> emaIds.remove(evt.instanceId) }

chart.removeIndicator(emaIds.first())  // remove just that instance ("EMA#1")
chart.removeIndicator("EMA")           // or remove ALL EMA instances
```

A few studies stay single-instance and toggle off when re-added: `VOL`, `OBV`,
`A/D`, `AO`, `VWAP`, `Ichimoku`, `PSAR`, `Pivot`, and Heikin-Ashi.

## Handling the hardware back button

When a flyout or modal is open in the chart (quantity edit, symbol picker,
indicator settings, chart settings, trade popover, topbar dropdowns, mobile
drawing flyout), the Android hardware back button should dismiss it rather
than finishing the host Activity. Wire `dismissAllUI()` in your Activity's
back-press callback — it returns `true` if anything was dismissed, in which
case the back event should be consumed.

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val chart = findViewById<ActtraderChartsView>(R.id.chart)

    onBackPressedDispatcher.addCallback(this) {
        if (!chart.dismissAllUI()) {
            // Nothing was open — fall through to normal back behavior.
            isEnabled = false
            onBackPressedDispatcher.onBackPressed()
        }
    }
}
```

`dismissAllUI()` is safe to call from any thread and returns synchronously
without a WebView round-trip when nothing is open. The view tracks its
dismissible state internally via the `uiStateChange` event; read
`chart.hasOpenUI` if you need to gate unrelated UI on the same state.

## Mobile mode — `hideLevelConfirmCancel`

Pass `hideLevelConfirmCancel = true` in `init()` to hide the on-canvas ✓/✗ buttons and drive the edit flow from your native UI instead.

Behaviour changes when this flag is active:

| Action | Result |
|--------|--------|
| Tap a trade level line | `onTradeLevelEditOpen` fires immediately (whole line is the edit target) |
| Tap empty canvas while a level is selected | Edit dismissed; pending drag changes reverted |
| Release a SL/TP bracket drag | `onTradeLevelEdit` fires automatically (no ✓ button needed) |

**Market orders from chart crosshair:** When live BID/ASK data is streaming and the crosshair trade button is tapped at a price inside the spread, `onDraftInitiated` fires with `orderType = "market"` — use this to open your market order form.

**Adding a bracket without a price:** Use `addBracket(bracketType, label?)` from your native form to auto-place a SL or TP bracket at a sensible default price:
- **Draft order (new order, no ID yet):** `chart.addBracket("sl")` — omit `label`; the chart operates on the active draft.
- **Existing order/position:** `chart.addBracket("sl", orderId)` — pass the OrderID/TradeID.

In both cases the chart fires `onTradeLevelBracketActivated` with the computed price — use it to populate your SL/TP input field. The event's `label` is `""` (empty string) for draft orders — check `label.isEmpty()` — and the OrderID string for existing levels.

To remove a bracket: use `removeBracket("sl")` (draft) or `removeBracket("sl", orderId)` (existing).

**Estimated P&L on bracket lines:** Call `setDraftBracketPnl("sl", "-$12.50")` to display a consumer-calculated P&L string next to the active bracket line on the chart. The text attaches to whichever level is the active bracket host — the draft order while drafting, or the currently selected existing pending order / position while modifying. Call `selectLevel(orderId)` (or have the user tap a level) before pushing the P&L text for an existing order. Pass `null` as the `pnlText` to clear.

## License

MIT
