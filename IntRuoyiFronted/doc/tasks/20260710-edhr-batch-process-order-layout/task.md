# eDHR 批次工序顺序与列表显示修复

## 任务目标
- 修复批次详情左侧将普通工序显示在“成品检记录（92）”之后的问题。
- 普通工序按工艺路线顺序显示，收尾特殊节点固定显示在普通工序之后、放行之前。
- 工序编码和工序名称保持紧凑可读，不发生文本重叠或卡片串行溢出；辅助表单继续由右侧面板展示。

## 上一任务检查
- 已完成任务 `doc/tasks/20260710-edhr-process-companion-forms/` 状态为 `completed`。
- 当前工作区存在同页面的并行未提交改动；本任务仅修改左侧工序排序渲染、对应样式和回归测试，不覆盖填写载体及右侧面板改动。

## 经验门禁
- PowerShell / UTF-8：已读取根目录 `docs/powershell-memory.md`，中文文档使用 UTF-8。
- 前端样式：遵循 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md` 的紧凑操作台样式。
- 缺陷修复：先复现特殊节点整体提前渲染，再新增失败回归测试，最后做最小实现。
- 前端契约：不修改后端接口、任务状态、权限或真实排序字段，只修正前端展示分组与布局。

## 设计约束检查
- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；按特殊节点业务阶段拆分前置与收尾展示集合，不再依赖模板块的固定先后位置。
- `是否存在临时补丁或绕过`：否。

## BDD 场景
- BDD: 普通工序位于收尾节点之前 -> Given 批次同时包含普通工序和成品检特殊节点 / When 用户打开批次详情 / Then 普通工序显示在灭菌、成品检报告、成品检记录之前。
- BDD: 来料检仍位于普通工序之前 -> Given 批次存在来料检特殊节点 / When 用户查看左侧工序列表 / Then 来料检显示在普通工序之前。
- BDD: 放行保持最后 -> Given 批次存在普通工序和收尾节点 / When 用户查看左侧工序列表 / Then 放行固定显示在全部工序之后。
- BDD: 工序文本不重叠 -> Given 工序编码和名称较长 / When 左侧栏宽度受限 / Then 编码名称保持单行省略且完整内容可通过提示查看。

## 里程碑
1. [已完成] 建立任务文档、经验门禁和 BDD 场景。
2. [已完成] 新增排序与文本布局 RED 静态测试。
3. [已完成] 最小修复特殊节点分段渲染和列表文本布局。
4. [已完成] 运行目标回归、类型检查和真实页面验证。
5. [已完成] 更新证据、清理任务产物并独立提交任务改动。

## 预期验证
- `node tests/e2e/edhr-batch-process-order-layout-static.spec.js`
- `node tests/e2e/edhr-batch-process-display-sort-static.spec.js`
- `node tests/e2e/edhr-batch-process-card-density-static.spec.js`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm.cmd ts:check`
- 本机测试租户真实登录后只读验证批次详情左侧顺序与文本布局。

## 当前状态
- COMPLETED：修复代码已随同页并行任务提交 `99be6babb` 进入当前 `int_main`；本任务回归测试和实施记录已提交 `a7e099710`。静态回归、类型检查、管理员真实登录、真实批次 `900000000480` 只读页面验收及收尾清理均通过。

## Cleanup Candidates
- 已清理 `tests/output/20260710-edhr-batch-process-order-layout/`。
- 已清理任务临时证据 `bug-regression-evidence.md` 和 `frontend-feature-evidence.md`。
- 保留 `task.md`、`execution-log.md` 和 `verification-report.md`。

## Current Status
completed
