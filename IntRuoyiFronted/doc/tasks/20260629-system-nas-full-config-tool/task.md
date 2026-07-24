# 任务：系统 NAS 配置工具扩展为完整连接参数台（前端）

- Task ID: `20260629-system-nas-full-config-tool`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- Created: `2026-06-29`
- Current Status: `completed`

## Task Goal

扩展 `/system/nas` 页面与前端 API 契约，让用户可以在现有 NAS 管理页中配置并提交完整 SMB NAS 参数，且继续保持当前运维操作台风格。

## Previous Task Check

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260629-srm-nas-locator-enter-search-no-autorefresh\task.md`
- 状态：`completed`
- 处理说明：上一前端任务已完成；本次继续在 `system/nas` 页面做独立功能扩展。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 命中 `docs/powershell-memory.md` 与 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`。
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - 中文文档、静态检查脚本与命令输出统一显式 UTF-8。
- `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
  - 保持当前白底操作台、紧凑表单和按钮布局，不做营销化改版。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。直接扩展正式 `NasConfigVO` 和页面表单，不做“展示字段但不提交/不校验”的空壳 UI。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: NAS 管理页展示完整 SMB 参数表单 -> Given 用户打开 /system/nas / When 页面加载成功 / Then 页面除基础 4 项外，还应展示新增的 domain、port、连接模式等正式参数输入项。`
- `BDD: 提交与测试连接复用同一完整参数对象 -> Given 用户填写完整参数 / When 点击保存或测试连接 / Then 前端提交的请求体都携带同一组完整 NAS 参数。`

## Milestones

1. M1：补 RED 静态检查锁定页面与 API 契约。`completed`
2. M2：实现最小前端改动并跑 GREEN。`completed`
3. M3：更新证据与收尾。`completed`

## Expected Verification

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\system-nas-management.test.mjs`

## Current Blockers

- 无。

## Final Verification Result

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\system-nas-management.test.mjs` -> PASS

## Completion Result

- `/system/nas` 页面已从 4 项基础参数扩展到 6 项 SMB 正式参数表单：`server`、`port`、`share`、`domain`、`username`、`password`。
- 新增字段已接入页面默认值、前端类型约束与保存/测试连接请求体。
- 页面布局继续保持现有操作台式样式，没有引入无效字段或无关视觉改版。
