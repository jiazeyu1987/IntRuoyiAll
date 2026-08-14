# PQC 组长列表一线填写表单口径对齐

## Task Goal

- 将班组长/PQC 管理提交列表从泛化结构化字段，进一步调整为能反映一线 PQC 填写卡片的关键表单事实。
- 列表必须展示检验项、检验阶段、检验设备、设备编号、接收标准、检验方法、判定、检验数量、损耗数量、不良说明、逐件/样本明细。
- PQC 超限参数继续允许提交，但在列表中以红色提示异常。

## Milestones

- [x] 建立任务文档、BDD 场景和前端证据。
- [x] 编写专用静态合同，使当前列表缺少一线表单字段时 RED。
- [x] 修改班组长提交列表 PQC 字段解析与展示，按一线填写表单快照显示。
- [x] 运行定向静态合同、相邻合同、类型检查和 diff hygiene。
- [x] 完成验证报告、cleanup 与 Git 收尾判断。

## Expected Verification

- `node tests\e2e\pqc-leader-list-fill-form-parity-static.spec.js`
- `node tests\e2e\pqc-submission-structured-columns-static.spec.js`
- `node tests\e2e\pqc-leader-item-snapshot-static.spec.js`
- `node tests\e2e\pqc-leader-standard-list-template-static.spec.js`
- `pnpm ts:check`
- `git diff --check -- IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue IntRuoyiFronted/tests/e2e/pqc-leader-list-fill-form-parity-static.spec.js doc/tasks/20260806-pqc-leader-list-fill-form-parity`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260806-pqc-leader-list-fill-form-parity/frontend-feature-evidence.md`

## BDD Scenarios

- BDD: PQC 列表反映一线填写表单 -> Given 一线 PQC 填写清洗-外观-抽检表单 / When PQC 组长查看提交列表 / Then 列表展示检验项、阶段、设备、设备编号、接收标准、检验方法、判定和数量，而不是只显示参数汇总。
- BDD: PQC 数量字段对齐填写卡片 -> Given 一线表单填写检验数量、损耗数量和不良说明 / When 组长查看列表 / Then 列表能直接看到检验数量、损耗数量和不良说明。
- BDD: 逐件与超限提示保留 -> Given 一线表单存在逐件选择或样本值，且样本值超出冻结上下限 / When 列表渲染样本明细 / Then 样本/逐件明细可见，超限数值标红但不阻止提交。

## Applicable Gates

- 前端功能交付：BDD -> RED -> GREEN -> REGRESSION，保持现有 API 和列表模板。
- MES PQC 项目级检验快照门禁：PQC 列表必须从 `pqcItemDetails/itemResults` 和提交 rawPayload 的冻结快照解析，不回退到固定字段猜测。
- 前端静态契约隔离门禁：用任务专用静态合同覆盖本次截图口径，避免既有大合同无关失败阻塞。
- UTF-8/PowerShell 门禁：中文文档和测试按 UTF-8 处理，PowerShell 不使用 `&&`。

## Design Constraint Check

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，按一线 PQC 正式提交快照补齐组长列表展示字段。
- `是否存在临时补丁或绕过`：否。

## Current Status

blocked

- 一线 PQC 表单口径列表列、专用静态合同、定向验证、evidence validator 和 cleanup 已完成。
- 最新复验补齐了 PQC 重置空条件逻辑，相邻标准列表合同、PQC 合同、类型检查和 diff hygiene 均通过。
- Git 收尾阻塞：当前主工作区存在大量并行脏改动，且 `TeamLeaderWorkbenchPage.vue` 混有其它任务改动；不能安全宽泛暂存、提交或推送。
