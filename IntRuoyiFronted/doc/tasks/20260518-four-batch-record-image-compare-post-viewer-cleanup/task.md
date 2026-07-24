# Task: 四张批记录截图对比复核

## Goal

使用当前刚生成并经过 viewer 工具条清理后的电子批记录预览页，对比
`D:\ProjectPackage\Int\IntRuoyi\resource\批记录截图`
下的 4 张目标图，判断修改之后是否继续有优化，并记录每张图还剩下的主要视觉差异。

## Scope

- 只做对比复核与证据记录
- 对比口径是“当前 live 生成预览页”对“resource\批记录截图\1-4.png”
- 不做按模板名硬编码的新优化
- 若缺少真实预览入口或图片映射关系，必须直接报告阻塞

## Previous Task Check

- Previous frontend task:
  `doc/tasks/20260517-frontend-jmreport-viewer-chrome-suppression/task.md`
- Status before this task: completed
- Impact: viewer 顶栏抑制已完成，这一轮只复核修改后的视觉效果是否继续优化

## Milestones

- [x] M1: 建立任务包并确认前序任务已完成
- [x] M2: 识别 4 张目标图对应的表单
- [x] M3: 抓取当前 live 生成页的对应截图
- [x] M4: 完成逐张对比并记录结论

## Expected Verification

- 能读取 `D:\ProjectPackage\Int\IntRuoyi\resource\批记录截图\1.png` 至 `4.png`
- 能访问当前前端预览入口并抓取对应 live 截图
- 在 `execution-log.md` 记录逐张对比结论与是否继续优化

## Current Status

Completed for this compare pass. 基于 backend 最新的 Route B live 重生结果，4 张当前 live 预览页截图已经重新抓取完成；第 `1` 张不再落到旧的 `产品信息` 页型，而是直接切到 `装配及包装信息`。另外，这轮已经把截图口径从不稳定的 `8082` designer 壳页切到直接抓 backend `/jmreport/view` 纯报表页，并进一步收紧到纸面 `#jm-sheet-wrapper .jm-sheet` 级别。当前仍有 residual visual gaps，但本轮“对比复核与证据记录”目标已完成。

Latest recheck note:

- 在 backend 最新一轮“共享工序页满宽预算 + 压缩前列宽预算回灌”修复落地后，已经再次按同一脚本重抓 4 张 live 图
- 在继续补上“剩余宽度分配”之后，`4.png -> 清洁工序生产记录` 的 live `show` 已从 `dataRectWidth=720` 拉到 `1000`，对应纸面横向展开比上一轮继续更开
- 后端又把共享中密度页预算从 `1000` 收到 `1040`，`清洁工序生产记录` 的 live `show` 也已经跟上
- 后端进一步把共享 dense 页预算从 `1120` 收到 `1044`，`精洗工序生产记录` 的 live `show` 也已经跟上
- 比较脚本也已经进一步收紧到“真实纸宽”口径：
  - 读取每张 live `/jmreport/view` 页面的 `jmreport/show` 响应
  - 使用返回的 `dataRectWidth` 作为截图裁剪宽度
  - 因此当前 `3/4` live 图已经不再带着先前那段额外的右侧 viewer 空白
- 这一轮再次按同一口径重抓后，当前 artifacts 继续稳定为真实纸面宽度：
  - `3-fine-wash-live.png = 1044x720`
  - `4-clean-process-live.png = 1040x720`
- `1/2/3/4` 的总体判断现在更明确：`1` 页型正确且首屏占比已明显优于早期版本，`3` 和 `4` 的横向占比已经都更贴近目标图，`2` 的底部完整度继续缓慢改善

## Comparison Summary

- `1.png -> 装配及包装信息`
  - Improved materially
  - 当前 live 已能直接对应到 `装配及包装信息 + 过程放行信息`，文档头、装配矩阵和放行区都已进入首屏
  - 在最新一轮 backend worker 优化后，表内列宽不再被二次压窄；改成直接抓 `/jmreport/view`，再进一步缩到 `#jm-sheet-wrapper .jm-sheet` 纸面后，截图口径已稳定。随着后端概览页列宽修复，`1.png` 的纸面占比已经实质变大，右侧空白也比前几轮更少
  - 这轮后端继续把概览页共享纸宽预算从 `960` 提到 `1120`，并且抓图脚本在清缓存后已经拿到最新宽度，最新 live 首图也确实从 `960x720` 变成了 `1120x720`
  - 继续把 compare 口径按目标图高度对齐后，`1-assembly-packaging-live.png` 已经能稳定抓到 `1120x783`，和目标图的尺寸一致
