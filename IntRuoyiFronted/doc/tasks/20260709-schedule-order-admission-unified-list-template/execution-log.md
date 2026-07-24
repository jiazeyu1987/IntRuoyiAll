# 20260709 待同步差异列表替换标准列表模板执行日志

BDD: 待同步差异弹窗使用标准列表模板 -> Given 用户打开排产工单页并点击同步工单 When 待同步差异弹窗展示 Then 筛选区、显示字段、列表、分页由 `UnifiedListTemplate` 承载，原有汇总、批量加入排产工单池和行操作仍可使用。

BDD: 待同步差异列表保留业务筛选与状态列 -> Given 用户需要筛选待同步生产工单 When 输入工单编码、产品编号、选择入池状态或阻断原因 Then 查询仍调用原待同步差异接口并刷新列表、汇总和阻断原因选项。

BDD: 待同步差异列表支持字段配置 -> Given 用户在待同步差异弹窗内调整显示字段或拖拽列宽 When 配置保存 Then 使用独立 table key `mes.pro.scheduleOrder.admissionDiff` 持久化，不影响排产工单主列表字段配置。

BDD: 待同步差异列表列宽可拖拽 -> Given 用户打开待同步差异弹窗 When 鼠标拖拽表头列宽 Then Element Plus 表格提供列宽拖拽手柄，并把保存后的列宽绑定回对应业务列。

## 设计与门禁

- 经验门禁：PowerShell / UTF-8、统一前端样式、标准列表模板。
- 设计约束：不引入 fallback；不改变后端接口；不改权限、分页、行选择或批量入池业务规则。

## 执行记录

- GREEN: project-preflight -> PASS，已读取 PowerShell 经验、前端交付技能、统一前端样式、经验索引，并确认近期前端任务文档均已完成。

- RED: node tests/e2e/mes-pro-schedule-order-admission-unified-list-template-static.spec.js -> FAIL，待同步差异列表尚未接入 `UnifiedListTemplate` 独立 table key。
- GREEN: apply_patch -> 将待同步差异弹窗接入 `UnifiedListTemplate`，新增 `mes.pro.scheduleOrder.admissionDiff` 字段配置、列宽拖拽、快速过滤、显示字段配置和分页托管。
- GREEN: node tests/e2e/mes-pro-schedule-order-admission-unified-list-template-static.spec.js -> PASS。
- GREEN: node tests/e2e/mes-pro-schedule-order-batch-admission-static.spec.js -> PASS。
- GREEN: node tests/e2e/mes-pro-schedule-order-pool-static.spec.js -> PASS。
- GREEN: node tests/e2e/unified-list-template-static.spec.js -> PASS。
- GREEN: node tests/e2e/user-table-column-config-static.spec.js -> PASS。
- BLOCKER: NODE_OPTIONS=--max-old-space-size=8192 pnpm ts:check -> FAIL，PowerShell 不支持 Bash 风格环境变量前缀。
- GREEN: $env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check -> PASS。
- BLOCKER: frontend-feature-evidence-validation -> missing RED/GREEN/Verification markers，已补齐证据固定标记后重验。
- GREEN: frontend-feature-evidence-validation -> PASS，前端证据固定标记校验通过。
- GREEN: task-status -> completed，任务文档已标记完成。
- BLOCKER: task-closeout-apply -> status unknown，收尾脚本未识别任务完成状态；已补 `Status: completed` 后重跑。
- GREEN: task-status-parser -> PASS，新增 `## Current Status` / `completed` 供收尾脚本识别。
- GREEN: task-closeout-preview -> PASS，预览仅删除 `frontend-feature-evidence.md`，无阻塞。
- GREEN: task-closeout-apply -> PASS，已删除临时前端证据文件，保留 task.md 与 execution-log.md。
- Git commit -> BLOCKED，当前前端仓存在 93 个脏改路径，且本任务修改的 `src/views/mes/pro/scheduleorder/index.vue` 与 `docs/request-command-log.md` 在本轮开始前已存在未提交改动；为避免混入非本任务 hunk，未创建提交。

- RED: node tests/e2e/mes-pro-schedule-order-admission-unified-list-template-static.spec.js -> FAIL，待同步差异表格未启用 `border`，Element Plus 列宽拖拽手柄不会生效；部分业务列只设置 `min-width`，未把持久化列宽绑定回 `width`。
- GREEN: apply_patch -> 为待同步差异 `el-table` 启用 `border`，并为工单编码、产品编号、产品名称、规格型号、不可排原因补齐 `getWorkOrderAdmissionColumnWidthString(...)` 绑定。
- GREEN: node tests/e2e/mes-pro-schedule-order-admission-unified-list-template-static.spec.js -> PASS。
- GREEN: node tests/e2e/mes-pro-schedule-order-batch-admission-static.spec.js -> PASS。
- GREEN: node tests/e2e/mes-pro-schedule-order-pool-static.spec.js -> PASS。
- GREEN: node tests/e2e/unified-list-template-static.spec.js -> PASS。
- GREEN: node tests/e2e/user-table-column-config-static.spec.js -> PASS。
- GREEN: $env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check -> PASS。
