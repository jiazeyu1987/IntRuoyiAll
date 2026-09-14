# 20260728 Local Runtime Log Isolation

## Task Goal

- 将本地 SQL DEBUG 默认关闭，避免 MyBatis 海量 SQL 日志拖慢本地后端请求。
- 将后端本地日志文件默认改到工作区 runtime 独立目录，避免多个运行态抢写 `C:\Users\BJB110\logs\yudao-server.log`。

## Milestones

- [completed] 建立配置回归测试，先证明当前本地日志配置不满足要求。
- [completed] 修改本地后端日志级别和日志文件路径默认值。
- [completed] 运行定向验证并记录 RED/GREEN 证据。
- [completed] 收尾任务文档，记录风险、阻塞项和验证结果。

## Expected Verification

- `mvn -pl yudao-server -Dtest=LocalRuntimeLoggingConfigTest test` 先 RED 后 GREEN。
- 必要时补充静态配置检查，确认 `application-local.yaml` 不再默认启用 mapper DEBUG。

## Applicable Experience Gates

### 本地后端标准输出阻塞与日志目录门禁

- Trigger: 本地后端 health 为 `UP` 但 API 挂起、线程栈集中阻塞在 Logback `OutputStreamAppender`、或共享日志文件被 Java 进程持有。
- Preflight check: 长期运行后端必须将 stdout/stderr 和应用日志写到稳定 runtime 目录；不能只看 health。
- Blocker: 发现请求线程卡在 `OutputStreamAppender` 或日志写锁时，不得宣称运行态可用。
- Verification: 配置合同必须证明本地 SQL DEBUG 默认关闭，应用日志默认进入 `output/runtime/<profile>/logs`。
- Forbidden action: 禁止用 API-only 成功或 health `UP` 掩盖登录态/初始化接口卡顿。
- Evidence: `docs/local-runtime.md#2026-07-27-本地后端标准输出阻塞与日志目录门禁`。

## Current Status

ready_for_closeout

## Closeout Blocker

- cleanup preview/apply 已通过，无删除项。
- 提交/推送未执行：当前工作区在本任务开始前已有大量非本任务脏改动；按项目规则，提交前需要用户授权执行脏工作区基线提交，避免混入并行任务内容。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；通过默认配置减少本地日志放大和共享日志文件写锁。
- `是否存在临时补丁或绕过`：否。
