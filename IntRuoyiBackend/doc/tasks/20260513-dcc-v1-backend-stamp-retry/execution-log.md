# Execution Log: DCC v1 backend stamp retry

BDD: 管理员对盖章失败的 DCC 文件执行重试 -> Given 已存在一条 `STAMP_FAILED` 的 DCC 受控文件，且导致失败的外部前置条件已经修复 / When 管理员调用 DCC 盖章重试接口 / Then 后端必须重新执行同一条受控文件的盖章发布流程，并把成功或失败结果明确写回 DCC 状态与盖章记录

## Evidence

- M1: Completed. Previous backend task `doc/tasks/20260513-dcc-v1-backend-approval-to-publish/task.md` is completed and committed in `5f8cf6805b`.
- M2: Completed. This task document, execution log, and backend evidence file were created before production code changes.
- M3:
  - RED: `mvn --% -f D:\wt\dccred2\pom.xml -pl yudao-module-dcc -Dtest=DccControlledFileFinalizationServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL, because `CONTROLLED_FILE_STAMP_RETRY_NOT_ALLOWED` and `retryStamp(...)` did not exist yet.
- M4:
  - GREEN: backend added `POST /dcc/controlled-files/{id}/stamp-retry` and a reusable `retryStamp(...)` flow in DCC finalization.
- M5:
  - GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\.worktrees\dcc-v1-backend\pom.xml -pl yudao-module-dcc -Dtest=DccControlledFileFinalizationServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS
  - GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\.worktrees\dcc-v1-backend\pom.xml -pl yudao-server -am -DskipTests package` -> PASS
  - GREEN: isolated runtime verification:
    - manually prepared file `2054545668044042241` as `STAMP_FAILED`
    - `POST /admin-api/dcc/controlled-files/2054545668044042241/stamp-retry` returned `{"code":0,"msg":"","data":true}`
    - database state returned to `STAMPED`, with `stamped_file_id = 2224`
- M6:
  - Pending Git commit in the owning backend repository after evidence finalization.
