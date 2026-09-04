# Team Leader Production Report Detail Dedup

## Task Goal
生产组长报工列表主表不再重复展示展开详情已有的完成数量、损耗数量、未分配数量、物料明细、选用设备和设备参数；多物料、多设备、设备参数完整归属保留在展开详情中。

## Milestones
- [x] 记录多物料设备展开详情 BDD 与静态合同
- [x] 调整生产组长主表列展示规则
- [x] 验证静态合同、类型检查和构建
- [x] 收尾清理、提交并推送

## Expected Verification
- node IntRuoyiFronted/tests/e2e/team-leader-production-report-payload-columns-static.spec.cjs
- node tests/e2e/team-leader-multi-material-device-dialogs-static.spec.cjs
- python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260904-team-leader-production-report-detail-dedup/frontend-feature-evidence.md
- pnpm exec vue-tsc --noEmit --pretty false
- pnpm build:local
- node yudao-module-mes/src/test/js/mes-active-order-stage1-static.spec.cjs
- node yudao-module-mes/src/test/js/mes-active-order-submission-overview-static.spec.cjs
- mvn -pl yudao-module-mes -am "-DskipTests" test
- git diff --check

## Current Status
completed

## 设计约束检查
- 主表只保留提交记录识别、分配入口和操作列；重复事实放入展开详情。
- 展开详情按物料 -> 设备 -> 参数归属展示，不做跨物料设备汇总。
- 每台设备一行展示设备名称、设备编号和分号分隔参数。
- 编译错误已处理：TeamLeaderWorkbenchPage 上下限 null 类型收窄；RegistrationCertificateConfig 通知用户 ID 数组显式类型化。

