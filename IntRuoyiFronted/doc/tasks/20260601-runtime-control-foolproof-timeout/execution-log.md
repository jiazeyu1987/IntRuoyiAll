# 执行日志：运行控制台傻瓜式运维 30 秒超时

BDD: 傻瓜式运维接口使用运维级超时 -> Given 运行控制台并发加载告警、责任矩阵、候选、探针、容量、备份点和事故数据 / When 任一接口需要远端或 NAS 读取超过 30 秒但未超过运维允许窗口 / Then 前端请求不应因全局 30 秒默认超时失败。

BDD: 超时仍显式暴露 -> Given 运维接口超过显式长超时仍未返回 / When 请求失败 / Then 页面继续显示真实 timeout 错误，不得静默降级或伪造成功数据。

REPRO: 截图显示运行控制台顶部 `傻瓜式运维：timeout of 30000ms exceeded`，对应前端 `loadFoolproofData()` 捕获九个傻瓜式运维接口任一请求失败后的总错误。

ROOT_CAUSE: `src/config/axios/config.ts` 全局默认 `request_timeout` 为 30000ms；`src/api/infra/runtimeControl/index.ts` 中 `getRuntimeControlOperations`、告警、责任矩阵、向导、候选、发布包、事故等运行控制台接口未声明显式 timeout，因此仍使用全局 30 秒。运行控制台首页并发加载这些接口，任一慢接口超过 30 秒就显示 `傻瓜式运维` 超时。

RED: `node tests/e2e/runtime-control-foolproof-timeout-static.spec.js` -> FAIL，缺少 `RUNTIME_CONTROL_FOOLPROOF_REQUEST_TIMEOUT = 70000`，且 13 个傻瓜式运维 API 未显式设置该 timeout。

GREEN: `node tests/e2e/runtime-control-foolproof-timeout-static.spec.js` -> PASS。

GREEN: `node --check tests/e2e/runtime-control-foolproof-timeout-static.spec.js` -> PASS。

VERIFY: `pnpm -s ts:check` -> FAIL，Node 默认约 4GB 堆内存 OOM，未进入类型错误输出。

GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm -s ts:check` -> PASS。
