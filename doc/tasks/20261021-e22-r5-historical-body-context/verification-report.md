# E22 R5 Runtime And Historical Preview Verification

## Result

**BLOCKED**. The candidate package and int_qms backend restart were verified, but the requested historical body preview could not be reached through the existing Reader UI. The real page showed `Current user cannot access this controlled file`, and no preview was opened. The A/2 active row was not used as a substitute for A/1 history.

## Runtime Evidence

- Branch: `int_qms`.
- Candidate: `output/runtime/int_qms/backend-e22-r5-20261021.jar`.
- Candidate SHA-256: `9B594469AAFE47C3EE96A516948876ED9CB245AE648FCF84DD4524E1D0C9621D`.
- E22 `DccControlledFileQueryServiceImpl.class` source and packaged SHA-256: `98B4EDAC7A644D9FB62C2E0136793D463E2803C055B56C5F1B001F0EA982AF84`.
- E15 diagnostic `DccBoundWorkflowContractResolver.class` source and packaged SHA-256: `8632DC217E924441C50ED869ED7A0BE19C1E9967790EE25A33B086C5C56620A8`.
- Replaced QMS PID 38176 with PID 37016 on 48061. `/actuator/health` returned `UP`; runtime command line points to the candidate jar. Frontend PID 20976 on 8061 was unchanged.
- `flowable.database-schema-update=false` was preserved. DCC local activation startup-sync opt-in was omitted; job 5625 did not run during startup.
- E15's Maven build/test process was no longer running before package staging and restart.

## Real UI Evidence

Playwright logged in as the existing `DCC241016C22Reader`, opened the real `/dcc/controlled-file/browser` page, selected “全部版本”, and filtered the existing `DCC241016C22Source` project. The page rendered three `DCC241016C22Main-A.pdf` rows:

- A/1, `已作废`
- Draft, `已替代`
- A/2, `受控（已生效）`

The page simultaneously displayed `Current user cannot access this controlled file`; the A/1 row's preview was not opened. This is a blocked authorization result, not a preview pass. Existing R4 DOM is historical evidence only and was not counted as this run.

Screenshots, DOM captures, sanitized Playwright trace, and runner are in this directory. No business writes were performed.

## E15 Job 5625

Job 5625 was **not triggered**. The timer page was not opened. No contract/processKey/digest tuple or fileId/processDefinitionId was collected.

## Validation

- Full Maven package: `mvn -pl yudao-server -am -DskipTests package` completed with exit 0.
- Playwright runner syntax: `node --check ui-r5.cjs` passed.
- Real UI historical preview: blocked by the visible authorization error above.
- Runtime health: `UP` on PID 37016 / port 48061.
- No API invocation, database write, permission edit, or fixture creation was performed.

## R6 directed real UI correction (2026-10-21)
Authorized directed rerun r6-directed/verification-report.md: PASS for Main A/1 historical body. Browse enabled; real click opened exact VERSION_HISTORY/A/1; original-only PDF marker visible, newer marker absent. Earlier R5 disabled-preview inference is superseded, not a proven product failure. Prior accepted A/1 PASS preserved. Does not claim all E22 or E2E01–24 complete. Owned browsers closed.

## R7 launcher restoration RED/GREEN (2026-10-21)
Scope only restart-qms-r5.py and test_restart_qms_r5.py. Original key purpose verified against doc/tasks/20261013-dcc-project-version-e2e/start-verified-runtime.py lines14-18 and technical-key-provision.json. No provision script executed.
RED: python -X utf8 doc/tasks/20261021-e22-r5-historical-body-context/test_restart_qms_r5.py -> 5 FAIL: missing explicit original-baseline-key assembler.
GREEN: same command -> 5/5 PASS. Covers original exact key, existing environment/config preservation without mutation, missing file, empty/short file without key disclosure, invalid existing JSON, and UTF-8 byte minimum.
Real configuration preflight: load g56.environment() and call only with_original_baseline_key with original private file. PASS: all preexisting config/env preserved, original key usable; no launch/log creation, key output or persistence. No real key fixture or config JSON written.
Launcher now fails on missing/blank/<32-byte key before port probe/log creation/Popen. It has no stop operation. Root must perform configuration preflight before any externally managed stop. Existing e22-r5-20261021 logs untouched; new launcher log directory e22-key-restore-<timestamp-with-microseconds> created with exist_ok=False.
Runtime remains backend37016/48061, frontend20976/8061. No build/repackage/restart, production token code change, Git commit or push. E22 attachment fixture still BLOCKED pending Root-reviewed restoration and resumed real UI. R6 accepted PASS unchanged. Launcher correction is GREEN and ready for Root review, not proof of restored running configuration.

