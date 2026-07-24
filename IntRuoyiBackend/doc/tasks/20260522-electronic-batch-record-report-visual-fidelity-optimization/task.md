# Task: 电子批记录报表视觉保真优化

## Goal

让系统通过 `电子批记录 -> A 直接 doc` 重新生成的 Jimu 报表，在布局、分页节奏、列宽、行高、块结构、页头页脚、留白、跨行跨列关系上尽量接近源 Word 文档 `D:\ProjectPackage\Int\IntRuoyi\resource\批记录模板.doc`，并且优先通过共享规则层修复，而不是针对单张报表硬编码。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-mes\src\main\java\cn\iocoder\yudao\module\mes\service\pro\batchrecordreport\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-mes\src\test\java\cn\iocoder\yudao\module\mes\service\pro\batchrecordreport\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-mes\src\test\java\cn\iocoder\yudao\module\mes\controller\admin\pro\batchrecordreport\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\src\views\**` 中与 `电子批记录 -> A 直接 doc`、`清除电子批记录报表` 真实入口直接相关的页面
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-electronic-batch-record-report-visual-fidelity-optimization\**`

## Non-Scope

- 不按某个报表标题、工序名、表编号硬编码布局补丁。
- 不通过替换对比口径、改截图或改前端展示逻辑来掩盖真实差异。
- 不引入 fallback、mock 成功、静默跳过或兼容分支。
- 不改动与电子批记录报表视觉求解无关的 showroom / DCC / 其他模块在途改动。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-showroom-product-batch-cover-auto-resume\task.md`
- Status before this task: `Blocked due scope switch on 2026-05-22`
- Impact: 上一同仓任务已显式阻塞，不阻塞本次电子批记录报表视觉保真优化；本任务仍需避开当前仓库内无关 showroom 在途改动。

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Current state: 仓库存在多组 showroom / SQL / AI 未提交改动。
- Impact: 本任务只允许修改电子批记录报表视觉规则、相关定向测试、必要的真实入口代码与本任务文档，不能覆盖无关改动。

## Milestones

1. 建立任务目录与工件，确认历史电子批记录布局任务、真实生成入口、源 Word 对比对象和当前阻塞条件。
2. 执行真实 `清除电子批记录报表 -> A 直接 doc` 生成，完成首轮差异盘点并按共享规则层拆分任务。
3. 先补失败测试，再按通用规则修改页型识别、行类型识别、版式求解、JSON 构建或视觉渲染规则。
4. 使用多个子 agent 并行处理彼此写入范围不冲突的优化方向，主 agent review 后只合入通用且风险可控的修改。
5. 跑定向测试、回归验证、重新真实生成并完成与源 Word 的再次对比。

## Expected Verification

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am "-Dtest=MesProBatchRecord*Test" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- 真实前端路径执行 `清除电子批记录报表 -> A 直接 doc` 后，重新打开生成结果并与 `D:\ProjectPackage\Int\IntRuoyi\resource\批记录模板.doc` 对比页头、页脚、分页节奏、表头层级、明细块结构、列宽比例、行高块高、跨行跨列、空白格表现、汇总区与清场区位置比例。
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260522-electronic-batch-record-report-visual-fidelity-optimization --mode preview`

## Current Status

Blocked on 2026-05-23 after the accepted shared pagination/layout slice was committed.

## Current Progress

- 已修复 live 生成前的两层跨租户唯一键阻塞：
  - `jimu_report.uniq_jmreport_code`
  - `mes_pro_batch_record_report.uk_mes_batch_record_report_sample_route_table`
- 已通过真实前端完成 `清空电子批记录报表 -> A 直接 .doc`，当前测试租户成功生成 15 张 Route A 报表。
- 已完成首轮真实视觉基线采集：
  - 源 Word PDF 页级截图位于 `artifacts/source-pdf-pages/`
  - 最新 Route A 生成清单与截图位于 `artifacts/route-a-baseline-manifest.json` 与 `artifacts/*.png`
- 已确认首轮不是单纯列宽微调问题，当前更上游的两个主差异为：
  1. Route A / Doc 解析后的表块顺序漂移，第 1 张报表错误落成 `装配及包装信息`，未保持源 Word 的前置产品信息/汇总结构。
  2. Jimu 预览中的大量填报控件叠压、遮挡、挤占表格空间，导致块高、留白、分页节奏明显偏离源 Word。
