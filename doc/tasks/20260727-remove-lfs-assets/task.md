# 20260727 删除当前分支 LFS 资产

## 任务目标

删除当前 `int_main` 分支中的 Win7 客户端 ZIP 和奖项导出回导 Excel，并同步撤销所有正式引用，确保不留下缺失资源接口、前端入口或会重新生成仓库大文件的测试路径。

## 适用经验门禁

- Git LFS / GitHub 大文件：普通删除提交不释放历史 LFS 配额；本任务禁止历史重写、force push 或 LFS 迁移。
- PowerShell / Git：使用 PowerShell 安全命令，不使用 `&&`；提交前检查分支、remote、staged 文件和 branch runtime port guard。
- 严格无 fallback：Win7 能力直接下线，不增加替代包、占位下载或静默降级。
- 并行改动：只暂存本任务文件，不混入基线提交后继续出现的 Codex 测试管理并行改动。

## BDD 场景

- BDD: 下线 Win7 客户端下载 -> Given Win7 客户端资产已批准删除 / When 用户进入公司工作台或调用展厅客户端下载接口 / Then 仅保留 Android 下载能力且不存在 Win7 下载入口。
- BDD: 导出回导文件不进入仓库 -> Given 奖项导出回导 E2E 需要同一 Excel 完成导入 / When 测试执行结束 / Then Excel 临时文件被清理且仓库不再跟踪该文件。

## 里程碑

1. M1 变更决策与基线：记录用户批准、现状、影响和脏工作区基线。
2. M2 RED：增加下线 Win7 能力和临时 Excel 清理的失败契约。
3. M3 GREEN：删除两个 LFS 文件、LFS 属性及正式引用。
4. M4 REGRESSION：运行后端目标测试、前端静态契约、类型检查和 LFS 检查。
5. M5 收尾：更新证据、清理、提交并推送 `int_main`。

## 预期验证

- 后端 `ShowroomClientDownloadControllerTest` 通过，Android 下载仍可用且 Win7 映射不存在。
- 前端静态契约通过，不再导出或调用 Win7 下载能力。
- E2E 脚本语法检查通过，并验证 Excel 清理逻辑存在。
- `git lfs ls-files` 不再列出当前分支文件。
- `git status --short --branch` 在最终推送后不领先 `origin/int_main`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；Win7 下载能力直接删除，Android 下载失败仍按原错误链暴露。
- `是否从根因和长期维护角度解决`：是；同时删除资源、接口、前端入口、测试引用和 LFS 属性，Excel 改为临时产物并清理。
- `是否存在临时补丁或绕过`：否。

## Cleanup Keep

- doc/tasks/20260727-remove-lfs-assets/backend-api-evidence.md

## Current Status

- Status: ready_for_closeout
- 当前里程碑：M5。
- 实现和必需验证已完成，等待经验沉淀、cleanup、提交与推送。
