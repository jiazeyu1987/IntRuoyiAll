# 测试计划：可信时间最小闭环

## BDD Scenarios

- BDD-01：业务时间不覆盖正式签名时间。
- BDD-02：chrony 正常时保存真实偏差并 PASS。
- BDD-03：chrony 不可验证时 BLOCKED/NO_GO。
- BDD-04：导出只读取指定巡检，不重新巡检。
- BDD-05：异常报告不得显示通过。
- BDD-06：HTML、JSON 与 SHA-256 清单一致。
- BDD-07：未配置偏差阈值时不做数值超限判定，但仍采集偏差且其它异常继续阻断。

## RED

- 签名时间覆盖测试先失败。
- Runtime Control 缺少时间检查测试先失败。
- 巡检 ZIP 导出合同测试先失败。
- 前端缺少状态和按钮测试先失败。
- 未配置阈值仍被整体阻断的测试先失败。

## GREEN

- 每个 milestone 只实现使当前 RED 转绿的最小代码。

## Backend Commands

```powershell
mvn -pl yudao-module-mes -am -Dtest=MesProBatchRecordExecutionSignatureServiceTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl yudao-module-infra -am -Dtest=RuntimeOpsTrustedTime*Test,RuntimeOpsInspection*Test -Dsurefire.failIfNoSpecifiedTests=false test
```

## Frontend Commands

```powershell
pnpm ts:check
node tests/e2e/runtime-control-trusted-time-static.spec.js
```

## E2E

仅在用户当轮明确授权后，Playwright 真实登录并点击巡检与导出按钮；API 仅做最终只读核验。

## Test Data

- 正常、无选中源、Leap 异常、配置阈值后偏差超限、未配置阈值和命令失败 chrony 文本。
- 带 `selectedSignedAt` 的签名对象。
- PASS 与 BLOCKED 巡检各一条。

## Failure Paths

- NTP 地址或远程执行配置缺失；正式环境阈值未恢复。
- chronyd 未运行、SSH 不可达、巡检 ID 不存在、ZIP 写入失败、下载无权限。
