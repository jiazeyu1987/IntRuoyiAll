# 20260728 提交推送 int_main 前后端代码

## Task Goal

按用户要求提交并推送当前 `E:\IntRuoyi` 主工作区 `int_main` 上的前端、后端代码改动及相关任务证据。

## Scope

- 当前工作区已有前端、后端和任务文档改动，本任务负责核对、验证、提交和推送。
- 不修改业务代码，不引入 fallback、mock、默认成功或异常吞噬。
- 不执行 force push、历史重写、destructive reset 或自动合并远端。

## Milestones

- [x] M1: 记录当前 Git 状态、分支、remote 和 dirty 文件清单。
- [x] M2: 运行相关后端与前端验证。
- [x] M3: 提交当前前后端代码和相关任务证据。
- [x] M4: 运行 cleanup preview/apply 并提交本任务收尾记录。
- [ ] M5: 推送 `int_main` 到 `origin` 并复核不再 ahead。

## Expected Verification

- `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordCellLinkServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `node tests/e2e/edhr-batch-product-info-virtual-process-static.spec.js`
- `node tests/e2e/edhr-visual-fill-config-static.spec.js`
- `node tests/e2e/edhr-batch-process-companion-forms-static.spec.js`
- `pnpm ts:check`
- `git diff --check`
- `git push origin int_main`

## Applicable Experience Gates

- 脏工作区基线门禁：提交前必须记录 dirty 文件，暂存区不得混入未识别文件。
- PowerShell 分号串联测试退出码门禁：关键测试逐条运行，不用最终 PASS 掩盖中间失败。
- PowerShell Maven `-D` 参数引号门禁：Maven `-Dtest` 和 `-Dsurefire.failIfNoSpecifiedTests` 参数整体加引号。
- 前端静态合同隔离门禁：前端行为用聚焦静态合同和 `pnpm ts:check` 验证。
- 后端 Maven Reactor 兄弟模块验证门禁：后端定向测试使用 `-pl yudao-module-mes -am`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，本任务只做提交推送和验证证据闭环。
- `是否存在临时补丁或绕过`：否。

## Cleanup Keep

- doc/tasks/20260728-commit-int-main-frontend-backend-code/task.md
- doc/tasks/20260728-commit-int-main-frontend-backend-code/execution-log.md
- doc/tasks/20260728-commit-int-main-frontend-backend-code/verification-report.md

## Current Status

ready_for_closeout

## Implementation Commit

- `9bd802bc fix: sync int main frontend backend changes`
- `a3e8af3c fix: keep route report options clickable`
- `cdc0d6a5 fix: simplify route report option pointer handling`
- `b5e5e6b7 fix: use native report option selection`
- `68c24d03 test: wait for route process editor readiness`
- 五次前端/后端相关提交的 `branch-runtime-port-guard` 均通过：`int_main/int_main frontend 8081, backend 48081`。
- 提交后复扫：`int_main...origin/int_main [ahead 3]`，仅剩无关并发任务文档 `doc/tasks/20260728-batch-record-product-name-dropdown/*` 和 `docs/experience-index.md` 未暂存。

## Cleanup Evidence

- Preview: `task_closeout.py --task-id 20260728-commit-int-main-frontend-backend-code --mode preview` -> `status: ready`，keep 三份任务记录，delete `<none>`，blocked `<none>`，warnings `<none>`。
- Apply: `task_closeout.py --task-id 20260728-commit-int-main-frontend-backend-code --mode apply` -> `status: applied`，deleted_paths `<none>`，blocked `<none>`，warnings `<none>`。
