#include "SmartPlugApi.h"

#include <Arduino.h>
#include <Crypto.h>
#include <EEPROM.h>
#include <ESP8266WiFi.h>
#include <cstring>
extern "C" {
#include <user_interface.h>
}

#include "BuildConfig.h"
#include "SmartPlugConfig.h"

#if SMARTPLUG_ENABLE_MQTT
#include "SmartPlugMqtt.h"
#endif

namespace {

const char kDashboard[] PROGMEM = R"HTML(
<!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1"><title>SmartPlug</title><style>
*{box-sizing:border-box}body{margin:0;background:linear-gradient(135deg,#edf4fb,#f8fafc);color:#10233e;font:15px Inter,system-ui,-apple-system,Segoe UI,sans-serif}.wrap{max-width:1120px}.card{border:1px solid #d8e4f0;box-shadow:0 10px 28px #15294d12}.label{font-weight:700}.state{border:1px solid #d9e4f0}button{transition:.18s transform,.18s background}button:hover{transform:translateY(-1px);background:#0b3c68}canvas{border:1px solid #e5edf5;background:linear-gradient(180deg,#fff,#f8fbfe)}.wrap{max-width:960px;margin:auto;padding:22px}header{display:flex;justify-content:space-between;align-items:center;margin-bottom:18px}h1{font-size:24px;margin:0}.sub{color:#63738a}.dot{display:inline-block;width:9px;height:9px;border-radius:50%;background:#c59000;margin-right:6px}.ok{background:#15803d}.bad{background:#b91c1c}.grid{display:grid;grid-template-columns:repeat(4,1fr);gap:12px}.card{background:#fff;border:1px solid #e4eaf2;border-radius:14px;padding:15px;box-shadow:0 3px 12px #15294d0a}.label{font-size:12px;color:#63738a;text-transform:uppercase;letter-spacing:.05em}.value{font-size:27px;font-weight:700;margin-top:5px}.unit{font-size:13px;color:#63738a}.wide{grid-column:span 2}.state{font-size:13px;padding:5px 8px;background:#edf2f7;border-radius:99px;display:inline-block;margin:5px 4px 0 0}canvas{width:100%;height:180px;background:#fbfcfe;border-radius:10px}input{width:100%;padding:10px;border:1px solid #cbd5e1;border-radius:8px;margin:5px 0 11px;font:inherit}button{border:0;border-radius:8px;padding:10px 14px;background:#0f4c81;color:white;font-weight:650;cursor:pointer}button.danger{background:#9f1239}.tab{background:#e7eff8;color:#17395d}.tab.active{background:#0f4c81;color:#fff;box-shadow:0 4px 10px #0f4c8133}button:disabled{opacity:.5}.two{display:grid;grid-template-columns:1fr 1fr;gap:12px}pre{white-space:pre-wrap;overflow:auto;background:#0f172a;color:#dbeafe;padding:12px;border-radius:9px;font-size:12px}#msg{margin-top:8px;color:#475569}@media(max-width:700px){.grid{grid-template-columns:repeat(2,1fr)}.wide{grid-column:span 2}.two{grid-template-columns:1fr}header{display:block}}
</style><style>
:root{--bg:#071215;--panel:#102126;--panel2:#132a30;--line:#294148;--ink:#edf5f3;--muted:#93a9aa;--orange:#ff7a18;--teal:#38d7ba;--ok:#67e68d;--bad:#ff6b75}.wrap{max-width:1120px;padding:28px}.wrap header{padding:0 0 19px;border-bottom:1px solid var(--line)}body{background:radial-gradient(circle at 50% -20%,#1a363b 0,transparent 46%),var(--bg);color:var(--ink);font:15px/1.45 Inter,"Segoe UI",Arial,sans-serif}.sub,.unit{color:var(--muted)}.card{background:linear-gradient(135deg,var(--panel),#0d1b1f);border-color:var(--line);box-shadow:none;border-radius:18px}.label{font:800 10px ui-monospace,Consolas,monospace;letter-spacing:.13em;color:var(--muted)}.value{color:var(--ink);font-size:30px}.state{background:var(--panel2);border-color:var(--line);color:var(--ink)}input{background:#0b191d;border-color:#426068;color:var(--ink)}button{background:var(--orange);color:#1b1510}.tab{background:transparent;color:var(--muted);border:1px solid var(--line);border-radius:999px;padding:8px 12px}.tab.active{background:var(--orange);border-color:var(--orange);color:#20160c;box-shadow:0 0 0 4px #ff7a1822}.danger{background:#4a1920!important;color:#ff8b93!important;border:1px solid #864149!important}.dot{background:#d99500}.ok{background:var(--ok)}.bad{background:var(--bad)}.chart-card{padding:0;overflow:hidden;background:linear-gradient(135deg,#102126,#091519)}.chart-top{display:flex;justify-content:space-between;gap:18px;align-items:flex-start;padding:19px 20px 12px;border-bottom:1px solid var(--line)}.chart-kicker{font:800 10px ui-monospace,Consolas,monospace;letter-spacing:.14em;color:var(--teal)}.chart-top strong{display:block;margin-top:4px;font-size:22px;letter-spacing:-.025em}.chart-live{font:800 10px ui-monospace,Consolas,monospace;letter-spacing:.08em;color:var(--ok);background:#123c31;border:1px solid #2e806d;border-radius:999px;padding:7px 9px}.chart-toolbar{display:flex;justify-content:space-between;gap:10px;align-items:center;padding:13px 20px 0}.chart-tabs{display:flex;gap:6px;flex-wrap:wrap}.chart-window{color:var(--muted);font:800 10px ui-monospace,Consolas,monospace;letter-spacing:.08em}.chart-wrap{position:relative;padding:13px 13px 0}.chart-wrap canvas{display:block;width:100%;height:653px;border:1px solid #20373d;border-radius:12px;background:#091519;touch-action:none}.chart-tip{position:absolute;display:none;pointer-events:none;z-index:2;min-width:112px;padding:8px 9px;border:1px solid #426068;border-radius:8px;background:#102126eF;color:var(--ink);box-shadow:0 8px 18px #0008;font:700 11px ui-monospace,Consolas,monospace}.chart-tip small{display:block;margin-top:3px;color:var(--muted);font-weight:600}.chart-stats{display:grid;grid-template-columns:repeat(4,1fr);margin:12px 20px 20px;border:1px solid var(--line);border-radius:11px;overflow:hidden}.chart-stat{padding:10px 12px;border-right:1px solid var(--line)}.chart-stat:last-child{border-right:0}.chart-stat span{display:block;color:var(--muted);font:800 9px ui-monospace,Consolas,monospace;letter-spacing:.1em}.chart-stat b{display:block;margin-top:3px;color:var(--ink);font:700 15px ui-monospace,Consolas,monospace}@media(max-width:700px){.wrap{padding:14px}.chart-top{padding:16px;display:block}.chart-live{display:inline-block;margin-top:9px}.chart-toolbar{display:block;padding:12px 16px 0}.chart-window{display:block;margin-top:10px}.chart-wrap{padding:12px 8px 0}.chart-wrap canvas{height:557px}.chart-stats{margin:12px 16px 16px;grid-template-columns:1fr 1fr}.chart-stat:nth-child(2){border-right:0}.chart-stat:nth-child(-n+2){border-bottom:1px solid var(--line)}}
.chart-range{min-width:192px}.chart-range label{display:flex;justify-content:space-between;color:var(--muted);font:800 9px ui-monospace,Consolas,monospace;letter-spacing:.09em}.chart-range output{color:var(--orange)}.chart-range input{width:100%;height:20px;margin:4px 0 0;padding:0;accent-color:var(--orange);background:transparent;border:0;border-radius:0}@media(max-width:700px){.chart-range{min-width:0;margin-top:12px}.chart-range input{width:100%}}.calibrate{margin-top:12px;padding-top:9px;border-top:1px solid var(--line)}.calibrate summary{display:flex;align-items:center;justify-content:space-between;cursor:pointer;list-style:none;color:var(--teal);font:800 10px ui-monospace,Consolas,monospace;letter-spacing:.1em}.calibrate summary::-webkit-details-marker{display:none}.calibrate summary:after{content:"+";display:grid;place-items:center;width:17px;height:17px;border:1px solid #426068;border-radius:50%;color:var(--ink);font:700 15px system-ui}.calibrate[open] summary{margin-bottom:8px}.calibrate[open] summary:after{content:"−"}.calibrate .cf input{height:38px;margin:0 0 8px;padding:8px 10px}.calibrate .cf button{padding:8px 10px;font-size:11px}.grid{grid-template-columns:repeat(5,minmax(0,1fr))}.status-card{min-width:0}.status-stack{display:flex;flex-wrap:wrap;gap:6px;margin-top:12px}.status-stack .state{width:100%;margin:0;padding:7px 8px;border-radius:8px;font:700 10px/1.3 ui-monospace,Consolas,monospace}.state.tone-green{background:#123c31;border-color:#2e806d;color:#8ff0b0}.state.tone-blue{background:#102e43;border-color:#26658c;color:#a9dcff}.state.tone-red{background:#421c23;border-color:#864149;color:#ffabb1}@media(max-width:700px){.grid{grid-template-columns:repeat(2,minmax(0,1fr))}.status-card{grid-column:span 2}.status-stack .state{width:auto;flex:1 1 130px}}.relay-card{display:flex;flex-direction:column;justify-content:space-between;min-height:132px}.relay-buttons{display:grid;grid-template-columns:1fr 1fr;gap:7px;margin-top:16px}.relay-buttons a,.relay-buttons button{width:100%}.relay-buttons button{padding:10px 6px}.relay-buttons .danger{background:#4a1920!important;color:#ffabb1!important}body[data-theme="light"]{--bg:#edf4f5;--panel:#fff;--panel2:#edf4f5;--line:#c4d4d5;--ink:#16211f;--muted:#5c6b66;--orange:#e0630f;--teal:#177a7c;--ok:#1f8f5c;--bad:#d5271a;background:radial-gradient(circle at 50% -20%,#d9eeee 0,transparent 46%),var(--bg)}body[data-theme="light"] .card{background:linear-gradient(135deg,var(--panel),#f7fbfb)}body[data-theme="light"] .chart-card{background:linear-gradient(135deg,#fff,#edf8f7)}body[data-theme="light"] input{background:#fff;color:var(--ink);border-color:#9fb9ba}body[data-theme="light"] .chart-live{background:#e2f3e9;color:#167343;border-color:#9ecfb1}body[data-theme="light"] .state.tone-green{background:#e2f3e9;color:#167343;border-color:#9ecfb1}body[data-theme="light"] .state.tone-blue{background:#e6f3fb;color:#1c5f86;border-color:#9ec9e7}body[data-theme="light"] .state.tone-red{background:#fbe1de;color:#aa2017;border-color:#e8aaa4}.header-actions{display:flex;align-items:center;gap:12px}.online-status{white-space:nowrap}.theme-toggle{display:inline-flex;align-items:center;gap:7px;padding:7px 10px;border:1px solid var(--line);border-radius:999px;background:var(--panel2);color:var(--ink);font:800 10px ui-monospace,Consolas,monospace;letter-spacing:.07em}.theme-toggle:hover{background:var(--panel2);transform:none}.theme-toggle span{font:700 15px system-ui;color:var(--orange)}@media(max-width:700px){.header-actions{margin-top:12px;justify-content:space-between}} .brand-row{display:flex;align-items:center;gap:10px}.sensor-state,.relay-state{display:inline-flex;align-items:center;border:1px solid var(--line);border-radius:999px;padding:5px 8px;font:800 10px ui-monospace,Consolas,monospace;letter-spacing:.06em}.sensor-state.tone-green,.relay-state.tone-green{background:#123c31;border-color:#2e806d;color:#8ff0b0}.sensor-state.tone-blue,.relay-state.tone-blue{background:#102e43;border-color:#26658c;color:#a9dcff}.sensor-state.tone-red,.relay-state.tone-red{background:#421c23;border-color:#864149;color:#ffabb1}.relay-state{margin-top:13px;width:max-content}.relay-card{min-height:150px}.relay-buttons{margin-top:11px}body[data-theme="light"] .sensor-state.tone-green,body[data-theme="light"] .relay-state.tone-green{background:#e2f3e9;color:#167343;border-color:#9ecfb1}body[data-theme="light"] .sensor-state.tone-blue,body[data-theme="light"] .relay-state.tone-blue{background:#e6f3fb;color:#1c5f86;border-color:#9ec9e7}body[data-theme="light"] .sensor-state.tone-red,body[data-theme="light"] .relay-state.tone-red{background:#fbe1de;color:#aa2017;border-color:#e8aaa4}.collapsible{min-height:0;align-self:start}.collapsible summary{display:flex;align-items:center;justify-content:space-between;gap:12px;cursor:pointer;list-style:none}.collapsible summary::-webkit-details-marker{display:none}.collapse-action{color:var(--teal);font:800 10px ui-monospace,Consolas,monospace;letter-spacing:.09em}.collapse-action:after{content:" +"}.collapsible[open] .collapse-action:after{content:" −"}.collapsible-body{padding-top:10px}.collapsible-body>p:first-child{margin-top:0}.collapsible[open]{align-self:start}.chart-full{grid-column:1/-1;width:100%;max-width:80vw;margin:0 auto;border-radius:18px;justify-self:center} .wrap{width:min(100%,1600px);max-width:none;padding:22px clamp(14px,2.4vw,38px) 42px}.grid{grid-template-columns:repeat(5,minmax(0,1fr));align-items:start;gap:clamp(10px,1.35vw,18px)}.card{min-width:0}.wide{grid-column:span 2}.chart-full{grid-column:1/-1}.collapsible{height:auto}.collapsible-body{min-width:0}.collapsible-body form{display:grid;gap:0}.collapsible-body input{min-height:42px}.relay-card{min-height:0}.status-stack{max-width:100%}@media(max-width:900px){.wrap{padding:18px 16px 34px}.grid{grid-template-columns:repeat(3,minmax(0,1fr))}.chart-full{width:100vw;margin-left:calc(50% - 50vw);margin-right:calc(50% - 50vw)}}@media(max-width:640px){.wrap{padding:14px 12px 28px}.wrap header{padding-bottom:14px}.grid{grid-template-columns:repeat(2,minmax(0,1fr));gap:10px}.card{border-radius:14px;padding:13px}.value{font-size:25px}.label{font-size:9px}.wide{grid-column:1/-1}.chart-full{grid-column:1/-1}.chart-full .chart-top{padding:15px 16px 11px}.chart-full .chart-toolbar{padding:11px 16px 0}.chart-full .chart-stats{margin:10px 16px 16px}.collapsible{grid-column:1/-1}.collapsible summary{min-height:28px}.collapsible-body{padding-top:10px}.collapsible-body input{min-height:40px}.relay-buttons{gap:6px}.header-actions{gap:8px}.theme-toggle{padding:6px 8px}.online-status{font-size:12px}}@media(max-width:390px){.grid{grid-template-columns:1fr}.grid>.card{grid-column:1/-1}.card{padding:12px}.brand-row{gap:7px}.sensor-state{font-size:9px;padding:4px 6px}.header-actions{justify-content:flex-start;flex-wrap:wrap}.chart-tabs{gap:5px}.tab{padding:7px 10px}.chart-full .chart-wrap{padding-left:5px;padding-right:5px}}.relay-card{transition:background .2s,border-color .2s}.relay-card.relay-is-on{background:linear-gradient(135deg,#123c31,#0c2a25);border-color:#2e806d}.relay-card.relay-is-on .label{color:#8ff0b0}.relay-card.relay-is-off{background:linear-gradient(135deg,#421c23,#281116);border-color:#864149}.relay-card.relay-is-off .label{color:#ffabb1}.relay-card .relay-state{font-size:16px;letter-spacing:.13em;padding:7px 10px}body[data-theme="light"] .relay-card.relay-is-on{background:linear-gradient(135deg,#e2f3e9,#f7fffa);border-color:#9ecfb1}body[data-theme="light"] .relay-card.relay-is-off{background:linear-gradient(135deg,#fbe1de,#fff8f7);border-color:#e8aaa4}.chart-settings{display:flex;align-items:end;gap:10px}.zero-latch{height:30px;margin-bottom:0;padding:7px 9px;border:1px solid #426068;border-radius:8px;background:#102126;color:#a9dcff;font:800 9px ui-monospace,Consolas,monospace;letter-spacing:.06em;white-space:nowrap}.zero-latch:hover{background:#17333a}.zero-latch.active{background:#5b3a08;border-color:#ffb020;color:#ffe2a8}@media(max-width:700px){.chart-settings{margin-top:10px;align-items:end}.zero-latch{height:30px;padding:7px}.chart-range{flex:1}}.label{font-size:12px}.calibrate summary{font-size:12px}.calibrate .cf button{font-size:13px}.collapse-action{font-size:12px}.chart-kicker{font-size:11px}.chart-live{font-size:11px}.chart-range label{font-size:11px}.zero-latch{font-size:11px}.sensor-state,.relay-state{font-size:11px}.theme-toggle{font-size:11px}.chart-stat span{font-size:10px}@media(max-width:640px){.label{font-size:11px}.calibrate summary{font-size:12px}.calibrate .cf button{font-size:13px}.collapse-action{font-size:12px}.chart-kicker{font-size:11px}.chart-live{font-size:11px}.chart-range label{font-size:11px}.zero-latch{font-size:11px}.sensor-state,.relay-state{font-size:11px}}body[data-theme="light"] .chart-wrap canvas{background:#f8fcfc;border-color:#c8dada}body[data-theme="light"] .chart-tip{background:#fffffff2;border-color:#b7cccd;color:#16211f;box-shadow:0 8px 18px #30404022}body[data-theme="light"] .chart-tip small{color:#5c6b66}.metric-head{display:flex;align-items:center;justify-content:space-between;gap:6px}.energy-unit-toggle{padding:5px 7px;border:1px solid #426068;border-radius:7px;background:#102126;color:#a9dcff;font:800 11px ui-monospace,Consolas,monospace}.energy-unit-toggle:hover{background:#17333a}body[data-theme="light"] .energy-unit-toggle{background:#eef6f6;color:#1c5f86;border-color:#9ec9e7}.chart-toolbar{display:grid;grid-template-columns:minmax(0,1fr) auto;gap:12px}.chart-toolbar-main{display:flex;align-items:center;justify-content:space-between;gap:12px;min-width:0}.chart-tabs{display:flex;gap:7px;min-width:0}.tab{display:grid;gap:3px;min-width:92px;text-align:left}.tab b{font:800 14px ui-monospace,Consolas,monospace;letter-spacing:0;color:inherit}.inline-relay{display:flex;gap:6px;flex:none}.inline-relay a{display:block}.inline-relay button{min-width:54px;padding:10px 12px}.inline-relay .active{box-shadow:0 0 0 3px #ffb02055;border-color:#ffb020}.calibration-panel{grid-column:span 2}.calibration-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:12px}.calibration-grid label{display:block;margin-bottom:5px;color:var(--muted);font-weight:700}.calibration-grid input{margin:0 0 8px}.calibration-grid button{width:100%}@media(max-width:900px){.chart-toolbar{grid-template-columns:1fr}.chart-toolbar-main{flex-wrap:wrap}.inline-relay{margin-left:auto}}@media(max-width:640px){.chart-toolbar-main{align-items:stretch}.chart-tabs{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));flex:1}.tab{min-width:0}.inline-relay{display:grid;grid-template-columns:1fr 1fr;width:100%;margin:0}.inline-relay a{width:100%}.inline-relay button{width:100%}.calibration-panel{grid-column:1/-1}.calibration-grid{grid-template-columns:1fr}.energy-unit-toggle{align-self:center}}.grid>details.collapsible{grid-column:1/-1;width:100%;max-width:80vw;justify-self:center}.inline-relay{align-items:center}.relay-live{display:grid;place-items:center;min-width:36px;height:34px;padding:0 8px;border:1px solid var(--line);border-radius:8px;font:800 11px ui-monospace,Consolas,monospace;letter-spacing:.08em}.relay-live.tone-green{background:#123c31;border-color:#2e806d;color:#8ff0b0}.relay-live.tone-red{background:#421c23;border-color:#864149;color:#ffabb1}.relay-live.tone-blue{background:#102e43;border-color:#26658c;color:#a9dcff}.inline-relay button:disabled{opacity:.34;cursor:not-allowed;filter:grayscale(.25);transform:none}.inline-relay button.active{box-shadow:0 0 0 3px #ffb02055;border-color:#ffb020}body[data-theme="light"] .relay-live.tone-green{background:#e2f3e9;color:#167343;border-color:#9ecfb1}body[data-theme="light"] .relay-live.tone-red{background:#fbe1de;color:#aa2017;border-color:#e8aaa4}body[data-theme="light"] .relay-live.tone-blue{background:#e6f3fb;color:#1c5f86;border-color:#9ec9e7}</style></head><body><main class="wrap"><header><div><div class="brand-row"><h1>SmartPlug</h1><span id="sensorStatus" class="sensor-state tone-blue">Sensor: menunggu</span></div><div class="sub">Local energy monitor</div></div><div class="header-actions"><button id="themeToggle" class="theme-toggle" type="button" aria-label="Ubah tema"><span>◐</span><b id="themeLabel">Light</b></button><div class="online-status"><i id="dot" class="dot"></i><span id="conn">Menghubungkan…</span></div></div></header>
<section class="grid"><div class="card chart-card chart-full"><div class="chart-top"><div><div class="chart-kicker">LIVE TREND / 500 MS</div><strong id="chartTitle">Tegangan</strong></div><div class="chart-live">● STREAM AKTIF</div></div><div class="chart-toolbar"><div class="chart-toolbar-main"><div class="chart-tabs"><button class="tab active" onclick="selectSeries('voltage_v',this)"><span>Voltage</span><b id="tabVoltage">0 V</b></button><button class="tab" onclick="selectSeries('current_a',this)"><span>Current</span><b id="tabCurrent">0 A</b></button><button class="tab" onclick="selectSeries('active_power_w',this)"><span>Watt</span><b id="tabWatt">0 W</b></button><button class="tab" onclick="selectSeries('energy_wh',this)"><span>Energy</span><b id="tabEnergy">0 Wh</b></button><button id="energyUnitToggle" class="energy-unit-toggle" type="button">Wh</button></div><div class="inline-relay"><span id="relayLiveStatus" class="relay-live tone-blue">—</span><button id="relayOnButton" type="button" onclick="relayCommand('on')">ON</button><button id="relayOffButton" class="danger" type="button" onclick="relayCommand('off')">OFF</button></div></div><div class="chart-settings"><div class="chart-range"><label for="xRange">RENTANG X <output id="rangeOut">1 menit</output></label><input id="xRange" type="range" min="1" max="60" value="1" step="1"></div><button id="zeroLatch" class="zero-latch" type="button">0 LATCH OFF</button></div></div><div class="chart-wrap"><canvas id="chart" width="800" height="270"></canvas><div id="chartTip" class="chart-tip"></div></div><div class="chart-stats"><div class="chart-stat"><span>TERAKHIR</span><b id="statLast">—</b></div><div class="chart-stat"><span>MINIMUM</span><b id="statMin">—</b></div><div class="chart-stat"><span>RATA-RATA</span><b id="statAvg">—</b></div><div class="chart-stat"><span>MAKSIMUM</span><b id="statMax">—</b></div></div><div id="cal" class="sub" style="padding:0 20px 20px"></div></div><details class="card wide collapsible calibration-panel"><summary><span class="label">Kalibrasi sensor</span><span class="collapse-action">Buka</span></summary><div class="collapsible-body calibration-grid"><form class="cf"><label>Voltage referensi</label><input name="voltage_v" type="number" step="0.01" placeholder="Volt"><button>Kalibrasi Voltage</button></form><form class="cf"><label>Current referensi</label><input name="current_a" type="number" step="0.001" placeholder="Ampere"><button>Kalibrasi Current</button></form><form class="cf"><label>Watt referensi</label><input name="power_w" type="number" step="0.1" placeholder="Watt"><button>Kalibrasi Watt</button></form></div></details>
<details class="card wide collapsible"><summary><span class="label">Sambungkan ke Wi-Fi yang ada</span><span class="collapse-action">Buka</span></summary><div class="collapsible-body"><p class="sub">Access point SmartPlug tetap aktif sebagai jalur setup. Isi password admin hanya untuk menyimpan pengaturan.</p><form id="wf"><label>Nama Wi-Fi (SSID)</label><input id="ssid" maxlength="32" required><label>Password Wi-Fi <span class="sub">(kosongkan bila jaringan terbuka)</span></label><input id="pass" type="password" maxlength="63"><label>Password admin perangkat</label><input id="admin" type="password" required><button>Simpan & hubungkan</button></form><div id="msg"></div></div></details>
<details class="card wide collapsible"><summary><span class="label">Pemulihan jaringan</span><span class="collapse-action">Buka</span></summary><div class="collapsible-body"><p class="sub">Tahan tombol fisik sesudah perangkat selesai boot selama 10 detik untuk menghapus Wi-Fi tersimpan dan kembali ke access point. Jangan menahan tombol saat perangkat baru dinyalakan: GPIO0 adalah pin boot.</p><button class="danger" id="forget">Hapus Wi-Fi tersimpan</button></div></details>
<details class="card wide collapsible"><summary><span class="label">Diagnostik</span><span class="collapse-action">Buka</span></summary><div class="collapsible-body"><pre id="raw">Memuat…</pre></div></details></section></main><script>
const $=id=>document.getElementById(id);let status={},series='voltage_v',histories={voltage_v:[],current_a:[],active_power_w:[],energy_wh:[]},hoverIndex=-1,rangeMinutes=1,zeroLatch=false,energyUnit='Wh';const chartMeta={voltage_v:{title:'Voltage',unit:'V',color:'#38d7ba'},current_a:{title:'Current',unit:'A',color:'#ffb020'},active_power_w:{title:'Watt',unit:'W',color:'#ff7a18'},energy_wh:{title:'Energy',unit:'Wh',color:'#7eb8ff'}};function chartPalette(){return document.body.dataset.theme==='light'?{bg:'#f8fcfc',grid:'#d8e6e6',tick:'#5c7073',axis:'#94abad',empty:'#5c6b66'}:{bg:'#091519',grid:'#213a40',tick:'#789095',axis:'#3b565c',empty:'#93a9aa'}}function fmt(v){return Number(v).toFixed(Math.abs(Number(v))<10?3:1)}let csrfToken='',sessionReady=false;async function login(){let p=$('admin')?.value||prompt('Password admin perangkat');if(!p)return false;let r=await fetch('/api/v1/auth/login',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:new URLSearchParams({username:'admin',password:p})});let d=await r.json().catch(()=>({}));if(!r.ok){alert('Login gagal: '+(d.error||'unknown'));return false}csrfToken=d.csrf_token;sessionReady=true;return true}async function secureFetch(url,opt={}){if(!sessionReady&&!(await login()))throw new Error('authentication_required');let h=new Headers(opt.headers||{});h.set('X-CSRF-Token',csrfToken);let r=await fetch(url,{...opt,headers:h,credentials:'same-origin'});if(r.status===401){sessionReady=false;csrfToken=''}return r}async function relayCommand(state){try{let r=await secureFetch('/api/v1/relay',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:new URLSearchParams({state})}),d=await r.json();if(!r.ok)alert('Relay ditolak: '+(d.error||'unknown'));load()}catch(e){alert('Login diperlukan untuk kontrol relay')}}function setEnergyUnit(unit){energyUnit=unit;$('energyUnitToggle').textContent=unit;draw()}function setZeroLatch(value){zeroLatch=value;let b=$('zeroLatch');b.textContent=zeroLatch?'0 LATCH ON':'0 LATCH OFF';b.classList.toggle('active',zeroLatch);draw()}function selectSeries(x,b){series=x;hoverIndex=-1;$('chartTitle').textContent=chartMeta[x].title;document.querySelectorAll('.tab').forEach(q=>q.classList.remove('active'));b.classList.add('active');draw()}function n(x,u,raw){let v=typeof x==='number'&&isFinite(x)?x:0;return fmt(v)+' '+u+' <span class="unit">raw: '+raw+'</span>'}function points(){let p=histories[series];return series==='energy_wh'&&energyUnit==='kWh'?p.map(x=>({v:x.v/1000,t:x.t})):p}function chartUnit(){return series==='energy_wh'?energyUnit:chartMeta[series].unit}function viewPoints(){let cutoff=Date.now()-rangeMinutes*60000;return points().filter(p=>p.t>=cutoff)}function scaleCanvas(c){let box=c.getBoundingClientRect(),d=Math.min(window.devicePixelRatio||1,2),w=Math.max(320,Math.floor(box.width*d)),h=Math.max(180,Math.floor(box.height*d));if(c.width!==w||c.height!==h){c.width=w;c.height=h}return{w,h,d}}function stat(id,v,u){$(id).textContent=v===null?'—':fmt(v)+' '+u}function draw(){let c=$('chart'),g=c.getContext('2d'),z=viewPoints(),m=chartMeta[series],u=chartUnit(),p=chartPalette(),q=scaleCanvas(c),w=q.w,h=q.h,L=54,R=18,T=25,B=37,pw=w-L-R,ph=h-T-B,now=Date.now(),rangeMs=rangeMinutes*60000;g.clearRect(0,0,w,h);g.fillStyle=p.bg;g.fillRect(0,0,w,h);g.strokeStyle=p.grid;g.lineWidth=1;g.font='11px ui-monospace,Consolas,monospace';g.fillStyle=p.tick;let vals=z.map(p=>p.v),mn=vals.length?Math.min(...vals):0,mx=vals.length?Math.max(...vals):1;if(zeroLatch){mn=Math.min(mn,0);mx=Math.max(mx,0)}let pad=Math.max((mx-mn)*.12,Math.abs(mx)*.03,.05);mn-=pad;mx+=pad;if(mx===mn)mx=mn+1;for(let i=0;i<5;i++){let y=T+ph*i/4,v=mx-(mx-mn)*i/4;g.beginPath();g.moveTo(L,y);g.lineTo(w-R,y);g.stroke();g.fillText(fmt(v),4,y+4)}for(let i=0;i<5;i++){let x=L+pw*i/4;g.beginPath();g.moveTo(x,T);g.lineTo(x,h-B);g.stroke();let sec=rangeMinutes*60*(1-i/4),label=i===4?'sekarang':(sec>=60?'-'+(sec/60).toFixed(sec%60?1:0)+'m':'-'+sec.toFixed(0)+'s');g.fillText(label,x-13,h-14)}g.strokeStyle=p.axis;g.beginPath();g.moveTo(L,T);g.lineTo(L,h-B);g.lineTo(w-R,h-B);g.stroke();if(zeroLatch&&0>=mn&&0<=mx){let zy=T+ph-(0-mn)/(mx-mn)*ph;g.setLineDash([7,5]);g.strokeStyle='#ffb020';g.lineWidth=1.4;g.beginPath();g.moveTo(L,zy);g.lineTo(w-R,zy);g.stroke();g.setLineDash([])}if(z.length<2){g.fillStyle=p.empty;g.font='600 13px Inter,Segoe UI,sans-serif';g.fillText('Menunggu minimal 2 sampel untuk membentuk tren…',L+15,T+ph/2);stat('statLast',null,u);stat('statMin',null,u);stat('statAvg',null,u);stat('statMax',null,u);return}let xy=i=>({x:L+pw*Math.max(0,Math.min(1,(z[i].t-(now-rangeMs))/rangeMs)),y:T+ph-(z[i].v-mn)/(mx-mn)*ph});let fill=g.createLinearGradient(0,T,0,h-B);fill.addColorStop(0,m.color+'55');fill.addColorStop(1,m.color+'00');g.beginPath();z.forEach((p,i)=>{let a=xy(i);if(i===0)g.moveTo(a.x,a.y);else{let prev=xy(i-1),cx=(prev.x+a.x)/2;g.bezierCurveTo(cx,prev.y,cx,a.y,a.x,a.y)}});g.lineTo(w-R,h-B);g.lineTo(L,h-B);g.closePath();g.fillStyle=fill;g.fill();g.beginPath();z.forEach((p,i)=>{let a=xy(i);if(i===0)g.moveTo(a.x,a.y);else{let prev=xy(i-1),cx=(prev.x+a.x)/2;g.bezierCurveTo(cx,prev.y,cx,a.y,a.x,a.y)}});g.strokeStyle=m.color;g.lineWidth=2.6;g.stroke();let last=xy(z.length-1);g.fillStyle=p.bg;g.beginPath();g.arc(last.x,last.y,5,0,Math.PI*2);g.fill();g.fillStyle=m.color;g.beginPath();g.arc(last.x,last.y,3,0,Math.PI*2);g.fill();if(hoverIndex>=0&&hoverIndex<z.length){let a=xy(hoverIndex);g.setLineDash([4,4]);g.strokeStyle='#8ea6aa';g.beginPath();g.moveTo(a.x,T);g.lineTo(a.x,h-B);g.stroke();g.setLineDash([]);g.fillStyle='#fff';g.beginPath();g.arc(a.x,a.y,4,0,Math.PI*2);g.fill()}let avg=vals.reduce((a,b)=>a+b,0)/vals.length;stat('statLast',vals[vals.length-1],u);stat('statMin',Math.min(...vals),u);stat('statAvg',avg,u);stat('statMax',Math.max(...vals),u)}function chartHover(e){let c=$('chart'),z=points(),tip=$('chartTip');if(z.length<2){tip.style.display='none';return}let b=c.getBoundingClientRect(),x=(e.clientX-b.left)/b.width*c.width,L=54,R=18,target=Date.now()-rangeMinutes*60000*(1-(x-L)/(c.width-L-R));idx=z.reduce((best,p,i)=>Math.abs(p.t-target)<Math.abs(z[best].t-target)?i:best,0);hoverIndex=Math.max(0,Math.min(z.length-1,idx));let p=z[hoverIndex],m=chartMeta[series],age=((z[z.length-1].t-p.t)/1000).toFixed(1);tip.innerHTML=fmt(p.v)+' '+chartUnit()+'<small>'+age+' dtk lalu</small>';tip.style.display='block';tip.style.left=Math.max(8,Math.min(b.width-126,e.clientX-b.left+12))+'px';tip.style.top=Math.max(8,e.clientY-b.top-55)+'px';draw()}$('chart').addEventListener('pointermove',chartHover);$('chart').addEventListener('pointerleave',()=>{hoverIndex=-1;$('chartTip').style.display='none';draw()});$('xRange').addEventListener('input',e=>{rangeMinutes=Number(e.target.value);$('rangeOut').textContent=rangeMinutes+' menit';hoverIndex=-1;draw()});$('zeroLatch').onclick=()=>setZeroLatch(!zeroLatch);$('energyUnitToggle').onclick=()=>setEnergyUnit(energyUnit==='Wh'?'kWh':'Wh');window.addEventListener('resize',draw);async function load(){try{let [s,m,h]=await Promise.all(['/api/v1/status','/api/v1/measurements/latest','/api/v1/health'].map(x=>fetch(x).then(r=>r.json())));status=s;if($('apSsid')&&!$('apSsid').value)$('apSsid').value=s.wifi?.access_point?.ssid||'';let z=m.electrical||{},r=m.raw_codes||{};$('tabVoltage').textContent=fmt(z.voltage_v||0)+' V';$('tabCurrent').textContent=fmt(z.current_a||0)+' A';$('tabWatt').textContent=fmt(z.active_power_w||0)+' W';$('tabEnergy').textContent=fmt((z.energy_wh||0)*(energyUnit==='kWh'?0.001:1))+' '+energyUnit;Object.keys(histories).forEach(k=>{let v=z[k];if(typeof v==='number'&&isFinite(v)){histories[k].push({v,t:Date.now()});if(histories[k].length>7200)histories[k].shift()}});draw();$('cal').textContent='Kalibrasi: '+(m.calibration||'tidak tersedia')+' · pembaruan 500 ms · moving average 10 sampel';let reader=h.meter?.reader_state||'unknown',relay=s.relay?.state||'unknown',sensorTone=reader==='active'?'green':reader==='awaiting_first_packet'?'blue':'red',sensorText=reader==='active'?'Sensor: aktif':reader==='awaiting_first_packet'?'Sensor: menunggu':'Sensor: tidak aktif',relayOn=relay.indexOf('on')===0,relayOff=relay.indexOf('off')===0,relayTone=relayOn?'green':relayOff?'red':'blue',relayText=relayOn?'ON':relayOff?'OFF':'—',sensor=$('sensorStatus'),relayKnown=relayOn||relayOff,relayLabel=relayOn?'ON':relayOff?'OFF':'—',relayOnButton=$('relayOnButton'),relayOffButton=$('relayOffButton'),relayLiveStatus=$('relayLiveStatus');sensor.textContent=sensorText;sensor.className='sensor-state tone-'+sensorTone;relayLiveStatus.textContent=relayLabel;relayLiveStatus.className='relay-live tone-'+(relayOn?'green':relayOff?'red':'blue');relayOnButton.disabled=relayKnown&&relayOn;relayOffButton.disabled=relayKnown&&relayOff;relayOnButton.classList.toggle('active',relayOn);relayOffButton.classList.toggle('active',relayOff);$('raw').textContent=JSON.stringify({status:s,measurement:m,health:h},null,2);$('conn').textContent='Online';$('dot').className='dot ok'}catch(e){$('conn').textContent='Tidak dapat membaca API';$('dot').className='dot bad'}}async function protectedJson(url,opt={}){let r=await secureFetch(url,opt),d=await r.json().catch(()=>({}));if(!r.ok)throw new Error(d.error||'request_failed');return d}document.querySelectorAll('.cf').forEach(f=>f.onsubmit=async e=>{e.preventDefault();let input=f.querySelector('input');try{let d=await protectedJson('/api/v1/settings/calibration',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:new URLSearchParams({[input.name]:input.value})});$('cal').textContent='Kalibrasi tersimpan; berlaku setelah reboot.';input.value='';load()}catch(x){alert('Kalibrasi ditolak: '+x.message)}});$('wf').onsubmit=async e=>{e.preventDefault();$('msg').textContent='Menyimpan…';try{let d=await protectedJson('/api/v1/settings/wifi',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:new URLSearchParams({ssid:$('ssid').value,password:$('pass').value})});$('msg').textContent='Wi-Fi tersimpan: '+d.ssid;load()}catch(x){$('msg').textContent='Ditolak: '+x.message}};$('forget').onclick=async()=>{if(!confirm('Factory reset menghapus Wi-Fi, kalibrasi, sesi, dan kredensial admin. Lanjutkan?'))return;try{await protectedJson('/api/v1/settings/wifi/reset',{method:'POST'});alert('Factory reset dijalankan. Sambungkan kembali ke AP dengan kredensial awal yang tercetak/tercatat saat provisioning.')}catch(x){alert('Ditolak: '+x.message)}};let access=document.createElement('details');access.className='calibrate';access.innerHTML='<summary>Pengaturan Access Point & Admin</summary><form id="accessForm" class="cf"><label>Nama Access Point</label><input id="apSsid" maxlength="32" required placeholder="SmartPlug-ID"><label>Password Access Point</label><input id="apPass" type="password" minlength="8" maxlength="63" required placeholder="minimal 8 karakter"><label>Password admin baru <span class="sub">(kosongkan bila tidak diubah)</span></label><input id="newAdminPass" type="password" minlength="12" maxlength="63" placeholder="minimal 12 karakter"><button>Simpan akses & reboot</button></form>';document.querySelector('#wf').parentElement.appendChild(access);$('accessForm').onsubmit=async e=>{e.preventDefault();try{await protectedJson('/api/v1/settings/access',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:new URLSearchParams({ap_ssid:$('apSsid').value,ap_password:$('apPass').value,admin_password:$('newAdminPass').value})});alert('Pengaturan akses disimpan. Perangkat reboot. Hubungkan ke AP baru.')}catch(x){alert('Ditolak: '+x.message)}};function setTheme(theme){document.body.dataset.theme=theme;$('themeLabel').textContent=theme==='dark'?'Dark':'Light';$('themeToggle').setAttribute('aria-label',theme==='dark'?'Gunakan mode terang':'Gunakan mode gelap');try{localStorage.setItem('smartplug-theme',theme)}catch(e){}draw()}let savedTheme='dark';try{savedTheme=localStorage.getItem('smartplug-theme')||'dark'}catch(e){}setTheme(savedTheme);$('themeToggle').onclick=()=>setTheme(document.body.dataset.theme==='dark'?'light':'dark');load();setInterval(load,500);</script></body></html>)HTML";

