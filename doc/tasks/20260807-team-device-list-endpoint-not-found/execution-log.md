# 执行日志

## 用户意图与范围

- 用户报告：`请求地址不存在:admin-api/mes/pro/process-pool/team-leader/team-device/list`。
- 目标是恢复生产组长设备列表正式接口，不通过新增兼容路由、前端返回空列表或隐藏错误绕过。
- 用户随后明确要求“重启成最新的后端”；执行范围变更为构建当前后端工作区的完整最新源码快照，并重启本机 `int_main:48081`，不再采用最小运行 Jar 热补丁。

## BDD

- BDD: 生产组长工作台可加载设备列表 -> Given 当前前端调用正式 `team-device/list` 且后端源码已有对应 GET 映射，When 登录用户打开生产组长工作台并加载设备选项，Then `48081` 运行态返回业务码 `0`，不出现“请求地址不存在”。
- BDD: 前后端路由合同保持唯一 -> Given 设备列表只有一个正式路径，When 执行静态与 Controller 合同验证，Then 前端请求与后端 GET 映射完全一致且不新增别名/fallback。

## 初步证据

- SOURCE: 前端 `getTeamDeviceList` 请求 `/mes/pro/process-pool/team-leader/team-device/list`。
- SOURCE: 后端 `MesProcessPoolTeamLeaderController#getTeamDeviceList` 已声明 `@GetMapping("/team-device/list")`。
- SOURCE: 既有 `MesProcessPoolTeamLeaderControllerTest` 已覆盖相邻 team-device create/status 映射，但设备 list 映射需要补充明确合同断言。
- RUNTIME: `48081` 监听 PID `59012`，命令行归属 `E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260807-upload-taxonomy-permission.jar`，health 为 `UP`；该 Jar 生成时间为 `2026-08-07 14:36:26`。
- TIMELINE: 后端 Controller 源文件最后修改时间为 `2026-08-07 15:52:31`，前端 API 文件最后修改时间为 `15:53:19`，均晚于当前运行 Jar。
- RED: `node doc/tasks/20260807-team-device-list-endpoint-not-found/team-device-route-static.spec.cjs output/runtime/20260807-team-device-list-endpoint-not-found/old/BOOT-INF/lib/yudao-module-mes-2026.04-SNAPSHOT.jar` -> FAIL，预期原因：前后端源码合同存在，但运行 Jar 内 `MesProcessPoolTeamLeaderController` 字节码不包含 `/team-device/list`。
- ROOT CAUSE: 当前 `48081` 运行 Jar 早于设备列表 Controller、VO 和查询服务源码；错误属于运行版本漂移，不是前端路径拼写错误。
- RUNTIME RECHECK: `48081` 监听 PID `40088`，运行 `E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260807-erp-connection-switch.jar`，health=`UP`；只读解析内嵌 `yudao-module-mes-2026.04-SNAPSHOT.jar` 确认 nested entry 为 `STORED`，但 Controller 字节码不含 `/team-device/list` 或 `getTeamDeviceList`，同时缺少 `MesTeamDeviceRespVO` 与 runtime-config `listDevices`。

## 里程碑状态

- M1 completed：前后端源码路径一致，根因优先指向本机运行 Jar 未刷新。
- M2 completed：已记录 `48081` PID、Jar 归属、源码/运行产物时间线和字节码 RED。
- M3 completed：任务专属源码快照完成目标测试、完整构建、哈希与内嵌 MES 字节码验证；新 Jar 通过门禁后已替换 `48081` 运行态。

## 阻塞项

- 无当前用户授权阻塞。Git/worktree 操作仍未获授权且不执行；当前并发源码只复制到任务专属构建快照，不修改、不暂存、不提交。

## 运行交付策略

- TARGET: 本机 `int_main` 后端 `http://127.0.0.1:48081`。
- BUILD: 当前 `IntRuoyiBackend` 源码的任务专属快照；排除 `.git`、既有 `target*` 和运行日志，避免复用共享构建产物。
- GATE: 完整 Maven package 成功；新 Jar SHA-256 与部署副本一致；内嵌 MES 模块通过 `/team-device/list` 静态字节码合同。
- RESTART: 新 Jar 门禁通过后，重新确认 `48081` PID/命令行归属，只停止该 PID并以原本地 profile/数据库/Redis参数启动新 Jar。
- ROLLBACK: 保留旧 Jar；若新进程健康或登录态接口验证失败，停止任务启动的新 PID并使用原参数恢复旧 Jar。

