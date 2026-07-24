# 统一审批平台长期治理机制

## 新模块接入流程

1. 需求阶段确认是否存在审批任务、审批人、待办、已办、我发起、签名、催办或审计语义。
2. 若存在审批能力，必须先写 BDD 场景和任务文档，明确正式处理页边界。
3. RED：补 `ApprovalModuleIntegrationGuard`、provider、摘要、轨迹、前端跳转和扫描器测试。
4. GREEN：新增 `ApprovalModuleCode`、`ApprovalModuleIntegrationDeclarations` 和唯一 `ApprovalTaskProvider`。
5. 前端只接入统一中心来源识别、摘要、轨迹和正式页跳转，不复制模块审批动作。
6. Playwright 使用真实租户从 `/approval-center` 打开真实任务到正式处理页。
7. 缺真实数据、权限、SLA、催办或外部依赖时记录 blocker，不得 mock 成功。

## 评审流程

每个涉及审批能力的 PR 必须通过 CI / review / design gate：

- CI：运行 `python script\unified_approval\governance_scan.py --backend-root . --frontend-root ..\yudao-ui-admin-vue3 --format json`。
- Review：确认没有新增私有审批中心、私有待办或私有审批状态机。
- Design gate：确认业务详情页、特殊签名、发布、归档、生效回调仍留在模块正式页，不被压进统一中心。
- Test gate：确认 provider 单元测试、统一中心静态契约、跨模块回归和真实 E2E 证据齐全。

## 季度审计流程

每季度执行一次统一审批治理审计：

1. 运行治理扫描器并归档 JSON 输出。
2. 对照 `unified-approval-platform-governance-report.md` 更新已合规模块和待整改模块。
3. 复查隐藏/退休菜单，确认 `/approval-center` 仍是唯一正式跨模块审批任务入口。
4. 抽样验证每个 provider 的 `sourceTaskType/sourceTaskId/businessKey` 能定位真实业务记录。
5. 抽样验证 `TIMELINE`、`AUDIT`、`REMINDER`、`SIGNATURE_AUTHORIZATION`、`EVIDENCE_LEDGER` 声明与真实能力一致。
6. 输出季度风险等级和整改责任人。

## 脚手架与测试模板

新模块不得复制已有私有审批页面作为入口。允许复制的只有以下模板：

- `docs/engineering/unified-approval-platform-adapter-template.md` 的 provider 骨架。
- `script/tests/test_unified_approval_phase7_governance.py` 的扫描器测试模式。
- 已接入模块的 provider 单元测试结构。

新模块必须补齐以下验收清单：

- `ApprovalModuleCode`。
- `ApprovalModuleIntegrationDeclarations`。
- `ApprovalTaskProvider`。
- provider RED/GREEN 测试。
- 前端统一中心静态契约。
- 真实 E2E。
- `execution-log.md` 中的 `BDD`、`RED`、`GREEN` 证据。

## 运营指标

长期巡检至少关注：

- 每个声明 provider 是否可注册并通过启动期 guard。
- 每个模块的待办、已办、我发起或签名待处理查询是否返回真实业务标识。
- 每个声明 `TIMELINE` 的模块是否有真实轨迹证据。
- 每个声明 `REMINDER` 的模块是否有真实 SLA/超时/催办实现。
- 每个失败是否显式暴露，不允许 fallback、mock 成功或静默降级。
