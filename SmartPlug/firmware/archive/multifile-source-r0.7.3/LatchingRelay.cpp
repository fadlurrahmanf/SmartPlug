#include "LatchingRelay.h"

LatchingRelay::LatchingRelay(const uint8_t setPin, const uint8_t resetPin,
                             const unsigned long pulseMs,
                             const unsigned long cooldownMs,
                             const bool actuationAllowed)
    : setPin_(setPin),
      resetPin_(resetPin),
      pulseMs_(pulseMs),
      cooldownMs_(cooldownMs),
      actuationAllowed_(actuationAllowed) {}

void LatchingRelay::begin() {
  // Write the inactive level before switching the GPIO direction. This avoids
  // a software-created coil pulse during setup.
  digitalWrite(setPin_, LOW);
  digitalWrite(resetPin_, LOW);
  pinMode(setPin_, OUTPUT);
  pinMode(resetPin_, OUTPUT);
  driveBothLow();
}

void LatchingRelay::tick() {
  if (activeCoil_ == ActiveCoil::kNone) {
    return;
  }

  if (millis() - pulseStartedAt_ < pulseMs_) {
    return;
  }

  driveBothLow();
  activeCoil_ = ActiveCoil::kNone;
  commandedState_ = pendingState_;
  pendingState_ = CommandedState::kUnknown;
  lastPulseEndedAt_ = millis();
  hasPulsed_ = true;
}

bool LatchingRelay::requestOn() {
  // Board verification: the coil named RESET in the source schematic closes
  // the load path. Keep the public command semantic aligned with the output:
  // ON connects the source to the load.
  return request(ActiveCoil::kReset, CommandedState::kOn);
}

bool LatchingRelay::requestOff() {
  // The source-schematic SET coil opens the load path on the tested board.
  return request(ActiveCoil::kSet, CommandedState::kOff);
}

bool LatchingRelay::request(const ActiveCoil coil,
                            const CommandedState resultingState) {
  if (!actuationAllowed_ || activeCoil_ != ActiveCoil::kNone) {
    return false;
  }
  if (hasPulsed_ && millis() - lastPulseEndedAt_ < cooldownMs_) {
    return false;
  }

  driveBothLow();
  digitalWrite(coil == ActiveCoil::kSet ? setPin_ : resetPin_, HIGH);
  activeCoil_ = coil;
  pendingState_ = resultingState;
  pulseStartedAt_ = millis();
  return true;
}

void LatchingRelay::driveBothLow() {
  digitalWrite(setPin_, LOW);
  digitalWrite(resetPin_, LOW);
}

bool LatchingRelay::busy() const {
  return activeCoil_ != ActiveCoil::kNone;
}

bool LatchingRelay::actuationAllowed() const { return actuationAllowed_; }

LatchingRelay::CommandedState LatchingRelay::commandedState() const {
  return commandedState_;
}

const char* LatchingRelay::stateText() const {
  if (busy()) {
    return "transitioning";
  }
  switch (commandedState_) {
    case CommandedState::kOn:
      return "on_commanded_unverified";
    case CommandedState::kOff:
      return "off_commanded_unverified";
    case CommandedState::kUnknown:
    default:
      return "unknown";
  }
}
