# DCC 项目代码列表未分类文件批量自动归类

## Task Goal

在 `基础数据 / DCC项目代码` 列表页顶部工具栏新增“按文件名归类未分类”入口，按当前查询筛选条件处理全部项目代码，包括未加载分页；对每个项目代码关联文档中的“未分类”阶段或“未分类文件类型”文件，复用正式 DCC 文件分类树和现有关联文件元数据更新接口，按文件名相似度归类到最大可能的正式阶段/文件类型。

## Milestones

- [x] 创建任务文档并记录 BDD/TDD 验收口径。
- [x] 建立列表级静态契约，先证明列表页缺少全量批处理入口与跨分页查询逻辑。
- [x] 实现列表页按钮、全部分页项目代码遍历、逐项目未分类文件归类与刷新。
- [x] 运行目标静态契约、相邻 DCC 契约与 TypeScript 检查。
- [x] 记录验证报告，完成清理、提交和推送。

## Expected Verification

- `pnpm e2e:dcc:project-code-list-unclassified-auto-classify:static`
- `pnpm e2e:dcc:project-code-associated-unclassified-auto-classify:static`
- `pnpm e2e:dcc:project-code-associated-three-column:static`
- `pnpm ts:check`

## Current Status

completed

## Applicable Experience Gates

### DCC 基础条目关联文档分类树门禁

- Trigger: DCC 基础条目、DCC项目代码关联文档、三栏导航文件类型、`fileTypeTaxonomyId`、`fileTypeLevel3`、DCC 文件分类树、技术文档阶段展开、未分类文件类型、按文件名归类未分类、未分类自动归类、列表页按文件名批量归类、未加载分页。
- Preflight check: 以 `DCC文件分类` 的正式树作为分类来源，按 `技术文档 / 阶段 / 文件类型` 解析阶段直接子分类；基础条目关联文件只能影响数量和右侧文件列表，不能反向决定中间列完整分类集合。若入口位于列表页且用户要求处理全部项目代码，必须按当前筛选条件从第 1 页遍历到总页数，不能只处理当前页已加载行。
- Blocker: 中间文件类型列只从当前关联文件的 `fileTypeLevel3` 动态生成、已配置但当前无文件的正式子分类不显示、`fileTypeTaxonomyId` 已能解析第三级却被归入“未分类文件类型”、或用“未分类文件类型”替代正式子分类时必须停止。
- Verification: 聚焦静态合同必须断言分类 helper 同时提供阶段、阶段直接子分类和 taxonomy path 第三级解析；页面合同必须断言中间列先由阶段直接子分类预置，再按文件归组计数；列表页批量归类合同还必须断言保留当前筛选条件、全分页拉取项目代码、逐项目全分页拉取关联文件和批处理进度/失败可见；运行目标 DCC 静态合同、相邻 DCC 文件分类静态合同和 `pnpm ts:check`。
- Forbidden action: 禁止用 `fileTypeLevel3`、当前关联文件列表、默认 `MAIN`、空值回填、`formBindings` 或前端硬编码文案替代正式 DCC 文件分类树；禁止吞掉分类树缺失导致的真实不一致。
- Evidence: `doc/tasks/20260731-dcc-project-code-associated-taxonomy-types/`、`doc/tasks/20260801-dcc-project-code-auto-classify-unclassified/` 与 `doc/tasks/20260801-dcc-project-code-list-auto-classify-unclassified/`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，计划复用正式 DCC 文件分类树、正式项目代码分页接口和正式关联文件元数据更新接口。
- `是否存在临时补丁或绕过`：否。
