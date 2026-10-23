# G05 改动清单

本轮只改三个授权生产文件、两个指派专属现有测试、新增一个前端专属测试与本任务记录。工作区已有大量并行改动，Git 相对 HEAD 的完整 diff 不是本任务增量。改前字节 SHA256 见 baseline-hashes.json，最终快照见 final-hashes.json。

| 文件（仓库相对路径） | 本轮必要差异 |
|---|---|
| IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccWorkflowSignoffAssignmentService.java | assignmentContext 取消本人部门豁免；assign 当前目录结果必须与义务部门相同且义务部门非空，在 readiness/关系准备/签名前拒绝 |
| IntRuoyiFronted/src/views/dcc/controlled-file/workflow/SignoffAssignmentPanel.vue | 删除全员 scope、加载端口及状态，只展示 props.users；本部门提示，保存校验当前正式候选 |
| IntRuoyiFronted/src/views/dcc/controlled-file/detail/DetailSignoffAssignment.vue | 删除全员会签目录连接和 loader；保存独立复核精确 taskId 和当前部门候选；整改人员目录继续独立使用 |
| IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccWorkflowSignoffAssignmentTest.java | 更新旧跨部门允许断言，增加转部门、目录注入、本人约束、三种流程直接保存拒绝、空部门；增强正向签名时间及义务不变、正式错误码和零写断言 |
| IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccWorkflowRelationStageIntegrationTest.java | 仅本类 assignment 专属 fixture 的候选部门6改5，与义务部门一致，保留真实 H2/事务/关系冻结/精确重放断言 |
| IntRuoyiFronted/tests/unit/dcc-signoff-department-assignee.test.cjs | 新增真实 Vue setup 与模板执行测试，验证全员不可加载/不可选/不可写、本人/成员可写、当前候选移除、详情越界及错任务拒绝 |

未修改共享 DccRelationStageSnapshotServiceTest、DccRelationStageH2Schema、Workflow、Controller、公共 VO/API、组织或权限实现。隔离编译只读取这些相邻依赖。

## V01 新增验证准备（2026-10-15）

原G05以上业务源码/test继续冻结且六文件当前hash与旧归档一致。V01单独授权：

- IntRuoyiBackend/yudao-module-dcc/src/test/resources/sql/create_tables.sql：仅七新列（六确认+category来源）、两处timestamp(6)，其他全文相同。
- IntRuoyiBackend/yudao-module-signature/src/test/resources/sql/create_tables.sql：仅system_electronic_signature.signed_at显式timestamp(6)。
- IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/category/DccFileCategoryAdminServiceImplTest.java：仅六个SaveReq+一个createCategory builder显式PUBLIC_MATRIX，共七行；所有原断言、调用、负例原样。

精确改前/当前字节SHA及LF SHA在v01-source-hashes.json；对本轮源快照的最小diff在v01-minimal.diff。新增自有schema合同源/运行入口/逐项结果/只读候选清单属于本任务资产；没有修改任何其他fixture、生产DDL、BaseDb、全局H2或SQL清理脚本。

## V02 正常Category fixture增量
Root明确授予v01清单14文件，仅18个正常Category builder各补一个显式PUBLIC_MATRIX调用。每文件原byte/LF SHA、current SHA、原断言AST摘要、调用数见v02-file-manifest.json，精确hunk见v02-minimal.diff。14文件不做其他改写，去增量完全还原。完整交还及验证边界见v02-review-ready.md；额外5处inline SQL仅只读清单，未写。

## A21 checkout测试schema增量
G06正式生产DDL/DO/Mapper合同出现后，仅DCC create_tables.sql中checkout新增一列checkin_payload_hash CHAR(64) NULL。保存V01后精确baseline/current byte及LF SHA，去单行增量原字节还原；a21-minimal.diff与a21-file-manifest.json归档。只新增/更新自有A21契约和V01脚本的当前列声明/逆向保护，不改任何生产源/G06test/其他表列。future三个custom draft字段未预造。

## A23/A26限定增量
Actual G03正式DDL落盘后只新增3个nullable custom草稿字段：VARCHAR32/LONGTEXT/VARCHAR128；routeSnapshot.route_version_no仅INTNOTNULL→INTNULL，无默认1。A23/a26独立源manifest/最小diff/真实RED-GREEN逐项JSON均在本目录；只改DCC共享test schema，不改业务Source/RouteDO/生产迁移/G03/G06tests，不增round。最终三schema/分类fixture+14正常categoryfixture共17文件manifest见schema-handoff.json，完整相对任务源基线union最小diff见schema-final-minimal.diff。
