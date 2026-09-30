#include <Arduino.h>
#include <ArduinoJson.h>
#include <FS.h>
#include <ESPmDNS.h>
#include <Preferences.h>
#include <SD.h>
#include <SPI.h>
#include <WebServer.h>
#include <WiFi.h>
#include <time.h>

// ServerSmartPlug R3.8.0
//
// This intentionally is one PlatformIO source file.  It is the complete
// ESP32-side server for the SmartPlug MQTT profile: commissioning AP, a small
// MQTT 3.1.1 broker, SD-card storage, and the application REST API.

namespace {

constexpr char kServerVersion[] = SERVER_SMARTPLUG_VERSION;
constexpr char kSetupApSsid[] = "ServerSmartPlug-Setup";
constexpr char kSetupApPassword[] = "SmartPlugSetup";
constexpr uint16_t kHttpPort = 80;
constexpr uint16_t kMqttPort = 1883;
constexpr uint8_t kMaxMqttClients = 8;
constexpr uint8_t kMaxSubscriptions = 8;
constexpr uint8_t kMaxRetainedMessages = 24;
constexpr uint8_t kMaxDevices = 12;
constexpr uint8_t kMaxDailySchedules = 8;
constexpr size_t kScheduleEventChars = 25;
constexpr size_t kMqttInputBytes = 1536;
constexpr uint32_t kCommandTimeoutMs = 5000UL;
constexpr uint32_t kIndexFlushIntervalMs = 10000UL;
constexpr uint32_t kScheduleCatchUpSeconds = 300UL;
constexpr char kIndexPath[] = "/smartplug/devices.csv";
constexpr char kIndexTemporaryPath[] = "/smartplug/devices.new";
constexpr char kIndexBackupPath[] = "/smartplug/devices.bak";
constexpr char kSchedulePath[] = "/smartplug/schedules.csv";
constexpr char kScheduleTemporaryPath[] = "/smartplug/schedules.new";
constexpr char kScheduleBackupPath[] = "/smartplug/schedules.bak";
constexpr char kHistoryPath[] = "/smartplug/history.csv";
constexpr char kEnergyResetAuditPath[] = "/smartplug/energy_resets.csv";

struct ServerSettings {
  char magic[4];
  uint16_t revision;
  char wifiSsid[33];
  char wifiPassword[65];
  char apiToken[65];
  char brokerUsername[33];
  char brokerPassword[65];
  uint32_t crc;
};

struct MqttSlot {
  WiFiClient tcp;
  bool occupied = false;
  bool connected = false;
  bool cleanSession = true;
  uint16_t keepAliveSeconds = 30;
  uint32_t lastRxAtMs = 0;
  char clientId[64] = {};
  char deviceId[20] = {};
  bool willEnabled = false;
  bool willRetain = false;
  uint8_t willQos = 0;
  char willTopic[128] = {};
  char willPayload[384] = {};
  char subscriptions[kMaxSubscriptions][128] = {};
  uint8_t subscriptionCount = 0;
  uint8_t rx[kMqttInputBytes] = {};
  size_t rxLength = 0;
};

struct RetainedMessage {
  bool used = false;
  char topic[128] = {};
  char payload[384] = {};
  uint8_t qos = 0;
};

struct DeviceRecord {
  bool used = false;
  char id[20] = {};
  bool online = false;
  bool calibrated = false;
  uint32_t lastSeenMs = 0;
  uint32_t lastSeenUtc = 0;
  float voltageV = 0.0F;
  float currentA = 0.0F;
  float activePowerW = 0.0F;
  float apparentPowerVa = 0.0F;
  float powerFactor = 0.0F;
  float energyWh = 0.0F;
  char relayState[8] = "unknown";
  uint32_t aggregateWindowUtc = 0;
  float sumVoltageV = 0.0F;
  float sumCurrentA = 0.0F;
  float sumActivePowerW = 0.0F;
  float sumApparentPowerVa = 0.0F;
  float sumPowerFactor = 0.0F;
  uint16_t aggregateCount = 0;
  char commandId[28] = {};
  char commandState[4] = {};
  char commandStatus[12] = "none";
  uint32_t commandCreatedAtMs = 0;
  uint32_t commandResolvedAtMs = 0;
  uint32_t timerDeadlineUtc = 0;
  uint32_t timerDurationSeconds = 0;
  uint32_t lastEnergyResetUtc = 0;
  bool awaitingEnergyReset = false;
  bool scheduleEnabled = false;
  int16_t timezoneOffsetMinutes = 0;
  struct DailyScheduleEntry {
    uint8_t hour = 0;
    uint8_t minute = 0;
    bool turnOn = false;
    char event[kScheduleEventChars] = {};
  } schedules[kMaxDailySchedules];
  uint8_t scheduleCount = 0;
  int64_t lastScheduleMinuteUtc = -1;
  uint32_t pendingScheduleDueUtc = 0;
  bool pendingScheduleTurnOn = false;
};

ServerSettings settings{};
MqttSlot mqttSlots[kMaxMqttClients];
RetainedMessage retainedMessages[kMaxRetainedMessages];
DeviceRecord devices[kMaxDevices];
WebServer http(kHttpPort);
WiFiServer mqttServer(kMqttPort);
Preferences preferences;
SPIClass sdSpi(VSPI);

bool settingsReady = false;
bool sdReady = false;
bool mdnsStarted = false;
uint32_t lastSdMountAttemptMs = 0;
bool indexDirty = false;
uint32_t lastIndexFlushAtMs = 0;
uint32_t commandCounter = 0;

uint32_t crc32(const uint8_t* bytes, const size_t length) {
  uint32_t value = 0xFFFFFFFFUL;
  for (size_t i = 0; i < length; ++i) {
    value ^= bytes[i];
    for (uint8_t bit = 0; bit < 8; ++bit) {
      value = (value >> 1U) ^ (0xEDB88320UL & (0U - (value & 1U)));
    }
  }
  return ~value;
}

template <size_t N>
void copyText(char (&destination)[N], const String& source) {
  const size_t length = min(source.length(), N - 1U);
  memcpy(destination, source.c_str(), length);
  destination[length] = '\0';
}

template <size_t N>
void copyLiteral(char (&destination)[N], const char* source) {
  const size_t length = min(strlen(source), N - 1U);
  memcpy(destination, source, length);
  destination[length] = '\0';
}

bool constantTimeEquals(const String& first, const char* second) {
  const size_t secondLength = strlen(second);
  const size_t maximum = max(first.length(), secondLength);
  uint8_t difference = static_cast<uint8_t>(first.length() ^ secondLength);
  for (size_t i = 0; i < maximum; ++i) {
    const char left = i < first.length() ? first[i] : 0;
    const char right = i < secondLength ? second[i] : 0;
    difference |= static_cast<uint8_t>(left ^ right);
  }
  return difference == 0;
}

bool validDeviceId(const String& id) {
  if (!id.startsWith("SP-") || id.length() != 15) return false;
  for (size_t i = 3; i < id.length(); ++i) {
    const char c = id[i];
    if (!((c >= '0' && c <= '9') || (c >= 'A' && c <= 'F'))) return false;
  }
  return true;
}

bool validCredentialText(const String& value, const size_t minimum,
                         const size_t maximum) {
  if (value.length() < minimum || value.length() > maximum) return false;
  for (size_t i = 0; i < value.length(); ++i) {
    const uint8_t c = static_cast<uint8_t>(value[i]);
    if (c < 0x21U || c > 0x7EU) return false;
  }
  return true;
}

bool validScheduleEvent(const String& value) {
  if (value.length() > kScheduleEventChars - 1U) return false;
  for (size_t i = 0; i < value.length(); ++i) {
    const char c = value[i];
    if (c < 0x20 || c > 0x7E || c == ',' || c == ':' || c == '"' || c == '\\') return false;
  }
  return true;
}

uint32_t currentUtc() {
  const time_t now = time(nullptr);
  return now >= 1700000000 ? static_cast<uint32_t>(now) : 0U;
}

bool timeIsSynchronized() { return currentUtc() != 0U; }

bool mqttCredentialsConfigured() {
  return strlen(settings.brokerUsername) >= 3 && strlen(settings.brokerPassword) >= 8;
}

bool applicationApiConfigured() { return strlen(settings.apiToken) >= 16; }

void resetSettings() {
  memset(&settings, 0, sizeof(settings));
  memcpy(settings.magic, "SPSV", 4);
  settings.revision = 1;
}

bool saveSettings() {
  memcpy(settings.magic, "SPSV", 4);
  settings.revision = 1;
  settings.crc = crc32(reinterpret_cast<const uint8_t*>(&settings),
                       offsetof(ServerSettings, crc));
  preferences.begin("smartplug-srv", false);
  const size_t written = preferences.putBytes("settings", &settings, sizeof(settings));
  preferences.end();
  return written == sizeof(settings);
}

void loadSettings() {
  resetSettings();
  preferences.begin("smartplug-srv", true);
  const size_t stored = preferences.getBytesLength("settings");
  if (stored == sizeof(settings)) preferences.getBytes("settings", &settings, sizeof(settings));
  preferences.end();
  const uint32_t expected = crc32(reinterpret_cast<const uint8_t*>(&settings),
                                  offsetof(ServerSettings, crc));
  if (stored != sizeof(settings) || memcmp(settings.magic, "SPSV", 4) != 0 ||
      settings.revision != 1 || settings.crc != expected) {
    resetSettings();
    settingsReady = false;
    return;
  }
  settings.wifiSsid[sizeof(settings.wifiSsid) - 1] = '\0';
  settings.wifiPassword[sizeof(settings.wifiPassword) - 1] = '\0';
  settings.apiToken[sizeof(settings.apiToken) - 1] = '\0';
  settings.brokerUsername[sizeof(settings.brokerUsername) - 1] = '\0';
  settings.brokerPassword[sizeof(settings.brokerPassword) - 1] = '\0';
  settingsReady = applicationApiConfigured() && mqttCredentialsConfigured();
}

void startNetwork() {
  WiFi.mode(WIFI_AP_STA);
  delay(100);
  const bool apStarted = WiFi.softAP(kSetupApSsid, kSetupApPassword);
  Serial.printf("INFO setup_ap started=%s ssid=%s ip=%s\\n",
                apStarted ? "true" : "false", kSetupApSsid,
                WiFi.softAPIP().toString().c_str());
  if (strlen(settings.wifiSsid) > 0) {
    WiFi.begin(settings.wifiSsid, settings.wifiPassword);
    Serial.println(F("INFO station_connecting"));
  }
}

String stationIpText() {
  return WiFi.status() == WL_CONNECTED ? WiFi.localIP().toString() : String();
}

String serverId() {
  String id = WiFi.macAddress();
  id.replace(":", "");
  id.toUpperCase();
  return String("SRV-") + id;
}

String mdnsHost() {
  String host = String("srvrplug-") + WiFi.macAddress();
  host.replace(":", "");
  host.toLowerCase();
  return host;
}

void serviceMdns() {
  if (WiFi.status() != WL_CONNECTED || mdnsStarted) return;
  const String host = mdnsHost();
  if (!MDNS.begin(host.c_str())) {
    Serial.println(F("WARN mdns_start_failed"));
    return;
  }
  MDNS.addService("srvrplug", "tcp", kHttpPort);
  MDNS.addServiceTxt("srvrplug", "tcp", "server_id", serverId());
  MDNS.addServiceTxt("srvrplug", "tcp", "api_version", "v1");
  MDNS.addServiceTxt("srvrplug", "tcp", "mqtt_port", String(kMqttPort));
  mdnsStarted = true;
  Serial.printf("INFO mdns_ready host=%s.local\\n", host.c_str());
}

void configureTimeIfConnected() {
  static bool configured = false;
  if (!configured && WiFi.status() == WL_CONNECTED) {
    configTime(0, 0, "pool.ntp.org", "time.nist.gov");
    configured = true;
  }
}

DeviceRecord* findDevice(const String& id, const bool create) {
  if (!validDeviceId(id)) return nullptr;
  for (DeviceRecord& device : devices) {
    if (device.used && id == device.id) return &device;
  }
  if (!create) return nullptr;
  for (DeviceRecord& device : devices) {
    if (!device.used) {
      device = DeviceRecord{};
      device.used = true;
      copyText(device.id, id);
      return &device;
    }
  }
  return nullptr;
}

uint8_t deviceCount() {
  uint8_t count = 0;
  for (const DeviceRecord& device : devices) if (device.used) ++count;
  return count;
}

void sendJson(const int status, const String& body) {
  http.sendHeader("Access-Control-Allow-Origin", "*");
  http.sendHeader("Access-Control-Allow-Headers", "Authorization, X-API-Key, Content-Type");
  http.sendHeader("Cache-Control", "no-store");
  http.send(status, "application/json", body);
}

void sendError(const int status, const char* error) {
  sendJson(status, String("{\"error\":\"") + error + "\"}");
}

bool requestIsAuthorized() {
  if (!applicationApiConfigured()) {
    sendError(503, "server_not_configured");
    return false;
  }
  String token = http.header("X-API-Key");
  const String authorization = http.header("Authorization");
  if (token.isEmpty() && authorization.startsWith("Bearer ")) {
    token = authorization.substring(7);
  }
  if (!constantTimeEquals(token, settings.apiToken)) {
    sendError(401, "authentication_required");
    return false;
  }
  return true;
}

String jsonNumber(const float value, const uint8_t decimals) {
  if (!isfinite(value)) return "0";
  return String(value, static_cast<unsigned int>(decimals));
}

String deviceSummaryJson(const DeviceRecord& device) {
  String json = "{\"device_id\":\"" + String(device.id) + "\",\"display_name\":\"" +
                String(device.id) + "\",\"status\":\"" + (device.online ? "online" : "offline") +
                "\",\"last_seen_utc\":" + String(device.lastSeenUtc) + ",\"relay\":\"" +
                String(device.relayState) + "\",\"energy_wh\":" + jsonNumber(device.energyWh, 3) +
                ",\"timer_deadline_utc\":" + String(device.timerDeadlineUtc) +
                ",\"timer_duration_seconds\":" + String(device.timerDurationSeconds) + "}";
  return json;
}

String deviceScheduleJson(const DeviceRecord& device);

String deviceLatestJson(const DeviceRecord& device) {
  return String("{\"device_id\":\"") + device.id +
         "\",\"captured_at_ms\":" + String(static_cast<uint64_t>(device.lastSeenUtc) * 1000ULL) +
         ",\"status\":\"" + (device.online ? "online" : "offline") +
         "\",\"relay_state\":\"" + device.relayState +
         "\",\"calibrated\":" + (device.calibrated ? "true" : "false") +
         ",\"voltage_v\":" + jsonNumber(device.voltageV, 3) +
         ",\"current_a\":" + jsonNumber(device.currentA, 4) +
         ",\"active_power_w\":" + jsonNumber(device.activePowerW, 2) +
         ",\"apparent_power_va\":" + jsonNumber(device.apparentPowerVa, 2) +
         ",\"power_factor\":" + jsonNumber(device.powerFactor, 3) +
         ",\"energy_wh\":" + jsonNumber(device.energyWh, 3) +
         ",\"timer_deadline_utc\":" + String(device.timerDeadlineUtc) +
         ",\"timer_duration_seconds\":" + String(device.timerDurationSeconds) +
         ",\"schedule\":" + deviceScheduleJson(device) + "}";
}

String deviceScheduleJson(const DeviceRecord& device) {
  const uint32_t now = currentUtc();
  uint32_t nextSeconds = 0U;
  int nextState = -1;
  if (now != 0U && device.scheduleEnabled && device.scheduleCount > 0U) {
    const time_t localEpoch = static_cast<time_t>(now) +
                              static_cast<time_t>(device.timezoneOffsetMinutes) * 60;
    tm local = {};
    gmtime_r(&localEpoch, &local);
    const int currentMinute = local.tm_hour * 60 + local.tm_min;
    for (uint8_t index = 0; index < device.scheduleCount; ++index) {
      const auto& entry = device.schedules[index];
      int deltaMinutes = (entry.hour * 60 + entry.minute) - currentMinute;
      if (deltaMinutes <= 0) deltaMinutes += 24 * 60;
      const uint32_t candidate = static_cast<uint32_t>(deltaMinutes * 60 - local.tm_sec);
      if (nextState < 0 || candidate < nextSeconds) {
        nextSeconds = candidate;
        nextState = entry.turnOn ? 1 : 0;
      }
    }
  }
  String result = String("{\"enabled\":") + (device.scheduleEnabled ? "true" : "false") +
      ",\"clock\":{\"synchronized\":" + (now != 0U ? "true" : "false") +
      ",\"utc_ms\":" + (now != 0U ? String(static_cast<uint64_t>(now) * 1000ULL) : "0") +
      ",\"timezone_offset_minutes\":" + String(device.timezoneOffsetMinutes) + "}" +
      ",\"next\":{\"remaining_seconds\":" + String(nextSeconds) + ",\"state\":\"" +
      (nextState == 1 ? "on" : nextState == 0 ? "off" : "") + "\"},\"entries\":[";
  for (uint8_t index = 0; index < device.scheduleCount; ++index) {
    if (index != 0U) result += ',';
    const auto& entry = device.schedules[index];
    result += String("{\"hour\":") + String(entry.hour) + ",\"minute\":" +
        String(entry.minute) + ",\"state\":\"" + (entry.turnOn ? "on" : "off") +
        "\",\"event\":\"" + String(entry.event) + "\"}";
  }
  return result + "]}";
}

void appendEnergyResetAudit(const DeviceRecord& device, const float previousEnergyWh,
                            const uint32_t resetUtc) {
  if (!sdReady || resetUtc == 0U) return;
  File file = SD.open(kEnergyResetAuditPath, FILE_WRITE);
  if (!file) return;
  file.printf("%lu,%s,%.3f\n", static_cast<unsigned long>(resetUtc), device.id, previousEnergyWh);
  file.close();
}

void appendHistory(const DeviceRecord& device, const uint32_t windowUtc,
                   const float voltageV, const float currentA,
                   const float activePowerW, const float apparentPowerVa,
                   const float powerFactor, const float energyWh) {
  if (!sdReady || windowUtc == 0U) return;
  File file = SD.open(kHistoryPath, FILE_WRITE);
  if (!file) return;
  file.printf("%lu,%s,%.3f,%.4f,%.2f,%.2f,%.3f,%.3f\n",
              static_cast<unsigned long>(windowUtc), device.id, voltageV, currentA,
              activePowerW, apparentPowerVa, powerFactor, energyWh);
  file.close();
}

void flushAggregate(DeviceRecord& device) {
  if (device.aggregateCount == 0 || device.aggregateWindowUtc == 0U) return;
  const float count = static_cast<float>(device.aggregateCount);
  appendHistory(device, device.aggregateWindowUtc, device.sumVoltageV / count,
                device.sumCurrentA / count, device.sumActivePowerW / count,
                device.sumApparentPowerVa / count, device.sumPowerFactor / count,
                device.energyWh);
  device.sumVoltageV = 0.0F;
  device.sumCurrentA = 0.0F;
  device.sumActivePowerW = 0.0F;
  device.sumApparentPowerVa = 0.0F;
  device.sumPowerFactor = 0.0F;
  device.aggregateCount = 0;
}

void addAggregateSample(DeviceRecord& device) {
  const uint32_t now = currentUtc();
  if (now == 0U) return;
  const uint32_t window = now - (now % 60U);
  if (device.aggregateWindowUtc == 0U) device.aggregateWindowUtc = window;
  if (window != device.aggregateWindowUtc) {
    flushAggregate(device);
    device.aggregateWindowUtc = window;
  }
  device.sumVoltageV += device.voltageV;
  device.sumCurrentA += device.currentA;
  device.sumActivePowerW += device.activePowerW;
  device.sumApparentPowerVa += device.apparentPowerVa;
  device.sumPowerFactor += device.powerFactor;
  if (device.aggregateCount < UINT16_MAX) ++device.aggregateCount;
}

bool replaceFileAtomically(const char* temporaryPath, const char* activePath,
                           const char* backupPath) {
  SD.remove(backupPath);
  if (SD.exists(activePath) && !SD.rename(activePath, backupPath)) return false;
  if (!SD.rename(temporaryPath, activePath)) {
    if (SD.exists(backupPath)) SD.rename(backupPath, activePath);
    return false;
  }
  SD.remove(backupPath);
  return true;
}

bool restoreBackupIfNeeded(const char* activePath, const char* backupPath) {
  if (SD.exists(activePath)) return true;
  return SD.exists(backupPath) && SD.rename(backupPath, activePath);
}

bool saveDeviceIndex() {
  if (!sdReady) return false;
  SD.mkdir("/smartplug");
  SD.remove(kIndexTemporaryPath);
  SD.remove(kScheduleTemporaryPath);
  File file = SD.open(kIndexTemporaryPath, FILE_WRITE);
  File scheduleFile = SD.open(kScheduleTemporaryPath, FILE_WRITE);
  if (!file || !scheduleFile) {
    if (file) file.close();
    if (scheduleFile) scheduleFile.close();
    SD.remove(kIndexTemporaryPath);
    SD.remove(kScheduleTemporaryPath);
    return false;
  }
  for (const DeviceRecord& device : devices) {
    if (!device.used) continue;
    file.printf("%s,%.3f,%s,%lu,%lu,%lu,%lu,%u\n", device.id, device.energyWh, device.relayState,
                static_cast<unsigned long>(device.timerDeadlineUtc),
                static_cast<unsigned long>(device.timerDurationSeconds),
                static_cast<unsigned long>(device.lastEnergyResetUtc),
                static_cast<unsigned long>(device.pendingScheduleDueUtc),
                device.pendingScheduleTurnOn ? 1U : 0U);
    // Schedule records are kept separately so an interrupted older index write
    // cannot make the core device record unreadable.
    scheduleFile.printf("%s,%u,%d", device.id, device.scheduleEnabled ? 1U : 0U,
                        static_cast<int>(device.timezoneOffsetMinutes));
    for (uint8_t index = 0; index < device.scheduleCount; ++index) {
      const auto& entry = device.schedules[index];
      scheduleFile.printf(",%u:%u:%u:%s", entry.hour, entry.minute,
                          entry.turnOn ? 1U : 0U, entry.event);
    }
    scheduleFile.println();
  }
  file.close();
  scheduleFile.close();
  if (!replaceFileAtomically(kIndexTemporaryPath, kIndexPath, kIndexBackupPath) ||
      !replaceFileAtomically(kScheduleTemporaryPath, kSchedulePath, kScheduleBackupPath)) {
    return false;
  }
  indexDirty = false;
  lastIndexFlushAtMs = millis();
  return true;
}

void loadDeviceIndex() {
  if (!sdReady) return;
  restoreBackupIfNeeded(kIndexPath, kIndexBackupPath);
  restoreBackupIfNeeded(kSchedulePath, kScheduleBackupPath);
  if (!SD.exists(kIndexPath)) return;
  File file = SD.open(kIndexPath, FILE_READ);
  if (!file) return;
  while (file.available()) {
    const String line = file.readStringUntil('\n');
    const int firstComma = line.indexOf(',');
    const int secondComma = line.indexOf(',', firstComma + 1);
    if (firstComma < 0 || secondComma < 0) continue;
    const String id = line.substring(0, firstComma);
    DeviceRecord* device = findDevice(id, true);
    if (device == nullptr) continue;
    const float savedEnergy = line.substring(firstComma + 1, secondComma).toFloat();
    if (isfinite(savedEnergy) && savedEnergy >= device->energyWh) {
      device->energyWh = savedEnergy;
    }
    const int thirdComma = line.indexOf(',', secondComma + 1);
    const String relay = line.substring(secondComma + 1, thirdComma < 0 ? line.length() : thirdComma);
    if (relay == "on" || relay == "off" || relay == "unknown") {
      copyText(device->relayState, relay);
    }
    if (thirdComma < 0) continue;
    const int fourthComma = line.indexOf(',', thirdComma + 1);
    device->timerDeadlineUtc = strtoul(line.substring(thirdComma + 1,
                                                       fourthComma < 0 ? line.length() : fourthComma).c_str(), nullptr, 10);
    if (fourthComma < 0) continue;
    const int fifthComma = line.indexOf(',', fourthComma + 1);
    device->timerDurationSeconds = strtoul(line.substring(fourthComma + 1,
                                                          fifthComma < 0 ? line.length() : fifthComma).c_str(), nullptr, 10);
    if (fifthComma >= 0) {
      const int sixthComma = line.indexOf(',', fifthComma + 1);
      device->lastEnergyResetUtc = strtoul(line.substring(fifthComma + 1,
          sixthComma < 0 ? line.length() : sixthComma).c_str(), nullptr, 10);
      if (sixthComma >= 0) {
        const int seventhComma = line.indexOf(',', sixthComma + 1);
        device->pendingScheduleDueUtc = strtoul(line.substring(sixthComma + 1,
            seventhComma < 0 ? line.length() : seventhComma).c_str(), nullptr, 10);
        if (seventhComma >= 0) {
          device->pendingScheduleTurnOn = line.substring(seventhComma + 1).toInt() != 0;
        }
      }
    }
  }
  file.close();
  if (!SD.exists(kSchedulePath)) return;
  File scheduleFile = SD.open(kSchedulePath, FILE_READ);
  if (!scheduleFile) return;
  while (scheduleFile.available()) {
    const String line = scheduleFile.readStringUntil('\n');
    const int firstComma = line.indexOf(',');
    const int secondComma = line.indexOf(',', firstComma + 1);
    if (firstComma < 0 || secondComma < 0) continue;
    DeviceRecord* device = findDevice(line.substring(0, firstComma), false);
    if (device == nullptr) continue;
    device->scheduleEnabled = line.substring(firstComma + 1, secondComma).toInt() != 0;
    int cursor = secondComma + 1;
    int nextComma = line.indexOf(',', cursor);
    const int offset = line.substring(cursor, nextComma < 0 ? line.length() : nextComma).toInt();
    device->timezoneOffsetMinutes = static_cast<int16_t>(constrain(offset, -720, 840));
    device->scheduleCount = 0;
    while (nextComma >= 0 && device->scheduleCount < kMaxDailySchedules) {
      cursor = nextComma + 1;
      nextComma = line.indexOf(',', cursor);
      const String encoded = line.substring(cursor, nextComma < 0 ? line.length() : nextComma);
      const int firstColon = encoded.indexOf(':');
      const int secondColon = encoded.indexOf(':', firstColon + 1);
      if (firstColon < 0 || secondColon < 0) continue;
      const int hour = encoded.substring(0, firstColon).toInt();
      const int minute = encoded.substring(firstColon + 1, secondColon).toInt();
      const int thirdColon = encoded.indexOf(':', secondColon + 1);
      const int state = encoded.substring(secondColon + 1,
                                          thirdColon < 0 ? encoded.length() : thirdColon).toInt();
      if (hour < 0 || hour > 23 || minute < 0 || minute > 59 || (state != 0 && state != 1)) continue;
      auto& entry = device->schedules[device->scheduleCount++];
      entry.hour = static_cast<uint8_t>(hour);
      entry.minute = static_cast<uint8_t>(minute);
      entry.turnOn = state == 1;
      if (thirdColon >= 0) copyText(entry.event, encoded.substring(thirdColon + 1));
    }
  }
  scheduleFile.close();
}

void beginStorage() {
  lastSdMountAttemptMs = millis();
  sdSpi.begin(SERVER_SMARTPLUG_SD_SCK, SERVER_SMARTPLUG_SD_MISO,
              SERVER_SMARTPLUG_SD_MOSI, SERVER_SMARTPLUG_SD_CS);
  sdReady = SD.begin(SERVER_SMARTPLUG_SD_CS, sdSpi);
  if (!sdReady) {
    Serial.println(F("WARN sd_unavailable"));
    return;
  }
  const uint8_t cardType = SD.cardType();
  Serial.printf("INFO sd_ready type=%u size_mb=%llu\\n", cardType,
                static_cast<unsigned long long>(SD.cardSize() / (1024ULL * 1024ULL)));
  SD.mkdir("/smartplug");
  loadDeviceIndex();
}

bool topicMatches(const String& filter, const String& topic) {
  if (filter == "#") return true;
  int filterStart = 0;
  int topicStart = 0;
  while (true) {
    const int filterSlash = filter.indexOf('/', filterStart);
    const int topicSlash = topic.indexOf('/', topicStart);
    const String filterLevel = filter.substring(filterStart,
                                                filterSlash < 0 ? filter.length() : filterSlash);
    const String topicLevel = topic.substring(topicStart,
                                              topicSlash < 0 ? topic.length() : topicSlash);
    if (filterLevel == "#") return filterSlash < 0;
    if (filterLevel != "+" && filterLevel != topicLevel) return false;
    if (filterSlash < 0 || topicSlash < 0) return filterSlash < 0 && topicSlash < 0;
    filterStart = filterSlash + 1;
    topicStart = topicSlash + 1;
  }
}

String mqttPacket(const uint8_t header, const String& topic, const String& payload,
                  const uint8_t qos, const bool retain) {
  String variable;
  const uint16_t topicLength = topic.length();
  variable.reserve(topicLength + payload.length() + 8);
  variable += static_cast<char>((topicLength >> 8U) & 0xFFU);
  variable += static_cast<char>(topicLength & 0xFFU);
  variable += topic;
  if (qos > 0U) {
    variable += static_cast<char>(0);
    variable += static_cast<char>(1);
  }
  variable += payload;
  String packet;
  packet.reserve(variable.length() + 5);
  packet += static_cast<char>(header | (qos << 1U) | (retain ? 1U : 0U));
  size_t remaining = variable.length();
  do {
    uint8_t encoded = remaining % 128U;
    remaining /= 128U;
    if (remaining > 0U) encoded |= 0x80U;
    packet += static_cast<char>(encoded);
  } while (remaining > 0U);
  packet += variable;
  return packet;
}

void sendMqttPacket(MqttSlot& slot, const String& packet) {
  if (slot.occupied && slot.tcp.connected()) {
    slot.tcp.write(reinterpret_cast<const uint8_t*>(packet.c_str()), packet.length());
  }
}

void sendSimpleMqtt(MqttSlot& slot, const uint8_t header, const uint8_t value = 0U) {
  uint8_t packet[2] = {header, 0U};
  if (header == 0x20U) {
    uint8_t connack[4] = {0x20U, 0x02U, 0x00U, value};
    slot.tcp.write(connack, sizeof(connack));
  } else if (header == 0x40U) {
    uint8_t puback[4] = {0x40U, 0x02U, 0x00U, value};
    slot.tcp.write(puback, sizeof(puback));
  } else {
    slot.tcp.write(packet, sizeof(packet));
  }
}

void forwardMqttPublish(const String& topic, const String& payload, const bool retain,
                        const uint8_t qos, const int skipSlot = -1) {
  const String packet = mqttPacket(0x30U, topic, payload, qos, retain);
  for (uint8_t index = 0; index < kMaxMqttClients; ++index) {
    MqttSlot& slot = mqttSlots[index];
    if (!slot.connected || static_cast<int>(index) == skipSlot) continue;
    for (uint8_t subscription = 0; subscription < slot.subscriptionCount; ++subscription) {
      if (topicMatches(slot.subscriptions[subscription], topic)) {
        sendMqttPacket(slot, packet);
        break;
      }
    }
  }
}

void storeRetained(const String& topic, const String& payload, const uint8_t qos) {
  RetainedMessage* selected = nullptr;
  for (RetainedMessage& message : retainedMessages) {
    if (message.used && topic == message.topic) {
      selected = &message;
      break;
    }
    if (!message.used && selected == nullptr) selected = &message;
  }
  if (selected == nullptr) selected = &retainedMessages[0];
  if (payload.isEmpty()) {
    *selected = RetainedMessage{};
    return;
  }
  selected->used = true;
  selected->qos = qos;
  copyText(selected->topic, topic);
  copyText(selected->payload, payload);
}

void publishEnergySync(DeviceRecord& device, const bool reset = false) {
  if (!device.used) return;
  const String base = String("smartplug/") + device.id;
  const String payload = String("{\"device_id\":\"") + device.id +
                         "\",\"energy_wh\":" + jsonNumber(device.energyWh, 3) +
                         ",\"recorded_at_ms\":" + String(millis()) +
                         ",\"reset\":" + (reset ? "true" : "false") + "}";
  forwardMqttPublish(base + "/sync/energy", payload, false, 0U);
}

String jsonField(const String& body, const char* key) {
  JsonDocument document;
  if (deserializeJson(document, body)) return String();
  const JsonVariant value = document[key];
  return value.is<const char*>() ? String(value.as<const char*>()) : String();
}

void handleRelayAcknowledgement(DeviceRecord& device, const String& payload) {
  JsonDocument document;
  if (deserializeJson(document, payload)) return;
  const bool accepted = document["accepted"] | false;
  const String state = document["state"] | "";
  if (state == "on" || state == "off") {
    copyText(device.relayState, state);
    // A timer configured while the relay was off is armed, not running. Start it only
    // after the device itself confirms that the relay is on.
    if (state == "on" && device.timerDeadlineUtc == 0U && device.timerDurationSeconds > 0U) {
      const uint32_t now = currentUtc();
      if (now != 0U) {
        device.timerDeadlineUtc = now + device.timerDurationSeconds;
        device.timerDurationSeconds = 0U;
        indexDirty = true;
      }
    }
  }
  if (strcmp(device.commandStatus, "queued") != 0) return;
  if (accepted && state == device.commandState) {
    copyLiteral(device.commandStatus, "completed");
  } else {
    copyLiteral(device.commandStatus, "rejected");
  }
  device.commandResolvedAtMs = millis();
}

void handleAllParameters(DeviceRecord& device, const String& payload) {
  JsonDocument document;
  if (deserializeJson(document, payload)) return;
  const JsonVariantConst voltage = document["voltage_v"];
  const JsonVariantConst current = document["current_a"];
  const JsonVariantConst activePower = document["active_power_w"];
  const JsonVariantConst apparentPower = document["apparent_power_va"];
  const JsonVariantConst powerFactor = document["power_factor"];
  const JsonVariantConst energy = document["energy_wh"];
  if (!voltage.is<float>() || !current.is<float>() || !activePower.is<float>() ||
      !apparentPower.is<float>() || !powerFactor.is<float>() || !energy.is<float>()) return;
  const float candidateEnergy = energy.as<float>();
  if (!isfinite(candidateEnergy) || candidateEnergy < 0.0F || candidateEnergy > 1000000.0F) return;
  device.voltageV = voltage.as<float>();
  device.currentA = current.as<float>();
  device.activePowerW = activePower.as<float>();
  device.apparentPowerVa = apparentPower.as<float>();
  device.powerFactor = powerFactor.as<float>();
  // During a server-authorized reset, ignore stale pre-reset telemetry until the
  // SmartPlug reports its newly reset counter. This keeps the server at zero
  // instead of briefly resurrecting the old total.
  if (device.awaitingEnergyReset) {
    if (candidateEnergy <= 1.0F) {
      device.energyWh = candidateEnergy;
      device.awaitingEnergyReset = false;
    }
  } else {
    // Normal telemetry is cumulative. A restart or old retained record cannot
    // lower the server-owned counter.
    device.energyWh = max(device.energyWh, candidateEnergy);
  }
  device.calibrated = document["calibrated"] | false;
  device.online = true;
  device.lastSeenMs = millis();
  device.lastSeenUtc = currentUtc();
  addAggregateSample(device);
  indexDirty = true;
}

void handleSingleMeasurement(DeviceRecord& device, const String& suffix,
                             const String& payload) {
  JsonDocument document;
  if (deserializeJson(document, payload)) return;
  const JsonVariantConst value = document["value"];
  if (!value.is<float>()) return;
  const float measurement = value.as<float>();
  if (!isfinite(measurement)) return;
  if (suffix == "measurement/voltage") device.voltageV = measurement;
  else if (suffix == "measurement/current") device.currentA = measurement;
  else if (suffix == "measurement/active-power") device.activePowerW = measurement;
  else if (suffix == "measurement/apparent-power") device.apparentPowerVa = measurement;
  else if (suffix == "measurement/power-factor") device.powerFactor = measurement;
  else if (suffix == "measurement/energy") {
    if (measurement < 0.0F || measurement > 1000000.0F) return;
    if (device.awaitingEnergyReset) {
      if (measurement <= 1.0F) {
        device.energyWh = measurement;
        device.awaitingEnergyReset = false;
      }
    } else {
      device.energyWh = max(device.energyWh, measurement);
    }
    indexDirty = true;
  } else {
    return;
  }
  // Individual topics update the current device view. The allparameters topic
  // is the one atomic sample used for one-minute aggregation.
  device.online = true;
  device.lastSeenMs = millis();
  device.lastSeenUtc = currentUtc();
}

void handleMqttApplicationMessage(const String& topic, const String& payload) {
  if (!topic.startsWith("smartplug/")) return;
  const int idStart = strlen("smartplug/");
  const int separator = topic.indexOf('/', idStart);
  if (separator < 0) return;
  const String deviceId = topic.substring(idStart, separator);
  DeviceRecord* device = findDevice(deviceId, true);
  if (device == nullptr) return;
  const String suffix = topic.substring(separator + 1);
  if (suffix == "measurement/allparameters") {
    handleAllParameters(*device, payload);
  } else if (suffix.startsWith("measurement/")) {
    handleSingleMeasurement(*device, suffix, payload);
  } else if (suffix == "availability") {
    const String state = payload;
    device->online = state == "online";
    device->lastSeenMs = millis();
    device->lastSeenUtc = currentUtc();
    if (device->online) publishEnergySync(*device);
  } else if (suffix == "state") {
    const String relay = jsonField(payload, "relay");
    if (relay == "on" || relay == "off" || relay == "unknown") {
      copyText(device->relayState, relay);
      if (relay == "on" && device->timerDeadlineUtc == 0U && device->timerDurationSeconds > 0U) {
        const uint32_t now = currentUtc();
        if (now != 0U) {
          device->timerDeadlineUtc = now + device->timerDurationSeconds;
          device->timerDurationSeconds = 0U;
        }
      }
      indexDirty = true;
    }
  } else if (suffix == "ack/relay") {
    handleRelayAcknowledgement(*device, payload);
    indexDirty = true;
  }
}

void publishBrokerMessage(const String& topic, const String& payload,
                          const bool retain, const uint8_t qos,
                          const int skipSlot = -1) {
  if (retain) storeRetained(topic, payload, qos);
  handleMqttApplicationMessage(topic, payload);
  forwardMqttPublish(topic, payload, retain, qos, skipSlot);
}

bool slotOwnsTopic(const MqttSlot& slot, const String& topic) {
  return strlen(slot.deviceId) > 0 && topic.startsWith(String("smartplug/") + slot.deviceId + "/");
}

bool deviceMayPublish(const MqttSlot& slot, const String& topic) {
  if (!slotOwnsTopic(slot, topic)) return false;
  const String suffix = topic.substring((String("smartplug/") + slot.deviceId + "/").length());
  return suffix == "measurement/voltage" || suffix == "measurement/current" ||
         suffix == "measurement/active-power" || suffix == "measurement/apparent-power" ||
         suffix == "measurement/power-factor" || suffix == "measurement/energy" ||
         suffix == "measurement/allparameters" || suffix == "state" ||
         suffix == "availability" || suffix == "ack/relay" || suffix == "telemetry";
}

bool deviceMaySubscribe(const MqttSlot& slot, const String& filter) {
  if (!slotOwnsTopic(slot, filter)) return false;
  const String suffix = filter.substring((String("smartplug/") + slot.deviceId + "/").length());
  return suffix == "cmd/relay" || suffix == "cmd/factory-reset" || suffix == "sync/energy";
}

bool decodeMqttString(const uint8_t* data, const size_t length, size_t& offset,
                      String& value, const size_t maximumLength) {
  if (offset + 2U > length) return false;
  const uint16_t textLength = (static_cast<uint16_t>(data[offset]) << 8U) | data[offset + 1U];
  offset += 2U;
  if (textLength > maximumLength || offset + textLength > length) return false;
  value = "";
  value.reserve(textLength);
  for (uint16_t i = 0; i < textLength; ++i) value += static_cast<char>(data[offset + i]);
  offset += textLength;
  return true;
}

void removeSlot(const uint8_t index, const bool unexpected) {
  MqttSlot& slot = mqttSlots[index];
  if (!slot.occupied) return;
  if (unexpected && slot.connected && slot.willEnabled) {
    publishBrokerMessage(slot.willTopic, slot.willPayload, slot.willRetain, slot.willQos,
                         static_cast<int>(index));
  }
  slot.tcp.stop();
  slot = MqttSlot{};
}

bool processConnect(MqttSlot& slot, const uint8_t* body, const size_t length) {
  size_t offset = 0;
  String protocol;
  if (!decodeMqttString(body, length, offset, protocol, 8) || protocol != "MQTT" ||
      offset + 4U > length || body[offset++] != 4U) {
    sendSimpleMqtt(slot, 0x20U, 0x01U);
    return false;
  }
  const uint8_t flags = body[offset++];
  const uint16_t keepAlive = (static_cast<uint16_t>(body[offset]) << 8U) | body[offset + 1U];
  offset += 2U;
  if ((flags & 0x01U) != 0U || (flags & 0x40U) != 0U && (flags & 0x80U) == 0U) {
    sendSimpleMqtt(slot, 0x20U, 0x02U);
    return false;
  }
  String clientId;
  if (!decodeMqttString(body, length, offset, clientId, 63)) {
    sendSimpleMqtt(slot, 0x20U, 0x02U);
    return false;
  }
  const bool willFlag = (flags & 0x04U) != 0U;
  String willTopic;
  String willPayload;
  if (willFlag && (!decodeMqttString(body, length, offset, willTopic, 127) ||
                   !decodeMqttString(body, length, offset, willPayload, 383))) {
    sendSimpleMqtt(slot, 0x20U, 0x02U);
    return false;
  }
  String username;
  String password;
  if ((flags & 0x80U) != 0U && !decodeMqttString(body, length, offset, username, 32)) {
    sendSimpleMqtt(slot, 0x20U, 0x04U);
    return false;
  }
  if ((flags & 0x40U) != 0U && !decodeMqttString(body, length, offset, password, 64)) {
    sendSimpleMqtt(slot, 0x20U, 0x04U);
    return false;
  }
  if (!mqttCredentialsConfigured() || !constantTimeEquals(username, settings.brokerUsername) ||
      !constantTimeEquals(password, settings.brokerPassword)) {
    sendSimpleMqtt(slot, 0x20U, 0x04U);
    return false;
  }
  slot.connected = true;
  slot.cleanSession = (flags & 0x02U) != 0U;
  slot.keepAliveSeconds = keepAlive == 0U ? 30U : keepAlive;
  slot.lastRxAtMs = millis();
  copyText(slot.clientId, clientId);
  const String deviceId = clientId.startsWith("SmartPlug-")
                            ? clientId.substring(strlen("SmartPlug-")) : String();
  if (!validDeviceId(deviceId)) {
    sendSimpleMqtt(slot, 0x20U, 0x02U);
    return false;
  }
  // One connected identity owns one base topic. A reconnect replaces the old
  // socket without publishing its will, as this is a normal client takeover.
  for (uint8_t index = 0; index < kMaxMqttClients; ++index) {
    MqttSlot& existing = mqttSlots[index];
    if (&existing != &slot && existing.occupied && existing.connected &&
        deviceId == existing.deviceId) {
      removeSlot(index, false);
    }
  }
  copyText(slot.deviceId, deviceId);
  slot.willEnabled = willFlag;
  slot.willRetain = (flags & 0x20U) != 0U;
  slot.willQos = (flags >> 3U) & 0x03U;
  if (willFlag) {
    copyText(slot.willTopic, willTopic);
    copyText(slot.willPayload, willPayload);
  }
  sendSimpleMqtt(slot, 0x20U, 0U);
  return true;
}

void sendSubscriptionAck(MqttSlot& slot, const uint16_t packetId, const uint8_t count) {
  String packet;
  packet += static_cast<char>(0x90U);
  packet += static_cast<char>(2U + count);
  packet += static_cast<char>((packetId >> 8U) & 0xFFU);
  packet += static_cast<char>(packetId & 0xFFU);
  for (uint8_t i = 0; i < count; ++i) packet += static_cast<char>(0U);
  sendMqttPacket(slot, packet);
}

void sendRetainedForSubscription(MqttSlot& slot, const String& filter) {
  for (const RetainedMessage& message : retainedMessages) {
    if (message.used && topicMatches(filter, message.topic)) {
      sendMqttPacket(slot, mqttPacket(0x30U, message.topic, message.payload,
                                      message.qos, true));
    }
  }
}

void processSubscribe(MqttSlot& slot, const uint8_t* body, const size_t length) {
  if (length < 5U) return;
  size_t offset = 0;
  const uint16_t packetId = (static_cast<uint16_t>(body[offset]) << 8U) | body[offset + 1U];
  offset += 2U;
  const uint8_t firstSubscription = slot.subscriptionCount;
  uint8_t granted = 0;
  while (offset < length) {
    String filter;
    if (!decodeMqttString(body, length, offset, filter, 127) || offset >= length) break;
    ++offset;  // Requested QoS; this broker grants QoS 0.
    if (deviceMaySubscribe(slot, filter) && slot.subscriptionCount < kMaxSubscriptions) {
      copyText(slot.subscriptions[slot.subscriptionCount++], filter);
      ++granted;
    }
  }
  if (granted > 0U) {
    sendSubscriptionAck(slot, packetId, granted);
    for (uint8_t index = firstSubscription; index < slot.subscriptionCount; ++index) {
      sendRetainedForSubscription(slot, slot.subscriptions[index]);
    }
  }
}

void processPublish(MqttSlot& slot, const uint8_t header, const uint8_t* body,
                    const size_t length) {
  size_t offset = 0;
  String topic;
  if (!decodeMqttString(body, length, offset, topic, 127)) return;
  const uint8_t qos = (header >> 1U) & 0x03U;
  uint16_t packetId = 0;
  if (qos == 1U) {
    if (offset + 2U > length) return;
    packetId = (static_cast<uint16_t>(body[offset]) << 8U) | body[offset + 1U];
    offset += 2U;
  } else if (qos > 1U) {
    return;
  }
  if (offset > length || length - offset > 383U) return;
  if (!deviceMayPublish(slot, topic)) {
    slot.tcp.stop();
    return;
  }
  String payload;
  payload.reserve(length - offset);
  for (size_t i = offset; i < length; ++i) payload += static_cast<char>(body[i]);
  publishBrokerMessage(topic, payload, (header & 0x01U) != 0U, qos, -1);
  if (qos == 1U) {
    uint8_t ack[4] = {0x40U, 0x02U, static_cast<uint8_t>(packetId >> 8U),
                      static_cast<uint8_t>(packetId & 0xFFU)};
    slot.tcp.write(ack, sizeof(ack));
  }
}

void processMqttPacket(const uint8_t index, const uint8_t header,
                       const uint8_t* body, const size_t length) {
  MqttSlot& slot = mqttSlots[index];
  slot.lastRxAtMs = millis();
  const uint8_t type = header & 0xF0U;
  if (!slot.connected) {
    if (type != 0x10U || !processConnect(slot, body, length)) removeSlot(index, false);
    return;
  }
  if (type == 0x30U) {
    processPublish(slot, header, body, length);
  } else if (type == 0x80U && (header & 0x0FU) == 0x02U) {
    processSubscribe(slot, body, length);
  } else if (type == 0xC0U) {
    sendSimpleMqtt(slot, 0xD0U);
  } else if (type == 0xE0U) {
    removeSlot(index, false);
  }
}

bool consumeMqttPacket(const uint8_t index) {
  MqttSlot& slot = mqttSlots[index];
  if (slot.rxLength < 2U) return false;
  size_t offset = 1U;
  size_t remaining = 0U;
  size_t multiplier = 1U;
  uint8_t encoded = 0;
  do {
    if (offset >= slot.rxLength || offset > 4U) return false;
    encoded = slot.rx[offset++];
    remaining += (encoded & 0x7FU) * multiplier;
    multiplier *= 128U;
  } while ((encoded & 0x80U) != 0U);
  if (remaining > kMqttInputBytes - offset) {
    removeSlot(index, true);
    return false;
  }
  const size_t fullLength = offset + remaining;
  if (slot.rxLength < fullLength) return false;
  processMqttPacket(index, slot.rx[0], slot.rx + offset, remaining);
  if (!slot.occupied) return false;
  memmove(slot.rx, slot.rx + fullLength, slot.rxLength - fullLength);
  slot.rxLength -= fullLength;
  return true;
}

void serviceMqttBroker() {
  WiFiClient incoming = mqttServer.available();
  if (incoming) {
    int freeIndex = -1;
    for (uint8_t i = 0; i < kMaxMqttClients; ++i) {
      if (!mqttSlots[i].occupied) { freeIndex = i; break; }
    }
    if (freeIndex < 0) {
      incoming.stop();
    } else {
      mqttSlots[freeIndex] = MqttSlot{};
      mqttSlots[freeIndex].tcp = incoming;
      mqttSlots[freeIndex].tcp.setNoDelay(true);
      mqttSlots[freeIndex].occupied = true;
      mqttSlots[freeIndex].lastRxAtMs = millis();
    }
  }
  const uint32_t now = millis();
  for (uint8_t index = 0; index < kMaxMqttClients; ++index) {
    MqttSlot& slot = mqttSlots[index];
    if (!slot.occupied) continue;
    while (slot.tcp.available() && slot.rxLength < kMqttInputBytes) {
      const int value = slot.tcp.read();
      if (value < 0) break;
      slot.rx[slot.rxLength++] = static_cast<uint8_t>(value);
      slot.lastRxAtMs = now;
    }
    if (slot.rxLength >= kMqttInputBytes) {
      removeSlot(index, true);
      continue;
    }
    while (slot.occupied && consumeMqttPacket(index)) {}
    if (!slot.occupied) continue;
    if (!slot.tcp.connected()) {
      removeSlot(index, true);
      continue;
    }
    const uint32_t allowance = static_cast<uint32_t>(slot.keepAliveSeconds) * 1500UL;
    if (slot.connected && allowance > 0U && now - slot.lastRxAtMs > allowance) {
      removeSlot(index, true);
    }
  }
}

void expireCommands() {
  const uint32_t now = millis();
  for (DeviceRecord& device : devices) {
    if (device.used && strcmp(device.commandStatus, "queued") == 0 &&
        now - device.commandCreatedAtMs > kCommandTimeoutMs) {
      copyLiteral(device.commandStatus, "timeout");
      device.commandResolvedAtMs = now;
    }
  }
}

void serviceTimers() {
  const uint32_t now = currentUtc();
  if (now == 0U) return;
  for (DeviceRecord& device : devices) {
    if (!device.used || device.timerDeadlineUtc == 0U || now < device.timerDeadlineUtc) continue;
    if (!device.online || strcmp(device.commandStatus, "queued") == 0) continue;
    ++commandCounter;
    const String commandId = String("timer-") + String(millis()) + "-" + String(commandCounter);
    copyText(device.commandId, commandId); copyLiteral(device.commandState, "off");
    copyLiteral(device.commandStatus, "queued"); device.commandCreatedAtMs = millis();
    device.commandResolvedAtMs = 0U; device.timerDeadlineUtc = 0U; indexDirty = true;
    publishBrokerMessage(String("smartplug/") + device.id + "/cmd/relay", "off", false, 0U);
  }
}

void serviceSchedules() {
  const uint32_t now = currentUtc();
  if (now == 0U) return;
  for (DeviceRecord& device : devices) {
    if (!device.used) continue;
    // A brief Wi-Fi/MQTT interruption must not silently discard a schedule.
    // Keep the newest due command for five minutes, then discard it rather
    // than applying a stale appliance action much later.
    if (device.pendingScheduleDueUtc != 0U) {
      if (now > device.pendingScheduleDueUtc + kScheduleCatchUpSeconds) {
        device.pendingScheduleDueUtc = 0U;
        indexDirty = true;
      } else if (device.online && strcmp(device.commandStatus, "queued") != 0) {
        ++commandCounter;
        const String commandId = String("schedule-catchup-") + String(millis()) + "-" + String(commandCounter);
        copyText(device.commandId, commandId);
        copyLiteral(device.commandState, device.pendingScheduleTurnOn ? "on" : "off");
        copyLiteral(device.commandStatus, "queued");
        device.commandCreatedAtMs = millis();
        device.commandResolvedAtMs = 0U;
        publishBrokerMessage(String("smartplug/") + device.id + "/cmd/relay",
                             device.pendingScheduleTurnOn ? "on" : "off", false, 0U);
        device.pendingScheduleDueUtc = 0U;
        indexDirty = true;
      }
    }
    if (!device.scheduleEnabled || device.scheduleCount == 0U) continue;
    const int64_t minuteKey = static_cast<int64_t>(now / 60U);
    if (minuteKey == device.lastScheduleMinuteUtc) continue;
    device.lastScheduleMinuteUtc = minuteKey;
    const time_t localEpoch = static_cast<time_t>(now) +
                              static_cast<time_t>(device.timezoneOffsetMinutes) * 60;
    tm local = {};
    gmtime_r(&localEpoch, &local);
    int selected = -1;
    for (uint8_t index = 0; index < device.scheduleCount; ++index) {
      const auto& entry = device.schedules[index];
      if (entry.hour == local.tm_hour && entry.minute == local.tm_min) selected = index;
    }
    if (selected < 0) continue;
    const bool turnOn = device.schedules[selected].turnOn;
    if (!device.online || strcmp(device.commandStatus, "queued") == 0) {
      device.pendingScheduleDueUtc = now;
      device.pendingScheduleTurnOn = turnOn;
      indexDirty = true;
      continue;
    }
    ++commandCounter;
    const String commandId = String("schedule-") + String(millis()) + "-" + String(commandCounter);
    copyText(device.commandId, commandId);
    copyLiteral(device.commandState, turnOn ? "on" : "off");
    copyLiteral(device.commandStatus, "queued");
    device.commandCreatedAtMs = millis();
    device.commandResolvedAtMs = 0U;
    publishBrokerMessage(String("smartplug/") + device.id + "/cmd/relay", turnOn ? "on" : "off", false, 0U);
  }
}

void serviceStorage() {
  // A card may be inserted after the ESP32 is powered. Keep retrying so the
  // setup/status endpoint becomes useful without requiring a firmware upload
  // or power cycle after each wiring adjustment.
  if (!sdReady) {
    if (millis() - lastSdMountAttemptMs >= 5000UL) beginStorage();
    return;
  }
  if (sdReady && indexDirty && millis() - lastIndexFlushAtMs >= kIndexFlushIntervalMs) {
    saveDeviceIndex();
  }
}

void handleHealth() {
  sendJson(200, String("{\"server_version\":\"") + kServerVersion +
                "\",\"web_server_started\":true,\"mqtt_broker_started\":true,\"sd_ready\":" +
                (sdReady ? "true" : "false") + ",\"time_synchronized\":" +
                (timeIsSynchronized() ? "true" : "false") + "}");
}

void handleStatus() {
  if (!requestIsAuthorized()) return;
  sendJson(200, String("{\"server_version\":\"") + kServerVersion +
                "\",\"configured\":" + (settingsReady ? "true" : "false") +
                ",\"mqtt_broker\":{\"port\":1883,\"credentials_configured\":" +
                (mqttCredentialsConfigured() ? "true" : "false") + "},\"storage\":{\"sd_ready\":" +
                (sdReady ? "true" : "false") + "},\"network\":{\"station_connected\":" +
                (WiFi.status() == WL_CONNECTED ? "true" : "false") +
                ",\"station_ip\":\"" + stationIpText() + "\",\"access_point_ssid\":\"" +
                kSetupApSsid + "\",\"access_point_ip\":\"" + WiFi.softAPIP().toString() +
                "\"},\"devices\":" + String(deviceCount()) + "}");
}

void handleDevices() {
  if (!requestIsAuthorized()) return;
  String response = "{\"devices\":[";
  bool first = true;
  for (const DeviceRecord& device : devices) {
    if (!device.used) continue;
    if (!first) response += ',';
    response += deviceSummaryJson(device);
    first = false;
  }
  response += "]}";
  sendJson(200, response);
}

String requestPathPart(const uint8_t index) {
  const String uri = http.uri();
  int start = 0;
  uint8_t part = 0;
  while (start < uri.length()) {
    const int slash = uri.indexOf('/', start + 1);
    if (part == index) return uri.substring(start, slash < 0 ? uri.length() : slash);
    if (slash < 0) break;
    start = slash;
    ++part;
  }
  return String();
}

uint32_t historyResolutionSeconds(const String& resolution) {
  if (resolution == "1m") return 60U;
  if (resolution == "5m") return 300U;
  if (resolution == "30m") return 1800U;
  if (resolution == "1h") return 3600U;
  if (resolution == "1d") return 86400U;
  return 0U;
}

void handleHistory(const DeviceRecord& device) {
  if (!sdReady) { sendError(503, "sd_card_unavailable"); return; }
  const String resolutionText = http.hasArg("resolution") ? http.arg("resolution") : "1m";
  const uint32_t resolution = historyResolutionSeconds(resolutionText);
  if (resolution == 0U) { sendError(400, "invalid_resolution"); return; }
  const uint32_t from = http.hasArg("from") ? strtoul(http.arg("from").c_str(), nullptr, 10) : 0U;
  const uint32_t to = http.hasArg("to") ? strtoul(http.arg("to").c_str(), nullptr, 10) : UINT32_MAX;
  if (!SD.exists(kHistoryPath)) {
    sendJson(200, String("{\"device_id\":\"") + device.id + "\",\"resolution\":\"" +
                  resolutionText + "\",\"points\":[]}");
    return;
  }
  http.sendHeader("Access-Control-Allow-Origin", "*");
  http.sendHeader("Access-Control-Allow-Headers", "Authorization, X-API-Key, Content-Type");
  http.sendHeader("Cache-Control", "no-store");
  http.setContentLength(CONTENT_LENGTH_UNKNOWN);
  http.send(200, "application/json", "");
  http.sendContent(String("{\"device_id\":\"") + device.id + "\",\"resolution\":\"" +
                   resolutionText + "\",\"energy_reset_utc\":" +
                   String(device.lastEnergyResetUtc) + ",\"points\":[");
  File file = SD.open(kHistoryPath, FILE_READ);
  bool sent = false;
  uint32_t bucket = 0U;
  uint16_t count = 0U;
  float sumV = 0.0F, sumA = 0.0F, sumW = 0.0F, sumVa = 0.0F, sumPf = 0.0F, lastEnergy = 0.0F;
  auto flush = [&]() {
    if (count == 0U) return;
    if (sent) http.sendContent(",");
    const float divisor = static_cast<float>(count);
    http.sendContent(String("{\"timestamp_utc_ms\":") + String(static_cast<uint64_t>(bucket) * 1000ULL) +
      ",\"voltage_v\":" + jsonNumber(sumV / divisor, 3) +
      ",\"current_a\":" + jsonNumber(sumA / divisor, 4) +
      ",\"active_power_w\":" + jsonNumber(sumW / divisor, 2) +
      ",\"apparent_power_va\":" + jsonNumber(sumVa / divisor, 2) +
      ",\"power_factor\":" + jsonNumber(sumPf / divisor, 3) +
      ",\"energy_wh\":" + jsonNumber(lastEnergy, 3) + "}");
    sent = true;
  };
  while (file && file.available()) {
    const String line = file.readStringUntil('\n');
    char id[20] = {};
    unsigned long timestamp = 0;
    float voltage = 0, current = 0, watt = 0, va = 0, pf = 0, energy = 0;
    if (sscanf(line.c_str(), "%lu,%19[^,],%f,%f,%f,%f,%f,%f", &timestamp, id,
               &voltage, &current, &watt, &va, &pf, &energy) != 8 || String(device.id) != id) continue;
    if (timestamp < from || timestamp > to) continue;
    const uint32_t nextBucket = static_cast<uint32_t>(timestamp) -
                                (static_cast<uint32_t>(timestamp) % resolution);
    if (count > 0U && nextBucket != bucket) {
      flush();
      count = 0U; sumV = sumA = sumW = sumVa = sumPf = 0.0F;
    }
    bucket = nextBucket;
    sumV += voltage; sumA += current; sumW += watt; sumVa += va; sumPf += pf;
    // Samples are chronological. Keeping the final reading lets a true reset
    // boundary appear in the requested graph instead of being hidden by max().
    lastEnergy = energy;
    if (count < UINT16_MAX) ++count;
  }
  if (file) file.close();
  flush();
  http.sendContent("]}");
}

void handleDeviceRoute() {
  if (!requestIsAuthorized()) return;
  const String uri = http.uri();
  const String prefix = "/api/v1/devices/";
  const int start = prefix.length();
  const int separator = uri.indexOf('/', start);
  const String id = uri.substring(start, separator < 0 ? uri.length() : separator);
  DeviceRecord* device = findDevice(id, false);
  if (device == nullptr) { sendError(404, "device_not_found"); return; }
  const String action = separator < 0 ? "" : uri.substring(separator + 1);
  if (http.method() == HTTP_GET && action.isEmpty()) {
    sendJson(200, deviceSummaryJson(*device));
  } else if (http.method() == HTTP_GET && action == "latest") {
    sendJson(200, deviceLatestJson(*device));
  } else if (http.method() == HTTP_GET && action == "energy") {
    sendJson(200, String("{\"device_id\":\"") + device->id +
                  "\",\"energy_wh\":" + jsonNumber(device->energyWh, 3) +
                  ",\"source\":\"server_sd\",\"recorded_at_ms\":" +
                  String(static_cast<uint64_t>(device->lastSeenUtc) * 1000ULL) + "}");
  } else if (http.method() == HTTP_GET && action == "history") {
    handleHistory(*device);
  } else if (http.method() == HTTP_POST && action == "energy/reset") {
    JsonDocument document;
    if (!http.hasArg("plain") || deserializeJson(document, http.arg("plain")) ||
        String(document["confirm_1"] | "") != "RESET_ENERGY" ||
        String(document["confirm_2"] | "") != "RESET_ENERGY" ||
        String(document["confirm_3"] | "") != "RESET_ENERGY") {
      sendError(400, "triple_confirmation_required"); return;
    }
    const float previousEnergyWh = device->energyWh;
    const uint32_t resetUtc = currentUtc();
    device->energyWh = 0.0F;
    device->lastEnergyResetUtc = resetUtc;
    device->awaitingEnergyReset = true;
    appendEnergyResetAudit(*device, previousEnergyWh, resetUtc);
    indexDirty = true;
    publishEnergySync(*device, true);
    sendJson(202, String("{\"device_id\":\"") + device->id +
                    "\",\"result\":\"energy_reset_queued\",\"previous_energy_wh\":" +
                    jsonNumber(previousEnergyWh, 3) + ",\"reset_utc\":" + String(resetUtc) + "}");
  } else if (http.method() == HTTP_POST && action == "factory-reset") {
    JsonDocument document;
    if (!http.hasArg("plain") || deserializeJson(document, http.arg("plain")) ||
        String(document["confirm_1"] | "") != "FACTORY_RESET" ||
        String(document["confirm_2"] | "") != "FACTORY_RESET" ||
        String(document["confirm_3"] | "") != "FACTORY_RESET") {
      sendError(400, "triple_confirmation_required"); return;
    }
    if (!device->online) { sendError(409, "device_offline"); return; }
    publishBrokerMessage(String("smartplug/") + device->id + "/cmd/factory-reset", "FACTORY_RESET", false, 0U);
    sendJson(202, String("{\"device_id\":\"") + device->id + "\",\"result\":\"factory_reset_queued\"}");
  } else if (http.method() == HTTP_GET && action == "schedule") {
    sendJson(200, deviceScheduleJson(*device));
  } else if (http.method() == HTTP_POST && action == "schedule") {
    if (!sdReady) { sendError(503, "storage_unavailable"); return; }
    JsonDocument document;
    if (!http.hasArg("plain") || deserializeJson(document, http.arg("plain"))) {
      sendError(400, "invalid_schedule_payload"); return;
    }
    const String command = document["action"] | "";
    if (!document["timezone_offset_minutes"].isNull()) {
      const int offset = document["timezone_offset_minutes"] | 0;
      if (offset < -720 || offset > 840) { sendError(400, "invalid_timezone_offset"); return; }
      device->timezoneOffsetMinutes = static_cast<int16_t>(offset);
    }
    if (command == "set_enabled") {
      device->scheduleEnabled = document["enabled"] | false;
    } else if (command == "add") {
      const int hour = document["hour"] | -1;
      const int minute = document["minute"] | -1;
      const String state = document["state"] | "";
      const String event = document["event"] | "";
      if (hour < 0 || hour > 23 || minute < 0 || minute > 59 ||
          (state != "on" && state != "off") || !validScheduleEvent(event)) {
        sendError(400, "invalid_schedule_entry"); return;
      }
      if (device->scheduleCount >= kMaxDailySchedules) { sendError(409, "schedule_full"); return; }
      auto& entry = device->schedules[device->scheduleCount++];
      entry.hour = static_cast<uint8_t>(hour);
      entry.minute = static_cast<uint8_t>(minute);
      entry.turnOn = state == "on";
      copyText(entry.event, event);
    } else if (command == "delete") {
      const int index = document["index"] | -1;
      if (index < 0 || index >= device->scheduleCount) { sendError(400, "invalid_schedule_index"); return; }
      for (uint8_t i = static_cast<uint8_t>(index); i + 1U < device->scheduleCount; ++i) {
        device->schedules[i] = device->schedules[i + 1U];
      }
      --device->scheduleCount;
    } else if (command == "move") {
      const int from = document["from"] | -1;
      const int to = document["to"] | -1;
      if (from < 0 || to < 0 || from >= device->scheduleCount || to >= device->scheduleCount) {
        sendError(400, "invalid_schedule_index"); return;
      }
      const auto moved = device->schedules[from];
      if (from < to) for (int i = from; i < to; ++i) device->schedules[i] = device->schedules[i + 1];
      if (from > to) for (int i = from; i > to; --i) device->schedules[i] = device->schedules[i - 1];
      device->schedules[to] = moved;
    } else {
      sendError(400, "invalid_schedule_action"); return;
    }
    indexDirty = true;
    if (!saveDeviceIndex()) { sendError(503, "storage_write_failed"); return; }
    sendJson(200, deviceScheduleJson(*device));
  } else if (http.method() == HTTP_GET && action == "timer") {
    const uint32_t now = currentUtc();
    const uint32_t remaining = device->timerDeadlineUtc > now && now != 0U
                                 ? device->timerDeadlineUtc - now : 0U;
    sendJson(200, String("{\"device_id\":\"") + device->id +
                  "\",\"armed_seconds\":" + String(device->timerDurationSeconds) +
                  ",\"deadline_utc\":" + String(device->timerDeadlineUtc) +
                  ",\"remaining_seconds\":" + String(remaining) + "}");
  } else if (http.method() == HTTP_POST && action == "timer") {
    if (!sdReady) { sendError(503, "storage_unavailable"); return; }
    JsonDocument document;
    if (!http.hasArg("plain") || deserializeJson(document, http.arg("plain"))) {
      sendError(400, "invalid_timer_payload"); return;
    }
    const String command = document["action"] | "apply";
    if (command == "reset") {
      device->timerDeadlineUtc = 0U;
      device->timerDurationSeconds = 0U;
      indexDirty = true;
      if (!saveDeviceIndex()) { sendError(503, "storage_write_failed"); return; }
      sendJson(200, String("{\"device_id\":\"") + device->id + "\",\"result\":\"timer_reset\"}");
      return;
    }
    const uint32_t days = document["days"] | 0U;
    const uint32_t hours = document["hours"] | 0U;
    const uint32_t minutes = document["minutes"] | 0U;
    const uint32_t seconds = document["seconds"] | 0U;
    if (hours > 23U || minutes > 59U || seconds > 59U) { sendError(400, "invalid_timer_duration"); return; }
    const uint64_t total = static_cast<uint64_t>(days) * 86400ULL +
                           static_cast<uint64_t>(hours) * 3600ULL +
                           static_cast<uint64_t>(minutes) * 60ULL + seconds;
    if (total == 0ULL || total > UINT32_MAX || !timeIsSynchronized()) {
      sendError(400, "invalid_or_unsynchronized_timer"); return;
    }
    if (strcmp(device->relayState, "on") == 0) {
      device->timerDeadlineUtc = currentUtc() + static_cast<uint32_t>(total);
      device->timerDurationSeconds = 0U;
    } else {
      device->timerDeadlineUtc = 0U;
      device->timerDurationSeconds = static_cast<uint32_t>(total);
    }
    indexDirty = true;
    if (!saveDeviceIndex()) { sendError(503, "storage_write_failed"); return; }
    sendJson(200, String("{\"device_id\":\"") + device->id +
                  "\",\"result\":\"timer_applied\",\"armed_seconds\":" +
                  String(device->timerDurationSeconds) + ",\"deadline_utc\":" +
                  String(device->timerDeadlineUtc) + "}");
  } else if (http.method() == HTTP_POST && action == "relay") {
    String state;
    JsonDocument document;
    if (http.hasArg("plain") && !deserializeJson(document, http.arg("plain"))) {
      state = document["state"] | "";
    } else if (http.hasArg("state")) {
      state = http.arg("state");
    }
    state.toLowerCase();
    if (state != "on" && state != "off") { sendError(400, "invalid_relay_state"); return; }
    if (!device->online) { sendError(409, "device_offline"); return; }
    if (strcmp(device->commandStatus, "queued") == 0) { sendError(409, "command_pending"); return; }
    ++commandCounter;
    const String commandId = String("cmd-") + String(millis()) + "-" + String(commandCounter);
    copyText(device->commandId, commandId);
    copyText(device->commandState, state);
    copyLiteral(device->commandStatus, "queued");
    device->commandCreatedAtMs = millis();
    device->commandResolvedAtMs = 0U;
    const String topic = String("smartplug/") + device->id + "/cmd/relay";
    publishBrokerMessage(topic, state, false, 0U);
    sendJson(202, String("{\"command_id\":\"") + device->commandId +
                  "\",\"status\":\"queued\",\"state\":\"" + state + "\"}");
  } else {
    sendError(404, "not_found");
  }
}

void handleCommandRoute() {
  if (!requestIsAuthorized()) return;
  const String prefix = "/api/v1/commands/";
  const String commandId = http.uri().substring(prefix.length());
  for (const DeviceRecord& device : devices) {
    if (device.used && commandId == device.commandId) {
      sendJson(200, String("{\"command_id\":\"") + device.commandId +
                    "\",\"device_id\":\"" + device.id +
                    "\",\"state\":\"" + device.commandState +
                    "\",\"status\":\"" + device.commandStatus + "\"}");
      return;
    }
  }
  sendError(404, "command_not_found");
}

void handleSetupStatus() {
  sendJson(200, String("{\"access_point\":{\"ssid\":\"") + kSetupApSsid +
                "\",\"ip\":\"" + WiFi.softAPIP().toString() +
                "\"},\"station\":{\"configured\":" +
                (strlen(settings.wifiSsid) > 0 ? "true" : "false") +
                ",\"connected\":" + (WiFi.status() == WL_CONNECTED ? "true" : "false") +
                ",\"ip\":\"" + stationIpText() + "\"},\"configured\":" +
                (settingsReady ? "true" : "false") + ",\"server_id\":\"" + serverId() +
                "\",\"mdns_host\":\"" + mdnsHost() + ".local\",\"mqtt_port\":" + String(kMqttPort) + ",\"sd_ready\":" +
                (sdReady ? "true" : "false") + "}");
}

void handleSetupPage() {
  const String html = R"HTML(<!doctype html><html lang="en"><meta name="viewport" content="width=device-width,initial-scale=1"><title>ServerSmartPlug setup</title><style>body{margin:0;background:#f5f7f8;color:#10202a;font:16px Arial,sans-serif}.wrap{max-width:680px;margin:36px auto;padding:28px;background:#fff;border:1px solid #c7d1d5;border-radius:12px}h1{margin:0 0 10px}p{line-height:1.5}label{display:block;font-weight:bold;margin-top:15px}input{box-sizing:border-box;width:100%;padding:11px;margin-top:6px;border:1px solid #71838b;border-radius:6px;font-size:16px}button{margin-top:22px;padding:12px 16px;border:0;border-radius:6px;background:#075e54;color:#fff;font-weight:bold;font-size:16px}.note{background:#eef5f3;padding:12px;border-left:4px solid #075e54}.small{font-size:13px;color:#253940}</style><main class="wrap"><h1>ServerSmartPlug setup</h1><p>Configure the Wi-Fi connection, the application API token, and the credentials used by SmartPlug MQTT clients. The setup access point remains available after saving.</p><p class="note">The application calls this server REST API. SmartPlug devices use the MQTT broker at port 1883.</p><form method="post" action="/setup"><label>Wi-Fi SSID</label><input name="ssid" maxlength="32" required><label>Wi-Fi password</label><input name="wifi_password" type="password" maxlength="63"><label>Application API token</label><input name="api_token" type="password" minlength="16" maxlength="64" required><label>MQTT username</label><input name="broker_username" minlength="3" maxlength="32" required><label>MQTT password</label><input name="broker_password" type="password" minlength="8" maxlength="63" required><button type="submit">Save configuration</button></form><p class="small">After saving, use the ServerSmartPlug station IP shown by <code>/setup/status</code> as the MQTT broker host in SmartPlug.</p></main></html>)HTML";
  http.send(200, "text/html; charset=utf-8", html);
}

void handleSetupSave() {
  const String ssid = http.arg("ssid");
  const String wifiPassword = http.arg("wifi_password");
  const String apiToken = http.arg("api_token");
  const String brokerUsername = http.arg("broker_username");
  const String brokerPassword = http.arg("broker_password");
  if (ssid.isEmpty() || ssid.length() > 32 || wifiPassword.length() > 63 ||
      !validCredentialText(apiToken, 16, 64) ||
      !validCredentialText(brokerUsername, 3, 32) ||
      !validCredentialText(brokerPassword, 8, 63)) {
    http.send(400, "text/plain", "Invalid setup data. Token and MQTT credentials must use printable ASCII characters.");
    return;
  }
  copyText(settings.wifiSsid, ssid);
  copyText(settings.wifiPassword, wifiPassword);
  copyText(settings.apiToken, apiToken);
  copyText(settings.brokerUsername, brokerUsername);
  copyText(settings.brokerPassword, brokerPassword);
  if (!saveSettings()) {
    http.send(500, "text/plain", "Configuration could not be saved.");
    return;
  }
  settingsReady = true;
  WiFi.disconnect(false, true);
  WiFi.begin(settings.wifiSsid, settings.wifiPassword);
  http.send(200, "text/html; charset=utf-8",
            "<p>Configuration saved. The server is connecting to Wi-Fi. Return to <a href='/setup'>setup</a> and read <code>/setup/status</code> for its station IP.</p>");
}

void handleNotFound() {
  if (http.method() == HTTP_OPTIONS) {
    http.sendHeader("Access-Control-Allow-Origin", "*");
    http.sendHeader("Access-Control-Allow-Headers", "Authorization, X-API-Key, Content-Type");
    http.sendHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
    http.send(204);
    return;
  }
  const String uri = http.uri();
  if (uri.startsWith("/api/v1/devices/")) { handleDeviceRoute(); return; }
  if (uri.startsWith("/api/v1/commands/")) { handleCommandRoute(); return; }
  sendError(404, "not_found");
}

void beginHttpApi() {
  const char* headers[] = {"Authorization", "X-API-Key", "Content-Type"};
  http.collectHeaders(headers, 3);
  http.on("/", HTTP_GET, handleSetupPage);
  http.on("/setup", HTTP_GET, handleSetupPage);
  http.on("/setup", HTTP_POST, handleSetupSave);
  http.on("/setup/status", HTTP_GET, handleSetupStatus);
  http.on("/health", HTTP_GET, handleHealth);
  http.on("/api/v1/status", HTTP_GET, handleStatus);
  http.on("/api/v1/devices", HTTP_GET, handleDevices);
  http.onNotFound(handleNotFound);
  http.begin();
}

}  // namespace

void setup() {
  Serial.begin(115200);
  delay(100);
  Serial.printf("INFO server_boot version=%s\\n", kServerVersion);
  loadSettings();
  startNetwork();
  beginStorage();
  beginHttpApi();
  mqttServer.begin();
  mqttServer.setNoDelay(true);
}

void loop() {
  http.handleClient();
  serviceMdns();
  configureTimeIfConnected();
  serviceMqttBroker();
  expireCommands();
  serviceTimers();
  serviceSchedules();
  serviceStorage();
  delay(2);
}
