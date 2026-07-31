# Execution Log

## Intent

用户在上一轮确认 3 条正式串行路线均失败后要求继续。本任务按“定位并修复阻塞，再通过真实页面完整复验 3 条路线”推进，不重启或停止归属不明的共享运行态。

## Rule Preflight

- Read: `docs/task-closeout-rules.md`
- Read: `docs/powershell-encoding.md`
- Read: `docs/local-runtime.md`
- Read: `docs/e2e-rules.md`
- Read: `docs/login-access.md`
- Read: `docs/experience-index.md`
- Skill: `bug-regression-fix-loop`
- Skill: `playwright`
- Existing unrelated path preserved: `doc/tasks/20260730-smart-scheduling-workorder-admission-e2e/`

## BDD Scenarios

- `BDD: Runner 可启动 Codex 完成串行路线 -> Given 本机前后端、测试租户、Runner 和 Codex CLI 前置条件有效且 3 条正式节点串完整; When 用户从测试管理页面分别顺序执行工艺路线、批记录和智能排产节点串; Then Runner 按顺序完成每个节点、持续心跳并结构化回写，3 个批次最终全部 PASS 且运行计数归零`
- `BDD: Codex CLI 配置错误应在正式路线前失败 -> Given Runner 继承了不受支持的认证、插件目录或 feature 配置; When 执行受控短预算 CLI 自检; Then 自检明确失败并阻止创建正式长运行批次，不得静默降级或等待 600000ms`
- `BDD: Runner 失败截图 artifact 可回写 -> Given Codex 子进程返回带 screenshotPath 的结构化检查点结果; When Runner 通过 artifact 接口上传临时截图; Then 本地后端必须使用运行态 artifact 临时目录保存文件并返回 artifactId，不能因缺少 artifact-temp-dir 阻断整条串行路线`

## Milestone Updates

- M1 completed: Runner 同构短预算 CLI 自检退出码 `0`，证明远程插件认证与旧 feature 警告当前为非致命 warning。
- M1 completed: 写入型任务未应用只读快速策略，Codex 子进程从仓库根目录启动并继承用户级 `xhigh`；工艺路线任务实际创建任务文档并执行 Git 基线提交，智能排产任务创建任务文档后在 600 秒内未完成业务页面测试。
- M1 completed: `resource/批记录节点-解析样本.docx` 不存在，批记录路线存在独立的正式固定样本阻塞。
- M2 completed: 已更新 Runner 隔离、统一执行参数和失败诊断静态合同，并获得预期 RED。
- M3 progress: 已生成固定样本 `resource/批记录节点-解析样本.docx`，结构校验通过；LibreOffice/soffice 不可用，未做视觉渲染。
- M3 progress: 已补充 Runner 统一执行参数、临时工作目录隔离和失败诊断尾部保留；静态 GREEN 已通过。
- M3 progress: 真实页面创建工艺路线串行批次 `37` 后，首节点已不再创建仓库任务文档，但因后端缺少 `yudao.codex-test.artifact-temp-dir`，失败截图 artifact 上传被 fail-fast 拦截，批次 `37` 终态 `FAIL`。
- M3 progress: 已新增 `CodexTestLocalConfigTest` 锁定本地 artifact 目录配置，并在 `application-local.yaml` 补齐 `artifact-temp-dir` 和 `artifact-retention-hours`。
- M3 progress: 2026-07-31 复核发现 `CodexTestLocalConfigTest` 已存在但 `application-local.yaml` 实际缺少 artifact 配置；已重新补齐 `yudao.codex-test.artifact-temp-dir` 与 `artifact-retention-hours`，并完成静态断言与 Maven 定向回归。

## Verification Evidence

