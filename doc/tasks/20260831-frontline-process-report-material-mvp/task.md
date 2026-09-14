# 工序多物料一线报工 MVP 实施

## Task Goal

在独立 worktree 中按已确认 PRD 实现工序多物料一线报工 MVP：复用路线产品 BOM 的工序物料关系，一线页面按物料切换独立填写数据，页签只按完成数量显示灰色或绿色，一次正式提交全部物料，工序进度取各物料完成数量最小值，批号只读系统每日同步的 ERP 表单数据；完成严格 BDD/TDD、定向回归和真实页面验证后，提交任务代码并安全融合到 `int_main`。

## Milestones

- [x] 创建合规 worktree，并复制 PRD、用户流程和验收标准作为实施基线。
- [x] 核对现有数据模型、正式提交、路线快照、前端状态和 ERP 接口边界，形成设计与测试计划。
- [x] 按 BDD/TDD 完成数据库与后端多物料提交、读取和系统同步批号查询能力。
- [x] 按 BDD/TDD 完成一线生产物料页签、独立草稿、灰绿状态和统一提交。
- [x] 运行数据库、后端、前端、真实页面和相邻回归验证。
- [x] 完成经验归档、收尾清理、任务提交和 `int_main` 融合。

## Expected Verification

- 数据库迁移静态测试和迁移策略门禁通过，新增结构支持同一次正式提交的多条物料明细且不复制 ERP 同步批号。
- 后端单元/契约测试覆盖冻结路线产品 BOM 物料解析、全部物料一次提交、整体事务、缺完成数量失败、物料身份错误失败、最小值进度和系统同步批号读取。
- 前端静态/组件合同覆盖物料页签生成、灰绿状态、`0` 为已填写、清空恢复灰色、独立草稿和一次请求提交全部物料。
- `pnpm.cmd ts:check` 与受影响 Maven 定向测试通过。
- Playwright 通过真实一线生产页面验证至少两个物料的切换、灰绿状态和正式提交请求结构；写入验证只使用任务自有测试数据和已确认账号。
- branch runtime port guard、差异空白检查、任务 evidence validator 和收尾 preview/apply 通过。
- worktree 实现提交完成，`int_main` 按融合前实际增量与脏文件交集检查后安全融合，并复跑关键门禁。

## Current Status

completed

实现、验证、任务数据清理、经验归档、worktree 收尾和 `int_main` 融合均已完成。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；缺少冻结工序物料或正式物料身份时明确失败；同步表没有批号时返回空集合，不改读库存表或直连 ERP。
- `是否从根因和长期维护角度解决`：是；复用路线产品 BOM 正式关系并建立多物料提交明细，不在前端复制第二套物料来源。
- `是否存在临时补丁或绕过`：否；不使用默认物料、物料名称匹配、返回顺序猜测、临时批号或单物料兼容分支冒充完成。

## Worktree

- Path: `D:\IntRuoyiWorktree\20260831-frontline-process-report-material-mvp`
- Branch: `codex/20260831-frontline-process-report-material-mvp`
- Base: `int_main@58479242435efa1f7eafd6e0a17e36bd9c811e5f`
- Runtime slot: `56`；前端 `8311`，后端 `48311`。
- 收尾：原实现分支提交 `3be91f119`，融合分支提交 `39d1b5cc9`，`int_main` 快进至 `c0f957188`，经验文档补充提交 `c385f762d`；两个 worktree 已删除，slot `56/60` 已标记 `active=false`。

## Resolved Implementation Decisions

- 工序进度直接取各物料完成数量最小值，不按 BOM 比例折算、不求和、不任取一项。
- 批号只读系统内每日同步的 ERP 表单；正式来源是同时具备生产订单号、物料编码和批号的 `erp_kingdee_production_pick_list_item`。同步表没有记录时返回空批号集合，不报假成功、不生成占位、不直连 ERP。
- 旧单行主报工保持数量守恒：主报工合格数量等于工序最小进度，主报工不合格数量等于逐物料损耗合计，总报工数量为两者之和；逐物料原始完成数和损耗仍以新明细事实为准。

## Applicable Experience Gates

- 多物料必须保存独立正式事实，主报工只做数量守恒聚合，工序池只使用最小值进度；不得循环单物料提交或让损耗产生负合格数量。
- `.vue` 泛型 helper 不能只以 `vue-tsc` 通过为准；必须同时通过目标 ESLint/Vite 模块转换或真实页面编译。

## Cleanup Keep

- doc/tasks/20260831-frontline-process-report-material-mvp/prd.md
- doc/tasks/20260831-frontline-process-report-material-mvp/user-flows.md
- doc/tasks/20260831-frontline-process-report-material-mvp/acceptance-criteria.md
- doc/tasks/20260831-frontline-process-report-material-mvp/design.md
- doc/tasks/20260831-frontline-process-report-material-mvp/development-plan.md
- doc/tasks/20260831-frontline-process-report-material-mvp/test-plan.md
