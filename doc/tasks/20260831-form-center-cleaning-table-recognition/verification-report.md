# Verification Report

## Result

PASS：表单中心能够从包含多个物理表格和同表多个工序的 Word 生产记录中唯一选择清洗工序，并按源文档结构生成可填写表格；未引入文件名、产品名或工序名硬编码，也未增加静默 fallback。

## Automated Verification

- 后端定向与相邻回归：10 tests，0 failures，0 errors。
- 真实 DOCX 已作为 BPM 模块正式测试资源提交，classpath 读取回归同样为 10 tests、0 failures、0 errors，SHA-256 与用户源文件首次读取证据一致。
- 后端完整打包：`yudao-server` 及依赖模块 `BUILD SUCCESS`。
- 前端斜线与源样式静态合同：PASS。
- 前端目标文件 ESLint：PASS。
- 前端 TypeScript 全量检查：PASS。
- 生产代码硬编码审计：未发现“清洗”“粗洗”“压力泵”或 `IDPR` 特例。
- `git diff --check`：无空白错误；仅保留 Windows CRLF 转换提示。

## Structural Acceptance

- 选中的逻辑表单为 45 列、44 行，不包含粗洗工序字段。
- 保留标题、生产前检查、设备编号、8 个物料清洗块、生产自检、生产批量汇总和生产后清场记录。
- 保留关键横向/纵向合并、列宽、行高、字体、对齐、边框和 Word 段落换行。
- 共识别 96 个斜线禁填格，方向均为源文档对应的右上到左下方向。
- 标题中的关键/非关键选择框保持静态，不再错误生成填写控件。
- 空白实际值、日期、数量、操作人和复核人按位置生成独立填写规则；斜线格不生成填写规则。
- 源文档螺纹块区域的错误表头忠实保留，识别器未静默改写。
- 多候选无法唯一匹配时明确失败，不回退到第一张表。

## Real Page Verification

- 使用 worktree 前端 `8312` 和后端 `48312`，通过真实表单中心导入最终测试模板并打开预览。
- 页面确认标题与设备编号分行、45 列源网格、8 个物料块、斜线方向、生产自检、批量汇总和清场记录均正确显示。
- Form Center 仍按既有设计在可填写格上叠加字段类型标记；该标记不属于源 Word 内容，不影响源表格结构。
- 唯一控制台错误来自外部头像资源连接失败，与表单识别及渲染无关。
- 任务创建的 3 个测试模板已通过正式页面提交作废并显示“已作废”，未直接修改数据库。

## Residual Risk

- 当前候选选择依赖逻辑表单标题与导入模板名称存在唯一语义匹配；没有唯一匹配时会按设计阻断并返回明确错误。
- 本次验证针对 DOCX；旧 `.doc` 文件未作为输入，需先转换为 DOCX 后再进入现有导入链路。

## Scope

- 未修改表单中心上传 API、审批、版本或持久化合同。
- 用户已确认截图正确并要求融合至 `int_main`；融合过程不得覆盖主工作区其它任务的未提交改动。
- 正式回归样本存放于 BPM 模块测试资源目录，测试不依赖或改写主工作区 `resource` 下正被打开的用户原文件。
- 实现已通过 `--ff-only` 融合进入 `int_main`；主工作区并行文档改动经独立 stash 审计、恢复和哈希/numstat 复核后保持未提交状态，未混入本任务提交。

## Closeout

- 前后端服务和 Playwright 浏览器均已停止，`8312`、`48312` 无监听。
- cleanup preview 无阻塞；apply 已删除临时截图、技能证据文件和前端依赖目录，`mvn clean` 已删除后端构建产物。
- 保留 `task.md`、`execution-log.md`、`verification-report.md`、生产代码、正式回归测试和真实 DOCX fixture。
- 用户要求查看识别结果后，已从最终作废模板 `Codex清洗工序识别20260831C` 的真实表单中心最大化预览重新生成 4 张分段截图并列入任务保留清单；未重新导入或修改业务数据。
- 融合后在 `E:\IntRuoyi` 重跑后端 10 项回归、前端静态合同、ESLint、TypeScript、完整后端打包和端口门禁，结果全部通过。
- `D:\IntRuoyiWorktree\form-center-cleaning-recognition` 已从 Git worktree 登记和文件系统中删除；本地任务分支提交已由 `int_main` 包含。
- 端口登记中的 `form-center-cleaning-recognition / slot 57 / 8312 / 48312` 已标记 `active=false`，并保留 `deletedAt`、`cleanupTask` 审计信息；其它 worktree 登记未变。

## 2026-09-01 Int Main Recheck

- 用户再次要求融合后，复核当前 `int_main` 历史已包含清洗工序识别实现提交 `59316b6a1` 与融合收尾提交 `5b875f56c`。
- 未把旧 detached worktree 的 `FormCenterRuntimeServiceImpl.java` 覆盖到主线，因为该旧文件缺少 `int_main` 后续表单模板编辑相关实现，覆盖会造成回退风险。
- `scripts\preflight\branch-runtime-port-guard.ps1` PASS，确认 `int_main/int_main` 仍使用前端 `8081`、后端 `48081`。
- BPM 识别相关回归 PASS：`FormCenterRuntimeImportRecognitionFlowContractTest`、`DefaultWordFormTemplateRecognizerTest`、`WordTableVisualSchemaBuilderTypeRecognitionTest` 共 9 tests、0 failures、0 errors。
