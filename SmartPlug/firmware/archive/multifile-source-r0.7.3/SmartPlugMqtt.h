#pragma once

#include <Arduino.h>

#include "Bl0940Protocol.h"
#include "SmartPlugMetering.h"

// MQTT is intentionally a separate transport module.  The REST profile does
// not instantiate this class, so it carries neither a broker dependency nor
// MQTT credentials.
class SmartPlugMqtt {
 public:
  struct SettingsView {
    bool configured = false;
    bool connected = false;
    String host;
    uint16_t port = 1883;
    String username;
    String baseTopic;
  };

  void begin();
  void tick();

  void setMeterSnapshot(const bl0940_protocol::RawMeasurement& raw,
                        const smartplug_metering::ElectricalSample& electrical,
                        unsigned long capturedAtMs);
  void setMeterHealth(bool hasPollResult, bool latestPollValid,
                      uint32_t packetsOk, uint32_t packetsBad);
  void setRelayState(const char* relayState, bool actuationAllowed);

  bool takeRelayCommand(bool& turnOn);
  void reportRelayCommandResult(bool turnOn, bool accepted);
  bool handleConsoleCommand(const char* command);
  bool configured() const;
  bool connected() const;
  SettingsView settingsView() const;
  bool saveWebSettings(const String& host, uint16_t port,
                       const String& username, const String& password,
                       const String& baseTopic);

 private:
  void loadSettings();
  bool saveSettings();
  void clearSettings();
  void connectIfNeeded();
  void publishAvailability(const char* value);
  void publishState(bool retain = true);
  void publishTelemetry();
  void onMessage(char* topic, const uint8_t* payload, unsigned int length);
  static void callback(char* topic, uint8_t* payload, unsigned int length);
  String deviceId() const;
  String topic(const char* suffix) const;
  bool validText(const String& value, size_t minLength, size_t maxLength) const;

  bool settingsValid_ = false;
  bool hasSample_ = false;
  bl0940_protocol::RawMeasurement raw_ = {};
  smartplug_metering::ElectricalSample electrical_ = {};
  unsigned long capturedAtMs_ = 0;
  bool hasPollResult_ = false;
  bool latestPollValid_ = false;
  uint32_t packetsOk_ = 0;
  uint32_t packetsBad_ = 0;
  String relayState_ = "unknown";
  bool relayActuationAllowed_ = false;
  bool relayCommandPending_ = false;
  bool requestedRelayOn_ = false;
  unsigned long lastConnectAttemptAtMs_ = 0;
  unsigned long lastTelemetryAtMs_ = 0;
  String lastPublishedRelayState_;
};
