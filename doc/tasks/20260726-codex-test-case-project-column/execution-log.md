# Execution Log

## User Intent

- 用户要求在测试管理列表增加 `项目` 列，并把当前测试项分类到 `智能排产`、`文控`、`批记录` 三个项目。

## Milestone Log

- 2026-07-26：创建任务目录，开始梳理页面、API、后端持久化和现有测试项来源。
- 2026-07-26：确认 `system_codex_test_case` 缺少 `project` 字段；前端测试管理页和 API 类型也未暴露 `项目`。
- 2026-07-26：新增后端字段、分页过滤、保存校验、MySQL/H2 schema、种子项目值和 `20260726_system_codex_test_case_project.sql` backfill 迁移。
- 2026-07-26：前端测试管理列表新增 `项目` 列、快速过滤和表单必填选择；项目枚举为 `智能排产`、`文控`、`批记录`。
- 2026-07-26：按 project-experience-consolidation 规则沉淀 PowerShell Maven `-D` 参数加引号经验到 `docs/powershell-memory.md` 并更新 `docs/experience-index.md`。
- 2026-07-26：根据用户截图反馈，修复项目列直接渲染空 `row.project` 导致空标签的问题；列表显示改为 `resolveCaseProject(row)`，旧数据未回填时也按当前测试项内容解析为 `智能排产`、`批记录`、`文控` 三类之一。

## BDD / TDD

- BDD: 测试管理列表展示项目归属 -> Given 当前测试管理已有测试项, When 用户打开测试管理列表, Then 列表以标准列表模板展示 `项目` 列且每个当前测试项归属 `智能排产`、`文控` 或 `批记录`。
- RED: `pnpm e2e:system:codex-test-management:static` -> FAIL，页面源码不包含 `项目` 列/API 项目枚举。
- RED: `python -m pytest script\tests\test_codex_test_management_migration.py script\tests\test_codex_smart_scheduling_test_items_seed.py script\tests\test_dcc_codex_test_items_seed.py script\tests\test_codex_test_case_project_migration.py` -> FAIL，基础 schema、种子和项目 backfill 迁移缺少 `project`。
- GREEN: `pnpm e2e:system:codex-test-management:static` -> PASS。
- GREEN: `python -m pytest script\tests\test_codex_test_management_migration.py script\tests\test_codex_smart_scheduling_test_items_seed.py script\tests\test_dcc_codex_test_items_seed.py script\tests\test_codex_test_case_project_migration.py` -> PASS，11 passed。
- GREEN: `mvn -pl yudao-module-system -am "-Dtest=CodexTestCaseServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，4 tests passed。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `git diff --check -- <task-owned files>` -> PASS，仅 CRLF 提示。
- GREEN: `pnpm e2e:system:codex-test-management:static` -> PASS，覆盖项目列不得直接渲染空 `row.project`。
- GREEN: `pnpm ts:check` -> PASS，覆盖项目列解析函数类型正确。

## Verification Evidence

- 项目分类规则：`文控` 命中名称/路径中的 `文控`、`/dcc/`、`controlled-file`、`dcc-`；`批记录` 命中 `批记录`、`记录本`、`edhr`、`batch-record`、`recordbook`；`智能排产` 命中 `排产`、`smart-scheduling`、`scheduler`。
- Backfill fail-fast：迁移后任何非删除测试项 `project` 为空或不在三值枚举内，会 `SIGNAL SQLSTATE '45000'` 并中止。
- 现有智能排产种子写入 `project='智能排产'`；现有 DCC/文控种子写入 `project='文控'`；已有批记录/eDHR/记录本项由 backfill 规则归类为 `批记录`。
- 前端显示顺序与用户反馈保持一致：`智能排产`、`批记录`、`文控`。

## Blockers

- 当前工作区在任务开始前已有非本任务脏改动且分支领先 origin 3 个提交；为避免混入并行任务改动，本次未提交/推送，状态保留为 `ready_for_closeout`。
- 首次 Maven 命令 `mvn -pl yudao-module-system -am -Dtest=CodexTestCaseServiceImplTest test` 失败在上游模块无目标测试类；按项目门禁加入 `surefire.failIfNoSpecifiedTests=false`。随后 PowerShell 将未加引号的 `-Dsurefire.failIfNoSpecifiedTests=false` 拆成非法 lifecycle phase；整体加引号后 PASS。
