# G25 fresh actual-source input collector preparation

ready_for_closeout for Root Review.16 offline cases PASS. No actual source query, backup, collector invocation, S3GET, source upgrade, service/Maven/Git by this Agent. This tool only prepares the collection utility/queries and runs explicit fake transport/dump tests; Root performs actual collection. FrozenG21/G23/sourceproof/Rootsupport/materials remain unchanged.

## Artifact and fixed scope

Own `g25-source-input-collector.py` builds a disk-only plan and a Root-run `collect` mode. It only uses fixed LocalMysql.read and protected RecordingMysql snapshot capture, reviewedSELECT query texts and fixed local readonly mysqldump commands. It has no run_authorized_sql/upgrade call, database creation/restoration/deletion or application mutation entry. It requires source ruoyi-vue-pro/int-ruoyi-mysql/UUID92ca05d0-aec8-11f1-a944-02b4e226a5ef/MySQL8.0.40 and validates sealed19 scripts/25 proof/922schema/210data/16original+ledger before returning preparedsource inputs.

`g25-source-input-collector-contract.json` seals the exactG23driver/template/G21driver/cloneprepared and Rootscope/material/transport/capturehelper hashes. Disk-only plan validates all dependencies and lists six readonly query units: actualidentity, writer(trx/otherclient/events), complete922tableidentities, formal environment, freshstructure and BPM proofs. Query files themselves remain the original frozen Owner files; collector doesn't edit/regenerate them.

The source input plan is g25-source-input-collector-prepared-plan.json. It contains readonly queries and sealed path/hash references only, no actualsource body or credential. Rootwriter attestation template deliberately defaults allKnownWritersExcluded=false and exclusionReference=null; it cannot be accepted before actual Root affirmation.

## Root must supply

1. Exactactual user authorization file C:/IntRuoyiAll-int_main/doc/tasks/20261001-dcc-integration-unblock/g25-user-authorization.json. Collector requires isolated-first/source-after-PASS authorization and no remote/prod scope; referenced in resulting source request by exactfile+SHA, never interpreted as conditionalaudit permission completion.
2. Actualprotected clone journal C:/IntRuoyiBackups/20261003-dcc-integration/g25-rehearsal-run/driver-receipt.json. Its complete run artifacts are enumerated andrawhashed, then the frozenG23verify_clone_pass rechecksrestore/first-repeat/33config/19ledger/oldrows/new17/schema fromactual privatefacts, not a status label.
3. Root's own real writer exclusion attestation JSON with database/UUID, allKnownWritersExcluded=true, attestedBy=Root and exclusionReference. It must represent application/session/async/Quartz/Flowable/integration/directDB/event writers actually excluded; a DBzero snapshot alone is not auto-converted into this statement. Root maintains exclusion during all queries/backups/upgrade. Collector never stops/services or fences another task.
4. A new independent protection directory directly beneath BACKUPS, e.g. C:/IntRuoyiBackups/20261003-dcc-integration/g25-source-fresh-NEW. It must not exist, cannot overwrite priorcapture/backup. All real rawfacts,rowhashes,stderr,credential-freebackup/command/proof/request/prepared receipts stay there.

## Root command sequence

Optional disk-only preparation:

`python -B <own-task>/g25-source-input-collector.py plan --result <own-task>/g25-source-input-collector-prepared-plan.json`

Actual Root collection, after explicitwriterexclusion and while within900s freshness:

`python -B <own-task>/g25-source-input-collector.py collect --destination C:/IntRuoyiBackups/20261003-dcc-integration/g25-source-fresh-NEW --writer-attestation <Root-actual-attestation.json> --authorization C:/IntRuoyiAll-int_main/doc/tasks/20261001-dcc-integration-unblock/g25-user-authorization.json --clone-journal C:/IntRuoyiBackups/20261003-dcc-integration/g25-rehearsal-run/driver-receipt.json`

