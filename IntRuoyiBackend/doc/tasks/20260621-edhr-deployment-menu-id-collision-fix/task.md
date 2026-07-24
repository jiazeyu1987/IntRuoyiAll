# 任务: 修复 eDHR 部署授权接口菜单 ID 冲突阻塞

## 任务目标

修复 `sql/mysql/20260619_mes_edhr_deployment_license_interface.sql` 在真实页面“部署测试服”阶段暴露的菜单 ID 冲突根因：当前脚本错误复用了已被 `eDHR统一变更` 占用的 `900296-900300` 号段，导致发布迁移在真实测试库中 fail fast，报错 `Invalid eDHR deployment page menu definition; cannot merge tenant package menu_ids`。

本任务只修改业务仓正式 SQL 发布输入与对应契约测试，不手工改测试服库，不绕过页面发布流程，不靠维护仓脚本临时补丁掩盖错误。

## 上一任务检查

- 上一后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260621-post-release-role-e2e-gate-backup-scheduler-role\task.md`
- 状态：`COMPLETED`
- 处理：上一任务修复的 post-release 排产员角色阻塞已经被新的页面构建包吸收，维护仓真实链路已推进到新的测试服阻塞，不存在未收尾事项阻止本任务继续。

## 用户要求与执行边界

- 用户要求：
  - 必须按真实页面点击流程重新走完整构建、测试服、正式服、备份服发布链路
  - 遇到问题先记录，再修复，再回页面重走
  - 禁止接口替代页面动作
- 本任务边界：
  - 只修复 `20260619_mes_edhr_deployment_license_interface.sql` 及其测试契约
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
- `是否从根因和长期维护角度解决`：是。通过改正正式 SQL 使用的菜单号段并补齐冲突回归测试，避免后续所有环境继续命中相同 ID 冲突。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: deployment 菜单切片不得复用已占用 eDHR 菜单号段 -> Given 真实测试库中 900296-900299 已被 eDHR统一变更占用且 900220 是 eDHR父菜单 / When 执行 20260619_mes_edhr_deployment_license_interface.sql / Then 迁移必须使用独立且未冲突的 deployment 菜单 ID 段，而不是覆盖或误判现有统一变更菜单。`
- `BDD: 错误半执行后的 deployment 残留菜单也必须被正式迁移吸收 -> Given 某环境曾因旧脚本失败而留下错误的 deployment permission/path 菜单残留 / When 执行修复后的 20260619_mes_edhr_deployment_license_interface.sql / Then 迁移必须正式清理或收敛旧残留，并为测试租户合并正确的 deployment 菜单和权限。`
- `BDD: 旧 deployment 残留必须先清理再插入新菜单 -> Given 真实测试库里还残留旧的 deployment path 或 permission 行，例如旧的 precheck 菜单 900300 / mes:pro-edhr-deployment:precheck / /mes/pro/feedback/edhr-deployment / When 执行 20260619_mes_edhr_deployment_license_interface.sql / Then 迁移必须先清理这些旧残留，再插入新的 900315-900319 菜单，不能先被旧残留挡住插入、再在后续清理中把旧行删掉导致最终菜单集缺项。`

## 里程碑

1. 建立任务文档并固化真实页面 deploy-test 失败证据。`DONE`
2. 只读核对真实测试库 eDHR 菜单 ID 占用，确认冲突根因。`DONE`
3. 先补 RED 契约测试，再最小修复 SQL 菜单号段与残留清理。`DONE`
4. 运行目标 pytest 与迁移策略回归。`DONE`
5. 回填维护仓主任务，等待重新构建新包并从页面重走。`DONE`

## 预期验证

- `python -X utf8 -m pytest script\tests\test_edhr_deployment_schema_sql.py -q` 先 RED 后 GREEN
- `python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql` 通过
- 维护仓主任务文档明确记录“deployment SQL 修复改变发布包输入，必须重新 build-release 并从页面重走测试服/正式服/备份服”

## 当前状态

COMPLETED：业务仓已经按严格 TDD 完成 `20260619_mes_edhr_deployment_license_interface.sql` 的两阶段根因收口。第一阶段把 deployment 菜单号段改为独立的 `900315-900319`，避免与 `eDHR统一变更` 冲突；第二阶段根据真实页面 `deploy-test(v3)` 失败日志与测试库只读现场，确认旧 `900300 / mes:pro-edhr-deployment:precheck` 残留会先挡住 `900319` 插入、再在后续清理中被删掉，最终导致 `system_menu` 只剩 4 条 deployment 菜单。当前正式修复已将 legacy cleanup 前移到新菜单插入之前，并通过 `test_edhr_deployment_schema_sql.py` 6 项用例与 `run-release-migration-policy-gate.py --sql-root sql\mysql`。维护仓主任务文档已回填“需要新的 `releaseTag` 重新 build-release 再从真实页面重走”的要求。
