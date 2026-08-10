# 一线PQC工序卡片点选即关闭

## Task Goal

- 一线PQC选择工序时，点击工序卡片即代表确认选择；选择完成后弹框直接关闭，不再要求用户点击返回。

## Milestones

- [x] 创建任务记录并写入适用经验门禁。
- [x] 定位一线PQC工序选择弹框、选择处理器和现有静态合同。
- [x] 先补充 RED 静态合同，锁定 PQC 工序卡片点击后关闭顺序。
- [x] 实现最小正式交互改动，不引入 fallback、不吞异常。
- [x] 运行目标合同、相邻回归和必要类型检查，记录验证结果。
- [x] 收尾前更新验证报告与最终状态。

## Expected Verification

- `node tests/e2e/frontline-pqc-process-picker-autoclose-static.spec.cjs`：覆盖一线PQC工序选择点选即关闭。
- 相邻一线选择器静态合同：覆盖原有生产/PQC切换行为不回归。
- `pnpm ts:check`：若现有无关类型问题阻塞，记录首个无关 blocker，不用其冒充通过。
- `git diff --check`：确认补丁无空白格式问题。

## Applicable Experience Gate

- Trigger: 修改一线PQC工序 picker 的选中流程，命中 `docs/frontend-development.md#前端选择弹框即时反馈门禁`。
- Preflight check: 区分“打开候选”和“确认选择”；若产品口径为点选即关闭，关闭弹框必须发生在耗时异步请求、运行配置加载、员工/上下文切换之前；保留正式错误暴露。
- Blocker: 点击后卡片 active 但弹框仍等待接口或上下文切换；静态合同只断言最终关闭不检查关闭顺序。
- Verification: 静态合同锁定 `closePicker()` 或隐藏面板位于目标 `await` 之前，并复跑相邻 picker/页签合同。
- Forbidden action: 禁止用固定延迟、loading 遮罩、吞异步错误、mock/default 候选、或统一提前关闭校验型选择来掩盖正式流程。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，目标是调整正式选择流程顺序，让卡片点击承担确认职责。
- `是否存在临时补丁或绕过`：否。

## Current Status

completed

- 已实现一线PQC工序卡片点选即关闭，目标合同、相邻 picker 合同、PQC 标准/方法弹框合同、`pnpm ts:check` 和 `git diff --check` 通过。
- 已记录一个非本任务相邻宽合同失败：`edhr-frontline-fill-tabs-static.spec.cjs` 仍失败在既有生产设备卡片三台限制断言，失败点不属于本次工序选择关闭顺序改动。
- task-closeout-cleanup preview/apply 通过，已删除临时 `frontend-feature-evidence.md`，保留 `task.md`、`execution-log.md`、`verification-report.md`。
