# 20260803 切换本机 int_main 后端运行态

## Task Goal

将本机 `48081` 后端运行态切换为当前 `int_main` 提交 `6f5f52814547146d9c90cd70f34e8a274751ed32` 构建出的最新代码，修复运行 jar 落后导致 `/dcc/controlled-files/{id}/controlled-print/records` 返回“请求地址不存在”的问题。

## Milestones

- [x] 建立任务记录并核对本机运行态门禁
- [ ] 从干净 detached worktree 构建当前 `int_main` 后端 jar
- [ ] 停止确认归属的旧 `48081` 后端进程并启动新 jar
- [ ] 验证 health 与目标 DCC 受控打印记录接口
- [ ] 记录验证结果和最终状态

## Expected Verification

- `git worktree add --detach` 使用当前 `int_main` HEAD 的干净快照，不从脏主工作区直接打包。
- Maven package 成功生成 `yudao-server-exec.jar`。
- 新运行 jar 位于 `E:\IntRuoyi\output\runtime\int_main\`，且包含 `controlled-print/records` Controller 字符串。
- `http://127.0.0.1:48081/actuator/health` 返回 `UP`。
- 登录态只读请求 `/admin-api/dcc/controlled-files/2054545668044052098/controlled-print/records` 不再返回“请求地址不存在”。

## Applied Experience Gates

- `docs/local-runtime.md#2026-07-24-隔离构建-jar-加载门禁`：主工作区存在并发脏改动时，不得从脏目录重新打包；必须使用干净构建产物并记录 PID、jar、health 和登录态接口验证。
- `docs/local-runtime.md#2026-07-27-本地后端运行-jar-不可变门禁`：长期运行后端必须使用 `output\runtime\int_main` 下的稳定 jar 副本，不能直接运行 Maven `target` jar。
- `docs/worktree-restrictions.md`：临时构建 worktree 必须位于 `D:\IntRuoyiWorktree\` 下，不占用 `48081` 以外的附加运行端口。
- `docs/experience-index.md`：DCC 受控打印、`controlled-print/records` 与 `viewer=1` 预览问题归属到 DCC 受控打印门禁。

## Design Constraint Check

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，本任务切换正式 `int_main` 构建运行态，不修改接口契约。
- `是否存在临时补丁或绕过`：否。

## Current Status

in_progress

