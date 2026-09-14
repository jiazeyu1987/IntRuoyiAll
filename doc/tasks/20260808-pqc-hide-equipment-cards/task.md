# 20260808-pqc-hide-equipment-cards

## Task Goal

一线 PQC 检验方法详情区按正式设备选项条件显示“检验设备”和“设备编号”两张信息卡：有设备的检验方法显示，无设备的检验方法隐藏且不显示“无需设备”占位。

## Milestones

- [x] M1 定位一线 PQC 检验方法详情区域和现有设备展示逻辑
- [x] M2 用静态合同复现设备方法仍显示设备卡的问题
- [x] M3 实现最小前端修复，不引入 fallback/降级
- [x] M4 运行定向验证并记录结果
- [x] M5 收尾前更新任务文档、验证报告和经验沉淀状态
- [x] M6 追加真实前端路径 E2E，覆盖有设备显示、无设备隐藏

## Expected Verification

- 定向静态合同先 RED 后 GREEN，覆盖有设备方法显示设备卡、无设备方法隐藏设备卡。
- 运行相关前端静态测试；全量 `pnpm ts:check` 若先失败于无关文件，记录阻塞归因，不用无关失败否定当前目标合同。
- 运行 `git diff --check` 确认无格式尾随问题。
- 真实 Playwright 打开本机一线 PQC 页面，使用正式 active-order/processes 返回的检验项目数据，分别断言有设备项目显示“检验设备/设备编号”卡片、无设备项目隐藏两张卡片且无“无需设备”占位；不执行 PQC 提交。

## Current Status

completed：真实 E2E、静态合同、diff 检查和 cleanup preview/apply 均已完成。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，按正式设备选项作为唯一展示条件；有设备时显示设备卡，无设备时隐藏设备卡。
- `是否存在临时补丁或绕过`：否。

## Applicable Gates

- Frontend: `docs/frontend-development.md#前端提交前严格验证与草稿态计算隔离门禁`，PQC 设备字段仍按正式提交链路处理；最终 UI 口径为有设备显示检验设备和设备编号卡片，无设备隐藏卡片。
- Experience index: `docs/experience-index.md` 命中 PQC、检验设备、设备编号、无设备检验项目、有设备检验项目、`data-pqc-equipment` 相关门禁。

## Cleanup Keep

- doc/tasks/20260808-pqc-hide-equipment-cards/pqc-equipment-cards-real.e2e.cjs
