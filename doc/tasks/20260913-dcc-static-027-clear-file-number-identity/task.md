# DCC-STATIC-027 清空文件编号后逻辑身份同步修复

## Task Goal

修复 DCC-STATIC-027 剩余问题：正式文件基础信息修改允许 `fileNumber` 清空时，必须在同一事务内同步清除 Master 权威身份字段、规范化编号和冲突投影，且后续读取不得再通过旧编号命中新文件；正常改编号路径保持可用且不回退。

## Milestones

- [x] P001 读取仓库规则、缺陷登记和技能要求，确认当前 worktree 基线为 `origin/int_main`。
- [x] P002 补充能暴露“清空编号后旧身份索引未清除”的 BDD/RED 回归测试或静态合同。
- [x] P003 最小代码修复：清空编号时同步清除 Master 身份投影，正常改编号路径不回退。
- [x] P004 运行定向单元/合同测试、必要编译或静态检查。
- [x] P005 按 `docs/task-closeout-rules.md` 记录验证、执行 cleanup preview，并完成本任务分支提交与推送；主工作区并行改动导致自动合并收尾阻塞。

## BDD

BDD: DCC-STATIC-027 clear file number identity -> Given 已发布正式文件的 Master 保存旧 `fileNumber` 和 `normalizedFileNumber`，When 文控通过基础信息修改将文件编号清空或改为空白，Then 当前文件行和 Master 权威身份均清空编号投影，旧编号查询不得命中新文件，旧编号不再占用冲突检查。

BDD: DCC-STATIC-027 rename file number identity still works -> Given 已发布正式文件编号为 OLD，When 文控通过基础信息修改将编号改为 NEW，Then Master 权威身份同步为 NEW，NEW 可命中当前版本，OLD 不得命中且不得占用冲突检查。

## Expected Verification

- RED: 定向测试或静态合同先失败，证明清空编号时旧 Master 身份投影仍残留。
- GREEN: 同一命令通过，证明清空编号同步清除 Master `fileNumber` / `normalizedFileNumber` 及相关投影。
- REGRESSION: 正常修改编号路径测试通过；必要时运行 DCC 模块定向 Maven 编译。

## Design Constraints Check

- 不处理 DCC-STATIC-027 以外编号。
- 不引入 fallback、降级、吞异常、兼容补丁或默认成功。
- 不执行 E2E，不启动/重启服务，不写数据库，不操作远程服务器。
- 已获得 Git 提交/推送授权；仍不执行 E2E、服务启动/重启、数据库写入或远程服务器操作。
- PowerShell 命令不得使用 `&&`，中文文档保持 UTF-8。

## Current Status

blocked

实现、验证、实现提交和分支推送已完成；实现提交为 `4cd77e18a`，任务分支已推送到 `origin`。cleanup preview 已识别主工作区脏改动和非 fast-forward blocker，因此未执行 apply、主分支合并或 worktree 删除。不得触碰 `E:\IntRuoyi` 的并行改动。

## Verification Summary

- RED：静态合同在旧实现调用 `updateById` 时失败，证明空值身份字段存在 MyBatis Plus 跳过 `NULL` 的风险。
- GREEN：静态合同通过；DCC 元数据更新服务 18 个定向单元测试通过；控制器与旧身份不一致读取保护的 3 个回归测试通过；`git diff --check` 通过。
- REGRESSION：正常改编号路径覆盖 `OLD -> NEW`，清空编号路径覆盖 `fileNumber=""` 与 `normalizedFileNumber=null`，失败路径覆盖 Master 更新返回非 1 时抛出业务异常。
- E2E：未执行，符合本任务未明确要求 E2E 的范围。

## Cleanup Keep

- doc/tasks/20260913-dcc-static-027-clear-file-number-identity/task.md
- doc/tasks/20260913-dcc-static-027-clear-file-number-identity/execution-log.md
- doc/tasks/20260913-dcc-static-027-clear-file-number-identity/verification-report.md