// Raw BL0940 codes are shown directly inside their matching metric tabs.  The
// graph statistics below remain solely for the calibrated chart values.
const char kRawChartStatsAddon[] PROGMEM = R"HTML(
(()=>{const rawTargets={v_rms:'tabVoltage',i_rms:'tabCurrent',active_power:'tabWatt',cf_count:'tabEnergy'};function rawText(value){return Number.isFinite(value)?String(Math.round(value)):'—'}function rawNode(tabId){const value=document.getElementById(tabId);if(!value)return null;let node=value.parentElement.querySelector('.tab-raw');if(!node){node=document.createElement('small');node.className='tab-raw';node.style.cssText='display:block;color:var(--muted);font:700 10px ui-monospace,Consolas,monospace;letter-spacing:.04em;white-space:nowrap';value.insertAdjacentElement('afterend',node)}return node}function showRaw(raw){Object.entries(rawTargets).forEach(([rawKey,tabId])=>{const node=rawNode(tabId);if(node)node.textContent='RAW: '+rawText(Number(raw[rawKey]))})}async function refreshRaw(){try{const measurement=await fetch('/api/v1/measurements/latest',{cache:'no-store'}).then(r=>r.json());showRaw(measurement.raw_codes||{})}catch(_){}}setTimeout(refreshRaw,150);setInterval(refreshRaw,500)})();
)HTML";

