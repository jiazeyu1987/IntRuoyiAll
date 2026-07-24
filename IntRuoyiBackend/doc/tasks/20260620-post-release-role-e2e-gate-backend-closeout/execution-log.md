# 执行日志：发布后三角色 E2E 门禁后端收口归档

- `BDD: 发布后角色账号与 smoke 运行契约 -> Given 测试服每次发布后都要用芋道源码租户真实账号做三角色验收 / When 发布链执行数据库迁移并写出运行时 .env / Then 角色账号、密码、smoke 账号密码新鲜度与 ERP 手动触发权限必须由正式 SQL 和发布契约保证，而不是依赖环境残留。`
- `BDD: 展厅运行态 schema 发布契约 -> Given 展厅代码已经依赖 showroom 产品附件表与大厅背景字段 / When 构建并部署发布包 / Then required SQL 必须包含对应 showroom schema，后端启动前 preflight 缺表或缺字段必须 fail fast。`
- `BDD: 智能排产依赖查询长范围 POST 契约 -> Given 生产排产页会加载大量工单依赖线 / When 查询当前甘特图依赖线 / Then 后端必须支持 POST request body，避免真实路径因 URL 过长命中 414。`
- `BDD: 排产工序快照 schema 契约 -> Given 智能排产 smoke 会把工单工序写入 mes_pro_schedule_order_process 快照表 / When 后端持久化 process_code 与 process_name / Then 发布迁移必须先补齐列并完成历史回填。`
- `BDD: A03388 同设备同工序唯一产能契约 -> Given 芋道源码租户智能排产 smoke 会读取 A03388 的设备工序产能 / When 同设备同工序存在重复高产能记录 / Then 发布迁移必须逻辑删除冲突高产能，只保留保守容量，避免自动排产预览 500。`
- `BDD: 最终测试服发布闭环 -> Given 后端 release SQL、发布契约与调度服务修复全部完成 / When 维护仓重新构建发布并运行三角色真实 E2E / Then 最终测试服 evidence 必须显示 gaomin、zhaojie、wangsiyu 全部通过，且智能排产 smoke 全链路 PASS。`

- `GREEN: carry-over-subtasks-check -> PASS，已核对 20260619-post-release-role-e2e-gate-route-900026-line-fix、20260619-post-release-role-e2e-gate-autocode-counter-recovery、20260619-post-release-role-e2e-gate-edhr-batch-trigger-gate、20260619-post-release-role-e2e-gate-feedback-approver-identity 四个后端子任务当前均为 COMPLETED；本任务仅承接剩余未挂任务的发布契约、SQL 与控制器改动。`
- `RED: python -X utf8 -m pytest script\tests\test_post_release_role_e2e_gate_sql.py -q` -> FAIL，新增发布后角色门禁契约前，仓内缺少正式发布 SQL 来固化角色账号、密码新鲜度、ERP 手动触发权限、A03388 产能冲突、路线 900026 产线绑定等测试服真实门禁修复。`
- `RED: mvn -pl yudao-module-mes -Dtest=MesProAutoScheduleControllerContractTest test` -> FAIL，控制器尚未提供 POST /mes/pro/auto-schedule/dependencies request body 契约，真实长工单范围依赖线查询会在发布后路径命中 414。`
- `RED: git -C D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro show HEAD:sql/mysql/20260619_mes_schedule_order_process_snapshot_identity_p8.sql` -> FAIL，说明当时 HEAD 尚未带上排产工序快照缺列修复迁移，测试服真实 smoke 会在 create-from-work-order 阶段暴露 Unknown column 阻塞。`
- `GREEN: python -X utf8 -m pytest script\tests\test_post_release_role_e2e_gate_sql.py script\tests\test_scheduler_smoke_release_contract.py script\tests\test_showroom_release_sql_contract.py script\tests\test_publish_int_ruoyi_to_test_tooling.py script\tests\test_mes_scheduling_closed_loop_sql.py -q` -> PASS，发布后角色 SQL、scheduler smoke 契约、showroom required SQL、发布脚本打包契约和排产快照 schema 契约全部通过。`
- `GREEN: python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql` -> PASS，MySQL release migration gate 通过；当前收口范围涉及的 `20260618_post_release_role_e2e_gate*` 与 `20260619_*` 正式迁移均能进入发布链。`
- `GREEN: mvn -pl yudao-module-mes -Dtest=MesProAutoScheduleControllerContractTest test` -> PASS，后端控制器已支持 POST body 查询依赖线。`
- `GREEN: mvn -pl yudao-module-mes "-Dtest=MesMdAutoCodeSerialNumberPartStrategyTest,MesProFeedbackImportRecordServiceImplTest,MesProAutoScheduleControllerContractTest,MesProAutoScheduleServiceImplTest,MesProAutoScheduleAlgorithmContractTest,MesProAutoScheduleContractTest" test` -> PASS，自动编码恢复、审批人身份解析、依赖查询 POST 契约、eDHR 触发门禁与智能排产相关回归测试全部通过。`
- `GREEN: maintenance-final-evidence -> PASS，维护仓最终证据 D:\ProjectPackage\Int\IntRuoyiMaintance\doc\tasks\20260618-post-release-role-e2e-gate\evidence\runtime-console-build-deploy-1781882335880.json 与 D:\ProjectPackage\Int\IntRuoyiMaintance\doc\tasks\20260618-post-release-role-e2e-gate\evidence\post-release-role-e2e-1781882555897.json 均为 PASS，证明本任务收口范围内的后端改动已成功随最终发布进入测试服并完成三角色真实验收闭环。`

RED: `python -X utf8 -m pytest script\tests\test_post_release_role_e2e_gate_sql.py -q` -> FAIL, 新增发布后角色门禁契约前缺少正式发布 SQL，无法把真实测试服门禁修复沉淀为发布链正式迁移。
RED: `mvn -pl yudao-module-mes -Dtest=MesProAutoScheduleControllerContractTest test` -> FAIL, 控制器尚未提供 POST /mes/pro/auto-schedule/dependencies request body 契约，真实长工单范围依赖线查询会在发布后路径命中 414。
GREEN: `python -X utf8 -m pytest script\tests\test_post_release_role_e2e_gate_sql.py script\tests\test_scheduler_smoke_release_contract.py script\tests\test_showroom_release_sql_contract.py script\tests\test_publish_int_ruoyi_to_test_tooling.py script\tests\test_mes_scheduling_closed_loop_sql.py -q` -> PASS
GREEN: `python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql` -> PASS
GREEN: `mvn -pl yudao-module-mes -Dtest=MesProAutoScheduleControllerContractTest test` -> PASS
GREEN: `mvn -pl yudao-module-mes "-Dtest=MesMdAutoCodeSerialNumberPartStrategyTest,MesProFeedbackImportRecordServiceImplTest,MesProAutoScheduleControllerContractTest,MesProAutoScheduleServiceImplTest,MesProAutoScheduleAlgorithmContractTest,MesProAutoScheduleContractTest" test` -> PASS
