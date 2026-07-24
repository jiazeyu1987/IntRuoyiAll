# 任务：修复 worktree 端口脚本无法识别 codex 分支 worktree 名称

## 任务目标

- 修复 `worktree-port-map.ps1` 对 `refs/heads/codex/dcc-nas-transfer-mirror-verify-20260525` 与 `worktrees\ruoyi-vue-pro-...` 扁平 worktree 路径无法推导名称的问题。
- 保持现有 fail-fast 策略：名称可确定时继续进入配对校验；前后端 worktree 未配对时仍必须明确失败，不静默跳过。
- 通过 PowerShell 回归测试记录 RED/GREEN 证据。

## 前序任务检查

- 后端上一任务目录：`doc/tasks/20260526-test-password-update-time-schema-fix/`
- 状态：已完成。
- 处理策略：本任务仅修改本地 worktree 端口分配脚本、脚本测试与任务文档，不触碰业务代码和运行数据。

## BDD 场景

- BDD: codex 扁平后端 worktree 可推导任务名 -> Given git worktree 返回 `D:\ProjectPackage\Int\IntRuoyi\worktrees\ruoyi-vue-pro-dcc-nas-transfer-mirror-verify-20260525` 且分支为 `refs/heads/codex/dcc-nas-transfer-mirror-verify-20260525` / When 端口脚本推导 IntRuoyi worktree 名称 / Then 返回 `dcc-nas-transfer-mirror-verify-20260525`，不在名称推导阶段抛错。
- BDD: 未配对 worktree 仍失败 -> Given 后端存在 `dcc-nas-transfer-mirror-verify-20260525` worktree 但前端没有同名 worktree / When 同步端口登记表 / Then 脚本必须在前后端配对校验阶段失败并指出缺失前端，不得静默跳过。

## 里程碑

- [x] M1：补充可复现当前报错的 PowerShell 回归测试。
- [x] M2：最小修改名称推导逻辑，支持 `codex/<name>` 和历史扁平路径。
- [x] M3：运行目标脚本测试与实际命令验证，记录 RED/GREEN。
- [ ] M4：运行 task-closeout-cleanup 预览，更新任务文档并提交本任务改动。

## 预期验证

- RED：`powershell -ExecutionPolicy Bypass -File .\script\tests\test-worktree-port-map.ps1` 在新增测试后失败，原因是 `ConvertTo-IntRuoyiWorktreeName` 无法识别 codex 扁平后端 worktree。
- GREEN：同一测试在修复后通过。
- GREEN/Fail-fast：实际 `sync-int-ruoyi-worktree-ports.ps1 -NoWrite` 不再在名称推导阶段失败；若当前工作区缺少前端配对 worktree，则在配对校验阶段明确报告缺失前端。

## 当前状态

- 状态：已完成。
- 已完成：确认上一任务已完成；复现用户报错；新增 codex 扁平 worktree 名称推导回归测试；修复脚本后目标测试通过；实际只读同步已进入前后端配对校验阶段，证明原名称推导错误已消除；execution-log bug 回归证据校验通过；task-closeout-cleanup 预览确认无需要删除的临时产物。
- 当前阻塞：当前工作区存在后端 `dcc-nas-transfer-mirror-verify-20260525` worktree，但没有同名前端 worktree；端口同步仍会按设计 fail-fast 报告缺失前端，需另行创建配对前端 worktree 或清理该后端 worktree 才能完成全量同步。

## 最终验证结果

- PASS：`powershell -ExecutionPolicy Bypass -File .\script\tests\test-worktree-port-map.ps1`
- PASS：`python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260526-worktree-port-codex-name-fix\execution-log.md`
- PASS：`python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260526-worktree-port-codex-name-fix --mode preview`
