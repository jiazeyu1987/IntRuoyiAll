# 验证报告：批次执行已发布工艺路线快照运行态更新

## 已验证实现

- `openOrCreate` 查询路线的 ACTIVE 版本，在插入批次前持久化 `routeVersionId`、`routeVersionNo` 和 `routeSnapshotJson`。
- 批次任务构建接收批次对象，并从其冻结路线快照解析配置；不再直接读取当前草稿配置。
- 质量拒收后的重执行同样重新选择当时 ACTIVE 版本并冻结快照。

## 执行结果

- PASS: `node src\test\js\edhr-route-form-slot-frozen-runtime-static.spec.cjs`
- PASS: `mvn.cmd '-Dtest=MesProEdhrBatchExecutionServiceTest#openOrCreate_usesFrozenRouteVersionFormBindingsInsteadOfCurrentDraft,MesProEdhrBatchExecutionRouteVersionFreezeTest' surefire:test`
- Maven 结果：2 tests run，0 failures，0 errors，0 skipped。

## 运行态结论

当前本地 `48081` 的 Java PID `39264` 启动于 `2026-07-24 14:28:55`，早于包含本修复的源码时间；其命令行不可读取。当前后端工作区还包含大量不属于本任务的未提交 eDHR 改动，因此不得从该工作区重新打包或重启该进程。运行态更新和真实路径验证保持阻塞，等待可确认的隔离构建输入与服务归属。

## Playwright E2E 路径

- PASS（只读）：使用本机入口登录后，依次操作 `MES 系统 -> eDHR批记录 -> 批次执行 -> 打开/创建`。
- PASS（页面契约）：创建对话框展示生产工单、工艺路线、批次号和备注字段；工艺路线在选择工单前保持禁用，符合页面流程。
- 未执行提交：已取消对话框并关闭浏览器，未创建任何批次。

## E2E 阻塞

默认身份下页面存在大量既有待办和业务批次，无法确认其为可写测试租户；且运行中的后端 Jar 未包含已验证的冻结快照修复。缺少可写测试租户、任务自有工单、清理方案和已更新运行态时，提交创建请求会污染未知数据且无法验证目标逻辑，因此按 E2E 门禁停止。

## 本地重启门禁阻塞

`docs/experience-index.md` 要求 PowerShell 命令编排和本地重启先读取 `E:\IntRuoyi\docs\powershell-memory.md`。该文件不存在；这属于高风险运行态操作的必需经验门禁缺失，不能以其他文档替代。因此未执行构建、停止 PID、重启后端或写入型 E2E。

2026-07-24 更新：`docs/powershell-memory.md` 已恢复并完成读取，本阻塞解除。为避免部署主工作区并行脏改动，后端将从干净 worktree 构建，再加载到 `int_main` 运行端口。
