# 执行记录

## 用户授权
- 用户要求：提交推送主干代码，然后启动前后端。
- 范围：当前主干代码与本机运行态；未要求 E2E、远端发布或数据库迁移。

## 规则及技能
- 已读取项目任务、Git、运行端口、启动和编码规则。
- 使用 task-closeout-cleanup 和 project-experience-consolidation；经验优先核对现有 docs 文档。
- 既有工作区改动将独立作为基线提交，本任务记录另行提交。

## 执行进度
- 基线 HEAD：6a012519b；fetch origin int_main 成功，领先 12、落后 0。
- 既有改动：39 个源码、测试与文档文件；暂存区初始为空。文件路径与 SHA256 清单暂存在 Git 元数据目录，不属于提交内容。
- git diff --check：PASS；branch-runtime-port-guard.ps1：PASS，int_main 使用 8081/48081。
- 前后端端口无监听进程；Docker MySQL、Redis、MinIO、OnlyOffice 依赖存在且运行，Vite 依赖存在。
- Java 21.0.10、Maven 3.9.16、Node 24.12.0、pnpm 10.22.0 可用。
- task.md 结构验证、正式本机启动脚本 PowerShell parser：PASS。
- 前端 5 个定向行为/静态测试脚本：64 tests，64 PASS，0 FAIL；没有执行真实 E2E。
- 正在执行后端 8 个目标测试类及完整 server package、MES/Showroom 只读 MySQL mapper 回归。
- MES mapper 回归 6 tests PASS；Showroom mapper 回归 4 tests PASS，仅运行 SELECT。
- 正式启动脚本的 32 项 schema/seed 探针全部通过；展厅正式文件配置、媒体对象存在性只读检查通过。
- 探针辅助调用最初因 SELECT-only helper 不接收末尾分号、未选定数据库而失败，均为调用前置条件错误；随后直接加载正式脚本探针函数，在正式目标库执行，32/32 PASS。未执行迁移写入。
- 经验核对：docs/worktree-memory.md 已有“主干快照提交”“运行时融合后的版本与端口交叉核验”，docs/database-rules.md 已有启动授权不等于迁移授权，均覆盖本次工作；不新增重复长期经验。
- 后端命令：mvn.cmd -pl yudao-server -am '-Dtest=MesProFeedbackMaterialServiceTest,MesProFrontlineFeedbackMaterialSubmissionValidatorTest,MesProFrontlineFeedbackPayloadSplitterTest,MesProFrontlineFeedbackSubmitServiceTest,MesProcessPoolProductionReportCorrectionServiceTest,MesKingdeeProductionMaterialListQueryServiceImplTest,MesTeamLeaderActiveOrderProductionMaterialControllerTest,MesTeamLeaderActiveOrderProductionMaterialServiceTest' '-Dsurefire.failIfNoSpecifiedTests=false' package -> BUILD SUCCESS，31 模块成功，96 tests PASS，0 failures/errors/skipped，2026-10-09 13:59:22 +08:00 完成。
- Java 编译目标为 17，实际构建/测试运行 JDK 为 21.0.10；未把该结果声明为 JDK 17 运行验证。
- Git 暂存白名单精确等于冻结的 39 文件；staged diff --check 与 CRLF 归一后的内容等价检查 PASS。
- 正式启动解析目录为 E:/IntRuoyi，前端 IntRuoyiFronted、后端 IntRuoyiBackend，端口 8081/48081。
- 基线提交：29df5ede23e231e397f6fd2daccbcbc9d33cb177，文件清单见“基线文件清单”；提交前后端口守卫 PASS。
- git push origin int_main -> PASS，origin/int_main 从 30038aeca 前进至 29df5ede2；原有 12 个提交与本次基线提交全部推送，git status 显示无 ahead/behind。
- 本次 server Jar：508045569 bytes，2026-10-09 13:59:19 +08:00，SHA256=1C8F95FBAED9DECD89F9DC99F1CB057B633135005371F75BC1FFD7C20EBABE72。
- 正在执行正式 full 启动，传入本次 Jar 与 SHA256，并启用 BackendSchemaReadOnly。
- 正式 full 入口执行前再次检查端口时，发现并行任务已启动当前主干服务；本任务启动调用在预检处停止，未停止或重启共享服务。
- 前端 PID=20488，2026-10-09 13:37:24 +08:00 启动，命令属于 E:/IntRuoyi/IntRuoyiFronted 的 Vite env.local；8081 HTTP 200，包含 /@vite/client 或 /src/main.ts 正式入口。
- 后端 PID=31316，2026-10-09 13:38:15 +08:00 启动；运行 Jar 位于 output/runtime/int_main/backend-runtime-control-20261009-133724-561-a71d76a67350474a8ff92c56c8324e79.jar，运行 JDK 17.0.20。
- 运行 Jar SHA256=C5C20F38BE9B9647288860B1C7DAE0A1D58BF500F6BA2668909BCCF7A494E817；整体归档哈希与新打包 Jar 不同，851 个条目名称及每个解压内容逐字节全部相同，说明运行负载与本次已验证构建等价。
- 后端 /actuator/health -> HTTP 200 / UP；运行 Jar 修改时间早于 Java 进程启动时间；未覆盖运行包。
- git status 无业务 dirty，HEAD...origin/int_main 为 0/0；状态转为 ready_for_closeout。只清理本任务 Git 元数据目录下 7 个明确临时清单，保留既有服务、运行日志和其它任务。
- 验证记录提交：4fa2c100ef5f2ccda10914ace274807ea8adbdb8，仅 task.md、execution-log.md、verification-report.md；因本机 .git/info/exclude 排除任务目录，对本任务三个正式记录使用 git add -f。
- cleanup preview -> ready，keep=3、delete=7、blocked=0、warnings=0；确认删除范围为本任务 7 个明确 Git 元数据清单。
- cleanup apply -> applied；7 个临时文件存在性复验均为 False，3 份核心记录全部保留。当前是主工作区，无 worktree 融合、移除或其它任务清理。
- 收尾状态更新为 completed；最终收尾提交仅上述 3 份核心记录，提交 hash 由 git log -1 --format=%H -- doc/tasks/20261009-main-commit-push-start 回查并随最终回复报告；提交后推送 origin int_main，复验同步和服务健康。

