# 20260705-showroom-target-market-migration-dependency-fix

## 任务目标

修复 `20260704_showroom_product_target_market_text.sql` 的 `release-migration dependsOn` 引用了不存在 migrationId 的发布门禁阻塞，使 IntRuoyi code-only 发布前 migration policy gate 可通过。

## 上一任务检查

- 最近任务目录已检查；本次只处理发布前门禁暴露的展厅 target_market SQL 元数据问题。
- 主工作区存在无关脏改：`yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecordreport/MesProBatchRecordJingxiTableStructureVerificationTest.java`，本任务不暂存、不修改、不提交。

## 经验门禁

- 新增 SQL 的 `dependsOn` 必须引用真实存在的 SQL 文件 stem / migrationId，不得凭模块名或历史记忆编造依赖。
- 修复后必须重跑 `run-release-migration-policy-gate.py`，再提交后重建发布 worktree；不得把未提交修复混入发布包。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，修复 release-migration 元数据源头并更新契约测试。
- 是否存在临时补丁或绕过：否。

## BDD 场景

- Given 发布前 migration policy gate 扫描 `sql/mysql`；When SQL 声明 `dependsOn`；Then 每个依赖必须能在同一发布扫描范围内找到真实 migrationId。
- Given `showroom_product_revision.target_market` 字段需要扩展为 `text`；When 执行发布 SQL 契约测试；Then SQL 元数据应引用真实存在的前置 migrationId，并保留非破坏性字段扩展语义。

## 里程碑

1. 记录 RED：migration policy gate 因缺失 dependsOn 失败。
2. 修复 SQL `dependsOn` 与对应契约测试。
3. 重跑 targeted tests 与 migration policy gate。
4. 提交本任务直接相关改动。

## 当前状态

- 状态：已完成。
- 已完成：任务文档初始化、失败原因定位、SQL 元数据修复、契约测试与 migration policy gate 验证。
- 当前阻塞：无。

## 预期验证

- `python -X utf8 -m pytest script/tests/test_showroom_product_target_market_release_sql.py`
- `python -X utf8 script/release/run-release-migration-policy-gate.py --sql-root sql/mysql`

## 最终验证

- PASS：`python -X utf8 -m pytest script\tests\test_showroom_product_target_market_release_sql.py`，1 passed。
- PASS：`python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql`，240 migrations scanned，status=passed。

## 完成状态

- 当前状态：Completed
- 完成时间：2026-07-05 00:14:00 +08:00
