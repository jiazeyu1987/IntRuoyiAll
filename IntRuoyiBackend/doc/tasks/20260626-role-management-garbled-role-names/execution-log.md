# Execution Log：角色管理页乱码角色名回归修复（后端）

BDD: 乱码角色修复 SQL 必须恢复正式名称 -> Given SQL 修复文件执行于含损坏角色数据的库 / When 更新目标角色记录 / Then 这些角色名称恢复为正式中文值。
BDD: 修复 SQL 必须保持最小范围 -> Given 运行库存在大量正常角色 / When 执行修复 SQL / Then 仅命中明确列出的角色 ID，不修改其他 system_role 记录。

INFO: task-created -> 后端任务文档已创建，准备补 SQL RED 契约与正式修复脚本。
RED: `python -X utf8 -m pytest script/tests/test_role_name_garbled_repair_sql.py -q` -> FAIL，正式修复 SQL `20260626_role_name_garbled_repair.sql` 不存在。
GREEN: `apply_patch` -> PASS，已新增 `sql/mysql/20260626_role_name_garbled_repair.sql`，以 `CONVERT(0x... USING utf8mb4)` 方式最小修复 `910208/910209/910234/910235/910236/910237`。
GREEN: experience-preflight -> PASS，数据库写入前已完成 UTF-8/十六进制 SQL 安全校验与目标记录范围复核，可执行本机最小范围数据修复。
GREEN: `python -X utf8 -m pytest script/tests/test_role_name_garbled_repair_sql.py -q` -> PASS，SQL 契约确认正式修复文件存在且包含目标记录与中文十六进制字面量。
GREEN: local-db-repair -> PASS，正式修复 SQL 已执行到本机 `int-ruoyi-mysql / ruoyi-vue-pro`，未扩散到无关角色。
GREEN: `docker exec int-ruoyi-mysql mysql --default-character-set=utf8mb4 -uroot -p123456 -D ruoyi-vue-pro -e "SELECT id, tenant_id, code, name, remark FROM system_role WHERE id IN (910208,910209,910234,910235,910236,910237) ORDER BY id;"` -> PASS，6 条目标角色回查结果均恢复为预期中文与正式 code。
