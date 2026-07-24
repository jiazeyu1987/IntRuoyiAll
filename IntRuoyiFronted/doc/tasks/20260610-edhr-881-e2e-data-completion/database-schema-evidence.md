# 数据补齐证据

## 数据库

- 引擎：MySQL，本机 `127.0.0.1:3306/ruoyi-vue-pro`。
- 范围：仅测试租户必要 E2E 数据。

## Data

- 源租户：`tenant_id=1`，只读核对。
- 目标租户：`tenant_id=122`，本轮核对目标数据是否足以支撑 E2E。
- 目标工单：`mes_pro_work_order.id=922142`，`code=881MO090863`。
- 目标路线：`mes_pro_route.id=922045`，`code=ROUTE-YXN.069.001.1001`。
- 目标批次执行：`mes_pro_edhr_batch_execution.id=9`，`batch_code=PC-E2E-20260610-0210`。
- 本轮未新增、更新或删除业务数据。

## Migration

- 本轮无 schema migration。
- 本轮无 seed migration。
- 本轮无数据平移 SQL，因为测试租户目标数据已经满足 E2E 前置条件。

## Safety

- 未写入 `tenant_id=1` 芋道源码租户。
- 未操作正式服务器、测试服务器或远程数据库。
- 未修改受保护展厅文件配置、DCC 审核矩阵或无关业务表。
- 未引入 fallback、mock 成功、静默降级或接口绕过真实前端流程。

## BDD

- BDD: 必要主数据补齐 -> Given 测试租户缺少工单、路线、报表绑定、用户签名前置数据或权限，When 从芋道源码租户读取同类数据并创建测试租户副本，Then eDHR 前端流程可按真实业务路径继续，且源租户记录不被修改。

## RED

- RED: 测试租户缺口核对 -> FAIL, expected reason: 不能假设目标数据已存在；必须先查询目标工单、路线、报表、绑定、产品规格、权限和批次执行状态。

## GREEN

- GREEN: 目标工单 -> PASS，测试租户存在 `mes_pro_work_order.id=922142`，`code=881MO090863`，`temporary_frozen=0`，`deleted=0`。
- GREEN: 目标路线 -> PASS，测试租户存在 `mes_pro_route.id=922045`，`code=ROUTE-YXN.069.001.1001`，`status=0`，`deleted=0`。
- GREEN: 工序与报表绑定 -> PASS，目标路线存在 21 道工序；15 道工序绑定测试租户报表 `EBR_TN122_A_T01` 到 `EBR_TN122_A_T15`，6 道工序无默认批记录且为无需填写任务。
- GREEN: 路线产品规格 -> PASS，测试租户目标路线已关联 11 个冠状动脉棘突球囊扩张导管规格 `1002/1004/1005/1006/1007/1008/1009/1010/1012/1013/1014`。
- GREEN: 权限 -> PASS，测试租户 `aoteman` 的 `tenant_admin` 角色拥有 eDHR 批次执行查询、创建、更新、关闭、归档查询、归档生成、归档下载权限。
- GREEN: 完成批次 -> PASS，`mes_pro_edhr_batch_execution.id=9`，`batch_code=PC-E2E-20260610-0210`，`work_order_code=881MO090863`，`route_code=ROUTE-YXN.069.001.1001`，`status=40`，`task_total=21`，`task_approved_count=15`，`blocked_count=0`。
- GREEN: 单表执行与签名 -> PASS，15 张单表执行状态均为批准终态；`APPROVE`、`FIELD_CHANGE`、`FORM_REVIEW`、`SUBMIT` 签名各 15 条。
- GREEN: 最终归档 -> PASS，`archive_status=SEALED`，文件名 `PC-E2E-20260610-0210-edhr-final.pdf`，`content_hash=a55f09a4c7a02117aefb6c5a345093a51c459cfaeb18040ebf2d44b721bdf1e0`。

## Rollback

本轮未执行数据插入、更新或删除；无需业务数据回滚。若后续要清理演示批次，必须按批次执行、任务、单表执行、签名、字段审计、归档的依赖顺序单独制定清理任务。

## Verification

- `node --check tests\e2e\edhr-881-completed-batch-review.e2e.js` -> PASS。
- `$env:EDHR_881_E2E_PASSWORD='admin123'; node tests\e2e\edhr-881-completed-batch-review.e2e.js` -> PASS。
- SQL 校验批次、任务、签名、归档状态 -> PASS。

## Blockers

无。
