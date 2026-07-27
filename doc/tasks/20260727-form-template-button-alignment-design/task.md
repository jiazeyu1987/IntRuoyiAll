# 表单模板三按钮独立执行纠偏

## Task Goal

纠正此前把“交互行为对齐”误解成“绑定批记录表单”的实现。表单模板与批记录表单没有直接数据关系，红框内三个按钮始终执行当前模板自身操作：

- `打开`：查看当前表单模板。
- `编辑`：进入当前表单模板规则编辑工作区。
- `填写`：进入当前表单模板模拟填写工作区。

## Milestones

- [x] 变更评估：记录用户澄清，确认不建立批记录数据绑定。
- [x] RED：前端、BPM 和迁移独立性合同稳定复现错误实现。
- [x] GREEN：恢复当前模板三个工作区，移除前后端错误绑定契约和未发布迁移。
- [x] 回归：聚焦静态合同、TypeScript、BPM Maven、数据库合同和真实页面点击通过。
- [x] 文档校准：更新四份设计文档、三份交付证据、验证报告和项目经验门禁。
- [x] Closeout：完成最终验证、cleanup、提交并推送到 `origin/int_main`。

## Expected Verification

- 变更申请和自测验证通过。
- 三个独立性合同按 RED/GREEN 留存证据。
- `pnpm ts:check` 通过。
- 真实 Playwright 页面点击显示三个 FormCenter 工作区，不显示绑定错误、不跳转 MES。
- 四份设计文档和三份交付证据验证器通过。
- UTF-8、`git diff --check` 和分支端口门禁通过。

## Current Status

completed

## 经验门禁

- “行为对齐”必须先拆分交互、数据和领域边界；交互相似不代表共享主键。
- FormCenter 模板查看、规则编辑和模拟填写只依赖当前模板自身上下文。
- 不保留“有绑定走 MES、无绑定走 FormCenter”的双路径或 fallback。
- 已进入本地 schema 的冗余列不得未经授权执行破坏性删除。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；三个按钮只有 FormCenter 自身正式链路。
- `是否从根因和长期维护角度解决`：是；移除错误跨域绑定，而不是隐藏提示。
- `是否存在临时补丁或绕过`：否；不伪造 `reportId`、不名称匹配、不保留兼容分支。

## Behavior Authorization

- 2026-07-27 用户曾要求“三个按钮按批记录表单执行”，此前被技术实现误解为建立 `reportId` 绑定。
- 2026-07-27 用户进一步明确：三个按钮提示未绑定是错误行为，实际表单与批记录表单没有直接关系。本条澄清是当前最终行为依据。

## Output Documents

- `doc/tasks/20260727-form-template-button-alignment-design/frontend-design.md`
- `doc/tasks/20260727-form-template-button-alignment-design/backend-api-design.md`
- `doc/tasks/20260727-form-template-button-alignment-design/data-model.md`
- `doc/tasks/20260727-form-template-button-alignment-design/config-security-deployment.md`
- `doc/tasks/20260727-form-template-button-alignment-design/frontend-feature-evidence.md`
- `doc/tasks/20260727-form-template-button-alignment-design/backend-api-evidence.md`
- `doc/tasks/20260727-form-template-button-alignment-design/database-schema-evidence.md`
- `doc/tasks/20260727-form-template-button-alignment-design/bug-regression-evidence.md`
- `doc/tasks/20260727-form-template-button-alignment-design/verification-report.md`

## Cleanup Keep

- doc/tasks/20260727-form-template-button-alignment-design/frontend-design.md
- doc/tasks/20260727-form-template-button-alignment-design/backend-api-design.md
- doc/tasks/20260727-form-template-button-alignment-design/data-model.md
- doc/tasks/20260727-form-template-button-alignment-design/config-security-deployment.md
- doc/tasks/20260727-form-template-button-alignment-design/frontend-feature-evidence.md
- doc/tasks/20260727-form-template-button-alignment-design/backend-api-evidence.md
- doc/tasks/20260727-form-template-button-alignment-design/database-schema-evidence.md
- doc/tasks/20260727-form-template-button-alignment-design/bug-regression-evidence.md

## Closeout Evidence

- 功能、自动化、真实 E2E、文档验证、UTF-8、差异和端口门禁均已通过。
- Cleanup preview/apply：PASS，11 份任务文档全部保留，无删除、阻塞或警告。
- 实现与设计证据由并行脏工作区基线提交 `698d6ba3` 保存；该提交同时包含其他并行任务文件，已在日志中明确记录。
- 补充收尾证据提交：`a6714535`。
- 远端同步整合提交：`97ecf51a`。
- 首次推送：`origin/int_main` 从 `70a4b414` 推进到 `97ecf51a`，成功。
- 收尾提交：`46380517`，仅包含本任务 `task.md`、`execution-log.md`、`verification-report.md`。
- 收尾推送：`origin/int_main` 从 `97ecf51a` 推进到 `46380517`，成功。
