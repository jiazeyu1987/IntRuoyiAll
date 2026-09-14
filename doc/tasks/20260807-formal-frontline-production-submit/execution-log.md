# Execution Log

## User Intent

- 用户要求把截图中的一线生产“提交”改造成正式提交，并确认提交后进入对应生产组长的报工确认列表。
- 用户随后明确要求按所述方案完成设计、开发和验证。

## BDD Scenarios

- BDD: 缺少完成数量时阻止正式提交 -> Given 一线员工已选择正式工序、员工和设备但完成数量为空 When 点击提交 Then 页面定位并提示“请填写完成数量”且不发送任何提交请求。
- BDD: 正式提交前二次确认 -> Given 一线员工已填写合法完成数量、损耗明细和必填设备参数 When 点击提交 Then 页面展示订单、工序、员工、数量、损耗、设备和参数摘要，并明确提示提交后不可修改。
- BDD: 一次性正式提交成功 -> Given 员工确认合法报工数据且正式上下文有效 When 确认正式提交 Then 系统只调用一次正式提交接口，并在同一事务生成正式报工、记录本原始条目和工序池事件，页面展示正式编号并锁定输入。
- BDD: 正式提交失败保留输入 -> Given 员工已填写报工数据 When 后端拒绝正式上下文或业务数据 Then 页面显示后端明确错误，保留输入且不显示成功状态。
- BDD: 提交进入唯一对应生产组长列表 -> Given 实际填写员工仅属于一个启用的生产组长责任范围 When 正式提交成功 Then 当日事件出现在该生产组长的报工确认列表且其他生产组长不可见。
- BDD: 生产组长归属缺失或不唯一时拒绝 -> Given 实际填写员工没有启用生产组长归属或同时属于多个启用生产组长 When 正式提交 Then 后端拒绝且不生成报工、记录本或工序池事件。
- BDD: 缺少正式必填设备参数时拒绝 -> Given 当前路线工序设备存在启用的数值参数规则 When 员工未填写任一必填读数并提交 Then 前后端均明确指出缺失参数且不产生正式数据。
- BDD: 设备参数超限仍留痕提交 -> Given 必填设备参数均已填写但存在超限读数 When 员工确认正式提交 Then 系统允许提交并在正式事件中保存上下限和异常状态，供生产组长复核。

## Command Intent

- 只读检查现有前端提交、后端事务写入、生产组长责任范围和工作台查询链路。
- 后续测试命令仅运行当前任务目标前端合同、TypeScript 检查和 MES 模块目标 JUnit；不启动或停止共享运行态，除非进入真实 E2E 前按本地运行规则确认。

## Milestone Updates

- M1 completed：确认现有后端已在事务中写入正式报工、记录本和工序池事件；生产组长工作台按启用责任员工范围读取事件。正式改造重点为前端校验/确认/锁定，以及后端唯一组长归属与参数完整性权威门禁。
- M2 completed：生产页先本地校验数量、损耗和数值参数，展示订单/工序/员工/数量/设备摘要确认，仅调用一次正式事务接口；成功展示正式 ID 并锁定输入，失败保留原草稿。
- M3 completed：`requireTeamEmployee` 在授权前验证实际员工只能归属一个启用生产组长；设备参数校验要求所有启用非文本标准规则均有且仅有一条读数，超限仍保存异常状态；聚焦 JUnit 6 项通过。
- M4 partial：前端目标及相邻回归通过，后端生产代码构建和新增聚焦测试通过；既有事务/回滚测试的重新编译被后续出现的共享 PQC 编译错误阻断。
- M5 blocked：缺少已授权生产组长测试身份和任务自有正式 fixture，且当前运行 Jar 未加载本任务后端改动。
- M4 completed：重新确认前端目标静态合同、`pnpm ts:check`、运行 Jar marker 和后端聚焦测试证据均满足本任务放行要求。
- M5 ready_for_closeout：真实 Playwright 写型路径 PASS，剩余仅 cleanup preview/apply 与最终状态回写。

## TDD Evidence

