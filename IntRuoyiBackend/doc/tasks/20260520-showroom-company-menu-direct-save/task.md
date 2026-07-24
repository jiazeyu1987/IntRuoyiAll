# 任务：展厅公司菜单可见即编辑直存（后端）

## Goal

将公司相关接口授权从 `showroom_publicity` 专用角色切换为“已登录且可进入公司菜单路径的用户可调用当前页面所需接口”的最小改动模型；保持接口结构不变，保持公司文字保存自动升公司版本，保持公司语音生成/保存不进入审批且不联动公司版本。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\controller\admin\ShowroomAdminController.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\controller\ShowroomApiRuntime.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\test\java\cn\iocoder\yudao\module\showroom\integration\ShowroomHttpApiIntegrationTest.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260520-showroom-company-menu-direct-save\**`

## Non-Scope

- 不新增权限码或菜单 seed
- 不修改公司 `/draft`、`/submit` 路由的业务语义，除非测试证明当前页面仍依赖它们
- 不修改产品、展厅、审批中心其他接口
- 不变更数据库 schema

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260520-yingtai-admin-login-failure\task.md`
- Status before this task: completed
- Impact: 无，可继续处理新后端任务

## Milestones

- [x] M1: 创建后端任务文档并确认上一同仓库任务状态。
- [x] M2: 先补 RED 集成测试，锁定去角色门控、公司保存升版本、语音保存不升公司版本。
- [x] M3: 实现最小后端授权语义调整。
- [ ] M4: 执行集成测试并记录 GREEN 证据。
- [ ] M5: 更新后端证据并准备收尾。

## Expected Verification

- `mvn "-Dsurefire.failIfNoSpecifiedTests=false" -pl yudao-module-showroom -Dtest=ShowroomHttpApiIntegrationTest test`
- `python -X utf8 C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260520-showroom-company-menu-direct-save\backend-api-evidence.md`

## Current Status

Blocked on 2026-05-20.

## Blockers And Impact

- Blocker: `ShowroomHttpApiIntegrationTest.java` 当前工作区混入与本任务无关的旧展示接口断言漂移，仍引用不存在的 `displayController.getAppConfig()` / `AppConfigPayload`。
- Impact:
  - 本次公司页接口授权语义已落代码，且主源码编译通过。
  - 但目标集成测试命令会先在同一测试文件的 unrelated 旧断言处失败，无法把本任务作为“整套 showroom integration 全绿”收口。

## Current Verification Snapshot

- PASS: `mvn "-Dmaven.test.skip=true" -pl yudao-module-showroom -am compile`
- FAIL-BLOCKED: `mvn "-Dsurefire.failIfNoSpecifiedTests=false" -pl yudao-module-showroom -Dtest=ShowroomHttpApiIntegrationTest test`
