# Database Schema Evidence

## Data Change Goal And Affected Entities

- Goal: 在本地验证库为 QA 检验项目持久化 `inspection_tool` 与 `sampling_plan_text` 两项正式原文，支持一线 PQC 详情弹窗真实页面验收。
- Entity: `mes_qa_inspection_regulation_item`。
- Scope: 仅本机 Docker MySQL `ruoyi-vue-pro`；不访问或修改测试服、正式服、备用服。

## Engine And Migration Tool

- Engine: MySQL 8.0.39。
- Migration: `IntRuoyiBackend/sql/mysql/20260809_mes_qa_inspection_item_display_fields.sql`，通过 UTF-8 stdin 送入 `int-ruoyi-mysql` 容器中的 MySQL CLI。

## Schema Change

- 新增 nullable `varchar(512)` 列 `inspection_tool`。
- 新增 nullable `varchar(512)` 列 `sampling_plan_text`。
- 不执行 fixture、seed、DML 或历史数据回填。

## Data Safety Analysis

- 迁移前目标表共 166 行，两个目标列均不存在。
- 正式依赖的 5 个 QA 项目字段和设备关联表均存在。
- 迁移通过幂等存储过程逐列检查后执行 `ALTER TABLE ADD COLUMN`；现有业务列和值不变。
- 历史原文无法从结构化数量、比例或设备选项无损推断，因此保持 NULL，并由正式 QA 页面重新保存/发布任务自有项目。

## Rollback Or Recovery Plan

- 迁移失败时可幂等重跑，补齐尚未创建的列。
- 需要回滚时，必须先只读确认两个字段的非空值计数均为 0，再执行精确 `ALTER TABLE ... DROP COLUMN`。
- 若任一字段已有非空业务值，回滚必须阻塞并保留现场，禁止丢弃数据。

## BDD Scenarios

BDD: 本地 QA 项目展示原文字段迁移 -> Given 正式依赖 schema 已存在且两个目标列缺失 When 执行正式幂等迁移 Then 两个 nullable varchar(512) 列存在、166 行历史项目保持原值且新增列为空。

BDD: 迁移后正式页面保存 QA 原文 -> Given 本地测试租户和任务自有 QA 项目可通过真实页面维护 When 用户保存并发布抽样方案与检验器具及设备 Then 发布项目在一线 PQC 弹窗中按对应区域显示相同原文。

## RED Evidence

- RED: `information_schema.COLUMNS` 查询 -> 0 行，expected reason: 两个正式目标列尚未应用。

## GREEN Evidence

- GREEN: 正式迁移首次执行退出码 0。
- GREEN: 两个目标列均为 `varchar(512)`、nullable、默认 NULL。
- GREEN: 历史行数保持 166，两个字段非空计数均为 0。
- GREEN: 正式迁移幂等重跑退出码 0，两列均报告 already exists。
- GREEN: migration pytest 2 passed；release migration policy gate passed，migrationCount=457。
- BLOCKED: schema 已满足；真实页面验证缺少经确认租户内可安全写入的完整 QA 业务数据链。

## Migration Verification

- 列类型与 nullable：PASS。
- 历史非空计数与原行数：PASS。
- 幂等重跑：PASS。
- migration pytest：PASS，2 passed。
- policy gate：PASS，目标迁移依赖和风险元数据有效。

## Blockers

- 真实登录已确认“测试租户/瑛泰管理员”可进入 QA 页面，但该租户 QA 规程数、QA 项目数和活跃订单数均为 0，且没有可用于本任务的完整 DCC→MES 产品→路线→活跃订单链。
- 截图无法唯一证明目标租户；继续前需由用户确认是在测试租户创建完整任务自有数据链，还是另行指定允许写入的租户和业务对象。当前 QA 业务写入数为 0，不切换到 admin 基线或其它租户现有规程。