const char kDashboardVersionAddon[] PROGMEM = R"HTML(
(()=>{async function showVersion(){try{const status=await fetch('/api/v1/status',{cache:'no-store'}).then(r=>r.json()),version=status.firmware&&status.firmware.version;if(!version)return;let badge=document.getElementById('firmwareVersion');if(!badge){badge=document.createElement('span');badge.id='firmwareVersion';badge.style.cssText='display:inline-flex;align-items:center;padding:7px 9px;border:1px solid var(--line);border-radius:999px;background:var(--panel2);color:var(--muted);font:800 11px ui-monospace,Consolas,monospace;letter-spacing:.05em;white-space:nowrap';document.querySelector('.header-actions')?.append(badge)}badge.textContent='v'+version}catch(_){}}showVersion()})();
)HTML";

#if SMARTPLUG_ENABLE_MQTT
const char kMqttDashboardAddon[] PROGMEM = R"HTML(
(()=>{const node=id=>document.getElementById(id);let panel;function statusText(data){const message=node('mqttMessage');if(message)message.textContent=data.connected?'Broker: terhubung':data.configured?'Broker: tersimpan, menghubungkan…':'Broker: belum dikonfigurasi'}async function brokerStatus(){try{const response=await secureFetch('/api/v1/settings/mqtt');const data=await response.json();if(!response.ok)throw new Error(data.error||'request_failed');node('mqttHost').value=data.host||'';node('mqttPort').value=data.port||1883;node('mqttUser').value=data.username||'';node('mqttTopic').value=data.topic||('smartplug/'+(status.device_id||'device'));statusText(data)}catch(error){const message=node('mqttMessage');if(message)message.textContent='Status broker memerlukan login admin'}}function addPanel(){if(document.getElementById('mqttForm'))return;panel=document.createElement('details');panel.className='card wide collapsible';panel.innerHTML='<summary><span class="label">Server MQTT (ESP32)</span><span class="collapse-action">Buka</span></summary><div class="collapsible-body"><p class="sub">ESP32 harus menjalankan MQTT broker pada Wi-Fi yang sama. Konfigurasi disimpan di perangkat.</p><form id="mqttForm" class="cf"><label>Alamat IP / host ESP32</label><input id="mqttHost" maxlength="63" required placeholder="192.168.1.10"><label>Port MQTT</label><input id="mqttPort" type="number" min="1" max="65535" value="1883" required><label>Username broker <span class="sub">(kosongkan bila tanpa akun)</span></label><input id="mqttUser" maxlength="32" autocomplete="username"><label>Password broker <span class="sub">(isi ulang saat ingin mengubah)</span></label><input id="mqttPass" type="password" maxlength="64" autocomplete="new-password"><label>Base topic</label><input id="mqttTopic" maxlength="64" required><button>Simpan server MQTT</button><button id="mqttStatus" type="button" class="tab" style="margin-top:8px">Perbarui status</button></form><div id="mqttMessage" class="sub" style="margin-top:9px">Broker: belum dikonfigurasi</div></div>';document.querySelector('#wf').closest('details').insertAdjacentElement('afterend',panel);node('mqttTopic').value='smartplug/'+(status.device_id||'device');node('mqttStatus').onclick=brokerStatus;node('mqttForm').onsubmit=async event=>{event.preventDefault();const message=node('mqttMessage');message.textContent='Menyimpan…';try{const response=await secureFetch('/api/v1/settings/mqtt',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:new URLSearchParams({host:node('mqttHost').value,port:node('mqttPort').value,username:node('mqttUser').value,password:node('mqttPass').value,topic:node('mqttTopic').value})}),data=await response.json();if(!response.ok)throw new Error(data.error||'request_failed');node('mqttPass').value='';statusText(data)}catch(error){message.textContent='Server MQTT ditolak: '+error.message}}}setTimeout(addPanel,80)})();
)HTML";
#endif

