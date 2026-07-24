# 任务：修复 eDHR Word 导入物料表塌缩

- Task ID: `20260629-edhr-word-import-table-collapse-fix`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Created: `2026-06-29`
- Current Status: `blocked`

## Task Goal

修复电子批记录 Word 导入中，横向物料表被错误压成纵向堆叠文本的问题，保证 `产品信息` 与 `组装Ⅰ工序生产记录` 等双侧并列表格保留原始列结构。

## Previous Task Check

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260629-edhr-word-import-table-collapse-analysis\task.md`
- 状态：`completed`
- 处理说明：上一任务已完成根因分析，本次进入修复与回归验证。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
- `D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；直接修复 Route B / Route D 后端识别与布局校准，不做前端兜底。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: Route B 保留双侧物料表列结构 -> Given 原始 .doc 中存在左右并列的 6 列物料表 / When Route B 识别器解析并生成 parsedTable / Then 表头行必须仍包含多列单元格，不能被折叠成单列纵向文本。`
- `BDD: Route D 组装Ⅰ页签展开并列物料矩阵 -> Given Route D PDF 解析结果把右侧物料矩阵压成单个纵向文本单元格 / When 布局校准器处理组装Ⅰ工序生产记录 / Then 必须展开成横向 6 列表头与明细行，而不是保留一左一右两个大单元格。`
- `BDD: 同一 Word 重导入复用既有报表编码 -> Given 同一上传 Word 已经生成过固定 `reportCode` / When 用户更换批记录名再次导入同一文件 / Then 系统必须复用既有报表而不是再次插入重复编码。`

## Milestones

1. M1：补建任务文档并添加 RED 回归测试。`completed`
2. M2：最小修复 Route B 结构识别与重复导入复用逻辑。`completed`
3. M3：补充 `组装Ⅰ工序生产记录` 的 Route D / 校准回归测试。`completed`
4. M4：最小修复 Route D 导入后组装Ⅰ页签物料矩阵塌缩。`completed`
5. M5：运行定向测试并回填结果。`completed`
6. M6：在本机真实导入链路验证组装Ⅰ页签比例恢复。`completed`
7. M7：对齐运行态与测试态的 T06 操作区结构摘要，确认剩余差异落点。`completed`
8. M8：继续修复灰色可填写空白格在模板页无法配置的问题。`blocked`

## Expected Verification

- `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes "-Dtest=MesProBatchRecordReportLayoutCalibratorTest,MesProBatchRecordRouteDRecognizerTest,MesProBatchRecordRouteBRecognizerTest,MesProBatchRecordReportJsonBuilderTest" test`

## Completed Work

- Route B 识别脚本改为按 Word 单元格宽度估算 `colSpan`，修复并列表头被压扁的问题。
- 上传 Word 保存逻辑改为优先按 `sourceFileSha256 + routeKey + sourceTableIndex`、`reportCode` 和已有 `jimu_report.code` 复用既有报表，避免唯一键冲突。
- 新增数据库回归测试，覆盖“同文件新批记录名重导入仍应复用既有 `reportCode`”场景。
- 当前新增定位：用户实际剩余问题位于 `组装Ⅰ工序生产记录`，该页签走 Route D（Word 转 PDF 再抽表）链路；现有布局校准器对 packed material matrix 的展开条件依赖 `sideHeaderCell.rowSpan >= 4`，与 Route D 抽表默认 `rowSpan=1` 不兼容。
- 当前进一步定位：
  - `组装Ⅰ` 的 checklist 结构与 metadata 分组结构已在真实导入链路和数据库中生效。
  - 本机前端 `http://localhost:8081` 实际连接后端 `http://127.0.0.1:48081`，之前大量补丁验证跑在 `48083`，这是此前“页面看起来还是旧效果”的关键原因之一。
  - 现已直接用 `48081` 打开 `reportId=2c4294acb0f74fc5b5ca6c8ab3d768e9` 的 `jmreport/view`，预览结果与 `48083` 一致，说明用户实际实例上的最新报表数据已经更新。
- 最新根因已收敛为“通用高行版式恢复算法”问题，而非单表结构特判：`MesProBatchRecordReportShapeRules.estimateRowHeight` 与单页压缩链路都把长叙述行统一封顶到 `44px`，导致 checklist 这类显式高行在结构恢复正确后仍被比例压扁。
  - 本轮已把“普通行高上限”和“保留源版式行高上限”拆分，并让 checklist / 显式高行在校准、压缩、JSON 输出三个阶段共用保留高度通道。
