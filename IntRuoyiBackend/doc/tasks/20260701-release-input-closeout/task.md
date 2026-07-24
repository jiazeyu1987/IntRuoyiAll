# 任务：发布输入收口（后端）

- Task ID: `20260701-release-input-closeout`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Created: `2026-07-01`
- Current Status: `completed`

## Task Goal

在“先提交再发布”的当前发布策略下，把本轮已经独立完成并验证通过的后端改动正式收口到主分支发布输入，确保后续测试服 `build-release -> publish-test` 只使用已提交、已记录 TDD 证据的后端版本。

## Previous Task Check

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260701-local-admin-api-48081-connection-refused\task.md`
- 状态：`completed`
- 处理说明：上一任务已完成本地 48081 拒连排查；本轮不新增业务修复，只收口当前已完成任务的后端发布输入。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 命中 `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - 任务文档、执行日志与命令输出统一显式 UTF-8；提交前先核对 staged 文件只包含本轮已验证改动。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。本轮不拼凑未经验证的后端改动，而是只收口已完成任务的正式修复与测试。
- `是否存在临时补丁或绕过`：否。仅通过已有任务证据和已跑通验证收口后端发布输入，不跳过 TDD 门禁。

## BDD 场景

- `BDD: 先提交再发布时后端发布输入必须来自已完成任务 -> Given 用户已选择先提交主工作区再发布 / When 后端仓准备提交当前改动 / Then 只允许纳入已经完成、具备 RED/GREEN 证据且验证通过的任务产物。`
- `BDD: 发布输入收口不新增未验证业务行为 -> Given 当前后端改动来自 showroom / SRM / 本地运行态排查任务 / When 重新执行本轮关键验证 / Then 构成发布输入的测试必须全部通过。`

## Milestones

1. M1：建立后端发布输入收口台账并确认纳入范围。`completed`
2. M2：复核 staged 改动对应任务与证据。`completed`
3. M3：重新执行当前关键验证并记录结果。`completed`
4. M4：作为后端发布输入完成提交收口。`completed`

## Expected Verification

- `python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_srm_d7_d10_sql_contract.py -k nas_locator -q`
- `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-srm -am "-Dtest=SrmNasLocatorWildcardTenantSqlRegressionTest,SrmNasLocatorServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-showroom "-Dtest=ShowroomProductExcelImportExportIntegrationTest" test`

## Final Verification Result

- `python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_srm_d7_d10_sql_contract.py -k nas_locator -q` -> `PASS`
- `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-srm -am "-Dtest=SrmNasLocatorWildcardTenantSqlRegressionTest,SrmNasLocatorServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> `PASS`
- `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-showroom "-Dtest=ShowroomProductExcelImportExportIntegrationTest" test` -> `PASS`
- 已纳入本轮后端发布输入的任务：
  - `20260701-showroom-product-excel-audio-keyword-roundtrip`
  - `20260701-srm-nas-locator-blacklist-button-missing`
  - `20260701-srm-nas-locator-blacklist-srm-admin-binding`
  - `20260701-srm-nas-locator-wildcard-search-error`
  - `20260701-local-admin-api-48081-connection-refused`

## Current Blockers

- 无。
