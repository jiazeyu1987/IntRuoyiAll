# DCC Major-Version Notification Gate

## Goal
修复关联文件后续通知链路：只有受控文件正式生效时发生修订版（大版本）变化，才创建关联影响评估和通知；同一修订版内的小版本迭代不通知、不改变既有关联。保留接收人决定是否关联新版的既有流程。

## Current Status
ready_for_closeout

## Milestones
1. 记录当前源码入口、版本规则和现有测试缺口。
2. 先补充小版本不触发、大版本触发及无前一正式版本边界测试，形成 RED。
3. 实现后端大版本判定与通知跟进门禁，更新必要的回归测试和静态合同。
4. 运行 DCC 定向回归、静态检查并整理验证报告。
5. 按收尾规则提交、推送并完成任务记录。

## Expected Verification
- `DccControlledFileVersionPolicy` 统一解析大版本身份和小版本迭代。
- 默认 `majorIdentitySegmentCount=1` 时，`A/1 -> A/2` 不创建 follow-up、影响评估或通知。
- 默认 `majorIdentitySegmentCount=1` 时，`A/2 -> B/1` 创建 follow-up，并保留接收人决定是否关联新版的流程。
- 配置 `majorIdentitySegmentCount=2` 时，`A/1/1 -> A/1/2` 不创建 follow-up、影响评估或通知。
- 配置 `majorIdentitySegmentCount=2` 时，`A/1/2 -> B/1/1` 或 `A/1/2 -> A/2/1` 视为大版本身份变化。
- 首次正式生效没有前一正式版本时不伪造“大版本变更通知”。
- 重复调用和既有幂等身份校验仍然有效。
- 运行 DCC 相关 Maven 测试、必要的 JavaScript 静态合同和 `git diff --check`。

## BDD
- BDD: 小版本不通知 -> Given 当前正式版本是 `A/1`，新版本 `A/2` 正式生效；When 发布后处理关联跟进；Then 不创建 follow-up 批次、不生成影响评估任务、不生成通知投递，既有关联保持原版本。
- BDD: 大版本通知 -> Given 当前正式版本是 `A/2`，新版本 `B/1` 正式生效；When 发布后处理关联跟进；Then 创建一次 follow-up 批次，按当前有效权限生成候选人，由接收人决定是否关联新版。
- BDD: 首次生效不通知 -> Given 没有前一正式版本，新文件 `A/1` 首次生效；When 发布后处理关联跟进；Then 不把首次生效伪造成版本升版通知。
- BDD: 幂等重放 -> Given 同一大版本事件重复调用；When 后续跟进重放；Then 既有批次身份保持一致，不重复快照、任务或通知。

## 设计约束检查
- 版本大/小判断必须走服务端可配置版本策略，不信任前端标志或仅凭 `changeType` 猜测。
- 小版本路径必须 fail closed，不创建任何跟进副作用。
- 不自动切换已有关系；只有既有影响评估处理人通过正式入口选择并关联新大版本。
- 不修改数据库数据；如不需要新增持久化字段则不增加 schema 迁移。
- 不执行真实页面 E2E、服务重启、部署或远程操作。

## Cleanup Keep
- doc/tasks/20260916-dcc-major-version-notification-gate/task.md
- doc/tasks/20260916-dcc-major-version-notification-gate/execution-log.md
- doc/tasks/20260916-dcc-major-version-notification-gate/verification-report.md
- IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileVersionPolicy.java
- IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileVersionPolicyProperties.java
- IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileVersionPolicyTest.java
- IntRuoyiBackend/yudao-module-dcc/src/test/js/dcc-static-028-major-version-notification-gate-contract.spec.cjs


## Scope Extension 2026-09-16
- BDD: 固定两段规则 -> Given 版本策略按第一段为大版本身份；When `A/1` 发布后再发布 `A/2`；Then 视为小版本，不创建关联新版通知。
- BDD: 固定两段大版本 -> Given 版本策略按第一段为大版本身份；When `A/1` 发布后发布 `B/1`；Then 视为大版本，创建关联新版通知链路。
- BDD: 三段可配置规则 -> Given 版本策略按前两段为大版本身份；When `A/1/1` 发布后再发布 `A/1/2`；Then 视为小版本，不创建关联新版通知。
- BDD: 三段大版本身份变化 -> Given 版本策略按前两段为大版本身份；When `A/1/2` 发布后发布 `B/1/1` 或 `A/2/1`；Then 视为大版本，创建关联新版通知链路。
- Expected additional verification: static contract and unit tests must prove notification gating calls the configurable policy rather than hardcoded `revisionCode` comparison.

