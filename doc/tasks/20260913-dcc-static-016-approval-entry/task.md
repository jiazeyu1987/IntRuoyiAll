# DCC-STATIC-016 产品建档审批入口权限修复

## Task Goal

修复 `docs/bugs/20260912-dcc-90-step-static-audit.md` 中 DCC-STATIC-016：产品建档待审批列表只能从要求 `create` 权限的按钮打开，导致只有查询和审批权限的处理人无法从正式页面续办。修复后，审批人可按查询/审批权限打开待办并批准，但仍不能创建建档申请。

## Milestones

- [x] 建立干净 worktree、读取 AGENTS 与指定规则文件、读取 bug-regression-fix-loop 技能。
- [x] 记录 BDD 场景与 RED 静态合同目标。
- [x] 增加最小静态回归合同并先得到 RED。
- [x] 最小化修复前端入口权限与创建/批准动作守卫。
- [x] 运行定向静态合同、类型/编译或静态验证，并记录 GREEN。
- [x] 收尾前更新共享 bug 状态/证据（仅 DCC-STATIC-016）。

## Expected Verification

- `node IntRuoyiFronted/tests/e2e/dcc-static-016-product-onboarding-approval-entry-static.spec.cjs` 先 RED 后 GREEN。
- 定向静态检查证明：
  - 待审批入口不再只依赖 `dcc:project-code:create`。
  - 创建申请动作仍由 `dcc:project-code:create` 守卫。
  - 批准待办动作仍由 `dcc:project-code:update` 守卫。
  - 后端 pending/approve 权限合同保持 create-or-update / update。
- `git diff --check` PASS。
- 不执行真实 E2E、不启动或重启服务、不写数据库；用户后续已明确授权 Git 提交并融合 `int_main`。

## Design Constraints Check

- 严格无 fallback：不添加降级权限、默认成功或异常吞掉路径。
- 范围限制：只处理 DCC-STATIC-016，不修改 017-027 或无关模块。
- 权限边界：查询/审批入口与创建动作分离；审批人能打开待办，不能创建申请。
- Worktree：使用 `D:\IntRuoyiWorktree\20260913-dcc-static-016-approval-entry`，不继承 `E:\IntRuoyi` 未提交 diff。
- 端口登记：历史 `D:\IntRuoyiWorktree\.ports` 登记为 slot 43；当前提交钩子使用 Git common-dir 登记，已补 `int_main` slot 11（8092/48092），未启动服务。
- 验证边界：仅静态代码逻辑检查、定向静态合同和必要编译/格式检查。

## Current Status

ready_for_closeout

DCC-STATIC-016 已完成实现、静态合同、后端定向测试、前端类型检查、端口门禁和 bug 文档状态更新。用户已在后续指令中授权先提交再融合 `int_main`；当前分支准备提交后追上最新 `int_main` 并复验。
