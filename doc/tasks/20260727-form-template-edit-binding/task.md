# 表单模板编辑绑定修复

## Task Goal

修复表单模板列表点击“编辑”时提示“当前模板未绑定批记录表单，无法执行该操作”的问题，确保已发布且可预览的模板能够进入编辑流程，且缺少绑定关系时仍明确失败。

## Milestones

- [x] 建立问题复现与根因定位
- [x] 编写并记录最小回归测试 RED
- [x] 实施最小前端修复
- [x] 运行 GREEN 与相关回归验证
- [ ] 完成收尾、经验沉淀、提交与推送

## Expected Verification

- 受影响的前端静态/单元测试先 RED 后 GREEN。
- 相关前端类型检查或最小静态契约通过。
- 缺少绑定关系的错误不被吞掉，真实错误仍向用户暴露。

## Current Status

ready_for_closeout

## Experience Gate

## 经验门禁

- 前端静态契约隔离门禁：宽 FormCenter 静态合同存在与本问题无关的 `activeMenu: '/mdm/form-center/policy'` 既有失败，本任务使用聚焦静态合同覆盖当前编辑按钮行为。
- Windows 换行与脚本行为同步门禁：修改 `tests/e2e/*static.spec.js` 后运行目标静态合同，确认当前工作区合同先 RED 后 GREEN。
- 脏工作区基线门禁：任务开始时工作区存在并行改动；已有并行基线提交 `fc07fc8a`、`32df0a46` 保护既有改动，本任务后续仅选择性暂存当前任务文件。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，编辑按钮恢复到现有本页模板规则编辑流程，批记录预览/填写仍保留绑定关系 fail-fast 校验。
- `是否存在临时补丁或绕过`：否。

## Cleanup Keep

- doc/tasks/20260727-form-template-edit-binding/bug-regression-evidence.md
