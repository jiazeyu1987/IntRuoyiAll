# Frontend Feature Evidence

## Feature Goal

把一线生产填写和 PQC 填写作为 eDHR 批记录页面级独立页签落地，复用真实前端数据流和现有一线简化填写组件。

## Non-goals

- 不新增后端接口、数据库迁移或菜单 SQL。
- 不引入静态 HTML/PNG 作为运行页面。
- 不改生产报工、记录本、资源池正式后端契约。

## UI Entry Points

- `/mes/pro/feedback/edhr-batch-production-fill`
- `/mes/pro/feedback/edhr-batch-pqc-fill`

## API Contracts

- 复用 `ProFeedbackApi.getFrontlineDeviceAccountProcesses`。
- 复用 `ProFeedbackApi.getFrontlineEmployeeCandidates`。
- 复用 `ProFeedbackApi.switchFrontlineActualEmployee`。
- 复用 `FrontlineTemplateApi.validatePayload`。

## Data States

- 工序/员工/上下文缺失：页面显示缺失原因并阻塞提交。
- 员工模板类型与当前页签不一致：页面显示模式不匹配并阻塞提交。
- 设备数量超过 3：生产填写只展示前三个设备卡片。
- 无设备：生产填写展示“本工序无设备，直接填数量”。

## Verification

- RED/GREEN evidence recorded in `execution-log.md`。
