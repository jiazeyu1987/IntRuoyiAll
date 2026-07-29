# 20260729-fill-config-current-form-title

## Task Goal

在批记录填写配置的辅助表单映射界面顶部红框位置显示当前表单的名称和版本，便于用户确认正在配置的目标表单。

## Milestones

- [ ] 建立任务记录并读取适用经验门禁。
- [ ] 定位辅助表单映射顶部区域的数据来源和组件边界。
- [ ] 先补充静态合同，锁定当前表单名称和版本展示行为。
- [ ] 实现最小前端改动并通过目标验证。
- [ ] 完成收尾记录、经验沉淀、提交和推送。

## Expected Verification

- `node tests/e2e/edhr-fill-config-current-form-title-static.spec.js`
- `pnpm ts:check`
- 前端功能证据校验：`python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260729-fill-config-current-form-title/frontend-feature-evidence.md`

## Current Status

in_progress

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，计划直接使用当前表单正式上下文中的名称与版本展示，不增加替代数据源。
- `是否存在临时补丁或绕过`：否。

## Experience Gate Summary

待读取 `docs/experience-index.md` 后补充。
