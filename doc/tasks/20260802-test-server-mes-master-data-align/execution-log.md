# Execution Log

## User Intent

用户要求检查后将当前测试服务器的工作站、工序、工艺路线实际数据修复为与本机一致。

## BDD

- BDD: 测试服 MES 主数据对齐本机 -> Given 本机与测试服均为芋道源码租户 tenant_id=1, When 对比工作站、工序、工艺路线、路线工序有效数据, Then 对齐后缺失、多余、字段差异均为 0。
- BDD: 测试服数据修复可回滚 -> Given 修复会修改测试服数据库, When 执行同步前, Then 必须创建受影响表的备份表并记录备份表名。

## Commands And Evidence

- 已读取 `docs/server-access.md`、`docs/database-rules.md`、`docs/powershell-encoding.md`、`docs/release-backup-restore.md`、`docs/task-closeout-rules.md`。
- 已读取 `database-schema-delivery` 技能及 `references/database-contract.md`。
- RED: 本机/测试服差异校验脚本 -> FAIL, 修复前测试服缺 118 个有效工作站、缺 1 条有效工艺路线，路线工序绑定差异为 missing 40 / extra 26。
- 数据备份: 测试服创建 `zz_bak_ws_20260802_1530` 124 行、`zz_bak_proc_20260802_1530` 65 行、`zz_bak_route_20260802_1530` 34 行、`zz_bak_rp_20260802_1530` 190 行。
- 数据同步: 从本机导入 staging 并事务性 upsert 测试服有效数据，随后软删除测试服有效范围外多余行；staging 表已清理。
- GREEN: 本机/测试服差异校验脚本 -> PASS, 工作站 144/144、工序 65/65、工艺路线 4/4、有效路线工序绑定 77/77，缺失/多余均为 0。
- Database evidence validator: `python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc/tasks/20260802-test-server-mes-master-data-align/database-schema-evidence.md` -> PASS。
- 经验沉淀: 已将“路线工序对齐需区分页面可见有效路线工序与挂在已删除路线下的孤儿历史行”合并到 `docs/database-rules.md` 的 MES 三页签同步门禁，并在 `docs/experience-index.md` 增加关键词。
- 文档检查: `git diff --check -- docs/database-rules.md docs/experience-index.md doc/tasks/20260802-test-server-mes-master-data-align` -> PASS，仅提示 CRLF 工作区转换 warning。

## Milestones

- 任务目录与初始任务文档已创建。
- 修复前 RED 已记录。
- 测试服备份表已创建并保留。
- 测试服 MES 有效工作站、工序、工艺路线、有效路线工序绑定已对齐本机。
- 修复后 GREEN 已通过。
- 数据库证据校验已通过。
- 长期经验已合并到既有门禁文档。

## Blockers

- 数据修复与复验暂无阻塞。
- 仓库收尾提交未执行：当前工作区存在其它任务的未提交改动与 Git status 目录读取警告，本任务不混入 unrelated changes。
