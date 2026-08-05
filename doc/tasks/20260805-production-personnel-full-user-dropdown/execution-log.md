# Execution Log

## 2026-08-05

- User intent: 将生产组长新增人员中的正式工下拉改为全量系统用户。
- Scope: 后端正式工候选搜索与关联校验；前端远程搜索接口和交互保持不变。
- BDD: 全量用户姓名搜索 -> Given 当前生产组长负责部门之外存在姓名或用户名匹配的有效系统用户；When 组长在正式工姓名下拉输入关键字；Then 候选接口返回该用户且最多返回 20 条匹配结果。
- BDD: 跨部门正式工关联 -> Given 候选用户是有效系统用户但不属于当前组长负责部门；When 组长提交关联；Then 创建当前组长的正式工档案并保留重复关联与显示名唯一校验。
- BDD: 空关键字校验 -> Given 搜索关键字为空白；When 请求正式工候选；Then 返回空候选，不执行全量无条件扫描。
- BDD: 权限校验 -> Given 登录用户缺少维护权限；When 调用正式工候选或关联接口；Then 由现有 Controller 权限注解拒绝请求。
- BDD: 无效系统用户失败 -> Given 提交不存在或不可用的系统用户编号；When 关联正式工；Then 沿用系统用户校验错误，不创建人员档案且不返回默认成功。
- BDD: 重复关联失败 -> Given 当前组长已关联同一系统用户；When 再次提交关联；Then 在数据库写入前返回正式工重复关联业务错误。

## Command Intent

- 读取现有前后端调用链和系统用户 API，确认最小正式数据源变更。
- 在修改生产代码前新增并运行目标失败测试。
- 实现后运行目标测试、相邻 Controller 回归、证据校验和差异检查。

## RED / GREEN Evidence

- RED: `mvn -pl yudao-module-system,yudao-module-mes -am "-Dtest=AdminUserApiImplPostIdsTest,MesTeamLeaderRuntimeConfigServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，`AdminUserApiImpl` 缺少 `getUserListByNickname(String)`，符合全量用户正式 API 尚未实现的预期。
- GREEN attempt: 合并运行 system + MES Reactor 超时；任务自有 Maven PID `31152` 的 `jcmd Thread.print` 显示卡在 `JavacFileManager.close -> ZipFileSystem.close -> WindowsPath.toRealPath`，停止的仅为该任务 PID `31152` 及父 `cmd` PID `19692`，未停止其它 Maven/Java 进程。
- GREEN: `mvn -pl yudao-module-system -am "-Dtest=AdminUserApiImplPostIdsTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 2。
- REGRESSION discovery: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderRuntimeConfigServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL before MES Surefire because BPM 测试手写 `RecordingAdminUserApi` 未实现新增正式接口；已按编译契约补齐显式 `UnsupportedOperationException`，未增加默认空列表 fallback。

## Baseline

- Baseline commit: `3db8a7030 chore: preserve dirty worktree baseline`.
- Baseline scope: 39 个既有前端、静态测试和并行任务文档文件；本任务目录未进入基线提交。
- Baseline check: `git show --name-status --oneline -1` 已核对；提交后并行任务继续产生新的非本任务改动，本任务后续只选择性暂存后端代码、后端测试和本任务记录。
- Baseline diff note: 既有生产人员重复错误任务的 3 个 Markdown 文件存在 EOF 空行告警；为保留用户原始脏改动，基线提交未改写这些非本任务文件。
