# G23 fixed local-test source upgrade preparation

ready_for_closeout for Root independent Review. **27 offline tests PASS**. The only concrete prepared output is **PREPARED_BLOCKED_MISSING_REAL_REHEARSAL_RECEIPT**, writeConnectionAllowed=false. No real clone PASS exists yet; no source fresh25/current backup/snapshot/exclusion receipt is fabricated. No actual driver invocation, DB query/write, service/Docker/Maven/Git or formal Java/SQL/Root-support/G21 modification occurred in this batch.

## Separate source tool and inputs

`g23-source-upgrade-driver.py` has prepare/validate/upgrade only for the fixed local int-ruoyi-mysql / ruoyi-vue-pro / UUID92ca05d0-aec8-11f1-a944-02b4e226a5ef / MySQL8.0.40. It imports the frozen G21 helper by exact driver SHA86e9e8b5504f926dfd1d7ad529afe006393289ec811378232ded997c097e99a9. It never changes the frozen clone driver/support/tools or implements database create/restore/delete/service control. The source-specific postflight function requires ruoyi-vue-pro explicitly and cannot pass a clone schema result.

Complete executable input field contract is `g23-source-upgrade-input-contract.json`; editable request template is `g23-source-upgrade-request-template.json`. Descriptors require raw file SHA and exact protected path. Fresh current source receipt directories are new direct children beneath C:/IntRuoyiBackups/20261003-dcc-integration. Root collects and seals actual receipts later. All operation proof files stay protected; repository files contain no actual business row dump/credential.

No real runtime inputs are available now. Disk-only `prepare` with no clone descriptor yields the explicit blocked status before loading a client. A supplied actual clone PASS plus missing source artifacts yields PREPARED_BLOCKED_MISSING_FRESH_SOURCE_INPUTS. Neither status authorizes a connection/write. Complete prepared receipts still have writeConnectionAllowed=false; upgrade independently requires `--authorize-local-test-upgrade` and Root's actual user authorization record/reference. A CLI flag/receipt boolean is a technical gate and cannot establish session authorization; Root may invoke it only after obtaining real permission.

## Exact clone evidence, not a PASS string

verify_clone_pass validates the actual hashed driver journal inside one protected run directory, marks offline fixture descriptors ineligible, and requires every sibling artifact in a relative-path SHA manifest. Paths cannot escape; missing/extra/rehashed inconsistent files reject. Exact restore schema/data receipts reference protected original prepared dump hashes with exit0 and protected stdout/stderr. Original16+ledger snapshots are compared by frozen exact helper, complete V3-copy facts bind27info/10procdef business projections, and raw33 newly selected config facts recompute the expected new primary IDs/payloads. First/repeat19 SQL marker order/material/prefix/wholeinput hashes and exact same-session clone environment are verified.

Raw first/repeat ledger19 facts must equal the ledger proof and the actual appended keys; every row's migration/file/SHA/op/release/env/status/deleted/tenant is exact. Each17-table rawcount must be zero and match the summary. Raw restored922 and final939 table identity sets must match original922+new17. Actual first/repeat schema JSONL is revalidated through the frozen detail validator against its original environment, and must reproduce exact sealed schema proof/fingerprints. Entire first/repeat row snapshots are identical. Simply copying a successful status label, adjusting a summary, or providing33 correct-count but wrong-content rows cannot pass.

## Fresh source and backup boundary

Root chose **maxAgeSeconds=900** solely for technical preflight evidence. It is not business date policy or approval expiry; elapsed time never supplies permission. Timestamps cannot be future or exceed900 seconds at validation. Each fresh25 group seals captureReceiptSha256 and must match the actual capture timestamp, database/server/version/query/facts/tool hashes. Both original Owner validators re-run in memory against the captured facts; a prewritten PASS/proof hash alone is insufficient. Exact25 IDs must equal the formal19/44 dependency closure. No old ledger/APPLIED rows are created to satisfy missing execution history.

New source backup requires its own protected child directory and actual source connection/epoch identity, three verified gzip artifacts, sourceSnapshotSha256 and writerExclusionReceiptSha256. Each artifact has a hashed command receipt sealing kind/sourceDB/container/UUID/captureId/exit0/path/rawhash/command profile/start-end times. The collector profile is mysqldump-readonly-fixed-local-no-routines-events-no-credentials-export. Actual dump tables are parsed: exact922 schema identities,210 data scope,17 affected originals including ledger. Reusing old G18 artifact paths rejects. Equal payload bytes from a truly fresh unchanged source are allowed; content difference is not manufactured as proof of freshness.

Epoch ordering requires writer exclusion <= each proof/baseline/environment <= backup start <= each dump start/end <= backup completion, with all applicable evidence within900 seconds. These are honest Root collector provenance contracts, not a claim that arbitrary invented JSON establishes history. Root must actually collect the new source facts/backup under excluded writers. The old clone baseline cannot stand in for current source snapshot.