## 基线文件清单
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/feedback/frontline/MesProFeedbackMaterialServiceImpl.java
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/feedback/frontline/MesProFrontlineFeedbackMaterialSubmissionValidator.java
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/feedback/frontline/MesProFrontlineFeedbackPayloadSplitter.java
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/MesProcessPoolProductionReportCorrectionService.java
- IntRuoyiBackend/yudao-module-mes/src/main/resources/mapper/profileworkbench/MesWorkbenchTodoMapper.xml
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/feedback/frontline/MesProFeedbackMaterialServiceTest.java
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/feedback/frontline/MesProFrontlineFeedbackMaterialSubmissionValidatorTest.java
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/feedback/frontline/MesProFrontlineFeedbackPayloadSplitterTest.java
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/feedback/frontline/MesProFrontlineFeedbackSubmitServiceTest.java
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/MesProcessPoolProductionReportCorrectionServiceTest.java
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/workorder/kingdee/MesKingdeeProductionMaterialListQueryServiceImplTest.java
- IntRuoyiBackend/yudao-module-showroom/src/main/resources/mapper/profileworkbench/ShowroomWorkbenchTodoMapper.xml
- IntRuoyiFronted/src/api/mes/pro/batchrecordreport/index.ts
- IntRuoyiFronted/src/api/mes/pro/processpool/teamLeader.ts
- IntRuoyiFronted/src/views/mes/pro/batchrecordformlist/index.vue
- IntRuoyiFronted/src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue
- IntRuoyiFronted/src/views/mes/pro/processpool/ActiveOrderSubmissionDetailPage.vue
- IntRuoyiFronted/src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue
- IntRuoyiFronted/tests/e2e/active-order-copy-detail-long-id-static.spec.cjs
- IntRuoyiFronted/tests/e2e/active-order-detail-erp-nonblocking-behavior.spec.cjs
- IntRuoyiFronted/tests/e2e/team-leader-active-order-production-material-list-tab-static.spec.cjs
- docs/e2e-rules.md
- IntRuoyiBackend/script/tests/test_mes_workbench_regexp_collation.py
- IntRuoyiBackend/script/tests/test_showroom_workbench_regexp_collation.py
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/MesTeamLeaderActiveOrderProductionMaterialController.java
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderProductionMaterialService.java
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/MesTeamLeaderActiveOrderProductionMaterialControllerTest.java
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderProductionMaterialServiceTest.java
- IntRuoyiFronted/src/api/mes/pro/batchrecordreport/versionGovernance.ts
- IntRuoyiFronted/src/views/mes/pro/batchrecordformlist/BatchRecordVersionReviewDialog.vue
- IntRuoyiFronted/tests/e2e/batch-record-version-review-dialog-behavior.spec.cjs
- IntRuoyiFronted/tests/e2e/frontline-production-qualified-output-loss-behavior.spec.cjs
- docs/system/user-management/acceptance-matrix.md
- docs/system/user-management/architecture.md
- docs/system/user-management/backend-api-design.md
- docs/system/user-management/config-security-deployment.md
- docs/system/user-management/data-model.md
- docs/system/user-management/frontend-design.md
- docs/system/user-management/implementation-plan.md
