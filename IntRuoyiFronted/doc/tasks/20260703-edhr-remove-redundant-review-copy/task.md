# 任务：删除 eDHR 工序复盘冗余说明

## 任务目标

删除 eDHR 批次执行详情页中用户红框标注的冗余说明内容，让首屏直接聚焦工序列表、表单内容和当前工序控制按钮。

## 里程碑

1. 建立任务文档、读取经验门禁并确认页面位置。completed
2. 补充 RED 静态回归，覆盖红框冗余内容不再渲染。completed
3. 最小修改 `BatchExecutionDetailPage.vue` 模板，不改接口和数据契约。completed
4. 运行目标静态验证和类型检查。completed
5. 更新任务记录与收尾状态。completed

## 经验门禁

- PowerShell / Windows shell：已先读取 `docs/powershell-memory.md`；中文文本读写使用 UTF-8 读取或 `apply_patch`。
- 前端页面 / 表格 / 样式：已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`；本次仅删除冗余说明，不做无关视觉重构。
- 真实 E2E / 服务器 / 数据库：本次不涉及真实登录、服务器写入、数据库写入或发布链路。

## BDD 场景

- BDD: 删除复盘顶部冗余标题说明 -> Given 用户打开 eDHR 批次执行详情页 / When 工序复盘区域渲染 / Then 不再显示“工序复盘”标题和围绕工序的说明文案，保留基础信息与刷新复盘按钮。
- BDD: 删除表单区冗余工序摘要 -> Given 用户选中一个工序 / When 中间表单区渲染 / Then 不再重复显示“表单 / 已填写表单 / 当前工序”摘要头，直接显示执行状态和已填写批记录。
- BDD: 删除右侧控制按钮冗余摘要 -> Given 用户选中一个工序 / When 右侧控制按钮区渲染 / Then 不再重复显示“当前工序控制按钮”标题、说明和工序摘要，直接展示工序执行、审签归档、审计追溯和关联引用入口。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；从模板源头移除冗余展示，保留既有状态、接口和按钮入口。
- 是否存在临时补丁或绕过：否。

## 预期验证

- `node tests/e2e/edhr-remove-redundant-review-copy-static.spec.js`
- `node tests/e2e/edhr-batch-basic-info-dialog-static.spec.js`
- `node tests/e2e/edhr-batch-detail-review-fusion-static.spec.js`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check`
- `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260703-edhr-remove-redundant-review-copy/frontend-feature-evidence.md`

## 当前状态

- 状态：completed
- 当前里程碑：完成。
- 已完成：已删除 `BatchExecutionDetailPage.vue` 中红框冗余内容；聚焦静态契约、基础信息弹框契约和复盘融合契约均通过。
- 阻塞：全量 `pnpm ts:check` 被非本次修改文件 `src/views/mes/pro/feedback/index.vue` 的既有类型错误阻塞；本次不提交，避免混入其它任务改动。

## 最终验证

- PASS: `node tests/e2e/edhr-remove-redundant-review-copy-static.spec.js`
- PASS: `node tests/e2e/edhr-batch-basic-info-dialog-static.spec.js`
- PASS: `node tests/e2e/edhr-batch-detail-review-fusion-static.spec.js`
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260703-edhr-remove-redundant-review-copy/frontend-feature-evidence.md`
- FAIL: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check`，失败点为非本次修改文件 `src/views/mes/pro/feedback/index.vue` 第 125 行与第 688 行既有类型错误。

## Current Status

completed
