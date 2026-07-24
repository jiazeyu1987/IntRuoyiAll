# 任务：运行控制台按钮弹框与候选展示

## 任务目标

实现运行控制台前端按钮级目标交互：每个执行按钮打开明确弹框；发布/上线/回滚使用发布包候选，备份/恢复使用备份包候选；回滚版本与恢复数据的展示字段必须明显区分。

## BDD 场景

- BDD: 发布类按钮弹框 -> Given 操作员点击构建、部署测试、标记测试通过或上线 / When 弹框打开 / Then 字段和候选来源符合发布包逻辑。
- BDD: 备份恢复按钮弹框 -> Given 操作员点击立即备份、回滚版本或恢复数据 / When 弹框打开 / Then 回滚显示发布包信息，恢复显示备份点信息。
- BDD: 事故闭环不执行运维动作 -> Given 操作员打开事故闭环 / When 关联告警、发布包、备份点和操作记录 / Then 只提交事故闭环记录，不触发发布、备份、回滚或恢复。

## 里程碑

- [x] M1：创建任务文档。
- [ ] M2：补充 RED 静态/E2E 测试。
- [ ] M3：实现弹框和候选展示。
- [ ] M4：运行前端验证。
- [ ] M5：记录最终证据并提交。

## 预期验证

- `node --check tests\e2e\runtime-control-release-package-static.spec.js`
- `node tests\e2e\runtime-control-release-package-static.spec.js`
- `node tests\e2e\runtime-control-dialog-contract-static.spec.js`

## Current Status

Blocked.

## Blocker

- BLOCKED: 2026-05-30 新任务 `20260530-dcc-delete-parent-folder` 已被用户要求立即执行；当前任务尚未完成 RED/GREEN 与实现，不能在同一轮混入 DCC 目录删除交付。
- Impact: 运行控制台按钮弹框与候选展示保持未完成，后续恢复该任务时需从 M2 RED 静态/E2E 测试继续。
