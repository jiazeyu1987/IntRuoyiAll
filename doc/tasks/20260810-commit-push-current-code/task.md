# 20260810 提交并推送当前代码

## Task Goal

按用户再次确认的要求，将当前 `E:\IntRuoyi` 工作区中的正式前后端源码、测试、SQL/迁移与可执行脚本作为明确授权的当前快照提交到 `int_main` 并推送到 `origin/int_main`；排除任务文档、规则文档、运行态、编译产物、审查输出、日志、PID 和凭据。

## Milestones

- [x] M1：读取 Git、任务收尾、worktree、端口和编码门禁，确认用户授权覆盖已知未完成任务与失败回归风险。
- [x] M2：盘点并稳定当前正式代码快照，运行分支运行端口守卫。
- [x] M3：显式暂存正式代码并完成文件范围、空白、敏感信息和大文件审计。
- [x] M4：创建当前代码快照提交并复扫残余正式代码。
- [ ] M5：沉淀经验、完成任务清理、提交收尾记录并推送 `int_main`。

## Expected Verification

- 用户明确授权覆盖先前已报告的未完成任务和失败回归；不把这些产品验证记录成 PASS。
- `scripts\preflight\branch-runtime-port-guard.ps1` 通过。
- `git diff --cached --check` 通过。
- staged 文件只包含 `IntRuoyiBackend/` 与 `IntRuoyiFronted/` 下正式源码、测试、SQL/迁移和可执行脚本。
- staged 文件不包含 `target*`、`.review-fix-loop`、`node_modules`、`dist`、任务文档、规则文档、日志、PID、压缩包、凭据或超大文件。
- 提交后复扫前后端正式代码残余；如并行写入产生新代码，必须单独审计处理后才能推送。
- 推送后 `int_main` 不再领先 `origin/int_main`。

## Current Status

ready_for_closeout

当前代码快照提交 `90b9f6c1521a1092030dcf870dbb62c78f099b71` 已创建；正式前后端代码残余为 0，仅保留 5 个明确排除的编译/诊断临时文件。等待 task-closeout-cleanup preview/apply、收尾记录提交和推送。

当前代码候选为 106 个已跟踪改动和 114 个未跟踪文件；已识别并排除 5 个 `target-pqc-route-snapshot*` 临时文件。分支运行端口守卫通过。

正式代码暂存区包含 215 个文件（109 个新增、106 个修改），全部位于前后端目录；空白、冲突标记、强特征凭据和大文件检查通过。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；Git 快照提交不修改产品行为。
- `是否从根因和长期维护角度解决`：是；保留已知验证缺口和授权证据，不伪造验证结果。
- `是否存在临时补丁或绕过`：否；本任务只执行用户明确授权的 Git 操作。

## Experience Gate

- `docs/experience-index.md` 存在并已读取。
- 适用 `docs/powershell-memory.md#Git 提交与推送门禁`。
- 适用 `docs/powershell-memory.md#共享分支并发基线提交门禁`。
- 适用 `docs/powershell-memory.md#批量暂存脚本被拦截时的显式路径门禁`。
- 适用 `docs/powershell-memory.md#提交后残余改动复扫门禁`。

## Cleanup Candidates

无。本任务不创建任务附属临时产物。
