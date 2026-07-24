# 任务：电子签名治理增强前端代码实施

## 目标

配合后端任务 `20260528-signature-governance-implementation`，在同名 worktree 和分支中实现电子签名治理增强前端页面、API client、路由和每个功能点的 E2E 测试。

## 范围

- 分支：`codex/20260528-signature-governance-docs`
- 后端主任务目录：`D:\ProjectPackage\Int\IntRuoyi\worktrees\20260528-signature-governance-docs\ruoyi-vue-pro\doc\tasks\20260528-signature-governance-implementation`
- 本前端任务目录用于记录前端 worktree 状态；详细计划以主任务目录为准。

## 放行标准

- 前端代码必须遵守 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`。
- 每个功能点必须有 E2E 测试用例。
- 不允许前端 mock 成功、吞掉后端 blocker 或直接拼接生产表数据形成审阅结果。

## Milestone List

- SG-GOV-FE-REVIEW-REAL-SOURCE：周期审阅 UI 必须允许录入至少一条真实 review projection source，并提交给后端 `projections`。
- SG-GOV-FE-CSV-GATE-REAL-INPUTS：CSV release gate UI 必须允许录入材料、追溯关系、培训记录、变更控制、QA 批准信息，并提交给后端对应 payload。
- SG-GOV-FE-E2E-FAIL-FAST：周期审阅和 CSV E2E 必须要求真实测试数据环境变量；缺少真实样本时直接 fail-fast，不提交空数组或 mock 成功。
- SG-GOV-FE-DOC-EVIDENCE：补齐前端任务 milestone、expected verification、BDD/RED/GREEN/BLOCKER 证据。

## Expected Verification

- `node scripts\signature-governance-page-contract.test.mjs` 必须通过，确认工作台不再固定提交 `projections: []`、CSV 空数组或 `qaApproval: undefined`。
- `node tests\e2e\signature-governance-e2e-static.spec.js` 必须通过，确认真实 E2E helper 要求 review projection 与 CSV 质量包环境变量并覆盖填表路径。
- `node --check tests\e2e\signature-governance-real-flow-helper.js`、`node --check tests\e2e\signature-governance-periodic-review.e2e.js`、`node --check tests\e2e\signature-governance-csv-package.e2e.js` 必须通过。
- `npm run ts:check` 必须通过。
- 在缺少真实样本环境变量时，`node tests\e2e\signature-governance-periodic-review.e2e.js` 和 `node tests\e2e\signature-governance-csv-package.e2e.js` 必须 fail-fast 并列出缺失变量。

## 当前状态

- 状态：completed
- 已完成：SG-GOV-00 前端共享契约 `src/api/signature-governance/shared.ts` 与静态 RED/GREEN 测试。
- 已完成：SG-GOV-API-FE 前端 TypeScript API client，覆盖留存预检、周期审阅批次、CSV release gate、当前统一策略四个后端入口。
- 已完成：SG-GOV-WORKBENCH-FE 电子签名治理工作台页面和隐藏路由，覆盖长期留存、周期审阅、CSV质量包、统一策略四个入口。
- 已完成：SG-GOV-E2E-GATE 四个真实 E2E 脚本与共享 helper 已建立，覆盖 retention/recovery、periodic review、CSV package、policy 四条路径。
- 已完成：SG-GOV-E2E-TENANT-GATE 增加实际登录租户校验，防止配置测试租户但页面仍用默认租户时误判 E2E 通过。
- 验证：`node scripts\signature-governance-shared-contract.test.mjs` PASS；`node scripts\signature-governance-page-contract.test.mjs` PASS；`node tests\e2e\signature-governance-e2e-static.spec.js` PASS；`npm run ts:check` PASS；SG-GOV-API-FE 导出静态检查 PASS。
- 已完成：测试租户账号 `aoteman/admin123` 已通过可见租户控件校验，E2E 能进入统一策略真实请求。
- 已完成：SG-GOV-FE-REVIEW-REAL-SOURCE 周期审阅页面新增真实投影样本输入，提交 `reviewForm.projections`，缺失来源表、来源ID、来源Hash、动作、含义时页面 fail-fast。
- 已完成：SG-GOV-FE-CSV-GATE-REAL-INPUTS CSV 质量包页面新增材料、追溯关系、培训记录、变更控制、QA 批准输入，提交真实数组和 `qaApproval` 对象，缺失任一必需样本时页面 fail-fast。
- 已完成：SG-GOV-FE-E2E-FAIL-FAST 周期审阅和 CSV E2E helper 增加真实样本环境变量门禁，并覆盖对应 UI 填表路径。
- 已完成：SG-GOV-FE-POLICY-READY 前端策略类型和 E2E 断言增加 `ready: boolean`、`moduleStatuses`，统一策略页只接受后端明确 `READY/ready=true`。
- 已完成：四条真实 Playwright E2E 已在当前 worktree 前端 `http://127.0.0.1:18198`、当前 worktree 后端 `http://127.0.0.1:48198`、测试租户 `测试租户 / aoteman / admin123` 下通过。
- 验证：`node scripts\signature-governance-page-contract.test.mjs` PASS，3 tests；`node tests\e2e\signature-governance-e2e-static.spec.js` PASS；`node --check` helper 和四个 E2E 入口 PASS；`npm run ts:check` PASS；retention/recovery、periodic review、CSV package、policy 四条真实 E2E PASS。
- 已完成：`task-closeout-cleanup` preview 已执行；preview 识别 runtime 日志和 `test-results/signature-governance/` 为清理候选，本轮已按安全路径校验手动删除临时产物。
- 已完成：最终 reviewer 子 agent Round 4 放行，`final_decision: pass`。
- 已完成：本任务直接改动已准备随 task-specific commit 提交。

## 当前 release decision

- 结论：GO for task-specific commit。
- 实现验证：PASS。前端工作台、类型检查、静态 E2E 约束和四条真实 Playwright E2E 均已通过。
- 收尾状态：`task-closeout-cleanup` preview 已执行，临时产物已删除；最终 reviewer 子 agent 已放行；本文件随 task-specific commit 一并提交。worktree 快进合并/删除被当前分支不能 fast-forward merge into `int_main` 阻塞，不在本文档中声明已完成。
- superseded：前期关于缺少 E2E 环境变量、错误后台端口、缺少菜单权限、缺少 `ready` 字段和缺少真实样本输入的 blocker，均已由后续 PASS 证据 superseded。

## Cleanup Keep

- `doc/tasks/20260528-signature-governance-implementation/task.md`
- `doc/tasks/20260528-signature-governance-implementation/execution-log.md`
- `doc/tasks/20260528-signature-governance-implementation/frontend-feature-evidence.md`
- `doc/tasks/20260528-signature-governance-implementation/qa-test-suite-evidence.md`

## Cleanup Candidates

- `doc/tasks/20260528-signature-governance-implementation/runtime/`
- `test-results/signature-governance/debug-retention-page.png`
- `test-results/signature-governance/retention.json`
- `test-results/signature-governance/periodic-review.json`
- `test-results/signature-governance/csv-package.json`
- `test-results/signature-governance/policy.json`

## Closeout Preview

- 2026-05-28 15:55 +08:00：`task-closeout-cleanup` preview 已执行。
- preview 保留核心任务证据，识别 runtime 日志和 `test-results/signature-governance/` 为删除候选。
- 已按路径安全校验删除上述临时产物。
- apply/快进合并清理被阻塞：当前分支不能 fast-forward merge into `int_main`；该 worktree 合并/删除不在本文档中声明完成。
