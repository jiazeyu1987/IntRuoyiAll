# 一线生产重复行组批记录链接 PRD

## Task Goal

形成可直接用于设计、开发和验收的产品需求：在现有“批记录单元格链接”页面增加“重复行组”模式，只配置当前工序一线生产字段到当前工序正式批记录表单重复记录的对应关系；生产组长点击“申请放行”时，再按正式一线生产提交顺序统一生成批记录资料。

## Milestones

- [x] M1：核对现有固定单元格链接、完整报工字段目录和正式批记录来源边界。
- [x] M2：冻结用户确认的模板行、重复区域、候选行确认和顺序写入规则。
- [x] M3：输出 PRD、用户流程和验收标准。
- [x] M4：运行 PRD 结构校验并完成最终记录。
- [x] M5：按用户确认修正生成时点，删除一线提交时写入、数量一致性和独立复核时点逻辑。
- [x] M6：按用户最新澄清收紧范围：本页面只做对应关系，申请放行时的数据生成不把数量不一致或复核时间作为本功能问题。

## Expected Verification

- PRD 包含首版范围、非目标、功能要求、业务规则、状态、异常、验收标准和待决问题。
- 用户流程覆盖配置、一线提交不生成、申请放行统一使用对应关系和版本变化。
- 验收标准可直接转为 BDD、后端测试、前端测试和真实 E2E。
- 产品需求校验脚本通过。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；采用工序及表单版本隔离的重复行组，并把配置、正式生产事实和申请放行资料生成三阶段明确分离，不按工序名或固定 4 行硬编码。
- 是否存在临时补丁或绕过：否。

## Current Status

completed

PRD、用户流程和验收标准已按最新业务边界修订：这里只做对应关系，申请放行时才生成批记录；申请放行时生产数据已封口，本需求不处理数量不一致，也不配置或生成复核时间字段。产品需求结构校验、差异检查、经验沉淀和任务清理门禁均通过。需求文档完成不代表功能已开发，功能实现仍为待开发。

## Cleanup Keep

- doc/tasks/20260812-batch-record-repeating-row-link-prd/task.md
- doc/tasks/20260812-batch-record-repeating-row-link-prd/execution-log.md
- doc/tasks/20260812-batch-record-repeating-row-link-prd/verification-report.md
- doc/tasks/20260812-batch-record-repeating-row-link-prd/docs/product/prd.md
- doc/tasks/20260812-batch-record-repeating-row-link-prd/docs/product/user-flows.md
- doc/tasks/20260812-batch-record-repeating-row-link-prd/docs/product/acceptance-criteria.md
