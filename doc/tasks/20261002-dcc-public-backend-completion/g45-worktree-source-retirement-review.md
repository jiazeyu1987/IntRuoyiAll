# G45 readonly worktree source/asset retirement review

2026-10-04 snapshot; Root exclusively owns current archive, Git integration and removal. This reviewer did not commit/stage/merge/push/prune/delete/copy worker sources, start/stop services, run Maven/browser/DB/API or edit original workers/Root files. Only this report is written in the existing integration backend task. Generated node_modules/target/dist were excluded from source comparison, not treated as missing delivered source.

## Branch/commit state is not source completeness

All A/B/C/D and integration heads are `a801dc8b91579241e221d129ab34343997673f40`. Each has **zero commits unique versus current int_qms**. Their branches are respectively `codex/20260930-dcc-a`, `...-b`, `...-c`, `...-d`, `codex/20261001-dcc-integration`; main is int_qms. This means a bare branch merge would not transfer their uncommitted development. It does not mean there were no成果. Readonly git status lists substantial tracked/untracked assets:

| Worktree | Changed tracked paths | Untracked paths | BE/FE source/test/SQL/script paths compared | UTF8 LF equal to integration | Different text | Missing path in integration |
|---|---:|---:|---:|---:|---:|---:|
| A |126|256|335|237|98|0|
| B |124|248|305|203|102|0|
| C |122|247|285|165|120|0|
| D |127|254|339|242|97|0|
| integration |158|664|500|—|—|—|

Numbers are a bounded status snapshot before this new report; task files/Root operations may add paths. Each actual source comparison decoded UTF8 with optional BOM and normalized CRLF/CR to LF. Raw byte differences solely from line endings were not counted as logic loss. Counts are paths, not tests/features. Every worker changed source path inspected through git's tracked/untracked list exists in integration; this fact alone does not prove semantics for every differing hunk.

## Imported evidence chain and later worker differences

Reviewed Root manifests: initial `integration-manifest.json`242files; `ad-delivery-import-manifest.json`62; `b-delivery-import-manifest.json`43; `d2-import-manifest.json`41; h01/h02/h07/h08 increments; final `f01-import-manifest.json` source317verified,109unchanged,41applied,0conflicts. Ownership/import records and `dependency-sync-manifest.json` explain why copies of otherowners' modifiedsource are present in each worker. Do not blindly choose any worker's whole tree over the current integration.

Against those actual owner-source import SHA records (latest known for each path): B117/117 currentrawmatch; D70/71currentrawmatch and its later `DccRelationControlledEventIntegrationTest.java` matches currentintegrationLF. A81/99rawmatch and C24/40rawmatch; rawdifferences include later deliveries/line-endings and integration-only repairs, so these cannot be interpreted as omittedfixes. Important differing paths were explicitly read:

- A `DccControlledFileLifecycleService.java`: currentintegration replaces oldcurrent-active-only obsolescence with the reviewed locked fullcontrolled-chain lower-version retirement/activation logic (G07). Worker is the earlier implementation; copying it would regress currentbehavior.
- A `DccControlledFileObsoleteServiceImpl.java`: integration resolves selecteddepartments before nativeOBSOLTEpreview/readiness and preserves taskroutefacts; worker had the earlier postresolution transformation. This is an integrationrepair, not an absent workerfix.
- C `DccControlledFileRevisionServiceImpl.java`: integration adds lockedsourceplacement, verifiedexistingnameprojection, precise failedcandidate/rework attempt/history and same-targetreuse. Worker is the earlier candidateallocation implementation that said reusepolicyundefined; copying it would regress confirmedA/2retry.
- C `revision-model.ts`: integration keeps exactrevisionIDs/attributes and adds sharedbuildApplicationFields/buildInitialCommand used by the actual parent. The worker's corepartial/replacement fields remain represented; the difference is an addedintegrationinitialpath.
- SharedDO/VO/Query/Workflow/schema/test files differ because integration contains subsequent owner-selection, legacysealedsource, formalBPM/attribute/readiness/visiblehistory and mainline fixes. Current publicdetail/browser/upload sources intentionally supersede component-only worker baselines. No specific necessary worker-only laterfix was discovered by this bounded import/diff read. This is **not** a blanket semanticPASS for all~100 differingpaths perworker; Root's archive retains them for traceability and conflict review.

