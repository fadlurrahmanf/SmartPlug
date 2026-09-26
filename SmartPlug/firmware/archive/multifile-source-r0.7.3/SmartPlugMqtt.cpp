#include "SmartPlugMqtt.h"

#include "BuildConfig.h"

#if SMARTPLUG_ENABLE_MQTT

#include <EEPROM.h>
#include <ESP8266WiFi.h>
#include <PubSubClient.h>
#include <cstring>

namespace {

constexpr size_t kMqttSettingsOffset = 512;
constexpr uint32_t kMqttSettingsMagic = 0x53504D31UL;  // SPM1
constexpr uint16_t kMqttSettingsVersion = 1;
constexpr unsigned long kReconnectIntervalMs = 5000UL;
constexpr unsigned long kTelemetryIntervalMs = 500UL;

struct MqttSettings {
  uint32_t magic;
  uint16_t version;
  uint16_t port;
  uint32_t crc;
  char host[64];
  char username[33];
  char password[65];
  char baseTopic[65];
};

static_assert(sizeof(MqttSettings) <= 512,
              "MQTT settings exceed the reserved EEPROM sector.");

MqttSettings mqttSettings = {};
WiFiClient mqttWifiClient;
PubSubClient mqttClient(mqttWifiClient);
SmartPlugMqtt* activeMqtt = nullptr;

uint32_t crc32(const uint8_t* data, const size_t length) {
  uint32_t crc = 0xFFFFFFFFUL;
  for (size_t i = 0; i < length; ++i) {
    crc ^= data[i];
    for (uint8_t bit = 0; bit < 8; ++bit) {
      crc = (crc >> 1) ^ ((crc & 1U) ? 0xEDB88320UL : 0U);
    }
  }
  return ~crc;
}

uint32_t settingsCrc(MqttSettings value) {
  value.crc = 0;
  return crc32(reinterpret_cast<const uint8_t*>(&value), sizeof(value));
}

void copyText(char* destination, const size_t size, const String& value) {
  value.substring(0, size - 1).toCharArray(destination, size);
}

bool reached(const unsigned long deadline) {
  return static_cast<long>(millis() - deadline) >= 0;
}

}  // namespace

String SmartPlugMqtt::deviceId() const {
  char id[7] = {};
  snprintf(id, sizeof(id), "%06lX", static_cast<unsigned long>(ESP.getChipId()));
  return String(id);
}

String SmartPlugMqtt::topic(const char* suffix) const {
  return String(mqttSettings.baseTopic) + "/" + suffix;
}

bool SmartPlugMqtt::validText(const String& value, const size_t minLength,
                               const size_t maxLength) const {
  if (value.length() < minLength || value.length() > maxLength) return false;
  for (size_t i = 0; i < value.length(); ++i) {
    const char c = value[i];
    if (c < 0x21 || c > 0x7E) return false;
  }
  return true;
}

void SmartPlugMqtt::loadSettings() {
  EEPROM.get(kMqttSettingsOffset, mqttSettings);
  settingsValid_ = mqttSettings.magic == kMqttSettingsMagic &&
                   mqttSettings.version == kMqttSettingsVersion &&
                   mqttSettings.crc == settingsCrc(mqttSettings) &&
                   validText(String(mqttSettings.host), 1, 63) &&
                   mqttSettings.port > 0 &&
                   validText(String(mqttSettings.baseTopic), 1, 64);
}

bool SmartPlugMqtt::saveSettings() {
  if (!validText(String(mqttSettings.host), 1, 63) ||
      mqttSettings.port == 0 ||
      !validText(String(mqttSettings.baseTopic), 1, 64)) {
    return false;
  }
  mqttSettings.magic = kMqttSettingsMagic;
  mqttSettings.version = kMqttSettingsVersion;
  mqttSettings.crc = settingsCrc(mqttSettings);
  EEPROM.put(kMqttSettingsOffset, mqttSettings);
  settingsValid_ = EEPROM.commit();
  return settingsValid_;
}

