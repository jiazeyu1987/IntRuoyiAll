# Task: 提交推送前后端代码并启动本地运行态

## 任务目标

在 `int_main` 分支上验证当前后端和前端改动，先修复验证或启动暴露的真实错误，再提交并推送代码，最后启动并核对本地前后端运行态。

## 里程碑

1. 完成任务范围、规则、当前分支和工作区基线核对。
2. 执行后端定向回归、前端类型检查、前端本地构建和分支端口门禁；若失败，按 BDD + RED/GREEN 修复。
3. 只提交当前请求范围内的后端、前端及必要任务记录，推送 `int_main` 到 `origin`。
4. 使用标准脚本启动 `int_main` 前后端，验证 `8081` 前端入口和 `48081` 后端健康检查。
5. 标记 `ready_for_closeout`，执行 cleanup preview/apply，补齐验证报告和最终状态。

## 预期验证

- 后端 runtime-control 定向测试和相关脚本测试通过。
- 前端 `pnpm ts:check` 与 `pnpm build:local` 通过。
- `scripts/preflight/branch-runtime-port-guard.ps1` 通过。
- `git diff --check` 通过，提交文件清单不包含凭据、临时产物或本机端口登记状态。
- `git push origin int_main` 成功，推送后本地分支不再领先 `origin/int_main`。
- 标准本地重启脚本完成；`http://127.0.0.1:8081/` 返回 HTTP 200，`http://127.0.0.1:48081/actuator/health` 返回 `status=UP`。
- cleanup preview/apply 保留 `task.md`、`execution-log.md` 和 `verification-report.md`。

## BDD / TDD 适用性

本任务主体是提交、推送和本地运行态操作，不新增产品行为。若验证发现当前改动存在代码错误，必须在 `execution-log.md` 中补充对应 `BDD:`、`RED:`、`GREEN:` 和回归记录，并只提交修复后的代码。

## 设计约束检查

- 仅使用 `int_main` 固定端口 `8081/48081`，不随机换端口。
- 不停止归属不明或非当前 `int_main` 的进程。
- 不提交 `intrruoyi-runtime/worktree-ports.json` 等本机运行态登记文件。
- 不使用 fallback、mock 成功、吞异常或跳过真实失败。
- 保留已有用户或并行任务改动；提交前按文件清单核对范围。

## Cleanup Candidates

- `doc/tasks/20260919-submit-push-start-local-runtime/tmp-pytest/`

## Cleanup Keep

- `doc/tasks/20260919-submit-push-start-local-runtime/task.md`
- `doc/tasks/20260919-submit-push-start-local-runtime/execution-log.md`
- `doc/tasks/20260919-submit-push-start-local-runtime/verification-report.md`

## Current Status

completed
