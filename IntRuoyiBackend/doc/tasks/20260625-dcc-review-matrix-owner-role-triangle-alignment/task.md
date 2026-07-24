completed

# 任务：DCC 审阅矩阵负责人/角色/三角标记后端对齐

## Current Status

completed

## 任务目标

只调整 DCC 审阅矩阵后端保存/读取/预览/运行时解析逻辑：

- 审阅矩阵 `DEPT` 从“部门树全员解析”改为“按部门负责人解析”。
- 审阅矩阵新增 `ROLE` 主体类型，使用系统角色成员解析。
- 新增阻塞风险码：`DEPT_LEADER_MISSING`、`DEPT_LEADER_USER_NOT_FOUND`、`ROLE_EMPTY`。
- 审阅矩阵 stage source 文案改口径：`DEPT` 为“按部门负责人解析”，`ROLE` 为“按系统角色解析”。
- 旧 route node 中的 `●` 读取时规范成 `▲`，重新保存后也持久化为 `▲`。
- 保存时清空单条规则 `ruleRemark`，不再保留规则行备注；矩阵级 `remark` 继续保留。

## 当前状态

status: completed

## 上一相关任务检查

- 已检查后端上一任务 `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260625-dcc-route-node-stage-type-schema-drift\task.md`，状态为 `已完成`，无未关闭阻塞。
- 当前后端仓存在其他上下文任务，本次仅修改审阅矩阵相关服务、VO、测试与本任务文档。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`
- 适用强制门禁：
  - 本阶段先做源码与定向单测，不做服务器写入、发布或远端联调。
  - 真实 E2E 由前端阶段在本机测试租户执行；若后端单测已证明 `DEPT` 无负责人、`ROLE` 无成员时应阻塞，则真实环境缺样本也必须显式阻塞。
  - 不得为旧 `DEPT` 行为保留部门树全员 fallback；负责人缺失直接报阻塞。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。`DEPT` 仅认 `leaderUserId`，`ROLE` 仅认有效角色成员。
- `是否从根因和长期维护角度解决`：是。统一审阅矩阵路由节点、预览、风险码和运行时访问解析口径。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: DEPT 审阅矩阵规则按部门负责人解析 -> Given 审阅矩阵规则主体类型为 DEPT When 预览或解析当前生效矩阵 Then 仅部门负责人被解析为参与人。`
- `BDD: DEPT 缺负责人时阻塞 -> Given 审阅矩阵规则主体类型为 DEPT 但部门没有 leaderUserId 或负责人用户不存在 When 预览或保存 Then 返回阻塞风险码并拒绝保存。`
- `BDD: ROLE 审阅矩阵规则按系统角色解析 -> Given 审阅矩阵规则主体类型为 ROLE When 预览或解析当前生效矩阵 Then 该角色下所有有效用户都被解析为参与人。`
- `BDD: ROLE 无成员时阻塞 -> Given 审阅矩阵规则主体类型为 ROLE 但角色无成员 When 预览或保存 Then 返回 ROLE_EMPTY 阻塞风险并拒绝保存。`
- `BDD: 旧 route node 标记读取时规范为 ▲ -> Given 历史 route node 仍保存 marker=● When 读取矩阵或构造列表摘要 Then 返回值统一为 ▲。`

## 里程碑

1. M1：创建任务文档并记录门禁。`DONE`
2. M2：补后端 RED 单测，锁定负责人/角色/风险码合同。`DONE`
3. M3：实现读取、预览、保存与运行时解析。`DONE`
4. M4：执行 GREEN 验证并补后端证据。`DONE`

## 阻塞记录

- `2026-06-25 22:44 +08:00`：`mvn -pl yudao-module-dcc "-Dtest=DccCategoryApprovalMatrixAdminServiceImplTest,DccControlledFileReviewMatrixAccessServiceTest" -DfailIfNoTests=false test` 被仓库既有 `DccProjectCodeControllerTest` 无关 `testCompile` 错误拦截，当前无法进入本任务目标测试类执行。
- `2026-06-26 00:41 +08:00`：上述阻塞已解除，定向 Maven 测试成功进入目标测试类并通过。

## 预期验证

- `mvn -pl yudao-module-dcc "-Dtest=DccCategoryApprovalMatrixAdminServiceImplTest,DccControlledFileReviewMatrixAccessServiceTest" -DfailIfNoTests=false test`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260625-dcc-review-matrix-owner-role-triangle-alignment/backend-api-evidence.md`

## 完成记录

- 后端已支持审阅矩阵 `ROLE` 主体类型，并将 `DEPT` 切换为按部门 `leaderUserId` 负责人解析。
- 已补齐阻塞风险码 `DEPT_LEADER_MISSING`、`DEPT_LEADER_USER_NOT_FOUND`、`ROLE_EMPTY`，负责人缺失、负责人用户无效、角色无成员均直接阻塞。
- 保存与回显统一 `▲`，规则行 `ruleRemark` 保存时清空，矩阵级 `remark` 保留。
- 最终验证：`mvn -pl yudao-module-dcc "-Dtest=DccCategoryApprovalMatrixAdminServiceImplTest,DccControlledFileReviewMatrixAccessServiceTest" -DfailIfNoTests=false test` -> PASS，`19 tests, 0 failures, 0 errors`。