## 构建与验证证据

- SNAPSHOT: `robocopy` 将当前 `IntRuoyiBackend` 复制到任务专属 `output/runtime/20260807-team-device-list-endpoint-not-found/snapshot-backend`，排除 `.git`、`.idea`、`target*`、根运行状态目录、日志和 PID；复制退出码 `1`、只读一致性检查退出码 `0`，Controller SHA-256 一致，共 `14333` 个文件。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolTeamLeaderControllerTest,MesTeamLeaderRuntimeConfigServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Controller `17` 项、runtime-config service `18` 项，共 `35` 项，失败/错误/跳过均为 `0`。
- GREEN: `mvn -pl yudao-server -am "-Dmaven.test.skip=true" package` -> PASS，30 个 reactor 模块全部 SUCCESS，生成 `yudao-server-exec.jar`。
- ARTIFACT: 构建 Jar 长度 `502125898` 字节，SHA-256=`8F8C8443C1F2B66613899C79FED5E97631DE7A6848A147EA5830445121982691`；部署副本哈希完全一致。
- GREEN: `node doc/tasks/20260807-team-device-list-endpoint-not-found/team-device-route-static.spec.cjs <new nested MES jar>` -> PASS；内嵌 MES 模块包含 `/team-device/list`、`MesTeamDeviceRespVO`、Controller 和 `MesTeamLeaderRuntimeConfigService`。
- RESTART: 新 Jar 门禁通过后，确认旧 `48081` PID `61676` 归属 `backend-runtime-control-20260807-active-order-abnormal-fix.jar` 且 health=`UP`；停止该 PID。停止命令的即时端口检查因监听释放竞态返回非零，随后独立复查确认端口无监听且健康地址拒绝连接，再启动新 PID `2396`，未停止其它 Java 进程。
- GREEN: `GET /actuator/health` -> `UP`；`48081` 监听 PID `2396`，命令行加载 `backend-latest-20260807-1919-team-device-list.jar`，运行 Jar SHA-256 与构建产物一致。
- GREEN: 使用本机默认身份标签 `芋道源码/admin` 登录（不记录密码/token），登录业务码 `0`；`GET /admin-api/mes/pro/process-pool/team-leader/team-device/list?enabled=true` -> HTTP `200`、业务码 `0`、数组结果、当前 `0` 条设备。
- GREEN: Playwright 真实页面 `http://localhost:8081/mes/pro/process-pool/team-leader` -> 标题“工序池班组长工作台”可见；“请求地址不存在”计数 `0`、“班组设备列表加载失败”计数 `0`、匹配控制台错误 `0`。

## 里程碑完成更新

- M3 completed：隔离构建、产物门禁、`48081` 重启和健康验证已完成。
- M4 verification completed：登录态接口和真实页面回归通过；任务状态进入 `ready_for_closeout`，仅剩任务专属临时产物清理。

## 收尾证据

- EVIDENCE: bug-regression 与 CI/CD 证据校验器在临时证据清理前均通过；`git diff --check -- doc/tasks/20260807-team-device-list-endpoint-not-found` 通过。
- EXPERIENCE: `project-experience-consolidation` 核对后确认 `docs/local-runtime.md#2026-07-24-隔离构建-Jar-加载门禁` 已完整覆盖本次“源码已有路由但运行 Jar 漂移”的复用经验，因此不新建或重复修改长期经验文档。
- CLEANUP PREVIEW: 仅保留 `task.md`、`execution-log.md`、`verification-report.md`，删除任务专属临时证据、静态合同脚本和 `output/runtime/20260807-team-device-list-endpoint-not-found`。
- CLEANUP APPLY: 初次清理因 Windows 文件枚举在已消失的快照文件上返回 `FileNotFoundError`；确认目标绝对路径严格位于任务专属输出目录后，使用空目录 `robocopy /MIR` 清空残留（退出码 `2`、剩余项 `0`），随后 closeout apply 成功删除两个空目录。未触及 `output/runtime/int_main` 的运行 Jar。
- M4 completed：登录态 API、真实页面、证据校验、经验归档核对和任务专属清理全部完成；核心任务记录保留。
