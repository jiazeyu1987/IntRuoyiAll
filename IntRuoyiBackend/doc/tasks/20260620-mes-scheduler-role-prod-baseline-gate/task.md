# 任务: 修复智能排产角色迁移在正式库缺基线时的阻塞

## 任务目标

修复 `sql/mysql/20260617_mes_scheduler_role_smart_scheduling_tab.sql`，使其在正式库不存在启用中的排产/计划角色时按幂等 no-op 结束，不再因为“无目标角色可授权”阻断 code-only 正式发布。

本任务只修改发布迁移 SQL 与契约测试，不向任何环境手工补业务角色、租户包或菜单数据。

## 上一任务检查

- 上一后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260620-dcc-file-view-matrix-migration-dependency\task.md`
- 状态：`COMPLETED`
- 处理：上一任务已完成 DCC 迁移依赖修复，无遗留阻塞，本任务作为新的正式发布根因修复单独建档。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 命中文档：
    - `D:\ProjectPackage\Int\IntRuoyi\docs\release-backup-restore.md`
    - `D:\ProjectPackage\Int\IntRuoyi\docs\server-access.md`
- 本任务强制门禁摘录：
  - 发布阻塞必须先用真实库只读探查确认 schema、菜单、租户包和角色基线，不能凭记忆改 SQL。
  - `code-only` 发布不会跳过迁移门禁，缺失业务目标时必须从迁移契约层根因修复，不能靠正式库手工补角色掩盖。
  - 迁移 SQL 必须保持幂等；当目标数据不存在时，应显式 no-op，而不是把“无目标可修改”误报成发布失败。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。通过修正迁移契约，让同一发布包在“有目标角色”和“无目标角色”两种真实环境基线上都按预期执行。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 正式库无排产角色时迁移必须 no-op -> Given system_menu 与租户包菜单基线已存在但正式库没有启用中的排产/计划角色 / When 执行 20260617_mes_scheduler_role_smart_scheduling_tab 迁移 / Then 迁移应直接结束而不是 SIGNAL 失败。`
- `BDD: 存在目标角色时仍必须只授权智能排产菜单树 -> Given 某租户存在启用中的排产/计划角色且租户包包含 900120 菜单树 / When 执行 20260617 迁移 / Then 仍只同步智能排产菜单树并保持幂等。`

## 里程碑

- [x] 建立任务文档并记录正式发布真实阻塞。
- [x] 只读核对正式库菜单、租户包与角色基线。
- [x] 先补 RED 契约测试，再最小修改 SQL。
- [x] 运行相关 pytest 与迁移策略门禁。
- [x] 更新任务证据并回填正式发布主任务。

## 当前状态

COMPLETED：已将 `20260617_mes_scheduler_role_smart_scheduling_tab.sql` 修正为“无目标角色时 no-op 并清理临时表”，保留“有目标角色时仍只同步智能排产菜单树”的既有契约；相关 pytest 与迁移策略门禁均已通过。

## 最终验证

- `RED: python -X utf8 -m pytest script\tests\test_mes_scheduler_role_smart_scheduling_tab_sql.py -q -> FAIL, 1 failed 4 passed；新增契约证明旧 SQL 会把“无目标角色”误判为发布阻塞`
- `GREEN: python -X utf8 -m pytest script\tests\test_mes_scheduler_role_smart_scheduling_tab_sql.py -q -> PASS, 5 passed`
- `GREEN: python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql -> PASS, status=passed, migrationCount=166`
