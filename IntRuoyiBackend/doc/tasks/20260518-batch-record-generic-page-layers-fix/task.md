# Task: 批记录通用页型与版式分层修复

## Goal

用 4 个子 agent 继续修复批记录生成链路，但不针对某一张表格硬编码。
本任务把共享能力拆成 4 层：
- 页型识别层
- 行类型识别层
- 版式求解层
- 视觉渲染层

目标是让同一套规则能同时改善 `信息汇总页`、`工序记录页`、`重复明细页`，
并避免把 `装配及包装信息` 误当成 `产品信息`。

## Scope

- 使用 4 个子 agent 并行处理 4 个修改层
- 不新增单模板分支，不按具体表格名写特例
- 优先复用已有共享规则与共享 calibrator/json/style 链路
- 若缺少固定样本、运行时、测试依赖，必须失败并记录阻塞

## Previous Task Check

- Previous backend task:
  `doc/tasks/20260516-six-route-report-doc-consistency-review/task.md`
- Status before this task: blocked
- Impact: 共享通用修复已进入第一批，但 live 重生仍被固定源合同和无关编译问题阻塞

## Milestones

- [x] M1: 建立任务包并完成 4 个子 agent 分工
- [x] M2: 落地共享页型识别层
- [x] M3: 落地共享行类型识别层
- [x] M4: 落地共享版式求解与视觉渲染层
- [x] M5: 跑通聚焦测试并记录验证证据

