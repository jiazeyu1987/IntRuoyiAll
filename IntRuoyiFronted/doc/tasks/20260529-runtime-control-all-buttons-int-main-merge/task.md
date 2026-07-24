# 任务：运行控制台全按钮 E2E 融合进 int_main（前端）

## 任务目标

将运行控制台全按钮真实数据 E2E 脚本从 `codex/20260529-runtime-control-all-buttons-e2e` 融合进当前前端 `int_main`，并在融合结果上重新运行真实数据 E2E，确认每个控制台按钮仍有效且逻辑正确。

## BDD 场景

- BDD: 前端 int_main 只融合运行控制台 E2E -> Given 全按钮 E2E 分支相对当前 `int_main` 含非本任务差异 / When 融合运行控制台 E2E 成果 / Then 只能引入本任务提交 `310c6e988`，不得带入 DCC 页面、package.json 或其他无关差异。
- BDD: 融合后每个按钮仍通过真实路径验证 -> Given 后端和前端 `int_main` 融合完成且可登录运行控制台 / When Playwright 在测试租户执行全按钮真实 E2E 并在 `芋道源码/admin` 做最终验证 / Then 39 个检查均为 PASS 或明确的 LOGICALLY_BLOCKED。

## 里程碑

- [x] M1：确认当前前端 `int_main` 工作区干净，旧任务文档完成。
- [x] M2：融合本任务前端提交到 `int_main`。
- [x] M3：确认前端融合后差异仅包含本任务文件。
- [x] M4：启动合并后的前端入口并重新执行真实数据 E2E。
- [x] M5：更新任务证据并提交。

## 预期验证

- `git cherry-pick 310c6e988` 成功。
- `node --check tests\e2e\runtime-control-all-buttons-real.e2e.js` 通过。
- `node tests\e2e\runtime-control-all-buttons-real.e2e.js` 通过，覆盖 39 checks。

## Current Status

completed

## 当前状态

completed

## 验证结果

- GREEN: `git cherry-pick 310c6e988` -> PASS，前端 `int_main` 产生提交 `dfd37d463`。
- RED: `node tests\e2e\runtime-control-all-buttons-real.e2e.js` on `8081` -> FAIL，当前 `8081` 的 `.env.local` 指向 `48098`，不是本次运行控制台后端 `48081`。
- RED: `node tests\e2e\runtime-control-all-buttons-real.e2e.js` on `8092` -> FAIL，先后暴露 `48081` 后端缺失、弹窗关闭竞态和刷新按钮 loading 竞态。
- GREEN: `node --check tests\e2e\runtime-control-all-buttons-real.e2e.js` -> PASS。
- GREEN: `node tests\e2e\runtime-control-all-buttons-real.e2e.js` on `http://127.0.0.1:8092` -> PASS，39 checks；测试租户 19 PASS / 5 LOGICALLY_BLOCKED，`芋道源码/admin` 14 PASS / 1 LOGICALLY_BLOCKED。
- GREEN: `task-closeout-cleanup --mode apply --worktree-closeout off` -> PASS，仅删除本任务 8092 前端临时日志。
- FACT: E2E 结束后已停止本任务临时前端 `8092`。
- 结果文件：`doc/tasks/20260529-runtime-control-all-buttons-int-main-merge/artifacts/runtime-control-all-buttons-results.json`。

## Cleanup Keep

- doc/tasks/20260529-runtime-control-all-buttons-int-main-merge/task.md
- doc/tasks/20260529-runtime-control-all-buttons-int-main-merge/execution-log.md
- doc/tasks/20260529-runtime-control-all-buttons-int-main-merge/artifacts/runtime-control-all-buttons-results.json
