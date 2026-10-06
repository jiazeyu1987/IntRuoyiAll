# G71 authorized detail independent from explicit historical relation permissions

Status: ready_for_closeout. Root real Doc can read target4028 ACTIVE, but detail fails because its eager relatedFiles legacy projection reads old obsolete4026 without relation-name permission. The optional detail field has no consumers; explicit current/history child endpoints own relation reads. Root owns actual UI/DB/runtime/Git.

BDD: Given actual Query authorization for the main active file and an immutable historical relation to an unavailable target, When its formal main detail is read, Then it succeeds without reading that historical relation, leaves optional legacy relatedFiles null, and does not grant target names/body. The explicit history relation read still fails with the unchanged actual Query name-policy denial, locally in that child panel. Original file/Master/relation rows stay byte-for-byte unchanged. Denied main hard scope still rejects the main detail.

Scope: remove only the unused eager setRelatedFiles(listRelatedFiles(...)) from Query.toRespVO. Do not filter, mask, catch denied targets, set fake empty list, redirect old relation to latest, create a new DTO/security platform, grant old body access or modify history. Test the actual Query/RelatedService/Resolver/NamePolicy on isolated H2 through the formal Controller.


## Actual RED / finite GREEN / release

EffectiveRED r3 (old one-line eager source restored only for the test) is exact real Query.assertRelationNameVisible1310→RelatedHistory→legacylist→toRespVO3004→authorized main getFile542, code1080000012;1test1error. Earlier duplicateA1 fixtureunique and main-onlygetFile540 missingPREVIEW were prerequisite fixture failures, not this business RED. Real directory3PREVIEW grants the source using unchanged actual guard, targetdirectory4/OBSOLETE still denied. No Query success mock/targetpermission bypass.

One production line removed: eager setRelatedFiles; optionalnullable untouched (not fake[]). Existing explicit history method and actual name-policy deny remain, main hard-scope denial still enforced. New test actual Controller/Query/Related/Resolver/H2 confirms source success/explicithistory denial and full oldFile/relation maps unchanged after each binary value is normalized to taggedHEX. DownloadPolicyService initialized as the real existing fixture requires, no defaultnull relaxation. Original binary-array address equality failure was a verification representation error, not loosened oldrow guard.

Finalcurrent-treeMaven56/4 all0/CLI0/BUILD SUCCESS2026-10-06T09:36:31+08:00 =metadata40/formalhistoryguard3/relatedservice11/multipart2, not cumulative with earlier runs. Four XML bytecopiedg71-final-junit beforeFREE. 1production/1test/7affectedmainclasses pinned g71-detail-relation-fingerprints.json SHA459302a8250c89f2c800f206ea8b326077379c4f3afacc8233a396689fbd9a3c; receipt5cc3f1a886aef7b67a7e4046f5ff99fe20997a85bc73f411e0f127a096ea6757. QuerySHA2f2d075638dc637d3db53d23a39a672911a7db54efca1a04e9d2abdd024bad3a supersedespriorQueryonly; G69other5assets exactunchanged. Source/target/Mavenfrozen. Rootfinalpackage/actualDoc4028detailpending, childnoactualDB/API/UI/services/Git/package.
