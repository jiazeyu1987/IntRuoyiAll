# QA 规程总览备注展示

## Task Goal

在 QA 规程页面的“总览”页签中，将用户截图里的“备注”及四条完整要求显示在适用范围卡片下方，并保持四条固定检验规则与当前产品模板、检验项目数据相互独立。

## Scope

- 在 `QaRegulationPage.vue` 的总览区域增加语义化备注块。
- 保留现有 DCC 项目选择、产品级草稿、工艺路线范围和末检开关逻辑。
- 不修改后端接口、数据库字段或 QA 检验项目来源。

## Note Content

1. 设备初次开机、模具更换、参数调整、模具维修等需要按照抽样规则进行首件检验；
2. 首检如果发现不合格，及时向部门主管/领导汇报，待问题得到纠正后，生产稳定之后，重新进行首检，检验全部合格后，才可转入正常生产；
3. 如果样本量等于或超过批量，则进行100%检验；
4. 过程巡检应每班记录两次，上午和下午各一次，巡检过程中若发现产品不合格，应及时向部门主管反映不合格问题，并对之前生产的产品进行隔离，问题纠正之后，进行双倍检验，确认无异常之后，转入正常抽样。然后对之前生产的产品组织评审，根据评审结果对该批次产品进行处理。

## Milestones

- [x] M1：建立任务记录，确认页面入口、产品模板边界和备注来源。
- [ ] M2：新增备注专用静态合同并确认当前页面 RED。
- [ ] M3：在总览适用范围下方实现备注展示及移动端样式。
- [ ] M4：运行静态合同、相邻 QA 合同、类型检查和真实只读 Playwright 页面验证。
- [ ] M5：更新证据、执行收尾清理、提交并推送 `int_main`。

## Expected Verification

- `node tests/e2e/qa-regulation-overview-note-static.spec.cjs`
- `node tests/e2e/qa-regulation-display-fields-titlebar-static.spec.js`
- `node tests/e2e/qa-regulation-final-applicability-static.spec.cjs`
- `node tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs`
- `pnpm ts:check`
- `git diff --check -- IntRuoyiFronted/src/views/mes/pro/processpool/QaRegulationPage.vue IntRuoyiFronted/tests/e2e/qa-regulation-overview-note-static.spec.cjs doc/tasks/20260807-qa-regulation-overview-note`
- 真实 Playwright：`http://127.0.0.1:8081/mes/pro/process-pool/qa-regulation`，只读选择 QA 产品，验证备注位于“总览”适用范围下方且四条文本完整可见。

## Applicable Gates

- `docs/frontend-development.md#前端静态契约隔离门禁`：使用本任务专用最小静态合同，锚定总览区块边界，先 RED 后 GREEN。
- `docs/frontend-development.md#验证方式`：运行目标静态合同、相邻 QA 合同和 `pnpm ts:check`。
- `docs/e2e-rules.md#静态合同与真实-e2e-同步门禁`：区分静态合同与真实页面验证，不以 API 或旧截图替代真实用户路径。
- `docs/task-closeout-rules.md#收尾规则`：先 `ready_for_closeout`，再执行 cleanup preview/apply，最后标记 `completed`。

## Design Constraint Check

- 是否引入 fallback/降级/吞异常：否；备注是固定正式展示内容，不增加异常分支。
- 是否从根因和长期维护角度解决：是；备注作为总览语义区块维护，不混入产品项目数组或临时 DOM 文本。
- 是否存在临时补丁或绕过：否。

## Cleanup Candidates

- `doc/tasks/20260807-qa-regulation-overview-note/qa-regulation-overview-note-real.e2e.cjs`
- `output/playwright/20260807-qa-regulation-overview-note/`

## Current Status

in_progress

已完成页面和产品模板边界核对，待新增静态合同并实现备注展示。

