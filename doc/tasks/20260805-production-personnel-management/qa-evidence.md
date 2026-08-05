# QA Evidence

## Scope

验证生产组长生产人员档案管理的后端合同、数据库迁移合同、前端静态合同、脚本语法、类型检查和真实 Playwright E2E。

## Matrix

- 列表只显示当前组长关联员工：后端 JUnit + 前端静态合同 + 真实 E2E 覆盖。
- 正式工搜索不暴露全系统用户：前端静态合同禁止 `/system/user/page`，后端 JUnit 覆盖 scoped candidates，真实 E2E 通过远程姓名下拉选择任务自有正式工。
- 正式工重复关联失败路径：后端 RED/GREEN 覆盖写库前业务拒绝，避免 DB 唯一键 500。
- 临时工不创建登录账号且有签名密码：后端 JUnit/schema 静态合同 + 真实 E2E 页面新增覆盖。
- 禁用后不进入新报工选择：后端运行态候选 JUnit + 真实 E2E runtime-config 候选前后对比覆盖。
- 审计留痕：后端 JUnit + 前端审计列表静态合同 + 真实 E2E 审计表可见覆盖。

## Test

- `node tests/e2e/production-personnel-management-static.spec.cjs`
- `node tests/e2e/team-leader-workbench-static.spec.cjs`
- `node --check tests/e2e/team-leader-workbench-real-flow.e2e.js`
- `pnpm e2e:production-personnel-management:real:check`
- `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolTeamLeaderControllerTest,MesProcessPoolTeamLeaderSchemaTest,MesTeamLeaderRuntimeConfigServiceTest,MesFrontlineRuntimeConfigServiceTest,MesFrontlineRuntimeConfigControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `pnpm install --frozen-lockfile --offline --ignore-scripts --reporter append-only`，PASS。
- `pnpm ts:check`，PASS。
- `pnpm e2e:production-personnel-management:real`，PASS。
- backend/database/frontend/QA evidence validator，PASS。
- `git diff --check`

## RED / GREEN

- RED: `node tests/e2e/production-personnel-management-static.spec.cjs` -> FAIL, 缺少标准列表和生产人员管理 tab。
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderRuntimeConfigServiceTest#shouldRejectDuplicateFormalUserBeforeDatabaseInsert" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 缺少重复正式工业务错误码。
- GREEN: 前端静态合同 PASS，后端 30 个 JUnit PASS，真实 E2E 脚本 `node --check` PASS，`pnpm ts:check` PASS，真实 Playwright E2E PASS，`git diff --check` PASS。

## Verification

- 当前可执行验证均已通过。
- 真实 Playwright E2E 使用 worktree slot 1 的 `8082/48082` 成对 URL；页面路径完成正式工关联、临时工新增、重复名拒绝、绑定、密码重置、禁用和审计可见。

## Blockers

- 无当前验证 blocker。