No worker source file is absent at the corresponding integration path. No automatic overwrite/import is recommended now. Root should retain the accepted integration snapshot and its current verification evidence as the candidate to int_qms, preserving worker sources/manifests before removing their worktrees.

## Worker task provenance must be archived

Original task directories remain mostly untracked and are **not** represented at the same paths in integration:

| Worker task | All task files observed | Permanent-format candidates | Candidates missing at same integration path |
|---|---:|---:|---:|
| `20260930-dcc-a-workflow` |198|28|28|
| `20260930-dcc-b-project` |195|43|43|
| `20260930-dcc-c-version` |170|51|51|
| `20260930-dcc-d-relations` |223|23|21|

Permanent-format counts are doc/json/yaml/SQL/script/diff/vue/TS candidates, not a decision to put all rawlogs/generatedreports in Git. Preserve fulltaskprovenance in protectedarchive before removal. Especially A reviewfreeze/h08/selectediteration manifests+handoff/diff/verify-directed; B h06/projectdiscovery/viewdirectory/rootreview/currentchangedfiles/migrationclosure; C h08/h09/lifecycleaudit/post-manager issue-response/count/fingerprint records; D reference/upload-relations/h04 manifests and usageVue integrationexamples. Root-copied f01 manifest evidence does not replace all originaltasknotes/latefix records. No worker ignoredpermanentscript was found within these taskdirs using exact-directory git ignoredquery; rawlogs/screenshot/generatedoutputs still require archive policy, not sourceintegration.

## Integration ignored task scripts need explicit retention

Git's scoped ignored-file inventory found13task `.cjs` source/test candidates in the three current integrationtask directories. They must be included in protectedarchive and, when chosen as permanentrepositoryassets, force-added explicitly; sameHEAD/untrackedstatus does not surface ignoredfiles:

- backendchild G27 helper+helpertest;
- G29 real-lifecycle runner+contracttest;
- G30 version/relations runner+contracttest;
- G31 distribution/obsolete runner+contracttest;
- deferred G34 negative-runner/observer/contracttest (**unfinished/unvalidated, retain that status**);
- public-browser `dcc-public-ui-acceptance.e2e.cjs` and `verify-ui-acceptance-preparation.cjs`.

These13paths were the actualignoredscope result, not all ignored artifacts globally. PermanentKeep records already exist in their respectivetasks; Root's current Git/archive inventory must preserve them, along with currentG35/G39/G43/G44 tools/SQL/seals/receipts/reports. Do not archive node_modules/target as a replacement for reproducible source. Protected runtimebackup/sourceobjects/credentials must not be copied into Git taskassets merely because they are evidence.

## Existing six non-task protected assets

The existing protected set is main+integration `AGENTS.md`, infrastructure `FileController.java`, and `FileControllerTest.java` (six path/workspace pairs), as recorded by Root `g26-preserved-assets-check.json` and laterchecks. Preserve those actualbaselineassets exactly and do not let this DCCretirement merge overwrite main's unrelatedmanualchanges. This reviewer did not change or expose their sensitivecontent. Root owns the final fresh hash check/archive/merge resolution.

## Retirement conclusion and Root checks

No absent worker source path or concrete unabsorbed necessary workerfix was found in this boundedreview. The major retirement risk is losing **uncommitted and ignored provenance**, not a uniquecommittograph. Root should complete currentprotectedarchives+exactassetinventories, commit onlyreviewedcandidateassets on the authorizedlocaltarget, resolve main's existing non-task changes explicitly, verify candidate→int_qms ancestry and sourcehashes, then remove only approvedA/B/C/D/integrationworktrees after their archive/retention evidence succeeds. This report grants no cleanup or Git execution and claims no actualmerge/retirementPASS.
