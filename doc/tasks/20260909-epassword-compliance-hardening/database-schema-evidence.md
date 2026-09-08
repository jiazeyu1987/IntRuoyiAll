# Database Schema Evidence

## Data Change Goal and Affected Entities

- 本次未修改数据库表结构、索引、迁移脚本或持久化字段。
- 复用现有正式签名证据字段：签名时间、认证方式、密码校验状态、流程实例、任务 ID、内容哈希和审计哈希。

## Database Engine and Migration Tool

- 数据库引擎：MySQL。
- 迁移工具：项目 SQL 脚本。
- 本次无迁移。

## Schema, Migration, Fixture, Seed, Index, or Constraint Changes

- 无。

## Data Safety Analysis

- 不删除、不回填、不改写历史签名数据。
- 历史 `USER_SELECTED` 数据保留展示，但标识为历史停用口径。

## Rollback or Recovery Plan

- 回滚代码即可恢复旧前端输入和旧服务行为；无数据库回滚步骤。

## BDD Scenarios

- BDD: 不变更数据库结构完成签名时间合规整改 -> Given 现有签名表已包含签名时间和认证方式字段, When 禁止正式签名提交人工时间, Then 无需新增迁移即可保留证据能力。

## RED/GREEN/Migration Verification

- RED: 不适用，本次无数据库迁移；RED/GREEN 行为测试见 `backend-api-evidence.md` 与 `frontend-feature-evidence.md`。
- GREEN: 不适用，本次无数据库迁移；代码验证确认无需新增字段即可完成约束。
- Migration verification: 不适用，本次无迁移。

## Verification

数据库迁移验证不适用；本次未修改 schema。

## Blockers

- 无数据库层 blocker。
