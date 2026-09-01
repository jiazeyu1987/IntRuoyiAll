# Execution Log

## User Intent

- 用户要求在独立 worktree 中优化表单中心识别方案，使指定生产记录中的清洗工序识别结果与三张原始截图一致。
- 用户确认采用通用识别方案；专项文件用于分析和回归，不允许生产代码按文件名或工序名硬编码。
- 用户查看最终真实页面截图并确认结果正确，随后明确要求融合进 `int_main`。

## BDD

BDD: 整份生产记录中的清洗工序独立识别 -> Given 一个 DOCX 包含多个顶层表格且粗洗与清洗连续存在于同一张 Word 表格中，When 表单中心按模板名称导入清洗工序，Then 系统应选择清洗标题对应的逻辑表格区间，不得固定使用第一张产品信息表。

BDD: 清洗工序视觉结构保持 -> Given 清洗工序包含 45 列基础网格、横向合并、纵向合并、重复物料记录块和斜线无效格，When 系统生成表单中心视觉结构，Then 标题、生产前检查、设备信息、8 个物料块、生产自检、批量汇总和生产后清场应按原 Word 结构保留。

BDD: 重复填写格独立生成 -> Given 每个物料记录块重复出现参考值与实际值、数量、操作人和复核人空白格，When 系统生成填写规则，Then 每个可填写单元格应按位置独立保留，不得因标签文字重复而合并。

BDD: 源文档异常不被静默修正 -> Given 螺纹块记录块的源表头存在“清洗时间、清洗次数”错误，When 系统识别该区域，Then 输出应忠实保留源文字并允许上层提示异常，不得按工序名称硬编码改写。

BDD: 斜线禁填格在正式前端可见 -> Given 识别布局单元格带 `edhrDiagonalSlash=true`，When 表单中心使用可填写视图或只读视图渲染模板，Then 两个共享表格组件都应显示从左上到右下的斜线且该格不可填写。

## Setup Evidence

- 已读取 `docs/worktree-restrictions.md`、`docs/task-closeout-rules.md`、`docs/backend-development.md`、`docs/powershell-memory.md`、`docs/powershell-encoding.md`。
- 已读取 `bug-regression-fix-loop` 技能及其 bug evidence contract。
- 主工作区 `int_main` 存在大量与本任务无关的并行改动且领先远端 3 个提交；本任务不修改、暂存、提交或清理这些内容。
- worktree：`D:\IntRuoyiWorktree\form-center-cleaning-recognition`。
- 分支：`codex/20260831-form-center-cleaning-recognition`，起点 `58479242435efa1f7eafd6e0a17e36bd9c811e5f`。
- worktree 首次检出超过工具等待窗口，但后台 Git 检出已自行完成；复核 `git status --short --branch` 为干净状态、删除差异计数为 0。
- `docs/experience-index.md` 存在，已命中并读取批记录 Word 表格解析、Form Center 源布局持久化、源 Word profile 和 Jimu 填写组件类型四项门禁；适用摘要已写入 `task.md`。

## RED / GREEN

