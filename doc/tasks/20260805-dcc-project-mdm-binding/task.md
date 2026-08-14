# DCC 项目代码与 MDM 产品主数据绑定

## Task Goal

将芋道源码租户下可明确对应的 `dcc_project_code.product_master_id` 绑定到正式 `mdm_product.id`，让 QA 规程配置选择 DCC 项目代码后能够自动带出 MDM 产品主数据。

## Milestones

- [x] 核对 DCC 项目代码与 MDM 产品主数据 schema、租户范围和现有绑定状态。
- [x] 生成可审计的匹配规则与候选清单，区分唯一高置信、重复歧义和未匹配。
- [x] 仅写入唯一高置信匹配关系，保留歧义/无匹配数据不动。
- [x] 验证绑定结果、关键压力泵项目代码、租户边界和孤儿引用。

## Expected Verification

- 只读核对 `dcc_project_code.product_master_id`、`mdm_product.id/name_cn/product_code/dcc_product_code` 等字段存在。
- 记录 RED：关键 DCC 项目代码存在 MDM 同名产品但 `product_master_id` 为空。
- 记录 GREEN：事务更新唯一高置信匹配后，更新行数与候选数一致。
- 复核芋道源码租户绑定/未绑定统计、`IDI` 等关键项目代码映射、无跨租户或已删除产品引用。
- 运行真实页面 E2E：选择 `IDI` 后 DCC 项目代码返回 `productMasterId=14`，QA 状态接口成功，页面无写请求和前端错误。

## Current Status

ready_for_closeout

实现和数据验证已完成；尚未执行任务收尾 cleanup、提交和推送。

## Completed Work

- 芋道源码租户 `dcc_project_code` 共 119 条，其中 51 条已绑定到同租户启用状态的 `mdm_product`。
- 本次仅采用等值型高置信规则：29 条完全同名、7 条去注册/地区括号后缀后同名、15 条去“一次性使用”通用前缀后同名。
- 保留 68 条未绑定数据不动，原因是缺少等值主数据、名称过短或可能存在产品族/型号歧义。
- 压力泵相关项目代码已绑定：`ID`、`IDE`、`IDE(CE)`、`IDE(FDA)`、`IDI`、`IDPR`。
- 已修正并通过 `e2e:qa-regulation:dcc-status:real`，E2E 现在按 Element Plus 表单输入值断言 IDI 规程名称。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；不做默认产品、不按模糊包含关系强绑、不吞 SQL 错误。
- `是否从根因和长期维护角度解决`：是；直接修复 DCC 项目代码到 MDM 产品主数据的正式绑定字段。
- `是否存在临时补丁或绕过`：否；仅更新正式绑定关系，疑似匹配保留给人工确认。

## Applicable Gates

- DCC 项目代码 MDM 产品建档绑定门禁：写入前同时核对 DCC 项目代码表、MDM 产品主数据和 `dcc_project_code.product_master_id` 正式绑定字段；缺字段、跨租户、禁用/删除产品或只能靠前端推断时必须停止。
- 数据修复排序规则门禁：如使用临时表或中文 JOIN，必须显式确认字符列排序规则；本任务优先使用 UTF-8 脚本读取并用主键精确更新，避免中文临时表排序规则混用。
