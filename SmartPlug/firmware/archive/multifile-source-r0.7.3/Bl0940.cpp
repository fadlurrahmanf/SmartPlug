#include "Bl0940.h"

Bl0940::Bl0940(const uint8_t rxPin, const uint8_t txPin)
    : serial_(rxPin, txPin) {}

void Bl0940::begin() {
  // BL0940 UART is fixed at 4800 baud. The vendor datasheet states that a
  // one-stop-bit MCU receiver/transmitter is accepted even though the IC's
  // nominal frame is documented with 1.5 stop bits.
  serial_.begin(4800);
}

bool Bl0940::requestFullPacket(const unsigned long timeoutMs) {
  if (busy() || pendingResult_ != PollResult::kNone) {
    return false;
  }

  while (serial_.available() > 0) {
    static_cast<void>(serial_.read());
  }

  received_ = 0;
  timeoutMs_ = timeoutMs;
  startedAt_ = millis();
  state_ = State::kReceiving;
  serial_.write(bl0940_protocol::kReadCommand);
  serial_.write(bl0940_protocol::kFullPacketAddress);
  return true;
}

void Bl0940::tick() {
  if (!busy()) {
    return;
  }

  while (serial_.available() > 0 &&
         received_ < bl0940_protocol::kPacketSize) {
    const int value = serial_.read();
    if (value >= 0) {
      packet_[received_++] = static_cast<uint8_t>(value);
    }
  }

  if (received_ == bl0940_protocol::kPacketSize) {
    bl0940_protocol::RawMeasurement measurement = {};
    if (!bl0940_protocol::decodePacket(packet_, measurement)) {
      finishInvalid();
      return;
    }

    pendingMeasurement_ = measurement;
    pendingResult_ = PollResult::kValid;
    state_ = State::kIdle;
    ++validPacketCount_;
    return;
  }

  if (millis() - startedAt_ >= timeoutMs_) {
    finishInvalid();
  }
}

Bl0940::PollResult Bl0940::takeResult(
    bl0940_protocol::RawMeasurement& measurement) {
  const PollResult result = pendingResult_;
  if (result == PollResult::kValid) {
    measurement = pendingMeasurement_;
  }
  pendingResult_ = PollResult::kNone;
  return result;
}

bool Bl0940::busy() const { return state_ == State::kReceiving; }

void Bl0940::finishInvalid() {
  state_ = State::kIdle;
  pendingResult_ = PollResult::kInvalid;
  ++invalidPacketCount_;
}

uint32_t Bl0940::validPacketCount() const { return validPacketCount_; }

uint32_t Bl0940::invalidPacketCount() const { return invalidPacketCount_; }
