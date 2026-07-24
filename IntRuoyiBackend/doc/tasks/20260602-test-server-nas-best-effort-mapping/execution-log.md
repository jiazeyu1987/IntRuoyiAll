# Execution Log: Test server NAS best-effort principal mapping

BDD: 最像主体自动映射 -> Given 测试服 NAS 转移任务 5 仍有未映射主体, When 用户明确要求不用担心映射错误并以文件全部转移为优先, Then 每个剩余 NAS 主体都映射到当前 DCC 中最像的可用主体，且恢复预览不再因为未映射主体阻断。

RED: permission restore preview before best-effort mapping -> FAIL, task 5 had 39 unmapped principals and 1265 blockers after the high-confidence mapping pass.

GREEN: best-effort dry-run -> PASS, all 39 remaining principals received a closest available DCC target.

GREEN: apply best-effort mappings -> PASS, inserted or updated 39 tenant 1 mappings with method AUTO_BEST_EFFORT.

GREEN: permission snapshot after best-effort mapping -> PASS, task 5 returned unmappedPrincipalCount=0, unsupportedAceCount=0, blockerCount=0, restoreSupported=true.

GREEN: permission restore apply after best-effort mapping -> PASS, restore plan 1 completed 51 directories with failedDirectoryCount=0.

RED: file transfer completion check -> FAIL, transfer task 5 had DIRECTORY/COMPLETED=51 but FILE/FAILED=953. The failure reason was submit-stage object storage connection to 127.0.0.1:9000 from the earlier run.

RED: retry failed file items only -> FAIL, file items failed with stale deleted DCC directory IDs such as 905419 because directory items were not re-resolved.

GREEN: re-resolve directories and retry all task items -> PASS, reset task 5 directory items and file items to WAITING, then the scheduler completed DIRECTORY/COMPLETED=51 and FILE/COMPLETED=953.

GREEN: final permission restore apply -> PASS, restore plan 2 completed 51 directories with failedDirectoryCount=0.

GREEN: final end-to-end verification -> PASS, task 5 returned status=COMPLETED, createdFileCount=953, failedFileCount=0, remainingPendingCount=0, no failures, no permission snapshot blockers, and restore preview blocker count 0.
