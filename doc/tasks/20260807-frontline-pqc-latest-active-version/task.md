# 一线 PQC 按最新 ACTIVE 路线刷新待执行任务

## Task Goal

修复一线 PQC 打开活跃订单时混用旧 `activeOrder.routeVersionId` 与当前路线工序的问题。待执行 PQC 必须定位当前唯一 ACTIVE 路线版本及其已发布 QA 规程，旧 PENDING 任务不得复用；已经提交的任务必须保留生成时冻结的路线和规程版本。

## Milestones

- [ ] M1：完成现状、数据模型、经验门禁和既有测试核对。
- [ ] M2：记录 BDD，并以失败回归测试证明旧版本与当前工序混用。
- [ ] M3：实现最新 ACTIVE 路线解析、旧 PENDING 任务刷新及已提交任务冻结。
- [ ] M4：完成定向测试、相邻回归、证据验证和运行态核对。
- [ ] M5：完成经验沉淀、清理、提交与推送。

## Expected Verification

- `MesFrontlinePqcContextServiceTest` 聚焦 RED/GREEN 回归。
- 涉及任务刷新服务的目标 JUnit 测试。
- `mvn -pl yudao-module-mes -am` 定向测试及相关相邻回归。
- `bug-regression-fix-loop` 与 `backend-api-delivery` evidence validator。
- 如本机真实前端、后端、登录和规程数据前置齐全，使用 Playwright 复核一线 PQC 真实入口；缺任一正式前置时按规则阻塞，不使用 API-only 替代。

## Experience Gate

- 待读取 `docs/experience-index.md` 后补充匹配门禁摘要。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；统一待执行任务的路线、工序、规程和任务身份，并保留已提交历史快照。
- `是否存在临时补丁或绕过`：否。

## Current Status

in_progress - 已确认用户批准正式方案，正在完成任务前置、脏工作区基线和 BDD/TDD 准备。

