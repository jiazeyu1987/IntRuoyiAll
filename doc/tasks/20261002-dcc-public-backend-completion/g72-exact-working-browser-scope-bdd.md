# G72 exact working navigation candidate bound

Status: in_progress. Root actual ordinary account selected file4028 in the normal checkout browser, but route workingFileId/MasterId were client-only and the server scanned33425 file versions before permissions/aggregation/keyword. No deadlock; new pair is a candidate bound, never an authorization.

BDD: Given an exact existing same-tenant file/Master pair from the normal storage operation route, When browser-page loads, Then only that Master's complete candidate version IDs are submitted to the existing summary Mapper with its original directory/status/taxonomy filters and assignment intersection. Original permission→canonical/latest aggregation→final keyword order and history/selectedversion remain intact; unrelated Masters do not reach expensive per-row authorization.

BDD: Given an older matching keyword and nonmatching canonical current version in that exact chain, When latest browser loads, Then no older result is substituted. Given denied scope, malformed/half/wrong/foreign pair or a non-storage shared endpoint, Then no authorization or broad-scan fallback is introduced. Absent pair retains exact original list semantics.

Scope: PageReq two optional positive Long fields workingFileId/workingMasterId, both or absent; Query ordinary storage browser only resolves verified tenant/file/master chain and applies bounded IDs through its existing Mapper overload. Nonstorage/page/export requests reject the pair rather than ignore it. Frontend owner passes the already validated exact route pair into this existing browser request; no new endpoint/index/cache/job/permission/runtime timeout or other filter changes. Root owns actual UI/runtime/DB/Git; child unique finite backend/Maven.
