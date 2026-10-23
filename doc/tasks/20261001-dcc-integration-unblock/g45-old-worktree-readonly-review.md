# G45 older worktrees — bounded readonly retirement review

Snapshot after Root's actual local integration: main `int_qms` HEAD `ce88a18a295086285d27b2a59ba70fdf1f3fbea4`. Root records source127821/root33d5 and normalized820candidate/500source match with six protectedassets unchanged. This reviewer did not rerun that merge proof or mutateGit/files/services. Only this report is written; originalolderworktrees and main's unrelatedmodifiedassets are untouched. No tests/Maven/DB/browser/API/cleanup.

## `C:/IntRuoyi/20260918-001`

Branch `codex/20260918-001`, HEAD `8f08a64c728c868a18c1b75d50aa94c82348af69`. **15commits unique versus currentint_qms**, and `git cherry int_qms codex/20260918-001` marks all15 `+`: there is no samepatchID evidence that thesecommits are already equivalently merged. Do not call thisbranch fullyabsorbed or deleteitsrefs without a Gitbundle/retainedref archive. Most uniquecommits contain unrelatedmainprogramEDHR/Kingdee/AI/frontendbackendbaselinework; currentDCCgoal is not permission to automatically import them.

Uniquecommit subjects/shortIDs: db16e4277 EDHRordinaryproduction/releaseflow; aa2751632 EDHRcloseout; 2aa120ebc mainbaseline; 4139270eb/71ad43ac6 submitpushrestartdocs; 7a058f17f pendingbaseline; 15d72252e Kingdeetemplateunit; cd147f697 restartdocs; ff4392bfd mainbaseline; da5470688 followupbaseline; f9b62637d/ffa09296d submitpushdocs; 5fc6e7a08 mainbackendfrontendbaseline; 96145b5b9 DCCdualversion; 8f08a64c7 dualversioncloseout. Onlythree ofthesecommits listDCCproductionpaths: 2aa120 has registrationcertificateReminderConfigDialog; 5fc6 has onlinepreviewcontroller/test; 96145 has15DCCsourcepaths among23total. Pathpresence is not fullpatchsemanticequivalence.

`96145b5b9` implements **two separate uploaded bodies** READ_ONLY_VIEW/EDITABLE_SOURCE; currentint_qms single-sourcecontrolledflow does not contain those purposes in UploadTypePolicy/ArtifactRole/uploadsizechoices, nor the old migrations `20260919_dcc_controlled_file_dual_version.sql`, `20260919_dcc_dual_version_download_permissions.sql`, olddualversioncontracttest or itsdocs. This is a **real unabsorbed historical feature**, not a missingcurrentfour-modulefix. It is not required by the confirmedv1.6single-source/checkin/formalrevisiongoal. Do not merge it automatically into currentmainline; preservecommithistory/source/docs for laterownerdecision.

There are **9tracked realtextchanges +4untrackedtaskfiles**, not CRLF-onlydirty13:

- QueryService `resolvePreviewReferenceId` removes thepreferreadOnlyFileIdbranch. Currentmain already uses lifecyclepublished/stamped/source identity without thatoldfield, so theoldchange's narrowerintent is present in the newerflow; no needoverwriteQuery.
- UploadService catches ACCESS_DENIEDfromOnlyOfficebusinessreference and returns previewUnavailableReason instead ofthrowing. Currentmain has no suchcatch (sourcepreservation/readiness laterchanged); thisold“allowuploadwithoutpreview” behavior is not currentlyabsorbed. It is a fallback-style policy change, not automatically necessaryunder failclosed/currentHTML; archive rather than imports withoutreview.
- Infrastructure `BusinessFileAccessService` catches a providerresolutionfailure, continuesotherproviders anddeniesonlyifnone resolves. Currentmain stillfails immediately. This is a real unabsorbedinfra behavior outsideDCCcurrentfixscope; retain source/test forownerreview, do not silentlyoverwriteprotected/unrelatedinfra.
- `uploadSizePolicies.ts` adds READ_ONLY_VIEW/EDITABLE_SOURCEtypes/options, absentcurrentmain; partofoldtwo-bodyfeature.
- `ProjectCodeTabPanel.vue` changes `/mes/md/dcc-project-code` to `/mdm/project-code` and uses theconstant. Currentmain alreadyhas thecorrectactualroutepath inconstant androuter.replace; thephysicalstyle differs but theactualnavigationfix is present.
- Fourcorrespondingbackendtests anddualversionupload/downloaddoc are historicalevidence. Theuntrackedtask4 are `doc/tasks/20260919-dcc-dual-version-e2e/{task.md,execution-log.md,verification-report.md,verify-browser-downloads-real.e2e.js}`. Archiveall4includingtherealscript; currentmain doesnotinherit taskprovenance automatically.