## R8 reviewed exact-transition restoration — ready_for_closeout (Root review)

Authorization: Root accepted runtime configuration diagnosis and authorized only task launcher/tests/records; Root owns the sole later restart. No production file changed.

BDD recorded before edits in task.md. RED command: `python doc/tasks/20261021-e22-r5-historical-body-context/test_restart_qms_r5.py`. Result: 12 run, original5 PASS, new7 FAIL because with_verified_definition_transitions was absent. GREEN same command: 12 run,12 PASS (0.433s). No runtime/browser/business actions occurred.

Change: restart-qms-r5.py calls with_verified_definition_transitions after the existing original-key helper and before port probe/log creation/Popen. It reads only four fixed reviewed evidence paths. SHA pins (exact current reviewed bytes):
- original registry cef21ccc2dc4e1a48fbf8bd47885afe97a1fcd89e4bff094d4d54ab91e81367e
- expanded registry eca016d3ab2ed4beb6f66e32d549d35ed47aef2513c27edf237064d9b36bde96
- deployment evidence cb83e9e3eed77f7e1245f8932f8dda2b2d2bb0165109f8cab6e2f916613824d7
- approval evidence ebf4d336959297bd2f3ebce627cf9370f6a7d4907b65db9eedf8b818924b6d06

Checks retained from original launcher: tenant1/upload->revision scope, original registry hash, old row preserved exactly, two distinct source tuples, exact row fields, v6 IDs against deployment evidence, v6 digests against approval evidence. Additional byte pins reject any evidence change, including coordinated file changes, requiring new explicit review. Existing identical registry is accepted; conflicting/empty/null/partial registry fails closed. No latest-definition lookup or new mappings. Original-key helper unchanged. No font/r4 config assembly added. Existing schema=false assertion and startup-sync argument exclusion unchanged.

Coverage: exact two rows and all unrelated config/key/env retained without mutation or stdout/stderr; identical config accepted; each of four evidence files missing or byte-changed fails; conflicts fail; invalid Spring JSON fails; assembly precedes port/process side effects; no r4 startup-task import. Existing5 key tests still pass.

Safety preflight: `r8-transition-preflight.json`, captured2026-10-21 11:03:14+08:00, PASS_CONFIGURATION_ONLY_NOT_RESTARTED. AST imported only function/import nodes; g56.environment and original private key assembled in memory, then transition helper called. Equality checked after removing only added transitions (and new empty parent if applicable), all other env/config including key/scheduler unchanged. Verified2 exact rows; original runtime arguments contain schema=false; startup-sync options removed, no enabled startup-sync config/env observed. No Popen/port probe/service action, no business requests, no credentials persisted/printed. Evidence records safe rows and source/test fingerprints only.

Review fingerprints: launcher SHA256 c68875c5811d0a202eec2373a1d3adff9359c7ef610150b034519229439a5787; test SHA256449e47fd2e89a7d46ea58abb16d944d3b5c2d27410fb95a6edd0aa238380bed9.

Limit: configuration assembly PASS is not runtime restoration or business E2E PASS. Services were not restarted; corrected checkin not retried; A2 isolation remains BLOCKED until Root review and runtime restoration. Overall task remains blocked; code-review subphase ready_for_closeout. No Git submission or cleanup apply; preserve review evidence and resumable fixture assets.
