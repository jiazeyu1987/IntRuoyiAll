# G47 LD02 free upload — independent readonly boundary review

Initial bounded source read while BE/FE owners implement their assigned TDD. No source/test/DB/API/browser/service/Maven/Git mutation by this reviewer; only this report. LD01 typedproductresolver is frozen and not reworked. The fourcolumnDBupgrade is still Root's separately asked execution scope; currentDO/newfields cannot be used against unupgradedruntime as proof of a productlogicfailure.

## Positive direction and retained hard qualifications

Normal NEW_UPLOAD should choose project/logicalfolder/actualenabledfiletype and realFile, without a projecttemplatefilenamewhitelist. The current BE increment removes only oldprojecttemplatevalidation from `validateSourceUploadContext:695–698` and `prepareSubmitContext:2259–2262`; template service/admin/suggestion functions themselves remain available. This is the correct narrow direction. FE free-type/filename implementation was not yet complete in this source snapshot, so its oldcontrols are not reported as a newregression or finaldone claim.

- Preview service `DccControlledFileUploadServiceImpl.uploadPreviewFile:96–151` stillvalidates exactlyoneactualmultipart/nonemptyoriginalname, supportedsourceextension and genuinePDFbytes, categoryUPLOADpermission, actor/scope/session andsourceuploadcontext beforestorage. Actual NEWsource preflight receives `file.getOriginalFilename()` and computedactualbodySHA, not a template/displayname. `:178–179` requires exactactualmultipartname==infraStoredName==ticketName before returning successfulpreview. No synthetic originalname/ticketbody is introduced by removingtemplates.
- `DccControlledFileNameClaimService.preflightNewSourceName:44–81` stillrequires currenttenant, validrealbasename<=256/no slash, verifiedlegacyproof, completeoriginalcase/extensionname andexactownbound-unsentdraftreplay. `prepareSubmitContext:2314–2317` claims actualresolvedSOURCEticketfilename only; altereddisplaytitle orfileNumber doesnotreplace theoriginalfilenameoccupancykey.
- `DccSourceUploadSession.newUploadPrefix:17–19` remainsproject+taxonomy+displayname fingerprint; scopecontains thevalidatedclientsessionhash. Previewcontext andsubmit `resolveSubmitFiles:2166–2191` must match; actualtickets are actor/category/session/purpose bound. SourceOriginalFileName comes from resolved `source.fileName()` at`:2208–2210`, not caller sourceFileName. FreeFile.name caninitialize displayname beforeupload; anychangedname/type/project afterticket creationmustfollowoldticketcleanup/reupload, not mutatecontextusingoldsignedticket.
- `validateSourceUploadContext:664–698` stillvalidatescategoryUPLOAD,currentenabledproject, projectEDITOR/OWNER, requestedtaxonomy/path/leaf andcategorytaxonomyequality. CHECKIN keepsactualversion/Master/holder/editor andEXTERNAL_REVIEW branch unchanged. `prepareSubmitContext:2263–2278` keepsprojectOWNERforlegitimateexplicitrevision,editor/OWNERfornewfile andcategoryUPLOAD; `validateNewFileTaxonomyLeaf:2646–2660` rejectstypeswithactivechildren/invalidpath. Nofirsttype/category/defaultactor is picked tocompensate fortemplateempty.
- Submission retainslogicalfolderplacement/realtypedLD01product/requiredattributes/routes/departments/dates/nameclaim/version/ticket/BPM underoldtransactions. TheindependentcategorybounddirectoryleafremainsLD03scope, not silentlychanged inLD02. Removingthetemplateport doesnotlicensechangingcategory/fileTypeIDs, defaultproductcode or oldhistoricalidentity.

## Specific P1 mapping guard regression notified early

Old `DccProjectFileTemplateServiceImpl.validateUploadLocation/validateUploadSelection` called `validateTemplateTaxonomy:220–235`, whichalso required `activeCategoryCounts[taxonomyId]==1`. Cancelingtheseextra whitelistcalls mustretainthis genuinefiletypequalification.

