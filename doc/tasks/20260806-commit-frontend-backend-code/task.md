# 提交前后端代码

## Task Goal

核对并提交 `E:\IntRuoyi` 当前工作区中的前后端代码及其关联测试、SQL 和任务证据，完成 `int_main` 与 `origin/int_main` 的安全同步和推送。

## Milestones

- [x] M1：确认仓库根目录、当前分支、远端和工作区状态。
- [x] M2：确认现有变更的任务归属、验证状态、敏感文件和大文件风险。
- [ ] M3：按项目门禁完成暂存、提交、远端同步和推送。
- [ ] M4：复扫残余改动并完成任务收尾记录。

## Expected Verification

- `git diff --check`
- `git diff --cached --check`
- `scripts\preflight\branch-runtime-port-guard.ps1`
- 关联任务已记录的目标测试与验证证据复核
- 待推送历史大文件扫描
- `git push origin int_main`
- `git status --short --branch` 不再显示本地领先 `origin/int_main`

## Applicable Experience Gates

- 提交前必须检查当前分支、远端、工作区和 staged 文件清单。
- 当前脏工作区必须作为独立基线保存，不得丢弃、覆盖或静默忽略已有改动。
- 提交后必须复扫延迟保存或并行产生的残余改动，不得直接推送。
- 推送前必须运行分支运行端口守卫，并扫描待推送历史中的大文件。
- 分支当前落后远端时，必须先保全本地改动，再安全同步远端；禁止强推或重写历史。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；按统一仓库提交、验证、同步和推送门禁处理。
- 是否存在临时补丁或绕过：否。

## Current Status

in_progress

用户已明确授权一次性恢复非空锁。锁文件已按原 SHA-256 备份到 `.git/index.lock.backup-20260806-090540` 后删除，Git 状态和 staged 清单恢复可读。当前 `int_main` 在等待期间变为落后 `origin/int_main` 16 个提交，正在继续验证、基线提交和远端同步。

## Blockers

- `E:\IntRuoyi\.git\index.lock` 非空且无法安全判定为冗余锁文件。
- 只读检查曾发现并发 `git merge --no-ff --no-edit origin/int_main`，等待后该进程已自然退出，但锁文件未消失。
- `.git/index`：`4,011,941` 字节，SHA-256 `4DE525FE6D9BE801158E93CB422498A6D2A1BD075E7BDFAB4E34F7BB55593AEC`。
- `.git/index.lock`：`1,441,792` 字节，SHA-256 `9B97BC1366A299084C544168EFD3C81C2F5099D15FE7913BB356760BA073D869`。
- 使用锁文件执行只读 `git ls-files --stage` 时 Git 退出码为 `-1073741819`，不能证明锁中暂存状态可安全丢弃。

## Authorized Lock Recovery

- 用户于 2026-08-06 明确授权“备份该锁文件，再删除原锁并继续提交、合并和推送”。
- 删除前再次确认无活动 Git / Git LFS 进程。
- 备份路径：`.git/index.lock.backup-20260806-090540`。
- 备份长度：`1,441,792` 字节。
- 原锁与备份 SHA-256：`9B97BC1366A299084C544168EFD3C81C2F5099D15FE7913BB356760BA073D869`。
- 备份哈希一致后仅删除精确路径 `.git/index.lock`；未替换 `.git/index`，未使用备用索引执行提交。

## Verification Evidence

- `scripts\preflight\branch-runtime-port-guard.ps1` -> PASS，`int_main/int_main` 前端 `8081`、后端 `48081`。
- `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest,MesQaPqcSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，25 tests / 0 failures / 0 errors。
- 前端静态合同批量运行 -> PASS，覆盖 P0 生产执行、PQC 生产来源上下文、生产组长活跃订单池、生产组长 Tab、RRM preflight/local wrapper、统一列表多筛选和真实 E2E 脚本语法。
- `pnpm ts:check` -> PASS。
- `git diff --check` 与 `git diff --cached --check` -> PASS。
- 敏感信息复核：强 token / private key / bearer 模式未命中；RRM 本机包装脚本中的 SQL `password = '$escapedHash'` 为 here-string 变量占位，真实密码由进程环境变量提供，未发现明文密码值。