constexpr char kDashboardTail[] = "</body></html>";
const char kDashboardRuntimeScripts[] PROGMEM =
    "<script defer src=\"/raw-stats.js\"></script>"
    "<script defer src=\"/dashboard-version.js\"></script>"
#if SMARTPLUG_ENABLE_MQTT
    "<script defer src=\"/mqtt-config.js\"></script>"
#endif
    ;

constexpr uint32_t kSettingsMagic = 0x53505333UL;
// Bump the record version so units that used the former serial-only
// provisioning credentials are returned to the user-facing bootstrap access.
constexpr uint16_t kSettingsVersion = 4;
// MQTT settings, when enabled, occupy the second EEPROM-emulation sector.
// The REST profile retains its original one-sector footprint.
constexpr size_t kSettingsEepromBytes =
    SMARTPLUG_ENABLE_MQTT ? 1024 : 512;
constexpr unsigned long kSessionLifetimeMs = 15UL * 60UL * 1000UL;
constexpr unsigned long kLoginLockoutMs = 60UL * 1000UL;
constexpr unsigned long kMutationMinimumMs = 250UL;
constexpr unsigned long kRelayMinimumMs = 1000UL;
constexpr uint8_t kMaxFailedLogins = 5;

// The AP password is necessarily retained so the radio can use it.  The admin
// password is deliberately not retained: only a salted SHA-256 HMAC digest is.
struct PersistentSettings {
  uint32_t magic;
  uint16_t version;
  uint16_t reserved;
  uint32_t crc;
  char stationSsid[33];
  char stationPassword[65];
  char accessPointSsid[33];
  char accessPointPassword[65];
  char adminUsername[17];
  char adminSalt[17];
  char adminPasswordHash[65];
  float voltsPerCode;
  float ampsPerCode;
  float wattsPerCode;
};