- Previous verification report: `doc/tasks/20260730-test-management-serial-routes-verification/verification-report.md`
- Previous result: 工艺路线与批记录首节点 Codex 子进程 `exit 1`；智能排产首节点 `600000ms` 超时。
- CLI reproduction: Runner 同构参数执行最短结构化 prompt -> PASS；Codex CLI `0.145.0`，当前认证类型为 API key，stderr 中插件和旧 feature 信息为 warning。
- Repository side effect evidence: `doc/tasks/20260730-route-node-basic-maintenance-e2e/` 创建于工艺路线执行窗口；其日志记录自动执行了 Git 基线提交 `2e2d1eb0`。`doc/tasks/20260730-smart-scheduling-workorder-admission-e2e/` 创建于智能排产首节点执行窗口并停留 `in_progress`。
- Runtime preflight: frontend `8081` HTTP `200`；backend `48081` health `UP`；Runner session `94` `ONLINE/currentRunningCount=0`；无活动 execution/case。
- Fixed sample preflight: `E:\IntRuoyi\resource\批记录节点-解析样本.docx` missing。
- `RED: node tests\e2e\codex-test-runner-readonly-timeout-static.spec.js -> FAIL, expected reason: mutating tasks have no controlled reasoning effort or repository-isolation policy`
- `RED: node tests\e2e\codex-runner-on-demand-startup-script-static.spec.js -> FAIL, expected reason: CODEX_TEST_WORKDIR still points at repository root`
- `RED: node tests\e2e\codex-test-runner-failure-diagnostics-static.spec.js -> FAIL, expected reason: non-zero exit diagnostics preserve warning prefix instead of sanitized stderr tail`
- `GREEN: node --check scripts\codex-test-runner.mjs -> PASS`
- `GREEN: node tests\e2e\codex-test-runner-readonly-timeout-static.spec.js -> PASS`
- `GREEN: node tests\e2e\codex-runner-on-demand-startup-script-static.spec.js -> PASS`
- `GREEN: node tests\e2e\codex-test-runner-failure-diagnostics-static.spec.js -> PASS`
- `GREEN: node tests\e2e\codex-test-runner-http-client-static.spec.js -> PASS`
- `GREEN: node tests\e2e\codex-test-runner-child-settlement-static.spec.js -> PASS`
- `GREEN: node --check doc\tasks\20260730-test-management-serial-routes-repair\run-serial-routes-real-e2e.mjs -> PASS`
- `RED: node stdin static config assertion -> FAIL, expected reason: application-local.yaml missing yudao.codex-test.artifact-temp-dir`
- `GREEN: node stdin static config assertion -> PASS, application-local.yaml contains runtime-specific Codex artifact temp dir and retention hours`
- `BLOCKED/WAIT: mvn.cmd -pl yudao-server -Dtest=CodexTestLocalConfigTest test -> TIMEOUT after 180000ms with no surefire report; stopped only current task Maven PID 3952/82500 per Windows Maven timeout gate`
- Real E2E preflight: `node doc\tasks\20260730-test-management-serial-routes-repair\run-serial-routes-real-e2e.mjs --preflight-only -> PASS` using explicit Chrome executable; page filtered all three route totals `4/6/4`, Runner session `95` online, `currentRunningCount=0`.
- Real E2E partial: 页面顺序执行 `工艺路线节点闭环` -> execution `37`; final status `FAIL`, first case `BLOCKED` with `artifact 临时目录未配置`; remaining route cases correctly `BLOCKED` by serial predecessor failure.
- `GREEN: rg -n -C 3 "artifact-temp-dir|artifact-retention-hours|codex-test" IntRuoyiBackend/yudao-server/src/main/resources/application-local.yaml -> PASS, local profile now contains Codex artifact temp dir and retention hours`
- `GREEN: node stdin static config assertion -> PASS, application-local.yaml contains runtime-specific Codex artifact temp dir and retention hours`
- `GREEN: git diff --check -- IntRuoyiBackend/yudao-server/src/main/resources/application-local.yaml -> PASS, only line-ending warning reported by Git`
- `GREEN: mvn.cmd -pl yudao-server -Dtest=CodexTestLocalConfigTest test -> PASS, Tests run: 1, Failures: 0, Errors: 0, BUILD SUCCESS`
- `GREEN: python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence E:\IntRuoyi\doc\tasks\20260730-test-management-serial-routes-repair\bug-regression-evidence.md -> PASS`
- `BLOCKED/WAIT: Invoke-RestMethod http://127.0.0.1:48081/actuator/health -> connection refused after Maven GREEN; shared int_main backend still not listening, so real page three-route execution remains blocked`

## 2026-07-31 Resume After Local Restart

