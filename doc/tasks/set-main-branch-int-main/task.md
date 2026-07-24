# Task: 设置主分支为 int_main

## Task Goal

按用户要求将当前 `E:\IntRuoyi` 仓库主分支设置为 `int_main`，并在项目 `AGENTS.md` 中记录主分支约束，避免后续任务继续把 `main` 作为主分支。

## Milestones

- [x] 创建任务目录并记录初始分支状态
- [x] 将本地主分支从 `main` 改为 `int_main`
- [x] 更新 `AGENTS.md` 主分支规则
- [x] 验证当前分支、规则文本和 Git 状态
- [x] 收尾并记录最终验证结果

## Expected Verification

- `git branch --show-current` 输出 `int_main`。
- `AGENTS.md` 明确当前项目主分支为 `int_main`。
- `git status --short --branch` 显示当前分支为 `int_main`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，直接重命名本地分支并记录项目级规则。
- `是否存在临时补丁或绕过`：否。

## 经验门禁

- `docs/experience-index.md` 当前不存在；本任务只执行本地 Git 分支命名和规则文档更新，不执行真实 E2E、服务器、数据库、发布、备份、恢复或 worktree 创建/合并/清理。

## Current Status

completed

## Final Verification Result

PASS。当前仓库分支已从 `main` 设置为 `int_main`，`AGENTS.md` 已记录 `int_main` 为项目主分支，UTF-8 读取、规则校验和 task-closeout-cleanup preview/apply 均通过。实现提交为 `ac67dd89`。
