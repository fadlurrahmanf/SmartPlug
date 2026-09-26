#include <Arduino.h>
#include <ESP8266WiFi.h>
#include <cstring>

#include "Bl0940.h"
#include "BoardPins.h"
#include "BuildConfig.h"
#include "LatchingRelay.h"
#include "SmartPlugConfig.h"
#include "SmartPlugAnomaly.h"
#include "SmartPlugMetering.h"

#if SMARTPLUG_ENABLE_LOCAL_API
#include "SmartPlugApi.h"
#endif

#if SMARTPLUG_ENABLE_MQTT
#include "SmartPlugMqtt.h"
#endif

namespace {

constexpr char kFirmwareName[] = "smartplug-bringup";
// 35-byte BL0940 frame at 4800 bps takes roughly 73–84 ms on the wire.
// 500 ms keeps a generous margin for SoftwareSerial and Wi-Fi while providing
// responsive local monitoring.
constexpr unsigned long kMeterPeriodMs = 500UL;
constexpr std::size_t kCommandCapacity = 80;

Bl0940 meter(board_pins::kMeterRx, board_pins::kMeterTx);
LatchingRelay relay(board_pins::kRelaySet, board_pins::kRelayReset,
                     build_config::kRelayPulseMs,
                     build_config::kRelayCooldownMs,
                     build_config::kRelayActuationAllowed);
smartplug_metering::EnergyIntegrator energyIntegrator;
smartplug_metering::MovingAverage10 meterMovingAverage;
smartplug_metering::StandbyDetector standbyDetector;
smartplug_anomaly::VoltageDetector voltageDetector;
smartplug_metering::Calibration meterCalibration = {
    smartplug_config::kVoltageVoltsPerCode,
    smartplug_config::kCurrentAmpsPerCode,
    smartplug_config::kPowerWattsPerCode,
};

#if SMARTPLUG_ENABLE_LOCAL_API
SmartPlugApi smartPlugApi;
#endif

#if SMARTPLUG_ENABLE_MQTT
SmartPlugMqtt smartPlugMqtt;
#endif

bl0940_protocol::RawMeasurement lastValidMeasurement = {};
bool hasMeterPollResult = false;
bool latestMeterPollValid = false;
bool hasLastValidMeasurement = false;
unsigned long lastValidMeasurementAt = 0;
unsigned long lastMeterPollAt = 0;
bool wifiFactoryResetInProgress = false;
char commandBuffer[kCommandCapacity] = {};
std::size_t commandLength = 0;

const char* boolText(const bool value) { return value ? "true" : "false"; }

void printSafetyBanner() {
  Serial.println();
  Serial.println(F("SMARTPLUG SAFE BRING-UP FIRMWARE"));
  Serial.println(F("WARNING: schematic GND is tied to mains neutral."));
  Serial.println(F("Never attach a non-isolated programmer while mains is present."));
  Serial.print(F("Relay actuation compiled in: "));
  Serial.println(boolText(relay.actuationAllowed()));
  Serial.print(F("SmartPlug local API compiled in: "));
  Serial.println(boolText(build_config::kLocalApiEnabled));
  Serial.print(F("SmartPlug MQTT compiled in: "));
  Serial.println(boolText(build_config::kMqttEnabled));
  Serial.println(F("Type 'help'."));
}

void printMeter() {
  Serial.print(F("{\"latest_poll_valid\":"));
  if (hasMeterPollResult) {
    Serial.print(boolText(latestMeterPollValid));
  } else {
    Serial.print(F("null"));
  }
  Serial.print(F(",\"poll_active\":"));
  Serial.print(boolText(meter.busy()));
  Serial.print(F(",\"has_last_valid_sample\":"));
  Serial.print(boolText(hasLastValidMeasurement));
  Serial.print(F(",\"last_valid_age_ms\":"));
  if (hasLastValidMeasurement) {
    Serial.print(millis() - lastValidMeasurementAt);
    Serial.print(F(",\"last_valid_raw_codes\":{\"i_fast_rms_code\":"));
    Serial.print(lastValidMeasurement.fastCurrentRms);
    Serial.print(F(",\"i_rms_code\":"));
    Serial.print(lastValidMeasurement.currentRms);
    Serial.print(F(",\"v_rms_code\":"));
    Serial.print(lastValidMeasurement.voltageRms);
    Serial.print(F(",\"active_power_code\":"));
    Serial.print(lastValidMeasurement.activePower);
    Serial.print(F(",\"cf_count_code\":"));
    Serial.print(lastValidMeasurement.cfCount);
    Serial.print(F(",\"tps1_code\":"));
    Serial.print(lastValidMeasurement.internalTemperature);
    Serial.print(F(",\"tps2_code\":"));
    Serial.print(lastValidMeasurement.externalTemperature);
    Serial.print(F("}"));
  } else {
    Serial.print(F("null"));
  }
  Serial.print(F(",\"packets_ok\":"));
  Serial.print(meter.validPacketCount());
  Serial.print(F(",\"packets_bad\":"));
  Serial.print(meter.invalidPacketCount());
  Serial.println(F("}"));
}

void printStatus() {
  Serial.print(F("{\"firmware\":\""));
  Serial.print(kFirmwareName);
  Serial.print(F("\",\"version\":\""));
  Serial.print(build_config::kFirmwareVersion);
  Serial.print(F("\",\"uptime_ms\":"));
  Serial.print(millis());
  Serial.print(F(",\"relay\":\""));
  Serial.print(relay.stateText());
  Serial.print(F("\",\"relay_actuation_allowed\":"));
  Serial.print(boolText(relay.actuationAllowed()));
  Serial.print(F(",\"button_enabled\":"));
  Serial.print(boolText(build_config::kButtonEnabled));
  Serial.print(F(",\"led_enabled\":"));
  Serial.print(boolText(build_config::kLedEnabled));
  Serial.print(F(",\"local_api_enabled\":"));
  Serial.print(boolText(build_config::kLocalApiEnabled));
  Serial.print(F(",\"mqtt_enabled\":"));
  Serial.print(boolText(build_config::kMqttEnabled));
  Serial.println(F("}"));
}

void printHelp() {
  Serial.println(F("help"));
  Serial.println(F("status"));
  Serial.println(F("meter"));
#if SMARTPLUG_ENABLE_LOCAL_API
  Serial.println(F("web diag"));
#endif
  Serial.println(F("relay on CONFIRM"));
  Serial.println(F("relay off CONFIRM"));
#if SMARTPLUG_ENABLE_MQTT
  Serial.println(F("mqtt help"));
#endif
  Serial.println(F("Relay commands require both build-time enable and confirmation token."));
}

void processCommand(const char* command) {
#if SMARTPLUG_ENABLE_MQTT
  if (smartPlugMqtt.handleConsoleCommand(command)) return;
#endif
  if (std::strcmp(command, "help") == 0) {
    printHelp();
    return;
  }
  if (std::strcmp(command, "status") == 0) {
    printStatus();
    return;
  }
  if (std::strcmp(command, "meter") == 0) {
    printMeter();
    return;
  }
#if SMARTPLUG_ENABLE_LOCAL_API
  if (std::strcmp(command, "web diag") == 0) {
    smartPlugApi.printWebDiagnostics();
    return;
  }
#endif

  bool accepted = false;
  if (std::strcmp(command, "relay on CONFIRM") == 0) {
    accepted = relay.requestOn();
  } else if (std::strcmp(command, "relay off CONFIRM") == 0) {
    accepted = relay.requestOff();
  } else {
    Serial.println(F("ERR unknown_command"));
    return;
  }

  Serial.println(accepted ? F("OK relay_pulse_started")
                          : F("ERR relay_disabled_busy_or_cooling_down"));
}

void serviceConsole() {
  // Do not allow diagnostic output to stretch an active relay-coil pulse.
  if (relay.busy()) {
    return;
  }

  while (Serial.available() > 0) {
    const char value = static_cast<char>(Serial.read());
    if (value == '\r') {
      continue;
    }
    if (value == '\n') {
      commandBuffer[commandLength] = '\0';
      if (commandLength > 0) {
        processCommand(commandBuffer);
      }
      commandLength = 0;
      if (relay.busy()) {
        return;
      }
      continue;
    }
    if (commandLength + 1 < kCommandCapacity) {
      commandBuffer[commandLength++] = value;
    } else {
      commandLength = 0;
      Serial.println(F("ERR command_too_long"));
    }
  }
}

void serviceMeter() {
  meter.tick();

  bl0940_protocol::RawMeasurement measurement = {};
  const Bl0940::PollResult result = meter.takeResult(measurement);
  if (result != Bl0940::PollResult::kNone) {
    hasMeterPollResult = true;
    latestMeterPollValid = result == Bl0940::PollResult::kValid;
    if (latestMeterPollValid) {
      lastValidMeasurement = meterMovingAverage.update(measurement);
      hasLastValidMeasurement = true;
      lastValidMeasurementAt = millis();
      const smartplug_metering::ElectricalSample electrical =
          energyIntegrator.update(lastValidMeasurement, meterCalibration,
                                  lastValidMeasurementAt,
                                  build_config::kMaxEnergySampleGapMs);
      standbyDetector.update(electrical, build_config::kStandbyThresholdW,
                             build_config::kStandbyDurationMs,
                             lastValidMeasurementAt);
      voltageDetector.update(electrical, build_config::kUndervoltageV,
                             build_config::kOvervoltageV);
#if SMARTPLUG_ENABLE_LOCAL_API
      smartPlugApi.setMeterSnapshot(lastValidMeasurement, electrical,
                                 lastValidMeasurementAt);
      smartPlugApi.setStandbyState(standbyDetector.detected(),
                                standbyDetector.pending());
      smartPlugApi.setVoltageAnomaly(voltageDetector.stateText());
#endif
#if SMARTPLUG_ENABLE_MQTT
      smartPlugMqtt.setMeterSnapshot(lastValidMeasurement, electrical,
                                     lastValidMeasurementAt);
#endif
    }
  }

  // Starting a UART transaction is deferred while a relay coil is active.
  if (!meter.busy() && !relay.busy() &&
      millis() - lastMeterPollAt >= kMeterPeriodMs) {
    if (meter.requestFullPacket()) {
      lastMeterPollAt = millis();
    }
  }
}

void serviceLocalApi() {
#if SMARTPLUG_ENABLE_LOCAL_API
  smartPlugApi.setMeterHealth(hasMeterPollResult, latestMeterPollValid,
                           meter.validPacketCount(), meter.invalidPacketCount());
  smartPlugApi.setRelayState(relay.stateText(), relay.actuationAllowed());
  smartPlugApi.handleClient();
  smartplug_metering::Calibration replacement = {};
  if (smartPlugApi.takeRuntimeCalibration(replacement)) {
    meterCalibration = replacement;
    energyIntegrator.reset();
    standbyDetector = smartplug_metering::StandbyDetector();
    voltageDetector = smartplug_anomaly::VoltageDetector();
  }
  bool requestedRelayOn = false;
  if (smartPlugApi.takeRelayCommand(requestedRelayOn)) {
    const bool accepted = requestedRelayOn ? relay.requestOn() : relay.requestOff();
    Serial.println(accepted ? F("INFO web_relay_command_accepted")
                            : F("WARN web_relay_command_rejected"));
  }
#endif
}

void serviceMqtt() {
#if SMARTPLUG_ENABLE_MQTT
  smartPlugMqtt.setMeterHealth(hasMeterPollResult, latestMeterPollValid,
                               meter.validPacketCount(), meter.invalidPacketCount());
  smartPlugMqtt.setRelayState(relay.stateText(), relay.actuationAllowed());
  smartPlugMqtt.tick();
  bool requestedRelayOn = false;
  if (smartPlugMqtt.takeRelayCommand(requestedRelayOn)) {
    const bool accepted = requestedRelayOn ? relay.requestOn() : relay.requestOff();
    smartPlugMqtt.reportRelayCommandResult(requestedRelayOn, accepted);
  }
#endif
}

void serviceOptionalButton() {
#if SMARTPLUG_ENABLE_LOCAL_API
  if (!build_config::kButtonEnabled || relay.busy()) {
    return;
  }

  // GPIO0 is a boot strap pin. This is deliberately evaluated only after a
  // normal boot: holding the button while power is applied invokes UART flash
  // mode and cannot be treated as a factory-reset request.
  static bool wasPressed = false;
  static bool resetIssued = false;
  static unsigned long pressedAtMs = 0;
  const bool pressed = digitalRead(board_pins::kConfigButton) == LOW;
  if (!pressed) {
    wasPressed = false;
    resetIssued = false;
    return;
  }
  if (!wasPressed) {
    wasPressed = true;
    pressedAtMs = millis();
    Serial.println(F("INFO wifi_reset_hold_started"));
    return;
  }
  if (!resetIssued && millis() - pressedAtMs >= 10000UL) {
    resetIssued = true;
    wifiFactoryResetInProgress = true;
    Serial.println(F("INFO wifi_factory_reset"));
    // Make the confirmation visible before rebooting to AP-only mode.
    digitalWrite(board_pins::kStatusLed, HIGH);
    delay(120);
    digitalWrite(board_pins::kStatusLed, LOW);
    delay(120);
    digitalWrite(board_pins::kStatusLed, HIGH);
    delay(120);
    smartPlugApi.factoryResetWifi();
  }
#endif
}

void serviceStatusLed() {
  if (!build_config::kLedEnabled) {
    return;
  }

  // LED1 on GPIO2 is active-low. These patterns are deliberately based on
  // firmware-observable states only; relay contact position is not observable.
  const unsigned long phase = millis() % 1000UL;
  bool ledOn = false;
  if (wifiFactoryResetInProgress) {
    ledOn = phase < 100UL || (phase >= 200UL && phase < 300UL);
  } else if (hasLastValidMeasurement &&
             millis() - lastValidMeasurementAt <= 5000UL) {
    // A valid, checksum-verified BL0940 packet was received recently.
    ledOn = true;
  } else if (hasMeterPollResult) {
    // Repeating double flash: meter poll has been attempted but is invalid.
    ledOn = phase < 90UL || (phase >= 180UL && phase < 270UL);
  } else {
    // Slow single pulse: firmware/AP is running and awaiting first meter poll.
    ledOn = phase < 120UL;
  }
  digitalWrite(board_pins::kStatusLed, ledOn ? LOW : HIGH);
}

}  // namespace

