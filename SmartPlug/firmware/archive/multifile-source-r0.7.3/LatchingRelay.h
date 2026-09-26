#pragma once

#include <Arduino.h>

class LatchingRelay {
 public:
  enum class CommandedState : uint8_t { kUnknown, kOn, kOff };

  LatchingRelay(uint8_t setPin, uint8_t resetPin, unsigned long pulseMs,
                unsigned long cooldownMs, bool actuationAllowed);

  void begin();
  void tick();
  bool requestOn();
  bool requestOff();
  bool busy() const;
  bool actuationAllowed() const;
  CommandedState commandedState() const;
  const char* stateText() const;

 private:
  enum class ActiveCoil : uint8_t { kNone, kSet, kReset };

  bool request(ActiveCoil coil, CommandedState resultingState);
  void driveBothLow();

  const uint8_t setPin_;
  const uint8_t resetPin_;
  const unsigned long pulseMs_;
  const unsigned long cooldownMs_;
  const bool actuationAllowed_;
  ActiveCoil activeCoil_ = ActiveCoil::kNone;
  CommandedState pendingState_ = CommandedState::kUnknown;
  CommandedState commandedState_ = CommandedState::kUnknown;
  unsigned long pulseStartedAt_ = 0;
  unsigned long lastPulseEndedAt_ = 0;
  bool hasPulsed_ = false;
};

