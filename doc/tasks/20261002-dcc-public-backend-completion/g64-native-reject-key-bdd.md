# G64 native rejection callback key

- Status: ready_for_closeout; Root sole runtime/DB/UI/Git, child sole finite Java/Maven.
- Actual trigger: the real REVISION countersign rejection reached synchronous Flowable completion, but DCC's exact status CAS returned zero. Actual SQL still required the legacy `dcc-controlled-file-approval` key. This corrects the earlier unconfirmed reentrancy hypothesis and supersedes the no-new-defect conclusion of the prior read snapshot.
- Production scope: DccControlledFileMapper's reject CAS expected key argument and its validated FinalizationService caller only. BPM cancellation/event machinery and DCC signatures/route guards remain strict.

BDD: Given a real tenant-1 Flowable native upload/revision (and preserved legacy approval) with an exact in-review DCC file and shared candidate, When the actual BpmTaskService rejects it and synchronous process completion publishes its actual result, Then the real SQL CAS uses that verified definition key; DCC/shared state becomes REJECTED, task/history remains rejected, open candidate closes, and no control facts are created.

BDD: Given that same transaction has a newly recorded isolated signature projection, When an enclosing action fails after the actual rejection callback, Then signature/DCC/shared changes and Flowable rejection all roll back; the original live task remains. Authentication/HMAC is an explicit isolated port, not real front-end signature evidence.

BDD: Given a mismatched event/definition identity, When the rejection callback is attempted, Then it fails before the status update. The new key parameter must not remove tenant, file, BPM, status, deletion or one-row CAS constraints.

Validation: effective RED via actual Flowable completion + H2 Mapper (not mocked SQL); GREEN after the finite key parameter fix; existing FinalizationService and failure/retry identity neighbors only. Raw XML/logs retained and counts reported separately from real E2E.


## Effective RED / GREEN / regression

- Initial runner: g64-native-reject-effective-red.log / .xml, 5 setup errors (Flowable dialect); fixture-only, not business RED.
- Effective RED r2: CLI 1, actual same-tree reactor, 5 cases: 4 errors exactly `DCC reject lost its status CAS` for native upload/revision, 1 legacy success. g64-native-reject-effective-red-r2.xml preserved.
- Production fix: reject SQL binds exact validated processDefinitionKey rather than legacy literal; all identity/status/deletion and rowcount constraints retained. Two production files only; two tests (new transaction host plus necessary current mock signature update).
- GREEN/regression: g64-final-native-reject-regression.log, CLI 0, BUILD SUCCESS 2026-10-06T01:34:21+08:00. Actual XML 120/4, failures/errors/skips zero: real rejection transaction7, existing Finalization59, same-target retry50, native final completion4. Counts overlap historical runs and are not cumulative.
- Actual guard test proves foreign definition rejected; late enclosing failure after a successful real rejection rolls back real Flowable process/task, DCC status, shared candidate and actual isolated signature row. No DB schema/real history/API/E2E/role/route mutation.
- All four final XML copied byte-for-byte to g64-final-junit; source/class manifest and verification receipt sealed. Source/target/Maven frozen and released before Root package; current runtime real signature acceptance remains Root responsibility.
