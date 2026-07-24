# Execution Log：eDHR 工单路线自动识别修复

BDD: 有效工单自动识别当前批记录路线 -> Given 工单具有唯一当前启用批记录工艺流程 / When routeId 缺省创建批次执行 / Then 自动识别路线并创建批次。
BDD: 非当前流程不参与识别 -> Given 产品存在历史路线或禁用流程 / When 解析路线 / Then 仅当前启用且父子归属正确的批记录流程可成为候选。
BDD: 无唯一路线时失败 -> Given 正式配置无法确定唯一路线 / When 创建批次执行 / Then 显式失败且不猜测。

GREEN: task-bootstrap -> PASS，已建立根仓与后端任务台账。
GREEN: previous-task-check -> PASS，后端最近任务状态为 completed。
GREEN: prior-contract-check -> PASS，前端不再提交 routeId，后端负责自动解析路线。
GREEN: real-data-readonly-diagnosis -> PASS，工单 `923889` 与批次 `34126020001` 已有批次执行 `900000000480`，保存路线 `922099`；该路线当前 BATCH 顶层流程禁用。
RED: `mvn.cmd -pl yudao-module-mes -am "-Dtest=cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionServiceTest#openOrCreate_reopensExistingBatchWhenCurrentBatchFlowDisabled" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，抛出 `PRO_EDHR_BATCH_EXECUTION_ROUTE_NOT_EXISTS`。
GREEN: target-regression -> PASS，目标测试 1/1 通过。
GREEN: service-regression -> PASS，`MesProEdhrBatchExecutionServiceTest` 71/71 通过。
GREEN: experience-preflight -> PASS，已读取 `docs/login-access.md` 与 `docs/server-access.md`；仅允许本机重启和本机真实验证。
GREEN: local-backend-restart -> PASS，运行包 `backend-20260710-095556.jar` 已启动，48081 健康检查为 `UP`。
GREEN: packaged-runtime-contract -> PASS，内嵌 `yudao-module-mes` 为存储模式且包含 `selectListByWorkOrderIdAndBatchCode` 字节码标记。
GREEN: official-login-preflight -> PASS，测试租户 `aoteman` 已真实进入 `/mes/pro/feedback/edhr-batch-execution`。
INFO: admin-submit-not-run -> `tenant_id=1` 的打开/创建请求会写操作审计，当前任务未获得芋道源码租户写入授权，因此未执行该 POST。
GREEN: bug-evidence-validator -> PASS，缺陷摘要、复现、根因、回归测试、RED/GREEN、验证范围和后续项均完整。
GREEN: closeout-preview -> PASS，仅删除已校验的 `bug-regression-evidence.md`，保留任务文档、执行日志、生产代码和正式回归测试。
GREEN: closeout-apply -> PASS，已删除缺陷证据临时文件，仅保留 `task.md`、`execution-log.md`、生产代码和正式回归测试。
