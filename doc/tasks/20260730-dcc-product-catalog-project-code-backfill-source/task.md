# DCC 产品目录项目代码回填同步到芋道源码

## Task Goal

将 DCC 产品目录“项目名称”“项目代码”字段和瑛泰产品完全对应回填，同步到 `D:\ProjectPackage\IntRuoyi\IntRuoyiAll` 芋道源码仓库。

## Milestones

- [x] 核对目标仓现有 DCC 产品目录字段、迁移、前端列配置
- [x] 建立或复用 BDD/TDD 契约，确认缺口 RED
- [x] 补齐缺失的后端/前端/SQL 回填实现
- [x] 执行定向验证、迁移门禁和本地库核验
- [x] 收尾、提交并推送目标仓分支

## Expected Verification

- 迁移契约覆盖 `dcc_product_catalog.project_name/project_code` 字段和 115 条完全对应回填。
- 后端产品目录 DO/VO/API 契约包含 `projectName/projectCode`。
- 前端产品目录列表包含“项目名称”“项目代码”列并保留维护表单字段。
- 本地数据库核验：`瑛泰产品` 181 条中 115 条回填，非完全对应抽样为 0 条误填。

## Current Status

completed

## Verification Result

- 后端迁移契约、DCC Service JUnit、Controller/Service 回归均已通过。
- 前端产品目录静态契约和 `pnpm ts:check` 均已通过。
- 本地 Docker MySQL 已执行正式迁移，`瑛泰产品` 活跃行 181 条，其中完全对应回填 115 条，非完全对应样例行 8/25/29 误填数量为 0。
- Cleanup preview/apply 已通过；最终历史使用远端已有等价实现提交 `169ec7b0 feat: add DCC product catalog project fields`，本地重复实现提交 `d0ade5eb` 已在 rebase 时被跳过。

## Experience Gates

- 数据修复临时表排序规则门禁：中文临时表字段必须显式 `utf8mb4_unicode_ci`，避免 `ERROR 1267`。
- 前端静态契约隔离门禁：用产品目录专用静态合同证明列展示，不用无关全量失败替代。
- PowerShell 编排门禁：中文 SQL/Markdown 使用 UTF-8，PowerShell 不使用 `&&`。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，采用正式字段、迁移、后端契约和前端列配置。
- 是否存在临时补丁或绕过：否。
