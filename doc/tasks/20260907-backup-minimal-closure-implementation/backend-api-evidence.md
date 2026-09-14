# Backend API Evidence

## Contract

- `POST /infra/backup-plan/backup-now?backupKind=FULL|INCREMENTAL`：显式备份类型，非法或缺失直接失败。
- `GET /infra/backup-plan/evidence/export`：后端生成 ZIP，权限为 `system:backup-plan:evidence-export`，前端不计算 PASS。
- 历史响应暴露 `backupKind`、`baseBackupId`、`parentBackupId`、演练状态和不可恢复原因。

## Safety

- 备份动作使用全局非阻塞 mutex，禁止全量、增量和演练并发写仓库。
- FULL/INCREMENTAL 进入停服窗口后验证容器停止且活动事务为 0；`finally` 恢复服务并验证 health。
- 恢复演练在重建运行时前校验 FULL dump、完整 binlog 链和对象仓库 SHA-256。
- 演练报告先写入，manifest 再原子标记 PASSED；两者缺一不可成为 `RECOVERABLE`。

## Evidence Export

- 固定 9 个文件，含原始 manifest 字节 SHA-256、payload 路径/大小/hash 索引和 evidence manifest 自校验。
- BLOCKED 状态仍可导出缺失项；操作自由文本摘要不进入 ZIP，避免错误消息夹带 secret。
- API 使用独立导出权限和 `ApiAccessLog(EXPORT)`。

## Verification

- Java 定向测试 46 项通过。
- `mvn.cmd -pl yudao-module-infra -am '-DskipTests' package -q` 通过。
