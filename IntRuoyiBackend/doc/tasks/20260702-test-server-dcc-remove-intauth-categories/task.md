# 任务：测试服务器清理 INTAUTH 文控权限类别编码

## 任务目标

在测试服务器 `172.30.30.58` 的文控权限类别数据中，删除类别编码以 `INTAUTH` 起始的记录，只保留以 `DCC_` 起始的正式 DCC 类别编码。操作范围仅限测试服务器 DCC 文控权限类别及其直接关联规则，不操作正式服、备份服或本机业务数据。

## 经验门禁

- 命中 `docs/powershell-memory.md`：PowerShell 命令、中文文本、SQL 与远端 SSH 输出必须显式 UTF-8；不得使用 `&&` 串联命令；SQL 执行后必须做只读回查。
- 命中 `docs/server-access.md`：测试服务器 IP 为 `172.30.30.58`；测试服写入已由用户当前任务明确授权；执行前确认目标主机、目标容器、目标数据库和授权范围。
- 命中 `database-schema-delivery`：数据删除属于破坏性数据变更，必须先做只读预检、记录数据安全分析、明确恢复方案，并在执行后回查验证。
- 命中 `task-closeout-cleanup`：任务完成前先运行 cleanup preview；保留 `task.md` 与 `execution-log.md`。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，本次按明确数据口径清理测试服历史 `INTAUTH%` 类别，保留 `DCC_%` 正式类别；执行前后均用数据库断言验证。
- 是否存在临时补丁或绕过：否。

## BDD 场景

- `BDD: 清理测试服 INTAUTH 文控类别 -> Given 测试服务器文控权限类别中存在 code LIKE 'INTAUTH%' 的历史类别 / When 执行授权的数据清理 / Then 活动类别中不再存在 INTAUTH 起始编码，DCC_ 起始类别仍保留。`
- `BDD: 关联规则不悬挂 -> Given 被删除类别存在权限、分发、培训或目录绑定等关联数据 / When 清理类别 / Then 关联规则随同清理或软删除，回查不留下指向已清理 INTAUTH 类别的活动规则。`

## 里程碑

1. M1：建立任务台账、经验门禁与 BDD 场景。completed
2. M2：只读确认测试服数据库、目标表、INTAUTH 与 DCC_ 现状。completed
3. M3：执行最小范围删除并保留恢复证据。completed
4. M4：回查验证 INTAUTH 清零、DCC_ 保留、关联规则无悬挂。completed
5. M5：更新任务记录、运行 cleanup preview 并按验证结果收尾。completed

## 预期验证

- `RED: remote-select-intauth -> FAIL, 测试服仍存在 code LIKE 'INTAUTH%' 的文控类别。`
- `GREEN: remote-delete-intauth -> PASS, 授权 SQL 执行成功。`
- `GREEN: remote-readback -> PASS, code LIKE 'INTAUTH%' 活动类别为 0，code LIKE 'DCC_%' 活动类别仍大于 0。`
- `GREEN: relation-readback -> PASS, 活动关联规则不再指向已清理的 INTAUTH 类别。`

## 当前状态

completed

## 数据安全与恢复方案

- 删除范围：仅测试服务器 `172.30.30.58` 当前业务库内 `dcc_file_category.code LIKE 'INTAUTH%'` 的类别及其直接关联配置。
- 保留范围：所有 `DCC_%` 起始类别编码及非 INTAUTH 类别不删除。
- 恢复方案：执行删除前导出目标类别与直接关联记录的只读 JSON/SQL 证据到本任务目录；若回查异常，基于导出证据按原表恢复。

## 完成记录

- 目标环境：测试服务器 `172.30.30.58`，容器 `intruoyi-mysql`，数据库 `ruoyi-vue-pro`。
- 删除前只读预检：活动 `INTAUTH%` 类别 48 条，活动 `DCC_%` 类别 120 条；`INTAUTH%` 关联活动规则包括权限规则 186 条、分发规则 1 条、培训规则 1 条、审批路线 50 条。
- 执行结果：软删除 `INTAUTH%` 类别 48 条，并同步软删除直接关联配置；未删除任何 `DCC_%` 类别。
- 最终回查：活动 `INTAUTH%` 类别 0 条，已删除 `INTAUTH%` 类别 48 条，活动 `DCC_%` 类别 120 条，直接关联活动规则与受控文件引用均为 0。

## Cleanup Keep

- `doc/tasks/20260702-test-server-dcc-remove-intauth-categories/database-schema-evidence.md`
- `doc/tasks/20260702-test-server-dcc-remove-intauth-categories/pre-delete-intauth-export.tsv`