static_assert(sizeof(PersistentSettings) <= kSettingsEepromBytes,
              "EEPROM record no longer fits its reserved sector.");

PersistentSettings persistentSettings = {};

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

uint32_t settingsCrc(PersistentSettings value) {
  value.crc = 0;
  return crc32(reinterpret_cast<const uint8_t*>(&value), sizeof(value));
}

void copyText(char* destination, const size_t destinationSize,
              const String& source) {
  if (destinationSize == 0) return;
  source.substring(0, destinationSize - 1).toCharArray(destination,
                                                        destinationSize);
}

String sha256Hmac(const String& password, const String& salt) {
  return experimental::crypto::SHA256::hmac(
      password, salt.c_str(), salt.length(),
      experimental::crypto::SHA256::NATURAL_LENGTH);
}

bool constantTimeEquals(const String& left, const String& right) {
  if (left.length() != right.length()) return false;
  uint8_t difference = 0;
  for (size_t i = 0; i < left.length(); ++i) difference |= left[i] ^ right[i];
  return difference == 0;
}

bool reached(const unsigned long deadline) {
  return static_cast<long>(millis() - deadline) >= 0;
}

String cookieValue(const String& cookieHeader, const char* name) {
  const String prefix = String(name) + "=";
  int start = cookieHeader.indexOf(prefix);
  while (start >= 0) {
    if (start == 0 || cookieHeader[start - 1] == ' ' ||
        cookieHeader[start - 1] == ';') {
      start += prefix.length();
      const int end = cookieHeader.indexOf(';', start);
      return cookieHeader.substring(start, end < 0 ? cookieHeader.length() : end);
    }
    start = cookieHeader.indexOf(prefix, start + 1);
  }
  return String();
}

