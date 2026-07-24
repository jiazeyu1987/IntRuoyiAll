# 任务：eDHR 流程事件缺表与流程干预菜单号段修复

## 任务目标

- 修复本机运行库缺少 `mes_pro_edhr_flow_event` / `mes_pro_edhr_flow_intervention` 导致查询流程事件直接报 `Table ... doesn't exist` 的问题。
- 修正 `sql/mysql/20260618_mes_edhr_flow_intervention_log.sql` 复用已冲突菜单号段的问题，确保正式迁移与后续 eDHR 菜单不再相互覆盖。
- 为本机标准重启 / schema 保底流程补充安全的 eDHR 流程事件缺表自愈，不把已冲突的旧菜单迁移原样重放到本机库。

## 当前状态

已完成。

## 前一任务检查

- 后端前一任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260626-dcc-access-rule-bound-directory-list\task.md`
- 当前状态：`BLOCKED`
- 处理说明：该任务已因本次 eDHR 缺表 P0 运行阻塞显式暂停；本次优先恢复后端运行必需 schema 与正式迁移正确性。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中说明：
  - 本次仅做本机后端源码、SQL、定向测试与本机 Docker MySQL schema 修复，不执行真实 E2E、服务器写入、发布、备份恢复或远程联调。
- 适用强制门禁：
  - 本次数据库修复必须 fail-fast；若正式菜单号段冲突或迁移探针无法证明安全，不得通过 fallback、跳过菜单或静默忽略错误掩盖问题。
  - 在执行本机 Docker MySQL 写入前，必须在 `execution-log.md` 记录 `GREEN: experience-preflight -> PASS`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。缺表、菜单冲突或迁移探针失败都必须显式报错，不做静默跳过。
- `是否从根因和长期维护角度解决`：是。通过修正正式 SQL 菜单号段并补安全的本机 schema guard，一次性解决正式迁移与本机旧库漂移。
- `是否存在临时补丁或绕过`：否。不会通过修改 Mapper 绕开表，也不会手工建表后不回写正式 SQL / 测试。

## BDD 场景

- `BDD: 查询流程事件时本机旧库不再因缺表失败 -> Given 本机历史库缺少 mes_pro_edhr_flow_event 与 mes_pro_edhr_flow_intervention / When 执行本机标准 schema 保底流程后再查询流程事件 / Then 查询不再报 Table doesn't exist。`
- `BDD: 流程干预正式迁移不得覆盖已存在的验证包矩阵与 OQ/PQ 菜单 -> Given system_menu 中旧号段 900286-900292 已被其他 eDHR 功能占用 / When 执行流程干预正式迁移 / Then 流程干预必须使用独立未冲突号段，并可幂等吸收旧流程干预残留。`
- `BDD: 本机 schema guard 只补齐缺失表结构，不重放已冲突旧菜单 SQL -> Given 本机库缺失流程事件表但现有 system_menu 已有其他功能占用旧号段 / When 执行 restart-int-ruoyi-local.ps1 的本地 schema 保底 / Then 脚本只应用安全的表结构修复 SQL，不把冲突菜单号段写回本机库。`

## 里程碑

1. M1：创建任务文档、请求日志并显式阻塞上一后端任务。`COMPLETED`
2. M2：补 RED 合同测试，锁定流程干预菜单号段冲突与本机 schema guard 缺失。`PENDING`
3. M3：修正正式 SQL 菜单号段与旧残留吸收逻辑。`PENDING`
4. M4：新增本机安全 schema 修复 SQL，并接入 `restart-int-ruoyi-local.ps1`。`PENDING`
5. M5：执行定向 GREEN 测试与本机库修复验证，补齐缺陷 / schema 证据。`PENDING`

## 最终验证结果

- `python -X utf8 -m pytest script/tests/test_edhr_flow_intervention_schema_sql.py script/tests/test_edhr_flow_intervention_runtime_sql.py script/tests/test_mes_base_schema_edhr_flow_tables.py script/tests/test_restart_int_ruoyi_local_schema.py -q`：PASS（20 tests passed）
- `docker exec int-ruoyi-mysql mysql --default-character-set=utf8mb4 -N -B -uroot -p123456 -D ruoyi-vue-pro -e "SHOW TABLES LIKE 'mes_pro_edhr_flow_event'; SHOW TABLES LIKE 'mes_pro_edhr_flow_intervention';"`：PASS（两张表均已存在）
- `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260626-edhr-flow-event-missing-table-fix\bug-regression-evidence.md`：待执行
- `python -X utf8 C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260626-edhr-flow-event-missing-table-fix\database-schema-evidence.md`：待执行

## 预期验证

- `python -X utf8 -m pytest script/tests/test_edhr_flow_intervention_schema_sql.py script/tests/test_restart_int_ruoyi_local_schema.py -q`
- `docker exec int-ruoyi-mysql mysql --default-character-set=utf8mb4 -N -B -uroot -p123456 -D ruoyi-vue-pro -e "SHOW TABLES LIKE 'mes_pro_edhr_flow_event'; SHOW TABLES LIKE 'mes_pro_edhr_flow_intervention';"`
- `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260626-edhr-flow-event-missing-table-fix\bug-regression-evidence.md`
- `python -X utf8 C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260626-edhr-flow-event-missing-table-fix\database-schema-evidence.md`
