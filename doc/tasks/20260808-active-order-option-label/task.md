# 活跃订单候选展示订单号、产品、数量

## Task Goal

将当前活跃订单下拉从“生产订单 ID / 活跃池 ID”展示，调整为用户可识别的订单编号、产品、数量三项信息，匹配截图口径；后端列表接口补齐正式工单号、产品主数据和工单数量字段，且不改变活跃订单 ID、生产订单 ID 的提交身份字段。

## Milestones

- [x] 定位活跃订单下拉真实渲染区域与数据来源。
- [x] 用 BDD + RED 静态合同锁定展示要求。
- [x] 最小修改后端正式字段链路和前端展示逻辑，并保持提交 payload 不变。
- [x] 运行目标合同、相邻合同、类型/差异检查并记录结果。
- [ ] 后端定向 JUnit 在同模块 Maven 空闲后复跑。
- [ ] 完成收尾记录和最终状态。

## Expected Verification

- `node tests/e2e/team-leader-active-order-option-label-static.spec.js`
- `node tests/e2e/mes-process-pool-team-leader-static.spec.js`
- `node tests/e2e/production-leader-active-order-pool-tab-static.spec.js`
- `pnpm ts:check`
- `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderServiceTest,MesProcessPoolTeamLeaderControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `git diff --check`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260808-active-order-option-label/frontend-feature-evidence.md`

## Current Status

blocked：实现与前端验证已完成；后端定向 Maven 在 `yudao-module-mes` testCompile 阶段被同模块并行 Maven/陈旧 target 竞争阻塞，需等待同仓 Maven 空闲后复跑。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；前端不再用 `workOrderId` / `id` 作为可见文案兜底，后端正式字段缺失时按服务异常暴露。
- `是否从根因和长期维护角度解决`：是，直接补齐活跃订单列表正式读模型、响应 VO 和可见展示函数。
- `是否存在临时补丁或绕过`：否。

## Applicable Gates

- 用户可见描述与内部编码隔离：展示字段必须来自正式订单编号、产品和数量字段，不得以内部 ID 占位掩盖。
- 前端选择弹框即时反馈：仅调整候选展示，不改变选择动作、提交身份或加载错误暴露。
