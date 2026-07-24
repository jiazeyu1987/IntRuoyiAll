# Execution Log：eDHR Jimu 在线填写、多人电子签名与最终打印前端

BDD: 在线填写字段并签名保存 -> Given 操作员打开一张草稿 eDHR 表单 / When 修改字段并输入当前账号密码保存 / Then 页面调用字段审计保存接口并展示保存后的签名记录。

BDD: 同一张表单多人签名 -> Given 一张表单已有字段变更签名、提交签名和审批签名 / When 用户查看签名记录 / Then 页面按同一执行记录展示多条签名、签名人、动作和签名含义。

BDD: 最终表单可打印 -> Given 表单审批通过且有可封存归档权限 / When 用户生成或下载 PDF 归档 / Then 页面提供最终表单 PDF 下载入口用于打印。

- SETUP: 创建 worktree -> PASS，前端 `D:\ProjectPackage\Int\IntRuoyi\worktrees\edhr_jimu\yudao-ui-admin-vue3`，分支 `codex/edhr_jimu`。

- RED: `node scripts/edhr-execution-submit.test.mjs` -> FAIL，预期失败；`ProFeedbackEdhrFormReviewSignReqVO`、`cosignEdhrExecution` 和执行页“复核签名”入口尚未实现。
- RED: `node scripts/edhr-tracking-signature-contract.test.mjs` -> FAIL，预期失败；追踪/签名动作类型尚未包含 `FORM_REVIEW`。

- GREEN: `node scripts/edhr-execution-submit.test.mjs` -> PASS，4 tests；前端 API helper、复核签名按钮、密码弹窗、未保存字段变更门槛与刷新签名记录契约通过。
- GREEN: `node scripts/edhr-tracking-signature-contract.test.mjs` -> PASS，4 tests；追踪/签名动作类型已包含 `FORM_REVIEW`。
- GREEN: `node scripts/edhr-archive-export.test.mjs` -> PASS，8 tests；最终表单归档下载仍走受控归档接口，无 Jimu/浏览器打印 fallback。
- GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS；`pnpm ts:check` 在默认 4GB 堆内存下先因 Node heap OOM 失败，使用项目构建同等 8192MB 堆参数后类型检查通过。
- SMOKE: Playwright 打开 `http://127.0.0.1:8084/#/mes/pro/feedback/edhr-execution/detail?id=1` -> PASS，路由正常跳转登录页，无 pageerror/console error。

- CLEANUP_PREVIEW: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260608-edhr-jimu-online-fill-sign-print --mode preview` -> BLOCKED，delete `<none>`；脚本将待提交业务改动判定为 linked worktree 自动合并前的阻塞项，未执行 apply。
- FINAL: 前端实现已完成并验证通过，保留任务文档与正式契约测试。

- E2E_SETUP: `mvn -pl yudao-server -am -DskipTests package` -> PASS；后端 worktree jar 构建成功，并临时接管本机 `48081`；前端 worktree 临时接管本机 `8081`。
- GREEN: `pnpm e2e:edhr:tracking-signature` -> PASS；真实测试租户 `测试租户/aoteman` 在固定入口 `http://localhost:8081` 完成 eDHR 追踪页、详情时间线、签名页和动作筛选验证；证据见 `e2e-tracking-signature-evidence.md`。
- GREEN: `node doc/tasks/20260608-edhr-jimu-online-fill-sign-print/scripts/edhr-form-review-real-e2e.cjs` -> PASS；真实详情页执行“复核签名”，`/cosign` 返回 `FORM_REVIEW`，签名页展示“表单复核”；证据见 `e2e-form-review-evidence.md`。
- RED: `pnpm e2e:edhr:execution-list` -> FAIL；执行列表真实归档下载脚本等待浏览器 download 事件时上下文关闭，未产出 PASS 证据。
- RED: `node doc/tasks/20260608-edhr-jimu-online-fill-sign-print/scripts/edhr-archive-download-real-e2e.cjs` -> FAIL；旧 `SEALED` 归档下载返回 JSON 错误“归档文件存储侧 Retention/Object Lock/legal hold 证据校验失败，拒绝封存或下载”；改走真实页面“重新生成最终表单归档”后，后端 `/generate` 返回 `500: 归档文件保存失败`。最终表单下载/打印真实 E2E 未放行，未使用 mock、API-only 或降级路径。
- GREEN: `node doc/tasks/20260608-edhr-jimu-online-fill-sign-print/scripts/edhr-archive-download-real-e2e.cjs` -> PASS；后端修复 eDHR 专用受保护存储策略后，真实测试租户 `测试租户/aoteman` 登录固定入口 `http://localhost:8081`，进入已关闭执行记录 `40`，重新生成 `SEALED` PDF 归档并点击下载归档打印件；archiveId=`25`，downloadedBytes=`14740`，downloadedType=`application/pdf`，downloadedSha256=`6146b2141dabc9677c043802410d3c36b25b812466e1e4bc2dee15b7c50b03ca`，与最新归档 SHA-256 一致；证据见 `e2e-archive-download-evidence.md`。
- CLEANUP_PREVIEW: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260608-edhr-jimu-online-fill-sign-print --mode preview` -> BLOCKED，delete `<none>`；E2E 证据与脚本已通过 `Cleanup Keep` 明确保留，未执行 apply。阻塞原因：`codex/edhr_jimu` 不能快进合并到 `int_main`、主 worktree 脏、当前 worktree 存在运行日志未纳入本任务提交。
