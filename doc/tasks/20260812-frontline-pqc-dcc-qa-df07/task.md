# 20260812-frontline-pqc-dcc-qa-df07

## Task Goal

实现“从活跃订单锁定的 QA 版本读取 QA 自有工序列表”后端行为。输入为订单锁定的 dccProjectCodeId、qaRegulationId、qaRegulationVersionId；只允许读取同租户、归属关系正确且状态为 PUBLISHED 或 RETIRED 的锁定 QA 版本；返回该 QA 版本自有工序，排序为 sort ASC, id ASC。

## Milestones

- [x] 创建任务文档并记录 BDD/TDD 计划
- [x] 写入最小 RED 测试，证明当前实现没有按订单锁定 QA 版本读取 QA 自有工序
- [x] 执行目标 Maven 测试并记录 RED 失败原因
- [x] 实现最小正式后端逻辑
- [x] 重跑目标 Maven 测试取得 GREEN
- [x] 执行静态检查、禁止项扫描和任务证据归档

## Expected Verification

- mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesQaInspectionRegulationServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
- git diff --check
- touched production files 禁止项扫描：不得命中 product/material/formBindings/selectEnabledList/fallback/兼容/兜底/默认成功
- touched production files 语义扫描：不得存在 MES route process 存在性校验，不得从产品、物料、路线名或当前 QA 版本推算
- python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260812-frontline-pqc-dcc-qa-df07/backend-api-evidence.md

## Current Status

ready_for_closeout

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，目标是读取订单锁定 QA 版本正式归属链路，并在缺失或不合法时 fail fast。
- 是否存在临时补丁或绕过：否。

## BDD Scenarios

- BDD: locked QA version process list success -> Given 订单锁定的 DCC 项目代码、QA 规程、QA 版本同租户且归属一致，QA 版本状态为 PUBLISHED 或 RETIRED，并存在 QA 自有工序；When 后端按锁定三元组读取工序列表；Then 返回该 QA 版本自有工序，并按 sort ASC, id ASC 排序。
- BDD: locked QA version rejects invalid ownership -> Given 订单锁定三元组中 DCC 项目代码、QA 规程或 QA 版本存在跨租户或归属不一致；When 后端读取锁定 QA 版本工序；Then 请求失败并暴露正式归属校验错误，不返回空列表或默认成功。
- BDD: locked QA version rejects unsupported status -> Given QA 版本归属正确但状态不是 PUBLISHED 或 RETIRED；When 后端读取锁定 QA 版本工序；Then 请求失败并暴露版本状态错误。

## Experience Gate

- 已按经验索引命中一线 PQC DCC-QA 目标态、PowerShell Maven -D 参数引号、隔离验证 worktree、worktree 端口段与原子槽位、脏工作区功能分支融合增量门禁。
- 本任务未启动服务，因此 RED 隔离 worktree 不登记端口；Maven 命令的 -D 参数已整体加引号；不引入产品、物料、路线工序或 QA 当前版本推算。

## Cleanup Keep

- doc/tasks/20260812-frontline-pqc-dcc-qa-df07/independent-test-report.md
- doc/tasks/20260812-frontline-pqc-dcc-qa-df07/backend-api-evidence.md
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/qa/regulation/MesQaInspectionRegulationService.java
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/qa/regulation/MesQaInspectionRegulationServiceImpl.java
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/qa/regulation/MesQaInspectionRegulationServiceTest.java
