# 任务：按 worktree 自动分配本地前后端端口

## 任务目标

- 为 IntRuoyi 本机多 worktree 并行开发建立统一端口分配：`int_main` 固定为前端 `8081`、后端 `48081`。
- 当前已存在的其他成对 worktree 按顺序递增分配 `8082/48082`、`8083/48083` 等。
- 后续新增成对 worktree 时，通过本地脚本自动登记下一个可用端口，不复用历史端口。
- 将端口应用到现有本地重启和状态检查脚本，避免多个 worktree 默认抢占 `8081/48081`。

## 维护性评估

- 采用集中式 PowerShell 辅助脚本维护端口规则，避免分散修改 `.env.local` 或 `application-local.yaml` 造成脏改动和误提交。
- 本机端口登记表存放在工作区根目录 `worktrees\.ports\worktree-ports.json`，作为机器本地运行状态；仓库提交脚本和任务文档，不提交本机登记表。
- 对缺失前端/后端成对 worktree、重复端口、缺失 `int_main` 等前置条件直接失败，不做隐式 fallback。

## 前序任务检查

- 已检查最近同仓任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260524-showroom-manual-release-live-revision-blocker\task.md`
- 上一任务状态：`进行中`
- 影响：该任务为展厅发布阻塞排查，当前任务只修改本地运维脚本和任务文档，不修改展厅业务代码、不提交该未完成任务的文件。

## 里程碑

- [x] M1：建立任务记录、明确端口分配规则和失败条件。
- [x] M2：先写端口映射测试并看到 RED。
- [x] M3：实现 worktree 端口同步与查询脚本。
- [x] M4：把动态端口接入本地重启和状态脚本。
- [x] M5：同步当前 worktree 端口登记表并完成验证。
- [x] M6：更新任务文档、执行 cleanup 预览并提交本任务变更。

## 预期验证

- `powershell -ExecutionPolicy Bypass -File .\script\tests\test-worktree-port-map.ps1`
- `powershell -ExecutionPolicy Bypass -File .\script\deploy\sync-int-ruoyi-worktree-ports.ps1 -Json`
- `powershell -ExecutionPolicy Bypass -File .\script\deploy\show-int-ruoyi-local-status.ps1 -WorktreeName int_main -Json`
- 验证当前登记表中 `int_main=8081/48081`，其他现有 worktree 依次增加。

## 当前状态

状态：已完成

## 最终验证结果

- PASS：端口规则测试通过。
- PASS：当前 worktree 端口登记表已生成，`int_main=8081/48081`，已有成对 worktree 保持登记，新出现 worktree 自动使用历史最大端口后的下一组。
- PASS：本地状态脚本可按 `WorktreeName` 解析对应前端/后端端口。
- PASS：bootstrap evidence 校验通过。
- PASS：task-closeout-cleanup 预览无待删除文件、无阻塞。
