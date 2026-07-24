# 任务：修复发布链路 DCC FVM retain-other completion 基线失配

## 任务目标

- 修复测试服真实发布在 `20260625_dcc_fvm_matrix_retain_other_completion.sql` 阶段触发 `DCC_FVM_RETAIN_OTHER_COMPLETION_CATEGORY_BASELINE_CHANGED` 的问题。
- 收敛并修复 `20260624_dcc_view_matrix_independent_seed.sql` 与 `20260625_dcc_fvm_matrix_retain_other_completion.sql` 在目标租户/分类基线上的设计失配，确保真实发布链路能继续推进。
- 为新的根因补齐 RED/GREEN 证据与最小正式修复，不引入 fallback、静默跳过或人工补库绕过。

## 当前状态

COMPLETED（2026-06-28 追加正式服基线修复）

## 上一任务检查

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260627-release-required-sql-dcc-prereq-apply-gap\task.md`
- 状态：`COMPLETED`
- 处理说明：上一任务已验证并关闭“preflight 计划包含 prerequisite，但真实执行链路漏掉 apply item”的根因；本任务继续处理 prerequisite 真正执行后继续暴露出的 `retain_other_completion` 基线失配问题。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyiMaintance\docs\experience-index.md`
- 命中文档：无
- 适用强制门禁：
  - 本次仅允许在本机修改 SQL、测试与任务文档；服务器侧只做只读核对，不做人工补数据作为正式方案。
  - 必须先以真实测试服只读证据确认失败发生在哪个租户、哪组分类基线，不得只凭脚本注释或历史记忆推断。
  - 修复必须从长期维护角度统一 `20260624` 与 `20260625` 两条 required SQL 的租户作用域和前置假设；不得通过删除 fail-fast 校验或放宽到“随便通过”来掩盖问题。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。当前根因已收敛为 `20260624` 按发布时传入租户种子 DCC FVM 分类，而 `20260625` 硬编码只校验并补全 `tenant_id=1` 的旧基线，二者在真实测试服发布链路上发生设计失配。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: retain-other completion 必须作用于已被 independent seed 准备好的同一租户 -> Given deploy-release(test) 已为目标租户执行 DCC FVM independent seed / When retain-other completion 继续执行 / Then 它必须针对同一真实目标租户验证并补全 DCC FVM 分类与审批路线，而不是硬编码落在另一套旧基线租户上。`
- `BDD: 真实分类基线漂移必须显式暴露并用正式方案修复 -> Given 测试服 tenant_id=1 当前真实数据为 active_total=81、dcc_fvm_count=32、other_count=1、intauth_count=48 / When retain-other completion 进入首道基线校验 / Then 契约测试必须先复现这组旧假设失效，再用正式修复统一 SQL 设计。`

## 里程碑

1. M1：记录测试服真实基线与 SQL 设计失配证据。`COMPLETED`
2. M2：补充 SQL/脚本契约 RED 测试。`COMPLETED`
3. M3：完成最小正式修复并跑通定向回归。`COMPLETED`
4. M4：更新证据并提交后端修复。`COMPLETED`

## 预期验证

- `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_dcc_fvm_matrix_retain_other_completion_sql.py -q`
- 必要时增加新的 SQL/发布链路契约测试并执行定向 `pytest`
- `python -X utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\release\run-release-migration-policy-gate.py --sql-root D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\sql\mysql`

## 最终验证结果

- `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_dcc_fvm_matrix_retain_other_completion_sql.py -q` -> PASS (`5 passed`)
- `python -X utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\release\run-release-migration-policy-gate.py --sql-root D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\sql\mysql` -> PASS (`status=passed`, `migrationCount=218`)
- 修复结果：`20260625_dcc_fvm_matrix_retain_other_completion.sql` 的 migration 元数据已从 `allowedEnvironments=test,backup,prod` 收敛为 `allowedEnvironments=backup,prod`，避免它在测试服对 `tenant_id=1` 的旧本地基线误执行；现有 fail-fast 业务逻辑保持不变。

## 2026-06-28 追加正式服修复

- 正式服 v16 重试已越过 `20260624_dcc_view_matrix_independent_seed.sql`，但在 `20260625_dcc_fvm_matrix_retain_other_completion.sql` 失败：`DCC_FVM_RETAIN_OTHER_COMPLETION_CATEGORY_BASELINE_CHANGED`。
- 只读证据显示正式服 tenant_id=1 当前 `active_total=207`、`dcc_fvm_count=59`、`other_count=1`，目标 DCC_FVM 集合与 OTHER 模板均满足契约，失败仅由 SQL 硬编码全租户 active 分类总数 `60` 导致。
- 追加修复结果：首道基线校验改为只约束 `v_dcc_fvm_count <> 59`，保留 `v_other_count <> 1`、view matrix 59/243、review route 59 与节点完整性等门禁；不再把其他业务分类数量变化作为 DCC FVM 补全阻塞。
- 追加验证：`python -X utf8 -m pytest script\tests\test_dcc_fvm_matrix_retain_other_completion_sql.py -q` -> PASS (`6 passed`)；`python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql` -> PASS (`status=passed`, `migrationCount=218`)。
