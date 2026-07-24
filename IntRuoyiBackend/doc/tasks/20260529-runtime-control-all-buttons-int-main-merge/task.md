# 任务：运行控制台全按钮 E2E 融合进 int_main（后端）

## 任务目标

将运行控制台全按钮真实数据 E2E 的后端任务记录从 `codex/20260529-runtime-control-all-buttons-e2e` 融合进当前后端 `int_main`，并配合前端在融合结果上重新执行真实 E2E，确认融合正确。

## BDD 场景

- BDD: 后端 int_main 只融合本任务改动 -> Given 全按钮 E2E 分支相对当前 `int_main` 含非本任务差异 / When 融合运行控制台 E2E 成果 / Then 只能引入本任务提交 `379505afbe`，不得带入 DCC、Showroom、SQL 或发布脚本的无关差异。
- BDD: 融合后真实接口仍可用 -> Given 后端 `int_main` 融合完成 / When 前端 E2E 通过真实运行控制台接口执行按钮验证 / Then 所有按钮结果必须为 PASS 或明确的 LOGICALLY_BLOCKED。

## 里程碑

- [x] M1：确认当前后端 `int_main` 工作区干净，旧任务文档完成。
- [x] M2：融合本任务后端提交到 `int_main`。
- [x] M3：确认后端融合后差异仅包含本任务文件。
- [x] M4：配合前端在融合结果上运行真实数据 E2E。
- [x] M5：更新任务证据并提交。

## 预期验证

- `git cherry-pick 379505afbe` 成功。
- 后端 `git status --short` 仅出现本融合任务文档和全按钮 E2E 后端任务记录。
- 合并后的全按钮 E2E 重新通过。

## Current Status

completed

## 当前状态

completed

## 验证结果

- GREEN: `git cherry-pick 379505afbe` -> PASS，后端 `int_main` 产生提交 `8a135d6bb7`。
- GREEN: 后端 `git diff --name-status HEAD~1..HEAD` -> PASS，仅新增 `doc/tasks/20260529-runtime-control-all-buttons-e2e/` 两个任务记录文件。
- GREEN: 合并后后端 `48081` 恢复健康，前端 `int_main` 真实 E2E 通过 39 checks。
- 说明：本次未整体 merge `codex/20260529-runtime-control-all-buttons-e2e`，因为该分支相对当前 `int_main` 会带入非本任务差异。
