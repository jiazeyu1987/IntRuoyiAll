# 任务：调整展厅生成产品主数据映射规则

## 任务目标

按用户最新确认的规则，调整展厅“产品管理”列表生成产品主数据的正式映射逻辑，并确保展厅与 DCC 后续都只以产品主数据 `id` 作为稳定关联键。产品主数据 `product_code` 后续允许统一变更，不能作为展厅或 DCC 的长期绑定依据。

## Previous Task Check

- 上一个后端任务：`doc/tasks/20260607-showroom-to-product-master-seed/task.md`
- 状态：`completed`
- 处理：上一任务已完成首版生成记录；本任务在其基础上调整规则并重映射真实数据。

## 映射规则

- `name_cn`：来自展厅产品列表的中文名称。
- `name_en`：来自展厅产品列表的英文名称。
- `category`：来自展厅产品列表的持证人。
- `update_time`：按年月日展示或导出。
- `dcc_product_code`：展厅生成和重新同步时均清空，不需要 DCC 产品编号。
- `product_code`：按 `INT-展厅产品ID` 生成；已绑定既有主数据时同步更新同一个主数据的产品编码，不新建重复主数据。它是可变业务编码，不作为展厅或 DCC 的关联依据。
- 展厅关联：`showroom_product.product_master_id -> mdm_product.id`。
- DCC 关联：`dcc_controlled_file.product_master_id -> mdm_product.id`，DCC 文件内的产品编号和产品名称只作为提交时快照。

## BDD 场景

- BDD: 展厅产品按最新规则生成主数据 -> Given 本机 `芋道源码/admin` 展厅产品列表存在中文名称、英文名称和持证人 / When 管理员预览并确认映射 / Then 产品主数据中文名、英文名、分类分别来自展厅字段，DCC 产品编号为空，产品编码为 `INT-数字`。
- BDD: 重复执行保持可追溯绑定 -> Given 展厅产品已绑定产品主数据 / When 重新确认映射 / Then 系统更新已绑定主数据的名称、英文名、分类、产品编码和 DCC 产品编号，不新增重复主数据。
- BDD: 展厅映射清空 DCC 产品编号 -> Given 产品主数据已拥有历史 DCC 产品编号 / When 展厅按 `product_master_id` 同步该产品 / Then 同步请求与主数据更新把产品编码改为 `INT-展厅产品ID`，并把 DCC 产品编号置空。
- BDD: 产品编码变更不影响跨模块关联 -> Given 产品主数据已被展厅和 DCC 通过 `product_master_id` 引用 / When 管理员修改该产品主数据的 `product_code` / Then 产品主数据 ID 保持不变，展厅和 DCC 仍通过同一个 ID 对应该产品。
- BDD: 产品主数据导出更新时间按年月日 -> Given 产品主数据存在更新时间 / When 管理员导出 Excel / Then 更新时间列按年月日格式输出。

## Milestones

- [x] M1：建立任务文档、执行日志和 BDD。
- [x] M2：定位现有展厅映射服务、产品主数据导出和相关测试。
- [x] M3：先补失败测试覆盖最新映射规则、编码可变规则和导出日期格式。
- [x] M4：实现映射规则、产品编码可变规则和导出格式调整。
- [x] M5：用 `芋道源码/admin` 真实数据预览、确认并验证数据库与接口。
- [x] M6：记录证据并提交本任务相关改动。

## Expected Verification

- `mvn -pl yudao-module-showroom -am "-Dtest=ShowroomMdmProductMappingServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `mvn -pl yudao-module-mdm -am "-Dtest=MdmProductServiceImplTest,MdmProductExportExcelVOTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `GET /admin-api/showroom/product/mdm-mapping-preview`
- `POST /admin-api/showroom/product/mdm-mapping-confirm`
- 数据库验证：租户 1 的 `mdm_product.product_code` 符合 `INT-数字`，`dcc_product_code` 为空，`name_cn/name_en/category` 与展厅字段一致。
- 接口验证：`GET /admin-api/mdm/product/page`

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。缺少展厅字段、权限、接口、测试依赖或数据库连接时直接失败。
- `是否从根因和长期维护角度解决`：是。调整正式映射服务、主数据编码修改规则和导出格式，不用一次性 SQL 覆盖规则。
- `是否存在临时补丁或绕过`：否。不绕过管理端权限，不直接拼装绕过业务服务的数据。

## 数据风险与回滚说明

