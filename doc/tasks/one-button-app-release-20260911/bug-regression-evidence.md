# 发布按钮静态审查缺陷回归证据

## Bug summary and expected behavior

静态代码审查判定发布按钮不能放行：`app-release` 与低层 action 断裂、旧脚本路径和旧 `/actions` 绕过 workflow、稳定等待态被 heartbeat 误杀、test lease 泄漏、底层进程先于 workflow 绑定启动、前端固定操作排序第一条 workflow。

## Expected

期望行为：按钮发布必须是通用 workflow 驱动的 `app-release` 程序包发布；服务端先持久化 workflow operation 绑定再启动底层进程；低层发布动作必须要求 workflow 上下文；READY/TEST_DEPLOYED/TESTED 等人工等待态不因 heartbeat 超时失败；test/prod lease 按 workflowId+environment 隔离并正确释放；前端必须显式选择 workflowId。

## Reproduction command or path

- `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-infra -am "-Dtest=RuntimeControlServiceImplTest,ReleaseWorkflowOrchestratorTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `node tests/e2e/runtime-control-one-button-static.spec.cjs`

## Root Cause

按钮 workflow 与 RuntimeControl 低层发布动作没有同一份服务端合同：低层 action 仍按旧 scope/旧脚本/旧直调入口建模，workflow 派发前没有 durable operation 绑定，lease 只按 workflowId 保存且 TESTED 后未释放，heartbeat recovery 未区分执行态与人工等待态，前端默认取 `releaseWorkflows[0]`。

## Regression tests added or updated

- `RuntimeControlServiceImplTest`：覆盖 app-release、维护仓脚本/workingDirectory、workflow 上下文必填、preassigned operationId、旧直调阻断。
- `ReleaseWorkflowOrchestratorTest`：覆盖派发前持久绑定、READY 等等待态不误杀、TESTED 后释放 test lease、operationId 预分配。
- `runtime-control-one-button-static.spec.cjs`：覆盖前端显式 workflow selector、禁止 `releaseWorkflows[0]`、禁止旧 scope union。

## RED command and expected failure

- RED: Maven 目标测试 -> FAIL，新增字段/workingDirectory/上下文合同缺失。
- RED: 前端静态合同 -> FAIL，页面仍使用 `releaseWorkflows.value[0]`。

## GREEN command and passing result

- GREEN: `node tests/e2e/runtime-control-one-button-static.spec.cjs` -> PASS。
- GREEN: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-infra -am "-Dtest=RuntimeControlServiceImplTest,ReleaseWorkflowOrchestratorTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，80 tests。
- GREEN: `corepack pnpm run ts:check` -> PASS。

## Verification

上述 GREEN 命令均已在应用 worktree 执行通过；旧脚本路径、旧 scope 与固定第一条 workflow 的源码扫描无命中。

## Risk and regression scope

本轮只修改按钮发布 workflow 与本机合同测试，不操作服务器、不发布新 releaseTag、不执行正式服/审查服/mark-tested/promote-prod/promote-backup/MinIO/全量数据库同步。下一轮必须用新应用 commit 与全新 releaseTag 重新 build-release -> publish-test 验证真实运行态。

## Blockers and follow-up actions

R80 不复用。需要提交应用修复，再由维护仓 source freeze 绑定新应用提交，生成新的 releaseTag 并执行测试服 app-release publish-test 验收。
