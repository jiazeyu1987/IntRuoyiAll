# DCC 最小主流程 M01 项目编码唯一性

## Goal

按最小主流程补齐 M01 的项目编码身份边界：同一租户内项目编码独立唯一，项目名称只作为展示字段；保留旧项目代码导入和空值兼容规则，先不扩展产品主数据迁移。

## Milestones

1. 建立 M01 Given/When/Then 与 RED 证据。
2. 将新建/编辑项目代码的判重从“项目名称+项目代码”改为“项目代码”。
3. 运行项目代码服务定向测试、DCC 相关静态合同和差异检查。
4. 记录旧导入行为、未完成 M02—M30 和收尾边界。

## Expected Verification

- 新增测试覆盖“不同项目名称使用相同非空项目编码时拒绝”。
- 原有同名同编码判重、项目代码规范化和导入测试继续通过。
- 不修改数据库数据、不执行 E2E、服务启停、远程操作或 Git 提交/推送。

## Current Status

in_progress

## BDD

### BDD-M01-project-code-unique

Given：同一租户已有项目名称“项目A”、项目编码“P001”。\
When：另一个项目名称“项目B”尝试创建项目编码“P001”。\
Then：服务端拒绝并返回 `PROJECT_CODE_DUPLICATE`；项目名称不参与编码身份唯一性。

## Design Constraints

- 只修改项目代码身份判重和对应测试，不重构产品、注册证、模板或目录。
- 空项目编码继续保留旧兼容行为；本切片只收紧非空编码。
- 不用前端校验、默认值、吞异常或数据库写入替代服务端判重。
- 不回滚工作区已有改动。

## Cleanup Keep

- doc/tasks/20260917-dcc-minimal-project-code-identity/task.md
- doc/tasks/20260917-dcc-minimal-project-code-identity/execution-log.md
- doc/tasks/20260917-dcc-minimal-project-code-identity/verification-report.md
