# 任务：DCC 删除父文件夹后端

## Task Goal

为 DCC 目录管理提供后端删除父文件夹能力：管理员在输入 `PROD` 确认后，删除目标目录及全部子目录、目录下所有状态的 DCC 受控文件，并同步删除这些文件引用的底层上传文件。

## Previous Task Check

- 上一个后端任务 `20260530-showroom-release-tenant-scope-fix` 未完成，已标记 `Blocked`，原因是用户要求立即执行本 DCC 删除任务，不能混入展厅发布租户边界未完成实现。
- 当前仓库存在与本任务无关的未提交改动；本任务不回退、不提交这些改动。

## BDD Scenarios

- BDD: 删除父目录完整子树 -> Given 用户拥有 `dcc:controlled-file:directory:manage` 权限，且父目录下存在子目录、全部状态的受控文件和底层上传文件 / When 调用删除父文件夹接口并输入 `PROD` / Then 目录子树、DCC 文件依赖记录、DCC 文件记录、纳入范围的 master 和底层上传文件都被删除。
- BDD: 确认文本错误时失败 -> Given 目录和文件数据存在 / When 调用删除父文件夹接口但确认文本不是大小写敏感的 `PROD` / Then 后端失败并且所有目录、文件和底层文件保持不变。
- BDD: 跨目录引用时失败 -> Given 目标目录文件的底层文件或 master 版本链被目标目录外记录复用 / When 调用删除父文件夹接口并输入 `PROD` / Then 后端失败并且不删除任何业务或底层文件。

## Milestones

- [x] M1：创建任务文档并记录 BDD 场景。
- [x] M2：补充后端 RED 测试覆盖成功、确认失败、跨目录引用失败和控制器契约。
- [x] M3：实现目录子树删除接口、服务逻辑、前置校验和依赖清理。
- [x] M4：运行后端 targeted 验证并记录 GREEN。
- [x] M5：补充后端证据、执行收尾预览并提交本任务后端改动。

## Expected Verification

- `mvn -pl yudao-module-dcc -Dtest=DccDirectoryAdminServiceImplTest,DccDirectoryControllerTest test`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260530-dcc-delete-parent-folder/backend-api-evidence.md`

## Cleanup Keep

- `doc/tasks/20260530-dcc-delete-parent-folder/backend-api-evidence.md`

## Final Verification

- `mvn -pl yudao-module-dcc '-Dtest=DccDirectoryAdminServiceImplTest,DccDirectoryControllerTest' test` -> PASS.
- `mvn -pl yudao-server -am -DskipTests package` -> PASS.
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260530-dcc-delete-parent-folder/backend-api-evidence.md` -> PASS.
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260530-dcc-delete-parent-folder --mode preview` -> PASS, no deletion candidates.
- Playwright E2E with current backend/frontend on `http://127.0.0.1:8099` -> PASS for directory parent/child delete flow.

## Current Status

Completed. Full real-file upload/approval/delete E2E remains a recorded environment prerequisite gap because no task-scoped approved controlled-file fixture was available in the test tenant; no mock/API fixture was used.

## Residual E2E Gap

- Full real-file upload/approval/delete E2E was not executed because no task-scoped approved controlled-file prerequisite was available in the test tenant; backend service tests cover the bottom-file deletion path and the E2E did not use mock/API fixtures.
