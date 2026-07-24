# 20260630 修复发布后角色门禁对旧 wenkong 编码的硬依赖执行日志

BDD: 发布后 DCC E2E 角色兼容当前角色编码基线 -> Given tenant_id=1 的芋道源码租户已不存在 code=wenkong 但存在启用角色 doc_control/文控 与 wenkong_download/文控下载 When 执行 20260618_post_release_role_e2e_gate.sql Then 迁移必须识别当前有效 DCC E2E 角色并继续完成 wangsiyu 账号绑定。

BDD: 旧角色编码仍存在时门禁保持兼容 -> Given tenant_id=1 仍存在启用中的 wenkong 角色 When 重复执行 20260618_post_release_role_e2e_gate.sql Then 迁移继续复用现有角色，不重复创建脏角色，也不放宽其他 fail-fast 门禁。

GREEN: experience-preflight -> PASS，已读取 `D:\ProjectPackage\Int\IntRuoyiMaintance\docs\experience-index.md`、`D:\ProjectPackage\Int\IntRuoyiMaintance\docs\release-build-preflight-lessons.md`、`D:\ProjectPackage\Int\IntRuoyiMaintance\docs\release-agent-checklist.md`，确认 required SQL 发布阻塞必须先只读核对真实库前置条件，不得手工改测试库绕过。

GREEN: maintenance-log-readonly-evidence -> PASS，只读核对维护仓 `D:\ProjectPackage\Int\IntRuoyiMaintance\runtime\runtime-control\op-2026-06-30T124330918809800Z-e45cfcee-3242-4752-a407-eec8cb860ebc.json` 与对应 log 后确认：测试服真实发布失败于 `20260618_post_release_role_e2e_gate.sql`，错误为 `Missing enabled wenkong role; cannot prepare wangsiyu DCC E2E account`。

GREEN: test-env-role-baseline-audit -> PASS，只读查询测试服真实库后确认：租户 `1/芋道源码` 存在启用用户 `wangsiyu(id=910250)`，不存在启用角色 `code='wenkong'`；当前启用 DCC 相关角色基线为 `doc_control/文控(id=910233)` 与 `wenkong_download/文控下载(id=910234)`。这说明当前阻塞不是账号缺失，而是发布后角色门禁 SQL 对旧角色编码基线假设过强。

RED: `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_post_release_role_e2e_gate_sql.py -q` -> FAIL，新增契约 `test_post_release_role_gate_accepts_current_doc_control_role_baseline_for_dcc_account` 后，旧 SQL 仍只硬依赖 `code='wenkong'`，既不识别 `doc_control` / `wenkong_download`，也继续保留旧错误文案 `Missing enabled wenkong role; cannot prepare wangsiyu DCC E2E account`。

GREEN: role-code-baseline-fix -> PASS，已将 `sql/mysql/20260618_post_release_role_e2e_gate.sql` 修复为：admin 租户 DCC 角色候选改为 `doc_control`、`wenkong`、`wenkong_download`，并按 `doc_control -> wenkong -> wenkong_download` 的优先顺序选取启用角色；缺失时统一抛出正式错误 `Missing enabled DCC role; cannot prepare wangsiyu DCC E2E account`，不再把旧 `wenkong` 编码当作唯一正式前置条件。

GREEN: `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_post_release_role_e2e_gate_sql.py -q` -> PASS，12 passed in 0.10s。

GREEN: `python -X utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\release\run-release-migration-policy-gate.py --sql-root D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\sql\mysql` -> PASS，`status=passed`，`migrationCount=233`。
