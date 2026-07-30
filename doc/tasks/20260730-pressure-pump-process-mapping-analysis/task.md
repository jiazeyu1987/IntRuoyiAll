# 20260730 pressure pump process mapping analysis

## Task Goal

读取 `C:\Users\BJB110\Desktop\文档\压力泵工序.xlsx`，确认压力泵 MES 工序、对外批记录工序、设备、工序编码和产能之间的业务含义，并把本次线程新增口径继续写入现有项目构想与证据文档。

## Milestones

- [x] 读取工作表、表头、合并单元格和有效数据行
- [x] 识别 MES 工序与批记录工序的映射关系
- [x] 识别设备编码、设备名称、工序编码和产能的可选关系
- [x] 识别不能直接自动导入的数据异常
- [x] 更新项目构想与证据清单
- [x] 执行结构、UTF-8 和差异验证

## Expected Verification

- 文档明确 `工序名称` 是一线展示的 MES 工序名称。
- 文档明确 `批记录工序名称` 是最终对外批记录使用的名称。
- 文档明确 MES 工序与批记录工序可以一对一、多对一或不形成批记录。
- 文档明确设备编码代表实际设备；设备名称可为空，工序编码和产能也可为空。
- 文档记录 `B09032/G01160` 为两台设备的用户确认示例。
- 文档列出 Excel 中不能直接自动导入的异常和待确认项。
- 使用项目构想文档校验脚本验证结构。
- 使用 UTF-8 方式重新读取任务文档和更新后的项目文档。

## Current Status

completed

## Closeout Notes

- `task-closeout-cleanup` preview/apply 均通过。
- 保留 `task.md`、`execution-log.md`、`verification-report.md`，删除项为空，阻塞项为空。
- 文档实现提交：`28bdfb76 docs: record pressure pump process mapping`。
- 最终收尾记录提交与远端推送在本记录更新后执行。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；把 MES 展示名称、对外批记录名称和设备主数据拆分成正式映射，不依赖名称猜测。
- `是否存在临时补丁或绕过`：否。

## Experience Gate

- 已读取 `docs/powershell-encoding.md`：中文 Markdown 通过 `apply_patch` 写入，并以 UTF-8 重新读取验证。
- 已读取 `docs/task-closeout-rules.md`：本次文档更新需要任务记录、执行日志和验证报告。
- 已读取 `docs/experience-index.md`：命中 `AGENTS.md` 中批记录表单术语边界；本次只定义 MES 工序名称到正式批记录工序名称的业务映射，不用表单槽位或工序开始配置替代正式批记录绑定。
- 已读取 `project-inception-docs` 技能：新增内容按已确认事实、假设、待确认项和阻塞项分类，不把 Excel 异常猜成正式规则。
