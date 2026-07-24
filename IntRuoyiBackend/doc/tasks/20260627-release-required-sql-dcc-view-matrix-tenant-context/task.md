# 任务：修复发布链路 DCC 查阅矩阵 seed 发布前置契约

## 任务目标

- 修复 `publish-int-ruoyi.ps1` 在执行 `20260624_dcc_view_matrix_independent_seed.sql` 时未注入必需租户上下文，导致测试服真实发布失败的问题。
- 修复 `20260624_dcc_view_matrix_independent_seed.sql` 与 `script/dcc_view_matrix_test_tenant_prereq_20260624.sql` 仍假设旧测试租户组织树，导致测试服真实发布在主体预检查阶段失败的问题。
- 为发布脚本与 DCC seed / 测试租户前置 SQL 补充契约测试，锁定该发布链路必须显式绑定测试租户 `tenant_id=122`，并与当前测试服真实组织树一致。
- 恢复维护控制台 `deploy-release(test)` 真实链路可继续推进。

## 当前状态

COMPLETED

## 上一任务检查

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260627-release-build-frontend-memory-guard\task.md`
- 状态：`COMPLETED`
- 处理说明：已核对前序发布脚本任务完成，本次作为新的 required SQL 发布回归修复单独记录。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyiMaintance\docs\experience-index.md`
- 命中文档：无
- 适用强制门禁：
  - 本次仅修改本机发布脚本、契约测试与任务文档，不直接手工修改服务器数据库状态。
  - 修复必须围绕正式发布脚本入口完成，不得通过人工在服务器临时 `SET` 变量或跳过该 SQL 作为替代。
  - 测试租户写入只能落在 `tenant_id=122`，不得默认切到 `tenant_id=1` 或其他租户。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。根因分两层：一是发布脚本在 required SQL 执行链路缺少针对显式租户上下文 seed 的注入规则；二是 DCC seed 与测试租户前置 SQL 仍固化旧组织树，已与测试服 tenant `122` 当前真实部门层级漂移。本次将把发布脚本上下文注入与测试租户组织映射同时固化到正式契约。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 发布测试服执行 DCC 查阅矩阵 seed -> Given release package 包含 20260624_dcc_view_matrix_independent_seed.sql 且该 SQL 明确要求外部设置 @dcc_view_matrix_seed_tenant_id / When 发布脚本在测试环境执行 required SQL / Then 必须在同一 mysql 会话内先显式设置 @dcc_view_matrix_seed_tenant_id := 122，再执行该 SQL，避免真实页面 deploy-release 因缺少上下文失败。`
- `BDD: 测试租户主体映射保持与真实组织树一致 -> Given 测试服 tenant_id=122 当前真实组织树中 市场营销中心 挂在 瑛泰医疗 下、注册服务中心 挂在 顶级部门 下，且不存在 市场服务部 / 生产一车间 / 瑛泰医疗下注册服务中心 / When DCC 查阅矩阵 seed 与测试租户前置 SQL 为测试服准备主体与角色 / Then 契约必须直接匹配当前真实组织树，不再引用已不存在的旧部门层级。`

## 里程碑

1. M1：记录真实发布失败根因与发布脚本现状。`COMPLETED`
2. M2：补充脚本契约回归并取得 RED 证据。`COMPLETED`
3. M3：完成脚本修复并跑通契约回归。`COMPLETED`
4. M4：记录结果并提交后端修复。`COMPLETED`

## 预期验证

- `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_publish_int_ruoyi_to_test_tooling.py -q`
- 如需附加定向契约：`python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_dcc_view_matrix_independent_seed_sql.py -q`

## 最终验证结果

- `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_publish_int_ruoyi_to_test_tooling.py D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_dcc_view_matrix_independent_seed_sql.py D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_dcc_view_matrix_excel_seed_sql.py -q` -> PASS (`95 passed`)
- `python -X utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\dcc_view_matrix_independent_seed_sql_test.py` -> PASS
- `python -X utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\dcc_view_matrix_test_tenant_prereq_sql_test.py` -> PASS
