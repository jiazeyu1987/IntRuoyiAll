# Verification Report

## Result

PASS。`int_main` 真实前端已验证一线 PQC 同时显示产品 QA 与通用检验规程套内容，并完成通用工序多任务电子签名提交。

## Fixed Behavior

- 路线 `RT000028-IDI` 的历史正式生产配置已通过显式入口迁移并发布为 `V16 / routeVersionId=746`。
- 通用规程绑定按 MDM 产品主档身份解析，不再把工单 MES 物料 ID 当绑定 ID。
- A 套两份 Word 按套版本整体校验 FIRST/PATROL 覆盖，每份成员保留自己的工序、项目和来源版本。
- 一线回放、设备选项与提交均按来源规程自身 DCC/版本校验。
- 多个 item-scoped PQC 任务按 `pqcTaskId` 生成独立电子签名主题和幂等键。

## Verification Evidence

- `MesProRouteVersionWorkflowServiceTest`：22 tests PASS。
- `MesFrontlinePqcContextServiceTest`：15 tests PASS。
- `MesFrontlinePqcSubmissionConcurrencyTest`：5 tests PASS。
- `MesProBatchRecordExecutionSignatureServiceTest`：16 tests PASS。
- 组合任务产品主档/多成员定向回归：PASS，含整套缺必需类型负向场景。
- 通用规程与路线迁移静态合同：PASS。
- `pnpm ts:check`：PASS。
- 标准后端 31 模块打包与重启：PASS；48081 health=`UP`，最终运行 PID `40816`。
- Playwright 测试订单：`activeOrderId=1009200080`、`routeVersion=V16`。
- 工序选择器：6 道产品 QA 工序 + `初包装过程检验规程（通用包装）` + `大中包装过程检验规程（通用包装）`。
- 初包装、大中包装均显示真实 Word 解析项目、接收标准、检验方法、检具/抽样信息。
- 通用大中包装任务 `1009244773/1009244849/1009244774/1009244775` 提交业务码均为 `0`，事件 `32629..32632`，签名 `35..38`。
- 清理前只读终核：4 条任务状态均为 `SUBMITTED`；版本 72 / QA 工序 175 进度为总数 90、已提交 4、待提交 86。

## Residual Notes

- 任务自有模拟订单 `1009200079/1009200080` 已通过生产组长真实页面“清理测试单”完成清理；刷新活跃订单池后均不可见，只读数据库复核对应活跃订单和工单均为 `deleted=1`。
- 浏览器首页仍可能显示与本次 MES 链路无关的 DCC 待办错误；目标 PQC 请求和提交已独立通过。
- 本轮未执行 Git 提交或推送。
