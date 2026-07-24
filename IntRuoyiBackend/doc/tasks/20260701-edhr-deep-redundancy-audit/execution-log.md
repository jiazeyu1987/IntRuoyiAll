# Execution Log - 20260701-edhr-deep-redundancy-audit (后端)

BDD: 冗余模块必须可证明 -> Given 一个 eDHR 页面疑似重复 / When 检查菜单、路由、源码调用、API 测试和真实业务职责 / Then 只有四项证据均表明无生产职责时才允许删除。

BDD: 主流程继续唯一 -> Given 用户从 eDHR 批次列表处理批次 / When 进入详情页 / Then 主流程仍只经批次详情页承载，后台页不得重新成为并行主流程入口。

GREEN: task-bootstrap -> PASS，已在 `edhr_dedup_deep` 后端 worktree 建立深度冗余审计任务台账。
GREEN: experience-preflight -> PASS，已读取 `docs/powershell-memory.md`、`docs/worktree-memory.md` 与 `simplify-codebase`，本轮仅在独立 worktree 内审计和清理。

RED: redundant-route-alias-static -> FAIL，`node tests/e2e/edhr-redundant-route-alias-static.spec.js` 命中隐藏别名路由仍存在，证明 eDHR 仍有可删除的前端路由入口重复。
GREEN: route-alias-scan -> PASS，本机菜单只存在 `/mes/pro/feedback/edhr-work-task`，未发现 `pro/edhr-work-task`、`pro/edhr-recordbook`、`pro/feedback/edhr-signature`、`pro/feedback/edhr-print-task`、`pro/feedback/edhr-form-template`、`pro/feedback/edhr-form-instance` 的有效菜单行或非路由生产入口。
GREEN: redundant-route-alias-cleanup -> PASS，已删除 6 个无菜单/无有效入口的前端隐藏别名路由，仅保留正式页面入口。
GREEN: work-task-time-label-fix -> PASS，工作任务看板补充 `到期时间` 与 `逾期时间` 文案，避免时间状态语义混淆。
GREEN: frontend-static-regression -> PASS，`edhr-redundant-route-alias-static`、`edhr-existing-contract-static`、`edhr-work-task-board-static`、`edhr-recordbook-static`、`edhr-label-print-queue-static`、`edhr-form-static`、`edhr-signature-page-ui-static` 均通过。
GREEN: frontend-ts-check -> PASS，`NODE_OPTIONS=--max-old-space-size=8192 pnpm ts:check` 通过。
GREEN: backend-delete-decision -> PASS，本轮没有证明任何后端 eDHR API 可安全删除，后端不做生产代码清理。
BLOCKER: login-preflight -> 本机 Playwright headless shell 运行时报 `Invalid file descriptor to ICU data received`，该阻塞属于本机浏览器运行时问题，未作为业务放行证据。
GREEN: real-e2e-system-chrome -> PASS，使用项目允许的显式系统 Chrome 路径完成真实 E2E：`BASE_URL=http://127.0.0.1:8142`、`BACKEND_URL=http://127.0.0.1:48142`、测试租户 `aoteman`，从 eDHR 批次执行列表点击详情进入批次 `900000000463`，`/get` 与 `/workbench` 返回 200，6 个冗余别名路由均不可解析，输出 `EDHR_DEEP_DEDUP_LIST_DETAIL_REAL_E2E_PASS`。
GREEN: final-scope-decision -> PASS，本轮可删除对象限定为前端隐藏路由别名；任务看板、会签、归档、打印、表单、标签等正式能力仍有菜单、页面、API 或业务职责证据，继续保留。
GREEN: closeout-preview -> PASS，已运行 `task-closeout-cleanup --mode preview`；预览无待删除临时产物，调试脚本与调试输出目录已人工清理，保留项已写入任务文档。
GREEN: final-regression-rerun -> PASS，清理调试产物后重新运行 eDHR 静态契约、`pnpm ts:check` 与真实 E2E，全部通过。
GREEN: merged-result-verification -> PASS，前端分支已快进合并到 `int_main`；后端单任务文档提交已 cherry-pick 到 `int_main`。在合并结果上重新验证：`edhr-redundant-route-alias-static`、eDHR 既有静态契约、`pnpm ts:check` 均通过；重启主后端 `48081` 后，测试租户 `aoteman` 通过主前端 `8081` 真实进入 eDHR 批次详情，`/page`、`/get`、`/workbench` 均 200，输出 `EDHR_DEEP_DEDUP_LIST_DETAIL_REAL_E2E_PASS`。
