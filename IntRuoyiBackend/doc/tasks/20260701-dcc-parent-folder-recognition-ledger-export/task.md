# 任务：DCC 父文件夹产品识别并发与共享识别记录导出

## 任务目标
- 在本机实现 DCC 受控浏览父文件夹产品名称/产品编码识别。
- 支持页面设置 Codex 并发数，并将并发数随批量任务持久化。
- 成功、失败识别均写入共享识别记录，默认防止重复识别。
- 支持导出共享识别记录 Excel，用于判断哪些文件已识别。

## 里程碑
- [in_progress] 建立任务文档、经验门禁与 BDD/TDD 基线。
- [pending] 补后端 RED 测试，覆盖 workerCount 入参、任务快照、记录导出、防重复。
- [pending] 实现后端 schema、服务、导出接口与记录账本字段。
- [pending] 实现前端 workerCount 输入、父目录识别文案与识别记录导出入口。
- [pending] 运行后端与前端验证，记录 GREEN 证据并提交本机改动。

## 经验门禁
- PowerShell/UTF-8：已读取 docs/powershell-memory.md；中文文件读写必须显式 UTF-8。
- 后端交付：按 backend-api-delivery 执行，记录 API/服务行为、BDD、RED/GREEN。
- 数据库交付：按 database-schema-delivery 执行，schema 变更必须非破坏性并补测试。
- 前端交付：按 frontend-feature-delivery 执行，不隐藏接口错误，不引入 mock 数据。

## 设计约束检查
- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，通过任务快照、共享账本和文件级 claim 正式解决并发与防重复。
- 是否存在临时补丁或绕过：否。

## BDD 场景
- BDD: 父文件夹含子目录批量识别 -> Given 文控在受控浏览选择父文件夹 / When 启动批量识别并包含子目录 / Then 任务候选文件来自父目录及所有子目录且同一文件只排队一次。
- BDD: 页面设置 5 个 Codex 并发 -> Given 文控输入并发 Codex 数量 5 / When 创建批量识别任务 / Then 后端将 workerCount=5 持久化为任务快照，并按该快照并发执行。
- BDD: 已有成功或失败识别记录默认跳过 -> Given 文件在同一识别范围与版本下已有 SUCCESS 或 FAILED 记录 / When 未勾选重新识别 / Then 不再调用 Codex，任务计入跳过。
- BDD: 成功失败记录可导出 -> Given 多个目录均产生识别记录 / When 文控导出识别记录 / Then Excel 包含目录路径、文件名、文件 ID、状态、产品名称、产品编码、失败原因、任务 ID、识别人和识别时间。

## 预期验证
- RED: DCC 批量识别单测在旧实现下失败，暴露 workerCount 未持久化、失败记录不参与跳过、导出接口缺失。
- GREEN: DCC 批量识别、识别服务、Controller 与 schema 单测通过。
- GREEN: 前端类型检查或目标测试通过。

## 当前状态
- in_progress

## 当前状态
- 状态：已完成
- 完成时间：2026-07-01 21:48

## 最终验证
- GREEN: 后端 DCC 识别相关单测与 schema 目标测试通过，24 tests passed。
- GREEN: 前端 DCC 受控浏览批量识别静态契约测试通过。
- GREEN: task-closeout-cleanup preview 通过，无需清理临时产物。

## 完成结果
- 批量识别任务支持 workerCount 快照，页面默认 5。
- 父目录识别包含子目录候选并在任务内去重。
- 识别记录账本支持 SUCCESS/FAILED 去重跳过、batch_task_id 关联和 Excel 导出。
- 前端新增并发 Codex 数量输入、导出识别记录入口和父文件夹识别文案。
