# DCC E2E 测试身份与部门负责人配置

## Current Status
completed

### 2026-09-28 重试说明

前一会话已关闭，本轮按真实 `el-tree-select` 前景弹层规则恢复执行。第一次重试已通过归属部门选择并创建 Owner A，角色精简列表因异步加载尚未等待而中断；继续使用该已创建账号，不重复创建，不记录密码。

### 2026-09-28 最终结果

已通过真实 Playwright 页面复核现有 Owner A/B，不重建账号。两账号均保持最小合法角色集合“审批中心入口 + DCC Action View”；生产部负责人设置为 Owner A，质量部负责人设置为 Owner B。角色与部门负责人表单均重新打开确认，密码未写入任何任务文件。

## 任务目标

在测试租户“芋道源码”中，通过 `http://127.0.0.1:8062` 的真实前端页面创建两个最小权限普通测试账号，并将 DCC E2E 生产部、质量部负责人配置为这两个账号，用于后续 owner-change 与权限边界 E2E。

## 范围与约束

- 仅使用 Playwright 操作真实前端页面；允许监听页面自然触发的请求和读取页面 DOM。
- 禁止 `fetch`、API client、直接 HTTP 写请求、数据库写入、脚本注入业务写入和 Git 操作。
- 租户固定为“芋道源码”，管理员账号固定为 `admin`；密码不写入公开报告。
- 测试账号使用唯一前缀 `dccE2EOwnerA0928`、`dccE2EOwnerB0928`；仓库登录规则禁止在文档/任务目录记录密码，因此密码仅回传父线程，不落盘。
- 不在已有 DCC 业务文件上执行上传、审批、升版或作废动作。
- 不赋予超级管理员；角色仅限完成会签、电子签名和 DCC 相关页面动作所需的最小权限。

## BDD 验收

### BDD-01 创建测试账号

Given 管理员已登录“芋道源码”租户的系统管理用户页面
When 通过真实页面创建 Owner A 和 Owner B，并保存唯一用户名、显示名和强密码
Then 两个账号均创建成功、处于启用状态且不具备超级管理员角色

### BDD-02 配置最小角色

Given 两个普通测试账号已存在
When 通过真实页面配置审批、电子签名和 DCC 相关最小角色
Then 两个账号的角色绑定可在页面重新打开后确认，且未授予超级管理员

### BDD-03 配置部门负责人

Given DCC E2E 生产部和质量部已存在
When 通过部门管理页面分别将负责人设置为 Owner A 和 Owner B 并保存
Then 重新打开两个部门时，页面显示对应负责人、用户名/显示名和部门归属

### BDD-04 证据安全

Given 本任务会生成测试账号凭据
When 记录执行证据
Then 密码不写入仓库、任务文件或截图说明，只在父线程私下回传，不出现在普通报告中

## 里程碑

1. 建立任务文档并确认运行态。
2. 通过真实页面创建两个账号并记录凭据。
3. 通过真实页面配置最小角色并复核。
4. 通过真实页面配置两个部门负责人并复核。
5. 由独立只读 Playwright agent 复核结果，形成证据与最终 PASS/BLOCKED。

## 预期验证

- Playwright 真实页面操作结果、自然请求状态、最终页面 DOM 和截图。
- 用户列表重新打开可见两个账号、启用状态和角色摘要。
- 部门管理重新打开可见生产部/质量部对应负责人。
- 复核脚本不执行任何写入动作。

## 设计约束检查

- [x] 已读取 `AGENTS.md`、`docs/task-closeout-rules.md`、`docs/e2e-rules.md`、`docs/login-access.md`、`docs/local-runtime.md`、`docs/worktree-restrictions.md`。
- [x] 已确认本轮用户明确授权写入型 E2E，但禁止 API/fetch、数据库和 Git。
- [x] 未操作 DCC 业务文件；账号创建、角色分配和部门负责人配置均通过真实页面自然请求完成。
- [x] 两个账号及两个部门负责人已在页面重新打开后确认。
- [x] 角色不含超级管理员、管理员或日志权限；密码未写入任务目录。

## Cleanup Keep

- doc/tasks/20260928-dcc-e2e-test-identities/task.md
- doc/tasks/20260928-dcc-e2e-test-identities/execution-log.md
- doc/tasks/20260928-dcc-e2e-test-identities/verification-report.md
- doc/tasks/20260928-dcc-e2e-test-identities/create-identities-real-ui.cjs
- doc/tasks/20260928-dcc-e2e-test-identities/probe-department-select-real-ui.cjs
- doc/tasks/20260928-dcc-e2e-test-identities/evidence/result.json
- doc/tasks/20260928-dcc-e2e-test-identities/evidence/department-select-probe.json
- doc/tasks/20260928-dcc-e2e-test-identities/evidence/department-select-probe-failure.png
