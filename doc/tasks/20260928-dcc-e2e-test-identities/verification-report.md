# Verification Report

## Current Status
blocked

## 结论

本轮 owner-change/权限边界测试账号准备为 **BLOCKED**。没有创建账号、分配角色或修改部门负责人。

## 真实页面范围

- 登录入口：`http://127.0.0.1:8062`
- 租户：芋道源码
- 账号：`admin`
- 实际页面：系统管理 → 用户管理 → 新增用户弹窗
- 唯一自然写请求：无。只发生登录自然请求，HTTP 200。
- 用户创建、用户角色分配、部门负责人更新：均未提交。
- 禁止项遵守情况：未调用 `fetch`、API client、数据库或 Git；未使用 `force` 点击；未操作 DCC 业务文件。

## 阻塞细节

1. 首次尝试打开弹窗内归属部门树后，选择器节点被 `.el-overlay-dialog` 截获 pointer events。该节点虽在 DOM 中标记可见，但不是可安全点击的前景控件。
2. 第二次尝试从可见新增用户 dialog 内用 placeholder `请选择归属部门` 定位输入框，字段没有可定位 input（locator count 为 0）。页面只呈现选择框控件，尚未能安全定位属于该控件的可见 body popper/过滤输入。
3. 按用户指示达到两次有依据尝试后停止；未用 `force`、脚本注入或其它方式绕过遮罩。

## 结果证据

- 首次运行结果：`evidence/result.json`，`createdUsers=[]`、`assignedRoles=[]`、`configuredDepartments=[]`；自然网络记录只有管理员登录。
- 归属部门控件探查：`evidence/department-select-probe.json`，字段可见、input count 0。
- 页面截图：[department-select-probe-failure.png](evidence/department-select-probe-failure.png)
- Playwright 脚本：[create-identities-real-ui.cjs](create-identities-real-ui.cjs)、[probe-department-select-real-ui.cjs](probe-department-select-real-ui.cjs)

## 后续恢复条件

需先让新增用户弹窗中的部门选择器具备可见、可交互的前景 popper 或可聚焦过滤输入；再由用户当轮授权继续真实页面创建账号与负责人配置。本轮生成但未提交的临时凭据均作废，未落盘。
