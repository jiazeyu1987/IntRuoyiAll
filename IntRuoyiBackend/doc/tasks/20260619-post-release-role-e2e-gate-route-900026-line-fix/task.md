# 任务：发布后智能排产 smoke 路线 900026 产线绑定修复

## 任务目标

修复测试服 `芋道源码/zhaojie` 智能排产 smoke 在自动排产预览阶段返回 `LINE` 阻塞的问题，使发布后真实三角色验收中的智能排产链路能够从预览走到发布完成。

## 前置任务检查

- 后端最近任务 `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260616-showroom-company-honor-hall\task.md` 状态为 `COMPLETED`。
- 当前任务是维护仓 `20260618-post-release-role-e2e-gate` 的后端配套修复，不覆盖无关并行改动。

## 经验门禁

- 命中 `D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`：本次只允许使用用户明确授权的测试服 `芋道源码/zhaojie` 真实登录复现；登录失败必须记录实际租户、账号、入口和影响，不得切换账号或环境掩盖。
- 命中 `D:\ProjectPackage\Int\IntRuoyi\docs\release-backup-restore.md`：业务 SQL 必须通过维护仓发布链进入测试服；修复后必须以真实 releaseTag、远端 IMAGE_TAG 和真实 smoke 结果闭环验证。
- 命中 `D:\ProjectPackage\Int\IntRuoyi\docs\server-access.md`：测试服目标主机固定为 `172.30.30.58`，远端目录固定为 `/opt/intruoyi/runtime`；远端读写前必须确认目标主机、目标容器和授权范围。

## BDD 场景

- BDD: route 900026 棘突后续工序必须具备可用单产线 -> Given 智能排产 smoke 真实创建的 `棘突球囊扩张导管` 工单会走路线 `900026` / When 自动排产预览分析工单固定单产线 / Then `900379-900387` 对应工作站必须都绑定到同一启用产线，预览不再返回 `LINE` 阻塞。
- BDD: 发布后 zhaojie smoke 预览不再因缺少单产线失败 -> Given `芋道源码/zhaojie` 在测试服触发智能排产 smoke / When 自动排产调用 `/admin-api/mes/pro/auto-schedule/preview` / Then 返回 `blockingIssueCount = 0`，并允许继续执行发布。

## 里程碑

1. M1：建立任务文档，记录经验门禁与当前根因。`DONE`
2. M2：RED：补 SQL 回归测试，先让 route `900026` 产线绑定缺失契约失败。`DONE`
3. M3：GREEN：新增发布迁移 SQL，为 `900113-900121` 绑定启用产线 `900040` 并校验无漂移。`DONE`
4. M4：REGRESSION：运行目标 pytest 与发布迁移策略门禁。`DONE`
5. M5：测试服重新构建发布并复跑三角色真实 E2E。`DONE`
6. M6：更新证据并提交本任务相关改动。`DONE`

## 预期验证

- `python -X utf8 -m pytest script\tests\test_post_release_role_e2e_gate_sql.py -q`
- `python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql`
- 测试服 `POST /admin-api/mes/pro/auto-schedule/preview` 对 smoke 工单返回 `blockingIssueCount = 0`
- 维护仓三角色真实 E2E 至少推进到下一真实阻塞点，证明 `route 900026` 不再因缺少单产线失败

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。缺少工作站产线绑定时继续明确失败，不通过前端绕过或改脚本放宽门禁掩盖。
- `是否从根因和长期维护角度解决`：是。通过正式发布迁移修复路线 `900026` 缺失的工作站产线主数据，不做一次性手工库补。
- `是否存在临时补丁或绕过`：否。

## 当前状态

- 状态：COMPLETED。
- 已确认根因：测试服 `172.30.30.58` 上，真实 smoke 工单 `SMART-SCHED-20260619T095332038360222-MO` 调用 `/admin-api/mes/pro/auto-schedule/preview` 返回 `blockingIssueCount = 1`，阻塞项为 `processId=900379 / 棘突丝拉伸2`，消息 `工单工艺路线缺少可用单产线`。
- 已确认主数据现状：路线 `900026` 相关后续工作站 `900113-900121` 均位于 `workshop_id=900011`，但 `production_line_id` 全为 `NULL`；当前启用产线 `900040 / AUTO-LINE-01` 已位于同车间 `900011` 且具备 `calendar_plan_id=900030`。
- 已完成 RED 证据：`python -X utf8 -m pytest script\tests\test_post_release_role_e2e_gate_sql.py -q` 在新增 `20260619_post_release_role_e2e_gate_smoke_route_900026_line_fix.sql` 契约前失败，证明当前发布迁移缺少路线 `900026` 的正式产线绑定修复。
- 已完成修复：新增 `sql/mysql/20260619_post_release_role_e2e_gate_smoke_route_900026_line_fix.sql`，为 `900113-900121` 绑定 `production_line_id=900040`，并通过 `script/tests/test_post_release_role_e2e_gate_sql.py` 与 migration gate。
- 已完成验证：测试服重新构建发布 `release-20260619-1812-role-e2e-gate-route-900026-line-fix` 后，真实 smoke `preview` 不再出现 `LINE` 阻塞，三角色 E2E 已推进到新的后续阻塞 `apply -> 编码生成失败`，说明本任务范围内的路线单产线问题已闭环。
