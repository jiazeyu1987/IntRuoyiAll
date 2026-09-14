# 20260805 Merge Production Personnel Into Int Main

## Task Goal

提交 `int_main` 当前残余前端改动，然后将 `codex/20260805-production-personnel-management` 生产人员档案管理功能融合进 `int_main`，完成必要验证并推送到 `origin/int_main`。

## Milestones

- [x] 读取任务收尾、worktree、PowerShell、编码、前端、E2E 和端口规则
- [x] 读取经验索引并命中 worktree 融合、残余改动复扫、推送大文件门禁
- [x] 提交 `int_main` 残余前端改动，确保主工作区进入可合并状态
- [x] 合并生产人员功能分支并语义解决冲突
- [x] 运行端口守卫、前端静态合同、后端定向 JUnit、diff 与冲突标记门禁
- [x] 运行对象大小门禁
- [x] 推送 `int_main` 并确认功能分支已被主线包含
- [ ] 完成物理 worktree cleanup

## Expected Verification

- `git status --short --branch --untracked-files=all`
- `git diff --check`
- `scripts\preflight\branch-runtime-port-guard.ps1`
- `node tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs` 或残余 QA 页面相邻静态合同
- `node tests/e2e/production-personnel-management-static.spec.cjs`
- `pnpm ts:check`
- `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolTeamLeaderControllerTest,MesProcessPoolTeamLeaderSchemaTest,MesTeamLeaderRuntimeConfigServiceTest,MesFrontlineRuntimeConfigServiceTest,MesFrontlineRuntimeConfigControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- GitHub 100 MB 对象门禁扫描
- `git push origin int_main`

## Applicable Experience Gates

- 提交后残余改动复扫门禁：每次提交后立即复查 `git status --short --branch` 与 `git diff --name-status`，残余明确归属后单独提交。
- 多 Worktree 批量融合门禁：合并前记录 worktree 分支、HEAD、clean 状态、任务状态和验证结论；合并后证明分支 tip 已是 `int_main` 祖先。
- GitHub 推送前历史大文件门禁：推送前扫描 `origin/int_main..HEAD` 待推送对象，任一 blob 超过 100 MB 必须阻塞。
- GitHub HTTPS 443 本地代理门禁：若 push/fetch 出现代理或 443 连接错误，按代理门禁诊断，不静默切换协议。

## Current Status

ready_for_closeout

## Current Evidence

- 主工作区：`E:\IntRuoyi`，当前分支 `int_main`；因并行 dirty 改动，最终融合改在独立集成 worktree 执行，避免覆盖并行任务。
- 集成 worktree：`D:\IntRuoyiWorktree\20260805-integrate-production-personnel`，分支 `codex/20260805-integrate-production-personnel`，从 `origin/int_main` 创建并预留 slot `3`，frontend `8084` / backend `48084`。
- 功能分支：`origin/codex/20260805-production-personnel-management`。
- 语义冲突解决：保留损耗原因维护和生产人员档案管理两组控制器转换方法、schema 测试、服务测试和前端导入；保留 `bdd-tdd-design.md` 设计证据，未接受误删。
- QA 回归修复：`QaRegulationPage.vue` 手动路线候选改为 `ProRouteApi.getRouteSimpleList()`，继续通过 `ProRouteProductApi.saveRouteProductByItem()` 保存正式产品路线绑定。
- GREEN: `scripts\preflight\branch-runtime-port-guard.ps1` -> PASS for `codex/20260805-integrate-production-personnel/int_main`, frontend `8084`, backend `48084`。
- GREEN: `node tests/e2e/production-personnel-management-static.spec.cjs` -> PASS。
- GREEN: `node tests/e2e/process-loss-reason-maintenance-static.spec.cjs` -> PASS。
- GREEN: `node tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProcessPoolTeamLeaderControllerTest,MesProcessPoolTeamLeaderSchemaTest,MesTeamLeaderRuntimeConfigServiceTest,MesFrontlineRuntimeConfigServiceTest,MesFrontlineRuntimeConfigControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 32, Failures: 0, Errors: 0, Skipped: 0。
- GREEN: `git diff --cached --check` -> PASS。
- GREEN: scoped conflict marker scan on staged files with `rg -n "^(<<<<<<<|=======|>>>>>>>)"` -> PASS。
- GREEN: GitHub 100 MB object gate -> PASS, 299 objects scanned, all `<= 100MB`。
- GREEN: `git push origin HEAD:int_main` -> PASS, `origin/int_main` updated from `e9d97fa16` to `e6733202a`。
- GREEN: `git fetch origin int_main`; `git rev-parse HEAD`; `git rev-parse origin/int_main` -> both `e6733202a79f9b9cf928880067d42da68eebaf5b`。
- GREEN: `git merge-base --is-ancestor origin/codex/20260805-production-personnel-management origin/int_main` -> PASS。
- Cleanup: original worktree `D:\IntRuoyiWorktree\20260805-production-personnel-management` is removed from Git worktree registration, task-owned processes for `8082/48082` were stopped, and ports `8082/48082` no longer listen.
- Cleanup blocker: physical path `D:\IntRuoyiWorktree\20260805-production-personnel-management` still exists without `.git`; recursive physical deletion was blocked by local execution policy, so slot `1` remains active in `D:\IntRuoyiWorktree\.ports\worktree-ports.json` per registry rules until the physical path can be removed safely.

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；本任务只按正式 Git/worktree 门禁提交、合并、验证和推送。
- `是否存在临时补丁或绕过`：否。