Example rawolderhashes forarchivevalidation: infraBusinessFileAccessService `fd0d90b47e82c0bb93c6f4bd9cde842ca5b404ea2fe0345ed28662127a080c0c`; DCCUploadService `c57eab5ae7fe0d88bad7454cd79039eea84a0684e62bbed48155443aaa8c871e`; uploadSizePolicies `987c366eb6819ac7601a5f2950940143a1776130960b8c39a9b043ffcb63528b`.

**Disposition recommendation:** retain15uniquecommits via bundle/ref andall9dirtypatches+4taskfiles inprotectedarchive. No specificnecessarycurrentDCCfix was found that should be copiedbeforeworktreeretirement; several real historical/unrelatedfeatures are **not** absorbed andmustnotbelost. Root needs explicitarchiveverification, not “allpatchesmerged” justification.

## `C:/IntRuoyi/20260923-dcc-three-workflows-runtime`

Branch `codex/20260923-dcc-three-workflows-runtime`, HEAD `a9bcb6d36d96145ddc1252f111347b644b328deb`; uniquecommitsversuscurrentint_qms **0**. Worktreestatus16738entries =16431trackeddirty+307untracked. `git -c core.autocrlf=false diff --ignore-space-at-eol --name-only` leaves **204realtextdiffpaths**, so mostdirtyentries are lineendingnoise but this is **not entirelyCRLF**. No node_modules/target/dist recursion was used.

Of204realtrackedtextdiffs, **149match currentmain afterUTF8BOM/CRLF→LF normalization,55differ,0missingpaths**. A boundedDCCmodule/src plusfrontendDCCapi/views comparison:824trackeddirty;727LFsameasownHEAD,97actualchanges;737of824LFsamecurrentmain. Currentmaincontainsall45untrackedDCCsourcepaths. Wideruntrackedsource62paths:40LFsamecurrentmain,22different; theseprimarilyearlierproject/productcreate, nameclaims, actionpolicy and twointern-usertime-maintenanceVue components plusapi. Allcorrespondingpathsarepresent, notnewmissingcode.

ThedifferentDCCproductionpaths includeoldWorkflow/Query/Upload/Finalization/Obsolete/RouteReadiness/publicVOs/sharedparents, which currentintegrationlaterchanged withv1.6 OWNER/reviewer, exactselectedbody/rework, legacyregistration, matrixisolation andpositiveflowrepairs. Do not overwrite them withtheolderruntime snapshot. Thisboundedreview didnotinspectall55hunks or claimallsemantics-equivalent; preservefullactualnon-EOLpatch/currentuntrackedfiles inarchivebefore removal. RuntimealsocarriesBPM/signature/infra/MDM/intern-userchanges, which belongtotheactualuserparallelwork provenance, not a requestto expand thisDCCretirement intoallmoduleauditing.

**Disposition recommendation:** sourcepathinventoryfoundnoabsentnecessaryDCCfile; olderline-ending churn is not a reason to recopythewholetree. Retainactual204trackedtextdiffpatch+307untrackedassets,task/runtimeevidence andHEAD/ref inprotectedarchive. Rootmayretiretheworktreedirectory onlyafterarchiveverification/ownershipchecks under latestuserdirection; donotclaimunique0 alone meansallunsavedwork canbe discarded.

## Mainline boundary and final caution

Currentmain's AGENTS andinfraFileController/FileControllerTest remainuser-ownedmodifiedprotectedassets; neitherolderworktree comparison grants permissiontooverwrite them. RootremainssoleGit/archive/directorycleanupowner. Thisreport supportsreviewedretirementwithpreservedhistory; itdoesnotperformorcertifyretirement, importoldernonDCCfeatures, or verifybusinessE2E.
