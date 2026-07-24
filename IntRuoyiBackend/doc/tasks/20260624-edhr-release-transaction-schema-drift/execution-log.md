# 执行日志：eDHR放行事务表生命周期列漂移修复

BDD: 旧表缺生命周期列时可幂等修复 -> Given 运行库已存在 mes_pro_edhr_release_transaction 但缺少 submit_idempotency_key 等生命周期列 / When 执行修复 SQL / Then 表结构补齐 DO 查询所需列，Mapper 查询不再触发 Unknown column。

BDD: 新建库基础表结构与当前 DO 一致 -> Given 新环境从 eDHR 放行基础建表 SQL 初始化 / When 后端按 MesProEdhrReleaseTransactionDO 查询事务 / Then SELECT 字段均存在，不依赖后续菜单类 SQL 才能避免基础查询失败。

INFO: bug-evidence -> 用户提供真实 SQL 异常：`Unknown column 'submit_idempotency_key' in 'field list'`，失败 SQL 来自 `MesProEdhrReleaseTransactionMapper` 查询 `mes_pro_edhr_release_transaction`。

GREEN: root-cause-readonly -> PASS，只读核对 `MesProEdhrReleaseTransactionDO` 已声明 `submitIdempotencyKey/submittedBy/submittedAt/approvalIdempotencyKey/.../withdrawReason`，基础建表 SQL 未声明这些列，后续 lifecycle SQL 有补列逻辑但当前运行库未执行到一致状态。

GREEN: experience-preflight -> PASS，本次数据库写入仅限本机 `127.0.0.1:3306/ruoyi-vue-pro` schema 补列；不删除、不覆盖、不改正式服/测试服；修复 SQL 使用 `information_schema.columns` 幂等判断。

RED: python -X utf8 -m pytest script\tests\test_edhr_release_precheck_schema_sql.py script\tests\test_edhr_release_transaction_schema_sql.py -q -> FAIL, 基础建表 SQL 缺少 `submit_idempotency_key` 等生命周期列，且缺少 `20260624_mes_edhr_release_transaction_lifecycle_column_repair.sql`。

GREEN: schema-fix -> PASS，补齐 `20260618_mes_edhr_release_precheck_engine.sql` 建表字段，并新增只补列的幂等修复 SQL `20260624_mes_edhr_release_transaction_lifecycle_column_repair.sql`。

GREEN: python -X utf8 -m pytest script\tests\test_edhr_release_precheck_schema_sql.py script\tests\test_edhr_release_transaction_schema_sql.py -q -> PASS, 12 passed。

GREEN: local-db-precheck -> PASS，`127.0.0.1:23306/ruoyi-vue-pro` 当前缺少 14 个生命周期列；`127.0.0.1:3306` root/123456 无访问权限，未操作。

GREEN: local-db-repair -> PASS，在 `127.0.0.1:23306/ruoyi-vue-pro` 幂等补齐 14 个生命周期列：`submit_idempotency_key, submitted_by, submitted_at, approval_idempotency_key, approved_by, approved_at, approval_signoff_evidence_hash, approval_opinion, rejected_by, rejected_at, reject_reason, withdrawn_by, withdrawn_at, withdraw_reason`。

GREEN: local-db-postcheck -> PASS，`information_schema.columns` 确认 14 个生命周期列全部存在；模拟原报错字段 SELECT 执行成功，`select_ok rows=0`。

GREEN: mvn -pl yudao-module-mes -Dtest=MesProEdhrReleasePrecheckContractTest -DfailIfNoTests=false test -> PASS, 3 tests。

GREEN: python -X utf8 -m pytest script\tests\test_edhr_release_precheck_api_contract.py script\tests\test_edhr_release_transaction_api_contract.py -q -> PASS, 8 passed。
