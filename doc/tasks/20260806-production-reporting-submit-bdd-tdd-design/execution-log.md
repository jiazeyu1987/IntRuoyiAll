# Execution Log

## User Intent

- 用户要求先按已确认方案编写 TDD + BDD 设计文档。
- 需求口径：删除班组长报工管理截图红框中的 `生产工单`、`PQC`、`提交内容`列；报工表需包含图 2 提交的完整参数；通用字段包括工序、员工、完成数量、损耗数量；动态字段包括损耗原因及对应数量、选用设备、设备参数；设备参数超上下限允许提交但数值标红；损耗数量必须等于所有损耗原因数量之和。

## Context Evidence

- 读取 `C:\Users\BJB110\.codex\skills\bdd-tdd-acceptance-planner\SKILL.md` 和 `references\acceptance-structure.md`。
- 读取 `docs\task-closeout-rules.md`、`docs\powershell-encoding.md`、`docs\powershell-memory.md`。
- 读取 `docs\experience-index.md` 后命中 `docs\e2e-rules.md#规划型-e2e-前置与业务-red-分离门禁`。
- 定位班组长报工管理表：`IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue` 当前默认列包含 `workOrderCode` / `pqcResult` / `submissionContent`。
- 定位一线生产报工面板：`IntRuoyiFronted/src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue` 当前已有完成数量、损耗数量、不良明细、设备与参数输入，但提交 payload 仍只传首个 `lossReasonId`。
- 定位前端运行态配置契约：`IntRuoyiFronted/src/api/mes/pro/feedback/index.ts` 暴露 `FrontlineRuntimeConfigVO.devices`、`defectReasons` 和参数上下限字段。
- 定位班组长配置接口：`MesProcessPoolTeamLeaderController` 提供损耗原因、工序设备绑定、运行态设备参数规则保存接口。
- 定位后端提交链路：`MesProFrontlineFeedbackSubmitServiceImpl` 校验输出/损耗数量，`MesFrontlineLossReasonValidatorImpl` 按 `routeProcessId` 校验单个损耗原因。
- 定位既有后端证据：`MesProFrontlineFeedbackRawLimitBypassTest` 已覆盖超限设备参数不裁剪、不拒绝并保留原始值。

## BDD Markers

- BDD: 生产报工管理列表拆分提交内容 -> Given 班组长进入生产组长报工管理页签，When 列表加载生产报工记录，Then 不显示 `生产工单`、`PQC`、`提交内容`三列，并显示完成数量、损耗数量、损耗明细、选用设备、设备参数及参数异常状态。
- BDD: 一线生产报工提交完整 payload -> Given 员工在当前工序填写完成数量、多个损耗原因数量、选择设备并填写参数，When 提交报工，Then 后端保存每个损耗原因及数量、所选设备、每个设备参数值、上下限、单位和异常标记。
- BDD: 设备参数超限允许提交且标红 -> Given 班组长配置压力上限为 40，When 员工提交压力 50，Then 页面允许提交成功，报工管理列表和详情中压力 50 显示为红色异常值。
- BDD: 损耗数量等于原因明细合计 -> Given 员工填写总损耗数量和多个损耗原因数量，When 明细合计不等于总损耗，Then 前端提交前提示并且后端拒绝不一致 payload。
- BDD: 工序配置按当前工序作用域读取 -> Given 两个工序可能配置相同或不同的损耗原因、设备和参数，When 员工切换工序，Then 页面只使用当前 `routeProcessId/processId` 下的正式配置，不能跨工序推断或补齐。

## RED / GREEN Evidence Template

- RED: `node tests/e2e/team-leader-production-report-payload-columns-static.spec.cjs` -> FAIL, 当前生产组长报工管理仍包含红框列且缺少原子提交字段列。
- RED: `node tests/e2e/frontline-production-submit-payload-detail-static.spec.cjs` -> FAIL, 当前提交 payload 只包含单个 `lossReasonId`，不能表达多个损耗原因及对应数量。
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProFrontlineFeedbackSubmitDetailContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 后端尚未校验损耗明细合计与工序配置作用域。
- GREEN: 以上命令 -> PASS，生产实现完成后记录真实输出摘要。

## Verification Evidence

- 结构化验收设计校验：`python -X utf8 -c "<copy task acceptance docs into temp docs/acceptance; run validate_acceptance_plan.py>"` -> PASS，输出 `BDD/TDD acceptance plan validation passed.`。
- UTF-8 读取校验：`python -X utf8 -c "<read task.md, execution-log.md, bdd-scenarios.md, tdd-plan.md, e2e-plan.md, test-data.md>"` -> PASS，全部文件可按 UTF-8 读取。
- Diff 空白校验：`git diff --check -- 'doc/tasks/20260806-production-reporting-submit-bdd-tdd-design'` -> PASS，无尾随空白或补丁格式问题。

## Final Git Boundary

- 初始 `git status --short --branch`：显示大量任务开始前已暂存/未提交改动，且分支相对远端存在差异。
- 复核 `git rev-list --left-right --count origin/int_main...HEAD`：输出 `0 1`，表示当前本地 `int_main` 领先远端 1 个提交。
- 复核 `git status --short --branch --untracked-files=no`：仍显示多个非本任务 tracked 改动。
- 复核 `git status --short --branch -- 'doc/tasks/20260806-production-reporting-submit-bdd-tdd-design'`：本任务目录为 `??` 未跟踪新增。

## Experience Consolidation

- 已读取 `project-experience-consolidation` 技能并搜索长期经验归宿。
- 本次可复用规则已由 `docs\e2e-rules.md#规划型-e2e-前置与业务-red-分离门禁` 和 `docs\powershell-memory.md` 中的 Git 边界门禁覆盖，无需新增长期经验文档。

## Blockers

- 提交/推送 blocker：当前工作区存在非本任务 tracked 改动且分支已领先远端 1 个提交；为避免混入并行任务或既有改动，本次未执行 commit/push。