- `2.png -> 粗洗工序生产记录`
  - Improved
  - viewer 顶栏已经消失，页头、灰底层级、红色 `/pcs` 更接近目标图
  - 在最新一轮 live 中，下半页完整度继续提升，`生产批量汇总` 与后续清场区比上一轮更靠前、更接近首屏；这轮共享尾段预算修复后，`生产后清场记录` 的标题和头两行已经比上一轮更完整地进入当前截图口径
- `3.png -> 精洗工序生产记录`
  - Improved
  - viewer 顶栏已去掉，页头更干净，底部 `生产批量汇总 / 生产后清场记录` 已比上一轮更完整进入首屏
  - 最新一轮继续收窄了 dense 页宽预算，live `show` 已到 `1044`，并且比较脚本已经裁到真实纸宽；列宽比例、检查区密度和整体横向占比都更贴近目标图；当前它仍然是 4 张里最接近目标的一张
- `4.png -> 清洁工序生产记录`
  - Improved
  - viewer 顶栏已去掉，标题与页头更干净，下半页比上一轮进入更多
  - 在 backend 最新一轮共享宽度预算修复后，live `show` 已从 `720` 拉到 `1040`，并且比较脚本已经裁到真实纸宽；重复压力表明细和底部清场区不再像上一轮那样明显挤在左半边；不过多行压力表明细和更下方的清场区仍未完整展开，内容完整性仍不足
  - 在最近一次 backend medium-height 预算放宽后，这张图的横向口径保持稳定，但剩余问题已经更集中到“重复稀疏明细”和“清场尾段”本身的共享高度预算，而不是截图口径
  - 这轮共享行型和尾段预算修复后，重复压力表明细被进一步压平，`生产后清场记录` 标题和后续两行都更完整地推进到了首屏；它仍未完全达到目标图，但已经不再像前几轮那样主要卡在左半页挤压和尾段缺失
  - 最新一轮继续把共享中密度纸宽预算收口到 `1044` 后，配合 compare 脚本按 `showMeta.dataRectWidth` 裁切，当前 live `4-clean-process-live.png` 已经对齐到 `1044x723`

## Full Mapping

- `source table 1 -> live EBR_B_T01 装配及包装信息`
  - Source doc pages: `page-01` to `page-03`
  - Source doc actual content: `生产记录汇总表 / 产品信息 / 生产零配件批号信息 / 配件进货批号信息 / 装配及包装信息 / 过程放行信息`
  - Current live difference:
    - live 已锚到 `装配及包装信息 + 过程放行信息`
    - 但 source doc 这一整张顶层表前半段的 `产品信息 / 配件进货批号信息` 没有一起作为同页前序区保留下来
    - 所以当前 live 更像“首张顶层表的尾段代表页”，而不是 source doc 原始三页的完整连续版
- `source table 2 -> live EBR_B_T02 粗洗工序生产记录`
  - Source doc pages: `page-04`
  - Current live difference:
    - 整体结构已基本对应 source doc
    - 剩余差异主要是列宽比例、行高压缩、清场尾段进入首屏的程度
- `source table 3 -> live EBR_B_T03 精洗工序生产记录`
  - Source doc pages: `page-05`
  - Current live difference:
    - 已基本对应 source doc
    - 剩余差异主要是检查区密度、列宽比例和下半页汇总区占比
- `source table 4 -> live EBR_B_T04 清洗工序生产记录`
  - Source doc pages: `page-06` to `page-07`
  - Source doc actual content:
    - 多个零件分段
    - 每段都有 `清洗次数 / 清洗介质 / 清洗功率 / 清洗温度 / 清洗时间`
    - 并且带 `烘干温度℃ / 烘干时间` 的重复子结构
  - Current live difference:
    - 这是 source doc 里明显的多页重复子结构页
    - 当前系统若按单页 preview 输出，天然更容易把重复清洗/烘干子段压平
    - 这类页与 source doc 的主要差异预计会集中在重复子结构完整度，而不是标题识别
- `source table 5 -> live EBR_B_T05 清洁工序生产记录`
  - Source doc pages: `page-08`
  - Current live difference:
    - 标题、检查区、重复压力表区和清场区都已对应
    - 剩余差异主要是尾段完整度和结构密度
