# Execution Log：确认芋道源码 admin 下 eDHR 产品主数据

BDD: admin 产品主数据存在性检查 -> Given 用户要求在 `芋道源码/admin` 下确认产品主数据 / When 按产品名称查询 MES 主产品/物料主数据 / Then 如果已存在启用产品记录，不创建重复产品。

BDD: admin 缺失时新建产品主数据 -> Given `芋道源码/admin` 下产品主数据确实缺失 / When 创建正式产品主数据 / Then 必须按确定的编码、规格、单位、产品类型创建，且可查询到启用记录。

- SETUP: 创建任务文档 -> PASS，任务目录 `doc/tasks/20260608-edhr-admin-product-master-data`。

- VERIFY: 查询租户和 admin 用户 -> PASS；`system_tenant.id=1` 名称为 `芋道源码`，`system_users.username=admin` 对应 `tenant_id=1`。

- VERIFY: 查询 admin 产品类型 -> PASS；`tenant_id=1` 下产品类型为 `mes_md_item_type.id=2`，名称 `Kingdee Imported Product`，状态启用。

- RED: 查询精确编码 `YXN.069.001.1001` -> FAIL，当前数据库中未找到该编码，不能把路线编码直接当作产品编码。

- GREEN: 使用 Unicode escape 精确查询 admin 产品名称 `冠状动脉棘突球囊扩张导管` -> PASS，`tenant_id=1` 下已存在 13 条启用产品记录，另有 1 条同名停用记录。

- VERIFY: 未执行 INSERT / UPDATE / DELETE -> PASS；因 admin 下已有同名启用产品，本任务按“有则不新建”停止写入。

- FINAL: `芋道源码/admin` 下产品主数据已存在，无需新建；后续如必须补 `YXN.069.001.1001`，需先确认规格、单位和批次属性。
