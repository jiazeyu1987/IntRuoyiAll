# 20260904 Commit Frontend Backend Code

## Task Goal

提交并推送当前 `int_main` 分支中的前后端代码改动，保持只提交授权范围内的项目改动，并记录验证与推送证据。

## Milestones

- [x] 读取 Git、PowerShell、编码和任务收尾规则。
- [x] 盘点当前分支、remote 和工作区改动。
- [x] 暂存前后端代码及必要项目文档，复核 staged 清单。
- [x] 运行提交前校验和大文件/敏感输出边界检查。
- [x] 创建 Git 提交并推送到 `origin/int_main`。
- [x] 记录推送后状态和收尾证据。

## Expected Verification

- `git diff --cached --check` 通过。
- 提交前 staged 清单不包含运行日志、PID、构建产物或明显敏感文件。
- `git push origin int_main` 成功。
- `git status --short --branch` 显示不再 ahead。

## Current Status

completed

前后端代码提交与推送已完成；cleanup preview/apply 已通过且无删除项。仍保留未提交残留：`AGENTS.md`、`e2e_test/registration/reminder/registration-certificate-reminder-config-e2e-acceptance.md`、`e2e_binding_snapshot.txt`、`e2e_project_options.txt`、`e2e_snapshot.txt`；这些未纳入本次提交推送范围。

## Design Constraint Check

- 不执行 force push、reset、checkout 丢弃、rebase 或历史重写。
- 不停止/重启服务，不操作数据库，不执行远程服务器发布。
- 仅按本次授权提交前后端代码及必要项目文档；发现无法归属或敏感文件时阻塞。
