# 表单模板三按钮对齐批记录表单行为设计

## Task Goal

将“表单模板”预览区红框内 `打开 / 编辑 / 填写` 三个按钮的目标行为设计为与“批记录表单”一致：同样以稳定 `reportId` 进入批记录表单设计器预览、设计器编辑和模板模拟填写路径；不得继续使用表单模板本页弹窗、模拟填写弹窗或 `jimuSchema` 保存链路作为这三个按钮的替代行为。

## Milestones

- [x] 现状核对：确认表单模板按钮和批记录表单按钮当前调用链路不一致。
- [x] 设计范围：定义前端、后端 API、数据模型、权限与失败行为。
- [x] 阻塞项识别：明确表单模板响应当前缺少稳定批记录 `reportId` 映射。
- [x] 文档输出：生成任务级系统设计文档，供后续实现按 BDD/TDD 执行。
- [x] 实现与验证：补齐显式批记录绑定字段、迁移 SQL、前端三按钮同源批记录路由，并通过任务专用合同验证。
- [x] 文档校准：将设计文档从“推荐映射表/待定接口”更新为已落地的 `bpm_form_template_version` 显式绑定字段方案。
- [x] 运行态前置核对：本地 schema 已应用，前端入口可登录访问；当前 48081 原始 jar 尚未加载新增响应字段。
- [x] 真实 E2E：使用临时可回滚本地夹具，逐个点击 `打开 / 编辑 / 填写` 并验证路由与批记录表单一致。
- [x] 临时产物清理：删除本任务专用 `.runtime` JAR 目录和未登记为 Git worktree 的临时构建快照。
- [x] Cleanup：`task-closeout-cleanup` preview/apply 均通过，无删除项、阻塞项或警告。
- [ ] 行为冲突解除：并行任务已将 `打开 / 编辑 / 填写` 全部改回 FormCenter 本页流程，与本任务目标相反。
- [ ] Closeout：待同文件行为冲突明确解除后，恢复对齐、重跑验证并完成提交/推送。

## Expected Verification

- 文档结构检查：四份设计文档包含系统设计技能要求的必备章节。
- UTF-8 读取检查：所有中文任务文档可通过 `python -X utf8` 正常读取。
- 设计一致性检查：文档明确禁止名称匹配、空值兜底、静默回退旧弹窗。
- 实现验证：前端最小静态契约 + 后端接口契约测试 + SQL 迁移契约 + 前端类型检查。
- 后续运行态验收：真实页面选择已绑定模板，分别点击 `打开 / 编辑 / 填写`，验证路由与批记录表单一致。

## Current Status

blocked

## 经验门禁

- 前端按钮行为变更必须先记录 BDD，并用静态契约锁定 `打开 / 编辑 / 填写` 不再走本页弹窗或本地模拟填写。
- 后端不得根据模板名、源文件名、版本号猜测 `reportId`；必须有可追溯、租户隔离、唯一的正式映射。
- 缺少 `reportId` 时必须 fail fast 显示“当前模板未绑定批记录表单”，不得回退到旧弹窗、空页面、默认成功或 API-only 替代路径。
- 涉及数据映射或 schema 变更时，必须先核对真实表结构或迁移文件，再设计迁移与回归测试。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；设计要求缺少映射时 fail fast，不允许静默回退旧逻辑。
- `是否从根因和长期维护角度解决`：是；根因是表单中心模板缺少稳定批记录 `reportId`，设计先补正式映射再改按钮。
- `是否存在临时补丁或绕过`：否；禁止按模板名、版本号、源文件名模糊匹配批记录报表。

## Output Documents

- `doc/tasks/20260727-form-template-button-alignment-design/frontend-design.md`
- `doc/tasks/20260727-form-template-button-alignment-design/backend-api-design.md`
- `doc/tasks/20260727-form-template-button-alignment-design/data-model.md`
- `doc/tasks/20260727-form-template-button-alignment-design/config-security-deployment.md`
- `doc/tasks/20260727-form-template-button-alignment-design/verification-report.md`
- `doc/tasks/20260727-form-template-button-alignment-design/frontend-feature-evidence.md`
- `doc/tasks/20260727-form-template-button-alignment-design/backend-api-evidence.md`
- `doc/tasks/20260727-form-template-button-alignment-design/database-schema-evidence.md`

## Cleanup Keep

- doc/tasks/20260727-form-template-button-alignment-design/frontend-design.md
- doc/tasks/20260727-form-template-button-alignment-design/backend-api-design.md
- doc/tasks/20260727-form-template-button-alignment-design/data-model.md
- doc/tasks/20260727-form-template-button-alignment-design/config-security-deployment.md
- doc/tasks/20260727-form-template-button-alignment-design/frontend-feature-evidence.md
- doc/tasks/20260727-form-template-button-alignment-design/backend-api-evidence.md
- doc/tasks/20260727-form-template-button-alignment-design/database-schema-evidence.md

## Current Blocker

- 2026-07-27 当前工作区中的 `IntRuoyiFronted/src/views/form-center/template/index.vue` 已被并行任务改为：`打开` 进入 `TemplateViewDialog`、`编辑` 进入本页规则编辑、`填写` 进入本页模拟填写。
- `IntRuoyiFronted/tests/e2e/form-template-batch-record-button-alignment-static.spec.js` 也已被反向改写并暂存，用于要求三个按钮不得依赖批记录绑定。
- 该行为与本任务及用户明确要求“表单模板必须按批记录表单的 3 个按钮行为对齐”直接冲突。未取得行为优先级确认前，不覆盖并发任务已暂存的同文件改动。
