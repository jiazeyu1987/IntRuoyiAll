# 注册证历史导入权限发布预检修复

## 修复范围

- 维护侧构建 `JOB-e5d0db2c2d` 在 `migration-policy-and-history` 报错：`test: required target preflight missing (1): 20261124_registration_certificate_workbook_import_permission`。
- 修复提交：`462d6647fc484ef4d3433a5a640015c9559c37db`。仅新增同名 `target-preflight/*.preflight.sql`，不改原迁移、元数据、来源登记或发布门禁。
- 原迁移的 `type=menu` 已使真实发布工具强制要求目标预检；未通过修改 `requiresTargetPreflight` 或迁移类型绕过检查。
- 新预检是一条只读 `WITH ... SELECT CASE`，逐项对应原存储过程的前置断言：有效注册证页面恰好一个、其上传审批按钮恰好一个、导入菜单 ID/权限无冲突、所有未删除套餐 JSON 有效、已显式选择导入的套餐同时包含上传审批。非法 JSON 在解析前保护；返回具体阻断原因。
- 不自动选择套餐、不授权角色、不补业务数据，不执行迁移，不操作服务器。测试58和正式57的后续发布由维护侧负责，59排除。

## 固定提交 checkout 与实际包字节

- 在 `C:/SpugHandoff/release/wbp-verify-462d6647` 创建 detached、仅 SQL 目录的干净 worktree，checkout 上述完整提交；`git status --porcelain` 为空。验证后已删除该任务 checkout，无服务启动、无端口占用。
- 遵守 `IntRuoyiBackend/sql/mysql/.gitattributes` 中 `*.sql text eol=crlf`；以下摘要来自实际 checkout 文件，**不是 Git blob 的 LF 摘要**。原迁移与基线 `a330c9004fe1dafcc259de9dcf61ba97a8b07439` 的 Git 内容差异为空。
- 原迁移实际包 SHA256：`84aba32c08295bcae503d27a91179b99c56bfb32385e072f40d64114500f1b5e`。
- 新预检实际包 SHA256：`728f11f85a11957643fdde4aea03bb1bae8fb07e7160f3f1cdf7a28671fd55d0`。
- 正式绑定：`reference=sql/mysql/target-preflight/20261124_registration_certificate_workbook_import_permission.preflight.sql`，`readOnly=true`，SHA256 与资产字节一致。

## 发布输入与新迁移集合核验

使用本机真实维护工具 `C:/SpugHandoff/release/source/ops/spug/release-center/intruoyi_build.py`，直接调用 `migration_inputs`、`historical_integrity`、`target_input_plan`；未替换解析器、计划器或目标检查。`migration_inputs` 内真实来源登记、迁移策略及当前数据库契约收集通过：917条迁移、1240项资产、8项来源引用；所有已绑定目标预检均通过只读校验和包内摘要核对。

只以 `mode=ro` 读取本机 `C:/SpugHandoff/release/state/state.sqlite3` 的已登记台账快照，不连接任何目标服务器：测试849条记录、正式845条记录，历史完整性均通过。两环境均有4条待执行迁移，整个新增集合如下：

| 迁移 | 类型 | 现有门禁是否要求目标预检 | 结果 |
| --- | --- | --- | --- |
| `20261124_erp_order_sync_integrity` | schema | 否 | 已登记，APPLY，依赖顺序通过 |
| `20261124_erp_kingdee_auto_sync_retry_budget` | schema | 否 | 已登记，APPLY，依赖顺序通过 |
| `20261124_registration_certificate_workbook_import_permission` | menu | 是，非schema类型强制要求 | APPLY，本次补齐绑定 |
| `20261125_mes_pqc_aggregate_production_submit_nullable` | schema | 是，保留原有true标记 | APPLY，已有绑定有效 |

两环境各2项必需预检，缺失数均为0。仅在内存移除历史导入权限的绑定，测试与正式均准确重现各自的 `required target preflight missing (1)` 错误；原门禁仍拒绝缺失输入。

## 其他验证与交接边界

- 真实维护工具回归：`python -m unittest test_intruoyi_build.HistoryContractTests test_candidate_history -v`，18/18通过；仅历史与发布输入单元验证，无远程操作。
- 应用来源登记测试 `scripts/preflight/test_release_source_registration.py`：2/2通过。
- `check-menu-migrations.py` 普通及 `--staged` 检查通过；提交 hook 的暂存检查与分支端口检查通过；`git diff --check` 通过。
- 本机记录：`.runtime/verify-workbook-preflight-contract.py`、`.runtime/workbook-preflight-contract.log`、`.runtime/workbook-preflight-history-tests.log`。
- 本轮验证的是实际 checkout 字节、发布输入绑定和本机台账计划，未执行目标数据库 SELECT，也未宣称真实 build-release 或服务器发布通过。维护侧应从最终提交重新构建，并在58/57部署流程执行真实只读目标预检；任一业务前置条件不满足必须继续阻断。
- 既有 `docs/tasks/20261124-database-contract-gate/verification.md` 修改、未跟踪的 PQC 调查文档和 JFR 文件以 SHA256 核对原样保留，不纳入本次提交。