- 已完成第二轮 accepted 修改并重新 live 验证：
  - 第 1 张报表已恢复为 `产品信息`
  - `delete-all` 在当前测试租户已能真实删除 `15/15`
  - 紧凑填报网格页的控件尺寸与占位文案已有收敛，但仍未达到源 Word 保真目标
- 新发现：部分 live 报表右侧边界呈锯齿形，不同块的右侧结束线未保持统一，整体轮廓不是稳定矩形
- 已完成第三轮 accepted 修改并重新 live 验证：
  - `检测工序`、`光固Ⅱ`、`中包装` 等工序页的 live JSON 列越界已清零
  - 右侧结构性锯齿已从“超出声明列边界”收回到共享矩形预算内
  - 仍残留的主要问题转为 fillForm 密度过高导致的局部视觉不平整，而非结构越界
- 已完成第四轮 accepted 修改并重新 live 验证：
  - `生产批量汇总`、清场语义行、trailing padding 列上的无意义 fillForm 已被进一步移除
  - compact fill 已补充轻量外观参数 `border=false`、`size=small`
  - 当前 `T11` / `T14` 的 fillForm 总量继续下降，但正文明细录入区仍存在较多真实输入框
- 已完成第五轮 accepted 修改并重新 live 验证：
  - 通过共享 `cssStr` 将电子批记录报表的 fillForm 容器与输入框统一压成透明背景、无边框、无圆角
  - `T11` / `T14` 中原本大面积厚白框已明显变淡，矩形边界和纸面感进一步收敛
  - 当前剩余问题更多集中在分页节奏、块高留白和页头节奏，而不是白框噪音本身
- 已完成第六轮 accepted 修改并重新 live 验证：
  - 新增共享 `singlePageTargetHeight` 规则，`JsonBuilder` 统一用 relaxed `670px` 单页预算承接 live-like dense / medium / 叙述型低明细工序页
  - `LayoutCalibrator` 不再对 `检测工序生产记录` 这类叙述型低明细工序页做二次过压，但仍保持 `粗洗工序生产记录` 等固定矩阵页的紧凑预算
  - 新增 RED `build_shouldNotOverCompressFixedRouteADetectionPageIntoLargeBottomWhitespace` 已从 `totalHeight=632` 收敛到 `>=660px` 区间
  - 真实 live 再生后，`T11` 非白像素高度由上一轮约 `649px` 抬升到 `685px`，页内下半段空白已明显收敛
- 已完成第七轮 accepted 修改并重新 live 验证：
  - `JsonBuilder` 现在会对“文档头块 + 重复文档头块”统一注入共享页装饰：顶部留白、续页头前分页留白、`fixedPrintHeadRows`
  - `printConfig.marginY` 在文档头页型上已抬到共享值，`T10` 当前标题已完整露出，不再是上一轮那种明显顶切
  - `T13` 的续页头前留白已再扩大一档，起页感比上一轮更明确，但和源 Word 真页间距相比仍偏弱
- 已完成第八轮 accepted 修改与脚本修复：
  - `fixedPrintHeadRows` 已从错误的整数数组修正为积木报表约定的范围对象数组
  - `capture-source-doc-pdf-pages.mjs` 已替换为 `pdfjs-dist + pdf.worker + canvas native binding` 的真页渲染实现，当前可稳定输出 19 张互不相同的源页 PNG
  - 新源页基线已证明 `source-page-01/02/10/16/17/19.png` 不再重复，后续分页节奏对比口径恢复可靠
- 已恢复真实工作流前置条件：
  - 当前验证库已应用现有迁移 `sql/mysql/20260522_mes_route_process_batch_record_binding.sql`
  - `mes_pro_route_process.batch_record_report_id` 已补齐，真实 `delete-all` 再次恢复到 `15/15`
  - Route A 严格 `清空 -> A 直接 .doc` 再次回到 `createdCount=15`、`updatedCount=0`
- 已完成第九轮 accepted 修改并重新 live 验证：
  - 已确认 `zonedEditionList` 不参与打印分页，真正影响续页断点的是物理行高和 `row.pagingRow`
  - `JsonBuilder` 现在会对“文本语义匹配的重复文档头首行”输出 `pagingRow=true`，不再要求续页头和首页头部完全同构
  - 最新 `T13` live JSON 中已确认 `ROW 17 pagingRow=true`、前一行 `ROW 16 height=56`
  - 最新 `T13` 截图里第二个页头前的断页留白比上一轮更明显
