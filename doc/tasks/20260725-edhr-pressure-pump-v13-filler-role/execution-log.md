# Execution Log: eDHR Pressure Pump V13.0 Filler Role Update

## User Intent

用户要求：给“球囊扩张压力泵”V13.0 的所有表单填写员都改成 `压力泵生产1` 这个角色；先创建该角色，并把角色赋给 `wangxin` 账号。

## Milestone Log

- PRECHECK: 已读取 `docs/task-closeout-rules.md`、`docs/database-rules.md`、`docs/login-access.md`、`docs/powershell-encoding.md`、`docs/server-access.md`、`docs/release-backup-restore.md`、`docs/powershell-memory.md`。
- PRECHECK: 已读取 database-schema-delivery 技能与 `references/database-contract.md`。
- PRECHECK: 已读取 `docs/experience-index.md`，命中 eDHR 填写人、`fillableUsers`、数据库与 PowerShell 相关门禁。
- PRECHECK: `git status --short --branch` 显示当前工作区存在其它任务的已暂存/未暂存/未跟踪改动；本任务不会覆盖或回滚这些改动。
- BDD: pressure pump V13 forms use production role -> Given 本机授权数据库中存在“球囊扩张压力泵”V13.0 表单配置和 `wangxin` 账号, When 创建/确认 `压力泵生产1` 角色、绑定给 `wangxin` 并将 V13.0 所有表单填写人规则更新为该角色, Then 所有目标表单填写人规则均指向该角色且详情可解析到该角色下用户。
- RED: 本机数据库只读核对 -> FAIL as expected, `system_role` 中租户 1 不存在 `压力泵生产1` / `pressure_pump_production_1`，V13.0 的 15 条 FILL 规则仍为 `USERS:149` 1 条、`USERS:1` 14 条。
- ACTION: 使用本机后端正式接口登录后执行配置变更；创建角色 `压力泵生产1`，角色 ID `910405`，分类 `批记录`，code `pressure_pump_production_1`。
- ACTION: 通过 `/system/permission/assign-user-role` 将角色 `910405` 追加给租户 1 的 `wangxin` 用户 `810`，保留原角色 `910295`。
- ACTION: 对 V13.0 批记录版本 `118` 下 15 个 `mes_pro_batch_record_report.report_id` 调用 `/mes/pro/edhr-process-form-permission-rule/save-by-report`，将 `fillRule.candidateSourceType` 保存为 `ROLE`、`candidateSourceIds=[910405]`。
- GREEN: 数据库验证 -> PASS, `system_role.id=910405` 启用且 `system_user_role` 存在 `user_id=810, role_id=910405`。
- GREEN: 表单规则验证 -> PASS, `mes_pro_edhr_process_form_permission_rule` 中版本 `118` 的 15 条 FILL 规则全部为 `ROLE / 910405`。
- GREEN: 运行态任务验证 -> PASS, `responsibility_source_key LIKE 'FORM|%|118'` 的 20 条活动 FILL 工作任务均解析到 `candidate_source_type=ROLE`、`candidate_user_snapshot=810`、`assignee_user_id=810`。
- GREEN: 详情接口验证 -> PASS, `GET /admin-api/mes/pro/edhr-batch-execution/get?id=900000000823` 返回的 V13.0 表单任务 `fillableUsers.displayName` 为 `王歆`。
- NOTE: 详情接口首次复核时 48081 短暂未监听；按本机运行门禁确认 PID `59548` 属于 `E:\IntRuoyi\IntRuoyiBackend` 的 int_main 后端，等待后健康检查恢复 `UP` 后继续验证，未切换环境或端口。
- EXPERIENCE: 已读取 project-experience-consolidation 技能并搜索既有经验归宿；本次新增经验已被 `docs/backend-development.md`、`docs/e2e-rules.md`、`docs/local-runtime.md` 现有门禁覆盖，未新建长期经验文档。
- CLOSEOUT: 实现与验证完成；由于主工作区存在本任务开始前的并行脏改动与未跟踪文件，未执行基线提交/推送，任务状态保持 `ready_for_closeout`。
- BDD: filler select displays full selected name -> Given 批记录表单填写人设置弹窗已打开且“填写人”选择了角色或人员, When 选中名称较长例如 `压力泵生产1`, Then “填写人”选择框应完整展示该名称，不得显示为 `压...` 等截断文本。
- RED: `node tests/e2e/edhr-batch-record-form-list-filler-static.spec.js` -> FAIL, 当前弹窗缺少填写人专用布局类，中间选择框被三等分网格压缩。
- GREEN: `node tests/e2e/edhr-batch-record-form-list-filler-static.spec.js` -> PASS。
- IMPLEMENTATION: 已将批记录表单填写人设置弹窗中间“填写人”列改为更宽的专用列，并覆盖 Element Plus 多选标签默认省略宽度。
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260725-edhr-pressure-pump-v13-filler-role/frontend-feature-evidence.md` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260725-edhr-pressure-pump-v13-filler-role/bug-regression-evidence.md` -> PASS。
- GREEN: `git diff --check -- IntRuoyiFronted/src/views/mes/pro/batchrecordformlist/index.vue IntRuoyiFronted/tests/e2e/edhr-batch-record-form-list-filler-static.spec.js` -> PASS, 仅提示 Git 将在下次触碰时按仓库设置 CRLF 转换。
- GREEN: UTF-8 task document read check -> PASS。
- GREEN: project-experience-consolidation -> PASS, 已将 Element Plus 多选选择框显示完整门禁合并到 `docs/e2e-rules.md` 并更新 `docs/experience-index.md` 路由。
