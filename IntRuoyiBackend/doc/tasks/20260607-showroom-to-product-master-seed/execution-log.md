# 执行日志：从展厅产品生成产品主数据第一版

- BDD: 芋道源码展厅产品生成产品主数据第一版 -> Given 本机 `芋道源码/admin` 已有展厅产品列表 / When 管理员预览并确认展厅映射 / Then 系统按产品编码新增或绑定产品主数据，失败行存在时直接阻塞并不写入。
- BDD: 生成后可追溯绑定 -> Given 展厅产品映射确认成功 / When 查询展厅产品和产品主数据 / Then 展厅产品存在 `product_master_id`，产品主数据包含对应产品编码和中文名称。

- GREEN: `docker exec int-ruoyi-mysql mysql ... SELECT ... FROM mdm_product/showroom_product WHERE tenant_id=1` -> PASS，执行前租户 1 基线为 `mdm_product=0`、`showroom_product=191`、`showroom_product_bound=0`。
- RED: `GET /admin-api/showroom/product/mdm-mapping-preview` -> FAIL，本机后端运行时仍是旧 jar，接口返回 `No static resource admin-api/showroom/product/mdm-mapping-preview.`，无法执行正式映射接口。
- GREEN: `powershell -NoProfile -ExecutionPolicy Bypass -File script\deploy\restart-int-ruoyi-local.ps1 -Component backend` -> PASS，重启后 `http://127.0.0.1:48081/actuator/health` 返回 `UP`。
- GREEN: `GET /admin-api/showroom/product/mdm-mapping-preview` 使用 `tenant-id=1` 和 `芋道源码/admin` 登录令牌 -> PASS，`mappingHash=a43e235343829155df6a352d7b1fb20f8cc7963dfa5ef195458fa87d74037d1b`，`totalCount=178`，`createCount=178`，`updateCount=0`，`linkedCount=0`，`failureCount=0`。
- GREEN: `POST /admin-api/showroom/product/mdm-mapping-confirm` 使用相同 `mappingHash` -> PASS，确认生成首版产品主数据，`totalCount=178`，`createCount=178`，`updateCount=0`，`linkedCount=0`，`failureCount=0`。
- GREEN: `docker exec int-ruoyi-mysql mysql ... SELECT COUNT(*) ...` -> PASS，执行后租户 1 结果为 `mdm_product=178`、`showroom_product=191`、`showroom_product_bound=178`、`mdm_product_enabled=178`。
- GREEN: `docker exec int-ruoyi-mysql mysql ... SELECT deleted, COUNT(*) FROM showroom_product WHERE tenant_id=1 GROUP BY deleted` -> PASS，租户 1 展厅产品删除态 13 条、有效态 178 条；有效未绑定数 0，有效已绑定数 178。
- GREEN: `docker exec int-ruoyi-mysql mysql ... JOIN showroom_product/mdm_product LIMIT 10` -> PASS，样例绑定存在，`showroom_product.product_master_id` 指向同编码 `mdm_product.product_code`；样例中文名称以 `HEX(name_cn)` 验证为 UTF-8 字节，避免 PowerShell 控制台乱码误判。
- GREEN: `GET /admin-api/mdm/product/page?pageNo=1&pageSize=10` 使用 `tenant-id=1` 和 `芋道源码/admin` 登录令牌 -> PASS，返回 `total=178`，第一页记录数 10。
