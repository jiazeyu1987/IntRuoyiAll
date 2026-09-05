# Verification Report

## Result

PASS

## Verification

- 生产提交详情读模型新增 `originalPayloadJson/eventDeviceId/eventDeviceCode/eventDeviceName`，从生产报工事件正式 payload 和 `mes_pro_process_pool_team_device` 读取设备事实。
- 服务层输出 `SubmissionDeviceDetail` 集合，支持 `selectedDevices`、物料明细内设备集合、设备参数读数和事件设备身份，不按工序名/PQC/前端状态推断。
- 前端活跃订单详情的“生产提交”表格新增“设备”列，多台设备用 `、` 汇总显示。

## Commands

- `node IntRuoyiFronted\tests\e2e\team-leader-active-order-production-device-detail-static.spec.cjs` -> PASS
- `node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-active-order-submission-overview-static.spec.cjs` -> PASS
- `node IntRuoyiFronted\tests\e2e\team-leader-active-order-detail-split-main-tabs-static.spec.cjs` -> PASS
- `pnpm --dir IntRuoyiFronted ts:check` -> PASS
- `mvn -f IntRuoyiBackend\pom.xml -pl yudao-module-mes -am -DskipTests compile` -> PASS
- `git diff --check -- <本任务相关文件>` -> PASS
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260905-active-order-production-submission-device-detail/backend-api-evidence.md` -> PASS
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260905-active-order-production-submission-device-detail/frontend-feature-evidence.md` -> PASS
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260905-active-order-production-submission-device-detail --mode preview` -> PASS
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260905-active-order-production-submission-device-detail --mode apply` -> PASS

## Blockers

无。当前未按本轮请求重启 `int_main` 后端；需要运行态立即生效时需再执行重启。收尾复跑时已将仍读取旧弹框结构的静态合同更新为读取独立详情组件 `ActiveOrderSubmissionDetailPanel.vue`。实现提交：`5c969a5ff`；临时 evidence 已按 cleanup apply 删除。
