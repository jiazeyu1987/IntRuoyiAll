# Execution Log

- BDD: 生产提交显示设备 -> Given 活跃订单详情存在一线生产提交且提交记录有正式设备信息 When 用户打开详情并进入生产提交主 tab Then 每条生产提交显示对应设备信息，且设备信息来自正式读模型字段，不从 PQC 或前端状态推断。
- RED: node IntRuoyiFronted\tests\e2e\team-leader-active-order-production-device-detail-static.spec.cjs -> FAIL，原因：活跃订单详情未读取 `pool_event.raw_payload` 和正式设备主数据，生产提交详情缺少设备集合展示合同。
- GREEN: node IntRuoyiFronted\tests\e2e\team-leader-active-order-production-device-detail-static.spec.cjs -> PASS。
- GREEN: node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-active-order-submission-overview-static.spec.cjs -> PASS。
- GREEN: node IntRuoyiFronted\tests\e2e\team-leader-active-order-detail-split-main-tabs-static.spec.cjs -> PASS。
- GREEN: pnpm --dir IntRuoyiFronted ts:check -> PASS。
- GREEN: mvn -f IntRuoyiBackend\pom.xml -pl yudao-module-mes -am -DskipTests compile -> PASS。
- GREEN: git diff --check -- <本任务相关文件> -> PASS。
- FIX: 收尾复跑发现旧静态合同仍读取 `TeamLeaderWorkbenchPage.vue` 的弹框结构；已将生产提交设备列、PQC 主 tab 和领料单合同锚点更新到 `ActiveOrderSubmissionDetailPanel.vue` 独立详情组件。
- GREEN: node IntRuoyiFronted\tests\e2e\team-leader-active-order-production-device-detail-static.spec.cjs -> PASS，收尾复跑通过。
- GREEN: node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-active-order-submission-overview-static.spec.cjs -> PASS，收尾复跑通过。
- GREEN: node IntRuoyiFronted\tests\e2e\team-leader-active-order-detail-split-main-tabs-static.spec.cjs -> PASS，收尾复跑通过。
- GREEN: node IntRuoyiFronted\tests\e2e\production-leader-active-order-process-submission-detail-static.spec.cjs -> PASS，仓库根路径复跑通过。
- GREEN: python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260905-active-order-production-submission-device-detail/backend-api-evidence.md -> PASS。
- GREEN: python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260905-active-order-production-submission-device-detail/frontend-feature-evidence.md -> PASS。
- GREEN: python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260905-active-order-production-submission-device-detail --mode preview -> PASS，keep task/execution/verification，delete backend/frontend evidence，blocked/warnings 均无。
- COMMIT: `5c969a5ff` -> `fix: show active order submission material details`，包含生产提交设备详情读模型、前端展示和静态合同修正。
- GREEN: python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260905-active-order-production-submission-device-detail --mode apply -> PASS，删除 backend/frontend evidence，保留 task/execution/verification。