Collector does not automatically upgrade. It returns FRESH_SOURCE_INPUTS_VERIFIED_NOT_UPGRADED, databaseWritesExecuted=false, upgradeInvoked=false only when complete strictG23prepare(request) succeeds. Root then independently validates/invokes the existing strictG23 source upgrade using protected source-upgrade-prepared.json and its explicitactual authorization gate. Audit25+1/quality/legacyclaim work is separate and not added to nineteenSQL.

## Collection/data provenance

- Verifylocal identity and Rootattestation, readwriter query zero transactions/other non-daemonclients(allDB/null)/allevents; source receipt gets actualcollection UTC.
- Use existingLocalMysql read transport with one actualidentityprefix+originalquery for eachfreshOwner group. Preserve allquery rows, privateerrors, sourceconnection identity/queryrawSHA/factsrawSHA/captureactualtimestamp/selectcount. Regenerate only fresh group receipts in new directory; actualfacts revalidate through frozenOwnerpurevalidator and strictG23freshproof checks. No copiedclonefact/oldPASS substitutes.
- Freezeactualcurrent922tableidentityset againstrehearsed oldscope; reject17newtable/candidateledgeralready present. Discoveralloriginal columns live viaactualsnapshot helper throughRecordingMysql; capturefull16+ledger originalrowhashes/columnorder/originACTUAL_SOURCE_READ_NOT_CLONE; captureactualsourceenvironment separately.
- Readwritersagain beforebackup. Create exactlythree new gzip artifacts: fullschema(no-data), selected210data(no-create-info/skiptriggers), exactaffected16+ledger schema/data. Credentials neverleaveexistingcontainer: MYSQL_PWD from existingMYSQL_ROOT_PASSWORD inside sh; no-password values in argv/files/console. No olddump reuse, sourceDB fixed as argv anddump routing metadata sourcefunction rejects DB directives onlater strictrestoreproof.
- Eachactualdumpcommand receipt seals localDB/container/UUID/epochcaptureId/kind/exit0/start-end/path/hash/fixedreadonlyprofile andprivatestderr hash; gzip size/expansionintegrity validated. Parentbackup receipt seals sourcebaseline andwriterreceipt SHA, realsourceconnectionidentity,3artifactSHA andoriginalscope. No actualcredential orsourcepayload printed.
- Writercheckafterbackup plus secondactualfullrow snapshot must equalbaseline. Create freshprotectedrequest referencingclonejournal/fresh25/newbackup/baseline/environment/exclusion/auth. StrictG23prepare rerunsactualfreshchecks and900secondepochordering, completebackup922/210/17scope andallrawfingerprints. If capturetakeslong/drifts/fails it rejects; no silentagingexception orguessedfields.
- Firsterror stops andwrites sanitizedpublicsummary/privateactualerror. No sourceupgrade/DBDDL/DML/service/rollback invoked. Backupisfile-output ofreadonlymysqldump, not an upgrade.

## Offline tests and limitations

BDD then missingcollector effectiveRED2 assertions. g25-source-input-collector-offline-final.log16PASS includes disk-onlyqueryplan with subprocess mockeduninvoked, missing/wrongRootattestation/auth, actualprotectedcloneofflinehashvalidation withoutclient, freshqueryJSONL receipt/foreignID/unsafeSELECT/truncation/privatestderr, fakecompletepipeline withupgradepatchedraise,writerfailurebeforebackup,partialbackupstopsremainingfiles,actualsourcechangedafterbackup,strictpreparefailure, readonlydumpstreamgzip/commandreceipt,failedexitprivateerror andunsafe dump argument rejection.

One offlinecase independently verifies the now-real Rootclone receipt using localfiles/actualfrozenvalidators/hashes; this is not newDB execution. Allactualsource collection/backups remain Root work. RuntimeIdentity/database/source25/currentbaseline generated by futurelivecollect must not be confused with the test's explicitfakes. ExactSDK39bodyreader remainsseparate evidence anddoesnot enter this schema-source collection.

Root sourceupgradewrite is authorized atuserlevel butnotperformed bycollector. Actualnewfreshinputs still requireexecution; retainedG23previousblockedpreparedreceipt ishistorical ratherthan assertedcurrentruntime readiness. Root keeps soleDB/runtime/Gitownership.
