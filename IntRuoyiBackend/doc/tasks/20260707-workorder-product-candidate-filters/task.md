# 生产工单产品候选过滤后端

## 任务目标

扩展 MES 生产工单分页查询，支持前端“产品名称”和“产品编码”候选框传入的产品 ID，并与现有 `productId` 条件收敛为同一个产品过滤条件。

## 非目标

- 不新增数据库字段或迁移。
- 不按产品名称/编码字符串直接过滤工单表，因为工单表只保存 `product_id`。
- 不引入 fallback、mock 或静默吞异常。

## 经验门禁

- PowerShell / Windows shell / 中文编码：已读取 `docs/powershell-memory.md`，中文读写和命令输出显式 UTF-8。
- 后端 API 交付：已读取 `backend-api-delivery` 与 `backend-contract.md`，补充后端单测和证据。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，后端正式支持新增候选 ID 参数并按产品 ID 查询。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

BDD: 候选产品 ID 过滤生产工单分页 -> Given 前端选择产品名称或产品编码候选 / When 调用 `/mes/pro/work-order/page` / Then 后端按候选产品 ID 过滤生产工单。

BDD: 多个产品过滤条件保持一致 -> Given 请求同时包含现有产品选择器和候选产品 ID / When 产品 ID 一致 / Then 后端按该产品 ID 查询；When 产品 ID 不一致 / Then 返回空结果，不静默改用某一个条件。

## 里程碑

- [x] M1：补后端单测复现新增候选 ID 参数未生效。
- [x] M2：扩展分页 VO 与 service/mapper 查询逻辑。
- [x] M3：运行目标验证并记录证据。
- [x] M4：完成任务记录并提交本次相关改动。

## 预期验证

- `mvn.cmd -pl yudao-module-mes -Dtest=MesProWorkOrderServiceImplTest test`

## 当前状态

completed

## 完成记录

- `MesProWorkOrderPageReqVO` 新增 `productNameFilterId`、`productCodeFilterId`。
- `MesProWorkOrderServiceImpl` 将现有 `productId`、产品名称候选 ID、产品编码候选 ID 收敛为同一产品过滤条件；冲突时使用空命中哨兵，不静默降级。
- `MesProWorkOrderMapper` 在没有扩展产品 ID 集合时也可使用新增候选 ID 过滤。
- 验证：后端静态合同通过，`mvn.cmd -pl yudao-module-mes -DskipTests compile` 通过。
- 阻塞：`mvn.cmd -pl yudao-module-mes -Dtest=MesProWorkOrderServiceImplTest test` 在测试编译阶段被同模块无关 `ThirdPartyFeedbackImportServiceImplTest` 源码错误阻塞，未进入本次新增用例执行。

## Cleanup Keep

- `doc/tasks/20260707-workorder-product-candidate-filters/backend-api-evidence.md`