- RED: `pnpm e2e:frontline-formal-submit:static` -> FAIL，当前页面缺少 `assertProductionSubmissionReady`，符合“尚未实现正式提交前业务校验”的预期失败原因。
- RED: `mvn -pl yudao-module-mes '-Dtest=MesFrontlineDeviceParameterValidatorTest,MesFrontlineDeviceAccountContextServiceTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> FAIL，目标测试因 `requireUniqueResponsibleLeaderUserId` 尚不存在而编译失败；同时暴露当前工作区其它并发测试源的既有编译错误，后续目标 GREEN 必须区分任务内结果与该基线阻塞。
- GREEN: `pnpm e2e:frontline-formal-submit:static` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `mvn -pl yudao-module-mes '-Dmaven.test.skip=true' package` -> PASS（本任务后端实现完成后的生产代码构建）。
- GREEN: `mvn -pl yudao-module-mes -Pmes-frontline-formal-submit-targeted-tests compiler:testCompile` -> PASS（仅编译本任务 2 个聚焦测试类）。
- GREEN: `mvn -pl yudao-module-mes -Pmes-frontline-formal-submit-targeted-tests surefire:test` -> PASS（6 tests，0 failures，0 errors）。
- GREEN: 前后端技能 evidence validator 及 self-test -> PASS。
- REGRESSION: 9 项相关前端静态合同 PASS；既有事务/回滚测试的完整生命周期重编译仍被共享 PQC 主代码 22 个编译错误阻断。
- GREEN: `python -X utf8 -m py_compile doc\tasks\20260807-formal-frontline-production-submit\prepare-e2e-fixture.py` -> PASS。
- GREEN: `python -X utf8 doc\tasks\20260807-formal-frontline-production-submit\prepare-e2e-fixture.py` -> PASS，重建本机测试租户任务自有 fixture。
- GREEN: `node doc\tasks\20260807-formal-frontline-production-submit\formal-frontline-submit-real.e2e.cjs` -> PASS，正式提交事件 `187`，`feedbackId=873`，`recordbookEntryId=980112`，对应生产组长 `ffs0807lead1` 可见，非对应组长 `ffs0807lead2` 不可见。

## Blockers

- 共享工作区 `MesFrontlinePqcSubmitReqVO`、`MesFrontlinePqcSubmitCommand`、`MesFrontlinePqcContextServiceImpl` 等并发改动当前无法编译，阻断后端目标 JUnit 和新运行 Jar 构建。
- 当前运行 Jar 为 `backend-runtime-control-20260807-frontline-pqc-order-product-summary.jar`，不是本任务构建产物。
- 未提供生产组长写型 E2E 环境变量和任务自有正式 fixture；禁止使用默认 admin 或基线业务数据写入。

## 2026-08-07 E2E Continuation

- 用户确认此前共享编译错误已消除，要求继续真实 E2E。
- PREFLIGHT: `8081` 由 `E:\IntRuoyi\IntRuoyiFronted` Vite 监听并返回 HTTP 200；`48081` 由 `backend-runtime-control-20260807-active-order-abnormal-fix.jar` 监听并返回 health `UP`。
- RUNTIME GATE: 只读检查运行 Jar 内嵌 MES 模块，`MesFrontlineDeviceAccountContextServiceImpl` 不含 `requireUniqueResponsibleLeaderUserId`，设备参数校验类和错误码类也不含本任务标记，不能直接执行正式写入 E2E。
- VERIFIED CLASS SOURCE: 当前 `target/classes` 可通过 `javap` 看到唯一生产组长方法和正式错误码；需先重跑聚焦 Maven 生命周期，再按本地运行规则只热替换本任务相关 class 并验证新 Jar 哈希、嵌套模块压缩方式、端口归属和 health。
- UPDATED RUNTIME GATE: `48081` 当前 PID `27904` 运行 `E:\IntRuoyi\output\runtime\int_main\backend-latest-20260807-2002-responsible-routes.jar`，SHA256 `06e0025d3103abf28510cf2290545ed034aa815cc25fee28dd4bb729152edd2d`；内嵌 `yudao-module-mes-2026.04-SNAPSHOT.jar` 为 stored，三个关键 class marker 均为 `True`，health `UP`。
- FRONTEND REGRESSION: `pnpm e2e:frontline-formal-submit:static` -> PASS；`pnpm ts:check` -> PASS。
- FIXTURE RED: 首次真实 E2E 到达正式提交接口后因工单不是已确认状态失败；修正任务自有 fixture 将 `mes_pro_work_order.status` 设为 `1` 并即时断言。
- FIXTURE RED: 第二次真实 E2E 因记录本模板 `entry_schema_json=[]` 被正式后端拒绝；修正为覆盖 `fieldValues/defects/productionOrder/process/employee/equipmentParameters/rawPayload` 的正式字段定义。
- FIXTURE RED: 第三次真实 E2E 已生成事件 `186`，但对应生产组长报工列表不可见；只读核对发现 `mes_pro_process_pool_team_leader_scope` 缺少 `PRODUCTION + EMPLOYEE + worker` 正式范围，修正 fixture 新增任务自有 scope `980047` 并断言非对应组长没有同范围。
- FINAL E2E GREEN: `formal-frontline-submit-real.e2e.cjs` PASS。worker 空完成数量路径未发送提交请求；合法确认后仅 1 次 `POST /mes/pro/feedback/frontline/submit`；返回 `feedbackId=873`、`recordbookEntryId=980112`、`processPoolEventId=187`；页面锁定；生产组长 `ffs0807lead1` 列表命中事件 `187`；生产组长 `ffs0807lead2` 列表 `total=0`。

## 2026-08-07 Closeout

- CLEANUP PREVIEW: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260807-formal-frontline-production-submit --mode preview` -> PASS，blocked/warnings 均为 none。
- CLEANUP APPLY: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260807-formal-frontline-production-submit --mode apply` -> PASS，仅删除本任务一次性 helper/evidence 文件，保留 `task.md`、`execution-log.md`、`verification-report.md`。
- EXPERIENCE CONSOLIDATION: 已按 `project-experience-consolidation` 规则合并到既有 `docs/e2e-rules.md#写入型-e2e-任务自有模拟环境门禁`，并更新 `docs/experience-index.md`；未新建长期经验文档。
- CLOSEOUT: 当前为主工作区 `int_main`，无 linked worktree 合并/删除；未执行 Git 提交、合并或推送；任务状态更新为 `completed`。

