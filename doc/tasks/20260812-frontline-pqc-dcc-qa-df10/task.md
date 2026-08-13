# 20260812-frontline-pqc-dcc-qa-df10

## Task Goal

完成 DF10 Backend process page projection：从 active-order 快照、锁定 QA 版本、QA 工序/项目、任务 overlay 和生产候选组装专用一线 PQC 工序响应，不改变生产模式路线工序响应模型。

## Milestones

- M0 规则和权威计划读取：完成。
- M1 BDD/TDD 基线：记录 BDD，新增 RED 测试并取得真实失败。
- M2 后端投影实现：补齐专用 response assembler / service projection。
- M3 验证和禁止项扫描：目标 Maven、git diff --check、backend-api evidence validator、禁止项扫描通过。
- M4 收尾状态：验证完成后标记 ready_for_closeout，不提交、不合并、不删除 worktree。

## Expected Verification

- mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
- git diff --check
- python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260812-frontline-pqc-dcc-qa-df10/backend-api-evidence.md
- 禁止项扫描：不得修改 controller/frontend/生产路线 response model/schema/upstream mapper/主管任务记录；不得出现 product/material/routeProcess 推算 QA、QA 与 MES 路线工序存在性校验、fallback/兼容/默认成功。

## Applicable Experience Gates

- PQC 待检准入与工序选择必须分离：active-order/processes 只能返回锁定 QA 产品/规程中存在检验项目的正式 QA 工序；任务只做 overlay，不扩展候选工序。
- MES PQC 项目级检验快照门禁：检验项目事实来自发布规程和结构化 itemResults；列表可原样保留历史空原文，但详情/提交边界再严格拦截。
- QA 多工序正式发布与退役夹具唯一键必须隔离：运行态只接受 MES_QA/PUBLISHED 正式规程，不用 CODX_QA 或测试 owner 补齐。
- PowerShell Maven -D 参数引号门禁：所有 Maven -D 参数整体加双引号。
- Maven 单模块陈旧依赖门禁：目标 JUnit 使用 -pl yudao-module-mes -am，避免本地 reactor 依赖陈旧误判。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，沿用 DF02/DF07/DF08/DF09 的正式 active-order、locked QA、item projection 和 task overlay 链路。
- 是否存在临时补丁或绕过：否。

## Current Status

ready_for_closeout：DF10 已按 round-3 独立评审修复锁定 QA 边界与编译回归。一线 activeOrder 投影通过 MesQaInspectionRegulationService#getLockedVersionForOrder 读取完整 PUBLISHED/RETIRED 聚合，不再私有查询 regulation/version/process/item mapper；专用 PQC 转换器已删除旧别名 setter。目标 Maven 18 tests、两个 evidence validator、git diff --check 与禁止项扫描均通过；等待主管独立复验，不提交、不合并、不删除 worktree。

## Verification Summary

- RED: mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL，缺少 activeOrderId 单参投影方法与 task option inspectionRuleKey/status。
- RED: independent-test-report.md -> FAIL，专用响应缺少 productionSubmitCandidates，测试未证明候选归属/排除和正式 ruleSort 顺序。
- RED: independent-test-report.md -> FAIL，专用响应缺少 inspectionTypeRules、taskSummary、PqcTaskOption ruleSort/inspectionTypeRule 和完整发布态检验项目字段。
- RED: mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesFrontlinePqcContextServiceTest#listProcessesByActiveOrderIdRejectsNullTaskRecordWithServiceException" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL，null task 记录在异常消息生成时触发 NullPointerException。
- GREEN: 目标 Maven 命令 -> PASS，MesFrontlinePqcContextServiceTest 5 tests / 0 failures / 0 errors；已覆盖候选归属、排除、倒序、一次批量读取、FIRST/PATROL_AM/PATROL_PM/FINAL 顺序、完整合同字段和 null task 快速失败。
- VALIDATOR: backend-api evidence validator -> PASS。
- STATIC: git diff --check 与生产新增行禁止项扫描 -> PASS。
- RED: round-3 target Maven -> FAIL，专用转换器残留旧别名 setter 且锁定聚合补丁未完整接通。
- GREEN: mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesFrontlinePqcContextServiceTest,MesQaInspectionRegulationServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS，18 tests / 0 failures / 0 errors / 0 skipped。
- GREEN: backend-api evidence validator 与 bug-regression evidence validator -> PASS；activeOrder 投影边界扫描确认只有 getLockedVersionForOrder，不存在 resolveLockedQaProcessSource。