void setup() {
  relay.begin();
  Serial.begin(115200);

#if SMARTPLUG_ENABLE_LOCAL_API
  {
#if SMARTPLUG_ENABLE_MQTT
    smartPlugApi.setMqttConfigurator(&smartPlugMqtt);
#endif
    smartPlugApi.begin();
  }
#else
  {
    // The safe profile explicitly turns radio off instead of inheriting any
    // station configuration left in flash by an earlier image.
    WiFi.mode(WIFI_OFF);
    WiFi.forceSleepBegin();
    delay(1);
  }
#endif

#if SMARTPLUG_ENABLE_MQTT
  smartPlugMqtt.begin();
#endif

  if (build_config::kButtonEnabled) {
    pinMode(board_pins::kConfigButton, INPUT_PULLUP);
  }
  if (build_config::kLedEnabled) {
    pinMode(board_pins::kStatusLed, OUTPUT);
    // LED1 is wired active-low.  Drive it only after ESP8266 boot has
    // completed because GPIO2 is also a boot-strap pin.
    digitalWrite(board_pins::kStatusLed, LOW);
  }

  meter.begin();
  delay(25);
  printSafetyBanner();
}

void loop() {
  relay.tick();
  // Serve pending browser requests before any optional meter work. This makes
  // the dashboard responsive even if a meter transaction is malformed/noisy.
  serviceLocalApi();
  relay.tick();
  serviceMqtt();
  relay.tick();
  serviceConsole();
  relay.tick();
  serviceMeter();
  relay.tick();
  serviceLocalApi();
  relay.tick();
  serviceMqtt();
  relay.tick();
  serviceOptionalButton();
  relay.tick();
  serviceStatusLed();
  yield();
}
