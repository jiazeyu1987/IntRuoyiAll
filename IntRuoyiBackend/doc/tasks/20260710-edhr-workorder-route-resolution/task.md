# eDHR 工单路线自动识别修复

## 任务目标

- 修复生产工单 `881M0090889` 创建 eDHR 批次执行时无法自动识别工艺路线的问题。
- 保持当前启用工艺流程的严格运行态约束，不引入历史路线或禁用流程 fallback。

## 当前状态

completed

## Current Status

completed

## 上一任务检查

- `doc/tasks/20260710-dcc-product-catalog-database-import/task.md` 状态为 `completed`，不阻塞本任务。

## 经验门禁

- PowerShell / UTF-8：中文读写显式使用 UTF-8，命令不使用 `&&`。
- 缺陷修复：先复现、补失败测试，再修改生产代码。
- 数据验证：仅操作本机测试租户真实路径；未获授权不操作服务器。
- Git 隔离：当前仓库已有其他未提交改动，只提交本任务文件和精确 hunk。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；统一工单路线解析与当前启用工艺流程运行态契约。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 有效工单自动识别当前批记录路线 -> Given 工单具有唯一当前启用批记录工艺流程 / When routeId 缺省创建批次执行 / Then 自动识别路线并创建批次。`
- `BDD: 非当前流程不参与识别 -> Given 产品存在历史路线或禁用流程 / When 解析路线 / Then 仅当前启用且父子归属正确的批记录流程可成为候选。`
- `BDD: 无唯一路线时失败 -> Given 正式配置无法确定唯一路线 / When 创建批次执行 / Then 显式失败且不猜测。`

## 里程碑

1. [已完成] 复现并定位工单真实数据与解析分支。
2. [已完成] 新增 RED 回归测试。
3. [已完成] 实施最小修复。
4. [已完成] 回归验证、证据校验和本机运行态更新。

## 预期验证

- `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProEdhrBatchExecutionServiceTest#<target>" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProEdhrBatchExecutionServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- 缺陷证据校验脚本通过。

## 当前阻塞

- 暂无。

## 当前根因

- 已存在批次执行时，`openOrCreate` 仍先调用 `resolveRouteId` 校验当前启用流程。
- 历史批次保存的路线合法上下文因此被当前流程状态覆盖，无法执行“打开已有批次”。

## 最终验证

- RED：新增回归首先失败并复现 `PRO_EDHR_BATCH_EXECUTION_ROUTE_NOT_EXISTS`。
- GREEN：目标回归 1/1、服务测试 71/71 通过。
- 本机后端重新打包并重启，健康检查 `UP`。
- 运行包已包含新的工单 + 批次已有执行查询方法。
- 官方登录预检已进入本机 eDHR 批次执行页面。
- 未对 `tenant_id=1` 发送写审计的打开/创建请求；当前任务未获得芋道源码租户写入授权。

## Cleanup Candidates

- `doc/tasks/20260710-edhr-workorder-route-resolution/bug-regression-evidence.md`
