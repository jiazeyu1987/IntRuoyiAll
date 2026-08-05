# Execution Log

## User Intent

- 用户要求先给 DCC 项目代码和 MDM 产品主数据做一次对应，并希望按“多数已有对应关系”的事实找出合适绑定方式。

## BDD

- BDD: QA 规程项目代码自动绑定产品 -> Given 芋道源码租户存在 DCC 项目代码且 MDM 产品主数据中有唯一同名产品 When 用户选择该项目代码 Then `product_master_id` 应绑定到该 MDM 产品，QA 规程配置可获得产品主数据。
- BDD: 歧义项目不静默绑定 -> Given 一个 DCC 项目代码可能对应多个 MDM 产品或只有模糊近似名称 When 执行批量对应 Then 系统不应写入绑定，必须在候选清单中保留待确认。

## Evidence

- 规则已读取：`docs/database-rules.md`、`docs/powershell-encoding.md`、`docs/task-closeout-rules.md`、`database-schema-delivery` 技能和证据契约。
- schema 核对：`dcc_project_code` 存在 `id/product_master_id/project_name/project_code/tenant_id/deleted`；`mdm_product` 存在 `id/product_code/dcc_product_code/name_cn/status/tenant_id/deleted`。
- 排序规则核对：`dcc_project_code.project_code/project_name` 与 `mdm_product.product_code/dcc_product_code/name_cn/name_en` 均为 `utf8mb4_unicode_ci`。
- 初始状态：芋道源码租户 `119` 条项目代码，`0` 条已绑定，`119` 条未绑定；测试租户 `133` 条项目代码，`7` 条已绑定，`126` 条未绑定。
- RED: `python -X utf8 -` 只读断言 `IDI` 应有 MDM 精确同名绑定 -> FAIL，原因：`IDI / 按压式球囊扩充压力泵` 对应 MDM `14 / INT-15`，但 `product_master_id` 为空。
- 候选规则：只允许规范化完全等值、去注册/地区括号后缀后等值、去“一次性使用”通用前缀后等值；不使用模糊分数、包含关系、默认产品或跨租户推断。
- 写入事务：临时表候选数 `51`，事务内校验 DCC 行当前为空、MDM 产品同租户/未删除/启用后更新，实际 `updated_count=51`。
- GREEN: `python -X utf8 -` 复核脚本 -> PASS；芋道源码 `119/51/68`，测试租户保持 `133/7/126`，无孤儿绑定，无非启用产品绑定。
- 压力泵复核：`IDI -> INT-15 / 按压式球囊扩充压力泵`，`IDPR -> INT-14`，`ID -> INT-12`，`IDE/IDE(CE)/IDE(FDA) -> INT-13`。

## Milestone Updates

- completed: schema、排序规则、租户范围和现有绑定状态核对完成。
- completed: 生成唯一高置信候选 51 条，保留 68 条未绑定待业务确认。
- completed: 事务更新 51 条 `dcc_project_code.product_master_id`。
- completed: 绑定统计、关键压力泵项目、跨租户/删除产品引用和产品启用状态复核通过。

## Blockers

- 本次数据修复已完成；仍有 68 条项目代码因无法通过等值规则证明唯一对应，未自动绑定。