- 本任务会更新租户 1 现有产品主数据编码、名称、英文名、分类、DCC 产品编号、状态和更新时间，并保持展厅绑定关系。
- 本任务不物理删除展厅产品，不物理删除产品主数据。
- 如需要回滚，必须基于执行日志中的验证时间和更新结果人工恢复；不得清空主数据表。

## 当前状态

completed: 已按用户最新规则调整正式映射服务，并使用本机 `芋道源码/admin` 租户 1 的真实展厅产品执行确认。结果：178 条有效展厅产品全部绑定产品主数据；`product_code` 均为 `INT-数字`；`dcc_product_code` 均为空；中文名称、英文名称和分类分别来自展厅当前版本中文名、英文名和持证人；展厅和 DCC 的长期关联以 `product_master_id` 为准。

## 最终验证结果

- MDM 测试：`mvn -pl yudao-module-mdm -am "-Dtest=MdmProductServiceImplTest,MdmProductExportExcelVOTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，3 tests，覆盖展厅同步更新产品编码、清空 DCC 产品编号、手工更新产品编码和 Excel 更新时间日期格式。
- 展厅映射测试：`mvn -pl yudao-module-showroom -am "-Dtest=ShowroomMdmProductMappingServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，3 tests，覆盖已绑定展厅产品按 `INT-展厅产品ID` 预览和同步。
- 映射预览：`GET /admin-api/showroom/product/mdm-mapping-preview` -> PASS，`totalCount=178`，`createCount=0`，`updateCount=178`，`linkedCount=0`，`failureCount=0`，`mappingHash=d628c23f6c7bb56b97a0af23294c5913a6e665f34ede566a46579f5c3ea8fee5`。
- 映射确认：`POST /admin-api/showroom/product/mdm-mapping-confirm` -> PASS，`totalCount=178`，`createCount=0`，`updateCount=178`，`linkedCount=0`，`failureCount=0`。
- 数据库核验：`mdm_total=178`，`showroom_active=178`，`showroom_bound=178`，`missing_bound_master=0`，`invalid_code=0`，`code_mismatch=0`，`non_null_dcc=0`，`name_mismatch=0`，`blank_owner_category_not_null=0`，`company_category_mismatch=0`。
- 产品主数据接口：`GET /admin-api/mdm/product/page?pageNo=1&pageSize=10` -> PASS，`total=178`；导出接口 `/admin-api/mdm/product/export-excel` -> PASS，返回 Excel 内容 14264 bytes。
- 后端重启后只读核验：健康检查 `UP`；`GET /admin-api/showroom/product/mdm-mapping-preview` -> PASS，`totalCount=178`，`createCount=0`，`updateCount=178`，`linkedCount=0`，`failureCount=0`；`GET /admin-api/mdm/product/page?pageNo=1&pageSize=10` -> PASS，`total=178`，首条编码为 `INT-251`，DCC 产品编号为空。
- 修正提交后重启核验：后端提交 `b35ea670be` 后重启到 `backend-runtime-control-20260607-235941.jar`，健康检查 `UP`；`GET /admin-api/showroom/product/mdm-mapping-preview` -> PASS，`totalCount=178`，`createCount=0`，`updateCount=178`，`linkedCount=0`，`failureCount=0`；`GET /admin-api/mdm/product/page?pageNo=1&pageSize=10` -> PASS，`total=178`，首条编码为 `INT-251`，DCC 产品编号为空。
- 最终数据库一致性核验：`mdm_total=178`，`showroom_total=178`，`showroom_bound=178`，`missing_bound_master=0`，`invalid_code=0`，`code_mismatch=0`，`non_null_dcc=0`，`missing_revision=0`，`name_cn_mismatch=0`，`name_en_mismatch=0`，`category_mismatch=0`，`model_mismatch=0`；样例 `INT-1` 的中文名称为 `三通旋塞`、英文名称为 `Manifold for Single`、分类为 `瑛泰`、更新时间日期为 `2026-06-07`。
- Playwright 前端验证：从 `http://localhost:8081` 使用 `芋道源码/admin` 登录，访问 `/mdm/product` -> PASS，页面显示 `基础数据 / 产品主数据`、产品编码列、中文名称列、更新时间列、导入/导出按钮、10 行表格数据；列表显示 `INT-数字` 编码、DCC 产品编号为 `-`、更新时间按 `2026-06-07` 展示。

## Cleanup Keep

- doc/tasks/20260607-product-master-showroom-rule-update/backend-api-evidence.md
- doc/tasks/20260607-product-master-showroom-rule-update/database-schema-evidence.md
