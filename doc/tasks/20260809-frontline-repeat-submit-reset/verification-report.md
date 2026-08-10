# Verification Report

## Result

- 本任务行为验证通过：一线生产正式提交明确成功后，页面结束本次填写会话，清空本次数量、损耗、设备参数和模板 payload，轮换新的客户端草稿键，并恢复“正式提交”、工序选择和员工选择入口。
- 正式写请求仍为一次确认一次调用；失败或响应未明确成功时，复位函数不会从 `finally` 执行，当前输入和原幂等键保留。
- 已提交的后端正式事实、签名、事务和历史不可修改规则未变更。

## BDD And TDD Evidence

- BDD: 成功提交后开始下一次独立报工 -> Given 设备端已选择工序和实际员工并完成填写 / When 正式提交接口明确返回成功 / Then 页面清空本次业务输入、轮换幂等键并恢复可操作状态。
- BDD: 提交失败时保留当前填写会话 -> Given 本次填写已完成 / When 正式接口失败或未明确成功 / Then 页面保留本次输入和原幂等键，不伪造下一次提交会话。
- RED: `node tests/e2e/frontline-production-repeat-submit-static.spec.cjs` -> FAIL，旧实现缺少会话复位函数并由 `formalSubmitResult/isProductionSubmitted` 永久锁页。
- GREEN: `node tests/e2e/frontline-production-repeat-submit-static.spec.cjs` -> PASS。
- GREEN: `node tests/e2e/frontline-formal-submit-static.spec.cjs` -> PASS。

## Regression Evidence

- PASS：`node tests/e2e/frontline-production-fullscreen-submit-confirm-static.spec.cjs`
- PASS：`node tests/e2e/frontline-formal-submit-selected-employee-static.spec.cjs`
- PASS：`node tests/e2e/frontline-production-submit-payload-detail-static.spec.cjs`
- PASS：`node tests/e2e/frontline-production-employee-picker-immediate-close-static.spec.cjs`
- PASS：`node src/views/mes/pro/feedback/frontline-template-switch.spec.cjs`
- PASS：`node tests/e2e/frontline-production-risk-fixes-static.spec.cjs`
- PASS：`node tests/e2e/frontline-production-extra-restrictions-removed-static.spec.cjs`
- PASS：`node tests/e2e/frontline-production-no-device-empty-state-static.spec.cjs`
- PASS：`node tests/e2e/frontline-production-picker-initial-loading-static.spec.cjs`
- PASS：`node tests/e2e/frontline-production-maximize-runtime-cache-static.spec.cjs`
- PASS：`node tests/e2e/frontline-production-device-parameter-range-static.spec.cjs`
- PASS：`node tests/e2e/edhr-frontline-production-pixel-parity-static.spec.cjs`
- PASS：`node tests/e2e/edhr-frontline-production-prototype-parity-static.spec.cjs`
- PASS：`pnpm ts:check`
- PASS：`git diff --check -- <本任务文件和经验文档>`
- PASS：`validate_change_request.py --evidence docs/changes/20260809-frontline-repeat-submit-reset.md`
- PASS：`validate_frontend_feature.py --evidence doc/tasks/20260809-frontline-repeat-submit-reset/frontend-feature-evidence.md`

## Isolated Existing Failure

- `node tests/e2e/edhr-frontline-production-fullscreen-toggle-static.spec.cjs` 失败在既有“生产工序选择器立即关闭”正则：合同期望 `const shouldClosePickerImmediately = !isPqcMode.value` 且直接调用 `selectFrontlineProcess(deviceState, process)`，当前基线实现为 `true` 和 `selectedProcess`。失败代码区不包含本任务的成功复位、幂等键或提交按钮改动，按前端静态契约隔离门禁记录为非本任务阻塞，未修改该并行行为。

## Real E2E

- PASS：项目 Playwright 通过真实登录、生产填写、员工签名确认和正式提交路径，在同一页面连续完成四轮，期间主 frame 导航次数为 `0`，起止 URL 完全一致。
- 四轮完成数量分别为 `5/6/7/8`，设备参数分别为 `15/16/17/18`；每轮成功后两个输入均恢复为空，正式提交、工序选择和员工选择继续可用。
- 四轮反馈 ID 为 `881/882/883/884`，工序池事件 ID 为 `195/196/197/198`；四个幂等键均为 45 字符且互不重复。
- 独立数据库复核摘要为 `4|4|4|45|4`，确认四个事件、四个批次执行签名、四个唯一键及四组反馈映射一致。
- 当前无工单正式链路未生成可选记录本回执，符合 API 的可选字段合同；若任一轮返回记录本回执，脚本仍会校验其一致性和唯一性。
- 浏览器诊断无 page error、目标请求失败或目标 HTTP 错误；第四轮后截图已人工确认复位状态。
- 结果：`output/playwright/20260809-frontline-repeat-submit-reset/frontline-repeat-submit-result.json`；截图：`output/playwright/20260809-frontline-repeat-submit-reset/frontline-repeat-submit-after-fourth.png`。

