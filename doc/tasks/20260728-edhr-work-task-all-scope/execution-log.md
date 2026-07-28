# Execution Log

## User Intent

用户反馈创建 eDHR 批次执行时页面报错：`eDHR 工作任务责任范围快照无效：scopeKey=ALL`。

## BDD

- BDD: 普通整表填写人规则生成责任范围快照 -> Given 批次工序任务绑定正式批记录表单且填写人规则为 `scopeKey=ALL`、未显式保存单元格范围 When 创建初始填写工作任务 Then 系统应从正式批记录报表生成整表可填写范围快照并创建任务。
- BDD: 责任范围缺少正式来源仍失败 -> Given 任务无法解析批记录报表布局或动态表单范围 When 创建工作任务 Then 系统应 fail fast 并暴露责任范围快照无效。

## Milestone Updates

- in_progress: 已定位报错来自 `MesProEdhrWorkTaskServiceImpl#parseRequiredFillableScope` 对 `scopeKey=ALL` 的空 `fillableScopeJson` 校验。
- completed: 已用普通批记录 `ALL` 填写人规则复现创建批次执行失败；失败点为责任范围快照缺少正式范围。
- completed: 已实现普通批记录 `ALL` 规则从正式批记录报表成员生成整表 `ranges`，动态表单槽位仍读取任务自身 `fillableScopeJson`。
- completed: 已增加缺少正式报表成员的 fail-fast 测试，避免默认成功或空范围掩盖配置缺失。

## Evidence

- GREEN: experience-preflight -> PASS, read `docs/experience-index.md`, applied `docs/backend-development.md#edhr-详情回填门禁` and `docs/backend-development.md#edhr-批次任务配置来源门禁`.
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrWorkTaskServiceImplTest#createInitialFillTask_buildsAllScopeSnapshotFromReportMembersWhenRuleScopeIsBlank" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, `ServiceException: eDHR 工作任务责任范围快照无效：scopeKey=ALL`.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrWorkTaskServiceImplTest#createInitialFillTask_buildsAllScopeSnapshotFromReportMembersWhenRuleScopeIsBlank" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 1 test.
- REGRESSION: `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrWorkTaskServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 71 tests, 0 failures, 0 errors.
- Note: 未带 `-Dsurefire.failIfNoSpecifiedTests=false` 的首次 RED 命令被上游 reactor 模块 `yudao-common` 拦截，未进入目标测试；按 `docs/backend-development.md` 的指定测试类规则补充 Surefire 参数后复验。
- Closeout blocker: 当前 `int_main` 存在非本任务本地提交领先 `origin/int_main`，不能在不混入无关任务风险的前提下声明最终 push 完成。