- RED: `mvn.cmd -pl yudao-module-bpm "-Dtest=DefaultWordFormTemplateRecognizerTest#recognizeMultiFormProductionRecordSelectsCleaningSegmentAndPreservesVisualGrid" test` -> FAIL，当前识别字段仍包含“粗洗工序生产记录”，证明整份文档未按逻辑表单隔离，后续断言还将锁定 45 列、44 行与 96 个斜线格。
- RED 背景说明：首次带 `-am` 运行整个测试类时，新增用例如期失败；既有 `recognizeProcessInspectionDocx...` 同时因历史 fixture 路径缺失失败。该背景失败不作为本任务 RED，已用方法级命令隔离确认。
- RED: `node tests/e2e/form-template-word-diagonal-slash-static.spec.mjs` -> FAIL，前端两个共享渲染器未消费斜线方向和源单元格样式。
- RED: `mvn.cmd -o -pl yudao-module-bpm "-Dtest=FormCenterRuntimeImportRecognitionFlowContractTest" test` -> FAIL，最终复核发现字段类型表头探测仍可能读取已选逻辑表单之外的物理表格行。
- GREEN: `mvn.cmd -o -pl yudao-module-bpm "-Dtest=FormCenterRuntimeImportRecognitionFlowContractTest" test` -> PASS，表头探测已收窄到候选行区间，4 项合同测试通过。
- GREEN: `mvn.cmd -o -pl yudao-module-bpm -am "-Dtest=DefaultWordFormTemplateRecognizerTest,WordTableVisualSchemaBuilderTypeRecognitionTest,FormCenterRuntimeImportRecognitionFlowContractTest,FormCenterRuntimeServiceImplImportTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，10 项后端定向与相邻回归全部通过。
- GREEN: 将真实 DOCX 迁入 BPM 模块 `src/test/resources/formcenter` 后重跑同一命令 -> PASS，10 项测试、0 failures、0 errors；夹具 SHA-256 仍为 `77c0597e0a1b41c6d6415c4170a2217cc4a30255fe89dbe76d6d5271e2853514`。
- GREEN: `node tests/e2e/form-template-word-diagonal-slash-static.spec.mjs` -> PASS。
- GREEN: `pnpm.cmd exec eslint src/views/mes/pro/batchrecord-shared/batchRecordTemplateRules.ts src/views/mes/pro/edhr/components/EdhrExecutionTemplateEditableForm.vue src/views/mes/pro/edhr/components/EdhrExecutionReadonlyForm.vue tests/e2e/form-template-word-diagonal-slash-static.spec.mjs` -> PASS。
- GREEN: `pnpm.cmd ts:check` -> PASS。
- GREEN: `mvn.cmd -o -pl yudao-server -am "-DskipTests" package` -> PASS。

## Verification Evidence

- 真实 DOCX 识别结果：45 列、44 行、8 个物料块、96 个 `TR2BL` 斜线格；保留标题、设备信息、生产自检、批量汇总和生产后清场。
- 标题与设备编号的 Word 段落换行已保留；标题中的关键/非关键选择框保持静态。
- 多候选歧义明确失败，未保留静默选择第一张表的路径。
- 生产代码硬编码扫描未命中“清洗”“粗洗”“压力泵”或 `IDPR`。
- `git diff --check` 无空白错误，仅有 Windows CRLF 转换提示。
- bug regression evidence validator、frontend feature evidence validator 及两个 validator self-test 均通过。
- 使用 worktree 前端 `8312` 与后端 `48312` 完成真实页面导入和预览；页面结构与用户截图对应，Form Center 按既有设计在填写格上叠加字段类型标记。
- 唯一页面控制台错误为外部头像资源连接失败，与表单识别和渲染无关。
- 任务创建的 3 个测试模板均通过正式页面作废，状态显示“已作废”，未直接修改数据库。

## Blockers

- 当前无阻塞。

## Milestone Updates

- M1 completed：真实 DOCX 已复制到 worktree，源/目标长度均为 `207830`，SHA-256 均为 `77c0597e0a1b41c6d6415c4170a2217cc4a30255fe89dbe76d6d5271e2853514`；方法级 RED 稳定复现。
- M2 completed：按跨全表宽度的“工序生产记录”标题行切分逻辑候选，并按正式导入模板名称唯一选择；歧义明确失败。
- M3 completed：源网格、横纵合并、列宽、行高、字体、对齐、边框、段落换行和斜线方向已持久化；位置型空白格生成独立填写规则。
- M4 completed：后端 10 项回归、前端静态合同、ESLint、TypeScript、完整打包和真实页面验收通过。
- M5 completed：长期经验已合并到既有 Form Center Word 导入门禁；cleanup preview 无阻塞，正式 apply 清除任务临时证据和 Playwright 产物。`node_modules` 因 Windows 260 字符路径使 Python `shutil.rmtree` 连续失败，先在同一已核验 worktree 内缩短目录路径后再次由正式 cleanup 脚本删除成功；随后 `mvn clean` 清除后端构建产物。worktree 保留，未执行提交、合并或推送。
- M6 in progress：当前 `int_main` 为 `735f69782756fdc62ea1a1a69202e7d8a94e6d60`，主工作区存在其它任务的未提交改动；与本任务增量重叠的 tracked 路径仅为 `docs/backend-development.md`、`docs/experience-index.md`。主工作区原始 DOCX 正被其它程序占用，融合不移动、不覆盖该文件；同一 SHA-256 的回归样本改存 BPM 模块正式 `src/test/resources/formcenter`，测试从 classpath 读取。
- 融合前门禁：`scripts/preflight/branch-runtime-port-guard.ps1` -> PASS，分支 `int_main` profile、slot 57、端口 `8312/48312` 合规；`node tests/e2e/form-template-word-diagonal-slash-static.spec.mjs` -> PASS。
- M6 completed：任务实现提交为 `59316b6a1`；任务分支两次吸收融合窗口内最新 `int_main` 后形成 `1de1fd4d0`，主工作区执行 `git merge --ff-only codex/20260831-form-center-cleaning-recognition` 成功。主工作区并行修改的 `docs/backend-development.md`、`docs/experience-index.md` 使用路径级 stash `c17a74572076f1ddbf7545d8165ea266d3c6a42e` 保护，stash 审计仅含这两个文件，融合后自动合并恢复，恢复前后 numstat 均为 `32/8` 与 `18/10`，无冲突标记，随后仅删除该任务 stash。
- 融合后验证：BPM 定向与相邻回归 10 tests、0 failures、0 errors；前端斜线静态合同 PASS；目标文件 ESLint PASS；`pnpm.cmd ts:check` PASS；`mvn.cmd -o -pl yudao-server -am "-DskipTests" package` BUILD SUCCESS；`scripts/preflight/branch-runtime-port-guard.ps1` PASS。
- 并发保护：融合期间 `int_main` 继续前进至 `0f0f0cedf8b2a4f7e085545cf30c90af0916b9d9`，新增提交未触及本任务代码；本任务提交仍为当前主线祖先。主工作区其它未提交源码、测试、文档、临时文件和原始 DOCX 均未暂存、提交、删除或覆盖。
- Worktree cleanup：确认任务分支是 `int_main` 祖先、端口 `8312/48312` 无监听、无目标进程、测试夹具与任务源副本 SHA-256 一致后，cleanup apply 仅删除 worktree 内任务源副本；`git worktree remove D:\IntRuoyiWorktree\form-center-cleaning-recognition` 成功，物理路径不存在且 Git worktree 列表不再登记该路径。
- Slot cleanup：在与 `reserve-worktree-slot.ps1` 相同的跨进程 mutex 内精确更新 `D:\IntRuoyiWorktree\.ports\worktree-ports.json` 的 `form-center-cleaning-recognition` 唯一条目；确认目录已删除、任务状态为 `completed`、`8312/48312` 无监听后，设置 `active=false`，写入 `deletedAt` 与 `cleanupTask=20260831-form-center-cleaning-table-recognition`。其它登记项未修改。
- 2026-09-01 融合复核：用户再次要求“融合进 intmain”后，复核当前 `int_main` HEAD 为 `a1c5604b7f2135550e26289d882077163e0c9afa`，历史中已包含清洗识别实现提交 `59316b6a1` 与收尾提交 `5b875f56c`。本次未执行新的代码合并，避免把旧 detached worktree `D:\IntRuoyiWorktree\form-template-import-auto-recognition-runtime-20260828` 中缺少主线后续编辑能力的旧文件覆盖回主线。
- 2026-09-01 融合复核验证：`scripts\preflight\branch-runtime-port-guard.ps1` -> PASS；`mvn -pl yudao-module-bpm -am "-Dtest=FormCenterRuntimeImportRecognitionFlowContractTest,DefaultWordFormTemplateRecognizerTest,WordTableVisualSchemaBuilderTypeRecognitionTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，9 tests、0 failures、0 errors。
