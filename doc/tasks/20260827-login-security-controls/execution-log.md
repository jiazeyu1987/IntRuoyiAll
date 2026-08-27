# Execution Log

## User Intent

用户要求先实现以下三项：

- 输错 5 次锁定账号：增加登录失败计数、锁定状态、解锁逻辑。
- 15 分钟自动登出/锁屏：前端无操作 15 分钟后锁定或退出登录。
- 普通用户禁止管理员/日志权限：加系统校验，防止普通用户被分配高权限。

## Rule And Skill Bootstrap

- 已读取技能：`backend-api-delivery`、`frontend-feature-delivery`、`database-schema-delivery`。
- 已读取技能契约：`backend-contract.md`、`frontend-contract.md`、`database-contract.md`。
- 已读取项目规则：`docs/task-closeout-rules.md`、`docs/backend-development.md`、`docs/database-rules.md`、`docs/frontend-development.md`、`docs/e2e-rules.md`、`docs/powershell-encoding.md`、`docs/login-access.md`、`docs/local-runtime.md`、`docs/powershell-memory.md`。
- 已读取经验索引并按登录、权限、账号、锁定、会话关键词定位适用门禁；无直接命中“5 次锁定账号 / 15 分钟自动登出”的既有专项经验。

## BDD Scenarios

- BDD: 登录失败 5 次锁定账号 -> Given 一个启用且未锁定的后台账号，When 连续 5 次输入错误密码登录，Then 系统记录失败次数并锁定账号，后续登录返回账号锁定错误。
- BDD: 解锁账号恢复登录链路 -> Given 一个已锁定的后台账号，When 管理员通过正式解锁入口解除锁定，Then 账号锁定状态和失败计数被清零，账号可继续按正常密码策略登录。
- BDD: 成功登录清零失败计数 -> Given 一个启用且未锁定但已有失败次数的后台账号，When 使用正确密码登录成功，Then 系统清零失败次数和锁定信息。
- BDD: 普通用户禁止高权限分配 -> Given 一个普通用户账号，When 管理员尝试给该账号分配管理员角色或日志操作权限，Then 服务端拒绝保存并返回明确错误。
- BDD: 15 分钟无操作自动退出或锁屏 -> Given 用户已登录后台系统，When 前端连续 15 分钟没有键盘、鼠标或触摸操作，Then 系统触发锁屏或退出登录；When 15 分钟内有操作，Then 活跃时间被刷新。

## Command Intent Log

- 创建任务目录：`doc/tasks/20260827-login-security-controls/`。
- 经验索引检索：`rg -n "登录|权限|账号|账户|lock|session|logout|captcha|验证码|role|menu|用户" docs\experience-index.md`。
- RED: `mvn -pl yudao-module-system "-Dtest=AdminAuthServiceImplTest,AdminUserServiceImplTest,PermissionServiceTest,OAuth2GrantServiceImplTest" test` -> FAIL, 先暴露登录失败计数/锁定字段、解锁方法、角色高权限拦截和登录成功清零相关编译缺口。
- RED: `node tests/e2e/system-login-security-idle-logout-static.spec.js` -> FAIL, `src/hooks/web/useIdleLogout.ts` 缺失。
- GREEN: `mvn -pl yudao-module-system "-Dtest=AdminAuthServiceImplTest,AdminUserServiceImplTest,PermissionServiceTest,OAuth2GrantServiceImplTest" test` -> PASS, 后端定向回归 102 项全过。
- GREEN: `node tests/e2e/system-login-security-idle-logout-static.spec.js` -> PASS, 空闲退出静态契约通过。
- REGRESSION: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm exec vue-tsc --noEmit` -> FAIL, 仅剩 `src/views/form-center/template/index.vue` 的既有类型错误，和本任务改动无关。

## Verification Evidence

- 后端：`AdminAuthServiceImplTest`、`AdminUserServiceImplTest`、`PermissionServiceTest`、`OAuth2GrantServiceImplTest` 全绿。
- 前端：`tests/e2e/system-login-security-idle-logout-static.spec.js` 全绿。
- 静态回归：`pnpm exec vue-tsc --noEmit` 仍被 `src/views/form-center/template/index.vue` 的既有类型错误阻塞，已记录为无关 blocker。

## Blockers

- 无任务阻塞；仅有仓库既有前端类型错误噪声。
