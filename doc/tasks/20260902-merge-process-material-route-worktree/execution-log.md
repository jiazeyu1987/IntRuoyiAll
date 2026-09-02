# Execution Log

2026-09-02T00:00:00+08:00 REQUEST 用户要求“将 worktree 里的内容融合进 int_main”。

2026-09-02T00:00:00+08:00 PRECHECK 已读取：
- docs\task-closeout-rules.md
- docs\worktree-restrictions.md
- docs\codex-branch-runtime-handoff.md
- docs\branch-runtime-ports.md
- docs\request-command-log.md

BDD: merge completed worktree into int_main -> Given 已验证的附加 worktree 包含工艺路线输入/输出物料改动, When 将该 worktree 内容融合进 int_main, Then int_main 保留工艺路线按文档定义的输入物料和输出物料行为且不破坏端口契约。

2026-09-02T13:57:00+08:00 IDENTIFY 待融合 worktree 为 `D:\IntRuoyiWorktree\20260902-route-process-input-output-materials`，分支 `codex/20260902-route-process-input-output-materials`。该 worktree 的 10 个已修改文件和 1 个新增静态 E2E 脚本已与 `E:\IntRuoyi` 主工作区逐文件 SHA256 核对一致。

2026-09-02T13:57:57+08:00 GREEN `mvn.cmd -pl yudao-module-mes "-Dtest=MesProRouteFlowConfigServiceImplTest,MesProRouteServiceImplTest,MesFrontlineProcessMaterialServiceTest" test` -> PASS，71 tests, 0 failures, 0 errors。

2026-09-02T13:58:00+08:00 GREEN `node IntRuoyiFronted\tests\e2e\route-process-input-output-materials-static.spec.cjs` -> PASS。

2026-09-02T13:58:00+08:00 GREEN `powershell -NoProfile -ExecutionPolicy Bypass -File scripts\preflight\branch-runtime-port-guard.ps1` -> PASS，int_main/int_main frontend 8081 backend 48081。

2026-09-02T13:59:00+08:00 REGRESSION `pnpm exec vue-tsc --noEmit -p tsconfig.relaxed.json --pretty false` -> FAIL，未设置 8GB Node heap 时 OOM；按前端规则使用单实例 `$env:NODE_OPTIONS='--max-old-space-size=8192'` 重跑。

2026-09-02T14:00:00+08:00 GREEN `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm exec vue-tsc --noEmit -p tsconfig.relaxed.json --pretty false` -> PASS。
