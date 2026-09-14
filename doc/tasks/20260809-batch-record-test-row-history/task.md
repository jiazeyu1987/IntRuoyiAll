# 批记录测试逐行历史按钮

## 任务目标

在“批记录测试”四个内部 Tab 的每行操作中增加“历史”按钮。点击“测试”时只清空当前行旧回复并将当前行历史按钮置灰；收到该次执行的终态 Codex CLI 回复后置绿，点击历史按钮查看该行对应回复。所有回复必须按稳定测试项身份和执行 ID 隔离，禁止串行、过期轮询或其它行结果写错。

## 里程碑

- [x] P1：记录 BDD、根因和失败回归合同。
- [x] P2：实现逐行历史状态、灰绿按钮和结果查看。
- [x] P3：完成静态回归、类型检查和真实页面验证。
- [x] P4：完成证据归档与任务清理收尾。

## 预期验证

- 新静态合同先 RED，锁定四张列表均有历史按钮、当前行清空、稳定 key + executionId 双重校验、终态后变绿、点击后展示该行快照。
- 既有批记录测试静态合同、描述换行合同和订单分配合同通过。
- `pnpm ts:check` 通过。
- Playwright 通过真实菜单进入页面，验证历史按钮初始灰色；在满足测试租户写入门禁时执行真实测试，验证当前行灰 -> 绿、其它行不变、历史弹窗与当前行一致。

## 经验门禁摘要

- 异步返回必须冻结稳定业务身份和执行 ID；仅校验当前弹窗的全局 ID 不能形成逐行历史归属。
- 写入型 E2E 必须使用确认的测试租户和任务自有执行记录；前置不满足时不得在 admin 基线数据上点击测试。
- 不使用 localStorage 或默认成功作为历史回复来源，页面内历史只以正式执行结果为准。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；状态按稳定 `caseName` 建立，并用 `caseName + executionId + pollToken` 校验异步归属。
- 是否存在临时补丁或绕过：否。

## Current Status

completed：实现、静态回归、类型检查、真实两行 Playwright 隔离验证和任务清理均已通过；最终验证结果 PASS。

## Cleanup Candidates

- `output/playwright/batch-record-test-row-history/.playwright-cli/`

## Cleanup Keep

- `output/playwright/batch-record-test-row-history/browser-verification.json`
- `output/playwright/batch-record-test-row-history/history-dialog-row-a.png`
- `output/playwright/batch-record-test-row-history/history-buttons-two-rows-green.png`
