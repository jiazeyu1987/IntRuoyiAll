BDD: 前端本地 API 目标与后端实际端口一致 -> Given 本地开发环境已按项目约定启动 / When 前端发起 /admin-api 请求 / Then 命中的目标端口与可用后端运行态一致。
BDD: 页面初始化失败时错误来源可定位 -> Given 本地后端不可达 / When 页面发起 dict-data 或 permission-info 请求 / Then 可以从配置与日志明确定位目标地址，不靠猜测。
INFO: previous-task-blocked -> PASS，前端上一任务已显式转 blocked，本轮可开始本地 API 配置排查。
GREEN: frontend-config-check -> PASS，`yudao-ui-admin-vue3/.env.local` 指向 `VITE_BASE_URL=http://127.0.0.1:48081`、`VITE_PROXY_TARGET=http://127.0.0.1:48081`，`vite.config.ts` 将 `/admin-api` 代理到 `env.VITE_BASE_URL`，前端未发现把接口错误改写到其它端口的代码路径。
GREEN: frontend-runtime-check -> PASS，`http://127.0.0.1:8081/login?redirect=/index` 返回 HTTP `200`，本机 Vite 进程 `node ... vite.js --mode env.local --host 0.0.0.0 --port 8081 --strictPort` 正在监听 `8081`。
BLOCKER: login-preflight-selector-stale -> FAIL，官方 `scripts/preflight/login-preflight.mjs` 在 `form.login-form:visible` 选择器处超时；只读快照显示登录页存在多个 `form.login-form`，这是前端登录前置脚本的独立老化问题，不是 48081 端口拒连根因。
