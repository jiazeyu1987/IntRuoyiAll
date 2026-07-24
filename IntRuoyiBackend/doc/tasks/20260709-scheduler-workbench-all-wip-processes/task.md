# 排产员工作台显示全部在排工序

## 任务目标
- 修复排产员工作台「工序列表」只显示单个当前工序的问题。
- 列表应展示所有在排工序，不按某一天或单一当前工序收窄。
- 工序编号、工序名称仍以排产工序快照为准，避免主数据缺失时显示“无工序编码/未命名工序”。

## 经验门禁
- PowerShell 命令执行前已按 UTF-8 读取 `docs/powershell-memory.md`，命令中显式设置输入/输出编码。
- 本任务为后端接口行为修复，使用 BDD + 严格 TDD：先补失败测试，再最小实现，再回归验证。
- 不操作服务器、不改真实数据库、不做 E2E 写入；仅本地代码与单元测试验证。
- 不引入 fallback、降级、吞异常或 mock 成功。

## 里程碑
1. 建立回归场景，证明多个在排工序必须同时出现在工作台列表。
2. 最小化修复后端统计逻辑，保留排产工序快照编码/名称。
3. 运行定向测试与相关回归测试。
4. 清理任务临时产物并提交当前任务改动。

## 期望验证
- RED：新增/调整的回归测试在旧逻辑下失败，表现为只返回当前工序而不是全部在排工序。
- GREEN：修复后目标测试通过。
- REGRESSION：排产员工作台 WIP 统计相关测试通过。

## 设计约束检查
- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，恢复“全部在排工序”统计语义，仅保留快照字段作为展示来源。
- 是否存在临时补丁或绕过：否。

## 当前状态
- 已完成：后端统计逻辑已修复，回归测试、证据校验和收尾预览均已通过。


## 验证结果
- RED：旧逻辑下 2 个回归测试失败，确认只返回当前工序。
- GREEN：修复后 2 个回归测试通过。
- REGRESSION：5 个排产员工作台 WIP 相关测试通过。


## Current Status
completed


## 收尾结果
- `validate_bug_regression.py` -> PASS。
- `validate_backend_api.py` -> PASS。
- `task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260709-scheduler-workbench-all-wip-processes --mode preview` -> PASS，预览仅建议清理临时 evidence 文件，无阻塞。
