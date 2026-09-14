# 一线PQC最大化预加载切换缓存

## Task Goal

在一线PQC页面点击“最大化”后，预加载待检工单、各工单工序与PQC人员候选缓存，减少后续工单和工序切换时的等待。

## Milestones

- [x] 记录一线PQC最大化预加载的BDD与RED证据
- [x] 实现正式GET数据缓存，不预调用带上下文写语义的POST
- [x] 运行目标静态合同、相邻PQC合同与类型/差异检查
- [x] 记录验证报告并完成收尾状态

## Expected Verification

- `node tests/e2e/mes-frontline-pqc-fullscreen-preload-static.spec.js`
- `node tests/e2e/mes-frontline-pqc-active-order-switching-static.spec.js`
- `node tests/e2e/pqc-inspection-tabs-layout-static.spec.js`
- `node tests/e2e/mes-frontline-pqc-order-product-summary-static.spec.cjs`
- `pnpm ts:check`（如存在非本任务历史阻塞，记录首个阻塞）
- `git diff --check`

## Current Status

completed

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；缓存未命中仍走原正式GET链路，错误继续暴露到页面状态和消息。
- `是否从根因和长期维护角度解决`：是；把最大化作为显式预热触发点，缓存正式切换依赖数据，而不是隐藏loading。
- `是否存在临时补丁或绕过`：否。

## Applicable Gates

- 前端选择弹框即时反馈门禁：PQC待检工单、工序、人员候选必须来自正式接口，loading/empty/error 状态保持可见，不能用空数据或延迟遮罩掩盖慢请求。
- MES PQC 项目级检验快照门禁：PQC待检工单与工序必须使用正式 active-order 与 process 身份，不能用 formBindings、默认工序或前端猜测替代。
- Frontend Feature Evidence Contract：记录目标、非目标、入口、API契约、BDD、RED/GREEN、加载/空态/错误/权限边界。
