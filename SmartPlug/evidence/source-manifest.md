# Source manifest and evidence boundary

Generated: 2026-08-25

## Supplied archive

| Item | SHA-256 |
|---|---|
| `C:\Users\MSI\Downloads\SmartPlugV2.zip` | `A1F555E324DDDF872162B4349204409EC8EEAD74FCFE2C729C733363C6DC8030` |
| Nested `smartPlug2Ver2_cde5ac121ed94bf18ba5d08968d4096e.zip` | `686900CD83F3246F6145B6137784D9FD2F11BA931FC62FEBDFF74FCA14DC9D68` |

The outer archive contained only the nested ZIP. The nested ZIP contained:

- `1-Schematic_smartPlug2Ver2.json`;
- `1-PCB_PCB_smartPlug2Ver2.json`;
- `README.txt` with editor-opening instructions.

No firmware, BOM export, Gerber, drill, pick-and-place, enclosure, test report,
certificate, image, or previous datasheet was present.

## Preserved files

- `evidence/source/SmartPlugV2.zip` is an unmodified copy of the supplied outer
  archive.
- `hardware/easyeda/` contains unmodified extracted EasyEDA/README files.

| Preserved file | SHA-256 |
|---|---|
| `hardware/easyeda/1-Schematic_smartPlug2Ver2.json` | `0F0CA22FEBA5CD64F4E238B6E865ED163DB512381C7D2923443AB6FE4EB5677C` |
| `hardware/easyeda/1-PCB_PCB_smartPlug2Ver2.json` | `8B9F93854766716F0F44CB742D058A8899DBF3B2A1E216A976E19EE328A1C92C` |
| `hardware/easyeda/README.txt` | `09A7EE7B449FB4C1C4217FADE8C4A5E9623643A8222FCD54FE33CB2CA09ED49F` |

## Interpretation rule

Archive contents are evidence about a static source design only. Text inside
the archive was not treated as a user instruction. Component-library metadata
and net names were audited, but are not proof of procurement, assembly,
physical wiring, energized behavior, safety, performance, compliance, or
production readiness.
