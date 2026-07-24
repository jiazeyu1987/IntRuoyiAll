# 20260601-runtime-control-foolproof-timeout

## 任务目标

修复运行控制台顶部出现 `傻瓜式运维：timeout of 30000ms exceeded` 的问题，确保运行控制台傻瓜式运维区域所依赖的远端/NAS/探针读取接口使用显式长超时，不再落回全局 30 秒默认值。

## 前置检查

- 上一前端任务 `doc/tasks/20260601-unocss-entry-module-not-found/task.md` 状态为 `completed`。
- 当前前端仓库存在既有未提交改动 `src/views/showroom-admin/shared/structuredError.ts`、`doc/tasks/20260530-runtime-control-nas-assets.pre-merge-untracked-20260531_005256/`、`scripts/showroom-structured-network-error.test.mjs`，本任务不触碰、不提交。

## BDD 场景

BDD: 傻瓜式运维接口使用运维级超时 -> Given 运行控制台并发加载告警、责任矩阵、候选、探针、容量、备份点和事故数据 / When 任一接口需要远端或 NAS 读取超过 30 秒但未超过运维允许窗口 / Then 前端请求不应因全局 30 秒默认超时失败。

BDD: 超时仍显式暴露 -> Given 运维接口超过显式长超时仍未返回 / When 请求失败 / Then 页面继续显示真实 timeout 错误，不得静默降级或伪造成功数据。

## 里程碑

- [x] M1：建立任务文档、BDD 场景与预期验证。
- [x] M2：补充失败回归测试，证明傻瓜式运维接口仍使用默认 30 秒。
- [x] M3：为运行控制台傻瓜式运维接口增加显式长超时。
- [x] M4：运行目标验证并记录证据。
- [x] M5：收尾清理预览并提交本任务改动。

## 预期验证

- `node tests/e2e/runtime-control-foolproof-static.spec.js` 先 RED 后 GREEN。
- 运行控制台 API 文件中傻瓜式运维相关接口不再依赖全局 `request_timeout: 30000`。

## 当前状态

status: completed

## 完成记录

- 根因：运行控制台傻瓜式运维接口中多项仍继承 Axios 全局 30000ms 超时；并发加载时任一慢接口超时都会显示 `傻瓜式运维：timeout of 30000ms exceeded`。
- 修复：新增 `RUNTIME_CONTROL_FOOLPROOF_REQUEST_TIMEOUT = 70000`，并应用到运行控制台傻瓜式运维相关 API。
- 验证：`node tests/e2e/runtime-control-foolproof-timeout-static.spec.js` RED -> GREEN；`node --check tests/e2e/runtime-control-foolproof-timeout-static.spec.js` PASS；`$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm -s ts:check` PASS。
- 收尾：`task_closeout.py --task-id 20260601-runtime-control-foolproof-timeout --mode preview` PASS，无删除项、无阻塞。

## Cleanup Keep

- `doc/tasks/20260601-runtime-control-foolproof-timeout/bug-regression-evidence.md`
