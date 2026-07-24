# 任务：修复运行控制台空工作目录启动失败

## 任务目标

修复运行控制台执行本机 PowerShell 命令时报错 `Cannot run program "powershell.exe" (in directory ""): CreateProcess error=123` 的问题。运行控制台在配置项 `repoRoot` 为空或空白时必须直接失败并提示缺少正式前置配置，不得把空字符串传给 `ProcessBuilder.directory`。

## 前置任务状态

- 已检查后端最新任务 `20260608-backup-incremental-manifest-short-loop`，其状态为 `blocked`，阻塞原因为测试服真实 B1-B5 连续备份与 B3/B4/B5 恢复闭环证据缺失；该阻塞不影响本次本机命令启动缺陷修复。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；缺少 `repoRoot` 时直接 fail-fast。
- `是否从根因和长期维护角度解决`：是；在命令执行器进入 `ProcessBuilder` 前统一校验工作目录。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- BDD: 空 repoRoot 阻止命令启动 -> Given 运行控制台 `repoRoot` 配置为空 / When 执行本机 PowerShell 状态命令 / Then 后端返回明确的 `repoRoot missing` 错误，且不触发 Windows `CreateProcess error=123`。
- BDD: 有效 repoRoot 正常执行命令 -> Given 运行控制台 `repoRoot` 指向存在目录 / When 执行本机 PowerShell 状态命令 / Then 命令在该目录下启动并返回输出。

## 里程碑

- [x] M1：定位命令执行器和当前任务状态。
- [x] M2：添加空 `repoRoot` 的失败回归测试。
- [x] M3：实现 fail-fast 工作目录校验。
- [x] M4：运行目标测试和相关回归验证。
- [x] M5：记录证据、执行收尾预览并提交本任务改动。

## 预期验证

- `mvn -pl yudao-module-infra "-Dtest=RuntimeControlCommandExecutorImplTest" test`
- `mvn -pl yudao-module-infra "-Dtest=RuntimeControlCommandExecutorImplTest,RuntimeControlServiceImplTest" test`

## 当前状态

completed: 命令执行器已在启动进程前校验 `repoRoot`，空值、非法路径和不存在目录均 fail-fast；目标测试、相关回归、bug 证据校验和收尾预览均通过。