void SmartPlugMqtt::clearSettings() {
  mqttClient.disconnect();
  mqttSettings = {};
  EEPROM.put(kMqttSettingsOffset, mqttSettings);
  EEPROM.commit();
  settingsValid_ = false;
  Serial.println(F("INFO mqtt_settings_cleared"));
}

bool SmartPlugMqtt::configured() const { return settingsValid_; }
bool SmartPlugMqtt::connected() const { return mqttClient.connected(); }

SmartPlugMqtt::SettingsView SmartPlugMqtt::settingsView() const {
  SettingsView view;
  view.configured = settingsValid_;
  view.connected = mqttClient.connected();
  view.host = String(mqttSettings.host);
  view.port = mqttSettings.port == 0 ? 1883 : mqttSettings.port;
  view.username = String(mqttSettings.username);
  view.baseTopic = String(mqttSettings.baseTopic);
  return view;
}

bool SmartPlugMqtt::saveWebSettings(const String& host, const uint16_t port,
                                     const String& username,
                                     const String& password,
                                     const String& baseTopic) {
  if (!validText(host, 1, sizeof(mqttSettings.host) - 1) || port == 0 ||
      !validText(username, 0, sizeof(mqttSettings.username) - 1) ||
      !validText(password, 0, sizeof(mqttSettings.password) - 1) ||
      !validText(baseTopic, 1, sizeof(mqttSettings.baseTopic) - 1)) {
    return false;
  }
  mqttClient.disconnect();
  copyText(mqttSettings.host, sizeof(mqttSettings.host), host);
  mqttSettings.port = port;
  copyText(mqttSettings.username, sizeof(mqttSettings.username), username);
  copyText(mqttSettings.password, sizeof(mqttSettings.password), password);
  copyText(mqttSettings.baseTopic, sizeof(mqttSettings.baseTopic), baseTopic);
  lastConnectAttemptAtMs_ = 0;
  return saveSettings();
}

void SmartPlugMqtt::begin() {
  activeMqtt = this;
  loadSettings();
  mqttClient.setCallback(SmartPlugMqtt::callback);
  mqttClient.setBufferSize(512);
  if (!settingsValid_ && mqttSettings.baseTopic[0] == '\0') {
    copyText(mqttSettings.baseTopic, sizeof(mqttSettings.baseTopic),
             "smartplug/" + deviceId());
  }
  Serial.println(settingsValid_ ? F("INFO mqtt_settings_loaded")
                                : F("INFO mqtt_not_configured"));
}

void SmartPlugMqtt::publishAvailability(const char* value) {
  const String destination = topic("availability");
  mqttClient.publish(destination.c_str(), value, true);
}

void SmartPlugMqtt::publishState(const bool retain) {
  if (!mqttClient.connected()) return;
  const String destination = topic("state");
  String body = "{\"device_id\":\"" + deviceId() + "\",\"relay\":\"" +
                relayState_ + "\",\"relay_actuation\":" +
                (relayActuationAllowed_ ? "true" : "false") +
                ",\"wifi_connected\":" +
                (WiFi.status() == WL_CONNECTED ? "true" : "false") + "}";
  mqttClient.publish(destination.c_str(), body.c_str(), retain);
  lastPublishedRelayState_ = relayState_;
}

void SmartPlugMqtt::publishTelemetry() {
  if (!mqttClient.connected() || !hasSample_) return;
  const String destination = topic("telemetry");
  String body = "{\"device_id\":\"" + deviceId() + "\",\"captured_at_ms\":" +
                String(capturedAtMs_) + ",\"calibrated\":" +
                (electrical_.calibrated ? "true" : "false") +
                ",\"voltage_v\":" + String(electrical_.voltageV, 3) +
                ",\"current_a\":" + String(electrical_.currentA, 4) +
                ",\"active_power_w\":" + String(electrical_.activePowerW, 2) +
                ",\"energy_wh\":" + String(electrical_.energyWhSinceBoot, 3) +
                ",\"raw_codes\":{\"i_rms\":" + String(raw_.currentRms) +
                ",\"v_rms\":" + String(raw_.voltageRms) +
                ",\"active_power\":" + String(raw_.activePower) +
                "},\"packets_ok\":" + String(packetsOk_) +
                ",\"packets_bad\":" + String(packetsBad_) + "}";
  mqttClient.publish(destination.c_str(), body.c_str(), false);
}

