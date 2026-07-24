# Execution Log：绑定 eDHR 报表到冠状动脉棘突球囊扩张导管工艺路线

BDD: 电子批记录按顺序绑定现有工序 -> Given admin 下已有目标路线和 15 张电子批记录 / When 执行绑定 / Then 目标路线 15 个指定工序的 `batch_record_report_id` 与映射表一致，路线工序数量仍为 21。

BDD: 资源大表可查询目标产品路线 -> Given 目标路线需要出现在资源大表 / When 补充未占用的启用产品规格关联 / Then 资源大表按产品编码或路线编码可查询到目标路线工序行。

BDD: 不破坏已有产品路线归属 -> Given `1003/1011` 已关联 `ROUTE-XLSX-00002` / When 执行本任务 / Then 这两个规格仍保留原路线关联。

- SETUP: 创建任务文档 -> PASS，任务目录 `doc/tasks/20260608-edhr-route-report-binding`。

- RED: 执行前查询目标路线报表绑定 -> FAIL，`ROUTE-YXN.069.001.1001` 现有 21 道工序，只有 sort 1 `B010` 已绑定 `EBR_TN1_A_T01`，sort 2/3/4/5/6/7/8/9/10/16/17/18/19/21 共 14 个计划绑定为空。

- RED: 执行前查询产品路线关联 -> FAIL，计划新增的 11 个产品规格未关联任何路线；受保护规格 `YXN.069.001.1003`、`YXN.069.001.1011` 已关联 `ROUTE-XLSX-00002`，本任务不迁移。

- GREEN: 数据事务执行 -> PASS；事务内校验目标路线、21 道工序、15 张报表、11 个未占用产品和受保护产品归属后，临时停用目标路线，更新 15 个工序的 `batch_record_report_id`，新增 11 个 `mes_pro_route_product` 关联，再重新启用目标路线。

- GREEN: 报表绑定验证 -> PASS；`EBR_TN1_A_T01` 到 `EBR_TN1_A_T15` 均按计划映射到目标路线指定工序，`mapping_ok=true`。

- GREEN: 产品关联验证 -> PASS；`YXN.069.001.1002/1004/1005/1006/1007/1008/1009/1010/1012/1013/1014` 已关联 `ROUTE-YXN.069.001.1001`，`quantity`、`production_time`、`time_unit_type` 均为空；`1003/1011` 仍关联 `ROUTE-XLSX-00002`。

- GREEN: 资源大表接口验证 -> PASS；使用 `芋道源码/admin` 登录 `http://127.0.0.1:48082/admin-api` 后调用 `/mes/pro/route-resource/page?keyword=ROUTE-YXN.069.001.1001`，返回 `total=407`，样例行包含 `productCode=YXN.069.001.1002`、`routeCode=ROUTE-YXN.069.001.1001`、`processCode=B010`。

- FINAL: 目标路线报表绑定和资源大表可见性已完成，未改工序顺序、未迁移 `ROUTE-XLSX-00002` 产品归属。
