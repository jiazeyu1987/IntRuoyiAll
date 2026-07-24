# 任务：DCC 批量识别 5 个 Codex 并发

## 任务目标
- 支持 DCC 受控浏览批量识别在同一个任务内使用可配置 worker 并发执行。
- 测试服目标并发数为 5，同时保留文件级 claim，避免多个 Codex 识别同一个文件。
- 默认不覆盖已有成功识别账本；用户勾选覆盖时才重新识别。
- 批量识别进度弹窗不提供手动停止，任务持续到当前父文件夹及子文件夹全部识别结束；成功/失败卡片可点击查看对应记录，且成功 + 失败 = 总数。

## 里程碑
- [completed] 建立任务文档、经验门禁与 BDD/TDD 基线。
- [completed] 增加后端 worker-count 配置与并发执行测试。
- [completed] 实现批量识别任务内并发 worker，并保持进度统计线程安全。
- [completed] 补发布配置，使测试服可设置 5 worker。
- [completed] 运行验证并记录结果。
- [completed] 调整进度统计与前端交互：已有成功/失败账本计入对应数量，前端移除停止按钮并支持成功/失败记录钻取。

## 经验门禁
- PowerShell/UTF-8：已读取 `docs/powershell-memory.md`，PowerShell 读写中文必须显式 UTF-8。
- 服务器/发布：已读取 `docs/server-access.md` 与 `docs/release-backup-restore.md`；测试服写入、重启或发布需明确目标主机、目录、容器和授权范围。
- 后端交付：按 backend-api-delivery 执行，需记录 API/服务行为、配置、BDD、RED/GREEN。
- 数据库交付：本任务复用既有 claim/账本表，不新增 schema；如发现 schema 缺失则阻塞。

## 设计约束检查
- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，通过正式 worker-count 配置、任务内并发执行、共享识别账本状态复用和前端记录筛选解决。
- 是否存在临时补丁或绕过：否。

## 预期验证
- RED：新增/调整单测证明 worker-count 大于 1 时不再串行处理候选文件。
- GREEN：DCC 批量识别相关单测通过。
- REGRESSION：发布配置测试通过，确认测试服 runtime 可配置 worker-count。
- GREEN：前端静态契约与类型检查通过，确认无手动停止入口、成功/失败可钻取并带任务筛选。

## 当前状态
- completed

