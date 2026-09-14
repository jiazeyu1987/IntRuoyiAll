# Bug Regression Evidence

## Bug Summary

- Runner 协议端点为 `@PermitAll`，没有登录用户上下文。
- Runner 注册和 artifact 上传会写入带 `creator/updater NOT NULL` 约束的表。
- MyBatis 通用填充只能在存在登录用户时填充操作者，导致本机 Runner 注册真实探针失败。

## Expected Behavior

- Runner 协议在 token 校验和管理租户头通过后，应能注册/上传证据，并以固定系统操作者 `codex-runner` 写入审计字段。
- 缺 token、缺管理租户或目标 schema 错误仍必须 fail fast。

## Reproduction

- Real probe: POST `/admin-api/system/codex-test-runner/register` with valid Runner token and `tenant-id=1` -> 500, DB error `Column 'creator' cannot be null`.
- RED: `mvn.cmd -pl yudao-module-system "-Dtest=CodexTestRunnerServiceImplTest,CodexTestArtifactServiceImplTest" test` -> FAIL because `creator/updater` were null.

## Root Cause

- Runner 协议是非登录机器协议，不能依赖 `SecurityFrameworkUtils.getLoginUserId()` 填充审计字段。
- 修复前服务层未对 Runner 自身创建的记录设置系统操作者。

## Regression Tests

- `CodexTestRunnerServiceImplTest#registerRunner_stampsAuditFieldsWithoutLoginUser`
- `CodexTestArtifactServiceImplTest#saveArtifact_stampsAuditFieldsWithoutLoginUser`

## Verification

- GREEN: `mvn.cmd -pl yudao-module-system "-Dtest=CodexTestRunnerServiceImplTest,CodexTestArtifactServiceImplTest" test` -> PASS
- GREEN: `mvn.cmd -pl yudao-module-system "-Dtest=CodexTestRunnerControllerTest,CodexTestRunnerServiceImplTest,CodexTestArtifactServiceImplTest" test` -> PASS

## Blockers

- No remaining blocker for Runner registration.
- The requested downstream “作废测试” execution remains blocked because that test item is absent from current system data.

## Risk

- Scope is limited to Codex Runner service writes.
- No global DB fill behavior or authentication model was changed.

## Follow-up Regression: Runner Offline / Stuck Codex Child

- Bug summary and expected behavior: 页面点击“执行”不应因为 Runner 一次性注册后退出或长时间 Codex 子进程占用而提示 `没有在线 Codex Runner`；Runner 执行期必须持续心跳，超时或取消时必须终止任务自有 Codex 子进程。
- Reproduction command or path: `run-void-test-from-ui.mjs` 通过真实页面点击已有测试项 `排产工单手动重排 881MO093613/881MO093615`，创建 `executionId=3` 后观察到 Windows `codex` 后代进程超过超时时间仍持有 stdio，执行项停留 `CLAIMED/RUNNING`。
- Root cause: Runner loop 只依赖空轮询 claim 刷新在线状态，执行中缺少可靠心跳；Windows 下 `.cmd` 包装进程退出后，后代 `node/codex.exe` 继续持有 stdio，单独 `child.kill()` 无法让 Runner promise 结束。
- Additional root cause: 后端被本地运行任务重启时，Runner loop 对 claim 阶段 `ECONNREFUSED` 未做 loop 级恢复，Node 进程直接退出，导致旧 session heartbeat 过期。
- Regression test added or updated: focused Runner static contract 增加心跳、超时、`taskkill.exe` 进程树终止、按 outputFile 终止后代进程、服务器取消信号、`BLOCKED` 回写和 loop 级重新注册合同。
- RED command and expected failure: 真实页面点击创建 `executionId=3` 后只读 DB 轮询显示 `CLAIMED/RUNNING` 且 Runner heartbeat 后续过期，符合 Windows 子进程树未终止的失败形态。
- GREEN command and passing result: `node --check scripts/codex-test-runner.mjs` -> PASS；focused Codex Runner static contract -> PASS；当前 Runner 会话 `id=7` 在线，`heartbeat_age_seconds=3`；真实页面行级“执行”创建 `executionId=4` 且未出现 `没有在线 Codex Runner`。
- Risk and regression scope: 修改仅限本地 Codex Runner 脚本和本任务启动脚本；不改变业务测试项数据，不新增 fallback，不吞异常。
- Blockers and follow-up actions: “作废测试”测试项仍不存在，需正式创建或恢复后才能执行该目标项。
