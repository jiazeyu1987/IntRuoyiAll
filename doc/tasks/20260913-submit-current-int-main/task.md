# 20260913 Submit Current Int Main

## Task Goal

按用户“提交推送”要求，将当前 `int_main` 工作区中已完成的前后端代码、测试和任务文档改动提交并推送到 `origin/int_main`。

## Milestones

- [x] 读取项目规则和提交收尾规则。
- [x] 核对当前分支、远端和工作区脏改动。
- [x] 排除不属于当前提交范围的资源大文件和临时 tsconfig。
- [ ] 运行提交前静态检查并核对 staged 文件清单。
- [ ] 提交并推送到 `origin/int_main`。
- [ ] 记录最终 commit hash、push 结果和剩余本地未跟踪文件。

## Expected Verification

- `git status --short --branch`
- `git diff --check`
- `git diff --cached --name-status`
- `git push origin int_main`
- 推送后再次执行 `git status --short --branch`

## Current Status

in_progress

正在按用户授权提交当前 `int_main` 工作区改动。本轮不执行 E2E；提交前只做静态检查和 Git 范围核对。

## Design Constraints Check

- 不引入 fallback、降级、吞异常、模拟成功或兼容补丁。
- 不执行 E2E、数据库写入、服务器启停或发布操作。
- 不提交 `resource/` 下 Office 资源大文件。
- 不提交 `IntRuoyiFronted/tsconfig.route-production-migration.tmp.json` 临时文件。
