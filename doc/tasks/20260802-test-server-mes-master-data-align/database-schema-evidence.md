# Database Schema Evidence

## Data Change Goal And Affected Entities

将测试服务器 `172.30.30.58` 的芋道源码租户 `tenant_id=1` 中以下有效数据对齐本机：

- `mes_md_workstation`
- `mes_pro_process`
- `mes_pro_route`
- `mes_pro_route_process`

## Database Engine And Migration Tool

- Engine: MySQL 8 in Docker container.
- Migration tool: 一次性受控 SQL 数据修复，不新增 release migration。

## Schema, Migration, Fixture, Seed, Index, Or Constraint Changes

- 不修改 schema、索引、约束或生产代码。
- 仅同步测试服当前有效主数据。

## Data Safety Analysis

- 目标环境为测试服务器，用户已要求修复为一致。
- 修复前创建测试服备份表，备份受影响表全量当前行。
- 不操作正式服、备用服、共享存储、发布包或服务进程。

## Rollback Or Recovery Plan

- 使用本次创建的测试服备份表恢复对应表。
- 如修复后验证不通过，停止并保留备份表与失败证据。

## BDD Scenarios

- BDD: 测试服 MES 主数据对齐本机 -> Given 本机与测试服均为芋道源码租户 tenant_id=1, When 对比工作站、工序、工艺路线、路线工序有效数据, Then 对齐后缺失、多余、字段差异均为 0。
- BDD: 测试服数据修复可回滚 -> Given 修复会修改测试服数据库, When 执行同步前, Then 必须创建受影响表的备份表并记录备份表名。

## RED Command And Expected Failure

- RED: 本机/测试服差异校验脚本 -> FAIL, 修复前测试服缺 118 个有效工作站、缺 1 条有效工艺路线，路线工序绑定差异为 missing 40 / extra 26。

## GREEN Command And Passing Result

- GREEN: 本机/测试服差异校验脚本 -> PASS, 工作站 144/144、工序 65/65、工艺路线 4/4、有效路线工序绑定 77/77，缺失/多余均为 0。

## Migration Verification

- Verification: 不修改 schema；通过 MySQL 真实库只读复验确认测试服有效工作站、工序、工艺路线、有效路线工序绑定与本机一致。
- Backup verification: 测试服保留 `zz_bak_ws_20260802_1530`、`zz_bak_proc_20260802_1530`、`zz_bak_route_20260802_1530`、`zz_bak_rp_20260802_1530`。
- Cleanup verification: staging 表 `zz_stage_*_20260802_1530` 已清理。

## Blockers

- 数据修复暂无阻塞。
- 仓库收尾提交未执行：当前工作区存在其它任务的未提交改动，本任务不混入 unrelated changes。