At thiscurrentinitialsnapshot, Workflow replacement onlychecksrequestedcategory.taxonomyId==requestedtype andenabledleaf/path, inpreview`:690–698` andsubmit`:2274–2278`; itdoesnotcall theformal `DccFileTypeTaxonomyAdminService.resolveActiveCategoryId(type)`. Ifoneleafmaps totwoactivecategories, a callersupplyingeitherIDcanpass thoseexistingchecks, whereasoldtemplatevalidationrejectedambiguousmapping. Frontendactive-categoryAPIrefusingambiguitycannotreplace serverpreview/submitqualification.

SentBEownerandRoot anarrowfix: resolvethesameexistingformaluniqueactivecategory method (`DccFileTypeTaxonomyAdminServiceImpl:117–128`) andcompareactualcategoryIDinboth normalNEWpreview/submit, preservingtheleafguard. Nofiletemplatewhitelistrestoration/newplatform/schema isneeded. Owner testshould exercise actualambiguousmappingfailurebeforeticket/File/BPM. Thisisaconcreteintroducedguardgap, not anactualDBreproduction or a requestforwholeboundarynegativeplatform. **Open pendingOwnerfix/finalread** atthissnapshot.

## FE final-read focus when its increment is ready

Useexistingformal upload-options→userexplicitactualtype→uniqueactive-category, showfullpath notfirstcategoryfallback. Emptyoptionaltemplate suggestions do notmakefakeenabledtypes. InitializeactualsourceFile.name beforebuildingNEWsessioncontext; checkreturnedpreview.fullname matchesactualchosenFile andkeepvalidactor/category/scopedheaders. Changingtype/name/projectinvalidatesoldticket/previewbyformalcleanup. Publicsubmitcontinuesrealpreviewticket/fullsourceName/defaultactual/departments/date/training/relations/twoconfirm and serverproductprojection; neverinjectLD01catalogIDintoMDMfield or restoreprojectcodefallback.

## Initial read hashes and limits

Workflow50c185c5c7725bb162681b46246a8c2e8d648d131df68aa51de9f1b11315bdce; UploadService07f3b1769847b6c1dfae742ae25ff971c62804f6fea697181e6be9c4ea147d5d; SourceUploadSessionc45f29a9df1eea19c4e172f9093996b956bbdffe0c11aa88bec19f9f4caf29c9; taxonomyservicecbffc52b531ee828bcb158eb761bdbca89404306d0a535fc6d7c1fcff561a2ee. Theseare concurrentreadhashes, not finaldeliveries or testsPASS. Root/Ownerfinalhashes supersede them. Noadditional20year/legacy/quality/newbusinessrule isrequested.

## BE final-read closure — initial P1 resolved

The initial P1 above is historical and **closed** against the frozen LD02 backend delivery. `DccControlledFileWorkflowServiceImpl.validateSourceUploadContext:697` and `prepareSubmitContext:2281–2283` now both compare the requested category ID with the existing formal `fileTypeTaxonomyAdminService.resolveActiveCategoryId(requestedType)` result. That resolver rejects multiple enabled categories; the explicit leaf/path and category UPLOAD/project editor qualifications remain. This restores the genuine unique mapping qualification without restoring the optional project-template whitelist.

Read-only verification recomputed all three delivery file byte lengths and SHA-256 values against `g47-upload-template-fingerprints.json`; all matched. Manifest raw SHA-256: `4d18bfe65bad2fceb0a9184015d24123d74e347c3e6dd10473b97fe8756ccd7a`.

| Frozen asset | Bytes | SHA-256 |
| --- | ---: | --- |
| Workflow production service | 214186 | d7fa6f48ea509c01d4156381d7d74dfec1b36fcb441bcf70543fd27a0cd5509f |
| Workflow test | 293935 | 5d4a9a67d19a87d55b7164c6e720454481c544d355791e7f26e85b90dae107db |
| Source upload context test | 10929 | 42a58c7b2e983a6cda2c4c856804e8fbf5ebe6c48d660e606019f699627a57f5 |

The source contains `ambiguousActiveCategoryMappingStillBlocksNormalPreviewWithoutTemplate` in the context test. Root/BE owner reported the effective real taxonomy fixture RED followed by the final 240 executions across seven classes with zero failures/errors/skips at 23:05:29. This reviewer did not run those tests or any live action; the conclusion is the finite source/identity closure of this P1. FE final-read and LD03 single logical-folder storage mapping are separate pending increments.
