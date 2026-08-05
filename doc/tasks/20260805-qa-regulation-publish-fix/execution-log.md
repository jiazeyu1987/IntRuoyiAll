# Execution Log

## User Intent

- 用户要求“进行修复”，针对 `AC-M09 | QA | 维护检验规程` 当前不符合项，补齐正式维护、发布、不可变版本和发布失败校验链路。

## Baseline

- `git status --short --branch` 显示进入任务前已有大量前后端、测试和任务文档改动。
- `5486d9ba9`：Baseline commit，保存 71 个进入本任务前的既有改动。
- `fc5e98ffe`：Baseline commit，保存岗位矩阵分析残余文档更新。
- `515798d74`：Baseline commit，保存并发 AC 任务文档更新。
- 仍观察到 `doc/tasks/20260805-job-matrix-compliance/*` 被并发任务继续写入；本任务不触碰这些文件，提交时只选择性暂存 AC-M09 文件。

## BDD Scenarios

- BDD: 保存 QA 规程草稿 -> Given QA 用户填写产品、路线版本、工序、版本号、首检/巡检/末检规则和检验项目 When 调用保存草稿 Then 后端持久化 DRAFT 规程和 DRAFT 版本但不发布。
- BDD: 发布完整 QA 规程 -> Given 草稿包含首检、巡检、末检和完整检验项目 When 调用发布 Then 后端生成 PUBLISHED 版本、写入 `currentVersionId`、返回不可变发布版本。
- BDD: 缺少必要规则发布失败 -> Given 草稿缺少首检、巡检或末检规则 When 调用发布 Then 后端 fail-fast 返回业务错误且不生成 PUBLISHED 版本。
- BDD: 发布版本不可变 -> Given 规程已发布 When 尝试覆盖同一版本草稿或修改发布版本 Then 后端拒绝并保持原发布快照不变。
- BDD: 前端正式保存发布 -> Given QA 页面已选择 DCC 项目代码并填写完整规程 When 点击保存草稿或发布 Then 调用正式 API，失败时页面显示错误，成功时刷新后台状态。

## RED / GREEN Evidence

- pending。

## Verification Evidence

- pending。

## Blockers

- 当前共享工作区仍有并发文档写入，后续提交需选择性暂存本任务文件。
