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

## Root Cause

1. Runner 只对识别为只读的测试项追加受控推理和最短路径约束；写入型业务页面测试继承仓库开发规则与用户级 `xhigh`，被引导执行建档、Git 和工程流程，而不是直接完成业务 UI 测试。
2. Codex 子进程工作目录是仓库根目录，进一步触发项目开发规则。
3. 非零退出错误从 stderr 头部截断，已知非致命 warning 覆盖了真实尾部错误。
4. 批记录解析节点还缺少正式固定 Word 样本，属于独立前置缺口。
5. 本地后端 `application-local.yaml` 配置了 Codex Runner 启动参数，但未配置 `yudao.codex-test.artifact-temp-dir`；当 Codex 返回失败截图 `screenshotPath` 时，Runner artifact 上传接口按设计 fail-fast，导致串行路线首节点 `BLOCKED`。

## Regression Test

- `IntRuoyiFronted/tests/e2e/codex-test-runner-readonly-timeout-static.spec.js`
- `IntRuoyiFronted/tests/e2e/codex-runner-on-demand-startup-script-static.spec.js`
- `IntRuoyiFronted/tests/e2e/codex-test-runner-failure-diagnostics-static.spec.js`
- `IntRuoyiBackend/yudao-server/src/test/java/cn/iocoder/yudao/server/CodexTestLocalConfigTest.java`
- `doc/tasks/20260730-test-management-serial-routes-repair/run-serial-routes-real-e2e.mjs`

## RED

- `node tests\e2e\codex-test-runner-readonly-timeout-static.spec.js` -> FAIL，写入型任务没有独立推理预算和统一隔离策略。
- `node tests\e2e\codex-runner-on-demand-startup-script-static.spec.js` -> FAIL，Runner 工作目录仍为仓库根目录。
- `node tests\e2e\codex-test-runner-failure-diagnostics-static.spec.js` -> FAIL，错误诊断未脱敏保留 stderr 尾部。
- `node stdin static config assertion` -> FAIL，`application-local.yaml` 缺少 `yudao.codex-test.artifact-temp-dir`。

## GREEN

- `node --check scripts\codex-test-runner.mjs` -> PASS。
- `node tests\e2e\codex-test-runner-readonly-timeout-static.spec.js` -> PASS。
- `node tests\e2e\codex-runner-on-demand-startup-script-static.spec.js` -> PASS。
- `node tests\e2e\codex-test-runner-failure-diagnostics-static.spec.js` -> PASS。
- `node tests\e2e\codex-test-runner-http-client-static.spec.js` -> PASS。
- `node tests\e2e\codex-test-runner-child-settlement-static.spec.js` -> PASS。
- `node --check doc\tasks\20260730-test-management-serial-routes-repair\run-serial-routes-real-e2e.mjs` -> PASS。
- `node stdin static config assertion` -> PASS，`application-local.yaml` 已包含运行态 artifact 临时目录和保留时长。

## Risk And Regression Scope

- 覆盖 Runner Codex 子进程隔离、失败诊断、子进程超时收敛、artifact 截图上传、本地后端运行态配置和三条正式串行路线。
- 仍需在 `48081` 加载最新配置后复跑真实页面三路线，确认 `artifact-temp-dir` 不再阻断 Runner 回写。

## Blockers And Follow-up

- 当前共享 `48081` 正由独立重启任务打包/重启，health 暂不可达；恢复前不能重新创建正式长运行批次。
- Maven 定向 `CodexTestLocalConfigTest` 首次运行超时无 surefire 报告，待并行 Maven 释放后需复跑获得正式 JUnit GREEN。
