# 任务：修复展厅手动发布按钮对超级管理员不可见

## 任务目标

- 让“手动发布展厅”按钮对 `showroom_publicity` 和 `super_admin` 都可见。
- 前端可见性必须与后端 `ShowroomAdminController` 的发布权限契约保持一致。

## 非目标

- 不修改后端权限判定。
- 不调整按钮位置、文案、确认框或发布接口。
- 不顺手放大到所有展厅前端按钮的权限模型重构，除非本次修复直接需要。

## 前序任务检查

- 已检查上一同仓任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260524-showroom-manual-release-button-placement\task.md`
- 上一任务状态：`已完成`
- 影响：按钮位置调整已完成，本次只修正超级管理员可见性，不阻塞继续开发。

## 里程碑

- [ ] M1：建立任务记录并补 RED 测试，锁定 `super_admin` 也必须看到按钮。
- [ ] M2：在公司工作台可见性逻辑中对齐后端角色契约。
- [ ] M3：跑定向测试、静态检查与真实登录验证。
- [ ] M4：更新任务文档、执行日志并提交本任务改动。

## 预期验证

- `node --test scripts/showroom-admin-manual-release-button.test.mjs`
- `node node_modules\\eslint\\bin\\eslint.js src\\views\\showroom-admin\\company\\CompanyWorkbench.vue scripts\\showroom-admin-manual-release-button.test.mjs`
- Playwright 真实路径：
  - `http://127.0.0.1:18082/showroom/company`
  - 租户 `芋道源码` / 用户 `admin`

## 当前状态

状态：已完成

## Current Status

Completed

## Completed Work

- 已将公司工作台发布按钮可见性从仅 `showroom_publicity`，调整为 `showroom_publicity || super_admin`。
- 已保持按钮位置、发布接口、确认框、loading 和错误提示逻辑不变。
- 已使前端按钮可见性与后端 `ShowroomAdminController` 的发布权限契约对齐。

## Final Verification

- RED: `node --test scripts/showroom-admin-manual-release-button.test.mjs` -> FAIL，`CompanyWorkbench.vue` 仍使用 `isShowroomPublicity`，未覆盖 `super_admin`。
- GREEN: `node --test scripts/showroom-admin-manual-release-button.test.mjs` -> PASS。
- GREEN: `node node_modules\\eslint\\bin\\eslint.js src\\views\\showroom-admin\\company\\CompanyWorkbench.vue scripts\\showroom-admin-manual-release-button.test.mjs` -> PASS。
- GREEN: 代码契约核对 -> PASS，后端 `ShowroomAdminController` 允许 `showroom_publicity || super_admin`，前端按钮条件现已一致。
- BLOCKED: Playwright 真实登录 `芋道源码 / admin / admin123` 到本地临时前端 `http://127.0.0.1:18082/showroom/company` -> 当前本地环境未能走过登录页，未拿到可复核会话，因此无法在本机对该账号完成最终 UI 可见性实测。

## Note

- 当前本地环境中，测试租户 `测试租户 / aoteman / admin123` 能正常登录并复核按钮链路。
- `芋道源码 / admin` 的账号可见性本次通过代码契约修正完成；若你当前页面仍看不到按钮，需要加载这次前端新代码后再看。
