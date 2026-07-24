# 任务：本地管理端 48081 接口拒连排查（前端）

- Task ID: `20260701-local-admin-api-48081-connection-refused`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- Created: `2026-07-01`
- Current Status: `completed`

## Task Goal

核对并修复前端本地 API 目标配置、Vite 代理或运行时基址，使本地页面初始化请求不会再指向不可达的 `http://localhost:48081/admin-api/...`。

## Previous Task Check

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260701-showroom-product-excel-audio-keyword-roundtrip\task.md`
- 状态：`blocked`
- 处理说明：该任务已因当前本地联调入口不可用而暂停，不阻塞本轮前端配置/联调排查。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 命中 `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。仅修复真实本地 API 目标和联调合同，不通过静默跳过初始化请求规避报错。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 前端本地 API 目标与后端实际端口一致 -> Given 本地开发环境已按项目约定启动 / When 前端发起 /admin-api 请求 / Then 命中的目标端口与可用后端运行态一致。`
- `BDD: 页面初始化失败时错误来源可定位 -> Given 本地后端不可达 / When 页面发起 dict-data 或 permission-info 请求 / Then 可以从配置与日志明确定位目标地址，不靠猜测。`

## Milestones

1. M1：建立前端任务台账并锁定当前本地 API 目标来源。`completed`
2. M2：补 RED 复现或配置断言，确认拒连根因。`completed`
3. M3：实施最小修复并完成 GREEN 验证。`completed`

## Expected Verification

- `rg -n "48081|admin-api|baseUrl|proxy|VITE" D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`

## Current Blockers

- 暂无。

## Final Verification Result

- 前端本地环境文件 `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\.env.local` 明确把 `VITE_BASE_URL` / `VITE_PROXY_TARGET` 指向 `http://127.0.0.1:48081`，`vite.config.ts` 也将 `/admin-api` 代理到该地址；未发现前端代码把请求错误指到其他端口的漂移。
- 当前前端 dev server 正常：
  - `node ... vite.js --mode env.local --host 0.0.0.0 --port 8081 --strictPort` 正在监听 `8081`；
  - `http://127.0.0.1:8081/login?redirect=/index` 返回 HTTP `200`。
- 结论：本次 `ERR_CONNECTION_REFUSED` 不是前端 API 基址配置错误，而是用户打开页面时本地 `48081` 后端尚未完成启动。
- 非阻塞补充发现：官方 `scripts/preflight/login-preflight.mjs` 已不适配当前登录页多表单结构，后续若要依赖该脚本做真实登录前置，需要单独修复选择器。
