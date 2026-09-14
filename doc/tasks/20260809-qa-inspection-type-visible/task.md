# QA 检验类型默认可见

## Task Goal

让 QA 规程配置的“检验项目”表格默认显示“适用检验类型”列，使用户无需打开“显示字段”即可直接确认某条工序检验项目是否适用于首检、巡检或末检。

## Milestones

- [x] M1：记录 BDD 场景并建立聚焦 RED 静态合同。
- [x] M2：将“适用检验类型”调整为默认可见，保持现有编辑和列设置能力。
- [x] M3：完成目标测试、相邻回归、类型检查和收尾验证。

## Expected Verification

- `node tests/e2e/qa-regulation-applicable-types-default-visible-static.spec.js`
- `node tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs`
- `pnpm ts:check`
- 具备本地真实页面前置时，通过 Playwright 打开 QA 规程检验项目页，确认“适用检验类型”表头和“首检”选项默认可见。

## Applicable Experience Gate

- 命中 `docs/frontend-development.md#前端列表跨账号默认列布局统一门禁`。
- 适用要求：修改既有默认列集合时必须同步升级稳定 table key，避免历史用户列配置继续覆盖新默认值；模板 `table-key`、Element Plus 表格 `data-user-table-key` 和 `useUserTableColumns` 必须使用同一个新键。
- 验证要求：聚焦静态合同同时锁定新键三处一致、旧键不再作为完整配置键使用、“适用检验类型”默认可见，并保留显示字段自动保存能力。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；直接调整统一列定义的默认可见性，不增加页面特例。
- `是否存在临时补丁或绕过`：否。

## Cleanup Candidates

- `doc/tasks/20260809-qa-inspection-type-visible/frontend-feature-evidence.md`
- `doc/tasks/20260809-qa-inspection-type-visible/qa-applicable-types-visible.e2e.cjs`
- `output/playwright/20260809-qa-inspection-type-visible/`

## Cleanup Keep

- `doc/tasks/20260809-qa-inspection-type-visible/task.md`
- `doc/tasks/20260809-qa-inspection-type-visible/execution-log.md`
- `doc/tasks/20260809-qa-inspection-type-visible/verification-report.md`

## Current Status

completed：实现与全部必需验证通过；清理 preview/apply 完成，仅删除本任务临时证据、一次性 Playwright 脚本、结果和截图，保留正式回归测试及三份核心任务文档。未执行 Git 提交或推送。
