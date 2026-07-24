# Execution Log

- 2026-06-19: Created backend task package `20260619-post-release-role-e2e-gate-autocode-counter-recovery`.
- BDD: 自动编码流水缓存丢失后必须按数据库最新流水恢复 -> Given `PRO_TASK_CODE` 的 Redis 流水 key 因重启或过期丢失 / When 自动排产 `apply` 再次生成任务编码 / Then 系统必须从 `mes_md_auto_code_record` 已存在的最大 `serial_no` 继续分配，而不是回退到起始号导致重复。
- BDD: 发布后三角色验收中的智能排产 apply 不再因编码回退失败 -> Given 测试服 `芋道源码/zhaojie` 真实 smoke 的 `preview` 已无阻塞 / When 调用 `/admin-api/mes/pro/auto-schedule/apply` / Then 不再因 `MesMdAutoCodeRecordServiceImpl.generateAutoCode` 的重复编码检查抛出 `编码生成失败`。
- Finding: 测试服 `mes_md_auto_code_rule` 已存在 `tenant_id=1 / id=900070 / code=PRO_TASK_CODE`，`mes_md_auto_code_part` 已存在固定前缀 `PT-` 和流水号两段；本次失败并非规则缺失。
- Finding: 测试服 `mes_md_auto_code_record` 中 `tenant_id=1 / rule_id=900070` 当前仅有 `PT-0006(serial_no=6)`、`PT-0007(serial_no=7)` 两条记录。
- Finding: 测试服 `intruoyi-redis` 中不存在 `mes:md:auto_code:900070` key，说明自动编码 Redis 流水缓存已丢失。
- Finding: 远端 `docker logs intruoyi-backend --since 45m` 中真实 `apply` 的失败堆栈固定落在 `MesMdAutoCodeRecordServiceImpl.generateAutoCode(MesMdAutoCodeRecordServiceImpl.java:90)`，即重复编码校验路径。
- GREEN: experience-preflight -> PASS，已再次核对 `D:\ProjectPackage\Int\IntRuoyi\docs\release-backup-restore.md`、`server-access.md`、`login-access.md` 的当前任务约束；后续高风险动作仍仅限测试服 `172.30.30.58` 的重新构建发布与真实 E2E。
- RED: `mvn -pl yudao-module-mes -Dtest=MesMdAutoCodeSerialNumberPartStrategyTest test` -> FAIL，新增恢复用例前编译缺少 `selectLatestSerialRecord(...)` 契约，说明 Redis 丢 key 后的正式恢复逻辑尚未实现。
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesMdAutoCodeSerialNumberPartStrategyTest,MesProAutoScheduleServiceImplTest" test` -> PASS，28 passed；新增 `testGenerate_shouldResumeFromLatestRecordedSerialWhenRedisKeyMissing`，验证 Redis 丢 key 时首个编码恢复到 `0008` 而不是回到 `0001`。
- GREEN: `node D:\ProjectPackage\Int\IntRuoyiMaintance\doc\tasks\20260618-post-release-role-e2e-gate\scripts\post-release-role-e2e-static.test.cjs` -> PASS，维护仓 E2E 契约已收敛 `GET /admin-api/system/dict-data/simple-list` 的 benign abort 误报。
- GREEN: runtime-console-build-deploy -> PASS，测试服重发版 `release-20260619-2048-role-e2e-gate-autocode-recovery-rerun` 的构建与部署均成功；后端已带上自动编码恢复修复进入真实环境。
- GREEN: real-three-role-rerun-scope-check -> PASS，维护仓证据 `D:\ProjectPackage\Int\IntRuoyiMaintance\doc\tasks\20260618-post-release-role-e2e-gate\evidence\post-release-role-e2e-1781869546155.json` 显示 `gaomin`、`wangsiyu` 通过，`zhaojie` 的智能排产 smoke 已穿过自动编码阶段并推进到新的 eDHR 前置条件阻塞，说明 `编码生成失败` 已从真实链路消失。
