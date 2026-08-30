# Verification Report

## Summary

- 当前代码基线已提交：`a15678c63 chore: save current IntRuoyi changes`。
- 本地 `int_main` 在基线提交后领先 `origin/int_main` 11 个提交。
- 远端 fetch 成功，`git rev-list --left-right --count HEAD...origin/int_main` 在提交前为 `10 0`。
- branch runtime port guard 已通过，确认 `int_main/int_main` 使用前端 `8081`、后端 `48081`。
- staged `git diff --check` 首次发现两个新增 TXT 资源文档行尾空格；清理后复跑通过。
- 高置信密钥格式扫描结果为无命中。
- GitHub 待推送历史对象扫描未发现超过 100 MB 的 blob。

## Commands

- `git fetch origin int_main` -> PASS
- `git rev-list --left-right --count HEAD...origin/int_main` -> PASS, `10 0`
- `git diff --check` -> PASS, 仅 LF/CRLF 提示
- `scripts\preflight\branch-runtime-port-guard.ps1` -> PASS
- 高置信密钥格式扫描 -> PASS, `NO_HIGH_CONFIDENCE_SECRET_MATCHES`
- GitHub 待推送对象大小扫描 -> PASS, 最大约 220 KB
- `git diff --cached --check` -> FAIL, 新增 TXT 资源文档行尾空格
- 行尾空格清理后 `git diff --cached --check` -> PASS
- `git commit -m "chore: save current IntRuoyi changes"` -> PASS, `a15678c63`
- `task-closeout-cleanup --mode preview` -> PASS, delete/blocked/warnings 均为 `<none>`
- `task-closeout-cleanup --mode apply` -> PASS, deleted_paths 为 `<none>`
- 残余文档提交 -> PASS, `228c14a81 docs: save frontend approval route title gate`

## Remaining Verification

- 本任务记录提交
- `git push origin int_main`
- 推送后 `git status --short --branch`
