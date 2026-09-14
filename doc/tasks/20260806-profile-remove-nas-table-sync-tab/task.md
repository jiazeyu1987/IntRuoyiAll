# 移除个人工作台 NAS 表格自动同步入口

## Task Goal

在个人工作台“配置”页签中删除“NAS表格自动同步”可见页签和组件入口，只保留“ERP表格自动同步”入口；不改动后端 NAS 同步接口和既有 ERP 自动同步能力。

## Milestones

- [x] 记录 BDD 场景并更新前端静态契约，使旧 NAS 页签存在时先失败。
- [x] 移除 Profile 配置页中的 NAS 页签渲染和组件导出。
- [x] 运行目标静态契约与前端功能证据校验。

## Expected Verification

- `node IntRuoyiFronted/tests/e2e/profile-nas-table-auto-sync-static.spec.js`
- `node IntRuoyiFronted/tests/e2e/profile-erp-table-auto-sync-static.spec.js`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260806-profile-remove-nas-table-sync-tab/frontend-feature-evidence.md`

## Current Status

completed

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，移除用户可见入口和组件导出，避免配置页同时出现 NAS/ERP 两个相近入口。
- `是否存在临时补丁或绕过`：否。

## 经验门禁

- 前端静态契约隔离门禁：本次只改 Profile 配置页入口，使用专用静态契约验证，不用全量历史 E2E 失败替代当前需求证据。
- 业务运行记录用户可读展示门禁：保留 ERP 自动同步页签时，继续运行 ERP 静态契约，确保中文状态、触发来源、时间格式化和失败原因展示不回退。
