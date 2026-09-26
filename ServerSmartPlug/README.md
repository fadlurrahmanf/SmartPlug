# ServerSmartPlug — ESP32 firmware

`ServerSmartPlug` is the ESP32-side server for SmartPlug installations configured in MQTT mode. It runs these local services from one firmware image:

- MQTT 3.1.1 broker on TCP port 1883 for SmartPlug telemetry, relay commands, acknowledgement, retained state, availability, and energy synchronization.
- Application REST API on TCP port 80, protected by an API token.
- SD-card backed latest-state and one-minute history storage.
- A protected Wi-Fi access-point setup page; no serial-monitor step is part of normal commissioning.

## First commissioning

1. Power the ESP32 and join its setup access point: `ServerSmartPlug-Setup`.
2. Enter the setup password: `SmartPlugSetup`.
3. Open the local setup page displayed at the access point gateway and save the Wi-Fi credentials, application API token, MQTT username, and MQTT password.
4. Configure each SmartPlug for MQTT mode with the server's Wi-Fi address, port `1883`, and the MQTT credentials saved above.
5. Integrate the application through the server REST API, not directly through MQTT.

The access point remains available for later configuration. The broker denies MQTT connections until broker credentials have been configured. The application REST API denies access until its API token has been configured.

The broker accepts SmartPlug identities in the form `SmartPlug-SP-<STA_MAC>`.
After authentication, it enforces the device's own `smartplug/SP-<STA_MAC>/...`
base topic: a SmartPlug may publish only telemetry/state/availability/relay-ack
messages and may subscribe only to its own relay-command and energy-sync topics.

## REST API for the application

Every `/api/v1/...` endpoint requires either:

```text
Authorization: Bearer <application-api-token>
```

or:

```text
X-API-Key: <application-api-token>
```

Implemented routes are:

```text
GET  /api/v1/status
GET  /api/v1/devices
GET  /api/v1/devices/{device_id}
GET  /api/v1/devices/{device_id}/latest
GET  /api/v1/devices/{device_id}/history?from=<utc>&to=<utc>&resolution=1m|5m|30m|1h|1d
GET  /api/v1/devices/{device_id}/energy
POST /api/v1/devices/{device_id}/relay       {"state":"on"|"off"}
GET  /api/v1/commands/{command_id}
GET  /health
```

The server permits one pending relay command per device. It returns `409` when the device is offline or already has a pending command, resolves matching `ack/relay` messages as `completed` or `rejected`, and marks an unanswered command as `timeout` after five seconds.

## Storage behaviour

Telemetry refreshes latest data whenever `measurement/allparameters` arrives. The server aggregates voltage, current, active power, apparent power, and power factor into one-minute records; energy is stored as the latest cumulative counter, never averaged. The server maintains the greatest valid energy counter it has received and publishes it on `<base>/sync/energy` when the SmartPlug announces `availability=online`.

The SD card is the durable store. The status endpoint reports whether it was mounted. The default SD pins are defined in `platformio.ini` and must match the actual ServerSmartPlug board before hardware deployment.

## Build

```powershell
pio run -d D:\IoT\ServerSmartPlug -e server_esp32
```

This build command compiles the firmware only. It does not upload, provision, or prove MQTT, SD-card, or relay behaviour on physical hardware.
