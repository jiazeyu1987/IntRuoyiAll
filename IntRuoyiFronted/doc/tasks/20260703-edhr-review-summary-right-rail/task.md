# 任务：eDHR 复盘摘要移入右侧栏

## 任务目标

将批记录详情页截图红框中的“基础 / 详情”入口和当前执行摘要，从表单上方移动到表单右侧蓝框区域，减少表单顶部占高并利用右侧留白。

## 里程碑

- [x] M1：读取 PowerShell、经验索引、前端交付技能和统一前端样式门禁。
- [x] M2：补充 RED 静态契约，锁定右侧信息栏布局。
- [x] M3：最小修改 `BatchExecutionDetailPage.vue` 模板与样式，不改接口和数据契约。
- [x] M4：运行目标静态验证和前端证据校验。
- [x] M5：更新任务记录、收尾清理并仅提交本轮直接改动。

## 经验门禁

- PowerShell / Windows shell / 中文编码：已先读取 `docs/powershell-memory.md`；中文任务文档和命令输出使用显式 UTF-8。
- 前端页面 / 表格 / 样式：已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`；本轮只做 eDHR 详情页局部布局调整，保持蓝白灰运营台风格和紧凑控件。
- 前端交付：已读取 `frontend-feature-delivery` 与 `references/frontend-contract.md`；按 BDD + RED/GREEN 记录证据。
- 真实 E2E：本轮不执行真实登录、写入、服务器或数据库操作；若后续需要真实 E2E，需先读取 `docs/login-access.md` 并跑登录 preflight。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；从模板结构上把摘要归入右侧信息栏，而不是用定位覆盖或临时负边距。
- 是否存在临时补丁或绕过：否。

## BDD 场景

- BDD: 复盘摘要进入右侧栏 -> Given 用户打开 eDHR 批记录详情页并选中工序 / When 复盘区域渲染 / Then 页面呈现工序列表、已填写批记录、右侧摘要栏三列，“基础 / 详情”入口和执行编号、状态、提交时间、审批时间显示在右侧栏。
- BDD: 表单顶部不再占用摘要区域 -> Given 用户查看已填写批记录 / When 表单区域渲染 / Then 已填写批记录上方不再直接渲染 `el-descriptions` 和摘要标签，表单主体贴近顶部展示。
- BDD: 右侧栏无执行记录时保留任务态提示 -> Given 当前工序尚未形成执行记录 / When 右侧摘要栏渲染 / Then 仍显示未打开、状态、签名、审批等任务态信息，不隐藏必要状态。

## 预期验证

- `node tests/e2e/edhr-review-summary-right-rail-static.spec.js`
- `node tests/e2e/edhr-batch-basic-info-dialog-static.spec.js`
- `node tests/e2e/edhr-batch-detail-review-fusion-static.spec.js`
- `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260703-edhr-review-summary-right-rail/frontend-feature-evidence.md`

## 当前状态

- 状态：completed
- 当前里程碑：完成。
- 已完成：新增右侧栏静态契约并完成模板/样式调整；目标静态回归、证据校验、diff 检查和收尾预览均通过。
- 阻塞：暂无。

## 最终验证

- PASS: `node tests/e2e/edhr-review-summary-right-rail-static.spec.js`
- PASS: `node tests/e2e/edhr-batch-basic-info-dialog-static.spec.js`
- PASS: `node tests/e2e/edhr-batch-detail-review-fusion-static.spec.js`
- PASS: `node tests/e2e/edhr-remove-redundant-review-copy-static.spec.js`
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260703-edhr-review-summary-right-rail/frontend-feature-evidence.md`
- PASS: `git diff --check -- src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue tests/e2e/edhr-review-summary-right-rail-static.spec.js tests/e2e/edhr-batch-basic-info-dialog-static.spec.js doc/tasks/20260703-edhr-review-summary-right-rail/task.md doc/tasks/20260703-edhr-review-summary-right-rail/execution-log.md doc/tasks/20260703-edhr-review-summary-right-rail/frontend-feature-evidence.md docs/request-command-log.md`
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260703-edhr-review-summary-right-rail --mode preview`

## Cleanup Keep

- doc/tasks/20260703-edhr-review-summary-right-rail/frontend-feature-evidence.md
