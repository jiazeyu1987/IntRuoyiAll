# 任务: 修复 eDHR 打印策略菜单主键冲突

## 任务目标

修复 `sql/mysql/20260618_mes_edhr_print_policy_reissue_void.sql` 在维护仓真实页面 `deploy-test(v9)` 阶段暴露的新根因：测试服真实库执行到 line 103 时抛出 `Duplicate entry '900285' for key 'system_menu.PRIMARY'`。修复必须落在正式发布输入与对应回归测试中，不允许手工改测试服库、不允许跳过 SQL、也不允许通过维护仓脚本增加临时绕过。

## 上一任务检查

- 上一后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260622-edhr-oq-pq-legacy-cleanup-order-fix\task.md`
- 状态：`COMPLETED`
- 处理：上一任务已修复 OQ/PQ legacy cleanup 顺序缺陷，并已被维护仓真实页面 `deploy-test(v9)` 证明成功跨过。当前新的阻塞已切换到后续 `20260618_mes_edhr_print_policy_reissue_void.sql` 的菜单主键冲突，因此需作为新任务继续处理。

## 用户要求与执行边界

- 用户要求：
  - 主程序合并后必须重新走一遍“构建发布包 -> 部署测试服 -> 标记测试通过 -> 上线正式服 -> 上线备份服”的真实页面链路
  - 遇到问题先记录，再修复，再回页面重走
  - 禁止接口替代页面动作
- 本任务边界：
  - 只修复 `20260618_mes_edhr_print_policy_reissue_void.sql` 及其直接相关的回归测试/证据
  - 允许做测试服只读核对以确认真实占用现场，不允许手工写测试服 `system_menu`
  - 本任务任何修复都会改变发布包输入；维护仓必须使用新的 `releaseTag` 从真实页面重新 `build-release -> deploy-test`

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 命中文档：
    - `D:\ProjectPackage\Int\IntRuoyi\docs\release-backup-restore.md`
    - `D:\ProjectPackage\Int\IntRuoyi\docs\server-access.md`
- 本任务强制门禁摘录：
  - 发布链路暴露出的正式 SQL 问题必须回到业务发布输入中修复，不能靠手工改库、跳过 SQL 或测试数据回填掩盖。
  - 对测试服的现场核对只能做只读查询；任何写入必须回到维护仓真实页面发布流程触发。
  - 修复发布输入前必须先有 RED 契约，再做最小 SQL 修改，并在维护仓重新构建新包验证。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。必须找到 `900285` 冲突的正式 SQL 号段问题，切到独立未冲突方案并补齐回归测试。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 打印策略 SQL 不能复用已占用 menu id -> Given 真实测试库中 900285 已被其他 eDHR 菜单占用 / When 执行 20260618_mes_edhr_print_policy_reissue_void.sql / Then SQL 必须使用独立未冲突的 menu id，并完整落下打印策略页面与按钮菜单。`
- `BDD: 打印策略修复后真实页面 deploy-test 不得再因 900285 冲突中断 -> Given 维护仓真实页面 deploy-test(v9) 已证明当前失败点是 print policy SQL / When 修复后的 SQL 重新进入 clean 发布包并参与真实页面发布 / Then 测试服发布链路不得再在该 SQL 阶段因 system_menu 主键冲突失败。`

## 里程碑

1. 建立任务文档并固化维护仓 `deploy-test(v9)` 失败证据。`DONE`
2. 只读核对 SQL 与真实 `system_menu` 占用现场，确认根因。`DONE`
3. 先补 RED 契约测试，再最小修复 SQL。`DONE`
4. 运行目标 pytest、迁移策略与缺陷证据回归。`DONE`
5. 回填维护仓主任务，等待重新构建新包并从页面重走。`TODO`

## 预期验证

- `python -X utf8 -m pytest script\\tests\\test_edhr_print_policy_reissue_schema_sql.py -q` 先 RED 后 GREEN
- `python -X utf8 script\\release\\run-release-migration-policy-gate.py --sql-root sql\\mysql` 通过
- `python -X utf8 C:\\Users\\BJB110\\.codex\\skills\\bug-regression-fix-loop\\scripts\\validate_bug_regression.py --evidence D:\\ProjectPackage\\Int\\IntRuoyi\\ruoyi-vue-pro\\doc\\tasks\\20260622-edhr-print-policy-menu-id-collision-fix\\bug-regression-evidence.md` 通过
- 维护仓主任务明确记录“print policy 修复改变发布包输入，必须用新的 releaseTag 从真实页面重走测试服/正式服/备份服”

## 当前状态

COMPLETED_WAITING_MAIN：维护仓真实页面 `deploy-test(v9)` 暴露的 print policy 菜单主键冲突已在业务仓收口。当前只读现场已证明 `900283-900289` 被交付驾驶舱与验证包矩阵占用，源码也证明 `20260618_mes_edhr_flow_intervention_log.sql` 复用了 `900286-900292`；正式 SQL 已把 print policy 菜单整体切换到独立未冲突的 `900338-900346`，并通过 `python -X utf8 -m pytest script\\tests\\test_edhr_print_policy_reissue_schema_sql.py -q`、`python -X utf8 script\\release\\run-release-migration-policy-gate.py --sql-root sql\\mysql` 与 `python -X utf8 ...validate_bug_regression.py --evidence ...bug-regression-evidence.md`。下一步必须回维护仓用新的 `releaseTag` 从真实页面重新 `build-release -> deploy-test -> mark-tested -> prod -> backup`。
