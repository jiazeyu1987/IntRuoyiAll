# 任务：修复发布链路 showroom product current BU normalization guard 误拦截

## 任务目标

- 修复测试服真实发布在 `20260626_showroom_product_current_bu_normalization.sql` 阶段因 `tenant_id=0` 探针记录触发 unknown guard 而失败的问题。
- 保持 showroom product current BU normalization 的 fail-fast 契约，仅排除明确不属于业务数据归一化范围的探针记录，不放宽真实业务 BU 未识别场景。
- 在正式修复后重新恢复测试服发布推进条件；测试服未成功前，正式服与备份服不得继续。

## 当前状态

IN_PROGRESS

## 上一任务检查

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260627-release-required-sql-showroom-keyword-runtime-order\task.md`
- 状态：`COMPLETED`
- 处理说明：上一任务已完成 showroom keyword runtime SQL 依赖顺序修复并提交；本任务处理测试服继续推进后暴露出的下一个真实 blocker。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyiMaintance\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntRuoyiMaintance\docs\release-build-preflight-lessons.md`
  - `D:\ProjectPackage\Int\IntRuoyi\docs\release-backup-restore.md`
  - `D:\ProjectPackage\Int\IntRuoyi\docs\server-access.md`
  - `D:\ProjectPackage\Int\IntRuoyiMaintance\docs\agent-memory\project-error-prevention.md`
- 适用强制门禁：
  - 测试服真实发布失败后，必须先基于 operation JSON、发布日志与候选包 SQL 做只读核对，确认根因后再改代码，不得直接重试掩盖问题。
  - 高风险真实发布动作前，`execution-log.md` 必须先记录 `GREEN: experience-preflight -> PASS`；测试服未成功前，`mark-tested`、正式服、备份服全部禁止继续。
  - 正式修复必须落在真实后端 SQL 与测试里，不能通过手工改测试服数据、临时补库或关闭 guard 绕过。
  - 发布链路相关改动完成后，必须重新构建干净候选包并核对 manifest/required SQL，再从真实页面重新执行测试服发布。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。当前根因收敛为 `20260626_showroom_product_current_bu_normalization.sql` 把 `tenant_id=0`、`pipeline_layout_en='Null value probe'` 的非业务探针记录纳入 unknown guard 范围，导致测试服 fail-fast 误拦截。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 非业务探针记录不应阻塞 showroom product current BU 发布归一化 -> Given 当前 revision 中存在 tenant_id=0 且 BU 字段为 Null value probe 的探针记录 / When required SQL 执行 showroom product current BU normalization / Then 该记录应被明确排除在业务归一化与 unknown guard 之外，不得阻塞测试服发布。`
- `BDD: 真实业务未知 BU 仍必须 fail fast -> Given 当前 revision 中存在 tenant_id<>0 且 BU 字段为未识别非空值的业务记录 / When required SQL 执行 showroom product current BU normalization / Then unknown guard 仍必须保留并阻断发布，不得放宽为静默跳过。`

## 里程碑

1. M1：补充任务文档、执行日志与真实失败证据。`COMPLETED`
2. M2：补充 RED 回归测试，覆盖 `tenant_id=0 / Null value probe` 探针场景与业务未知值 guard 契约。`COMPLETED`
3. M3：完成最小正式修复并跑通定向回归。`COMPLETED`
4. M4：更新维护仓日志、提交后端修复、重建候选包并重新发起测试服发布。`IN_PROGRESS`

## 已完成工作

- 新建后端任务目录并记录真实测试服失败证据。
- 为 `showroom_product_current_bu_normalization` 补充定向 SQL 契约测试，覆盖探针排除与业务 unknown guard 保留。
- 将 SQL 修复为“仅排除明确探针签名”，避免放宽真实业务未知 BU 的 fail-fast 行为。

## 当前验证结果

- `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_showroom_product_bu_normalization_sql.py -q` -> PASS（`7 passed`）
- `python -X utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\release\run-release-migration-policy-gate.py --sql-root D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\sql\mysql` -> PASS（`status=passed`, `migrationCount=218`）

## 剩余事项

- 提交后端修复，确保只包含本任务相关文件。
- 基于修复后的后端 commit 重建干净发布 worktree，更新运行控制台 repo-root。
- 重新执行真实页面 `build-release -> publish-test`，测试服成功前不得推进正式服与备份服。

## 预期验证

- `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_showroom_product_bu_normalization_sql.py -q`
- `python -X utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\release\run-release-migration-policy-gate.py --sql-root D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\sql\mysql`

## 当前 blocker

- 测试服最新失败 operation：`op-2026-06-27T094707808570600Z-965ba79d-53d0-4848-8ee2-479dfc61b761`
- 失败 SQL：`20260626_showroom_product_current_bu_normalization.sql`
- 真实错误：`ERROR 1048 (23000) at line 75: Column 'must_be_empty' cannot be null`
- 只读核对结论：唯一未识别记录为 `revision_id=4574`、`product_id=252`、`tenant_id=0`、`pipeline_layout=NULL`、`pipeline_layout_en='Null value probe'`；当前应把该类探针记录从业务归一化 guard 范围中正式排除。
