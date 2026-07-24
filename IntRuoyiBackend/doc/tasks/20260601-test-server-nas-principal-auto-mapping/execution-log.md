# Execution Log: Test server NAS principal automatic mapping

BDD: 自动映射 NAS 主体 -> Given 测试服 NAS 转移任务存在未映射主体, When 按名称相似度匹配 DCC 用户、部门、角色或岗位, Then 只写入高置信度且无同分歧义的映射，低置信度或歧义候选必须保留为未映射并记录原因。

RED: permission snapshot summary before mapping -> FAIL, task 5 had 174 unmapped principals and restore was not supported.

GREEN: exact unique mapping dry-run -> PASS, resolved 174 NAS principals to readable NAS names and selected 130 exact unique mappings; 44 principals were skipped due to no unique exact DCC candidate.

GREEN: apply exact unique mappings -> PASS, inserted or updated 130 tenant 1 mappings with method AUTO_EXACT.

GREEN: conservative unique mapping dry-run -> PASS, selected 5 additional mappings using trailing-digit stripping or short unique username suffix matching.

GREEN: apply conservative unique mappings -> PASS, inserted or updated 5 tenant 1 mappings with method AUTO_CONSERVATIVE.

GREEN: permission snapshot summary after mapping -> PASS, task 5 tenant 1 returned snapshotStatus=CAPTURED, directorySnapshotCount=51, aceCount=2314, unmappedPrincipalCount=39, unsupportedAceCount=0, blockerCount=1265, restoreSupported=false.

GREEN: permission restore preview after mapping -> PASS, task 5 tenant 1 returned canRestore=false, directoryCount=51, ruleCount=5779, runtimeEnforcementReady=true, runtimeEnforcementBlocker=null, blocker count 1265.

BLOCKER: remaining principals -> 39 NAS principals still have no unique high-confidence DCC mapping candidate; restore remains blocked until these subjects are manually confirmed or created.