## 2026-08-07 E2E Revalidation

- USER REQUEST: 用户要求“进行E2E验证”，本轮不得复用旧 result/screenshot 作为新通过证据。
- PREFLIGHT: 已读取 Playwright/E2E/登录/本地运行态/数据库规则；`npx --version` -> `11.6.2`；`8081` 返回 HTTP 200；`48081/actuator/health` 为 `UP`；本机 Docker MySQL/Redis/OnlyOffice/MinIO 均运行。
- PREFLIGHT DB: 任务自有 fixture 仍存在：测试租户账号 `ffs0807worker/ffs0807lead1/ffs0807lead2` 启用，工单 `980738` 状态 `1`，任务 `982557`、路线工序 `980677`、设备参数规则 `FFS-20260807-PRESSURE` 和生产组长员工范围 `980047` 均可解析。
- RED: `node doc\tasks\20260807-formal-frontline-production-submit\formal-frontline-submit-rerun.e2e.cjs` -> FAIL，前置 SQL 使用旧字段 `scope_target_id`；按当前 schema 修正为 `employee_user_id` 后复跑。
- RED: 修正后首轮复跑 worker 端已完成正式提交并生成事件 `190`，但组长页脚本停留在默认“人员管理”页签，未切到“报工管理”导致 UI 定位失败；事件 `190`、feedback `874` 作为本地测试租户正式审计证据保留。
- GREEN: `node --check doc\tasks\20260807-formal-frontline-production-submit\formal-frontline-submit-rerun.e2e.cjs` -> PASS。
- GREEN: `node doc\tasks\20260807-formal-frontline-production-submit\formal-frontline-submit-rerun.e2e.cjs` -> PASS；本轮独立生成签名 `3393`、feedback `875`、recordbookEntry `980114`、processPoolEvent `191`。
- GREEN DETAILS: worker 空完成数量点击未发送正式提交请求；合法提交只发送 1 次 `POST /mes/pro/feedback/frontline/submit`；页面显示 `已正式提交 · 报工 875` 并锁定完成数量；设备参数 `25 MPa` 以 `ABOVE_UPPER` 留痕。
- GREEN DETAILS: 对应生产组长 `ffs0807lead1` 真实登录“报工管理”页可见事件 `191`，API 列表 `total=4` 且命中事件 `191`；非对应生产组长 `ffs0807lead2` 页面和 API 均不可见，API `total=0`。
- DB VERIFY: 只读 SQL 复核 `mes_pro_process_pool_event.id=191` 存在且 `tenant_id=122/deleted=0`；`mes_pro_feedback.id=875` 状态 `2`，`feedback_user_id=914535`，`approve_user_id=914533`。
- ARTIFACTS: 新结果文件 `E:\IntRuoyi\output\playwright\20260807-formal-frontline-production-submit\formal-frontline-submit-rerun-result.json`；截图 `worker-formal-submit-success-rerun-20260807140457.png`、`leader-a-submission-visible-rerun-20260807140457.png`、`leader-b-submission-not-visible-rerun-20260807140457.png`。
- DIAGNOSTICS: `pageErrors=[]`、目标链路 `targetHttpErrors=[]`；审批待办数量无权限 console 与 Baidu 统计请求 abort 均为非目标链路诊断，不影响本次正式提交与组长可见性断言。
- CLEANUP: 本轮临时 helper `formal-frontline-submit-rerun.e2e.cjs` 已删除；任务目录继续仅保留 `task.md`、`execution-log.md`、`verification-report.md`，新结果 JSON 和截图保留在 `output/playwright`。