- Runtime recovery: backend PID `8820` started from `E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260731-144208.jar`; `http://127.0.0.1:48081/actuator/health` returned `UP`.
- Frontend recovery: standard `restart-int-ruoyi-local.ps1 -Component frontend` eventually started Vite on `8081`; the first root request returned HTTP `200`.
- Frontend blocker reproduction: subsequent real module requests (`/login?redirect=/index`, `/@vite/client`, `/src/main.ts`, and the target record page module) returned zero bytes and timed out. The Vite process remained alive with approximately `4476` handles; no frontend error was logged.
- Frontend isolation evidence: a minimal Vite server using the same frontend root without project plugins returned HTTP `200`, proving the port and basic Vite runtime are usable. This was diagnostic only and was stopped; it was not used as the test runtime.
- Execution safety: execution `40` remains the task-owned suspended batch from the previous run. No new execution was created, and no Runner was started while the real `测试记录` page was unavailable.
- Blocker: continuing requires changing or bypassing the standard `VITE_OPTIMIZE_PROFILE=windows-safe` startup path. This is not authorized under the strict no-fallback policy; pause before using a non-standard frontend runtime.

## 2026-07-31 Resume After User Runtime Confirmation

- User update: 用户确认“现在可以运行了”，允许继续真实运行态复验。
- Runtime blocker update: latest real route execution `47` reached the Codex-generated temporary Playwright script, but the child script launched default Playwright bundled Chromium and failed because `E:\Int\DevCache\playwright-browsers\chromium_headless_shell-1223\chrome-headless-shell-win64\chrome-headless-shell.exe` did not exist.
- Browser precondition: `C:\Program Files\Google\Chrome\Application\chrome.exe` exists; `C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe` exists.
- `RED: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> FAIL, expected reason: Runner lacked resolveBrowserExecutablePath() and did not pass PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH to Codex child tasks`
- Fix: `IntRuoyiFronted/scripts/codex-test-runner.mjs` now resolves a configured browser executable path first, then known local Chrome/Edge paths, passes `PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH` into `spawnCodex`, and instructs temporary Playwright scripts to launch with that executable path.
- `GREEN: node --check scripts\codex-test-runner.mjs -> PASS`
- `GREEN: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> PASS`
- `GREEN: node tests\e2e\codex-test-runner-readonly-timeout-static.spec.js -> PASS`
- `GREEN: node tests\e2e\codex-test-runner-child-settlement-static.spec.js -> PASS`
- `GREEN: node tests\e2e\codex-test-runner-http-client-static.spec.js -> PASS`
- `GREEN: node tests\e2e\codex-test-runner-failure-diagnostics-static.spec.js -> PASS`
- Real execution `48`: browser executable blocker resolved; `工艺路线节点：基础维护` reached real browser execution but generated script failed to locate the current route list page. Checkpoint 1 actual text showed the browser remained on `个人中心 / 个人工作台`, while generated script only tried hash-style candidates such as `/#/mes/route`; source route evidence shows the official Vue history route is `/mes/pro/route`.
- `RED: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> FAIL, expected reason: Runner prompt lacked official navigation hints for Vue history routes and 工艺路线 /mes/pro/route`
- Fix: `IntRuoyiFronted/scripts/codex-test-runner.mjs` now adds task-text navigation hints, states this frontend uses Vue history routes rather than hash routes, and provides official path hints for 工艺路线、批记录 and 智能排产 pages.
- `GREEN: node --check scripts\codex-test-runner.mjs -> PASS`
- `GREEN: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> PASS`
- `GREEN: node tests\e2e\codex-test-runner-readonly-timeout-static.spec.js -> PASS`
- `GREEN: node tests\e2e\codex-test-runner-child-settlement-static.spec.js -> PASS`
- `GREEN: node tests\e2e\codex-test-runner-http-client-static.spec.js -> PASS`
- `GREEN: node tests\e2e\codex-test-runner-failure-diagnostics-static.spec.js -> PASS`

## Blockers

- 当前剩余门禁是重新启动任务自有 Runner，并从真实 `系统管理 > 测试管理` 页面复跑 3 条串行路线；不得用 API-only 或静态合同替代最终页面证据。
