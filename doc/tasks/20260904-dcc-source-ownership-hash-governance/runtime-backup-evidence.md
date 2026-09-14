# DCC 治理运行态备份与恢复证据

## Scope And Inventory

- 环境：本机测试环境 `int-ruoyi-mysql` / MySQL 8.0.39、MinIO `docker-minio-1`。
- 备份范围：`dcc_controlled_file_master`、`dcc_controlled_file`、`dcc_controlled_file_source_ownership`、`dcc_controlled_file_source_migration`、签名、路线快照、关联文件、分发、培训、打印和访问日志，共 11 张表。
- 备份文件：`runtime-backup/dcc-source-governance-before.sql`，约 27.9 MB，SHA-256 `7F02AA4828DB8CE944BED4D047BF32FE628475F99580A3A4FCB1979AA6C83CFC`。
- 对象存储验证对象：任务专属 smoke 对象，未备份或覆盖其它业务对象。

## Backup And Retention

- 本次为一次性治理维护窗口备份，至少保留到本任务验收和回滚窗口结束。
- 备份不包含数据库密码、MinIO 密钥或访问令牌；凭据只通过本机运行环境读取。

## RTO And RPO

- 本次测试目标：RPO 为迁移前快照时点，RTO 目标为本机隔离库可恢复并完成 checksum 对账。
- 正式全量治理的 RTO/RPO 尚未确认，必须在正式维护窗口前由业务负责人确认；因此不宣称生产恢复就绪。

## Restore And Verification

- 备份恢复到隔离库 `dcc_source_governance_restore_verify` 成功，11 张表可读。
- `dcc_controlled_file`、ownership、签名、路线快照、关联、分发、培训、打印 checksum 与源库一致。
- 访问日志历史行缺失 0、变更 0，仅存在两条本次通用直链拒绝新增审计，符合 append-only 预期。
- 隔离库验证完成后已删除，未触碰源库业务表。

## Blockers And Disaster Scenarios

- 数据库事务失败：由 DCC 后端维护人负责停止批次、保留失败状态并从数据库恢复点恢复。
- 对象存储副本失败：由 DCC 后端维护人负责按副本 ID/对象 key 清理；清理失败必须升级为 blocker。
- 运行 Jar 启动失败：由本机运行维护人按 `docs/local-runtime.md` 恢复上一份可执行 Jar；不得切换端口或数据源。
- 历史证据不一致：由文控/数据治理负责人决定人工处理，不得自动猜测替代。

## Config And External Dependencies

- 依赖 MySQL、MinIO、现有 `FileService` 和 DCC ownership/migration 表。
- 当前运行端口为 `48081`，测试数据源和对象存储均为本机测试配置；未访问远程服务器。
- 本证据不记录任何明文凭据。

## Release Impact And Next Actions

- additive schema 已部署，业务历史表未被全量回写；smoke 数据和对象已清理。
- 全量 Windchill 迁移仍为 NO-GO：最新只读盘点 `AUTO_MAP=0`，ownership/hash blocker 18,065，平台 ACTIVE 漂移 17,864。
- 下一步需先处理 blocker，再在新的维护窗口重新备份、生成确认清单、逐批执行和恢复演练。
