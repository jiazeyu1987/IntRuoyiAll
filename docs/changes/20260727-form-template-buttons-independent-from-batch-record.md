# 表单模板三按钮与批记录数据解耦

## Request Summary And Source

- 请求来源：用户在当前任务中明确纠正此前需求理解。
- 请求内容：表单模板与批记录表单没有直接关系；红框内 `打开 / 编辑 / 填写` 应执行当前表单模板自身操作，不得要求绑定批记录表单。
- 当前故障：三个按钮均提示“当前模板未绑定批记录表单，无法执行该操作”，导致普通表单模板无法使用。

## Current Baseline Reviewed

- `IntRuoyiFronted/src/views/form-center/template/index.vue` 当前通过 `batchRecordBindingStatus + batchRecordReportId` 校验三个按钮，并跳转批记录设计器或批记录模拟填写页。
- 页面原有 `TemplateViewDialog`、模板规则编辑工作区和模板内模拟填写弹窗仍完整存在，可直接承载当前表单模板操作。
- `FormTemplateListItemVO`、BPM 模板响应/数据对象、运行态映射和增量 SQL 曾因此前误解新增七个批记录绑定字段。
- 当前仓库未发现该增量 SQL 已进入正式发布记录；本地数据库已存在这些列，但当前数据均无需依赖这些列完成表单模板操作。

## Classification

- 缺陷：无批记录绑定的普通表单模板被错误阻断。
- 需求澄清：所谓“按批记录表单执行”指操作体验与职责对齐，不是建立表单模板到批记录报表的数据关系。
- 架构纠偏：FormCenter 不应为了自身预览、编辑、模拟填写依赖 MES 批记录 `reportId`。

## Impact

- Product: `打开` 查看当前模板，`编辑`进入当前模板规则编辑工作区，`填写`打开当前模板模拟填写；不再出现批记录绑定错误。
- Design: 保留现有页面布局和按钮位置，只恢复 FormCenter 自身组件与状态，不做视觉重构。
- Data: 不新增绑定数据；本次不执行破坏性 DROP COLUMN。已存在的本地冗余列保持惰性，后续如需物理删除必须单独审计迁移历史。
- API: 前端和 BPM 模板池契约移除七个批记录绑定字段；运行态不再映射这些字段。
- Tests: 将旧“必须 BOUND + reportId”合同改为“必须使用当前模板自身打开/编辑/填写且禁止批记录跳转”，并保留 FormCenter 全量静态合同。
- Release: 删除尚未发现发布引用的错误增量 SQL及其专用契约；不执行远端发布或远端数据库操作。
- Operations: 不停止共享本地服务，不修改端口、账号或运行环境。

## Decision

- 决策：接受。
- 立即范围：修正三个按钮、前端类型、BPM 模板池字段、运行态映射、错误迁移及相关合同/任务文档。
- 安全边界：不对已存在数据库列执行破坏性删除；代码完成解耦后，多余列不会影响运行。
- 禁止 fallback：不保留“有绑定走批记录、无绑定走表单模板”的双路径，三个按钮始终按当前表单模板执行。

## Required Approvals

- 用户已明确确认表单模板与批记录表单没有直接关系，本次纠偏无需额外产品批准。
- 远端发布、目标环境迁移和物理删列不在本次授权范围。

## Downstream Skill Reruns

- `frontend-feature-delivery`：修正三个按钮及前端静态合同。
- `backend-api-delivery`：移除 BPM 模板池错误绑定字段和运行态映射。
- `database-schema-delivery`：删除未发布引用的错误新增迁移及其测试，不执行 DROP COLUMN。
- `project-experience-consolidation`：纠正现有前端经验门禁，防止再次把交互对齐误解为跨域数据绑定。
- `task-closeout-cleanup`：完成验证后的任务产物清理。

## Blockers And Next Action

- 当前无实现 blocker。
- 下一步：先写入新的 BDD 和失败合同，再实施最小解耦修改并完成静态、类型、后端合同和真实页面验证。
