# PQC/报工管理提交列表结构化列

## Task Goal

- 删除提交列表主表中的“生产工单”“PQC”“提交内容”红框列。
- 报工管理与 PQC 管理主列表都直接展示提交参数，不再用统一“提交内容”列承载明细。
- PQC 参数超出班组长/QA 规程冻结上下限时允许提交，但列表展示中对应数值标红提示异常。

## Milestones

- [x] 建立任务文档、BDD 场景和前端证据文件。
- [x] 编写专用静态合同，先验证旧“提交内容”列表 RED。
- [x] 修改提交列表列定义、表格列和结构化 payload 解析。
- [x] 运行定向静态合同、相邻 PQC/报工静态合同、类型检查和证据校验。
- [x] 完成验证报告、收尾记录和 cleanup 预检/应用。
- [ ] 完成提交并推送。

## Expected Verification

- `node tests\e2e\pqc-submission-structured-columns-static.spec.js`
- `node tests\e2e\pqc-leader-standard-list-template-static.spec.js`
- `node tests\e2e\pqc-leader-item-snapshot-static.spec.js`
- `node tests\e2e\mes-frontline-pqc-submit-to-leader-chain-static.spec.js`
- `pnpm ts:check`
- `git diff --check -- IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue IntRuoyiFronted/src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue IntRuoyiFronted/tests/e2e/pqc-submission-structured-columns-static.spec.js doc/tasks/20260806-pqc-submission-structured-columns`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260806-pqc-submission-structured-columns/frontend-feature-evidence.md`

## Diagnostic Checks

- `node tests\e2e\team-leader-workbench-static.spec.cjs` -> FAIL, existing unrelated dirty change removed `data-team-leader-defect-reason-select` and abnormal reason binding before this task; not used as this task completion gate.

## Applicable Gates

- 前端功能交付：先记录 BDD，再执行 RED/GREEN/REGRESSION，保持现有 API、路由和 UI 模式。
- 前端静态契约隔离门禁：本任务用专用静态合同锁定主列表结构化字段，避免全量检查的历史问题掩盖当前需求。
- MES PQC 项目级检验快照门禁：PQC 明细必须读取 `pqcItemDetails/itemResults` 中冻结的设备、编号、接收标准、方法、上下限和样本值，不得回退到固定字段或 raw payload 猜测。
- UTF-8/PowerShell 门禁：中文文档和命令输出按 UTF-8 处理，PowerShell 不使用 `&&`。

## Design Constraint Check

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，主列表直接解析正式结构化提交事实与冻结快照。
- `是否存在临时补丁或绕过`：否。

## Current Status

blocked

- 结构化列实现、定向验证、类型检查和 cleanup 均已完成；最终提交/推送被共享工作区阻塞。
- 阻塞原因：当前 `int_main` 与 `origin/int_main` 已对齐，但工作区存在大量并行脏改动；`TeamLeaderWorkbenchPage.vue` 同时包含本任务结构化列表改动和其它任务的活跃订单/异常原因改动，不能安全使用宽泛暂存或推送。
