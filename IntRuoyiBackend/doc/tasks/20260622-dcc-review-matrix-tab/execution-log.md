# DCC 审阅矩阵页签后端与运行时权限改造执行日志

- BDD: 删除矩阵 -> Given 类别已有生效矩阵 When 删除矩阵 Then 当前 active route 失效、历史版本保留、后续新提交报未配置路线。
- BDD: 禁止双真源 -> Given 管理端提交 REVIEW/APPROVE 到 permission-rules When 保存 Then 后端拒绝并提示改用 DCC审阅矩阵。
- BDD: 旧流程不受影响 -> Given 已在流程中的文件 route snapshot 已生成 When 类别矩阵或 REVIEW/APPROVE 规则后续被修改或清空 Then 旧流程审批人与待审预览仍按 snapshot 放行。
- RED: `mvn --% -f D:\ProjectPackage\Int\IntRuoyiWorktrees\ruoyi-vue-pro-int-main-one-shot-integration\pom.xml -pl yudao-module-dcc -Dtest=DccCategoryReviewMatrixAdminServiceImplTest test` -> FAIL，当前基线没有 `DccCategoryReviewMatrixAdminServiceImplTest`，说明审阅矩阵页签后端契约在实现前并不存在独立测试入口。
- GREEN: 读取现有矩阵服务、控制器、工作流与查询服务 -> PASS
- GREEN: 读取现有 `DccCategoryApprovalMatrixAdminServiceImplTest`、`DccCategoryPermissionAdminServiceImplTest`、`DccControlledFileWorkflowServiceImplTest`、`DccControlledFileQueryServiceTest` -> PASS
- GREEN: integration-worktree-replay -> PASS，已在 clean integration worktree `D:\ProjectPackage\Int\IntRuoyiWorktrees\ruoyi-vue-pro-int-main-one-shot-integration` 单独重放 DCC 审阅矩阵后端相关文件，不含 DCC 文件名识别残留与 MES 排产改动。
- GREEN: scoped-maven-regression -> PASS，执行 `mvn --% -f D:\ProjectPackage\Int\IntRuoyiWorktrees\ruoyi-vue-pro-int-main-one-shot-integration\pom.xml -pl yudao-module-dcc -Dtest=DccCategoryApprovalMatrixAdminServiceImplTest,DccCategoryPermissionAdminServiceImplTest,DccControlledFileWorkflowServiceImplTest,DccControlledFileQueryServiceTest -Dsurefire.failIfNoSpecifiedTests=false test` 返回 `BUILD SUCCESS`，共通过 127 个测试，无失败、无错误、无跳过。