## Different Employee And Process E2E

- PASS：补齐任务自有正式候选后，真实页面运行态包含 2 名正式员工、2 道正式工序；四轮按“工序 A + 员工 A、工序 B + 员工 A、工序 A + 员工 B、工序 B + 员工 B”依次选择并签名提交。
- 四轮始终位于 `http://127.0.0.1:8081/mes/pro/feedback/edhr-batch-production-fill`，主 frame 导航次数 `0`，未刷新、未重新登录。
- 正式回执为 feedback `890/891/892/893`、工序池事件 `204/205/206/207`、签名 `3409/3410/3411/3412`；四个幂等键均唯一且长度为 45。
- 独立数据库复核 `4|4|2|2|2|4|4|45|4|4|4`，证明四组事件/反馈唯一、覆盖两路线工序/两 MES 工序/两实际员工，签名主体与报工人逐轮匹配实际员工，密码验证全部通过。
- 每轮成功后完成数量与设备参数恢复为空，人员/工序选择和正式提交恢复可用；第四轮截图显示第 2 工序、第 2 员工仍被选中，数量为空、损耗为 0。
- 结果：`output/playwright/20260809-frontline-repeat-submit-reset/combinations/frontline-repeat-submit-combinations-result.json`；截图：`output/playwright/20260809-frontline-repeat-submit-reset/combinations/frontline-repeat-submit-combinations-after-fourth.png`。
- 两次 harness 修正前已明确成功的正式事实 `event=199` 与 `event=200..203` 均按不可修改审计要求保留，未删除、覆盖或计入最终四轮 PASS。

## Defect Found And Fixed During Verification

- 首次签名通过后的真实提交因 `event_idempotency_key varchar(128)` 超长而在后端事务内回滚；未形成部分正式事实。
- 根因是客户端将路线、工序、工作站、员工等冗长上下文与 draft token 拼接。修复为固定短前缀加 draft token，并在请求前显式校验 128 字符上限。
- 修复先取得聚焦静态合同 RED，随后聚焦合同、14 项相邻合同、`pnpm ts:check` 和真实四连提全部 GREEN。
- bug/frontend/database evidence validators 与 validator self-tests 均 PASS；任务范围 `git diff --check` PASS。

## Experience Consolidation

- `project-experience-consolidation` 已将连续独立提交的“成功后复位并换键、失败保留原会话”规则合并到 `docs/frontend-development.md` 和 `docs/backend-development.md`，并更新 `docs/experience-index.md` 路由；未新建长期经验文档。
- 本轮又将“客户端幂等键必须遵守后端长度预算、使用短键并在请求前 fail fast”合并到同一前端门禁及经验索引。
- M6 将“先盘点页面运行态的去重员工/工序组合，正式 fixture 同时补齐员工档案、人员范围和签名审计，逐轮核对页面选择与落库事实”合并到 `docs/e2e-rules.md#写入型-e2e-任务自有模拟环境门禁`。
- M6 将“每次明确成功后先持久化回执，再执行页面/诊断断言；harness 后置失败先只读分类、不得盲目重放”合并到 `docs/e2e-rules.md#写入型-e2e-响应不确定断点恢复门禁`。

## Final Independent Verification

- PASS：`node --check doc/tasks/20260809-frontline-repeat-submit-reset/frontline-repeat-submit-combinations-real.e2e.cjs`。
- PASS：`node tests/e2e/frontline-production-repeat-submit-static.spec.cjs`。
- PASS：`node tests/e2e/frontline-formal-submit-static.spec.cjs`。
- PASS：`pnpm ts:check`。
- PASS：database schema evidence validator 及 validator self-test。
- PASS：任务范围 `git diff --check`，仅有工作区既有 LF/CRLF 提示，无空白错误。
- PASS：最终机器证据门禁确认 4 轮、2 路线工序、2 MES 工序、2 员工、4 个唯一幂等键、主 frame 导航 0，且结果 JSON 与截图均存在。

## Remaining Blockers

- 本任务范围内无 blocker。

## Cleanup

- M6 最终 `task-closeout-cleanup` preview/apply -> PASS，无 blocked 或 warning。
- 已删除 `apply-combination-e2e-fixture.sql`、`database-schema-evidence.md` 和 `frontline-repeat-submit-combinations-real.e2e.cjs` 三个任务临时产物；保留 `task.md`、`execution-log.md`、`verification-report.md`、生产实现、正式静态回归及 Playwright 结果和截图。
- 状态更新为 `completed` 后再次 preview：`delete=<none>/blocked=<none>/warnings=<none>`。