The source baseline envelope requires origin ACTUAL_SOURCE_READ_NOT_CLONE, same actualDB/UUID, fresh timestamp, complete16+ledger rowhash structure and recomputed aggregates. On live prewrite the tool independently discovers the complete original column list from information_schema via frozen snapshot support, then compares it to the sealed source baseline. It does not trust a caller-supplied reduced columns=['id'] list. Postwrite keeps this full actual column projection.

Writer gates run immediately on connection and immediately before the one migration write: zero active transactions, zero other non-daemon client connections across all databases (including DBNULL), zero enabled events across schemas. Root's allKnownWritersExcluded receipt covers maintaining exclusion/fencing throughout collection and upgrade; the driver never stops/restarts writers or treats a zero snapshot as a permanent barrier. If any writer/exclusion is unknown, Root must not authorize invocation.

## Future write and postflight sequence

After all real inputs and explicit authorized invocation:

1. Revalidate every disk hash and fresh evidence; instantiate only the fixed SOURCE client.
2. Read fresh server/writer identity, full original16+ledger columns/rows and exact source environment; compare to sealed fresh baseline. All17 new target tables and all19 candidate ledger identities must be absent. Existing targets stop rather than guessing retry state.
3. Capture actual six V3 source business-copy digests. Revalidate freshness/materials again and reread writer state immediately before writing.
4. Execute only the original frozen nineteen-item first.sql bytes, with the unchanged pure environment SELECT prefix. Source never runs a repeat script,25 audit configuration or quality registration. Record material/prefix/whole SQL SHA, explicit write-attempt and possible implicit-DDL-commit state before execution.
5. Require exact38 ordered markers and same executing-connection source environment. Preserve protected stdout/stderr and each readonly raw fact/query hash. Compare every original row/column and old ledger against prewrite, permit exact33 new configuration and19 task-only ledger rows, require17 new structural tables empty, and run source-specific formal schema postflight.
6. Success means LOCAL_TEST_SOURCE_NINETEEN_UPGRADE_PASS_NOT_APPLICATION_READINESS only. First failure becomes FAILED_SOURCE_UPGRADE_STOPPED_NO_AUTOMATIC_RECOVERY and retains exact private evidence. No automatic ROLLBACK promises for implicitDDL, database restoration/deletion or service control. Recovery is separate Root authorization/work.

## BDD / effective RED / GREEN / review findings

- g23-source-upgrade-red.log: two initial missing-driver behavioral gate assertions fail before implementation; no client was created.
- g23-source-upgrade-offline-final.log:27/27 tests PASS. Disk-only blocked prepare, explicitflag/actualRootreference, fixedtarget/UUID, exact900 freshness, source writer/event/connection, wrong clone status/path/hash, frozen helper drift, source-only postflight, fresh actual25validator replay, changedform/executor despite rehashed artifact, actualscope/currentbackup metadata and gzip corruption, omittedsourcecolumns, missingcurrentledger/newtables, firsterror and copiedseed mismatch are covered.
- Full fake source pipeline asserts the whole write bytes equal prefix+the original nineteen script and exactlyone write occurs; all create/restore/delete source operations are absent. Tests use explicit fakes and temporary directories; they never establish runtime source or clone PASS.
- Complete temporary clone fixture invokes actual frozen config/history/schema validation; resealed rawledger/newtable count/fulltable identity tampering rejects. Fixture provenance is explicitly offline in the test/report, never exported as real prepared inputs.
- Independent upload Owner Review identified R01 reduced original-column snapshots, R02 trusting proof labels/time, R03 backup provenance/scope and R04 summary-only clone evidence. All have concrete enforcement described above and matching offline negative coverage; old G21/R01/R02 receipts remain unchanged.

## Remaining work and handoff

Frozen hashes are in g23-source-upgrade-delivery-fingerprints.json. Current prepared output remains blocked for missing actual clone evidence. Root performs independent Review, obtains real DB approval, runs isolated rehearsal, collects fresh25 source proofs/current source16+ledger snapshot/new backups/writer exclusion/environment, and seals complete source request. Only then may this separate source tool validate and be authorized. No actual migration/DB/service/runtime/E2E readiness is claimed here.

## Independent final Review

UploadOwner independently verified exact frozen driver/test fingerprints, closed R01–R04 implementation issues offline, and reported six additional pure-memory/AST checks PASS. Those checks are reviewer-owned, separate from this tool's27test suite. Root has received the Review. The actualprepared state remains blocked missing realclone evidence andfreshsource inputs; no actualflag/client/DB execution occurred. No further source edits requested, tools stay frozen.
