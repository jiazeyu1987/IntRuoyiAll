# Test Report

- Task ID: `20260809-batch-record-test-order-allocation-tab`
- Created: `2026-08-09T15:00:22`
- Workspace: `E:\IntRuoyi`
- User Request: `在批记录测试页面新增订单分配 Tab，将确认的工序报工池、FIFO、手动调整、放行锁定、列表历史和并发审计规则整理成可执行验证的测试段落`

## Environment Used

- Evaluation mode: blind-first-pass
- Validation surface: real-browser
- Tools: playwright
- Initial readable artifacts: prd.md, test-plan.md
- Initial withheld artifacts: execution-log.md, task-state.json
- Initial verdict before withheld inspection: yes
- Runtime proof: `http://127.0.0.1:8081/` returned HTTP 200; `http://127.0.0.1:48081/actuator/health` returned `status=UP`; listeners were present on `8081` and `48081`.
- Browser identity label: `芋道源码/admin`; no password, token, cookie, storage state, network trace, or other authentication material is retained in this report or the evidence directory.

## Initial Verdict

- Verdict: `provisionally testable`
- Basis: 在仅阅读 `prd.md`、`test-plan.md` 及允许的运行/E2E规则后，P1-AC1 至 P1-AC6、T1-T5、真实菜单路径、八段规则、静态/类型/浏览器证据要求和阻塞条件均可执行。此结论在读取 withheld 工件前写入本报告，不代表实现已经通过。

## Results

### T1: 订单分配静态合同 RED/GREEN

- Result: passed
- Covers: P1-AC1, P1-AC2, P1-AC3, P1-AC4, P1-AC5
- Command run: `node .\tests\e2e\edhr-batch-record-test-order-allocation-static.spec.cjs`
- Environment proof: `E:\IntRuoyi\IntRuoyiFronted`; exit code 0; output `edhr-batch-record-test-order-allocation-static PASS`.
- Evidence refs: output/playwright/batch-record-test-order-allocation-tab/independent/browser-verification.json, output/playwright/batch-record-test-order-allocation-tab/independent/order-allocation-list-full.png
- Notes: 合同精确覆盖第四个 Tab、稳定锚点/table-key、独立状态、八个唯一 caseName/testScope、完整业务语义、checkpoint.remark、CODE_READONLY 原子入口，以及无裸调 CLI、空 catch 或 mock 成功。

### T2: 相邻三个 Tab 与描述换行回归

- Result: passed
- Covers: P1-AC3, P1-AC4, P1-AC5
- Command run: `node .\tests\e2e\edhr-batch-record-test-tab-static.spec.cjs`; `node .\tests\e2e\edhr-batch-record-test-description-wrap-static.spec.cjs`
- Environment proof: 两条命令均在 `E:\IntRuoyi\IntRuoyiFronted` 退出码 0；分别输出 `edhr-batch-record-test-tab-static PASS` 和 `edhr batch record test description wrap static contract passed`。
- Evidence refs: output/playwright/batch-record-test-order-allocation-tab/independent/browser-verification.json, output/playwright/batch-record-test-order-allocation-tab/independent/order-allocation-1440x900.png
- Notes: 原三个 Tab 的顺序和共享列表合同保持，四张列表数量断言成立；真实页面中长描述按 `white-space: normal` 换行，八行操作按钮均无重叠。

### T3: Vue/TypeScript 类型检查

- Result: passed
- Covers: P1-AC3, P1-AC4, P1-AC6
- Command run: `pnpm ts:check`
- Environment proof: `E:\IntRuoyi\IntRuoyiFronted`; exit code 0; `vue-tsc --noEmit -p tsconfig.relaxed.json` 无类型错误，耗时约 67 秒。
- Evidence refs: output/playwright/batch-record-test-order-allocation-tab/independent/browser-verification.json, output/playwright/batch-record-test-order-allocation-tab/independent/order-allocation-list-full.png
- Notes: 新分类类型、列表元数据、查询/分页/筛选/列配置及共享 CRUD 分派通过类型检查，并在真实页面完成渲染。

### T4: 真实浏览器菜单与订单分配 Tab

- Result: passed
- Covers: P1-AC1, P1-AC2, P1-AC3, P1-AC4, P1-AC6
- Command run: 使用 Playwright CLI 独立 session `br-order-allocation`，从 `http://127.0.0.1:8081` 登录后按 `MES 系统 -> eDHR批记录 -> 批记录测试 -> 订单分配` 操作；每次显著变化后重新 snapshot，并在 `1440x900` 视口执行 screenshot、DOM 布局探针、console 与 pageerror 检查。
- Environment proof: 最终 URL `http://127.0.0.1:8081/mes/pro/feedback/edhr-batch-test`；页面标题 `瑛泰管理系统 - 批记录测试`；Playwright session `br-order-allocation`；视口 `1440x900`；账号标签 `芋道源码/admin`。
- Evidence refs: output/playwright/batch-record-test-order-allocation-tab/independent/browser-verification.json, output/playwright/batch-record-test-order-allocation-tab/independent/order-allocation-1440x900.png, output/playwright/batch-record-test-order-allocation-tab/independent/order-allocation-list-full.png
- Notes: 原三个 Tab 后出现“订单分配”；稳定锚点数量为 1；固定八行及全部描述可见；租户、新增、分页和每行“测试/修改/删除”均可见且启用。八行描述 `scrollWidth=clientWidth`，操作按钮重叠结果均为 false，列表无水平越界。console 共 3 条信息，error=0、warning=0；重载后捕获的 pageErrors=[]。未点击会创建执行批次或业务数据的操作。

### T5: 范围与测试可执行性审查

- Result: passed
- Covers: P1-AC2, P1-AC3, P1-AC5
- Command run: 静态审查 `BatchRecordTestPage.vue`、`edhr-batch-record-test-order-allocation-static.spec.cjs` 和相邻合同，并执行 caseName/testScope 唯一性探针。
- Environment proof: 当前 HEAD 与任务 baseline 均为 `199836c5fc105033898c2df59fb8ca22ac005625`；探针结果 `CaseCount=8`、`UniqueCaseCount=8`、`ScopeCount=8`、`UniqueScopeCount=8`。
- Evidence refs: output/playwright/batch-record-test-order-allocation-tab/independent/browser-verification.json, output/playwright/batch-record-test-order-allocation-tab/independent/order-allocation-list-full.png
- Notes: 任务拥有的产品增量位于现有 `BatchRecordTestPage.vue`，测试增量位于批记录测试静态合同；未新增订单分配后端服务、业务 API、数据库、动态菜单或路由。工作区存在大量其它任务的未提交改动，本结论未将其归属本任务，也未修改或清理这些改动。固定八段文字仅作为未来生产功能的测试合同，不作为生产规则通过证据。

## Final Verdict

- Outcome: passed
- Verified acceptance ids: P1-AC1, P1-AC2, P1-AC3, P1-AC4, P1-AC5, P1-AC6
- Blocking prerequisites:
- Summary: T1-T5 全部通过。订单分配测试定义 UI、八段规则、正式 CODE_READONLY 接线、相邻回归、类型检查和真实菜单浏览器路径均有独立证据；本结论不验证或宣称订单分配生产功能已经实现。

## Open Issues

- None.
