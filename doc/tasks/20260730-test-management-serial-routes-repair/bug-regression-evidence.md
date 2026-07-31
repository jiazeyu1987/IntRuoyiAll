# Bug Regression Evidence

## Bug Summary

测试租户中的 3 条正式串行路线均无法完整跑完：两个首节点的 Codex 子进程退出码为 `1`，另一个首节点达到 Runner `600000ms` 超时。

## Expected Behavior

Runner 应在正式执行前验证 Codex CLI 可用性，并在真实页面发起后按节点串顺序完成全部节点、结构化回写检查点结果。

## Reproduction

- Runner 同构短预算 `codex exec` 自检当前可以成功，说明 stderr 前段的插件认证和旧 feature 信息本身不是致命错误。
- 上一轮写入型首节点从仓库根目录启动，未应用只读任务的受控推理与执行限制。
- 工艺路线首节点创建了 `doc/tasks/20260730-route-node-basic-maintenance-e2e/` 并执行 Git 基线提交；智能排产首节点创建了 `doc/tasks/20260730-smart-scheduling-workorder-admission-e2e/` 后达到 `600000ms` 超时。
- 批记录首节点要求的 `E:\IntRuoyi\resource\批记录节点-解析样本.docx` 不存在。
- 修复 Runner 隔离与固定样本后，从真实页面顺序执行 `工艺路线节点闭环` 创建批次 `37`；首节点不再触发仓库任务文档/Git 流程，但回写失败截图时被后端配置缺口阻断：`artifact 临时目录未配置`。
- 2026-07-31 复核发现 artifact 配置回归测试已存在，但 `application-local.yaml` 实际未包含对应本地配置；补齐配置后，静态断言和 Maven 定向测试均通过。
- 最新真实执行 `47` 证明 Codex 子任务已经开始运行临时 Playwright 脚本，但脚本依赖默认 Playwright 浏览器缓存；本机缓存 `chromium_headless_shell-1223` 不存在，导致首节点 `BLOCKED`，后续串行节点按前置失败阻断。
- 真实执行 `48` 证明浏览器 executablePath 阻塞已解除，但 Codex 临时脚本仍停在 `个人中心 / 个人工作台`；脚本只猜测了 `/#/mes/route` 等 hash 路由，未使用当前 Vue history 正式入口 `/mes/pro/route`。

## Root Cause

1. Runner 只对识别为只读的测试项追加受控推理和最短路径约束；写入型业务页面测试继承仓库开发规则与用户级 `xhigh`，被引导执行建档、Git 和工程流程，而不是直接完成业务 UI 测试。
2. Codex 子进程工作目录是仓库根目录，进一步触发项目开发规则。
3. 非零退出错误从 stderr 头部截断，已知非致命 warning 覆盖了真实尾部错误。
4. 批记录解析节点还缺少正式固定 Word 样本，属于独立前置缺口。
5. 本地后端 `application-local.yaml` 配置了 Codex Runner 启动参数，但未配置 `yudao.codex-test.artifact-temp-dir`；当 Codex 返回失败截图 `screenshotPath` 时，Runner artifact 上传接口按设计 fail-fast，导致串行路线首节点 `BLOCKED`。
6. Runner 只把前端 `node_modules` 传给隔离 Codex 子任务，没有同时传入本机正式 Chrome/Edge executablePath，也没有在 prompt 中要求临时 Playwright 脚本显式使用该路径；因此子任务在缺少 Playwright 浏览器缓存的本机上仍会调用默认缓存浏览器并失败。
7. Runner prompt 没有把当前前端 history 路由和正式页面路径传给自然语言子任务；Codex 生成的脚本沿用 hash 路由猜测，导致真实页面导航失败后首节点 `BLOCKED`。

## Regression Test

