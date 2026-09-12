# Task: 本地运行目录跨电脑可移植

## Task Goal

移除 IntRuoyi 对 `E:\IntRuoyi`、`D:\ProjectPackage` 和 `D:\IntRuoyiWorktree` 的固定目录依赖，使不同电脑可从当前 Git 仓库根目录运行，同时保留 profile、端口、槽位登记与进程归属门禁。

## Milestones

- [x] 建立任务记录与 BDD
- [x] 新增路径可移植 RED 合同
- [x] 更新权威规则与运行脚本
- [x] 运行 GREEN/REGRESSION 验证
- [x] 完成 cleanup 收尾

## Expected Verification

- 任意绝对目录中的 `int_main` 默认解析为 `int_main`，使用 `8081/48081`。
- 特殊的 `int_main_d` 通过显式 `INTRUOYI_RUNTIME_PROFILE` 选择，不依赖盘符。
- worktree 槽位登记允许用户指定任意共同根目录，不再硬编码 `D:`。
- 权威规则不再要求固定电脑盘符。
- PowerShell parser 与定向 Python 合同通过。

## BDD

- Given 项目在不同电脑位于不同绝对目录，When 从当前 Git 仓库运行 `int_main`，Then 系统按分支/default profile 使用 `8081/48081`，不因盘符或父目录不同而拒绝。
- Given 同一仓库需要附加 worktree，When 通过槽位分配脚本登记目录，Then 只要求目标是所选 worktree 根的直属或下级目录，并保持 profile/slot/端口唯一。
- Given 同一 `int_main` 分支需要 `int_main_d` 独立端口，When 显式设置 `INTRUOYI_RUNTIME_PROFILE=int_main_d`，Then 使用 `8101/48101`。

## 设计约束检查

- 不弱化端口冲突、进程归属、槽位唯一性检查。
- 不猜测特殊 profile；特殊 profile 必须显式配置。
- 不引入 fallback、降级、吞异常或模拟成功。
- 不执行 E2E、数据库写入、提交或推送。

## Current Status

completed
