# 执行日志：DCC 审阅矩阵负责人/角色/三角标记后端对齐

BDD: DEPT 审阅矩阵规则按部门负责人解析 -> Given 审阅矩阵规则主体类型为 DEPT When 预览或解析当前生效矩阵 Then 仅部门负责人被解析为参与人。
BDD: DEPT 缺负责人时阻塞 -> Given 审阅矩阵规则主体类型为 DEPT 但部门没有 leaderUserId 或负责人用户不存在 When 预览或保存 Then 返回阻塞风险码并拒绝保存。
BDD: ROLE 审阅矩阵规则按系统角色解析 -> Given 审阅矩阵规则主体类型为 ROLE When 预览或解析当前生效矩阵 Then 该角色下所有有效用户都被解析为参与人。
BDD: ROLE 无成员时阻塞 -> Given 审阅矩阵规则主体类型为 ROLE 但角色无成员 When 预览或保存 Then 返回 ROLE_EMPTY 阻塞风险并拒绝保存。
BDD: 旧 route node 标记读取时规范为 ▲ -> Given 历史 route node 仍保存 marker=● When 读取矩阵或构造列表摘要 Then 返回值统一为 ▲。

INFO: task-created -> 后端任务文档已创建，准备补后端 RED 单测。

RED: mvn -pl yudao-module-dcc "-Dtest=DccCategoryApprovalMatrixAdminServiceImplTest,DccControlledFileReviewMatrixAccessServiceTest" -DfailIfNoTests=false test -> FAIL, 仓库现有 DccProjectCodeControllerTest 无关编译错误（createProjectCode/updateProjectCode/deleteProjectCode 缺失）导致 testCompile 阶段提前失败，未进入本任务目标单测执行。
GREEN: mvn -pl yudao-module-dcc -DskipTests compile -> PASS
GREEN: experience-preflight -> PASS, 真实登录前置检查已通过；后续真实 E2E 允许在测试租户 测试租户/aoteman 上执行。
GREEN: mvn -pl yudao-module-dcc -DskipTests compile -> PASS, 已覆盖新增预览主体元数据字段后的主源码编译。
GREEN: mvn -pl yudao-module-dcc "-Dtest=DccCategoryApprovalMatrixAdminServiceImplTest,DccControlledFileReviewMatrixAccessServiceTest" -DfailIfNoTests=false test -> PASS, 19 tests, 0 failures, 0 errors。