- `IntRuoyiFronted/tests/e2e/codex-test-runner-readonly-timeout-static.spec.js`
- `IntRuoyiFronted/tests/e2e/codex-runner-on-demand-startup-script-static.spec.js`
- `IntRuoyiFronted/tests/e2e/codex-test-runner-failure-diagnostics-static.spec.js`
- `IntRuoyiBackend/yudao-server/src/test/java/cn/iocoder/yudao/server/CodexTestLocalConfigTest.java`
- `IntRuoyiFronted/tests/e2e/codex-test-runner-playwright-dependency-static.spec.js`
- `doc/tasks/20260730-test-management-serial-routes-repair/run-serial-routes-real-e2e.mjs`

## RED

- `RED: node tests\e2e\codex-test-runner-readonly-timeout-static.spec.js -> FAIL, expected reason: 写入型任务没有独立推理预算和统一隔离策略`
- `RED: node tests\e2e\codex-runner-on-demand-startup-script-static.spec.js -> FAIL, expected reason: Runner 工作目录仍为仓库根目录`
- `RED: node tests\e2e\codex-test-runner-failure-diagnostics-static.spec.js -> FAIL, expected reason: 错误诊断未脱敏保留 stderr 尾部`
- `RED: node stdin static config assertion -> FAIL, expected reason: application-local.yaml 缺少 yudao.codex-test.artifact-temp-dir`
- `RED: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> FAIL, expected reason: Runner 未解析本机 Chrome/Edge executablePath，未传递 PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH 给子任务`
- `RED: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> FAIL, expected reason: Runner prompt 缺少 Vue history 路由和工艺路线正式入口 /mes/pro/route`

## GREEN

- `GREEN: node --check scripts\codex-test-runner.mjs -> PASS`
- `GREEN: node tests\e2e\codex-test-runner-readonly-timeout-static.spec.js -> PASS`
- `GREEN: node tests\e2e\codex-runner-on-demand-startup-script-static.spec.js -> PASS`
- `GREEN: node tests\e2e\codex-test-runner-failure-diagnostics-static.spec.js -> PASS`
- `GREEN: node tests\e2e\codex-test-runner-http-client-static.spec.js -> PASS`
- `GREEN: node tests\e2e\codex-test-runner-child-settlement-static.spec.js -> PASS`
- `GREEN: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> PASS`
- `GREEN: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> PASS, includes browser executablePath and official navigation hints`
- `GREEN: node --check doc\tasks\20260730-test-management-serial-routes-repair\run-serial-routes-real-e2e.mjs -> PASS`
- `GREEN: node stdin static config assertion -> PASS, application-local.yaml 已包含运行态 artifact 临时目录和保留时长`
- `GREEN: mvn.cmd -pl yudao-server -Dtest=CodexTestLocalConfigTest test -> PASS, Tests run: 1, Failures: 0, Errors: 0, BUILD SUCCESS`

## Verification

- `Verification: 真实 E2E preflight -> PASS, 三条节点串筛选数量为 4/6/4，Runner session 95 ONLINE/currentRunningCount=0`
- `Verification: 真实 E2E partial -> BLOCKED, 工艺路线节点闭环 execution 37 因 artifact 临时目录未配置失败；该配置缺口已补齐但 48081 尚未恢复，不能继续页面复验`
- `Verification: Runner browser dependency static regression -> PASS, 子任务环境现在包含前端 Playwright 依赖与本机浏览器 executablePath prompt/环境变量`
- `Verification: 真实 E2E execution 48 -> BLOCKED, 浏览器启动已正常但临时脚本未进入 /mes/pro/route；已用静态契约补齐正式导航提示，待复跑真实页面`

## Risk And Regression Scope

- 覆盖 Runner Codex 子进程隔离、失败诊断、子进程超时收敛、artifact 截图上传、本地后端运行态配置和三条正式串行路线。
- 仍需在真实页面复跑三路线，确认 `artifact-temp-dir`、本机浏览器 executablePath 与正式 history 路由导航提示均不再阻断 Runner 回写。

## Blockers And Follow-up

- 当前剩余工作是重启任务自有 Runner 并通过真实 `系统管理 > 测试管理` 页面复跑 3 条串行路线；不得用 API-only、静态合同或缓存浏览器下载替代最终页面证据。
