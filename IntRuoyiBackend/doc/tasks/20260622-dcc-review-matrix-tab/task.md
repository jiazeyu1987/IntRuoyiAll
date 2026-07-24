# 任务：DCC 审阅矩阵页签后端与运行时权限改造

## 任务目标

- 为 DCC 文件类别提供审阅矩阵列表/删除接口。
- 让 `REVIEW/APPROVE` 不再作为通用类别权限手工维护入口。
- 让流程内审阅/批准与待审预览权限以 route snapshot 为准，只影响后续新提交。

## 当前状态

`COMPLETED`

## Current Status

COMPLETED

## 上一任务检查

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260622-edhr-release-transaction-permission-rows-fix\task.md`
- 状态：`COMPLETED_WAITING_MAIN`
- 处理：该任务已在等待主线集成；本任务不修改其文件，只隔离处理 DCC 审阅矩阵相关后端代码与测试。

## 经验门禁

- 已读取：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`
- 本任务适用强制门禁：
  - 不为缺失的 DCC 岗位分配做 fallback；预览/提交流程缺少岗位解析人时继续显式报错。
  - 流程内权限改造必须基于 route snapshot 真值，不能靠实时类别权限表兜底。
  - 涉及真实 E2E 前先记录 `experience-preflight`，当前阶段先做本机单测与静态验证。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否
- `是否从根因和长期维护角度解决`：是
- `是否存在临时补丁或绕过`：否

## BDD 场景

- `BDD: 删除矩阵 -> Given 类别已有生效矩阵 When 删除矩阵 Then 当前 active route 失效、历史版本保留、后续新提交报未配置路线。`
- `BDD: 禁止双真源 -> Given 管理端提交 REVIEW/APPROVE 到 permission-rules When 保存 Then 后端拒绝并提示改用 DCC审阅矩阵。`
- `BDD: 旧流程不受影响 -> Given 已在流程中的文件 route snapshot 已生成 When 类别矩阵或 REVIEW/APPROVE 规则后续被修改或清空 Then 旧流程审批人与待审预览仍按 snapshot 放行。`

## 里程碑

1. 补任务文档与执行日志。`DONE`
2. RED：补矩阵列表/删除、权限拒绝、snapshot 放权测试。`DONE`
3. GREEN：实现后端接口、服务与运行时权限调整。`DONE`
4. GREEN：跑定向 Maven 回归并回填证据。`DONE`

## 预期验证

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc -Dtest=DccCategoryApprovalMatrixAdminServiceImplTest,DccCategoryPermissionAdminServiceImplTest,DccControlledFileWorkflowServiceImplTest,DccControlledFileQueryServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`

## 预期交付物

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260622-dcc-review-matrix-tab\execution-log.md`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260622-dcc-review-matrix-tab\backend-api-evidence.md`

## 最终验证结果

- 已在 clean integration worktree `D:\ProjectPackage\Int\IntRuoyiWorktrees\ruoyi-vue-pro-int-main-one-shot-integration` 重放 DCC 审阅矩阵后端改动。
- `mvn --% -f D:\ProjectPackage\Int\IntRuoyiWorktrees\ruoyi-vue-pro-int-main-one-shot-integration\pom.xml -pl yudao-module-dcc -Dtest=DccCategoryApprovalMatrixAdminServiceImplTest,DccCategoryPermissionAdminServiceImplTest,DccControlledFileWorkflowServiceImplTest,DccControlledFileQueryServiceTest -Dsurefire.failIfNoSpecifiedTests=false test` -> `BUILD SUCCESS`
- 本次定向回归共通过 127 个测试，无失败、无错误、无跳过。
