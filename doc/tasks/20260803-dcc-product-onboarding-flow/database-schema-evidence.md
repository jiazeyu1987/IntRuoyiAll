# Database Schema Evidence

## Data

- Data change goal: 为 DCC 项目代码补正式 MDM 产品主数据绑定，并记录产品建档申请、审批人、审批时间和审批后生成的项目代码。
- Affected entities: `dcc_project_code`, `dcc_product_onboarding_request`, DCC 测试 schema fixture。
- Persistence model: `DccProjectCodeDO.productMasterId` and `DccProductOnboardingRequestDO`。

## Migration

- Migration: 新增 `sql/mysql/20260803_dcc_product_onboarding_flow.sql`。
- Base schema: 更新 `sql/mysql/20260513_dcc_base_schema.sql`，包含 `dcc_project_code.product_master_id` 和 `dcc_product_onboarding_request`。
- Test fixture: 更新 `yudao-module-dcc/src/test/resources/sql/create_tables.sql`，保证 DCC schema 测试可验证相同字段/表。
- Indexes: `idx_dcc_project_code_product_master_id`, `idx_dcc_product_onboarding_status`, `idx_dcc_product_onboarding_product`, `idx_dcc_product_onboarding_generated`。
- Constraints: `uk_dcc_product_onboarding_pending_project` 阻止同租户同目标项目代码重复待审批申请。

## Safety

- Safety: 本任务仅新增字段、表和索引，不执行数据删除、截断或强制回填。
- Existing data: `product_master_id` 允许为空，旧 DCC 项目代码和历史受控文件不被强制改写。
- Runtime safety: 受控文件提交只在项目代码正式绑定 MDM 后读取 MDM 产品；旧未绑定项目代码保持历史项目代码/项目名来源。

## Rollback

- Rollback: 如需回滚，先下线依赖建档申请 API 和前端入口，再删除新增索引、`dcc_product_onboarding_request` 表和 `dcc_project_code.product_master_id` 字段。
- Recovery: 由于未执行破坏性数据迁移，回滚前保留申请表和项目代码表备份即可恢复审批与绑定审计记录。

## BDD

- BDD: 审批通过生成正式 DCC 项目代码并绑定 MDM -> Given 产品建档申请处于待审批状态 / When 审批人审批通过 / Then DCC 项目代码表必须持久化 `product_master_id`。
- BDD: 重复 DCC 项目代码必须拒绝 -> Given 已存在同租户同目标项目代码的待审批申请 / When 再次提交申请 / Then schema 和服务必须阻止重复待审批记录。

## RED

- RED: `mvn -pl yudao-module-dcc -am "-Dtest=DccBaseSchemaTest#mysqlSchemaShouldSupportProductOnboardingAndProjectMdmBinding" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: 实现前基础 schema/test fixture 缺少 `dcc_project_code.product_master_id` 和 `dcc_product_onboarding_request`。

## GREEN

- GREEN: `mvn -pl yudao-module-dcc -am "-Dtest=DccBaseSchemaTest#mysqlSchemaShouldSupportProductOnboardingAndProjectMdmBinding" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 1, Failures: 0, Errors: 0, Skipped: 0。

## Verification

- Verification: 聚焦 schema 测试断言运行 schema 与测试 fixture 都包含 `dcc_project_code.product_master_id` 和 `dcc_product_onboarding_request`。
- Verification: 聚焦 schema 测试断言申请表包含产品、项目、状态、申请人、审批人、审批时间、生成项目代码和驳回原因字段。
- Verification: 聚焦 schema 测试断言状态索引和待审批唯一约束存在。

## Blockers

- Blockers: 未连接真实数据库执行 migration up/down；本轮只做 SQL 文件和测试 fixture 的静态/单元 schema 契约验证，未触碰远程或本机业务库。
