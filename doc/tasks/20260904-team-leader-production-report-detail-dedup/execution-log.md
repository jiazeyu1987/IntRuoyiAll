BDD: 主表隐藏展开详情已有字段 -> Given 生产组长查看报工管理列表 When 某次提交包含多物料和多设备 Then 主表不显示完成数量、损耗数量、未分配数量、物料明细、选用设备、设备参数这些展开详情已有字段。
BDD: 展开详情保留完整归属 -> Given 某次提交含物料1与物料2且各自绑定设备 When 用户展开报工行 Then 每个物料块只展示属于该物料的设备，设备名称、设备编号和参数在同一设备行内展示，多个参数用中文分号分隔。

RED: node tests/e2e/team-leader-production-report-payload-columns-static.spec.cjs -> FAIL expected before implementation, because production default columns still contained completion/loss/unallocated/material/device/parameter summary keys and template guards allowed production main-row rendering.
GREEN: node IntRuoyiFronted/tests/e2e/team-leader-production-report-payload-columns-static.spec.cjs -> PASS
GREEN: node tests/e2e/team-leader-multi-material-device-dialogs-static.spec.cjs -> PASS
GREEN: python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260904-team-leader-production-report-detail-dedup/frontend-feature-evidence.md -> PASS
GREEN: pnpm exec vue-tsc --noEmit --pretty false -> PASS after resolving two current-page null limit type errors and four RegistrationCertificateConfig recipient array type errors
GREEN: pnpm build:local -> PASS
GREEN: node yudao-module-mes/src/test/js/mes-active-order-stage1-static.spec.cjs -> PASS
GREEN: node yudao-module-mes/src/test/js/mes-active-order-submission-overview-static.spec.cjs -> PASS
GREEN: mvn -pl yudao-module-mes -am "-DskipTests" test -> BUILD SUCCESS
GREEN: git diff --check -> PASS
GREEN: rg -n "报工列表黄框字段|submissionMaterialSummary隐藏|参数分号分隔" docs/experience-index.md docs/frontend-development.md -S -> PASS
