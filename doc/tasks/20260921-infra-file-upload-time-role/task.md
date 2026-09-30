# Task: 基础设施文件上传时间角色权限

## Goal

为基础设施文件管理增加独立权限，使被授权角色可以修改文件的上传时间（`infra_file.create_time`），并让文件管理页提供对应编辑入口。未拥有该权限的用户不得通过接口或页面修改上传时间。

## BDD

- `BDD: 授权角色修改文件上传时间 -> Given` 用户拥有 `infra:file:update-upload-time` 权限且目标文件存在，`When` 在文件管理页提交新的上传时间，`Then` 后端更新文件的 `create_time` 并返回成功。
- `BDD: 未授权用户不能修改文件上传时间 -> Given` 用户没有 `infra:file:update-upload-time` 权限，`When` 直接调用修改接口或尝试提交页面编辑，`Then` 请求被权限守卫拒绝且文件上传时间保持不变。
- `BDD: 非法上传时间被拒绝 -> Given` 用户拥有修改权限，`When` 提交空值或非法时间，`Then` 请求校验失败且不产生数据库更新。

## Milestones

- [ ] M1: 核对文件表、服务、Controller、前端文件管理页和现有权限种子。
- [ ] M2: RED：新增后端权限行为测试和前端静态契约，证明能力尚不存在。
- [ ] M3: GREEN：实现后端接口、独立权限码、前端编辑入口及定向验证。
- [ ] M4: REGRESSION：运行受影响模块测试、前端类型/静态检查和差异检查。
- [ ] M5: 收尾：生成验证报告，执行 cleanup preview/apply；因本轮未授权 Git 提交/推送，按规则记录阻塞。

## Expected Verification

- 后端 `yudao-module-infra` 定向测试覆盖授权成功、无权限拒绝、非法时间拒绝和真实更新调用。
- 权限 SQL 静态合同覆盖新权限码及幂等插入，不修改无关角色绑定。
- 前端静态契约覆盖编辑按钮权限、编辑请求载荷、成功后刷新和失败提示。
- 前端 `pnpm` 定向类型检查或构建验证，后端 Maven 定向测试，`git diff --check`。
- 不执行 E2E；用户本轮未明确要求 E2E。

## Design Constraints

- 仅修改基础设施文件管理的文件元数据上传时间，不修改对象存储对象的物理时间或业务审计事件时间。
- 修改能力必须由后端 `@PreAuthorize` 和服务边界共同保护，不能只依赖前端按钮隐藏。
- 使用独立权限码 `infra:file:update-upload-time`，不复用文件删除、文件查询或管理员通配权限。
- 只处理当前任务资产，不回滚工作区中已有的并行改动。
- 不执行数据库写入，不执行 Git 提交或推送。

## Current Status

in_progress
