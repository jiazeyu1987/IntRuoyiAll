# 批记录表单导入重建工艺路线需求文档

## Task Goal

基于已确认的 PRD、开发计划和测试计划，完成批记录表单 Word 导入时“勾选工艺流程后重建工艺路线”的生产代码实现与验证。

本阶段包含前端导入边界、后端候选版本治理、升版候选旧配置保留、发布投影和对应自动化验证；不操作服务器、不创建 Git 提交。

## Milestones

1. completed：P1 明确导入入口和用户确认边界；前端静态合同、类型检查、后端绑定候选 DB 测试及独立验证均通过。
2. completed：P2 固化后端路线目标和候选版本治理；8 个数据库场景和 5 个治理合同通过。
3. completed：P3 升版候选保留正式批记录表单绑定、formBindings、工序开始配置和附件负责人配置。
4. completed：P4 发布投影、旧绑定关系保留、新工序正式权限建立和运行态回归均已通过。

## Expected Verification

- 按 test-plan.md 运行前端静态合同、pnpm ts:check 和后端目标 Maven 测试，并记录 RED/GREEN/REGRESSION。
- 具备本地 int_main 前后端、测试租户、账号和任务自有数据时，通过 Playwright 执行真实页面路径；缺少前置时记录精确 blocker，不以 mock、API-only 或直接 SQL 替代。
- python C:\Users\BJB110\.codex\skills\spec-driven-delivery\scripts\validate_artifacts.py --cwd E:\IntRuoyi --task-id task-6586818a22-20260814T121328
- git diff --check -- 本任务文件。
- UTF-8 回读本任务文档。

## Current Status

completed：P1-P4 实现和自动化验证均已完成；后端最终回归 12/12、发布投影类 9/9、前端静态合同与类型检查通过。任务清理 preview/apply 均通过，没有可删除的任务临时产物。

2026-08-14 恢复：重启后确认当前阶段为 P1，相关前端文件已有未提交改动；本任务将基于现状继续，不回滚或覆盖并行改动。

真实浏览器写入 E2E 未执行：当前缺少已确认的测试租户、账号和任务自有 Word fixture；未以 mock、API-only 或直接 SQL 替代。

## Evidence Reviewed

- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecordreport/MesProBatchRecordReportServiceImpl.java
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecordreport/MesProBatchRecordRouteGenerationServiceImpl.java
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/route/MesProRouteCandidateConfigServiceImpl.java
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/route/MesProRouteVersionPublishProjectionServiceImpl.java
- IntRuoyiFronted/src/views/mes/pro/batchrecordformlist/index.vue
- IntRuoyiFronted/src/api/mes/pro/batchrecordreport/index.ts
- docs/backend-development.md 中 “Word 工艺路线导入必须锁定唯一未结束候选” 和 DCC 项目绑定相关门禁。
- 项目 AGENTS.md 中工艺路线三类配置术语契约。

## Applicable Experience Gates

- Word 工艺路线导入必须锁定唯一未结束候选：预检返回候选 ID、版本号和状态；写入前最终校验候选 ID、状态和来源 ACTIVE 版本；PENDING_APPROVAL/READY_TO_PUBLISH 必须阻断。
- DCC 项目绑定：导入必须携带唯一 dccProjectCodeId；已有路线优先按正式 DCC 绑定识别，禁止按名称或任意产品路线猜测。
- 三类配置术语契约：批记录表单、formBindings 表单槽位、工序开始是三条独立链路；不得互相 fallback。
- 无 fallback：缺少正式来源、映射失败、版本漂移或配置快照缺失时必须 fail fast。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。文档明确缺 DCC、缺 ACTIVE、候选锁定、映射失败、绑定来源缺失均 fail fast。
- 是否从根因和长期维护角度解决：是。方案要求在候选生成阶段完整迁移旧配置，而不是发布后或运行态临时补齐。
- 是否存在临时补丁或绕过：否。计划禁止直接覆盖 ACTIVE、直接 SQL、mock 成功和 API-only 替代真实 UI 验证。

## Cleanup Keep

- doc/tasks/task-6586818a22-20260814T121328/prd.md
- doc/tasks/task-6586818a22-20260814T121328/development-plan.md
- doc/tasks/task-6586818a22-20260814T121328/test-plan.md
- doc/tasks/task-6586818a22-20260814T121328/test-report.md
- doc/tasks/task-6586818a22-20260814T121328/task-state.json

## Experience Consolidation

- 已将“旧工序正式批记录绑定的权限范围和两类冻结 hash 必须保留；新增工序发布后再建立正式权限范围”的通用门禁合并到 `docs/backend-development.md#Word-升版候选必须保留正式批记录绑定身份`。
- 已更新 `docs/experience-index.md` 关键词索引；未新建长期经验文档。