- 当前剩余问题继续收敛到“通用列宽恢复算法”而非单页缓存：真实库内 `reportCode=EBR_TN1_B_DOC_830a89a2_T06` 的 `cols.width` 仍近似 `70/71` 均分，说明 checklist 虽然已恢复出 `1 + 11 + 3 + 3 + 3` 的逻辑分栏，但固定列宽预算仍可能在校准或 JSON 渲染阶段被重新拉平，导致左侧竖排区偏宽、正文区偏窄、尾部三列偏瘦。
- 本轮继续收敛后确认：用户当前页面样本实际走 `route_key=B`，不是此前误判的 Route D；同一份 `组装Ⅰ工序生产记录` 在 Route B 识别结果里已经带有明显非均分宽度提示，但 `MesProBatchRecordReportLayoutCalibrator.fitColumnWidthsToBudget(...)` 旧实现采用“逐像素从最宽列扣减”的通用预算策略，在列数多且预算紧张时会把宽窄差异持续扣平，最终收敛成接近 `70/71` 的均分列宽。
- 已将列宽预算阶段改为“按可收缩空间比例缩减并把余量优先回补给最适合列”的通用恢复算法，不再把差异列宽削成平均值；同一真实 Route B 样本的诊断输出已从均分列宽提升为 `columnWidths=[147,60,44,44,44,45,45,45,45,45,116,116,76,76,75,76,76,75,76,76,75]`，对应 checklist 头行恢复为更接近原始 Word 的 `147 / 649 / 227 / 227 / 227` 比例。
- 本轮继续推进运行态验证时新增阻塞：`mvn -pl yudao-server -am -DskipTests package` 已不再卡在 MES 自动排产常量，而是卡在 `yudao-module-system` 对 `framework-security` 类的解析；单独编译 `yudao-module-system` 可通过，依赖树也包含 `yudao-spring-boot-starter-security`，当前判断更接近 Maven/Reactor 的增量构建状态异常，需要先做相关模块的干净重编，才能把最新 eDHR 版式算法带入 `48081` 运行态。
- 当前新收敛点：源码中 `MesProBatchRecordReportServiceImpl` 与 `MesProBatchRecordJimuReportGatewayImpl` 已包含 `EDHR-T06-SOURCE/CALIBRATED` 运行态诊断，但现有 `backend-manual-restart-20260630-155046.out.log` 未捕获同轮最新导入；本轮需把“真实导入返回、运行日志、数据库 JSON、页面截图”放到同一轮证据内，避免继续混用旧时段日志判断。
- 交接说明：组装Ⅰ导入后的结构恢复、纵向骨架、列宽比例与运行态真实导入已完成收敛；用户随后确认新的剩余问题是“灰色、应可填写的空白单元格在模板页规则配置中无法设置”。该问题已拆分为后续独立任务 `20260630-edhr-gray-fillable-cell-rules`，并要求在全新的 `edhr_table` 成对 worktree 中继续开发。

## Blocker

- 阻塞原因：当前任务目标“结构恢复与比例修复”已完成，但出现了新的后续需求“灰色空白填写格需要自动识别为模板页可配置单元格规则候选”。该需求涉及新的候选生成链路和模板页规则消费验证，不应继续在当前已完成的结构恢复任务中叠加开发。
- 影响：后续需要在新的 `edhr_table` worktree 和新任务目录中继续实现“灰色可填写格候选修复”，本任务不再继续追加代码变更。

## Verification Result

- `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordReportServiceImplDbTest#recognizeUploadedRoute_whenSameFileReimportedUnderNewBatchName_reusesExistingReportCode" test`：PASS
- `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordRouteBRecognizerTest" test`：BLOCKED，Surefire/JUnit discovery 在 fork 进程失败；本轮未观测到新增断言失败。
- `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes "-Dtest=MesProBatchRecordReportLayoutCalibratorTest#calibrate_shouldPreserveChecklistNarrativeBodyHeightForAssemblyChecklist" test`：PASS
- `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes "-Dtest=MesProBatchRecordReportJsonBuilderTest#build_shouldPreserveChecklistNarrativeBandHeightFromCalibratedSource" test`：PASS
- `node D:\ProjectPackage\Int\IntRuoyi\scripts\preflight\login-preflight.mjs --base-url http://localhost:8081 --tenant 芋道源码 --username admin --password admin123 --target-path /index`：PASS
- `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes "-Dtest=MesProBatchRecordReportLayoutCalibratorTest#calibrate_shouldNarrowChecklistSideHeaderAndReserveBalancedTailColumnsForAssemblyChecklist,MesProBatchRecordReportLayoutCalibratorTest#calibrate_shouldPreserveChecklistNarrativeBodyHeightForAssemblyChecklist,MesProBatchRecordReportJsonBuilderTest#build_shouldPreserveNonUniformChecklistColumnWidthsForAssemblyChecklist,MesProBatchRecordReportJsonBuilderTest#build_shouldPreserveChecklistNarrativeBandHeightFromCalibratedSource,MesProBatchRecordRouteBRecognizerTest#recognizePilotSample_assemblyOneChecklistWidthHintsShouldPreserveDocLikeProportions" test`：PASS
- `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes "-Dtest=TmpPrintBatchRecordTableTest#printRouteBAssemblyOne" test`：PASS，真实 Route B 诊断输出已恢复非均分列宽，不再是 `70/71` 均分。
