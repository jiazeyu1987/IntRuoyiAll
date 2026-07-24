# 20260703 eDHR 工序复盘三栏布局

## 任务目标

按照用户截图标注调整 eDHR 批次执行详情页工序复盘区域：左侧红框只显示工序列表，中间蓝框显示当前工序对应表单，右侧黄框显示针对当前工序的控制按钮。保留现有接口和证据链入口，不新增 fallback 或模拟数据。

## 里程碑

1. 建立任务文档、读取经验门禁并确认页面位置。completed
2. 补充 RED 静态回归，覆盖工序 / 表单 / 当前工序按钮三栏语义。completed
3. 最小调整 `BatchExecutionDetailPage.vue` 模板与样式。completed
4. 运行目标静态验证和相关回归。completed
5. 更新任务记录并按验证结果收尾。completed

## 经验门禁

- PowerShell / Windows shell：已读取根仓 `docs/powershell-memory.md`；本轮命令显式 UTF-8，不使用 `&&`。
- 前端页面 / 表格 / 样式：已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`；本次仅调整工序复盘操作台布局，沿用蓝/中性运维控制台风格。
- 前端交付：已读取 `frontend-feature-delivery` 与 `references/frontend-contract.md`；必须记录 BDD、RED/GREEN 和前端证据。
- QA：已读取 `quality-assurance-test-suite` 与 `references/qa-contract.md`；本次先补静态契约测试，不做真实 E2E、登录后写入、服务器写入或租户数据修改。

## BDD 场景

- BDD: 左侧聚焦工序 -> Given 用户打开 eDHR 批次详情的工序复盘区域 / When 批记录数据加载完成 / Then 左侧列表显示工序编号、工序编码、工序名称和状态，不再以“已填写表单”作为左栏标题。
- BDD: 中间聚焦表单 -> Given 用户选中某个工序 / When 查看中间内容区 / Then 中间只承载当前工序基础信息和已填写表单内容。
- BDD: 右侧聚焦当前工序控制按钮 -> Given 用户选中某个工序 / When 查看右侧操作区 / Then 当前工序的打开工序、工作任务、执行追踪、签名记录、审批记录、单表归档、字段审计、操作审计、变更记录、统一变更、主数据追溯、历史同工序、独立表单、记录本引用等入口集中显示在右侧。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否；仅调整既有数据和入口的布局，不新增兜底数据、吞异常或默认成功。
- 是否从根因和长期维护角度解决：是；按用户标注重新划分工序导航、表单内容、工序操作入口的职责边界。
- 是否存在临时补丁或绕过：否。

## 预期验证

- `node tests/e2e/edhr-process-form-action-columns-static.spec.js`
- `node tests/e2e/edhr-process-evidence-fusion-static.spec.js`
- `node tests/e2e/edhr-batch-detail-review-fusion-static.spec.js`

## 当前状态

- 状态：completed
- 当前里程碑：完成
- 已完成：确认目标页面为 `src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue`；新增三栏静态契约；页面已调整为左工序、中表单、右控制按钮；三项静态回归、证据校验和类型检查通过。
- 阻塞：暂无。

## Current Status

completed

## 最终验证

- `node tests/e2e/edhr-process-form-action-columns-static.spec.js` -> PASS
- `node tests/e2e/edhr-process-evidence-fusion-static.spec.js` -> PASS
- `node tests/e2e/edhr-batch-detail-review-fusion-static.spec.js` -> PASS
- `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260703-edhr-process-form-action-columns\frontend-feature-evidence.md` -> PASS
- `python -X utf8 C:\Users\BJB110\.codex\skills\quality-assurance-test-suite\scripts\validate_quality_assurance.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260703-edhr-process-form-action-columns\quality-assurance-evidence.md` -> PASS
- `pnpm ts:check` -> 首次因 Node 默认堆内存 OOM 失败；设置 `NODE_OPTIONS=--max-old-space-size=8192` 后 PASS。