void SmartPlugMqtt::connectIfNeeded() {
  if (!settingsValid_ || mqttClient.connected() ||
      WiFi.status() != WL_CONNECTED) return;
  const unsigned long now = millis();
  if (lastConnectAttemptAtMs_ != 0 &&
      now - lastConnectAttemptAtMs_ < kReconnectIntervalMs) return;
  lastConnectAttemptAtMs_ = now;
  mqttClient.setServer(mqttSettings.host, mqttSettings.port);
  const String clientId = "SmartPlug-" + deviceId();
  const String willTopic = topic("availability");
  const bool hasCredentials = mqttSettings.username[0] != '\0';
  const bool connected = hasCredentials
      ? mqttClient.connect(clientId.c_str(), mqttSettings.username,
                           mqttSettings.password, willTopic.c_str(), 1, true,
                           "offline")
      : mqttClient.connect(clientId.c_str(), willTopic.c_str(), 1, true,
                           "offline");
  if (!connected) {
    Serial.print(F("WARN mqtt_connect_failed state="));
    Serial.println(mqttClient.state());
    return;
  }
  const String commandTopic = topic("cmd/relay");
  mqttClient.subscribe(commandTopic.c_str());
  publishAvailability("online");
  publishState(true);
  Serial.println(F("INFO mqtt_connected"));
}

void SmartPlugMqtt::callback(char* topicName, uint8_t* payload,
                              unsigned int length) {
  if (activeMqtt != nullptr) activeMqtt->onMessage(topicName, payload, length);
}

void SmartPlugMqtt::onMessage(char* topicName, const uint8_t* payload,
                               const unsigned int length) {
  const String expected = topic("cmd/relay");
  if (String(topicName) != expected || length == 0 || length > 8) return;
  String command;
  for (unsigned int i = 0; i < length; ++i) command += static_cast<char>(payload[i]);
  command.trim(); command.toLowerCase();
  if (command != "on" && command != "off") {
    const String destination = topic("ack/relay");
    mqttClient.publish(destination.c_str(), "{\"accepted\":false,\"error\":\"invalid_state\"}", false);
    return;
  }
  if (!relayActuationAllowed_ || relayCommandPending_) {
    const String destination = topic("ack/relay");
    mqttClient.publish(destination.c_str(), "{\"accepted\":false,\"error\":\"relay_unavailable\"}", false);
    return;
  }
  requestedRelayOn_ = command == "on";
  relayCommandPending_ = true;
}

bool SmartPlugMqtt::takeRelayCommand(bool& turnOn) {
  if (!relayCommandPending_) return false;
  relayCommandPending_ = false;
  turnOn = requestedRelayOn_;
  return true;
}

void SmartPlugMqtt::reportRelayCommandResult(const bool turnOn,
                                              const bool accepted) {
  if (!mqttClient.connected()) return;
  const String destination = topic("ack/relay");
  const String body = String("{\"accepted\":") +
                      (accepted ? "true" : "false") + ",\"state\":\"" +
                      (turnOn ? "on" : "off") + "\"}";
  mqttClient.publish(destination.c_str(), body.c_str(), false);
}

void SmartPlugMqtt::setMeterSnapshot(
    const bl0940_protocol::RawMeasurement& raw,
    const smartplug_metering::ElectricalSample& electrical,
    const unsigned long capturedAtMs) {
  raw_ = raw; electrical_ = electrical; capturedAtMs_ = capturedAtMs;
  hasSample_ = true;
}

