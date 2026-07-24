# Task: DCC 审批岗位从 IntAuth 一次性导入并改为本地只读

## Goal

让 DCC 审批岗位支持从 IntAuth 一次性导入到本地 `dcc_approval_position`，并确保后续 `GET /dcc/approval-positions` 只读取本地表，不再在运行时调用 IntAuth。

## Scope

- 先检查同仓库上一条后端任务状态；若未完成，则显式阻塞后再启动本任务。
- 在生产代码修改前创建本任务文档、执行日志和后端证据文件。
- 把现有“读岗位列表时实时同步 IntAuth”的行为改成显式一次性导入行为。
- 新增一个明确的后端导入入口，用于从 IntAuth 读取岗位并同步进本地表。
- 保持岗位分配、已复用同名本地岗位、已映射 source 以及缺失远端岗位禁用逻辑的现有本地语义。
- 缺少 `yudao.dcc.int-auth` 配置、IntAuth 请求失败或 payload 非法时必须 fail fast。
- 运行定向测试并记录 BDD / RED / GREEN 证据。

## Previous Task Check

- Previous backend task: `doc/tasks/20260515-dcc-runtime-seed-garbled-names/task.md`
- Status before this task: completed
- Impact: the runtime seed garbled-name repair is closed and does not block this IntAuth-decoupling change for approval positions.

## Milestones

- [x] M1: Check the previous backend task state.
- [x] M2: Create this task document and execution log before production code changes.
- [ ] M3: Record BDD scenarios and RED tests for local-only list behavior plus explicit import behavior.
- [ ] M4: Implement the explicit import endpoint and remove runtime IntAuth calls from the normal list path.
- [ ] M5: Run targeted backend verification and collect evidence.
- [ ] M6: Update final task status and prepare a scoped backend commit.

## Expected Verification

- `GET /dcc/approval-positions` reads existing local imported rows without calling IntAuth at runtime.
- `POST /dcc/approval-positions/import-intauth` imports or adopts IntAuth positions into local `dcc_approval_position`.
- Missing IntAuth config or invalid IntAuth payload still fails fast with the existing position-sync error codes.
- Targeted DCC position tests pass.

## Current Status

Completed. `DccApprovalPositionAdminServiceImpl#getPositionList()` now reads only local imported rows, while a new explicit `POST /dcc/approval-positions/import-intauth` path performs the one-time IntAuth import. Focused DCC position tests are green.

## Blocker And Impact

- Blocker: no blocker remains for the scoped approval-position decoupling change itself.
- Impact:
  - Normal DCC approval-position list reads no longer call IntAuth at runtime.
  - Administrators now have an explicit one-time IntAuth import path for approval positions.
  - A broader module test compile that includes unfinished directory-import test files is still blocked by unrelated same-repo work and was not mixed into this task.

## Final Verification Result

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc -Dtest=DccApprovalPositionAdminServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS
- Broader follow-up command:
  - `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc -Dtest=DccApprovalPositionAdminServiceImplTest,DccIntAuthPositionClientImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> BLOCKED by unrelated in-progress directory-import test files already dirty in the same backend repo (`DccDirectoryAdminServiceImplTest`, `DccIntAuthDirectoryClientImplTest`).
