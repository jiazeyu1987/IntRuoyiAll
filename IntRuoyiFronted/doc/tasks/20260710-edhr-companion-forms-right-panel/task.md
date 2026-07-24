# eDHR 工序辅助表单右侧详情展示

## 任务目标
- 左侧只保留工序导航，不在工序卡片内展开主生产表、损耗单、过程检验单和参数记录表。
- 右侧详情根据当前选中工序动态展示该工序实际配置的表单；每个工序允许无表单、单表单或不同组合。
- 每张表单独立展示槽位名称、表单名称、状态、门禁原因和打开入口。

## 上一任务检查
- `doc/tasks/20260710-edhr-process-companion-forms/task.md` 状态为 `completed`，提交 `9fa5fdc58` 已完成，不阻塞本任务。

## 经验门禁
- PowerShell / UTF-8：已读取 `docs/powershell-memory.md`，中文文件仅用 UTF-8 安全路径读写。
- 前端页面：只调整 eDHR 批次详情的信息架构，不改变后端接口、权限、任务状态和打开任务契约。
- BDD + 严格 TDD：先增加右侧动态表单列表的失败契约，再最小修改组件。
- 无 fallback：当前工序没有表单时明确显示空状态，不补默认主表或虚拟任务。

## 设计约束检查
- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；工序负责导航，当前工序的任务集合负责右侧详情和操作。
- `是否存在临时补丁或绕过`：否。

## BDD 场景
- BDD: 左侧只展示工序 -> Given 一个工序配置多张表单 / When 用户查看批次详情 / Then 左侧仅出现一个工序导航项，不展开表单任务。
- BDD: 右侧展示当前工序表单 -> Given 当前工序配置主表和任意辅助表单 / When 用户选择该工序 / Then 右侧按实际配置列出对应表单、状态和入口。
- BDD: 不同工序表单组合独立 -> Given 相邻工序配置不同表单组合 / When 用户切换工序 / Then 右侧列表同步切换且不混入其他工序任务。
- BDD: 无表单工序明确为空 -> Given 当前工序没有表单任务 / When 用户选择该工序 / Then 右侧明确提示未配置表单，不生成默认任务。

## 里程碑
1. [已完成] 建立任务文档并确认现有页面结构与约束。
2. [已完成] 新增右侧动态表单列表的 RED 静态测试。
3. [已完成] 最小调整左侧工序导航和右侧详情。
4. [已完成] 运行静态回归、类型检查和管理员只读页面验证。
5. [已完成] 收尾清理和独立提交准备。

## 预期验证
- `node tests/e2e/edhr-batch-companion-forms-right-panel-static.spec.js`
- `node tests/e2e/edhr-batch-process-companion-forms-static.spec.js`
- `node tests/e2e/edhr-batch-process-card-density-static.spec.js`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm.cmd ts:check`

## 当前状态
- COMPLETED：左侧仅保留工序导航，右侧按当前工序动态展示实际表单组合、状态、门禁和打开入口；静态、类型与真实只读页面验证通过。

## Current Status
completed
