# P0 生产执行主闭环实现前置门禁

## Purpose and Scope

本文档用于确保后续开发按 P0 BDD/TDD 文档实施时，可以真实达到“谁、在哪台设备、做了哪个工序、做了多少、质量结果怎样、签名是谁、进入哪个生产工单、班组长是否复核、最后如何进入批记录追溯”的目标。它不是新范围，只是实现前必须通过或明确阻塞的 M0 门禁。

## M0 Required Gates

| Gate | 必查项 | 通过标准 | 阻塞处理 |
| --- | --- | --- | --- |
| P0-M0-01 | 后端命令工作目录 | 所有 Maven 命令在 `E:\IntRuoyi\IntRuoyiBackend` 执行，或显式使用 `-f IntRuoyiBackend/pom.xml`。 | 命令只因工作目录错误失败时，不得记录业务 RED；先修正文档/脚本。 |
| P0-M0-02 | 前端脚本入口 | `IntRuoyiFronted/package.json` 存在 `e2e:p0-production-execution-loop:static` 和 `e2e:p0-production-execution-loop:real`。 | 当前脚本缺失应作为第一个前端 RED，不能把 `ERR_PNPM_NO_SCRIPT` 当业务验证结果。 |
| P0-M0-03 | 前端 spec 文件 | 存在 P0 static spec 和 real E2E spec，且脚本指向这些正式文件。 | 缺 spec 时记录 E2E 前置 blocker，不得用 API wrapper 或旧脚本冒充。 |
| P0-M0-04 | 事件身份 | 生产提交、PQC 提交、复核、分配、完成、批记录字段审计都能保存正式来源 ID。 | 缺来源 ID 的链路不得进入 GREEN；trace 必须 `BLOCKED`。 |
| P0-M0-05 | 批记录正式来源 | 已核对逐工序批记录表单绑定、字段映射和目标单元格。 | 缺绑定或映射时阻塞；禁止使用 `formBindings`、默认 `MAIN` 或工序开始配置替代。 |
| P0-M0-06 | 电子签名接口 | 生产提交、PQC 提交、生产组长复核、PQC 组长复核均有正式签名字段和测试签名能力。 | 缺签名接口或测试能力时阻塞；禁止用登录人、备注或确认弹窗替代。 |
| P0-M0-07 | 质量可分配状态 | 后端白名单明确哪些质量状态可进入 FIFO。 | 未冻结白名单时，除明确 PASS 外全部不可分配。 |
| P0-M0-08 | 真实 E2E 数据 | 测试租户、账号、活跃订单、PQC 任务、QA 规程、设备、工作站、批记录绑定、字段映射和清理方案已确认。 | 任一缺失时真实 E2E `BLOCKED`，不改用 mock、SQL 或 API-only。 |

## Command Conventions

```powershell
# 后端定向测试：工作目录必须是 E:\IntRuoyi\IntRuoyiBackend
mvn -pl yudao-module-mes -am "-Dtest=<TestClassOrPattern>" test

# 前端静态合同和真实 E2E：工作目录必须是 E:\IntRuoyi\IntRuoyiFronted
pnpm e2e:p0-production-execution-loop:static
pnpm e2e:p0-production-execution-loop:real
```

PowerShell 命令不得使用 `&&`。若需要连续执行，逐条运行并分别记录退出码；不得用后一个 PASS 掩盖前一个 FAIL。

## Development Entry Order

1. 先完成 P0-M0 前置门禁，尤其是脚本/spec、事件身份和签名接口冻结。
2. 后端先写 RED：PQC 入工序池事件、复核签名 schema、复核签名服务、统一 trace。
3. 后端 GREEN 只能做最小正式链路，不得加 fallback、默认成功或 mock。
4. 前端先写 static RED：真实入口、请求字段、签名字段、trace 页面、错误展示。
5. 最后跑真实 E2E；若真实数据或入口缺失，记录 `BLOCKED` 和解除条件，不得改写成 PASS。

## Verification Evidence Requirements

- 每条 RED 必须包含命令、工作目录、退出状态和预期失败原因。
- 每条 GREEN 必须复跑同一命令并 PASS。
- E2E PASS 必须包含真实页面路径、测试租户标签、数据前缀、目标写请求数量、只读核验结果和清理/保留证据。
- E2E BLOCKED 必须包含缺失前置、影响范围和解除条件。
- 验证报告不得记录密码、token、cookie、Authorization、电子签名密码或私钥。

## Non-Negotiable Blockers

- 无 `processPoolEventId` 或正式事件关联 ID。
- PQC 结果没有工序池质量事件。
- 复核或确认分配没有正式电子签名。
- FIFO 使用非活跃订单、质量未知数量或默认合格。
- 批记录回填不是正式逐工序批记录表单绑定。
- trace 需要页面拼接、备注文本、默认槽位或截图才能回答 P0 审计问题。
