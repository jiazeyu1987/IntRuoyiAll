# 任务：MES 生产工单测试用随机 ERP 建单

## 任务目标

把生产工单列表现有的“创建ERP订单”能力改成测试用复制建单：复用当前工单主体内容创建 ERP 生产订单，但随机生成 ERP 工单编码，并把数量随机生成为 `10~1000` 的整数，使后续 `同步金蝶` 时可以新增本地生产工单。

## 当前状态

COMPLETED

## Current Status

COMPLETED

## 上一任务检查

- 上一个 backend 任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260623-dcc-short-code-recognition-hardening\task.md`
- 状态：`BLOCKED`
- 处理：用户已切换优先级，旧任务已显式阻塞；本任务只处理 MES 生产工单测试用 ERP 建单链路，不碰 DCC 识别逻辑。

## 经验门禁

- 已读取：
  - `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - `D:\ProjectPackage\Int\IntRuoyi\docs\integrations\kingdee-erp-official-docs.md`
- 本任务适用强制门禁：
  - 金蝶能力只允许走现有官方对接链路，不得引入 mock、默认成功、备用数据源或静默降级。
  - 本任务仅改本机后端代码、单测和任务文档，不操作服务器、不写测试租户数据、不做发布。
  - 先写失败测试再改生产逻辑；随机编码和数量的测试能力必须显式暴露，不能伪装成原有正式建单语义。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否
- `是否从根因和长期维护角度解决`：是；在用户明确要求的测试范围内，把随机复制建单收敛到现有 MES->ERP 建单入口，不引入隐式兼容分支。
- `是否存在临时补丁或绕过`：是；用户明确要求这是测试功能，随机编码/随机数量仅用于测试建单与回同步验证，风险是该入口不再代表“原单直推”，后续若恢复正式语义应单独拆回正式建单能力。

## BDD 场景

- `BDD: 测试建单会复制当前工单主体内容但使用随机编码和随机数量 -> Given 用户对一条可建 ERP 的生产工单点击测试建单 / When 后端向金蝶发起创建请求 / Then 请求仍复用当前工单产品与其他主体字段，但 billNo 必须随机生成，quantity 必须是 10~1000 的整数。`
- `BDD: 同一条基础工单允许重复触发测试建单 -> Given 同一条 MES 生产工单已经成功创建过一张测试 ERP 订单 / When 用户再次点击测试建单 / Then 后端不得再用 workOrderId 已关联记录阻断第二次创建。`
- `BDD: 后续同步金蝶时随机编码订单会作为新工单进入本地 -> Given 测试创建出的 ERP 订单 billNo 与原工单编码不同 / When 用户后续执行同步金蝶 / Then 同步逻辑应把该 ERP 订单识别为新的工单编码，而不是回写原工单。`

## 里程碑

1. M1：建立任务文档、证据文档与请求日志，并冻结旧 backend 任务状态。
2. M2：补后端 RED 测试，锁定随机编码、随机数量、重复触发允许等行为。
3. M3：实现测试用随机 ERP 建单逻辑，并更新前端文案与成功提示。
4. M4：运行定向后端/前端验证，更新证据与执行日志。

## 预期验证

- `mvn -pl yudao-module-mes -Dtest=MesKingdeeProductionOrderCreateServiceImplTest test`
- `mvn -pl yudao-server -am -DskipTests package`
- `node yudao-ui-admin-vue3/tests/e2e/workorder-create-erp-order-static.spec.js`

## 完成结果

- 后端 `create-kingdee-production-order` 已改为测试复制建单：复用当前工单主体字段，但随机生成 ERP `billNo` 与 `10~1000` 的整数数量。
- 不再写 `workOrderId` -> ERP 的本地同步关联记录，因此同一基础工单允许重复触发测试建单，后续 `同步金蝶` 可按新编码新增本地工单。
- 接口摘要同步改为测试语义，便于和正式“原单直推”语义区分。

## 最终核验

- `mvn -pl yudao-module-mes clean -Dtest=MesKingdeeProductionOrderCreateServiceImplTest test` -> PASS
- `mvn -pl yudao-server -am -DskipTests package` -> PASS