- `source table 6 -> live EBR_B_T06 组装Ⅰ工序生产记录`
  - Source doc pages: `page-09`
  - Current live difference:
    - 当前未发现明显页型错位
    - 预期剩余差异主要是字段区与明细区的比例、字距和填空区宽度
- `source table 7 -> live EBR_B_T07 光固Ⅰ工序生产记录`
  - Source doc pages: `page-10`
  - Current live difference:
    - 当前未发现明显页型错位
    - 预期剩余差异主要是工艺参数列宽与表头密度
- `source table 8 -> live EBR_B_T08 硅化Ⅰ工序生产记录`
  - Source doc pages: `page-11`
  - Current live difference:
    - 当前未发现明显页型错位
    - 预期剩余差异主要是长说明和字段区的高度分配
- `source table 9 -> live EBR_B_T09 硅化Ⅱ工序生产记录`
  - Source doc pages: `page-12`
  - Current live difference:
    - 当前未发现明显页型错位
    - 预期剩余差异主要是列宽和页脚前尾段的密度
- `source table 10 -> live EBR_B_T10 组装Ⅱ工序生产记录`
  - Source doc pages: `page-13`
  - Current live difference:
    - 当前未发现明显页型错位
    - 预期剩余差异主要是多字段行和检查区比例
- `source table 11 -> live EBR_B_T11 检测工序生产记录`
  - Source doc pages: `page-14`
  - Current live difference:
    - 当前未发现明显页型错位
    - 预期剩余差异主要是检测结果列的宽度和勾选区密度
- `source table 12 -> live EBR_B_T12 光固Ⅱ工序生产记录`
  - Source doc pages: `page-15`
  - Current live difference:
    - 当前未发现明显页型错位
    - 预期剩余差异主要是参数列宽和结果区占比
- `source table 13 -> live EBR_B_T13 单包装工序生产记录`
  - Source doc pages: `page-16` to `page-17`
  - Source doc actual content:
    - 第 2 页明显进入封口/热合设备参数与多设备选项矩阵
  - Current live difference:
    - 这是另一个 source doc 明显跨两页的复杂页
    - 当前系统如果仍按单页 preview 生成，最容易丢的是后半页设备矩阵完整度
- `source table 14 -> live EBR_B_T14 中包装工序生产记录`
  - Source doc pages: `page-18`
  - Current live difference:
    - 当前未发现明显页型错位
    - 预期剩余差异主要是字段区与尾段的纵向压缩
- `source table 15 -> live EBR_B_T15 大包装工序生产记录`
  - Source doc pages: `page-19`
  - Current live difference:
    - 当前未发现明显页型错位
    - 预期剩余差异主要是尾段留白和清场区占比

## Main Findings

- 这轮最确定的优化是共享层：
  - JMReport viewer 顶栏已被抑制
  - 预览画面比之前更接近纯净纸单
  - 四张 live 图现在都已经稳定按真实纸宽抓取，不再混入右侧 viewer 空白
- 这轮仍然存在的共性问题是报表本体：
  - `粗洗 / 清洁` 仍有下半页完整度不足的问题
  - `精洗 / 清洁` 仍有明显结构压缩和留白失衡
  - `装配及包装信息` 虽然已经有正确 live 页型，但高度占比和整体口径仍不如目标图完全贴近
  - 这轮以后，`2/4` 的主问题已经从“尾段完全进不来”收敛到“还可以再多进一点、比例再贴一点”
  - 其中 `4.png` 的宽度口径现在已经基本和目标一致，剩余差距更集中在尾段完整度和结构密度
  - 从 source doc 全量页级对照看，当前最需要继续关注的不是单页页型错乱，而是 3 类跨页/复杂结构：
    - `source table 1` 的三页首张顶层表完整承载问题
    - `source table 4` 的重复清洗/烘干子结构
    - `source table 13` 的单包装第二页设备矩阵
  - 最新一轮后，`4.png` 的重复压力表行已经明显不再被 re-inflate，剩余差距更像是尾段再多露一点，而不是行高逻辑还在反向抬高

## Blocker And Impact

- Blocker 1: `粗洗 / 清洁` 的底部内容仍未完整进入当前预览首屏，说明共享整页高度预算还可以继续压缩
- Blocker 2: `精洗 / 清洁` 仍有列宽比例和留白失衡，说明共享列宽/行高协同规则还不够贴近目标纸面比例
- Impact: 这轮已经可以确认 4 张图都基于最新 live 代码重新生成并继续优化，但还不能宣称视觉保真任务已经完全收敛
