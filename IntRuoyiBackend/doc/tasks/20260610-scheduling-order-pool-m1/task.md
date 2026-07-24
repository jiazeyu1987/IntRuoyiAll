# 排产工单池 M1

## 任务目标

新增排产工单池第一版：排产员从 ERP 同步来的生产工单生成唯一排产工单，填写承诺交期和优先级；排产工单数量必须等于生产工单数量，不允许拆分；生成时创建工序明细和基础快照，为后续自动排程、报工归属和 eDHR 重排保护提供正式业务边界。

## Previous Task Check

- 前序同仓库相关任务：`doc/tasks/20260610-scheduling-test-tenant-data-baseline/task.md`。
- 检查结果：该任务已标记 `completed`，测试租户已具备真实路线、产品、生产工单、工单 BOM、报工样本和 eDHR 保护验证前置数据。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。缺生产工单、缺产品路线、缺路线工序、重复有效排产工单、承诺交期为空时均直接失败并返回明确错误。
- `是否从根因和长期维护角度解决`：是。新增排产工单边界，不把承诺交期、优先级、排产状态继续塞入生产工单或生产任务。
- `是否存在临时补丁或绕过`：否。本任务不修改 ERP 来源语义，不绕过真实数据；测试写入仅限测试租户通过 API/SQL 受控验证。

## BDD 场景

- BDD: 从生产工单生成唯一排产工单 -> Given 测试租户存在 ERP 同步生产工单且未生成有效排产工单 / When 排产员填写承诺交期并生成排产工单 / Then 系统创建排产工单，排产数量等于生产工单数量，并记录生产工单快照。
- BDD: 重复生成排产工单失败 -> Given 某生产工单已有未取消排产工单 / When 排产员再次从该生产工单生成排产工单 / Then 系统拒绝生成并提示同一生产工单不可拆分。
- BDD: 生成排产工单工序明细 -> Given 产品存在有效工艺路线和组成工序 / When 排产工单创建成功 / Then 系统按路线工序创建排产工单工序明细，保存工序、工位和产能快照。
- BDD: 缺少承诺交期失败 -> Given 生产工单存在 / When 排产员未填写承诺交期生成排产工单 / Then 系统拒绝创建排产工单。

## 里程碑

- [x] M1：新增数据库 schema、SQL 契约测试和证据。
- [x] M2：新增后端 DO/Mapper/Service/Controller/VO 与单元测试。
- [x] M3：新增前端 API 与排产工单池最小页面入口。
- [x] M4：运行后端、前端和真实数据验证，记录证据。

## 预期验证

- RED/GREEN：SQL 契约测试覆盖 `mes_pro_schedule_order`、`mes_pro_schedule_order_process`、`mes_pro_schedule_order_diff`。
- RED/GREEN：后端测试覆盖创建成功、重复创建失败、承诺交期缺失失败、工序明细生成。
- GREEN：前端静态契约确认存在排产工单池入口、生产工单编码筛选、生成排产工单动作和承诺交期字段。

## 当前状态

completed

## 完成记录

- 新增排产工单三张正式表契约：`mes_pro_schedule_order`、`mes_pro_schedule_order_process`、`mes_pro_schedule_order_diff`。
- 兼容本机已有旧版排产工单草稿表，补齐正式字段并回填旧数据，不删除旧字段、不物理删除已有数据。
- 新增后端接口：
  - `POST /mes/pro/schedule-order/create-from-work-order`
  - `GET /mes/pro/schedule-order/page`
  - `GET /mes/pro/schedule-order/get`
  - `GET /mes/pro/schedule-order/process/list`
- 新增前端 API 和 `MES -> 排产工单池` 页面组件。
- 本阶段只做排产工单池最小边界，不接入自动排程，不改报工导入，不移动 eDHR 任务。

## 最终验证

- GREEN: `python -m pytest script\tests\test_mes_schedule_order_schema_sql.py` -> PASS。
- GREEN: `mvn -pl yudao-module-mes -Dtest=MesProScheduleOrderServiceImplTest test` -> PASS。
- GREEN: `node tests\e2e\mes-schedule-order-pool-static.spec.js` -> PASS。
- GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。
- GREEN: 本机 Docker MySQL schema 应用与只读校验 -> PASS。
