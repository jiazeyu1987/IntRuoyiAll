# PQC 当前检验标题显示检验方法

## Task Goal

修复一线 PQC 填写页当前检验卡片标题：AO5 终检等检验项名称不应作为主标题展示，主标题应展示正式检验方法“目视检验”，同时保留检验项编码/名称用于项目切换、提交身份和明细载荷。

## Milestones

- [x] 记录用户可见行为和回归场景。
- [x] 增加静态回归合同，先证明当前标题仍绑定检验项名称。
- [x] 最小修改 PQC 当前卡片标题展示逻辑，不改变提交身份字段。
- [x] 运行目标静态合同和相邻 PQC 合同验证。
- [x] 更新任务证据、验证报告和最终状态。

## Expected Verification

- `node tests/e2e/pqc-active-title-method-display-static.spec.cjs`
- 相邻 PQC 静态合同：`node tests/e2e/role-matrix-qa-regulation-static.spec.cjs`
- `git diff --check -- IntRuoyiFronted/src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue IntRuoyiFronted/tests/e2e/pqc-active-title-method-display-static.spec.cjs doc/tasks/20260807-pqc-active-title-method-display`

## Current Status

completed

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；仅调整用户可见标题来源，不新增降级分支或吞异常。
- `是否从根因和长期维护角度解决`：是；标题显示绑定正式检验方法，检验项名称继续作为 key/提交身份。
- `是否存在临时补丁或绕过`：否。

## Applicable Experience Gates

- PQC 项目级检验快照门禁：检验设备、接收标准、检验方法和逐件结果必须来自正式 QA/PQC 项目快照，不得用固定字段或前端猜测替代。
- 用户可见描述与内部编码隔离门禁：用户可见标题使用正式展示字段，内部编码/项目名称继续用于 key、tab 和提交载荷身份。
- 前端静态契约隔离门禁：新增聚焦静态合同覆盖当前标题行为，避免用宽泛全量检查掩盖当前 UI 回归。

## Cleanup Keep

- doc/tasks/20260807-pqc-active-title-method-display/task.md
- doc/tasks/20260807-pqc-active-title-method-display/execution-log.md
- doc/tasks/20260807-pqc-active-title-method-display/verification-report.md