String jsonEscape(const String& value) {
  String escaped;
  escaped.reserve(value.length() + 8);
  for (size_t i = 0; i < value.length(); ++i) {
    const char c = value[i];
    if (c == '"' || c == '\\') escaped += '\\';
    if (c >= 0x20) escaped += c;
  }
  return escaped;
}

}  // namespace

SmartPlugApi::SmartPlugApi() : server_(80) {}

#if SMARTPLUG_ENABLE_MQTT
void SmartPlugApi::setMqttConfigurator(SmartPlugMqtt* mqtt) { mqtt_ = mqtt; }
#endif

String SmartPlugApi::deviceId() const {
  char id[7] = {};
  snprintf(id, sizeof(id), "%06lX", static_cast<unsigned long>(ESP.getChipId()));
  return String(id);
}

String SmartPlugApi::randomHex(const size_t bytes) const {
  String token;
  token.reserve(bytes * 2);
  for (size_t i = 0; i < bytes; ++i) {
    const uint8_t value = static_cast<uint8_t>(ESP.random() & 0xFFU);
    if (value < 16) token += '0';
    token += String(value, HEX);
  }
  return token;
}

bool SmartPlugApi::validateText(const String& value, const size_t minLength,
                                 const size_t maxLength,
                                 const bool allowSpaces) const {
  if (value.length() < minLength || value.length() > maxLength) return false;
  for (size_t i = 0; i < value.length(); ++i) {
    const char c = value[i];
    if (c < 0x21 || c > 0x7E) {
      if (!(allowSpaces && c == ' ')) return false;
    }
  }
  return true;
}

void SmartPlugApi::applyFactoryDefaults() {
  persistentSettings = {};
  persistentSettings.magic = kSettingsMagic;
  persistentSettings.version = kSettingsVersion;
  // First connection must work without a serial monitor, factory label, or
  // device-specific lookup.  The owner is prompted for the admin password
  // only when using protected controls and can replace both credentials from
  // the dashboard afterwards.
  const String apSsid = "SmartPlug-Setup";
  const String apPassword = "SmartPlug123";
  const String adminPassword = "SmartPlug123";
  copyText(persistentSettings.accessPointSsid,
           sizeof(persistentSettings.accessPointSsid), apSsid);
  copyText(persistentSettings.accessPointPassword,
           sizeof(persistentSettings.accessPointPassword), apPassword);
  copyText(persistentSettings.adminUsername,
           sizeof(persistentSettings.adminUsername), "admin");
  const String salt = randomHex(8);
  copyText(persistentSettings.adminSalt, sizeof(persistentSettings.adminSalt),
           salt);
  copyText(persistentSettings.adminPasswordHash,
           sizeof(persistentSettings.adminPasswordHash),
           sha256Hmac(adminPassword, salt));
  persistentSettings.crc = settingsCrc(persistentSettings);
  saveSettings();
  Serial.println(F("INFO factory_defaults_created"));
  Serial.print(F("INFO initial_ap_ssid=")); Serial.println(apSsid);
  Serial.println(F("INFO initial_ap_password=SmartPlug123"));
  Serial.println(F("INFO initial_admin_username=admin"));
  Serial.println(F("INFO initial_admin_password=SmartPlug123"));
}

bool SmartPlugApi::saveSettings() {
  persistentSettings.magic = kSettingsMagic;
  persistentSettings.version = kSettingsVersion;
  persistentSettings.crc = settingsCrc(persistentSettings);
  EEPROM.put(0, persistentSettings);
  return EEPROM.commit();
}

void SmartPlugApi::loadSettings() {
  EEPROM.get(0, persistentSettings);
  const bool valid = persistentSettings.magic == kSettingsMagic &&
                     persistentSettings.version == kSettingsVersion &&
                     persistentSettings.crc == settingsCrc(persistentSettings) &&
                     persistentSettings.accessPointSsid[0] != '\0' &&
                     persistentSettings.accessPointPassword[0] != '\0' &&
                     persistentSettings.adminPasswordHash[0] != '\0';
  if (!valid) applyFactoryDefaults();
  stationConfigured_ = persistentSettings.stationSsid[0] != '\0';
  configuredStationSsid_ = stationConfigured_
      ? String(persistentSettings.stationSsid) : String();
  runtimeCalibration_ = {persistentSettings.voltsPerCode,
                         persistentSettings.ampsPerCode,
                         persistentSettings.wattsPerCode};
  // main.cpp owns the live metering instance, so re-apply a valid EEPROM
  // calibration on every boot before the first regular measurement is used.
  runtimeCalibrationChanged_ = true;
}

void SmartPlugApi::startNetwork() {
  WiFi.persistent(false); WiFi.forceSleepWake(); delay(1);
  // This board is primarily used through its own AP. Disable modem sleep so
  // the local HTTP server remains responsive while the meter UART is active.
  WiFi.setSleepMode(WIFI_NONE_SLEEP);
  loadSettings();
  // Do not rely on SDK defaults: this fixes the intended AP network and
  // starts the ESP8266 SoftAP DHCP service on the same subnet.
  const IPAddress apIp(192, 168, 4, 1);
  const IPAddress subnet(255, 255, 255, 0);
  WiFi.mode(WIFI_OFF);
  delay(50);
  WiFi.mode(stationConfigured_ ? WIFI_AP_STA : WIFI_AP);
  if (!WiFi.softAPConfig(apIp, apIp, subnet)) {
    Serial.println(F("ERR local_access_point_config_failed"));
  }
  if (!WiFi.softAP(persistentSettings.accessPointSsid,
                   persistentSettings.accessPointPassword,
                   smartplug_config::kApChannel, false, smartplug_config::kApMaxClients)) {
    Serial.println(F("ERR local_access_point_start_failed"));
  } else {
    // ESP8266WiFi::softAPConfig() and softAP() already manage the SDK DHCP
    // lifecycle. A second direct restart leaves the wrapper's DHCP state out
    // of sync and can make Windows clients fall back to 169.254/16.
    Serial.print(F("INFO AP ready ip="));
    Serial.println(WiFi.softAPIP());
  }
  if (stationConfigured_) {
    WiFi.begin(persistentSettings.stationSsid, persistentSettings.stationPassword);
  }
}

void SmartPlugApi::begin() {
  EEPROM.begin(kSettingsEepromBytes);
  startNetwork();
  server_.collectHeaders("Cookie", "X-CSRF-Token");
  server_.on("/", HTTP_GET, [this]() { handleDashboard(); });
  server_.on("/raw-stats.js", HTTP_GET, [this]() { handleRawStatsScript(); });
  server_.on("/dashboard-version.js", HTTP_GET,
             [this]() { handleDashboardVersionScript(); });
#if SMARTPLUG_ENABLE_MQTT
  server_.on("/mqtt-config.js", HTTP_GET,
             [this]() { server_.send_P(200, PSTR("application/javascript"),
                                       kMqttDashboardAddon); });
  server_.on("/api/v1/settings/mqtt", HTTP_GET,
             [this]() { handleMqttSettings(); });
  server_.on("/api/v1/settings/mqtt", HTTP_POST,
             [this]() { handleMqttSettings(); });
#endif
  server_.on("/api/v1", HTTP_GET, [this]() { handleDiscovery(); });
  server_.on("/api/v1/capabilities", HTTP_GET, [this]() { handleCapabilities(); });
  server_.on("/api/v1/status", HTTP_GET, [this]() { handleStatus(); });
  server_.on("/api/v1/measurements/latest", HTTP_GET, [this]() { handleLatestMeasurement(); });
  server_.on("/api/v1/health", HTTP_GET, [this]() { handleHealth(); });
  server_.on("/api/v1/auth/login", HTTP_POST, [this]() { handleLogin(); });
  server_.on("/api/v1/auth/logout", HTTP_POST, [this]() { handleLogout(); });
  server_.on("/api/v1/auth/session", HTTP_GET, [this]() { handleSession(); });
  server_.on("/api/v1/settings/wifi", HTTP_POST, [this]() { handleWifiSettings(); });
  server_.on("/api/v1/settings/wifi/reset", HTTP_POST, [this]() { handleWifiReset(); });
  server_.on("/api/v1/settings/calibration", HTTP_POST, [this]() { handleCalibration(); });
  server_.on("/api/v1/settings/access", HTTP_POST, [this]() { handleAccessSettings(); });
  server_.on("/api/v1/relay", HTTP_POST, [this]() { handleRelayCommand(); });
  server_.on("/api/v1/audit", HTTP_GET, [this]() { handleAuditLog(); });
  server_.onNotFound([this]() { handleNotFound(); });
  server_.begin();
  webServerStarted_ = true;
}

