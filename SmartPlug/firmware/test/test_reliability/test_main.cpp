#include <unity.h>
#include <limits>
#include <initializer_list>
#include "SmartPlugReliability.h"
#include "SmartPlugMetering.h"
using namespace smartplug_reliability;
void setUp() {}
void tearDown() {}
void roundTrip() { auto r = energyRecord(42, 12345678.125); uint32_t s; double v; TEST_ASSERT_TRUE(decode(r,s,v)); TEST_ASSERT_EQUAL_UINT32(42,s); TEST_ASSERT_TRUE(v == 12345678.125); }
void rejectsEachCorruptedByte() { auto r = energyRecord(42,125); for (size_t i=0;i<sizeof(r);++i) {auto broken=r;broken.bytes[i]^=1;uint32_t s;double v;TEST_ASSERT_FALSE(decode(broken,s,v));} }
void rejectsInvalidValues() {for(double v:{-1.0,1.1e12,std::numeric_limits<double>::infinity(),std::numeric_limits<double>::quiet_NaN()}){auto r=energyRecord(1,v);uint32_t s;double x;TEST_ASSERT_FALSE(decode(r,s,x));} }
void acceptsBoundaries() {for(double v:{0.0,1.0e12}){auto r=energyRecord(1,v);uint32_t s;double x;TEST_ASSERT_TRUE(decode(r,s,x));} }
void selectsSurvivingSlot() { TEST_ASSERT_EQUAL_INT(-1,newestSlot(false,1,false,2));TEST_ASSERT_EQUAL_INT(0,newestSlot(true,1,false,2));TEST_ASSERT_EQUAL_INT(1,newestSlot(false,1,true,2));TEST_ASSERT_EQUAL_INT(1,newestSlot(true,1,true,2)); }
void sequenceWrap() {TEST_ASSERT_EQUAL_INT(1,newestSlot(true,0xffffffffU,true,0));TEST_ASSERT_EQUAL_INT(0,newestSlot(true,0,true,0xffffffffU));}
void truncatedWriteKeepsPrevious() {
  const auto old=energyRecord(20,400), next=energyRecord(21,500);
  for(size_t count=0;count<sizeof(next);++count){ EnergyRecord partial;std::memset(&partial,0xff,sizeof(partial));std::memcpy(&partial,&next,count);uint32_t sa=0,sb=0;double a=0,b=0;bool av=decode(old,sa,a),bv=decode(partial,sb,b);TEST_ASSERT_EQUAL_INT(0,newestSlot(av,sa,bv,sb));TEST_ASSERT_TRUE(a==400); }
}
void freshnessWrapAndUnavailable() {TEST_ASSERT_FALSE(fresh(false,0,0));TEST_ASSERT_TRUE(fresh(true,6000,1000));TEST_ASSERT_FALSE(fresh(true,6001,1000));TEST_ASSERT_TRUE(fresh(true,100,0xfffffff0U));}
void bootNeedsConsecutiveSamples() {BootRelayDecision d;TEST_ASSERT_EQUAL_INT(-1,d.observe(false));TEST_ASSERT_EQUAL_INT(-1,d.observe(true));TEST_ASSERT_EQUAL_INT(-1,d.observe(false));TEST_ASSERT_EQUAL_INT(-1,d.observe(false));TEST_ASSERT_EQUAL_INT(0,d.observe(false));}
void invalidResetsBootCandidate() {BootRelayDecision d;d.observe(true);d.observe(true);d.invalid();TEST_ASSERT_EQUAL_INT(-1,d.observe(true));TEST_ASSERT_EQUAL_INT(-1,d.observe(true));TEST_ASSERT_EQUAL_INT(1,d.observe(true));}
void strictCalibrationNumbers() {double v;TEST_ASSERT_TRUE(positiveNumber("12.5",v));for(const char* s:{"","abc","12junk","nan","inf","0","-1","1e500"})TEST_ASSERT_FALSE(positiveNumber(s,v));}
void syncNeverDecreases() {smartplug_metering::EnergyIntegrator e;e.restoreEnergyWh(100);TEST_ASSERT_FALSE(e.syncEnergyWh(99));TEST_ASSERT_FALSE(e.syncEnergyWh(100));TEST_ASSERT_FALSE(e.syncEnergyWh(NAN));TEST_ASSERT_TRUE(e.syncEnergyWh(101));TEST_ASSERT_TRUE(e.energyWh()==101);}
void syncPreservesCfBaseline() {smartplug_metering::EnergyIntegrator e;smartplug_metering::Calibration c={.01F,.01F,.01F,.01F};bl0940_protocol::RawMeasurement r={};r.cfCount=10;e.update(r,c,1000);e.syncEnergyWh(100);r.cfCount=11;auto sample=e.update(r,c,1500);TEST_ASSERT_TRUE(sample.energyWhSinceBoot>100.009&&sample.energyWhSinceBoot<100.011);}
int main(){UNITY_BEGIN();RUN_TEST(roundTrip);RUN_TEST(rejectsEachCorruptedByte);RUN_TEST(rejectsInvalidValues);RUN_TEST(acceptsBoundaries);RUN_TEST(selectsSurvivingSlot);RUN_TEST(sequenceWrap);RUN_TEST(truncatedWriteKeepsPrevious);RUN_TEST(freshnessWrapAndUnavailable);RUN_TEST(bootNeedsConsecutiveSamples);RUN_TEST(invalidResetsBootCandidate);RUN_TEST(strictCalibrationNumbers);RUN_TEST(syncNeverDecreases);RUN_TEST(syncPreservesCfBaseline);return UNITY_END();}