- 已完成第十轮 accepted 修改并重新 live 验证：
  - 已确认 `jmsheet.js` 的 break-before 语义是“在命中 `pagingRow=true` 的当前行前强制翻页”，因此原先把 `pagingRow` 挂在续页头首行会把 `56px` spacer 留在上一页底部
  - `JsonBuilder.applyPageDecorations()` 现在会把 `pagingRow=true` 挂到续页 spacer 行本身，不再挂在续页头行上
  - 真实 `jmreport/show` 运行时已确认最新 `T13` 为 `ROW 16 pagingRow=true`、`ROW 16 text=' '`、`ROW 16 height=56`
  - 严格真实工作流再次通过：`delete-all` 仍为 `15/15`，随后 Route A 再次返回 `createdCount=15`、`updatedCount=0`
- 已完成第十一轮 accepted 验证脚本改进并重新 live 复核：
  - 已确认 Jimu 的真实分页对比口径不能继续只依赖 `/jmreport/view/{id}` 的连续长页截图；当前更稳的生成态抓取路径是 viewer 内的 `导出 -> PDF图像`
  - 已新增任务脚本 `scripts/render-pdf-pages.py`，可稳定将任意生成 PDF 渲染成逐页 PNG
  - 已实际导出并渲染 `T13` / `T10` 的打印态 PDF，其中 `T13` 当前为 `2` 页，`T10` 当前为 `1` 页
  - 新打印态证据表明：`T13` 的核心问题已不再只是“续页前留白弱”，而是第 1 页尾部存在一个源 Word 第 16 页中并不存在的 synthetic document header
- 新确认的验证风险：
  - 旧版 `source-pdf-pages/*.png` 重复截图问题已修复；后续必须继续使用新的 PDF 真页渲染脚本，不能退回浏览器 `file://...#page=` 截图口径
  - `/jmreport/exportPdfStream` 在当前 live 上仍不稳定；真实 `导出 -> PDF` 会命中 `Font.getSize()` 空指针，当前打印态基线先统一使用 `PDF图像` 导出
- 新确认的环境阻塞：
  - 当前整包 `yudao-server` 重新构建会被无关 DCC 模块 `DccControlledFileController` 的编译错误阻塞
  - 本轮真实 live 验证已显式使用“只替换当前任务相关 `yudao-module-mes` nested jar”的受控方式完成，未改动 DCC 代码，也未把该阻塞静默掩盖

## Remaining Priority

1. 优先回到 `LayoutCalibrator.insertContinuationHeadersForLongRepeatedOperationSegments()` 这一层，重新审视 synthetic document header 的适用边界；打印态证据已经表明，Route A `T13` 当前多出来的页尾重复头不贴近源 Word。
2. 继续修正 `printConfig` / print box / 页面顶部预留的共享规则，虽然 `T10` 已不再明显顶切，但整体页级留白仍未完全贴近源页。
3. 后续生成态对比统一优先使用“`导出 -> PDF图像` -> `render-pdf-pages.py` 渲染逐页 PNG”的口径，不再把连续长页截图当作主基线。
4. 在不触碰无关 DCC 代码的前提下，持续记录整包 `yudao-server` 编译阻塞，以及 `/jmreport/exportPdfStream` 的 live NPE 风险，避免后续验证误把工具链问题当业务问题。

## Scope Switch Recovery

- 2026-05-22 用户已明确恢复继续处理本电子批记录报表视觉保真优化任务。
- 影响：本任务从临时挂起恢复为进行中；后续改动继续限定在电子批记录报表共享规则、相关定向测试与本任务文档范围内。

## Blocker And Impact

- Blocker:
  - 当前打印态仍确认存在 Route A `T13` synthetic continuation header 与源 Word 不一致的分页差异。
  - live `/jmreport/exportPdfStream` 仍会命中 `Font.getSize()` 空指针，限制了最终打印态对比口径。
  - 整包 `yudao-server` 重新构建仍被无关 DCC 编译错误阻塞，只能继续使用受控 nested-jar 替换方式验证。
- Impact:
  - 共享分页与版式收敛修改已按独立后端提交落盘，但本任务还不能标记为最终完成。
  - 后续若继续追求与源 Word 的最终打印态一致性，必须基于当前打印态证据另起后续 slice，继续处理 continuation header 适用边界与页级留白规则。
