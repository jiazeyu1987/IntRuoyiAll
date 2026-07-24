# 任务：发布后智能排产 apply 自动编码流水恢复修复

## 任务目标

修复测试服 `芋道源码/zhaojie` 智能排产 smoke 在 `/admin-api/mes/pro/auto-schedule/apply` 阶段因为 `PRO_TASK_CODE` 自动编码流水缓存丢失而报 `编码生成失败` 的问题，使发布后真实三角色验收中的智能排产链路能够从 `preview` 成功走到 `apply` 完成。

## 前置任务检查

- 后端上一任务 `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260619-post-release-role-e2e-gate-route-900026-line-fix\task.md` 已按其范围 `COMPLETED`：`preview` 阶段的 `route 900026` 单产线阻塞已修复，真实 smoke 已推进到新的 `apply` 阻塞点。
- 当前任务继续服务于维护仓 `20260618-post-release-role-e2e-gate`，仅处理智能排产 `apply` 的自动编码根因，不覆盖无关并行改动。

## 经验门禁

- 命中 `D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`：本次只允许使用用户明确授权的测试服 `芋道源码/zhaojie` 真实登录复现；登录失败必须记录实际租户、账号、入口和影响，不得切换账号或环境掩盖。
- 命中 `D:\ProjectPackage\Int\IntRuoyi\docs\release-backup-restore.md`：修复必须通过正式构建发布链进入测试服；修复后必须以真实 releaseTag、远端 IMAGE_TAG 和真实三角色 E2E 结果闭环验证。
- 命中 `D:\ProjectPackage\Int\IntRuoyi\docs\server-access.md`：测试服目标主机固定为 `172.30.30.58`，远端目录固定为 `/opt/intruoyi/runtime`；远端读写前必须确认目标主机、目标容器和授权范围。

## BDD 场景

- BDD: 自动编码流水缓存丢失后必须按数据库最新流水恢复 -> Given `PRO_TASK_CODE` 的 Redis 流水 key 因重启或过期丢失 / When 自动排产 `apply` 再次生成任务编码 / Then 系统必须从 `mes_md_auto_code_record` 已存在的最大 `serial_no` 继续分配，而不是回退到起始号导致重复。
- BDD: 发布后三角色验收中的智能排产 apply 不再因编码回退失败 -> Given 测试服 `芋道源码/zhaojie` 真实 smoke 的 `preview` 已无阻塞 / When 调用 `/admin-api/mes/pro/auto-schedule/apply` / Then 不再因 `MesMdAutoCodeRecordServiceImpl.generateAutoCode` 的重复编码检查抛出 `编码生成失败`。

## 里程碑

1. M1：建立任务文档，记录经验门禁、前置任务和当前根因。`DONE`
2. M2：RED：补自动编码流水恢复回归测试，先证明 Redis 丢 key 时会从旧起始号回退。`DONE`
3. M3：GREEN：实现按历史最大流水恢复 Redis 计数器的正式逻辑。`DONE`
4. M4：REGRESSION：运行目标后端测试并更新维护仓 E2E 脚本误报契约。`DONE`
5. M5：测试服重新构建发布并复跑三角色真实 E2E。`DONE`
6. M6：更新证据并提交本任务相关改动。`DONE`

## 预期验证

- `mvn -pl yudao-module-mes -Dtest=MesMdAutoCodeSerialNumberPartStrategyTest,MesProAutoScheduleServiceImplTest test`
- `node doc\\tasks\\20260618-post-release-role-e2e-gate\\scripts\\post-release-role-e2e-static.test.cjs`
- 测试服真实日志不再出现 `MesMdAutoCodeRecordServiceImpl.generateAutoCode(...:90)` 的 `编码生成失败`
- 维护仓三角色真实 E2E：`gaomin`、`zhaojie`、`wangsiyu` 全绿

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。自动编码缓存丢失时通过正式恢复逻辑续号，不吞掉重复编码异常，也不绕过编码校验。
- `是否从根因和长期维护角度解决`：是。直接修复 Redis 计数器与数据库历史流水脱节的问题，避免每次缓存丢失后真实排产再次回退撞码。
- `是否存在临时补丁或绕过`：否。不得用手工改 Redis key、手工删历史记录或测试服一次性补数掩盖问题。

## 当前状态

- 状态：COMPLETED。
- 已确认真实根因：测试服 `172.30.30.58` 上，`tenant_id=1` 的 `mes_md_auto_code_rule` 已存在 `PRO_TASK_CODE(rule_id=900070)` 及其两段规则 `900071/900072`，但 `intruoyi-redis` 中不存在 `mes:auto_code:900070` key；同时 `mes_md_auto_code_record` 仅保留 `PT-0006(serial_no=6)` 与 `PT-0007(serial_no=7)` 两条历史记录。
- 已确认触发链路：远端 `docker logs intruoyi-backend --since 45m` 显示真实 `apply` 失败堆栈固定落在 `MesMdAutoCodeRecordServiceImpl.generateAutoCode(...:90)`，也就是重复编码检查命中历史记录后抛出 `AUTO_CODE_GENERATE_FAILED`。
- 已完成本地修复：`MesMdAutoCodeSerialNumberPartStrategy` 仅在 Redis key 缺失时回查 `mes_md_auto_code_record` 的历史最大 `serial_no` 并恢复起始值，`MesMdAutoCodeRecordMapper` 新增按规则、循环窗口与 `inputChar` 查询最新流水记录的方法；维护仓 E2E 静态契约同步收敛 `dict-data/simple-list` 的已确认 benign abort 误报。
- 已完成测试服闭环：重新构建发布 `release-20260619-2048-role-e2e-gate-autocode-recovery-rerun` 后，三角色真实 E2E 已穿过自动编码环节；`gaomin` 与 `wangsiyu` 通过，`zhaojie` 的智能排产 smoke 不再出现 `编码生成失败`，而是推进到新的后续阻塞 `排产完成创建 eDHR 批次缺少前置条件：工序与批记录绑定`。
- 结论：本任务范围内的自动编码 Redis 流水恢复问题已闭环，后续阻塞已转入新任务 `20260619-post-release-role-e2e-gate-edhr-batch-trigger-gate` 处理。
