#pragma once

#include <ESP8266WebServer.h>

#include "BuildConfig.h"
#include "Bl0940Protocol.h"
#include "SmartPlugMetering.h"

#if SMARTPLUG_ENABLE_MQTT
class SmartPlugMqtt;
#endif

class SmartPlugApi {
 public:
  SmartPlugApi();
  void begin();
  void handleClient();
  void printWebDiagnostics();
  // This clears every locally stored setting, including credentials and
  // sessions.  It is intentionally the only factory-reset primitive.
  void factoryResetWifi();
  bool takeRuntimeCalibration(smartplug_metering::Calibration& calibration);
  bool takeRelayCommand(bool& turnOn);
  void setMeterSnapshot(const bl0940_protocol::RawMeasurement& raw,
                        const smartplug_metering::ElectricalSample& electrical,
                        unsigned long capturedAtMs);
  void setMeterHealth(bool hasPollResult, bool latestPollValid,
                      uint32_t packetsOk, uint32_t packetsBad);
  void setStandbyState(bool detected, bool pending);
  void setVoltageAnomaly(const char* state);
  void setRelayState(const char* relayState, bool actuationAllowed);
#if SMARTPLUG_ENABLE_MQTT
  void setMqttConfigurator(SmartPlugMqtt* mqtt);
#endif

 private:
  void startNetwork();
  void handleDashboard();
  void handleRawStatsScript();
  void handleDashboardVersionScript();
  void handleDiscovery();
  void handleCapabilities();
  void handleStatus();
  void handleLatestMeasurement();
  void handleHealth();
  void handleWifiSettings();
  void handleWifiReset();
  void handleCalibration();
  void handleLogin();
  void handleLogout();
  void handleSession();
  void handleRelayCommand();
  void handleAccessSettings();
#if SMARTPLUG_ENABLE_MQTT
  void handleMqttSettings();
#endif
  void handleAuditLog();
  void handleNotFound();
  void sendJson(int code, const String& body);
  void sendError(int code, const char* error);
  bool requireSession(bool requireCsrf);
  bool validateMutationRate(unsigned long minimumIntervalMs);
  bool validateText(const String& value, size_t minLength, size_t maxLength,
                    bool allowSpaces) const;
  void loadSettings();
  bool saveSettings();
  void applyFactoryDefaults();
  void startSession();
  void clearSession();
  void addAuditEvent(const char* event);
  String deviceId() const;
  String randomHex(size_t bytes) const;
  String calibrationState() const;

  ESP8266WebServer server_;
  bool webServerStarted_ = false;
  bool stationConfigured_ = false;
  String configuredStationSsid_;
  bool hasSample_ = false;
  bl0940_protocol::RawMeasurement raw_ = {};
  smartplug_metering::ElectricalSample electrical_ = {};
  unsigned long capturedAtMs_ = 0;
  bool hasPollResult_ = false;
  bool latestPollValid_ = false;
  uint32_t packetsOk_ = 0;
  uint32_t packetsBad_ = 0;
  bool standbyDetected_ = false;
  bool standbyPending_ = false;
  const char* voltageAnomalyState_ = "unavailable_not_calibrated";
  const char* relayState_ = "unknown";
  bool relayActuationAllowed_ = false;
  smartplug_metering::Calibration runtimeCalibration_ = {};
  bool runtimeCalibrationChanged_ = false;
  bool relayCommandPending_ = false;
  bool requestedRelayOn_ = false;
  String sessionToken_;
  String csrfToken_;
  unsigned long sessionExpiresAtMs_ = 0;
  unsigned long lastMutationAtMs_ = 0;
  unsigned long lastRelayCommandAtMs_ = 0;
  unsigned long loginLockUntilMs_ = 0;
  uint8_t failedLoginCount_ = 0;
  String auditEvents_[12];
  uint8_t auditHead_ = 0;
  uint8_t auditCount_ = 0;
#if SMARTPLUG_ENABLE_MQTT
  SmartPlugMqtt* mqtt_ = nullptr;
#endif
};
