# 执行日志：eDHR 工序表单签核摘要展示

- BDD: 展示工序表单签核摘要 -> Given 用户打开 eDHR 批次复盘页并选中已填写工序 / When 右侧工序摘要渲染 / Then 蓝框内显示填写、审核、批准三类签核摘要入口。
- BDD: 按真实签名动作聚合人员时间 -> Given 当前工序存在 FIELD_CHANGE、SUBMIT、FORM_REVIEW、APPROVE 签名记录 / When 签核摘要计算 / Then FIELD_CHANGE 与 SUBMIT 归入填写，FORM_REVIEW 归入审核，APPROVE 归入批准，并按展示时间升序排列。
- BDD: 紧凑展示多人多时间 -> Given 同一签核类型有多个人和多次时间 / When 用户查看该类型摘要 / Then 折叠态显示人数和次数，展开态显示人员、时间、动作含义和备注，不把明细直接铺满右侧蓝框。
- RED: `node tests/e2e/mes-edhr-batch-review-signoff-summary-static.spec.js` -> FAIL，expected reason: 右侧轨道尚未新增“签核摘要”区块。
- FIX: `apply_patch` -> 在 `BatchExecutionDetailPage.vue` 使用现有 `selectedExecution.signatureRecords` 聚合填写、审核、批准签核摘要；在 `batchExecution.ts` 补齐签名时间字段类型。
- GREEN: `node tests/e2e/mes-edhr-batch-review-signoff-summary-static.spec.js` -> PASS。
- GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260703-edhr-process-signoff-summary/frontend-feature-evidence.md` -> PASS。
- CLOSEOUT: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260703-edhr-process-signoff-summary --mode preview` -> PASS，delete 仅包含 `frontend-feature-evidence.md`，blocked/warnings 均为 `<none>`。
