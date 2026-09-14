# 当前工序缺少待执行 PQC 检验任务修复

## Task Goal

修复 `activeOrderId=45` 的当前工序缺少待执行 PQC 检验任务问题，确保待检工单列表能基于正式活跃订单与当前工序生成或读取 `PENDING` 状态 PQC 任务，不用空值、默认工序或前端提示掩盖后端任务链路缺失。

## Milestones

- [x] 复现并定位 `activeOrderId=45` 下 `routeProcessId=null`、`processId=null` 的来源。
- [x] 增加 RED 回归测试，证明缺少待执行 PQC 任务或任务过滤错误。
- [x] 实施最小根因修复，保持正式路线、PQC 规程和任务状态链路可追溯。
- [x] 运行 GREEN 与相邻回归验证，记录证据。
- [x] 完成收尾检查并更新验证报告。

## Expected Verification

- 后端目标测试覆盖当前活跃订单存在可执行 PQC 工序时返回/生成 `PENDING` 检验任务。
- 相邻 PQC 待检列表、活跃订单切换、零排产活跃订单任务生成测试通过。
- 如需要真实运行态，仅在确认本机服务、账号、租户和测试数据前置后执行真实页面或 API 只读复核。

## Applicable Experience Gates

### PQC 待检工单任务链路

- Trigger: PQC 待检工单、无待执行 PQC 检验任务、`active-order/list`、`activeOrderId` 有值但 `routeProcessId/processId` 为空、`PENDING` 任务过滤。
- Preflight check: 先核对活跃订单、路线工序、PQC 正式规程、任务生成和待检列表过滤是否使用同一正式来源。
- Blocker: 正式 PQC 规程缺失、当前活跃订单无法解析路线工序、任务状态只有 `SUBMITTED` 或任务生成没有正式工序来源时必须停止。
- Verification: 后端回归覆盖待检列表返回 `PENDING` 任务，且不把 `SUBMITTED` 任务放入待检列表；必要时真实页面只读核验待检列表。
- Forbidden action: 禁止用默认工序、空值成功、前端文案、旧选中工单缓存或 API-only 伪成功掩盖任务链路缺失。
- Evidence: `docs/backend-development.md#mes-pqc-项目级检验快照门禁`，`docs/frontend-development.md#前端选择弹框即时反馈门禁`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，目标是补齐正式 PQC 任务链路或修正过滤/解析根因。
- `是否存在临时补丁或绕过`：否。

## Current Status

completed

用户反馈仍复现后已完成二次排查与本地运行态切换：48081 旧 Jar 未包含本次 PQC 修复，已用包含新版 `yudao-module-mes` 的新 Jar 重启本地后端并通过只读接口复核；二次 cleanup preview/apply 已完成。