## Expected Verification

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\worktrees\batch-record-print-fidelity-phase2\pom.xml -pl yudao-module-mes -am -Dtest=<focused tests> test`
- 相关共享规则的单元测试
- 至少一轮实际样本/实际运行态复核

## Current Status

In progress. 这轮 4 层共享修复已经进入 live Route B 运行时，并且又完成了一轮“4 个子 agent 分项优化 + 主线程 review”收口，当前重点结果如下：

- `页型识别层` 仍稳定把固定源第 1 张信息汇总表锚到 `装配及包装信息 / 过程放行信息` 尾段，没有退回 `产品信息`
- `版式求解层 + JSON 构建层` 现在统一使用预算后的渲染列宽，并且共享信息汇总页已解除 `72px` 列宽天花板，首张 live 纸面宽度已从 `432` 拉到 `960`
- `共享高度预算` 现在同时覆盖通用工序页和粗洗固定布局，并增加了“明细行数量感知”的 preferred viewport budget
- `共享行类型 / shape / style` 现在能更好地区分勾选结果列、长说明区和密集重复明细区，减轻 `精洗 / 清洁` 页的挤压与重复明细可读性问题
- 这一轮继续把共享工序页的“满宽预算”往无显式标题行的场景扩了一步：
  - 只要 `tableTitle` 已经被共享标题规则识别为 `PROCESS_RECORD`
  - 即使原始行里没有单独的标题行，仍然走工序页的满宽归一化
  - 同时把最终 `columnWidths` 的预算前移到单页压缩之前计算，避免被 `72px` 每列上限先压扁再也回不来
- 最新一轮继续把这个共享预算补齐到真实目标宽度：
  - 当共享工序页的列宽总和仍低于目标渲染宽度时
  - 现在会按当前列宽占比把剩余预算继续分配回各列
  - 这样 `清洁工序生产记录` 这类原始内容偏窄、但整体应按工序页满宽展示的页面，不会再停在 `720px`
  - 共享中密度页预算也已从 `1000px` 提升到 `1040px`，更贴近当前 target 截图的纸面利用率
- 共享高密度页预算也已从 `1120px` 收到 `1044px`，让 `精洗` 这类 20 列通用 dense 页不再明显比目标图更宽
- 最新一轮继续把 dense 页的“首屏高度”从纯宽度问题收口到了 JSON 层：
  - 不再全局抬高所有页面的 `650px` 单页上限
  - 只对真实 `Route B` dense 工序页在 `JsonBuilder` 层应用更高的单页高度目标
  - 这样 `精洗 / 清洁` 下半页能多进首屏，而 `粗洗` 固定布局不会被一起放大到失真
- 这一轮继续把同样的思路补到了共享中密度工序页：
  - `JsonBuilder` 仍保持通用 `650px` 作为默认单页上限
  - 真实 live-like dense 工序页继续走 `680px`
  - 真实 live-like medium 工序页也开始走 `670px`
  - 这样 `清洁工序生产记录` 这类 10 列、带汇总和页脚的共享工序页，不再被 generic `650px` 首屏上限过早截断
- 最新一轮继续把剩余的 `2/4` 下半页问题收口到两条共享规则：
  - `SharedRowTypeRules` 现在会把“位于结构化表头之后、带大量空白列、重复出现的稀疏勾选明细”识别成 `DETAIL_DATA`，不再误落到 `TABLE_HEADER`
  - `LayoutCalibrator` 的 preferred viewport budget 现在会识别 `SUMMARY` 后仍挂着 `TABLE_HEADER + LONG_DESCRIPTION` 结构化尾段的页面，为这类共享页型回补首屏高度预算
  - 两条规则都不依赖 `粗洗 / 清洁` 名称，仍然只依赖共享 `RowType` 信号

Latest live verification:

- 新 fixed source `D:\ProjectPackage\Int\IntRuoyi\resource\批记录模板.doc` 已经在重启后的 `48082` runtime 上成功重生
- `recognize-fixed?routeKey=B` 持续返回 `importedCount=15`, `updatedCount=15`
- live Route B 第 1 张报表标题仍保持为 `装配及包装信息`
- 新一轮 live 对比中，`1.png` 的纸面占比继续保持提升；`3.png` 的 live `show` 已从 `1120` 收到 `1044`，横向占比更贴近目标图；`4.png` 的 live `show` 已从 `dataRectWidth = 720` 拉到 `1040`，纸面横向展开进一步改善；同时真实 fixed-source `精洗` JSON 也已经能越过通用 `650px` 单页上限；`2.png / 4.png` 的首屏完整度继续改善
- 这一轮补充后，真实 fixed-source `清洁` JSON 也已经能越过通用 `650px` 单页上限；重新运行 MES 聚焦回归后为 `84` 通过、`0` 失败、`6` 跳过；前端再次按真实纸宽抓图后，`3-fine-wash-live.png = 1044x720`、`4-clean-process-live.png = 1040x720` 继续保持稳定
- 最新一轮共享尾段/重复明细修复 live 后，MES 聚焦回归已更新为 `86` 通过、`0` 失败、`6` 跳过；`Route B` 真实重生继续返回 `importedCount=15`, `updatedCount=15`；重新抓图后，`2.png` 的 `生产后清场记录` 更靠近首屏，`4.png` 的重复压力表明细被压得更平，`生产后清场记录` 标题和尾段也比上一轮更完整地进入首屏
- 最新一轮 JSON 层修复后，`JsonBuilder` 不再把 `calibrator` 已经压缩好的单行短明细重新抬回默认高度；真实 fixed-source `清洁` 页重复压力表明细在 JSON 中已从 `24px` 回落到与 `calibrator` 一致的 `20px`，`build_shouldNotReinflateLiveRepeatedChecklistDetailRowsOnFixedCleanProcessPage` 也已经转绿
- 重新运行 MES 聚焦回归后已更新为 `88` 通过、`0` 失败、`6` 跳过；`yudao-server` package 继续通过；`48082` live 重生与四图重抓保持成功，最新 compare 仍维持真实纸宽基线
- 最新一轮继续把信息汇总页的共享纸宽预算从 `960` 提到 `1120`，并补上了 `JsonBuilder` 的“窄列共享页补足预算”路径：
  - 低列数共享页在原始列宽总和低于共享预算时，不再直接落回 `72px` 上限压扁
  - 而是按当前列宽占比把宽度补足到共享预算
  - fixed-source `装配及包装信息` 概览页 builder 测试已经转成 `dataRectWidth = 1120`
  - 清缓存后重新抓图，live `1-assembly-packaging-live.png` 也已经从 `960x720` 拉到 `1120x720`
- 这一轮继续微调共享中密度页纸宽预算：
  - `MesProBatchRecordReportShapeRules` 的 medium band 预算从 `1040` 提到 `1044`
  - 这样 `清洁工序生产记录` 这类 10 列共享工序页在 live `json_str` 中也能对齐到 `dataRectWidth = 1044`
  - 配合抓图脚本改成以 `showMeta.dataRectWidth` 为主裁切，最新 live `4-clean-process-live.png` 已经从 `1040x723` 变成 `1044x723`
- 当前方向已切换为 `源 doc 优先`：
  - `Route B` 固定源第 1 个 Word 顶层表现在完整保留 `产品信息 / 配件进货批号信息 / 装配及包装信息 / 过程放行信息`
  - `EBR_B_T01` 不再只暴露代表尾段 `装配及包装信息`，而是恢复为源表首段标题 `产品信息`
  - live `recognize-fixed?routeKey=B` 继续保持 `importedCount=15`, `updatedCount=15`
  - `JsonBuilder` 的固定源概览页宽度回归已同步改到 `产品信息` 锚点，并继续保持 `dataRectWidth = 1120`
- 源 doc 优先的 `T4 / 清洗工序生产记录` 已完成第一步跨页节奏修复：
  - `LayoutCalibrator` 现在会用重复清洗参数行作为共享信号，为长重复工序页插入续页文档页头
  - 这条规则不依赖 `清洗工序生产记录` 表名，而是识别 `清洗次数 / 清洗介质 / 清洗功率 / 清洗温度 / 清洗时间` 这类重复操作参数组
  - live `EBR_B_T04` 的 JMReport JSON 现在有两个 `球囊扩张压力泵生产记录` 文档页头，第二个页头位于 `手柄` 段之后、`齿条` 段之前
  - Route B 真实重生继续保持 `importedCount=15`, `updatedCount=15`
- 源 doc 优先的 `T13 / 单包装工序生产记录` 也完成了续页页头节奏修复：
  - `LayoutCalibrator` 现在会把长重复设备参数矩阵作为共享续页信号
  - 规则识别 `封口热合机 / 热合机 / 自动热合机 / □是 / □否` 这类重复设备矩阵行，不依赖 `单包装工序生产记录` 表名
  - live `EBR_B_T13` 现在有两个 `球囊扩张压力泵生产记录` 文档页头，第二个页头位于热合设备矩阵中段、`生产自检` 之前
  - Route B 真实重生继续保持 `importedCount=15`, `updatedCount=15`
- 源 doc 优先的 `T1 / 产品信息` 已恢复三段信息汇总页头节奏：
  - `LayoutCalibrator` 现在会在长信息汇总页中识别相隔足够远的独立 `*信息` 段标题，并在后续大段前插入续页文档页头
  - live `EBR_B_T01` 现在有三个 `球囊扩张压力泵生产记录` 文档页头，分别覆盖 `产品/生产零配件`、`配件进货批号信息`、`装配及包装信息/过程放行信息`
  - Route B 真实重生继续保持 `importedCount=15`, `updatedCount=15`
- 源 doc 优先的 `T4 / 清洗工序生产记录` 又向块内结构推进了一步：
  - `LayoutCalibrator` 现在会把带烘干子层的重复清洗块右侧空白结果列按块级纵向合并
  - 这条规则仍不依赖 `清洗工序生产记录` 表名，而是依赖“重复清洗块 + 烘干子层 + 尾部空白列”这一组共享结构信号
  - live `EBR_B_T04` 的首个物料块右侧尾部空列已经在 JMReport JSON 中落成 `merge: [2,0]`
  - Route B 真实重生继续保持 `importedCount=15`, `updatedCount=15`

## Review Notes

- 页型识别层现在不再只做“识别短标题”，而是会为多短标题信息汇总表选出更能代表目标页的共享锚点
- 行类型识别层现在不仅参与列宽分配，也参与单页高度预算和压缩优先级
- 这轮没有引入按 `粗洗 / 精洗 / 清洁 / 装配` 名称写死的分支，规则仍保持在共享标题/共享行类型/共享 shape/style 层
- 子 agent 回传里最值得保留的是“builder 二次压宽修复”“粗洗固定布局接入共享首屏预算”“勾选列语义宽度保护”“密集重复明细轻量缩字”，这些都已通过主线程 review
- 最新一轮子 agent 复核也把剩余问题进一步收敛到了两类共享信号：
  - `2.png` 更像是 `SUMMARY` 后还挂着结构化尾段，但当前 preferred viewport budget 仍把它当普通 6-detail 页在压
  - `4.png` 更像是重复稀疏勾选明细被误判成 `TABLE_HEADER`，导致共享高度预算和 JSON 压缩优先级都没有把它当真正明细处理
- 最新一轮主线程修复后，`JsonBuilder` 已开始尊重 `calibrator` 的短明细压缩结果，不再在 JSON 层把单行重复明细重新撑高；这一步主要影响 `4.png` 的重复压力表细节和 `2.png` 的尾段推进空间
- 这轮进一步确认 `1.png` 的主矛盾确实是概览页横向预算，而不是页型、缓存或 viewer 外壳；builder 小回归、固定源概览页回归和清缓存后的 live 抓图三者已经对齐到同一结论
- 最新一轮确认 `4.png` 的宽度差距已经不再来自 viewer 裁切，而是可以直接通过共享中密度预算收敛；`2.png` 仍然是 rough fixed layout，当前宽度并不受 medium band 常量控制
- 源 doc 优先后，`T1` 的主差异从“只保留尾段代表页”收口为“首张 live 报表完整承载源 doc 第一个 Word 表”；这会让原先截图优先的 `1.png` 对比口径发生变化，属于用户选择 `源 doc 优先` 后的预期结果
- `T4` 本轮没有拆新报表，也没有按表名硬编码；它只是把源 doc `page-06 ~ page-07` 的续页页头节奏恢复到当前长页结构里，为后续继续恢复清洗/烘干块层次留出稳定锚点
- `T13` 本轮同样没有拆新报表；它把源 doc `page-16 ~ page-17` 的第二页热合/封口设备矩阵续页节奏恢复到当前长页结构里，后续可以继续细化设备矩阵内部列宽和块间层次
- `T1` 本轮也没有拆额外模板；它把源 doc `page-01 ~ page-03` 的三页页头节奏恢复到同一个 `EBR_B_T01` 长报表里，保持 Route B 的 15 模板数量不变
- `T4` 这一轮没有再改页头节奏，而是把源 doc 每个清洗/烘干物料块右侧的结果列更像原始块级结构；后续还能继续细化第二个及后续物料块的空列合并覆盖面

## Open Questions

- 是否需要继续把 `MesProBatchRecordSharedPageTitleRules` 扩展为显式“页锚点 + 页主体范围”模型，减少后续 route 之间的重复接线
- 如果后续还要继续压 `2/4` 的下半页，可考虑在不改模板数的前提下继续细化共享首屏高度预算，而不是恢复按工序名特判
- `1.png` 的主要矛盾已从“纸面太窄”收敛到“纸面占比仍可再贴近目标图”；如果后续继续追它，优先看页边距、首屏截图口径和整体缩放，而不是再回到 `432px` 级别的结构性宽度问题
- `4.png` 的剩余问题已经从“明显窄表”收敛成“还需继续把重复明细区和最底部清场区往首屏里推”，后续应继续细化共享高度预算和重复明细区的行高/列宽协同
- 源 doc 优先模式下，下一步最值得继续细化的是 `T4` 第二个及后续物料块右侧结果列的块级合并覆盖面、`T13` 的设备矩阵列宽/块间层次，以及 `T1` 三段内的局部列宽和空白行节奏；当前 `T1 / T4 / T13` 已先恢复续页文档页头节奏，`T4` 的首块结果列也已开始按块合并