void SmartPlugApi::handleClient() { server_.handleClient(); }
void SmartPlugApi::printWebDiagnostics() {
  Serial.print(F("{\"web_server_started\":"));
  Serial.print(webServerStarted_ ? F("true") : F("false"));
  Serial.print(F(",\"ap\":{\"ssid\":\""));
  Serial.print(WiFi.softAPSSID());
  Serial.print(F("\",\"ip\":\""));
  Serial.print(WiFi.softAPIP());
  Serial.print(F("\",\"gateway\":\""));
  Serial.print(WiFi.softAPIP());
  Serial.print(F("\",\"stations\":"));
  Serial.print(WiFi.softAPgetStationNum());
  Serial.print(F("},\"station\":{\"configured\":"));
  Serial.print(stationConfigured_ ? F("true") : F("false"));
  Serial.print(F(",\"connected\":"));
  Serial.print(WiFi.status() == WL_CONNECTED ? F("true") : F("false"));
  Serial.print(F(",\"ip\":\""));
  Serial.print(WiFi.localIP());
  Serial.print(F("\"},\"http\":{\"root\":\"/\",\"raw_stats\":\"/raw-stats.js\",\"version\":\"/dashboard-version.js\",\"health\":\"/api/v1/health\"},\"heap_free\":"));
  Serial.print(ESP.getFreeHeap());
  Serial.print(F(",\"heap_max_block\":"));
  Serial.print(ESP.getMaxFreeBlockSize());
  Serial.println(F("}"));
}
void SmartPlugApi::sendJson(const int code, const String& body) {
  server_.sendHeader("Cache-Control", "no-store");
  server_.send(code, "application/json", body);
}
void SmartPlugApi::sendError(const int code, const char* error) {
  sendJson(code, String("{\"error\":\"") + error + "\"}");
}
void SmartPlugApi::addAuditEvent(const char* event) {
  auditEvents_[auditHead_] = String(millis()) + ":" + event;
  auditHead_ = (auditHead_ + 1U) % 12U;
  if (auditCount_ < 12U) ++auditCount_;
}
void SmartPlugApi::clearSession() {
  sessionToken_ = String(); csrfToken_ = String(); sessionExpiresAtMs_ = 0;
}
void SmartPlugApi::startSession() {
  sessionToken_ = randomHex(24); csrfToken_ = randomHex(16);
  sessionExpiresAtMs_ = millis() + kSessionLifetimeMs;
  server_.sendHeader("Set-Cookie", String("sp_session=") + sessionToken_ +
      "; Path=/; HttpOnly; SameSite=Strict; Max-Age=900");
}
bool SmartPlugApi::requireSession(const bool requireCsrf) {
  if (sessionToken_.isEmpty() || reached(sessionExpiresAtMs_) ||
      !constantTimeEquals(cookieValue(server_.header("Cookie"), "sp_session"),
                          sessionToken_)) {
    clearSession(); sendError(401, "authentication_required"); return false;
  }
  if (requireCsrf && !constantTimeEquals(server_.header("X-CSRF-Token"), csrfToken_)) {
    sendError(403, "csrf_invalid"); return false;
  }
  sessionExpiresAtMs_ = millis() + kSessionLifetimeMs;
  return true;
}
bool SmartPlugApi::validateMutationRate(const unsigned long minimumIntervalMs) {
  const unsigned long now = millis();
  if (lastMutationAtMs_ != 0 && now - lastMutationAtMs_ < minimumIntervalMs) {
    sendError(429, "rate_limited"); return false;
  }
  lastMutationAtMs_ = now;
  return true;
}
String SmartPlugApi::calibrationState() const { const uint8_t count=(runtimeCalibration_.voltsPerCode>0)+(runtimeCalibration_.ampsPerCode>0)+(runtimeCalibration_.wattsPerCode>0); return count==3?"calibrated":(count?"partial":"not_calibrated"); }
void SmartPlugApi::handleDashboard() {
  const size_t dashboardBytes = strlen_P(kDashboard);
  const size_t tailBytes = sizeof(kDashboardTail) - 1U;
  const size_t scriptBytes = strlen_P(kDashboardRuntimeScripts);
  server_.sendHeader("Cache-Control", "no-store");
  server_.setContentLength(dashboardBytes + scriptBytes);
  server_.send_P(200, PSTR("text/html; charset=utf-8"), kDashboard,
                 dashboardBytes - tailBytes);
  server_.sendContent_P(kDashboardRuntimeScripts);
  server_.sendContent_P(kDashboard + dashboardBytes - tailBytes, tailBytes);
}
void SmartPlugApi::handleRawStatsScript() {
  server_.sendHeader("Cache-Control", "no-store");
  server_.send_P(200, PSTR("application/javascript; charset=utf-8"),
                 kRawChartStatsAddon);
}
void SmartPlugApi::handleDashboardVersionScript() {
  server_.sendHeader("Cache-Control", "no-store");
  server_.send_P(200, PSTR("application/javascript; charset=utf-8"),
                 kDashboardVersionAddon);
}
void SmartPlugApi::handleDiscovery() {
  sendJson(200, "{\"service\":\"SmartPlug local API\",\"version\":\"v1\",\"dashboard\":\"/\",\"endpoints\":[\"/api/v1/capabilities\",\"/api/v1/status\",\"/api/v1/measurements/latest\",\"/api/v1/health\",\"/api/v1/auth/login\",\"/api/v1/relay\"]}");
}
void SmartPlugApi::handleCapabilities() {
  const char* firmwareVersion = build_config::kFirmwareVersion;
  String b(F("{\"api_version\":\"v1\",\"firmware_version\":\""));
  b += firmwareVersion;
  b += F("\",\"features\":{\"local_dashboard\":true,\"local_api\":true,\"session_auth\":true,\"csrf_protection\":true,\"credential_storage\":\"salted_sha256_hmac\",\"wifi_provisioning\":true,\"mqtt\":");
  b += SMARTPLUG_ENABLE_MQTT ? "true" : "false";
  b += F(",\"ota_update\":false,\"relay_actuation\":");
  b += relayActuationAllowed_ ? "true" : "false";
  b += "},\"metering\":{\"calibration\":\"";
  b += calibrationState();
  b += "\",\"units_available\":";
  b += calibrationState() == "calibrated" ? "true" : "false";
  b += "}}";
  sendJson(200, b);
}
void SmartPlugApi::handleStatus() {
  const bool connected = WiFi.status() == WL_CONNECTED;
  const char* firmwareVersion = build_config::kFirmwareVersion;
  String b(F("{\"firmware\":{\"name\":\"smartplug-bringup\",\"version\":\""));
  b += firmwareVersion;
  b += F("\"},\"device_id\":\"");
  b += deviceId(); b += "\",\"uptime_ms\":"; b += String(millis());
  b += ",\"wifi\":{\"mode\":\""; b += stationConfigured_ ? "ap_sta" : "softap";
  b += "\",\"access_point\":{\"ssid\":\""; b += jsonEscape(WiFi.softAPSSID());
  b += "\",\"ip\":\""; b += WiFi.softAPIP().toString();
  b += "\"},\"station\":{\"configured\":"; b += stationConfigured_ ? "true" : "false";
  b += ",\"ssid\":\""; b += jsonEscape(configuredStationSsid_);
  b += "\",\"status\":\""; b += stationConfigured_ ? (connected ? "connected" : "connecting") : "not_configured";
  b += "\",\"ip\":\""; if (connected) b += WiFi.localIP().toString();
  b += "\"}},\"relay\":{\"state\":\""; b += relayState_;
  b += "\",\"actuation_allowed\":"; b += relayActuationAllowed_ ? "true" : "false";
  b += "},\"standby\":{\"state\":\""; b += standbyDetected_ ? "detected" : (standbyPending_ ? "pending" : "inactive");
  b += "\"},\"voltage_anomaly\":{\"state\":\""; b += voltageAnomalyState_; b += "\"}}";
  sendJson(200, b);
}
void SmartPlugApi::handleLatestMeasurement() { String b = "{\"captured_at_ms\":" + String(capturedAtMs_) + ",\"has_sample\":"; b += hasSample_ ? "true" : "false"; b += ",\"calibration\":\""; b += calibrationState(); b += "\",\"electrical\":{"; b += runtimeCalibration_.voltsPerCode>0?"\"voltage_v\":"+String(static_cast<float>(raw_.voltageRms)*runtimeCalibration_.voltsPerCode,3):"\"voltage_v\":0"; b += runtimeCalibration_.ampsPerCode>0?",\"current_a\":"+String(static_cast<float>(raw_.currentRms)*runtimeCalibration_.ampsPerCode,4):",\"current_a\":0"; b += runtimeCalibration_.wattsPerCode>0?",\"active_power_w\":"+String(static_cast<float>(raw_.activePower)*runtimeCalibration_.wattsPerCode,2):",\"active_power_w\":0"; b += calibrationState()=="calibrated"?",\"energy_wh\":"+String(electrical_.energyWhSinceBoot,3):",\"energy_wh\":0"; b += ",\"apparent_power_va\":0,\"power_factor\":0},\"raw_codes\":{\"i_rms\":"+String(raw_.currentRms)+",\"v_rms\":"+String(raw_.voltageRms)+",\"active_power\":"+String(raw_.activePower)+",\"cf_count\":"+String(raw_.cfCount)+"}}"; sendJson(200,b); }
void SmartPlugApi::handleHealth() { const bool readerActive = hasSample_ && millis() - capturedAtMs_ <= 5000UL; String b = "{\"meter\":{\"reader_state\":\""; b += readerActive ? "active" : (hasPollResult_ ? "not_receiving_valid_data" : "awaiting_first_packet"); b += "\",\"last_valid_sample_age_ms\":"; b += hasSample_ ? String(millis() - capturedAtMs_) : "null"; b += ",\"has_poll_result\":"; b += hasPollResult_ ? "true" : "false"; b += ",\"latest_poll_valid\":"; b += hasPollResult_ ? (latestPollValid_ ? "true" : "false") : "null"; b += ",\"packets_ok\":" + String(packetsOk_) + ",\"packets_bad\":" + String(packetsBad_) + "},\"api\":\"ok\"}"; sendJson(200, b); }
void SmartPlugApi::handleLogin() {
  if (loginLockUntilMs_ != 0 && !reached(loginLockUntilMs_)) {
    sendError(429, "login_temporarily_locked"); return;
  }
  const String username = server_.arg("username");
  const String password = server_.arg("password");
  const String expected = String(persistentSettings.adminPasswordHash);
  const bool valid = constantTimeEquals(username, String(persistentSettings.adminUsername)) &&
      constantTimeEquals(sha256Hmac(password, String(persistentSettings.adminSalt)), expected);
  if (!valid) {
    ++failedLoginCount_;
    addAuditEvent("login_failed");
    if (failedLoginCount_ >= kMaxFailedLogins) {
      failedLoginCount_ = 0;
      loginLockUntilMs_ = millis() + kLoginLockoutMs;
      addAuditEvent("login_lockout");
    }
    sendError(401, "authentication_failed"); return;
  }
  failedLoginCount_ = 0; loginLockUntilMs_ = 0;
  startSession(); addAuditEvent("login_success");
  sendJson(200, String("{\"result\":\"authenticated\",\"csrf_token\":\"") + csrfToken_ + "\",\"expires_in_s\":900}");
}
void SmartPlugApi::handleLogout() {
  if (!requireSession(true)) return;
  clearSession();
  server_.sendHeader("Set-Cookie", "sp_session=; Path=/; HttpOnly; SameSite=Strict; Max-Age=0");
  addAuditEvent("logout"); sendJson(200, "{\"result\":\"logged_out\"}");
}
void SmartPlugApi::handleSession() {
  if (!requireSession(false)) return;
  sendJson(200, String("{\"authenticated\":true,\"csrf_token\":\"") + csrfToken_ + "\",\"expires_in_s\":900}");
}
void SmartPlugApi::handleWifiSettings() {
  if (!requireSession(true) || !validateMutationRate(kMutationMinimumMs)) return;
  const String ssid = server_.arg("ssid");
  const String password = server_.arg("password");
  if (!validateText(ssid, 1, 32, true) || password.length() > 63) {
    sendError(400, "invalid_wifi_settings"); return;
  }
  copyText(persistentSettings.stationSsid, sizeof(persistentSettings.stationSsid), ssid);
  copyText(persistentSettings.stationPassword, sizeof(persistentSettings.stationPassword), password);
  if (!saveSettings()) { sendError(500, "settings_write_failed"); return; }
  configuredStationSsid_ = ssid; stationConfigured_ = true;
  WiFi.mode(WIFI_AP_STA); WiFi.disconnect(false);
  WiFi.begin(persistentSettings.stationSsid, persistentSettings.stationPassword);
  addAuditEvent("wifi_saved");
  sendJson(200, String("{\"result\":\"saved_connecting\",\"ssid\":\"") + jsonEscape(ssid) + "\"}");
}
void SmartPlugApi::factoryResetWifi() {
  clearSession();
  addAuditEvent("factory_reset");
  applyFactoryDefaults();
  WiFi.disconnect(true); delay(100); ESP.restart();
}
void SmartPlugApi::handleWifiReset() {
  if (!requireSession(true) || !validateMutationRate(kMutationMinimumMs)) return;
  sendJson(200, "{\"result\":\"factory_reset_rebooting\"}");
  delay(250); factoryResetWifi();
}
void SmartPlugApi::handleCalibration() {
  if (!requireSession(true) || !validateMutationRate(kMutationMinimumMs)) return;
  bool changed = false;
  if (server_.hasArg("voltage_v")) { const float reference = server_.arg("voltage_v").toFloat(); if (reference <= 0 || raw_.voltageRms == 0) { sendJson(400,"{\"error\":\"voltage_reference_or_raw_invalid\"}"); return; } runtimeCalibration_.voltsPerCode = reference / static_cast<float>(raw_.voltageRms); changed = true; }
  if (server_.hasArg("current_a")) { const float reference = server_.arg("current_a").toFloat(); if (reference <= 0 || raw_.currentRms == 0) { sendJson(400,"{\"error\":\"current_reference_or_raw_invalid\"}"); return; } runtimeCalibration_.ampsPerCode = reference / static_cast<float>(raw_.currentRms); changed = true; }
  if (server_.hasArg("power_w")) { const float reference = server_.arg("power_w").toFloat(); if (reference <= 0 || raw_.activePower == 0) { sendJson(400,"{\"error\":\"power_reference_or_raw_invalid\"}"); return; } runtimeCalibration_.wattsPerCode = reference / static_cast<float>(raw_.activePower); changed = true; }
  if (!changed) { sendJson(400,"{\"error\":\"reference_required\"}"); return; }
  persistentSettings.voltsPerCode = runtimeCalibration_.voltsPerCode;
  persistentSettings.ampsPerCode = runtimeCalibration_.ampsPerCode;
  persistentSettings.wattsPerCode = runtimeCalibration_.wattsPerCode;
  if (!saveSettings()) { sendError(500, "calibration_write_failed"); return; }
  runtimeCalibrationChanged_ = true;
  addAuditEvent("calibration_saved");
  sendJson(200,"{\"result\":\"calibration_saved\"}");
}
void SmartPlugApi::handleAccessSettings() {
  if (!requireSession(true) || !validateMutationRate(kMutationMinimumMs)) return;
  const String apSsid = server_.arg("ap_ssid");
  const String apPassword = server_.arg("ap_password");
  const String adminPassword = server_.arg("admin_password");
  if (!validateText(apSsid, 1, 32, true) || !validateText(apPassword, 8, 63, false)) {
    sendError(400, "invalid_access_point_settings"); return;
  }
  if (!adminPassword.isEmpty() && !validateText(adminPassword, 12, 63, false)) {
    sendError(400, "invalid_admin_password"); return;
  }
  copyText(persistentSettings.accessPointSsid,
           sizeof(persistentSettings.accessPointSsid), apSsid);
  copyText(persistentSettings.accessPointPassword,
           sizeof(persistentSettings.accessPointPassword), apPassword);
  if (!adminPassword.isEmpty()) {
    const String salt = randomHex(8);
    copyText(persistentSettings.adminSalt, sizeof(persistentSettings.adminSalt), salt);
    copyText(persistentSettings.adminPasswordHash,
             sizeof(persistentSettings.adminPasswordHash),
             sha256Hmac(adminPassword, salt));
  }
  if (!saveSettings()) { sendError(500, "settings_write_failed"); return; }
  clearSession(); addAuditEvent("access_settings_changed");
  sendJson(200, "{\"result\":\"access_settings_saved_rebooting\"}");
  delay(250); ESP.restart();
}
#if SMARTPLUG_ENABLE_MQTT
void SmartPlugApi::handleMqttSettings() {
  if (mqtt_ == nullptr) { sendError(503, "mqtt_unavailable"); return; }
  if (server_.method() == HTTP_GET) {
    if (!requireSession(false)) return;
    const SmartPlugMqtt::SettingsView settings = mqtt_->settingsView();
    String body(F("{\"configured\":"));
    body += settings.configured ? "true" : "false";
    body += F(",\"connected\":");
    body += settings.connected ? "true" : "false";
    body += F(",\"host\":\""); body += jsonEscape(settings.host);
    body += F("\",\"port\":"); body += String(settings.port);
    body += F(",\"username\":\""); body += jsonEscape(settings.username);
    body += F("\",\"topic\":\""); body += jsonEscape(settings.baseTopic);
    body += F("\",\"device_id\":\""); body += deviceId(); body += F("\"}");
    sendJson(200, body);
    return;
  }
  if (!requireSession(true) || !validateMutationRate(kMutationMinimumMs)) return;
  const String host = server_.arg("host");
  const String portText = server_.arg("port");
  const String username = server_.arg("username");
  const String password = server_.arg("password");
  const String topic = server_.arg("topic");
  uint32_t port = 0;
  for (size_t i = 0; i < portText.length(); ++i) {
    const char c = portText[i];
    if (c < '0' || c > '9') { sendError(400, "invalid_mqtt_port"); return; }
    port = port * 10U + static_cast<uint32_t>(c - '0');
    if (port > 65535U) { sendError(400, "invalid_mqtt_port"); return; }
  }
  if (port == 0 || !mqtt_->saveWebSettings(host, static_cast<uint16_t>(port),
                                             username, password, topic)) {
    sendError(400, "invalid_mqtt_settings"); return;
  }
  addAuditEvent("mqtt_settings_saved");
  sendJson(200, String("{\"result\":\"mqtt_settings_saved\",\"configured\":true,\"connected\":") +
                    (mqtt_->connected() ? "true" : "false") + "}");
}
#endif
void SmartPlugApi::handleRelayCommand() {
  if (!requireSession(true) || !validateMutationRate(kRelayMinimumMs)) return;
  const String state = server_.arg("state");
  if (state != "on" && state != "off") { sendError(400, "invalid_relay_state"); return; }
  if (!relayActuationAllowed_) { sendError(403, "relay_actuation_disabled"); return; }
  relayCommandPending_ = true;
  requestedRelayOn_ = state == "on";
  lastRelayCommandAtMs_ = millis();
  addAuditEvent(requestedRelayOn_ ? "relay_on_queued" : "relay_off_queued");
  sendJson(202, String("{\"result\":\"relay_command_queued\",\"state\":\"") + state + "\"}");
}
void SmartPlugApi::handleAuditLog() {
  if (!requireSession(false)) return;
  String body = "{\"events\":[";
  for (uint8_t i = 0; i < auditCount_; ++i) {
    const uint8_t index = (auditHead_ + 12U - auditCount_ + i) % 12U;
    if (i) body += ',';
    body += '"';
    body += jsonEscape(auditEvents_[index]);
    body += '"';
  }
  body += "]}"; sendJson(200, body);
}
bool SmartPlugApi::takeRelayCommand(bool& turnOn) { if (!relayCommandPending_) return false; relayCommandPending_ = false; turnOn = requestedRelayOn_; return true; }
void SmartPlugApi::handleNotFound() { sendJson(404,"{\"error\":\"not_found\"}"); }
bool SmartPlugApi::takeRuntimeCalibration(smartplug_metering::Calibration& calibration) { if (!runtimeCalibrationChanged_) return false; runtimeCalibrationChanged_ = false; calibration = runtimeCalibration_; return true; }
void SmartPlugApi::setMeterSnapshot(const bl0940_protocol::RawMeasurement& raw,const smartplug_metering::ElectricalSample& electrical,unsigned long capturedAtMs) { raw_=raw; electrical_=electrical; capturedAtMs_=capturedAtMs; hasSample_=true; }
void SmartPlugApi::setMeterHealth(bool hasPollResult,bool latestPollValid,uint32_t packetsOk,uint32_t packetsBad) { hasPollResult_=hasPollResult; latestPollValid_=latestPollValid; packetsOk_=packetsOk; packetsBad_=packetsBad; }
void SmartPlugApi::setStandbyState(bool detected,bool pending) { standbyDetected_=detected; standbyPending_=pending; }
void SmartPlugApi::setVoltageAnomaly(const char* state) { voltageAnomalyState_=state; }
void SmartPlugApi::setRelayState(const char* relayState,bool actuationAllowed) { relayState_=relayState; relayActuationAllowed_=actuationAllowed; }
