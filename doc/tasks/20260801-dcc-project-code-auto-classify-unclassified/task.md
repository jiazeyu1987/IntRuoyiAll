# DCC 基础条目未分类关联文件自动归类

## Task Goal

在 `DCC基础条目 > 关联文档` 三栏导航中新增一个按钮，将当前基础条目下处于“未分类”阶段或“未分类文件类型”的关联文件，按文件名与正式 DCC 文件分类树中的阶段/文件类型名称相似度，批量分配到最相似的正式阶段和文件类型；执行后当前产品不应继续保留“未分类”或“未分类文件类型”文件。

## Milestones

- [x] 建立 BDD/TDD 验收记录并保存既有脏工作区基线。
- [x] 新增静态契约，先证明当前页面缺少未分类自动归类按钮和相似度归类逻辑。
- [x] 实现前端按钮、候选分类选择、批量分配调用和归类后刷新。
- [x] 运行目标静态契约、相邻 DCC 契约与 TypeScript 检查。
- [x] 记录验证报告和收尾状态。

## Expected Verification

- `pnpm e2e:dcc:project-code-associated-unclassified-auto-classify:static`
- `pnpm e2e:dcc:project-code-associated-three-column:static`
- `pnpm e2e:dcc:category-lifecycle-stage:static`
- `pnpm e2e:dcc:file-type-taxonomy-basic-data:static`
- `pnpm e2e:dcc:file-type-taxonomy-tree-display:static`
- `pnpm e2e:dcc:file-type-taxonomy-unified-list-template:static`
- `pnpm ts:check`

## Current Status

completed

## Applicable Experience Gates

### DCC 基础条目关联文档分类树门禁

- Trigger: DCC 基础条目、项目代码、关联文档三栏导航、`fileTypeTaxonomyId`、`fileTypeLevel2/fileTypeLevel3`、中间“文件类型”列、DCC 文件分类树或技术文档阶段展开。
- Preflight check: 以 `DCC文件分类` 的正式树作为分类来源，按 `技术文档 / 阶段 / 文件类型` 解析阶段直接子分类；基础条目关联文件只能影响数量和右侧文件列表，不能反向决定中间列完整分类集合。
- Blocker: 中间文件类型列只从当前关联文件的 `fileTypeLevel3` 动态生成、已配置但当前无文件的正式子分类不显示、`fileTypeTaxonomyId` 已能解析第三级却被归入“未分类文件类型”、或用“未分类文件类型”替代正式子分类时必须停止。
- Verification: 聚焦静态合同必须断言分类 helper 同时提供阶段、阶段直接子分类和 taxonomy path 第三级解析；页面合同必须断言中间列先由阶段直接子分类预置，再按文件归组计数；运行目标 DCC 静态合同、相邻 DCC 文件分类静态合同和 `pnpm ts:check`。
- Forbidden action: 禁止用 `fileTypeLevel3`、当前关联文件列表、默认 `MAIN`、空值回填、`formBindings` 或前端硬编码文案替代正式 DCC 文件分类树；禁止吞掉分类树缺失导致的真实不一致。
- Evidence: `doc/tasks/20260731-dcc-project-code-associated-taxonomy-types/`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，按钮使用正式 DCC 文件分类树和现有关联文件分配接口，避免仅靠未分类文案或前端硬编码。
- `是否存在临时补丁或绕过`：否。
