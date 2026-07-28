# 20260728 再次提交推送 int_main 前后端代码

## Task Goal

按用户要求核对、提交并推送当前 `E:\IntRuoyi` 根仓库 `int_main` 中已有的前端、后端代码及相关任务证据。

## Scope

- 当前根仓库统一管理 `IntRuoyiBackend`、`IntRuoyiFronted`、`doc` 和 `docs`。
- 本任务不修改业务行为，只负责状态核对、必要验证、脏工作区基线提交、远端同步、收尾记录和推送。
- 禁止 force push、历史重写、destructive reset、静默丢弃改动或吞掉验证失败。

## Milestones

- [x] M1: 读取提交、推送、编码、端口和收尾规则，核对 Git 仓库、分支、remote 与 dirty 状态。
- [x] M2: 盘点改动归属和既有验证证据，执行本次提交所需的聚焦验证。
- [x] M3: 提交当前脏工作区基线并复扫残余改动。
- [x] M4: 同步 `origin/int_main`，处理冲突并运行提交前门禁。
- [x] M5: 推送 `int_main`，执行 cleanup preview/apply，提交并推送最终收尾记录。

## Expected Verification

- 相关后端聚焦 JUnit 或 compile 验证通过。
- 相关前端静态合同、脚本语法检查和 `pnpm ts:check` 通过。
- `git diff --check` 通过。
- `scripts\preflight\branch-runtime-port-guard.ps1` 通过。
- 待推送历史不存在超过 GitHub 100 MB 限制的 blob。
- `git push origin int_main` 通过，最终 `HEAD` 与 `origin/int_main` 一致且不再 ahead/behind。

## Applicable Experience Gates

- 提交推送前置门禁：提交前核对当前分支、remote、dirty 状态和 staged 文件清单。
- 脏工作区基线门禁：用户已明确要求提交推送当前前后端代码；先将既有 dirty 改动作为独立基线提交，不回滚或遗漏并发改动。
- 提交后残余改动复扫门禁：每次提交后立即复查状态，归属明确的残余改动单独提交。
- GitHub 大文件门禁：推送前扫描待推送历史中的大对象。
- PowerShell 编排门禁：命令不用 `&&`，关键验证逐条检查退出码，中文文档使用 UTF-8。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，本任务遵循单仓库提交、远端同步和证据闭环的正式流程。
- `是否存在临时补丁或绕过`：否。

## Cleanup Keep

- doc/tasks/20260728-commit-int-main-frontend-backend-code-round2/task.md
- doc/tasks/20260728-commit-int-main-frontend-backend-code-round2/execution-log.md
- doc/tasks/20260728-commit-int-main-frontend-backend-code-round2/verification-report.md

## Current Status

completed

## Verification Progress

- 后端聚焦 JUnit：PASS，34 tests，0 failures，0 errors。
- 前端聚焦静态合同：PASS，11 个合同逐条执行且遇错即停。
- 前端 `pnpm ts:check`：PASS。
- 真实 E2E 脚本和任务 E2E 脚本 `node --check`：PASS。
- `git diff --check`：PASS，仅行尾转换提示。
- `scripts\preflight\branch-runtime-port-guard.ps1`：PASS，`int_main` 使用 `8081/48081`。
- 敏感词检查只命中从本机环境或未跟踪本地配置读取并执行脱敏的测试逻辑，未发现硬编码密码、token、私钥或连接密钥。

## Baseline Commit

- `6b47dc8d chore: baseline current frontend backend changes`
- 40 个提交前已有文件已进入独立基线提交。
- 提交后复扫：仅剩本任务目录 `doc/tasks/20260728-commit-int-main-frontend-backend-code-round2/` 未跟踪；分支状态为 `ahead 1, behind 22`。

## Remote Merge

- `8fdf586a Merge remote-tracking branch 'origin/int_main' into int_main`
- 合并前远端推进到 `6cadc18d`，本地为 `ahead 1, behind 24`。
- 4 处文档冲突已解决：3 份同名并行工序任务记录保留远端已完成证据，`docs/experience-index.md` 同时保留本地角色填写人索引和远端 FormCenter 槽位索引。
- 合并后复验：后端 34 tests PASS，12 个前端静态合同 PASS，`pnpm ts:check` PASS，`git diff --cached --check` PASS，端口守卫 PASS。

## Experience Consolidation

- 复用现有 `docs/powershell-memory.md`，新增 `Git index.lock 陈旧锁恢复门禁`。
- 更新 `docs/experience-index.md` 的精确关键词路由。
- 未新建长期经验文档。

## Push And Cleanup

- Outgoing object scan: PASS，最大新增 blob `229153` bytes，低于 GitHub 100 MB 限制。
- `git push origin int_main`：PASS，远端从 `6cadc18d` 快进到 `8fdf586a`。
- 推送后复核：`HEAD` = `origin/int_main` = `8fdf586abcecd8dfe394a4babd42068729c2c507`。
- Cleanup preview：PASS，keep 三份正式任务记录，delete/blocked/warnings 均为空。
- Cleanup apply：PASS，deleted_paths/blocked/warnings 均为空。
- 最终任务记录与经验门禁将作为独立 closeout commit 推送。
