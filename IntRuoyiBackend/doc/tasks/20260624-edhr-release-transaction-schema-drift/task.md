# 任务：eDHR放行事务表生命周期列漂移修复

## 任务目标

修复运行库 `mes_pro_edhr_release_transaction` 缺少 `submit_idempotency_key` 等生命周期列导致 MyBatis 查询失败的问题，确保新建库和已存在旧表的库都能与 `MesProEdhrReleaseTransactionDO` 字段保持一致。

## 里程碑

- [x] M1：定位报错字段、DO、Mapper 与 SQL migration 的差异。
- [x] M2：补充 RED schema 回归测试，覆盖基础建表与修复 SQL。
- [x] M3：补齐基础建表 SQL 与幂等修复 SQL。
- [x] M4：在本机数据库执行列修复并只读确认字段存在。
- [x] M5：运行 schema/API 回归测试并记录结果。

## 预期验证

- `python -X utf8 -m pytest script/tests/test_edhr_release_precheck_schema_sql.py script/tests/test_edhr_release_transaction_schema_sql.py -q`
- 本机 MySQL `information_schema.columns` 只读确认 lifecycle 列存在。
- 原查询不再因 `Unknown column 'submit_idempotency_key'` 失败。

## 当前状态

已完成；本机 Docker MySQL `127.0.0.1:23306/ruoyi-vue-pro` 已补齐生命周期列。

## 经验门禁

- `docs/experience-index.md`：涉及数据库 schema 修改和本机库写入前，必须在 `execution-log.md` 记录 `experience-preflight`。
- 本次仅操作本机 `127.0.0.1:3306/ruoyi-vue-pro`，不操作正式服或测试服。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，补齐表结构与 DO 数据契约，不删除查询字段、不吞 SQL 异常。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 旧表缺生命周期列时可幂等修复 -> Given 运行库已存在 mes_pro_edhr_release_transaction 但缺少 submit_idempotency_key 等生命周期列 / When 执行修复 SQL / Then 表结构补齐 DO 查询所需列，Mapper 查询不再触发 Unknown column。`
- `BDD: 新建库基础表结构与当前 DO 一致 -> Given 新环境从 eDHR 放行基础建表 SQL 初始化 / When 后端按 MesProEdhrReleaseTransactionDO 查询事务 / Then SELECT 字段均存在，不依赖后续菜单类 SQL 才能避免基础查询失败。`
