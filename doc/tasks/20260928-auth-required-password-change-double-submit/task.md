# 强制改密重复提交修复

## Goal

确保强制改密表单一次点击或 Enter 只调用一次 `change-password-before-login`，避免成功后旧密码状态再次触发请求。

## BDD

- Given 用户处于登录前强制改密表单，且当前没有提交中的改密请求；
- When 用户点击提交按钮或在确认密码框按 Enter；
- Then 两种交互均走唯一的表单 submit 路径，同一时刻最多发起一个改密请求，成功后仅以 `rememberMe=false` 调用正常登录并保存 token。
- Given 一次改密请求仍在进行；
- When 表单再次触发 submit；
- Then pending guard 阻止第二次请求，且提交尝试后的密码清理规则保持不变。

## Milestones

1. 读取仓库规则并检查当前组件/静态测试。
2. RED：静态合同要求唯一 submit 路径及 pending guard，并在当前实现上失败。
3. GREEN：调整 `LoginForm.vue` 与登录静态测试。
4. 回归：指定静态测试、其他登录静态测试、`pnpm ts:check`、`git diff --check`。

## Expected Verification

- `node tests/e2e/login-required-password-change-static.spec.cjs`
- 其他 `login-*-static.spec.cjs` 与登录相关静态合同
- `pnpm ts:check`
- `git diff --check`（只读差异空白检查，不执行 Git 写操作）

## Design Constraints

- 写集仅限 `IntRuoyiFronted/src/views/Login/components/LoginForm.vue`、前端登录静态测试及本任务目录。
- 当前密码仅保存在内存 ref；提交尝试后清空。
- 强制改密后的正常登录固定 `rememberMe=false`；仅改密成功后调用正常登录并保存 token。
- 不写密码入 URL、localStorage 或 sessionStorage。
- 不接触后端、数据库、服务及 Git 写操作。

## Current Status

blocked

实现和验证已完成；按当轮约束不执行 Git 收尾，故暂不标记 completed。
