# 活跃订单放行资料实现任务

## Task Goal

实现生产组长在活跃订单池中，当生产进度和检验进度均为 100% 时手动点击“申请放行”，由后端创建正式放行资料申请并推送生产负责人待办的第一版正式链路。第一阶段实现申请入口、后端编排骨架、前端按钮、状态展示、幂等与严格阻塞式来源校验；不得生成假批记录、假过程检验单、假损耗单或绕过负责人放行。

## Milestones

- [x] 创建实现任务文档并记录 BDD/TDD 起点。
- [x] 后端 RED：新增申请服务/API/静态合同测试，先失败于缺少接口或编排类。
- [x] 后端 GREEN：实现申请接口、资格校验、阻塞项、幂等记录、放行事务/负责人待办对接。
- [x] 数据库 RED/GREEN：新增申请记录表迁移与静态 schema 测试。
- [x] 前端 RED/GREEN：新增活跃订单申请按钮、API wrapper、确认框、行状态和错误展示。
- [x] 定向验证与 evidence validator。

## Expected Verification

- 后端目标测试覆盖成功路径、进度不足、非当前组长、缺正式来源、重复申请幂等、负责人配置缺失。
- 前端静态合同覆盖按钮文案、禁用原因、确认弹框、API 调用、成功刷新和阻塞项展示。
- schema 静态测试覆盖申请表、唯一键、状态字段和权限码。
- UTF-8 读取任务文档成功。
- 若真实测试账号、签名、正式模板或租户数据缺失，真实 E2E 记录为 BLOCKED，不用 API-only 冒充通过。

## Current Status

completed

实现、定向验证、evidence validator 和 task-closeout-cleanup 均已完成；真实 E2E 因缺已确认的本地运行态、测试账号和满足双 100% 的任务自有活跃订单数据未执行。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。缺正式来源、负责人、权限、字段映射或签字证据时 fail fast 或返回 blocker，不生成假资料。
- `是否从根因和长期维护角度解决`：是。第一版复用现有活跃订单、eDHR 批次执行、正式批记录绑定、PQC 汇集、放行事务与工作任务对象。
- `是否存在临时补丁或绕过`：否。不得用 `formBindings`、默认 `MAIN`、空资料、mock 或 API-only 替代正式链路。

## 经验门禁

- 命中 `docs/backend-development.md#活跃订单申请放行资料必须只使用正式来源`：活跃订单申请放行资料只能从正式批记录绑定、已确认 PQC 汇集、正式损耗映射和 `RELEASE_APPROVE` 负责人生成；缺失时返回 blocker，不得用 `formBindings`、默认 `MAIN`、工序开始配置、空资料或直接放行替代。

## BDD Scenarios

- BDD: 生产组长申请放行资料 -> Given 当前用户负责的活跃订单生产进度为 100% 且检验进度为 100%，正式来源和负责人配置完整; When 点击申请放行; Then 系统创建或复用正式申请、eDHR 批次/放行事务并生成生产负责人待办。
- BDD: 进度不足时拒绝申请 -> Given 活跃订单任一进度未达 100%; When 前端显示或后端收到申请; Then 按钮禁用或接口拒绝，并返回明确未完成原因。
- BDD: 正式来源缺失时阻塞 -> Given 活跃订单两个进度均为 100% 但缺正式批记录绑定、PQC 汇集明细、损耗映射或负责人; When 申请放行; Then 系统返回阻塞项，不创建不完整资料或无人待办。
- BDD: 重复申请幂等 -> Given 同一活跃订单同一来源快照已申请; When 重复点击申请; Then 返回既有申请结果，不重复创建批次、事务或待办。

## Verification Evidence

- RED backend static: PASS 前失败于缺 `MesTeamLeaderActiveOrderReleaseApplicationServiceImpl.java`。
- RED schema static: PASS 前失败于缺 `20260808_mes_active_order_release_application.sql`。
- RED frontend static: PASS 前失败于缺前端 API 和页面合同。
- GREEN backend static: `node yudao-module-mes/src/test/js/mes-team-leader-active-order-release-application-static.spec.cjs` -> PASS。
- GREEN schema static: `node yudao-module-mes/src/test/js/mes-team-leader-active-order-release-application-schema-static.spec.cjs` -> PASS。
- GREEN frontend static: `node src/api/mes/pro/processpool/teamLeaderReleaseApplication.static.spec.cjs` -> PASS。
- Compile: `mvn -pl yudao-module-mes -am '-DskipTests' compile` -> PASS。
- JUnit: `mvn -pl yudao-module-mes '-Dtest=MesTeamLeaderActiveOrderServiceTest,MesTeamLeaderActiveOrderErpPlannedStartTest' test` -> PASS，21 tests。
- Type check: `pnpm ts:check` -> PASS。
- Evidence validators: backend/database/frontend validators -> PASS，并已归档到 `verification-report.md`。
- Cleanup preview/apply: `task_closeout.py --task-id 20260808-active-order-release-dossier-implementation --mode preview/apply` -> PASS，仅删除本任务三份临时 evidence 文件，保留 `task.md`、`execution-log.md`、`verification-report.md`。

## Blockers / Not Run

- 真实 E2E 未运行：缺已确认的本地运行态、测试账号、签名配置、正式模板和满足双 100% 的任务自有活跃订单数据。
- 第一版损耗单正式来源尚未确认；如果路线存在 `LOSS_REPORT` 绑定，后端会返回 blocker，不生成假损耗单。
- 未提交 Git：项目 Git Policy 规定未被用户明确要求时不做 commit/push；当前工作区还存在大量非本任务并发改动。

## Cleanup Keep

- doc/tasks/20260808-active-order-release-dossier-implementation/verification-report.md
