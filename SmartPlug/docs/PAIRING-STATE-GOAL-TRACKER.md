# Pairing state-machine delivery tracker

Last updated: 2026-10-20 16:52 WIB
Last tracker heartbeat: 2026-10-20 16:52 WIB

## Progress dashboard

| Scope | Progress | Status | Note |
|---|---:|---|---|
| **Overall goal** | **92%** | Active | Android 16 request now reaches Wi-Fi association; a disabled Wi-Fi radio on the test phone was isolated before AP authentication. |
| Phase 1. Pairing contract | 95% | Active | SSR/latch builds pass; device behavior awaits AP/LAN proof. |
| Phase 2. Android completion flow | 100% | Done | v0.1.72 debug/release builds contain Android 16 local-network permission recovery; physical flow awaits proof. |
| Phase 3. Build and regression | 90% | Done | SSR/latch and Android debug/release outputs built. |
| Phase 4. Factory-clean device simulation | 75% | Active | COM7 factory-clean SSR image and Android v0.1.72 are installed; Wi-Fi radio is being enabled for AP-to-monitoring proof. |

## Progress calculation

`35%×95 + 30%×100 + 20%×90 + 15%×75 = 92.25%` (physical end-to-end proof remains).

| Phase | Weight | Progress | Status | Closure evidence |
|---|---:|---:|---|---|
| 1. Pairing contract | 35% | 95% | Active | SSR/latch compile passed; physical AP/LAN state proof next. |
| 2. Android completion flow | 30% | 100% | Done | v0.1.72 debug/release build and focused Direct onboarding unit tests passed. |
| 3. Build and regression | 20% | 90% | Done | Both firmware profiles and Android debug/release assembled. |
| 4. Factory-clean device simulation | 15% | 75% | Active | AP is detected and Android's valid local-network request is observed; Wi-Fi radio must be enabled before association proof. |

### Phase 1 work items

| ID | Work item | Progress | Status | Next gate | Last update |
|---|---|---:|---|---|---|
| 1.1 | Audit current closure boundary | 100% | Done | Evidence recorded | Owner token hash is written by `pair/status`, closing normal pairing. |
| 1.2 | Implement pending/complete state | 100% | Done | SSR/latch compile | Configure enters pending; Wi-Fi failure/timeout/abort restore AP + unprovisioned. |
| 1.3 | Pairing state tests | 0% | Not started | Tests pass | — |

### Phase 2 work items

| ID | Work item | Progress | Status | Next gate | Last update |
|---|---|---:|---|---|---|
| 2.1 | Add complete endpoint client | 100% | Done | Android compile | Complete and authenticated abort clients added. |
| 2.2 | Remote rename then complete then local save | 100% | Done | Android build + focused tests | Order is remote name → local save → complete; local entry removed when complete fails. |

### Phase 3 work items

| ID | Work item | Progress | Status | Next gate | Last update |
|---|---|---:|---|---|---|
| 3.1 | Firmware SSR/latch builds | 100% | Done | Both pass capacity check | SSR 459,299 B; latch 460,559 B; both below 466,928 B. |
| 3.2 | Android tests and release/debug builds | 90% | Done | Artifacts exist | Debug built/installed; release unsigned output built. Unit suite deferred by narrowed scope. |

### Phase 4 work items

| ID | Work item | Progress | Status | Next gate | Last update |
|---|---|---:|---|---|---|
| 4.1 | Erase then upload revised SSR image | 100% | Done | Device rebooted after verified write | COM7 full erase succeeded; 463,600 B write hash verified; serial `info` responds in multiline form. |
| 4.2 | Simulate AP setup through completion | 55% | Active | Registered device shown in app | AP scan and Android 16 local-network request are confirmed; `cmd wifi status` revealed the test phone Wi-Fi radio was disabled, so no AP association could occur. |

## Issue register

| ID | Status | Evidence | Closure gate |
|---|---|---|---|
| PAI-01 | Open | Firmware SSR/latch builds passed after latest recovery edit. | Physical pairing verifies AP recovery and closure. |
| PAI-02 | Open | v0.1.72 declares and requests `NEARBY_WIFI_DEVICES`; debug/release build plus focused Direct tests passed. | Install and prove AP-to-monitoring pairing on Android 16. |
| PAI-03 | Open | Android issued a valid `WifiNetworkSpecifier` request with `LOCAL_NETWORK`, but it timed out while the test phone Wi-Fi radio was disabled; SmartPlug AP recorded 0 stations. | Enable Wi-Fi and prove WPA2 association, then continue onboarding. |

