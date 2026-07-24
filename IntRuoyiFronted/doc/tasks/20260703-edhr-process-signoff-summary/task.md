# 任务：eDHR 工序表单签核摘要展示

## 任务目标

在 eDHR 批次复盘页右侧工序摘要蓝框中，紧凑展示当前工序表单的填写、审核、批准人员与对应时间，支持同一类型多人员多时间展开查看。

## 里程碑

1. 建立任务文档、读取经验门禁并确认页面位置。completed
2. 补充 RED 静态回归，覆盖签核摘要聚合与紧凑展示。completed
3. 最小修改批次复盘页模板、类型与样式，不新增后端接口。completed
4. 运行目标静态验证、类型检查和前端证据校验。completed
5. 更新任务记录、收尾状态并提交本任务改动。completed

## 经验门禁

- PowerShell / Windows shell：已先读取 `docs/powershell-memory.md`；中文文本读写使用 UTF-8 Python 写入或 `apply_patch`。
- 前端页面 / 表格 / 样式：已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`；本次沿用蓝/中性运维控制台风格，只增强右侧轨道展示。
- 前端交付：已读取 `frontend-feature-delivery` 与 `frontend-contract.md`；需记录 BDD、RED/GREEN 与前端证据。
- 真实 E2E / 服务器 / 数据库：本次仅前端静态展示增强，不涉及真实登录、服务器写入、数据库写入或发布链路。

## BDD 场景

- BDD: 展示工序表单签核摘要 -> Given 用户打开 eDHR 批次复盘页并选中已填写工序 / When 右侧工序摘要渲染 / Then 蓝框内显示填写、审核、批准三类签核摘要入口。
- BDD: 按真实签名动作聚合人员时间 -> Given 当前工序存在 FIELD_CHANGE、SUBMIT、FORM_REVIEW、APPROVE 签名记录 / When 签核摘要计算 / Then FIELD_CHANGE 与 SUBMIT 归入填写，FORM_REVIEW 归入审核，APPROVE 归入批准，并按展示时间升序排列。
- BDD: 紧凑展示多人多时间 -> Given 同一签核类型有多个人和多次时间 / When 用户查看该类型摘要 / Then 折叠态显示人数和次数，展开态显示人员、时间、动作含义和备注，不把明细直接铺满右侧蓝框。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；直接复用现有真实 `signatureRecords` 聚合展示，不新增冗余数据源。
- 是否存在临时补丁或绕过：否。

## 预期验证

- `node tests/e2e/mes-edhr-batch-review-signoff-summary-static.spec.js`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check`
- `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260703-edhr-process-signoff-summary/frontend-feature-evidence.md`

## 当前状态

- 状态：completed
- 当前里程碑：完成。
- 已完成：已读取 PowerShell、前端交付、前端样式经验门禁；已定位目标页 `BatchExecutionDetailPage.vue` 与接口类型 `batchExecution.ts`；已完成签核摘要实现，目标静态测试、类型检查、前端证据校验和收尾预览通过。
- 阻塞：暂无。

## 最终验证

- PASS: `node tests/e2e/mes-edhr-batch-review-signoff-summary-static.spec.js`
- PASS: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check`
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260703-edhr-process-signoff-summary/frontend-feature-evidence.md`
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260703-edhr-process-signoff-summary --mode preview`

## Current Status

completed
