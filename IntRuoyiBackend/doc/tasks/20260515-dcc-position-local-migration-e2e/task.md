# Task: DCC 岗位本地化迁移并完成真实 E2E 验证

## Goal

将 IntAuth 当前岗位主数据一次性迁移到本地 `dcc_approval_position`，删除 live 本地库中的旧 `E2E` 测试岗位脏数据，并验证 `GET /dcc/approval-positions` 与真实前端 `DCC岗位分配` 页面都能稳定展示这批本地岗位。

## Scope

- 先检查上一条后端任务文档状态。
- 在 live 数据变更前创建本任务文档、执行日志和证据文件。
- 只迁移岗位主数据，不迁移 IntAuth 岗位成员分配。
- 复用现有 `POST /dcc/approval-positions/import-intauth` 一次性导入能力，不重新设计旁路写入规则。
- 删除 live 本地库中的 `source='E2E'` 测试岗位与对应 `dcc_position_assignment` 数据。
- 对 live 本地 MySQL `127.0.0.1:23306/ruoyi-vue-pro` 做迁移前后名称集合对齐验证。
- 如 live `48081` 后端尚未加载“本地只读 + import-intauth”实现，则先切换到当前后端代码再执行迁移。

## Previous Task Check

- Previous backend task: `doc/tasks/20260515-ptca-erp-mes-search-diff-analysis/task.md`
- Status before this task: completed.
- Impact: no unfinished backend task blocks this DCC local-position migration work.

## Milestones

- [x] M1: Confirm previous backend task status and create this task directory before changes.
- [x] M2: Record BDD scenarios and RED evidence for the live local DCC position gap.
- [x] M3: Verify the live backend/runtime prerequisites and migrate IntAuth positions into local MySQL.
- [x] M4: Remove the live `E2E` test row and verify post-migration local name-set alignment.
- [x] M5: Update task evidence, validate backend evidence, and prepare a scoped backend commit.

## Expected Verification

- Read-only precheck confirms IntAuth currently has `31` positions and live local `dcc_approval_position` is not aligned.
- `POST /dcc/approval-positions/import-intauth` completes against the live backend and writes IntAuth positions into local MySQL.
- Post-migration local active `INTAUTH:%` positions count equals `31`.
- Post-migration name-set diff is empty and no `source='E2E'` row remains in `dcc_approval_position`.
- Backend evidence validation passes.

## Current Status

Completed. Live local MySQL now contains 31 active `INTAUTH:*` DCC positions aligned with IntAuth current names, the old `E2E` seed row has been removed, and the live DCC runtime returns the migrated local rows successfully.

## Blocker And Impact

- Blocker: none.
- Impact:
  - `DCC岗位分配` runtime now reads the intended local IntAuth-derived岗位集合 from MySQL.
  - The prior test-only `E2E` row no longer pollutes the live verification dataset.

## Final Verification Result

- `python D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260515-dcc-position-local-migration-e2e\scripts\inspect_position_alignment.py` before migration -> FAIL, `localIntAuthActiveCount=0`, `missingInLocal` contained all 31 names, and one `E2E` row existed.
- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-server -am -DskipTests package` -> PASS.
- `$env:INTERNAL_SERVICE_TOKEN='intkb-local-internal-token'; cmd /c D:\ProjectPackage\Int\IntRuoyi\restart-ruoyi.bat` -> PASS.
- `python D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260515-dcc-position-local-migration-e2e\scripts\migrate_positions_via_live_api.py` -> PASS, `createdCount=31`, removed `E2E` row id `900301`.
- `python D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260515-dcc-position-local-migration-e2e\scripts\inspect_position_alignment.py` after migration -> PASS, `localIntAuthActiveCount=31`, `missingInLocal=[]`, `extraInLocal=[]`, `e2eRows=[]`.
