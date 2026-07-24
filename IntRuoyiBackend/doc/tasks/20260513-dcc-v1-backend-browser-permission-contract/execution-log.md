# Execution Log: DCC v1 backend browser permission contract

BDD: 普通用户只看到有 query 权限的目录树 -> Given DCC 目录中存在多个根目录与子目录且访问规则只授权其中一个分支 / When 普通用户请求 `/dcc/directories/tree` / Then 后端只返回该用户有 query 权限的目录以及必要的祖先目录，不暴露其他目录分支。
BDD: 管理员仍可看到全量目录树 -> Given 当前用户具有 DCC 目录管理或访问规则维护权限 / When 请求 `/dcc/directories/tree` / Then 后端返回全量启用目录树，不受目录 query 规则限制。
BDD: 受控文件分页对 browser 风格查询执行目录权限过滤 -> Given 普通用户请求受控文件分页且未指定自己为 requester / When 其 query 权限不包含目标目录 / Then 后端返回空结果或仅返回其有 query 权限目录下的记录，不静默放开越权目录。
BDD: 提交人查看我的文件不受目录 query 限制误伤 -> Given 提交人请求自己的受控文件分页且 requesterId 等于当前用户 / When 目录 query 权限并未覆盖该目录 / Then 后端仍返回该提交人的记录，避免 my-file 页面被误过滤。

## Evidence

- M1: Completed. Previous backend task `doc/tasks/20260513-dcc-v1-backend-user-flow-contract/task.md` is completed.
- M2: Completed. This task document, execution log, and backend API evidence were created before production code changes.
- M3: Completed.
  - RED: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\.worktrees\dcc-v1-backend\pom.xml -pl yudao-module-dcc -Dtest=DccDirectoryAdminServiceImplTest,DccDirectoryAccessPermissionServiceTest,DccControlledFileWorkflowServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL, directory tree filtering, browser-style page visibility enforcement, and supporting permission helper classes did not exist.
- M4: Completed. Added `DccDirectoryAccessPermissionService`, current-user-aware `/dcc/directories/tree`, ancestor-preserving visible-directory filtering, and response-tree children support.
- M5: Completed. Updated controlled-file paging to honor current-user query-visible directories for browser-style reads while preserving requester-owned my-file queries and administrator bypass.
- M6: Completed.
  - GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\.worktrees\dcc-v1-backend\pom.xml -pl yudao-module-dcc -Dtest=DccDirectoryAdminServiceImplTest,DccDirectoryAccessPermissionServiceTest,DccControlledFileWorkflowServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS.
  - REGRESSION: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\.worktrees\dcc-v1-backend\pom.xml -pl yudao-module-dcc -am -Dmaven.test.skip=true package` -> PASS.
  - GREEN: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260513-dcc-v1-backend-browser-permission-contract/backend-api-evidence.md` -> PASS.
- M7: Completed. Task-specific backend browser-permission changes were committed as `51bbd80bce` with message `任务: 补齐DCC浏览权限后端契约`.
