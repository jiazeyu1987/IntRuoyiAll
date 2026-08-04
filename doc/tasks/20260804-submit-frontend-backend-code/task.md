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

in_progress

- 已识别此前提交 `af1bfb191 chore: baseline current frontend backend updates before push`。
- 已识别后续本地提交，当前分支仍需推送到 `origin/int_main`。
- 已确认本轮用户明确要求推送，覆盖此前“不推送”的旧口径。
- 用户已明确最终口径：同时保留 `PQC组长` 独立页签和 `生产组长` 独立页签。
- 已把 eDHR 顶部页签、隐藏路由、页面关系图和静态合同统一为 `组长工作台` + `生产组长` + `PQC组长` 三入口。
- 已完成本地提交 `b98d82594 chore: submit current frontend backend updates`，包含当时已验证的前端、测试、任务证据和经验文档改动。
- 用户最新口径已再次复验：`生产组长` 与 `PQC组长` 均保留为 eDHR 独立页签，PQC 包装页、隐藏路由和页面关系图节点均已恢复。
- 当前剩余工作：提交最新残余改动、运行提交前门禁、推送并复核远端同步状态。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；按项目 Git 门禁做提交、推送、复扫和对象大小检查。
- `是否存在临时补丁或绕过`：否。

## Applicable Gates

- `docs/powershell-memory.md#Git 提交与推送门禁`
- `docs/powershell-memory.md#提交后残余改动复扫门禁`
- `docs/powershell-memory.md#GitHub 推送大文件门禁`
- `docs/task-closeout-rules.md#提交规则`
