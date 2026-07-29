# Execution Log

## 2026-07-28

- User intent: 粗洗工序除了 `MAIN` 批记录表单还有附加表单/表单槽位，附加表单也有对应填写人；切换填写人必须覆盖附加表单。
- BDD: 粗洗工序切换填写人显示附加表单候选 -> Given wangxin 打开粗洗工序辅助填写页且该工序存在 `MAIN` 批记录表单和附加表单填写人 / When 打开“切换填写人” / Then 弹窗列出两个载体的正式候选且附加表单候选可点击。
- BDD: 切换附加表单填写人刷新表单上下文 -> Given wangxin 在弹窗选择附加表单的其他填写人 / When 后端 `task/open` 成功 / Then URL query、当前填写人、表单标题、槽位和内容上下文均切到所选附加表单任务。
- GREEN: experience-preflight -> PASS，已读取任务关闭、后端、前端、E2E、本地运行态、PowerShell/Git、编码规则和经验索引，并写入适用门禁。
- Preflight: git status -> `int_main...origin/int_main [ahead 4]`，存在既有脏改动：`MesProBatchRecordRuntimeSnapshotSupport.java`、`doc/tasks/20260728-edhr-batch-record-design-docs/task.md`、`doc/tasks/20260728-rename-product-master-tab/*`。
- Next: 独立提交既有脏改动基线后，新增 RED 静态合同和后端测试。

- RED: `node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-edhr-assist-filler-switch-snapshot-static.spec.cjs` -> FAIL，执行详情切换快照仍未包含 `buildAssistSwitchProcessFormRuleMap`，说明只依赖 active workTask 快照会漏附加表单候选。
- RED: `mvn -pl yudao-module-mes "-Dtest=MesProEdhrWorkTaskServiceImplTest#createInitialFillTask_createsAllCompanionTasksForSameProcess,MesProBatchRecordExecutionServiceImplTest#buildResp_assistSwitchTasksIncludesExtraFormFillersFromProcessRuleWithoutWorkTask" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，`createInitialFillTask` 只创建 MAIN + optional companion，缺少同工序必填附加表单；执行详情测试先被夹具 `batch_record_sort` 唯一键冲突阻断。
- Implementation: 修正执行详情切换快照候选来源，按 active workTask、过程表单填写规则、工序填写规则、路线绑定候选源解析 `fillableUsers`；保留 `available/allowedActions/activeWorkTaskId` 只来自真实 active workTask。
- Implementation: 修正同工序 companion 填写任务生成，不再只派发 `requiredFlag=false` 的可选附加表单，同工序必填附加表单也生成真实 `FILL` workTask。
- Implementation: 修正新增后端 JUnit 夹具，为 MAIN 和附加表单任务设置不同 `batchRecordSort`，避免测试被唯一键冲突误挡。
- GREEN: `node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-edhr-assist-filler-switch-snapshot-static.spec.cjs` -> PASS。
- GREEN: `node IntRuoyiFronted\tests\e2e\edhr-switch-filler-selectability-static.spec.js` -> PASS。
- GREEN: `git diff --check -- <本任务后端/测试文件>` -> PASS，仅报告 Windows CRLF 工作区警告，无 whitespace error。
- BLOCKED: `mvn -pl yudao-module-mes "-Dtest=MesProEdhrWorkTaskServiceImplTest#createInitialFillTask_createsAllCompanionTasksForSameProcess,MesProBatchRecordExecutionServiceImplTest#buildResp_assistSwitchTasksIncludesExtraFormFillersFromProcessRuleWithoutWorkTask" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> BLOCKED，编译阶段先失败于非本任务脏文件 `MesProBatchRecordRouteGenerationServiceImpl.java` 第 263、272 行未转义 JSON 字符串；影响：目标 JUnit、模块 compile、真实 E2E 都不能作为 GREEN 结论。
- GREEN: stale blocker复验 `mvn -pl yudao-module-mes "-Dtest=MesProEdhrWorkTaskServiceImplTest#createInitialFillTask_createsAllCompanionTasksForSameProcess,MesProBatchRecordExecutionServiceImplTest#buildResp_assistSwitchTasksIncludesExtraFormFillersFromProcessRuleWithoutWorkTask" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 2, Failures: 0, Errors: 0, Skipped: 0。
- BLOCKED: 真实 E2E `node doc/tasks/20260728-switch-filler-extra-form-candidates/e2e-artifacts/switch-filler-extra-form-wangxin-real.e2e.cjs` 仍未 GREEN；`real-e2e-evidence.md` 记录 `no_wangxin_extra_form_switch_sample_found`，当前数据未找到可验证的 wangxin 附加表单切换样本。

## 2026-07-29

- User scope change: MES 全量测试不再作为当前任务完成门禁；仅按本任务开发文档和测试计划列出的内容做定向测试与验证，不把未运行全量写成通过。
- BDD: eDHR 动态表单切换后不依赖模板管理权限 -> Given wangxin 已通过“切换填写人”选择同工序附加 FormCenter 表单候选且 `task/open` 已完成业务授权 / When 批次详情自动打开动态表单抽屉 / Then 抽屉使用 `task/open` 响应里的模板渲染快照，不再调用需要 `form:template:query` 的模板管理查询作为必要前置。
- Finding: 真实 E2E 已证明附加表单候选可见可选、`workTaskId=2296` 和 `assistUserId=152` 能通过 `task/open`；后续卡在 `/admin-api/form-center/templates/25/versions/V2.0` 返回业务 `403 没有该操作权限`，这是 FormCenter 模板管理权限被运行态抽屉误用，不是候选人链路失败。
- RED: `node tests\e2e\edhr-switch-filler-formcenter-slot-static.spec.js` -> FAIL，批次详情未把 `task/open` 返回的 `formTemplateJimuSchemaJson/formTemplateRecognizedFields` 传给 `ActionFormPanel`。
- RED: `mvn -pl yudao-module-mes "-Dtest=MesProEdhrBatchExecutionServiceTest#openTask_allowsApprovedDynamicRouteFormBeforeCloseForCurrentFiller" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，测试编译失败于 `EdhrBatchExecutionTaskOpenRespVO` 缺少 `getFormTemplateJimuSchemaJson/getFormTemplateRecognizedFields`，证明后端 openTask 契约未提供动态表单渲染快照。
