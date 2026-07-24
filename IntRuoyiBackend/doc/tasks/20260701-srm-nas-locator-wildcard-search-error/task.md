# 任务：SRM NAS定位 通配搜索系统异常（后端）

- Task ID: `20260701-srm-nas-locator-wildcard-search-error`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Created: `2026-07-01`
- Current Status: `completed`

## Task Goal

修复 `SrmNasLocatorEntryMapper` / `SrmNasLocatorService` 在真实 MySQL 环境下处理 `*MO13*.pdf` 通配搜索时的 SQL 合同问题，确保 `/srm/nas-locator/page` 查询稳定返回结果而不抛系统异常。

## Previous Task Check

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260701-srm-nas-locator-blacklist-pattern-search\task.md`
- 状态：`completed`
- 处理说明：上一后端任务已完成，不阻塞本轮回归修复。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 命中 `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - Java、XML、测试、文档和证据文件统一按 UTF-8 处理。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。修正式 SQL 转义与查询合同，不靠捕获 SQL 异常后兜底为普通搜索。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: *MO13*.pdf 在真实查询合同下返回 PDF 命中 -> Given 最新成功快照中存在 MO13 PDF 与非 PDF 文件 / When 服务按 *MO13*.pdf 查询分页 / Then 仅返回 PDF 命中文件。`
- `BDD: SQL LIKE ESCAPE 合同兼容真实 MySQL -> Given 通配关键字里包含被转义的 % 或 _ / When Mapper 执行通配分页查询 / Then 语句使用真实 MySQL 可接受的 ESCAPE 写法且按字面量匹配。`
- `BDD: 普通关键字查询排序保持原样 -> Given 查询关键字不含 * / When 服务分页查询 / Then 仍走原有关键字优先级排序。`

## Milestones

1. M1：建立后端任务台账并确认 mapper / service / test 入口。`completed`
2. M2：补 RED 测试，证明当前通配 SQL 合同存在问题。`completed`
3. M3：实现最小修复并跑 GREEN。`completed`
4. M4：补证据与收尾。`completed`

## Expected Verification

- `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-srm -am "-Dtest=SrmNasLocatorServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260701-srm-nas-locator-wildcard-search-error\bug-regression-evidence.md`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260701-srm-nas-locator-wildcard-search-error\backend-api-evidence.md`

## Current Blockers

- 暂无。

## Cleanup Candidates

- `doc/tasks/20260701-srm-nas-locator-wildcard-search-error/bug-regression-evidence.md`
- `doc/tasks/20260701-srm-nas-locator-wildcard-search-error/backend-api-evidence.md`

## Final Verification Result

- `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-srm -am "-Dtest=SrmNasLocatorWildcardTenantSqlRegressionTest,SrmNasLocatorServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> `PASS`
- `powershell.exe -NoProfile -ExecutionPolicy Bypass -File D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\deploy\restart-int-ruoyi-local.ps1 -Component backend -WorktreeName int_main` -> `PASS`
- 真实运行态日志对照：
  - 修复前：`backend-runtime-control-20260701-104031.out.log` 出现 `BadSqlGrammarException`
  - 修复后：`backend-runtime-control-20260701-134637.jar` 上真实页面复验返回 `code=0`

## Current Status

completed
