# 任务：从展厅产品生成产品主数据第一版

## 任务目标

使用本机 `芋道源码/admin` 账号对应的真实租户数据，将展厅“产品管理”里的产品列表按产品主数据映射规则生成第一版 `mdm_product` 数据，并回写展厅产品的 `product_master_id` 绑定关系。

## Previous Task Check

- 上一个后端任务：`doc/tasks/20260607-product-master-data/task.md`
- 状态：`completed`
- 处理：产品主数据功能已合入 `int_main` 并通过主分支验证；本任务只执行本机 `芋道源码/admin` 租户数据初始化，不修改功能代码。

## BDD 场景

- BDD: 芋道源码展厅产品生成产品主数据第一版 -> Given 本机 `芋道源码/admin` 已有展厅产品列表 / When 管理员预览并确认展厅映射 / Then 系统按产品编码新增或绑定产品主数据，失败行存在时直接阻塞并不写入。
- BDD: 生成后可追溯绑定 -> Given 展厅产品映射确认成功 / When 查询展厅产品和产品主数据 / Then 展厅产品存在 `product_master_id`，产品主数据包含对应产品编码和中文名称。

## Milestones

- [x] M1：建立任务文档和执行日志。
- [x] M2：用 `芋道源码/admin` 预览展厅映射，记录新增、更新、绑定和失败数。
- [x] M3：失败数为 0 时确认映射并生成第一版产品主数据。
- [x] M4：验证数据库和接口结果，记录证据并提交任务记录。

## Expected Verification

- `GET /admin-api/showroom/product/mdm-mapping-preview`
- `POST /admin-api/showroom/product/mdm-mapping-confirm`
- 数据库验证：`mdm_product` 租户 1 记录数、`showroom_product.product_master_id` 绑定数。
- 接口验证：`GET /admin-api/mdm/product/page`

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。预览失败、重复编码、缺少编码或中文名、权限不足、接口失败均直接阻塞。
- `是否从根因和长期维护角度解决`：是。使用已实现的正式展厅映射产品主数据接口，不直接手写猜测映射。
- `是否存在临时补丁或绕过`：否。不跳过 admin 权限，不绕过业务接口直接拼装产品主数据。

## 数据风险与回滚说明

- 本任务会新增或更新租户 1 的 `mdm_product`，并回写 `showroom_product.product_master_id`。
- 本任务不物理删除产品主数据和展厅产品。
- 如需要回滚，必须基于本次执行日志记录的执行时间和生成结果，人工清理本次新增的产品主数据和对应展厅绑定；不得用全表清空替代。

## 当前状态

completed: 已使用本机 `芋道源码/admin` 租户 1 真实展厅产品数据生成第一版产品主数据。有效展厅产品 178 条已生成并绑定，删除态展厅产品 13 条未纳入映射；接口 `/admin-api/mdm/product/page` 返回产品主数据总数 178。

## 最终验证结果

- 映射预览：`totalCount=178`，`createCount=178`，`updateCount=0`，`linkedCount=0`，`failureCount=0`。
- 映射确认：成功生成 178 条产品主数据，失败数 0。
- 数据库验证：租户 1 下 `mdm_product=178`，`showroom_product=191`，`showroom_product.product_master_id IS NOT NULL=178`，有效未绑定展厅产品 0。
- 接口验证：`GET /admin-api/mdm/product/page?pageNo=1&pageSize=10` 返回 `total=178`。
