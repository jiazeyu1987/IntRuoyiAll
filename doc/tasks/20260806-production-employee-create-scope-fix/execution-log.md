# Execution Log

## 2026-08-06

- User intent: 新建生产人员时不应因为员工尚未加入负责范围而报“班组长不在该员工或工序的负责范围内”。
- Scope: 生产组长工作台新增人员、正式工/临时工生产人员档案创建，以及后续工序绑定范围校验边界。
- BDD: 新建正式工档案 -> Given 班组长选择一个全量系统用户作为正式工；When 提交新增生产人员档案但尚未绑定工序；Then 创建当前班组长名下生产人员档案，不校验该员工是否已在负责员工范围内。
- BDD: 新建临时工档案 -> Given 班组长录入临时工姓名和签名密码；When 提交新增临时工档案；Then 创建当前班组长名下临时工档案，不校验该员工是否已在负责员工范围内。
- BDD: 工序绑定仍受控 -> Given 班组长将生产人员绑定到某个工序；When 该工序不在班组长负责范围；Then 继续返回负责范围错误，防止越权维护工序配置。
- BDD: 报工复核仍受控 -> Given 班组长查看或复核非负责员工的报工；When 请求列表、详情、复核或确认；Then 继续返回负责范围错误。

## Command Intent

- 读取新增人员前后端调用链，确认哪一步误触发 `PRO_PROCESS_POOL_TEAM_SCOPE_DENIED`。
- 在生产代码修改前补充目标失败测试。
- 实施最小边界修复并运行相关后端定向测试。

## TDD Evidence

- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderScopeServiceTest,MesTeamLeaderRuntimeConfigServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, `PRO_PROCESS_POOL_TEAM_TARGET_SCOPE_DENIED` 尚未定义，新增目标化错误信息测试先失败。
- Root cause: 新增正式工/临时工档案接口本身未绑定工序，也不应校验员工负责范围；实际范围拒绝来自后续工序关系保存或报工访问链路，但旧错误文案把“员工或工序”合并，导致用户误解为新员工必须已被班组长负责。
- Fix: 新增 `PRO_PROCESS_POOL_TEAM_TARGET_SCOPE_DENIED`，`MesTeamLeaderScopeServiceImpl` 按员工、工序、产线、设备、订单分别抛出精确范围错误；新增人员成功路径测试断言不调用 `assertCanAccessEmployee` / `assertCanMaintainProcess`。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderScopeServiceTest,MesTeamLeaderRuntimeConfigServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 18, Failures: 0, Errors: 0, Skipped: 0。
- REGRESSION: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderScopeServiceTest,MesTeamLeaderRuntimeConfigServiceTest,MesTeamEmployeeBindingServiceTest,MesTeamLeaderReportConfirmationServiceTest,MesTeamLeaderSubmissionReviewServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 35, Failures: 0, Errors: 0, Skipped: 0。
- VALIDATOR: bug regression evidence 与 backend API evidence validator 均 PASS。
- Experience: 已合并长期经验到 `docs/backend-development.md#MES 生产人员档案正式工重复关联门禁`，并在 `docs/experience-index.md` 增加 `PRO_PROCESS_POOL_TEAM_TARGET_SCOPE_DENIED`、`新增人员不校验负责员工范围` 等关键词。
- Runtime: 已复制当前运行 Jar 并只替换本任务 `ErrorCodeConstants.class` 与 `MesTeamLeaderScopeServiceImpl.class`，新 Jar `output/runtime/int_main/backend-runtime-production-employee-scope-fix-20260806.jar` SHA256 `1BFE4C9AC3F72708AB78756E295F02228A8371CDDAEEB591F89D3A8EAFB804F1`，内嵌 MES jar 为未压缩存储。
- Runtime GREEN: 已停止旧 PID `17936`，启动新 PID `60484`，`http://127.0.0.1:48081/actuator/health` -> `UP`，进程命令行确认使用新 Jar。
- Cleanup note: 临时解包目录 `output/runtime/int_main/patch-production-employee-scope-fix-20260806` 清理命令被本地策略拦截，未改用更激进删除方式；该目录不影响运行态。
