# AC-M03 ERP 候选数据同步补证

## Task Goal

分析并补齐 AC-M03「同步 ERP 候选数据」从当前部分实现到可验证实现的差距，优先证明订单、调拨、发货、批次按正式 ID 幂等同步，且重复、乱序或冲突来源不生成重复事实。

## Milestones

- [x] 建立 AC-M03 专项任务档案和 BDD/TDD 门禁。
- [x] 核对当前 ERP 生产订单同步、调拨/批次追溯实现和既有测试覆盖。
- [x] 先写 RED，复现重复、乱序或冲突来源会生成重复事实或缺少正式失败证据的缺口。
- [x] 实施最小正式修复，不引入 fallback、默认成功或吞异常。
- [x] 运行目标后端测试和必要静态/契约验证。
- [x] 更新 AC-M03 当前状态、剩余 M6/E2E blocker 和下一步建议。

## Expected Verification

- `mvn -pl yudao-module-mes -am "-Dtest=<AC-M03 target tests>" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- 若修改 E2E/静态合同，再运行对应 `node --check` 或 `pnpm --dir IntRuoyiFronted ...` 定向命令。
- 后端 API evidence validator：`python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260805-ac-m03-erp-candidate-sync/backend-api-evidence.md`

## Applicable Gates

- 后端服务改动必须遵守 BDD + 严格 TDD，先 RED 后 GREEN。
- ERP/调拨/批次事实必须来自正式 ID 和正式源记录；禁止用空值、默认 ID、mock、API-only 成功或前端文案替代同步事实。
- 缺少 schema、测试 fixture、正式来源字段或运行依赖时必须 fail fast，并记录影响。
- Maven `-Dtest` 参数在 PowerShell 中必须加引号；若 Windows Maven target 文件系统异常，必须记录诊断，不能把无报告超时当通过。
- 当前工作区已有非本任务脏改动；本任务不得回滚、覆盖或混入无关改动。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；目标是以正式 ID 幂等、冲突检测和测试证据补齐 AC-M03。
- `是否存在临时补丁或绕过`：否。

## Current Status

ready_for_closeout

已完成 AC-M03 后端最小正式修复和定向验证；AC-M03 仍不能声明 ACCEPTED，因为角色需求矩阵 M6 真实 E2E 覆盖账本尚未补入并复跑。
