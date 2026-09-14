# Backend API Evidence

## Scope

- Service scope: `MesProBatchRecordExecutionServiceImpl.buildAssistSwitchTasksSnapshot(...)` and `MesProEdhrWorkTaskServiceImpl.createInitialFillTask(...)`.
- Contract: eDHR 执行详情 `assistSwitchTasks` 必须覆盖同工序 `MAIN` 批记录表单和附加表单/表单槽位候选；候选来源按真实后端来源解析，不由前端补齐。
- Auth behavior: `openTask` 仍是最终授权入口；切换快照只提供候选展示，`available/allowedActions/activeWorkTaskId` 仍必须来自真实 active workTask。
- Data contract: 候选优先级为 active workTask `candidateUserSnapshot`、过程表单填写规则、工序填写规则、路线绑定 `candidateSourceType/candidateSourceIds`。

## BDD

- BDD: 粗洗工序显示附加表单候选 -> Given 当前工序存在 MAIN 和附加表单任务 / When 执行详情构建 `assistSwitchTasks` / Then 附加表单任务返回正式 `fillableUsers`。
- BDD: 附加表单可真实打开 -> Given 同工序附加表单需要填写 / When 初始或推进当前工序填写任务 / Then 后端创建真实 `FILL` workTask，前端只能通过该 workTask 打开。

## Validation

- RED: `node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-edhr-assist-filler-switch-snapshot-static.spec.cjs` -> FAIL，缺少过程表单规则和路线绑定候选源。
- RED: `mvn -pl yudao-module-mes "-Dtest=MesProEdhrWorkTaskServiceImplTest#createInitialFillTask_createsAllCompanionTasksForSameProcess,MesProBatchRecordExecutionServiceImplTest#buildResp_assistSwitchTasksIncludesExtraFormFillersFromProcessRuleWithoutWorkTask" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，必填 companion 附加表单未生成 active workTask；执行详情夹具唯一键冲突已修正。
- GREEN: `node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-edhr-assist-filler-switch-snapshot-static.spec.cjs` -> PASS。
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProEdhrWorkTaskServiceImplTest#createInitialFillTask_createsAllCompanionTasksForSameProcess,MesProBatchRecordExecutionServiceImplTest#buildResp_assistSwitchTasksIncludesExtraFormFillersFromProcessRuleWithoutWorkTask" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 2, Failures: 0, Errors: 0, Skipped: 0。

## Verification

- Frontend static contract: `node IntRuoyiFronted\tests\e2e\edhr-switch-filler-selectability-static.spec.js` -> PASS。
- Whitespace check: `git diff --check -- <本任务后端/测试文件>` -> PASS。
- Module compile and target JUnit blocker resolved by the route generation JSON compile fix.

## Blockers

- 真实 Playwright E2E 仍阻塞：`real-e2e-evidence.md` 记录 `no_wangxin_extra_form_switch_sample_found`，未找到可验证的 wangxin 附加表单切换样本。
- 影响范围：真实页面闭环暂不能作为最终 GREEN。

## No-Fallback Check

- 未引入当前登录人兜底。
- 未引入空列表默认成功。
- 未让前端伪造候选人或 `workTaskId`。
- 缺少 active workTask 时，任务仍不可点击；必须由后端工作任务生成链路提供真实可打开上下文。
