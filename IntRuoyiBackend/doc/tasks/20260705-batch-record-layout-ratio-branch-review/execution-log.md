# 执行日志

BDD: 复杂工序表保持 Word 行高比例 -> Given 真实批记录 Word 文档中的复杂工序表，When 系统识别并渲染表格，Then 大合并说明区、生产记录区和右侧统计签名区应保持与 Word 原表一致的整体宽高比例，不新增可见文字或幽灵列。

BDD: 正常对照表不被破坏 -> Given 当前已基本正确的简单表、中等复杂表和正常右侧布局表，When 应用通用行高/比例算法修改，Then 行列结构、rowSpan/colSpan、文本落位、边框和视觉比例不得退化。

BDD: 禁止特例通过 -> Given 任一工序名、表格标题、字段文本或页码变化，When 系统执行表格识别和渲染，Then 算法仍应基于 Word 原始结构、合并关系、列宽和行高推导结果工作，而不是依赖文本特征。

GREEN: experience-preflight -> PASS，已记录 PowerShell UTF-8、worktree 隔离、无 fallback、无文本特例、真实页面验证门禁。

## 2026-07-05

- 初始化任务文档，明确本轮只做复杂表整体宽高比例 / 大合并说明区行高模型的分支隔离评审。
- 当前后端主工作区存在无关脏改，本轮不在主工作区直接修改生产代码。

## 2026-07-06

GREEN: page-scale-unit-tests -> PASS，`codex/batch-layout-page-scale` 完成新增宽表/正常宽度对照测试和后端构建。

GREEN: merged-row-unit-tests -> PASS，`codex/batch-layout-merged-row` 完成大合并说明区行高 cap 相关测试和后端构建。

GREEN: source-height-unit-tests -> PASS，`codex/batch-layout-source-height` 完成 source row height 保真相关测试和后端构建。

GREEN: route-b-timeout-precondition -> PASS，确认 RouteB Word COM 真实文档提取耗时约 173s，原 180s timeout 对全量真实导入过紧；三个候选分支均补充 `yudao.mes.batch-record-report.route-b.timeout-ms` 系统属性用于验证环境显式配置，默认值仍为 180000，非法值 fail fast。

GREEN: page-scale-real-import -> PASS，隔离端口 48083 + 8083 真实导入 `RE-PP-ID-01（A 1）球囊扩张压力泵生产记录(1).doc`，`ok=true`、`importedCount=15`、`updatedCount=15`、`errors=[]`，截图目录：`screenshot-review/scale-import-all-timeout-20260706-012954`。

FAIL: page-scale-visual-review -> FAIL，`comparison-review/old-vs-new-pixel-diff-summary.json` 显示 10 个代表性表单与旧渲染截图 `changedPixelsOver8=0`，候选修改未改变真实页面。

GREEN: merged-row-real-import -> PASS，隔离端口 48083 + 8083 真实导入同一 Word 文档，`ok=true`、`importedCount=15`、`updatedCount=15`、`errors=[]`，截图目录：`screenshot-review/merged-import-all-timeout-20260706-015004`。

FAIL: merged-row-visual-review -> FAIL，`comparison-review/merged-vs-old-pixel-diff-summary.json` 显示无有效视觉变化；`光固Ⅱ` 仅为 `rms=0.025` 且 `changedPixelsOver8=0` 的非实质差异。

GREEN: source-height-real-import -> PASS，隔离端口 48083 + 8083 真实导入同一 Word 文档，`ok=true`、`importedCount=15`、`updatedCount=15`、`errors=[]`，截图目录：`screenshot-review/source-import-all-timeout-20260706-020429`。

FAIL: source-height-visual-review -> FAIL，`comparison-review/source-vs-old-pixel-diff-summary.json` 显示 10 个代表性表单与旧渲染截图 `rms=0.0`、`changedPixelsOver8=0`，候选修改未改变真实页面。

FAIL: branch-review-final -> FAIL，三条隔离候选均未通过真实页面截图验证，当前不应合入任何候选。共性原因指向“视觉列宽 / 视觉网格推导”而非整体缩放或单纯大合并行高度。

## 2026-07-08

RED: `python -X utf8 -m pytest script/tests/test_showroom_legacy_product_code_auto_confirmable_draft_sql.py -q` -> FAIL，测试文件不存在，SQL 草案缺少 `script/tests/` 契约门禁。

GREEN: `python -X utf8 -m pytest script/tests/test_showroom_legacy_product_code_auto_confirmable_draft_sql.py -q` -> PASS，契约测试确认草案保持 review-only、无可执行 SQL 行，且每条确认映射都有产品名称与 legacy code 成对更新。
