#pragma once

#include <Arduino.h>
#include <SoftwareSerial.h>

#include "Bl0940Protocol.h"

class Bl0940 {
 public:
  enum class PollResult : uint8_t { kNone, kValid, kInvalid };

  Bl0940(uint8_t rxPin, uint8_t txPin);

  void begin();
  bool requestFullPacket(unsigned long timeoutMs = 180UL);
  void tick();
  PollResult takeResult(bl0940_protocol::RawMeasurement& measurement);
  bool busy() const;
  uint32_t validPacketCount() const;
  uint32_t invalidPacketCount() const;

 private:
  enum class State : uint8_t { kIdle, kReceiving };

  void finishInvalid();

  SoftwareSerial serial_;
  State state_ = State::kIdle;
  uint8_t packet_[bl0940_protocol::kPacketSize] = {};
  std::size_t received_ = 0;
  unsigned long startedAt_ = 0;
  unsigned long timeoutMs_ = 0;
  PollResult pendingResult_ = PollResult::kNone;
  bl0940_protocol::RawMeasurement pendingMeasurement_ = {};
  uint32_t validPacketCount_ = 0;
  uint32_t invalidPacketCount_ = 0;
};