void SmartPlugMqtt::setMeterHealth(const bool hasPollResult,
                                    const bool latestPollValid,
                                    const uint32_t packetsOk,
                                    const uint32_t packetsBad) {
  hasPollResult_ = hasPollResult; latestPollValid_ = latestPollValid;
  packetsOk_ = packetsOk; packetsBad_ = packetsBad;
}

void SmartPlugMqtt::setRelayState(const char* relayState,
                                   const bool actuationAllowed) {
  relayState_ = relayState; relayActuationAllowed_ = actuationAllowed;
}

void SmartPlugMqtt::tick() {
  if (WiFi.status() != WL_CONNECTED && mqttClient.connected()) {
    mqttClient.disconnect();
  }
  connectIfNeeded();
  if (!mqttClient.connected()) return;
  mqttClient.loop();
  if (relayState_ != lastPublishedRelayState_) publishState(true);
  const unsigned long now = millis();
  if (lastTelemetryAtMs_ == 0 || now - lastTelemetryAtMs_ >= kTelemetryIntervalMs) {
    lastTelemetryAtMs_ = now;
    publishTelemetry();
  }
}

bool SmartPlugMqtt::handleConsoleCommand(const char* command) {
  const String input(command);
  if (input == "mqtt help") {
    Serial.println(F("mqtt show | mqtt host <host> | mqtt port <1-65535>"));
    Serial.println(F("mqtt user <username> | mqtt pass <password> | mqtt topic <base>"));
    Serial.println(F("mqtt save | mqtt clear"));
    return true;
  }
  if (input == "mqtt show") {
    Serial.print(F("{\"configured\":")); Serial.print(settingsValid_ ? F("true") : F("false"));
    Serial.print(F(",\"connected\":")); Serial.print(connected() ? F("true") : F("false"));
    Serial.print(F(",\"host\":\"")); Serial.print(mqttSettings.host);
    Serial.print(F("\",\"port\":")); Serial.print(mqttSettings.port);
    Serial.print(F(",\"topic\":\"")); Serial.print(mqttSettings.baseTopic);
    Serial.print(F("\",\"username\":\"")); Serial.print(mqttSettings.username);
    Serial.println(F("\"}"));
    return true;
  }
  const struct Field { const char* prefix; char* target; size_t size; } fields[] = {
      {"mqtt host ", mqttSettings.host, sizeof(mqttSettings.host)},
      {"mqtt user ", mqttSettings.username, sizeof(mqttSettings.username)},
      {"mqtt pass ", mqttSettings.password, sizeof(mqttSettings.password)},
      {"mqtt topic ", mqttSettings.baseTopic, sizeof(mqttSettings.baseTopic)},
  };
  for (const Field& field : fields) {
    const String prefix(field.prefix);
    if (input.startsWith(prefix)) {
      const String value = input.substring(prefix.length());
      const bool passwordField = String(field.prefix) == "mqtt pass ";
      if (!validText(value, passwordField ? 0 : 1, field.size - 1)) {
        Serial.println(F("ERR mqtt_invalid_value")); return true;
      }
      copyText(field.target, field.size, value);
      Serial.println(F("OK mqtt_setting_staged")); return true;
    }
  }
  if (input.startsWith("mqtt port ")) {
    const long port = input.substring(10).toInt();
    if (port < 1 || port > 65535) Serial.println(F("ERR mqtt_invalid_port"));
    else { mqttSettings.port = static_cast<uint16_t>(port); Serial.println(F("OK mqtt_setting_staged")); }
    return true;
  }
  if (input == "mqtt save") {
    mqttClient.disconnect();
    Serial.println(saveSettings() ? F("OK mqtt_settings_saved") : F("ERR mqtt_settings_invalid"));
    return true;
  }
  if (input == "mqtt clear") { clearSettings(); return true; }
  return input.startsWith("mqtt ");
}

#endif  // SMARTPLUG_ENABLE_MQTT
