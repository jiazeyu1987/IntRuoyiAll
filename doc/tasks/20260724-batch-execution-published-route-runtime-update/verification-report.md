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

已解除阻塞并加载新后端。旧 `int_main` 后端 PID `57944` 可确认为 `E:\IntRuoyi\IntRuoyiBackend` 的 `48081` 进程；已停止后用隔离 worktree 构建产物覆盖目标 Jar 并启动新 PID `47120`。当前 `E:\IntRuoyi\IntRuoyiBackend\yudao-server\target\yudao-server-exec.jar` SHA256 为 `10C7B39A5B3920FEB3E8C71C3719AAC06840C808E729A1E19867A26F9B725C44`，与隔离构建 Jar 一致；`http://127.0.0.1:48081/actuator/health` 返回 `{"status":"UP"}`。

## Playwright E2E 路径

- PASS：`node tests\e2e\edhr-batch-execution-real-flow.e2e.js`。
- 路径：本机入口 `http://localhost:8081`，测试租户 `测试租户/aoteman`，真实页面选择工单 `925555 / TESTERPA9ED2D417434`、路线 `922186 / E2E-OSF-20260721042549`，提交创建批次 `BRS20260724195134`，随后打开 eDHR 执行页。
- 测试脚本同步修复：租户选择改为点击真实下拉项；路线选择改为按页面可见 ID/编码/名称显式等待；最终执行页断言改为 URL + `/task/open` 成功，避免被页面文案漂移误判。

## 数据库核验

- PASS：批次 `900000000787` 持久化 `route_id=922186`、`route_version_id=239`、`route_version_no=V2`，`route_snapshot_json` 长度 `40670`，`configSnapshots.batchUseConfigs` 数量 `2`。
- PASS：任务表 `mes_pro_edhr_batch_execution_task` 对该批次生成 `8` 个任务，其中传统批记录任务 `4` 个，已打开关联执行任务 `4` 个，`blocked_count=0`。
- PASS：路线 `922186` 当前 ACTIVE 版本仍为 `239 / V2`，同时存在 `open_draft_count=1`，证明创建读取并冻结 ACTIVE 发布版本而非草稿。
- PASS：路线 `922185` 的一次真实创建尝试返回正式业务校验 `1040750243`（未确认填写规则），不再返回原始“缺少工艺流程批记录配置流程配置或默认批记录”错误，说明本修复已越过原缺陷点。

## 本地重启门禁阻塞

`docs/experience-index.md` 要求 PowerShell 命令编排和本地重启先读取 `E:\IntRuoyi\docs\powershell-memory.md`。该文件不存在；这属于高风险运行态操作的必需经验门禁缺失，不能以其他文档替代。因此未执行构建、停止 PID、重启后端或写入型 E2E。

2026-07-24 更新：`docs/powershell-memory.md` 已恢复并完成读取，本阻塞解除。为避免部署主工作区并行脏改动，后端从干净 worktree 构建，并已加载到 `int_main` 运行端口完成真实 E2E。

## 2026-07-25 E2E 复跑结果

- BLOCKED：按用户要求复跑 `node tests\e2e\edhr-batch-execution-real-flow.e2e.js`，脚本已使用显式 `EDHR_BATCH_E2E_TASK_ID` 和 `EDHR_BATCH_E2E_EVIDENCE_FILE`，但测试租户 `测试租户/aoteman` 登录失败。
- 失败原因：脱敏登录诊断显示 `/system/auth/login` 返回 `code=1002000000`、`msg=登录失败，账号密码不正确`；当前本机 `.env` 默认身份为受保护的 `芋道源码/admin`，不能作为写入型 E2E 替代账号。
- 影响：本轮未进入批次创建页提交动作，未生成新的批次执行数据；因此没有新的 DB 冻结快照核验结果。2026-07-24 已通过的批次 `900000000787 / BRS20260724195134` 证据仍保留在本报告上方。
