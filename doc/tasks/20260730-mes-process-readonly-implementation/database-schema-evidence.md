# Database Schema Evidence

## Data Change Goal

新增只读 MES 工序目录和设备关联，种子范围仅为 `二代压力泵` 工作表；执行工序关联现有正式工序，设备关联现有设备。

## Engine And Migration

- Engine: MySQL 8 compatible.
- Migration: repository release SQL under `IntRuoyiBackend/sql/mysql/`.

## Data Safety

- 仅新增表、索引、菜单和权限，不删除或改写现有报工、工序、设备、路线或批记录数据。
- 设备和执行工序匹配缺失或歧义时 fail fast。
- 回滚为删除新增菜单、权限和新增目录表，不触碰现有主数据。

## BDD Scenarios

- 二代压力泵有效工序行生成目录。
- 多设备拆成多条设备关联。
- 无工序名称的孤立产能不生成目录。
- 缺设备或执行工序映射时迁移失败。

## RED

Pending.

## GREEN

Pending.

## Migration Verification

Pending.

## Blockers

None at task start.

