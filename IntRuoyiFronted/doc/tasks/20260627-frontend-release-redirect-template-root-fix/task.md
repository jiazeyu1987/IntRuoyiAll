# 任务：修复发布重定向页空模板构建阻塞

## 任务目标

- 修复前端发布构建因重定向壳页面空模板触发 `vue/valid-template-root` 的阻塞。
- 为同类重定向页补充静态回归，防止再次以空模板形式进入发布链路。
- 完成最小修复后恢复 `vite build --mode test` 可通过，支撑维护控制台继续执行测试服/正式服/备份服发布。

## 当前状态

IN_PROGRESS

## 上一任务检查

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260627-frontend-closeout-commit\task.md`
- 状态：`COMPLETED`
- 处理说明：已核对前序任务完成，本次作为新的前端构建回归修复单独记录。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyiMaintance\docs\experience-index.md`
- 命中文档：无
- 适用强制门禁：
  - 本次仅修改本机前端源码与静态回归，不执行真实 E2E、服务器写入、发布、恢复或 worktree 清理。
  - 修复必须围绕构建根因完成，禁止用关闭 lint、绕过构建或静默降级替代正式修复。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。根因是重定向页使用空模板，不满足 Vue 模板根节点校验；将以最小可编译根节点和静态回归一并修复。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 重定向壳页面可编译 -> Given DCC 与 eDHR 的遗留审批入口页只负责 mounted 后跳转到统一审批中心 / When 前端执行静态契约检查与 test 模式构建 / Then 页面仍保留原有 redirect 行为，且模板必须提供合法根节点，不得再因空模板阻塞发布构建。`

## 里程碑

1. M1：记录任务与构建阻塞现状。`COMPLETED`
2. M2：补充重定向页模板根节点静态回归并取得 RED 证据。`COMPLETED`
3. M3：完成最小修复并跑通静态回归与 test 构建。`COMPLETED`
4. M4：记录结果并提交前端修复。`IN_PROGRESS`

## 预期验证

- `node tests/e2e/redirect-template-root-static.spec.js`
- `node tests/e2e/approval-center-phase5-retirement-static.spec.mjs`
- `pnpm build:test`

## 最终验证结果

- `node tests/e2e/redirect-template-root-static.spec.js` -> PASS
- `node tests/e2e/approval-center-phase5-retirement-static.spec.mjs` -> PASS
- `pnpm build:test` -> PASS
