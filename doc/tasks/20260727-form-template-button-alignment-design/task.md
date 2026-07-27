# 表单模板三按钮对齐批记录表单行为设计

## Task Goal

将“表单模板”预览区红框内 `打开 / 编辑 / 填写` 三个按钮的目标行为设计为与“批记录表单”一致：同样以稳定 `reportId` 进入批记录表单设计器预览、设计器编辑和模板模拟填写路径；不得继续使用表单模板本页弹窗、模拟填写弹窗或 `jimuSchema` 保存链路作为这三个按钮的替代行为。

## Milestones

- [x] 现状核对：确认表单模板按钮和批记录表单按钮当前调用链路不一致。
- [x] 设计范围：定义前端、后端 API、数据模型、权限与失败行为。
- [x] 阻塞项识别：明确表单模板响应当前缺少稳定批记录 `reportId` 映射。
- [x] 文档输出：生成任务级系统设计文档，供后续实现按 BDD/TDD 执行。
- [ ] 实现与验证：本任务仅做文档设计，生产代码修改另起实现任务。
- [ ] Closeout：受当前工作区已有非本任务脏改动和本地领先提交影响，暂不执行提交/推送。

## Expected Verification

- 文档结构检查：四份设计文档包含系统设计技能要求的必备章节。
- UTF-8 读取检查：所有中文任务文档可通过 `python -X utf8` 正常读取。
- 设计一致性检查：文档明确禁止名称匹配、空值兜底、静默回退旧弹窗。
- 后续实现验证：前端最小静态契约 + 后端接口契约测试 + 真实页面路径 E2E。

## Current Status

ready_for_implementation_design_complete

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

