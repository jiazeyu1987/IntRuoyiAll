# NAS 表格自动同步 Test Data

## Purpose and Scope

定义开发、静态测试、后端测试和真实 E2E 所需数据。所有写入数据必须归属本任务，避免污染生产租户或无关业务记录。

## Evidence Reviewed

- ERP 同步支持类型来自 `ErpKingdeeSyncTypeEnum`。
- NAS 连接配置由 infra NAS 管理提供。
- worktree 运行端口为 `8088/48088`。

## Required Test Data

- 测试账号：具备 `mes:pro-batch-record-execution:golden-finger`，用于查看个人工作台配置页签。
- ERP 表类型：至少选择 `PRODUCT` 和 `STOCK` 中一个；若对应导出器无数据，允许生成只有表头的正式 workbook，但 run item row count 必须真实记录为 0。
- NAS 目录：任务专用相对目录，例如 `codex/20260805-profile-nas-table-auto-sync`。
- 文件名模式：`ERP自动同步_{yyyyMMdd_HHmmss}.xlsx`。
- 测试写入文件：后端生成 `nas-sync-test-<timestamp>.txt` 或等价可追踪文件。

## Reset Procedure

- 将本任务 plan disabled，保留 run 日志作为证据。
- 如需清理 NAS 测试文件，必须只清理任务专用目录下的文件，不删除共享根或其他任务目录。
- 数据库清理只能针对本任务 plan/run，需记录主键和租户。

## Data Ownership

- plan、run、run item 和 NAS 测试文件均属于 `20260805-profile-nas-table-auto-sync`。
- 不修改 admin 基线数据、生产租户数据或无关 NAS 文件。

## Test Blockers

- 无测试账号或 NAS 目录不可写时，真实写入 E2E 阻塞；静态合同和后端 fail-fast 测试仍可继续。
