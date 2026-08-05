# 20260804 Submit Frontend Backend Code

## Task Goal

按用户明确要求提交并推送当前 `int_main` 上的前后端相关代码、测试、任务证据和经验文档，推送后确认本地分支不再领先 `origin/int_main`。

## Milestones

- [x] M0: 读取 Git、PowerShell、编码和收尾门禁。
- [x] M1: 复核当前分支、远端、暂存区、工作区残余改动和已完成验证证据。
- [x] M2: 将 eDHR 冲突口径统一为同时保留 `生产组长` 与 `PQC组长` 独立页签。
- [x] M3: 复核当前分支与远端状态，并识别本地已领先 `origin/int_main`。
- [ ] M4: 提交当前已验证的残余改动。
- [ ] M5: 推送并复核 `git status --short --branch` 不再 dirty/ahead。

## Expected Verification

- `git diff --cached --check`
- `node tests\e2e\edhr-batch-record-leader-tabs-static.spec.js`
- `node tests\e2e\edhr-batch-page-graph-tab-static.spec.js`
- `node tests\e2e\mes-process-pool-team-leader-static.spec.js`
- `pnpm ts:check`
- `scripts\preflight\branch-runtime-port-guard.ps1`
- GitHub 大文件对象扫描无超过 100MB blob
- `git push origin int_main`
- `git status --short --branch`

## Current Status

ready_for_closeout

- 已识别此前提交 `af1bfb191 chore: baseline current frontend backend updates before push`。
- 已识别后续本地提交，当前分支仍需推送到 `origin/int_main`。
- 已确认本轮用户明确要求推送，覆盖此前“不推送”的旧口径。
- 用户已明确最终口径：同时保留 `生产组长` 与 `PQC组长`，二者都是类似 `批次执行` 的 eDHR 顶部同级独立页签，不是 process-pool 左侧菜单独立入口。
- 已把 eDHR 顶部页签、隐藏路由、页面关系图和静态合同统一为 `组长工作台` + `生产组长` + `PQC组长` 三入口。
- 已完成本地提交 `b98d82594 chore: submit current frontend backend updates`，包含当时已验证的前端、测试、任务证据和经验文档改动。
- 用户最新口径已再次澄清：`生产组长` 与 `PQC组长` 必须按 eDHR 顶部同级页签建模，路由使用 `/mes/pro/feedback/edhr-batch-production-leader` 与 `/mes/pro/feedback/edhr-batch-pqc-leader`。
- 2026-08-05 复扫确认当前 `HEAD` 与 `origin/int_main` 一致，但工作区仍有残余前后端、测试和多任务证据改动。
- 代码级提交门禁通过：`git diff --check`、`pnpm ts:check`、eDHR 组长页签静态合同、QA 规程静态合同、审批中心静态合同、DCC adapter 静态合同、MES/DCC 目标 Maven 测试和端口 guard 均已通过。
- 当前提交被真实 E2E 前置阻塞：QA 规程状态真实 E2E 缺正式 `IDI -> productMasterId` 绑定；排产局部重排 fixture E2E 缺显式 `MES_PARTIAL_REPLAN_E2E_PASSWORD` / `MES_REPLAN_E2E_PASSWORD` 环境变量。
- 用户已明确授权接受上述两个真实 E2E 前置 blocker 后继续提交推送；本轮仍未引入 fallback、默认产品、默认账号或静默降级。
- 当前剩余工作：提交残余改动、推送 `origin/int_main`，再标记完成并提交最终收尾记录。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；按项目 Git 门禁做提交、推送、复扫和对象大小检查。
- `是否存在临时补丁或绕过`：否。

## Applicable Gates

- `docs/powershell-memory.md#Git 提交与推送门禁`
- `docs/powershell-memory.md#提交后残余改动复扫门禁`
- `docs/powershell-memory.md#GitHub 推送大文件门禁`
- `docs/task-closeout-rules.md#提交规则`
