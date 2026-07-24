# 任务：电子批记录报表视觉保真优化

## 任务目标

- 通过真实前端路径先清除电子批记录报表，再使用 `A 直接 doc` 重新生成 Jimu 报表。
- 以源 Word 文档 `D:\ProjectPackage\Int\IntRuoyi\resource\批记录模板.doc` 为准，提升重新生成报表在布局、分页节奏、列宽、行高、块结构、页头页脚、留白、跨行跨列关系上的视觉保真度。
- 所有修复优先落在通用规则层，不按报表标题、工序名或表编号硬编码。

## 工作范围

- 后端批记录 DOC 解析、Jimu JSON 构建、布局求解、样式规则与相关测试。
- 必要的前端真实路径验证：`电子批记录 -> 清除电子批记录报表 -> A 直接 doc`。
- 源 Word 与最新生成 Jimu 报表的结构化和可视化对比证据。

## 非目标

- 不为单张报表标题、工序名、表编号添加特判。
- 不新增测试专用前端控件，不改变真实用户路径来掩盖问题。
- 不使用 mock 成功、静默跳过、fallback 或替换对比口径。
- 不改写 live 审核矩阵或芋道源码租户数据。

## 前序任务检查

- 后端上一同仓任务：`D:\ProjectPackage\Int\IntRuoyi\worktrees\automation-2-ebr-visual-fidelity-20260524-review\ruoyi-vue-pro\doc\tasks\20260524-showroom-prompt-template-garbled-text-fix\task.md`
- 状态：`Completed on 2026-05-24`
- 影响：上一后端任务已完成，当前后端 worktree 干净，不阻塞本任务启动。

## 里程碑

- [x] M1：确认 worktree、前序任务和任务文档；记录 BDD 场景。
- [x] M2：通过真实前端路径清除已有电子批记录报表并使用 `A 直接 doc` 重新生成。
- [x] M3：对比最新 Jimu 报表与源 Word，覆盖页头、页脚、分页、表头层级、明细块、列宽、行高、跨行跨列、空白格、汇总区和清场区。
- [x] M4：按可通用规则修复的方向拆分给子 agent，先补 RED 测试，再做最小实现。
- [x] M5：主 agent review 子 agent 结果，只保留通用、风险可控、测试完整的修改。
- [x] M6：合并后回归测试，重新真实生成并再次对比。
- [x] M7：执行 closeout cleanup 预览，更新证据并提交当前任务直接相关文件。

## 预期验证

- 定向后端测试：覆盖 DOC 表格解析、行类型识别、页型/分页、列宽行高、跨行跨列与 Jimu JSON 构建规则。
- 必要前端静态或 E2E 验证：按用户要求使用本任务专用端口，前端 `http://localhost:18081`、后端 `http://localhost:18083`，通过真实用户路径触发清除和 `A 直接 doc`。
- 真实生成验证：清除后重新生成的 Jimu 报表必须与 `D:\ProjectPackage\Int\IntRuoyi\resource\批记录模板.doc` 重新对比。
- 所有 BDD、RED、GREEN 与真实验证结果记录在本任务 `execution-log.md`。

## 当前状态

- 状态：已完成
- 已完成：worktree 复核、前序任务检查、任务文档创建；真实前端清空与 A 路重新生成；Round 0/1/2/3/4/5/6 源 Word/Jimu 证据采集；少列概览页满版规则；低/中列工序页满版规则；T13 陈旧测试断言修正；打包物料矩阵展开规则；源 `.doc` cell 边框样式贯通到 `ParsedCell`/`JsonBuilder`；process 页 footer `生效日期` 紧凑化规则；前端和后端验证端口已切换到 `18081` / `18083`。
- 阻塞：无影响交付的阻塞。automation memory 文件未在当前环境中找到，路径 `$CODEX_HOME\automations\automation-2\memory.md` 无法解析，`C:\Users\BJB110\.codex\automations\automation-2\memory.md` 与 `D:\automations\automation-2\memory.md` 均不存在；影响仅限读取历史 automation 记忆，不阻塞用户本轮提供的完整任务目标。task-closeout-cleanup 已完成 preview，但 apply 被 worktree 自动合并前置条件阻塞，未删除证据文件。
- 当前首要差异：斜线格语义仍未被模型显式表示；部分行高/块高、页脚位置与底部留白节奏仍可继续细化。当前已经具备源 cell 边框信号，footer 紧凑性也已收敛，可继续在此基础上精调块级节奏。

## 最终验证

- 后端单元回归：`MesProBatchRecordReportJsonBuilderTest` 44 条通过；`MesProBatchRecordReportShapeRulesTest` 10 条与布局校准邻近回归 2 条通过。
- 后端打包：`mvn --% -pl yudao-server -am -Dmaven.test.skip=true package` 通过。
- 真实生成：前端 `http://127.0.0.1:18081`、后端 `http://127.0.0.1:18083`，Playwright 真实点击 `清空电子批记录报表` 与 `A 直接 .doc` 通过，重新生成 15 张 A 路报表。
- 结构结果：全 A 路 15 张报表均为 `landscape`；T01 为 `1120`，T06/T10/T11/T14/T15 为 `1044`，T09 为 `1120`；T06/T15 的打包物料矩阵已从单个 merge 区拆成 `物料编码/物料名称/批号` 两组 header + detail rows；固定样本 process 页的 `生效日期` footer 行已重新收敛到紧凑高度 20。
