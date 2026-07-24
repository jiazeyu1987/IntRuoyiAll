# eDHR 工序辅助表单联动展示

## 任务目标
- 批次详情左侧按工序展示主表、损耗单、过程检验单和参数记录表，不再把同工序多个任务显示成无法区分的重复工序。
- 每个表单展示槽位名称、表单名称、状态和门禁原因，并可进入对应表单。
- 表单页保留批次任务上下文，用户可返回同一批次继续填写未完成辅助表单。

## 上一任务检查
- `doc/tasks/20260710-edhr-batch-process-card-density/task.md` 状态为 `completed`，提交 `bb09d2efe` 已完成，不阻塞本任务。

## 经验门禁
- PowerShell / UTF-8：已读取 `docs/powershell-memory.md`。
- 前端样式：遵循 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md` 的紧凑操作台样式，不做无关重设计。
- 前端契约：不新增接口，沿用现有任务字段和打开任务 API。
- BDD + 严格 TDD：先新增静态行为契约并取得 RED，再修改组件。

## 设计约束检查
- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；使用已有 `routeProcessId + formSlotType` 建立工序表单组。
- `是否存在临时补丁或绕过`：否。

## BDD 场景
- BDD: 同工序表单分组展示 -> Given 一个工序包含主表和三类辅助表单 / When 用户打开批次详情 / Then 左侧只显示一个工序组并列出 4 个可区分表单。
- BDD: 辅助表单显示真实状态 -> Given 同工序表单处于待打开、已提交或完成状态 / When 用户查看工序组 / Then 每个表单显示自己的状态和门禁原因。
- BDD: 点击槽位打开对应表单 -> Given 用户选择损耗单或检验单 / When 点击表单项 / Then 使用该任务 ID 和报告 ID 打开对应执行页。
- BDD: 表单页返回批次继续填写 -> Given 用户从批次详情进入表单页 / When 点击返回批次执行 / Then 返回原批次并聚焦原任务。

## 里程碑
1. [已完成] 建立任务文档、BDD 和前端证据骨架。
2. [已完成] 新增工序分组、槽位标签和返回路径的 RED 静态测试。
3. [已完成] 最小实现同工序表单组展示和表单页返回批次。
4. [已完成] 运行静态回归、类型检查和本机真实页面验证。
5. [已完成] 收尾清理、任务完成记录和独立提交准备。

## 预期验证
- `node tests/e2e/edhr-batch-process-companion-forms-static.spec.js`
- `NODE_OPTIONS=--max-old-space-size=8192 pnpm ts:check`
- 本机测试租户真实登录后，只读验证批次详情表单组。

## 当前状态
- COMPLETED：批次详情已按 `routeProcessId` 展示表单组，槽位状态/门禁/打开入口和表单返回上下文已完成；静态契约、类型检查和结构性真实页面验收通过。当前数据库没有任何多槽位历史任务，完整四槽位真实数据验收保持显式前置缺口，不创建测试数据绕过。

## Current Status
completed
