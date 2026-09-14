# Bug Regression Evidence

## Bug

## Summary

“切换填写人”弹窗把候选可选态硬锁为当前登录人，导致 wangxin 可见其他填写人但不能点击；同时弹窗仍调用全量批次详情接口，没有消费执行详情 `assistSwitchTasks` 快照。

## Expected Behavior

执行详情快照返回的可打开填写人候选应可点击；点击后继续调用正式 `openTask`，表单上下文随所选任务刷新，后端保留最终授权校验。

## Reproduction

- `node IntRuoyiFronted\tests\e2e\edhr-switch-filler-selectability-static.spec.js`
- `node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-edhr-assist-filler-switch-snapshot-static.spec.cjs`

## Root Cause

- 前端候选可选态使用 `currentAssistUserId() === item.userId`，把“当前登录人”误当成“可切换填写人”，导致 wangxin 只能点自己。
- 切换弹窗打开时仍依赖全量批次详情重载，没有使用执行详情中冻结的 `assistSwitchTasks` 快照。
- 切换后 active 曾用 route `assistUserId` 字符串与数字 `item.userId` 严格等于，导致已切换到任丹后重开弹窗不高亮任丹。
- 顶部填写人仍优先读当前登录人昵称，未消费 route `fillerName` / `assistUserId` 对应的快照候选名称，导致切换后仍显示王歆。
- 辅助填写行上下文 key 未包含所选 `assistUserId` 时，切换填写人后仍复用旧上下文或空状态，导致“我的填写项”显示未配置辅助模式。
- 传统批记录执行记录未按 `batchExecutionId + taskId` 隔离时，重新创建批次执行可能复用旧执行详情，进而出现缺少填写人快照。

## Regression Test

- `IntRuoyiFronted/tests/e2e/edhr-switch-filler-selectability-static.spec.js`
- `IntRuoyiBackend/yudao-module-mes/src/test/js/mes-edhr-assist-filler-switch-snapshot-static.spec.cjs`

## RED:

- `node IntRuoyiFronted\tests\e2e\edhr-switch-filler-selectability-static.spec.js` -> FAIL，旧实现仍硬锁当前登录人。
- `node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-edhr-assist-filler-switch-snapshot-static.spec.cjs` -> FAIL，旧实现仍调用全量批次详情。

## GREEN:

- `node IntRuoyiFronted\tests\e2e\edhr-switch-filler-selectability-static.spec.js` -> PASS。
- `node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-edhr-assist-filler-switch-snapshot-static.spec.cjs` -> PASS。
- `node doc\tasks\20260728-switch-filler-wangxin-e2e\e2e-artifacts\switch-filler-wangxin-real.e2e.cjs` -> PASS，wangxin 可点击任丹，后端确认 `assistUserId=910181`，顶部填写人显示 `任丹`，辅助填写行 `87` 行，重开弹窗任丹高亮。

## Risk And Scope

- Scope is limited to eDHR auxiliary filler switching on `ExecutionPage.vue`.
- No backend authorization bypass is introduced; `openTask` remains authoritative.

## Verification

- Static contracts, ESLint, backend compile, targeted JUnit and real wangxin Playwright E2E passed; full `pnpm ts:check` is blocked by unrelated existing files; see `verification-report.md`.

## Blockers

- 当前实现和真实 E2E 无阻塞。Full `pnpm ts:check`、提交/推送收尾仍被无关既有类型错误和 Git 边界阻塞。
