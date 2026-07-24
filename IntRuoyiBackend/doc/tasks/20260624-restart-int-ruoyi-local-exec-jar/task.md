# 任务：修复本机重启脚本启动包

## 任务目标

修复 `script/deploy/restart-int-ruoyi-local.ps1` 的本机后端启动逻辑，使其复制并启动可执行的 `yudao-server-exec.jar`，而不是普通的 `yudao-server.jar`。该修复用于支撑本机真实 E2E，不改变业务 API。

## 里程碑

- [x] M1：定位本机后端重启失败根因。
- [x] M2：修正脚本为可执行 jar，并验证脚本契约测试通过。
- [x] M3：重启本机后端并确认 `http://127.0.0.1:48081/actuator/health` 为 `UP`。

## 预期验证

- `python -X utf8 -m pytest script/tests/test_restart_int_ruoyi_local_schema.py -k executable_backend_jar -q`
- `curl.exe --fail --silent --show-error --max-time 10 http://127.0.0.1:48081/actuator/health`

## 当前状态

已完成。

## Current Status

completed

## 经验门禁

- `docs/server-access.md`：本机重启和健康检查先看本机运行控制脚本，避免手工改写远端环境。
- `docs/release-backup-restore.md`：涉及重启前应先确认不会触发远端发布流程。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 本机后端重启脚本使用可执行包 -> Given 本机需要重新启动 48081 后端 / When 执行 restart-int-ruoyi-local.ps1 / Then 脚本应复制并启动 yudao-server-exec.jar，健康检查返回 UP。`
