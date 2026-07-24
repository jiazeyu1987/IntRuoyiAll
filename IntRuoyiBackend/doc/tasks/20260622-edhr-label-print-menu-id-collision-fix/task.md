# 任务: 修复 eDHR 标签打印菜单 ID 冲突阻塞

## 任务目标

修复 `sql/mysql/20260618_mes_edhr_label_print_queue.sql` 在真实页面“部署测试服（v4）”阶段暴露的菜单主键冲突根因：当前脚本复用了已被 `20260618_mes_edhr_form_instance.sql` 占用的 `900272-900279` 号段，导致发布迁移在真实测试库中 fail fast，报错 `Duplicate entry '900273' for key 'system_menu.PRIMARY'`。

本任务只修改业务仓正式 SQL 发布输入与对应契约测试，不手工改测试服库，不绕过页面发布流程，不靠维护仓脚本临时补丁掩盖错误。

## 上一任务检查

- 上一后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260621-edhr-deployment-menu-id-collision-fix\task.md`
- 状态：`COMPLETED`
- 处理：上一任务修复的 `20260619_mes_edhr_deployment_license_interface.sql` 已在维护仓真实页面 `deploy-test(v4)` 日志中确认 `APPLIED`，说明 cleanup-order 根因已被真实发布链路跨过；当前新的阻塞已切换为 `20260618_mes_edhr_label_print_queue.sql` 的独立菜单号段冲突，可作为新任务继续处理。

## 用户要求与执行边界

- 用户要求：
  - 必须按真实页面点击流程重新走完整构建、测试服、正式服、备份服发布链路
  - 遇到问题先记录，再修复，再回页面重走
  - 禁止接口替代页面动作
- 本任务边界：
  - 只修复 `20260618_mes_edhr_label_print_queue.sql` 及其测试契约
  - 如需同步更新与菜单号段唯一性直接相关的测试文件，可一并最小修改
  - 不手工删除或修改测试服真实库菜单作为绕过
  - 修复会改变发布包输入，必须由维护仓重新构建新 `releaseTag` 并从页面重走全链路

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 命中文档：
    - `D:\ProjectPackage\Int\IntRuoyi\docs\release-backup-restore.md`
    - `D:\ProjectPackage\Int\IntRuoyi\docs\server-access.md`
- 本任务强制门禁摘录：
  - 页面发布链路暴露出的 SQL 阻塞必须修到正式发布输入中，不能用手工改库或跳过门禁掩盖。
  - 菜单类迁移在真实库执行前，必须先用只读证据核对目标 ID 段、父菜单和已存在菜单定义，不能凭记忆挑号。
  - 任何会改变发布包内容的修复，都必须先 RED 后 GREEN，再回维护仓重新构建新包重走页面。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。通过改正正式 SQL 使用的菜单号段并补齐跨 SQL 冲突回归测试，避免后续所有环境继续命中同类主键冲突。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: label print 菜单不得复用 eDHR form 菜单号段 -> Given 20260618_mes_edhr_form_instance.sql 已占用 900272-900279 且真实测试库已存在这些 system_menu 行 / When 执行 20260618_mes_edhr_label_print_queue.sql / Then 迁移必须使用独立且未冲突的 label-print 菜单号段，而不是复用已存在的 form 菜单 ID。`
- `BDD: 真实页面 deploy-test 在跨过 deployment SQL 后仍必须能继续执行 label print SQL -> Given 20260619_mes_edhr_deployment_license_interface.sql 已在真实测试库成功 APPLIED / When 页面 deploy-test 继续执行 20260618_mes_edhr_label_print_queue.sql / Then 不得再因 system_menu 主键冲突中断整个测试服发布链路。`

## 里程碑

1. 建立任务文档并固化真实页面 deploy-test(v4) 失败证据。`DONE`
2. 只读核对冲突 SQL 与真实菜单号段占用，确认根因。`DONE`
3. 先补 RED 契约测试，再最小修复 SQL 菜单号段。`DONE`
4. 运行目标 pytest 与迁移策略回归。`DONE`
5. 回填维护仓主任务，等待重新构建新包并从页面重走。`DONE`

## 预期验证

- `python -X utf8 -m pytest script\tests\test_edhr_label_print_queue_schema_sql.py -q` 先 RED 后 GREEN
- `python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql` 通过
- 维护仓主任务文档明确记录“label print SQL 修复改变发布包输入，必须重新 build-release 并从页面重走测试服/正式服/备份服”

## 当前状态

COMPLETED：业务仓已按严格 TDD 完成 `20260618_mes_edhr_label_print_queue.sql` 的菜单号段冲突修复。真实页面 `deploy-test(v4)` 日志、源码搜索和测试服只读 `system_menu` 现场共同证明：`900272-900279` 被 eDHR 独立表单占用，`900280-900283` 被其他 eDHR 菜单占用，因此 label print 菜单整体切换到独立的 `900320-900331`。当前已通过 `test_edhr_label_print_queue_schema_sql.py + test_edhr_form_schema_sql.py`、bug regression validator 与 `run-release-migration-policy-gate.py --sql-root sql\mysql`，维护仓主任务也已回填“必须以新的 `releaseTag` 重新 build-release 再从真实页面重走”的要求。