## Current work / agent state

| Sub-agent / worker | Active work item | Started at | Elapsed | Progress | Activity / state |
|---|---|---|---|---:|---|
| /root | 4.2 Factory-clean pairing simulation | 2026-10-20 16:15 WIB | 00:12:00 | 55% | Isolated disabled phone Wi-Fi as the reason no AP client was recorded; enabling Wi-Fi and retrying the same app flow. |

## Evidence 2026-10-20 16:22-16:25 WIB (v0.1.72 + connectedWifiNetworkFor fix)

- Root cause of the 30 s timeout (from `dumpsys connectivity`): phone was already on `SP-RZZV2AE2` (network 122, wlan0, 192.168.4.100), but the AP has no internet so the default/active network stayed CELLULAR (network 121). The first shortcut used `activeNetwork`, so it returned null and a second `WifiNetworkSpecifier` request (logged at `WifiOnboardingRepositoryImpl.kt:263`) was issued and timed out.
- Fix: `connectedWifiNetworkFor` now picks the WIFI-transport network from `allNetworks` after the SSID matches. `:app:testDebugUnitTest :app:assembleDebug` pass; APK installed with `adb install -r`.
- Re-run on phone: tap SmartPlug RZZV2AE2 -> no `requestNetwork` in logcat -> app advanced to "Choose home Wi-Fi" list (read from device over local API). Connect-AP stage passes.
- NOT yet proven: home Wi-Fi config, remote rename, `pair/complete`, monitoring page. Needs a home Wi-Fi password typed by the user. Package id for debug is `com.smartplug.app.debug`.

## Evidence 2026-10-20 16:31-16:34 WIB - END-TO-END PAIRING PROVEN

- Real root cause of the AP-connect timeout: the `NET_CAPABILITY_LOCAL_NETWORK` added to the `WifiNetworkSpecifier` request made Samsung/Android 16 `WifiNetworkFactory` ignore it (`mActiveSpecificNetworkRequest null`, no system dialog). Removing it: request becomes active (`Forbidden: LOCAL_NETWORK`), system "Connect to device?" dialog appears, WPA2 4-way completes.
- Second finding: a leftover `pairing_pending` from a failed attempt expires and `recoverPendingPairing` -> `startNetwork` rebuilds the AP, deauthing the phone (`reason=2`). Serial `web diag` then shows `station.configured:false` (back to unprovisioned) - expected per contract, not a bug.
- Also fixed: `connectedWifiNetworkFor` looks up the Wi-Fi transport in `allNetworks` (activeNetwork is cellular while the no-internet AP is joined).
- Run: AP connect (dialog Connect) -> pair info -> home Wi-Fi scan -> Bn (saved password auto-filled) -> configure -> SmartPlug STA 192.168.8.37 connected (serial) -> rename -> pair/complete -> "SmartPlug is ready" -> detail page "SmartPlug 93E3", Live, DIRECT, relay Off. No firmware change/upload; relay was not actuated.

## Evidence 2026-10-20 16:36-16:39 WIB - REPEAT RUN after factory reset (not a fluke)

- Reset via app menu (device detail -> Factory reset, typed device name). Serial `web diag` after reset: `station.configured:false`, AP `SP-RZZV2AE2` up; app Devices list empty.
- Second full pairing with the final APK (no `LOCAL_NETWORK`, `allNetworks` lookup): scan -> AP connect (no dialog, OS remembered approval) -> home Wi-Fi list -> Bn (saved password auto-filled) -> Connect -> serial `station.configured:true, connected:true, ip 192.168.8.37` -> "SmartPlug is ready" -> monitoring page. Same result as run 1; no retries needed.

## Evidence 2026-10-20 16:47-16:51 WIB - paired units hidden from Add SmartPlug scan

- Change: `AddSmartPlugViewModel.startScan` filters out APs whose `unitId` equals a registered device's `apUnitId` (`DeviceRepository.observeDevices().first()`); unit test added in `AddSmartPlugViewModelTest`.
- Before pairing, scan listed two unpaired units: RZZV2AE2 and EM4C75SC. After pairing RZZV2AE2 (registered as "SmartPlug 93E3", Live), re-opening Add SmartPlug listed only EM4C75SC; RZZV2AE2 was hidden although its AP still beacons.
- Limitation: a plug factory-reset by hardware only (profile still in app) stays hidden until the profile is removed in the app; app-side Factory reset removes the profile so it reappears.
