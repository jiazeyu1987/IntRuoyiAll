# 任务：电子批记录报表视觉保真优化接续

## 任务目标

- 在新前后端 worktree 中接续 Automation `automation-2`。
- 通过真实前端路径先点击 `清除电子批记录报表`，再点击 `A 直接 doc` 重新生成 Jimu 报表。
- 以源 Word 文档 `D:\ProjectPackage\Int\IntRuoyi\resource\批记录模板.doc` 为准，继续提升布局、分页节奏、列宽、行高、块结构、页头页脚、留白、跨行跨列关系的视觉保真度。
- 所有修复必须优先落在共享规则层，不能按报表标题、工序名或表编号硬编码。

## 工作范围

- 后端批记录 DOC 解析、版式求解、Jimu JSON 构建、视觉样式规则与测试。
- 必要前端真实路径验证；除非入口本身损坏，否则不计划修改前端生产代码。
- 源 Word 与最新生成 Jimu 报表的结构化和可视化对比证据。

## 非目标

- 不为单张报表做特判修补。
- 不新增测试专用前端控件。
- 不用 mock、fallback、静默跳过或替换对比口径掩盖失败。
- 不修改正式租户数据或 live 审核矩阵。

## 前序任务检查

- 后端上一同主题任务：`doc/tasks/20260524-ebr-report-visual-fidelity/task.md`
- 状态：已完成。
- 前端上一同主题任务：`..\yudao-ui-admin-vue3\doc\tasks\20260524-ebr-report-visual-fidelity\task.md`
- 状态：已完成。
- 影响：不阻塞本任务启动。

## 里程碑

- [x] M1：新建 paired worktree，创建任务文档、BDD、PRD、开发计划与测试计划。
- [x] M2：完成真实清除与 `A 直接 doc` 重新生成，采集最新对比对象。
- [x] M3：覆盖页头、页脚、分页、表头层级、明细块、列宽、行高、跨行跨列、空白/斜线格、汇总区、清场区的差异清单。
- [x] M4：按可通用规则拆分子 agent 任务，执行 RED -> GREEN。
- [x] M5：主 agent review 子 agent 结果，仅保留通用、风险可控、测试完整的修改。
- [x] M6：合并后回归测试并重新真实生成对比。
- [x] M7：task-closeout-cleanup preview，提交当前任务直接相关文件。

## 预期验证

- 后端定向测试：`DocParser`、`ShapeRules`、`LayoutCalibrator`、`JsonBuilder` 及相邻回归。
- 前端验证：如未改前端，仅执行真实 Playwright 路径；如改前端，增加定向测试和 lint。
- 真实生成验证：清除后通过 `A 直接 doc` 重新生成，并对比源 Word。
- 所有 BDD、RED、GREEN 和真实验证结果记录到 `execution-log.md`。

## 当前状态

- 状态：Round 4 已完成代码、测试、review、打包和真实生成验证，等待 closeout preview 与 scoped commit。
- 已完成：真实前端清空/生成五轮；Round 0 差异清单；Round 1 长重复设备矩阵分页通用规则；Round 2 明细/重复矩阵空白输入视觉静默规则；Round 3 doc-like 页脚固定打印尾行规则；Round 4 分页后当前明细带续页表头通用规则；后端定向测试、相邻回归、生产打包、真实生成验证；独立 reviewer 复审放行。
- 当前阻塞：task-closeout-cleanup apply/自动合并仍预计阻塞，原因是主后端 worktree 存在未清理改动且本任务分支不能直接 fast-forward 到 `int_main`；前端主分支 `master` 没有对应 checked-out worktree。该阻塞不影响本分支已完成的功能验证和提交，但影响自动快进合并与删除 worktree。
- Round 1 保留方向：当连续重复设备参数矩阵足够长，且后续跟随汇总/清场结构化尾块时，在矩阵内部断页，避免尾块直接贴在超长矩阵之后。
- Round 2 保留方向：明细数据行和重复设备矩阵行中的空白输入控件保留 fillForm 能力，但视觉上保持空白，不显示 `请填写` 和输入边框干扰源 Word 的空白格节奏；普通字段行和说明性大空白不受影响。
- Round 2 真实验证：测试租户 `122` 于 `2026-05-25 23:03:58` 通过真实前端清空后重新生成 15 条 Route A Jimu 报表；T01/T13 等明细空白控件已转为 quiet props，T04/T05/T13 剩余 `请填写` 属普通字段提示。
- Round 3 保留方向：当表格存在 doc-like 页头并且共享行类型识别到 FOOTER 行时，把页脚行登记为 Jimu `fixedPrintTailRows`；普通无 doc-like 页头表格不启用该规则。
- Round 3 真实验证：测试租户 `122` 于 `2026-05-25 23:55:39` 通过真实前端清空后重新生成 15 条 Route A Jimu 报表；15 条全部包含 1 个 `fixedPrintTailRows` 范围并匹配 `生效日期` 页脚行。
- Round 4 保留方向：当 builder 插入 pagingRow 后直接续接明细/重复设备矩阵数据带时，只在当前连续明细带正上方复制最近且结构可复用的 TABLE_HEADER 行；不可复用表头、非明细续接或任何结构边界均不跨段回退。
- Round 4 真实验证：测试租户 `122` 于 `2026-05-26 01:08:38` 通过真实前端清空后重新生成 15 条 Route A Jimu 报表；T01/T04/T13 有 pagingRows，T13 pagingRow `[19]` 后继续重复设备矩阵且无 header clone，因为当前源结构没有紧邻的可复用 TABLE_HEADER；merge ranges 未跨 pagingRow 或 cloned header。
