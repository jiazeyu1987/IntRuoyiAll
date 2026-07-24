# 任务: 修复备份服发布后角色 E2E gate 缺少排产员角色的发布阻塞

## 任务目标

修复 `sql/mysql/20260618_post_release_role_e2e_gate.sql` 在备份服真实发布链路上的阻塞：当 `tenant_id=1 / 芋道源码` 缺少启用中的排产员角色时，发布迁移必须以正式、可持续的方式补齐所需角色并继续准备 `gaomin/zhaojie/wangsiyu` 三个发布后 E2E 账号，不能要求人工先改库，也不能让页面真实发布链路停在 `Missing enabled scheduler role`。

本任务只修改业务仓 SQL 迁移与对应契约测试；不手工写库修正备份服数据，不通过临时 SQL 绕过发布。

## 上一任务检查

- 上一后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260620-mes-scheduler-role-prod-baseline-gate\task.md`
- 状态：`COMPLETED`
- 处理：上一任务已修复 `20260617_mes_scheduler_role_smart_scheduling_tab.sql` 在无目标角色时应 no-op 的正式发布阻塞；无未收尾事项阻止本任务继续。

## 用户要求与执行边界

- 用户要求：
  - 必须按真实页面流程走完整构建发布到测试服、正式服、备份服
  - 禁止用接口替代页面动作
  - 新问题必须先记录，再修复，再回到页面链路继续
- 本任务边界：
  - 只修复导致备份服页面发布失败的业务仓 SQL/测试契约
  - 不允许用手工改库把备份服补成“看起来能过”
  - 如需改发布包内容，必须重新构建新 releaseTag 并从页面重走全链路

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 命中文档：
    - `D:\ProjectPackage\Int\IntRuoyi\docs\release-backup-restore.md`
    - `D:\ProjectPackage\Int\IntRuoyi\docs\server-access.md`
- 本任务强制门禁摘录：
  - 发布链路根因修复必须先用真实环境只读证据确认目标环境、目标主机、目标库与角色基线，不得凭记忆直接改迁移 SQL。
  - 业务仓只负责业务源码、SQL 与发布输入；发布脚本和运行控制台链路修改统一留在 `D:\ProjectPackage\Int\IntRuoyiMaintance`，本任务不能把发布问题伪装成维护仓临时脚本补丁。
  - 备份服与正式服同等级门禁；任何会改变发布包内容的修复，都必须通过契约测试和迁移策略门禁后，再回到真实页面从新 releaseTag 全链路重走。
  - 发布、备份、恢复任务不得用 mock、静默跳过、自动降级或手工预写库掩盖失败；缺少角色基线时要么由正式迁移补齐，要么明确 fail fast 报出不可满足前置条件。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。通过修复正式迁移契约，让备份服在真实基线缺少排产员角色时也能被发布包正式收口，不依赖人工预处理。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 备份服 admin 租户缺排产员角色时发布 gate 必须自补齐 -> Given tenant_id=1 芋道源码租户存在 showroom_publicity 与 wenkong 角色但缺少启用中的排产员角色 / When 执行 20260618_post_release_role_e2e_gate.sql / Then 迁移必须正式创建或启用可用的排产员角色，并继续完成 zhaojie 账号绑定与菜单授权。`
- `BDD: 已存在排产员角色时发布 gate 仍保持幂等 -> Given tenant_id=1 已存在启用中的排产员角色 / When 重复执行 20260618_post_release_role_e2e_gate.sql / Then 迁移只保持既有角色、用户和菜单契约，不重复创建脏角色，也不放宽其他 fail-fast 门禁。`

## 里程碑

1. 建立任务文档并记录备份服真实发布阻塞。`DONE`
2. 只读核对测试服/备份服 admin 租户角色与账号基线，确定根因。`DONE`
3. 先补 RED 契约测试，再最小修改 SQL。`DONE`
4. 运行目标 pytest 与迁移策略回归。`DONE`
5. 回填证据到维护仓主任务，等待重新构建新包并从页面重走。`DONE`

## 预期验证

- `python -X utf8 -m pytest script\tests\test_post_release_role_e2e_gate_sql.py -q` 先 RED 后 GREEN
- `python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql` 通过
- 维护仓主任务文档明确记录“本次修复会改变发布包内容，必须从页面重新构建新 tag 重走测试服/正式服/备份服”

## 当前状态

COMPLETED：该业务仓修复已被维护仓的新发布包输入吸收，并在真实页面链路上完成“重新构建新 releaseTag”验证。维护仓随后使用 `release-20260621-page-full-flow-mainmerge-v2` 成功通过了新的页面 `build-release`，并继续推进到测试服部署阶段；当前页面链路暴露出的新阻塞已转移为另一条独立根因 `20260619_mes_edhr_deployment_license_interface.sql` 的菜单 ID 冲突，不再是本任务修复的排产员角色 gate。由此可确认：本任务的 SQL 修复已完成职责范围，旧阻塞已被清除。
