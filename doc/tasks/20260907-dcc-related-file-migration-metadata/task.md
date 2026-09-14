# DCC 关联文件迁移元数据修复

## Task Goal

为既有 `20260903_dcc_controlled_file_related_file.sql` 补齐正式发布迁移元数据，使全 SQL 根目录门禁能够识别该迁移，同时不改变表结构和业务行为。

## Milestones

- M1 复现缺少迁移元数据的门禁失败
- M2 先补静态回归测试，再添加最小元数据修复
- M3 运行定向测试和全 SQL 根目录门禁

## Expected Verification

- 新增静态合同先 RED 后 GREEN
- 迁移策略门禁不再因该文件缺少 metadata 失败
- `git diff --check` 通过

## Current Status

completed

实现提交已推送，task-closeout-cleanup preview/apply 通过，任务完成。

## 设计约束检查

- 仅增加一行 release-migration metadata，不修改建表 SQL。
- 迁移类型为 schema，风险等级为 medium，依赖现有 DCC 基础 schema。
- 不执行数据库写入，不重启服务，不处理任何业务数据。
- 未获当轮 Git 授权，不提交或推送。

## Cleanup Keep

- doc/tasks/20260907-dcc-related-file-migration-metadata/task.md
- doc/tasks/20260907-dcc-related-file-migration-metadata/execution-log.md
- doc/tasks/20260907-dcc-related-file-migration-metadata/verification-report.md
