# Execution Log

## User Intent

- 用户确认 AC-D03 新业务口径：不再由生产班组长或 PQC 组长维护“不良原因”主数据；PQC 出现不良时手动输入即可。
- 用户要求继续处理系统是否支持手动输入、原始快照、订单/工序/PQC 追溯、历史不覆盖的问题。

## BDD / TDD Notes

- BDD: PQC 手动录入不良说明 -> Given PQC 检验员在当前活跃订单和工序执行检验；When 任一逐件结果不合格或损耗数量大于 0；Then 页面要求手动输入不良说明/原因，正式提交保存该说明并进入 PQC 追溯详情。
- BDD: 缺少不良说明时失败 -> Given PQC 检验结果为不合格；When 提交 payload 没有手动不良说明；Then 后端 fail-fast 拒绝，不创建 PQC event 或 PQC record。
- BDD: 原始说明可追溯 -> Given PQC 已提交含手动不良说明的失败记录；When PQC 组长查看详情或系统读取时间线；Then 能通过 rawPayload 追溯到订单、工序、PQC task/event/record 和原始说明。

## Command Intent

- 已读取 `docs\task-closeout-rules.md`、`docs\powershell-encoding.md`、`docs\frontend-development.md`、`docs\backend-development.md`。
- 已读取 `docs\e2e-rules.md`；本轮只执行静态合同和后端 JUnit，未声明真实页面 E2E 通过。
- 已读取 `behavior-driven-development`、`frontend-feature-delivery`、`backend-api-delivery` 技能说明。
- 已读取 `docs\experience-index.md` 并确认适用 PQC 项目级检验快照门禁。
- 已读取 `project-experience-consolidation` 技能；本轮经验属于一次性 AC-D03 口径与局部测试收窄，未命中现有 `docs\*memory*.md`，未获授权不新建长期经验文档。
- 已读取 `task-closeout-cleanup` 技能和 closeout references，并执行 cleanup preview/apply。

## Milestone Updates

- completed：已建立 BDD 场景和前后端聚焦测试。
- completed：前端 PQC 面板新增手动“不良说明”文本输入、失败必填校验、提交字段和 rawPayload.pqcDraft 快照。
- completed：后端 PQC 提交 VO/Command 新增 `nonconformanceDescription`；失败结果缺说明时在写库前 fail-fast；rawPayload 由服务端写入标准化说明并保留订单、工序、PQC task 等追溯身份。
- ready_for_closeout：定向 GREEN 已通过；完整 closeout/提交/推送因共享工作区非本任务脏改动和无关类型检查失败暂不执行。

## Verification Evidence

- RED: `node E:\IntRuoyi\IntRuoyiFronted\tests\e2e\role-matrix-pqc-manual-defect-note-static.spec.cjs` -> FAIL，缺少 `data-pqc-defect-description` 稳定输入控件。
- RED/GREEN 调整: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` 首次执行后新失败用例触发 Mockito unnecessary stubbing；原因是生产代码已在查库前 fail-fast，测试已收窄为只验证无写入。
- GREEN: `node E:\IntRuoyi\IntRuoyiFronted\tests\e2e\role-matrix-pqc-manual-defect-note-static.spec.cjs` -> PASS。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，17 tests。
- REGRESSION: `node E:\IntRuoyi\IntRuoyiFronted\tests\e2e\frontline-formal-submit-static.spec.cjs` -> PASS。
- REGRESSION: `pnpm --dir E:\IntRuoyi\IntRuoyiFronted e2e:role-matrix-pqc-dynamic-form:static` -> PASS。
- REGRESSION: `pnpm --dir E:\IntRuoyi\IntRuoyiFronted ts:check` -> FAIL，阻塞点为 `src/views/mes/pro/processpool/QaRegulationPage.vue(1204,3)` 的 `PATROL_AM` 类型不匹配，非本次 AC-D03 修改文件。
- STRUCTURE: `git diff --check` 针对本次相关文件 -> PASS。
- RUNTIME PREFLIGHT: `GET http://127.0.0.1:8081/src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue?t=1785913672517` -> FAIL，Vite eslint 阻塞 3 个 Vue 模板自闭合标签；已修复为显式闭合标签。
- RUNTIME PREFLIGHT GREEN: 同一 Vite 模块 -> HTTP 200；`node E:\IntRuoyi\IntRuoyiFronted\tests\e2e\role-matrix-pqc-manual-defect-note-static.spec.cjs` -> PASS；相关文件 `git diff --check` -> PASS。
- RUNTIME READONLY: Playwright 真实页面登录 `芋道源码/admin` 并打开 `/mes/pro/feedback/edhr-batch-pqc-fill` -> PASS，`data-frontline-pqc-operator` 可见，`data-pqc-defect-description` 可见；只读接口 `/pqc/active-orders` 返回 code=0/count=2，首个活跃订单工序接口返回 code=0/count=13。
- RUNTIME READONLY INPUT: Playwright 在 `data-pqc-defect-description` 输入 `AC-D03只读预检手动输入-未提交` 并读取 value -> PASS；未观察到 `/pqc/submit` 写请求，提交请求数 0。
- RUNTIME JAR: 当前 48081 运行 Jar 的 `BOOT-INF/lib/yudao-module-mes-2026.04-SNAPSHOT.jar` 中 `MesFrontlinePqcSubmitReqVO.class` 与 `MesFrontlinePqcContextServiceImpl.class` 均包含 `nonconformanceDescription`，说明运行态已加载本次字段。
- CLEANUP: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260805-ac-d03-pqc-manual-defect-note --mode preview` -> PASS，keep 3，delete/blocked/warnings 均为 `<none>`。
- CLEANUP: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260805-ac-d03-pqc-manual-defect-note --mode apply` -> PASS，deleted_paths 为 `<none>`。

## Blockers

- 当前共享工作区已有大量非本任务脏改动，后续提交/推送需按项目规则单独处理，不能混入无关改动。
- 当前分支 `int_main...origin/int_main [ahead 13]` 且存在大量已修改文件；按项目规则，提交/推送前需要先处理共享脏工作区基线，本轮未擅自提交。
- 全量前端 `ts:check` 存在无关历史阻塞 `QaRegulationPage.vue(1204,3)`；本次 AC-D03 定向静态合同与后端 JUnit 已通过。
- 真实写入型 PQC E2E 仍未执行：当前只读预检使用本机默认身份和现有活跃订单，不能替代任务自有测试租户/数据的正式提交；页面当前还暴露既有业务数据错误 `精洗-外观-抽检样本数量0与任务计划数量15不一致。`，写入验收前需准备可追踪、可清理的 PQC 任务数据。
