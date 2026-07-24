# 任务：NAS 配置页只显示当前已设置值

- Task ID: `20260629-system-nas-config-hide-empty-values`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- Created: `2026-06-29`
- Current Status: `completed`

## Task Goal

调整 `/system/nas` 配置表单的展示逻辑：当前有真实设置值的字段显示其值；当前没有设置值的可选字段不显示，避免出现空输入框或仅占位提示；同时保留显式展开补充连接参数的入口，避免隐藏后无法继续配置端口、域等可选项。

## Previous Task Check

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260629-system-nas-full-config-tool\task.md`
- 状态：`completed`
- 处理说明：上一任务已完成基础 NAS 参数扩展；本次以前端显示细化为主，并在必要处联动后端读取语义，避免默认值被误判为“当前已配置”。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 命中 `docs/powershell-memory.md` 与 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`。
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - 中文文档、静态检查脚本与命令输出统一显式 UTF-8。
- `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
  - 保持当前白底操作台与紧凑表单风格，不做无关改版。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。直接按真实返回值控制可选字段显隐，并保留显式补充配置入口，不再用默认值或占位文本伪装“已配置”。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 已设置字段显示真实值 -> Given 后端返回某个 NAS 字段已有真实配置值 / When 页面加载配置 / Then 该字段显示当前值，而不是空占位。`
- `BDD: 未设置的可选字段不显示 -> Given 后端返回某个可选 NAS 字段为空 / When 页面加载配置 / Then 页面不渲染该输入项，不显示空值或仅占位提示。`
- `BDD: 未设置的可选字段默认收起但仍可主动补录 -> Given 当前 NAS 可选字段没有真实配置值 / When 用户需要补录端口或域 / Then 页面提供显式展开入口，展开后可填写并保存可选参数。`

## Milestones

1. M1：补 RED 静态检查锁定显示行为。`completed`
2. M2：实现最小前端修复并跑 GREEN。`completed`
3. M3：更新证据与收尾。`completed`
4. M4：补充可选参数展开入口，并与后端读取语义对齐。`completed`

## Expected Verification

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\system-nas-management.test.mjs`

## Current Blockers

- 无。

## Final Verification Result

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\system-nas-management.test.mjs` -> PASS

## Completion Result

- `/system/nas` 现在会优先显示当前真实配置值；不会再为了空值渲染占位输入项。
- `domain` 为空时不显示；`port` 只有当前配置里已有真实值时才显示。
- 页面新增“补充连接参数”入口；未设置的 `port/domain` 默认收起，但仍可按需展开补录并保存。
- 页面其余已配置的基础字段仍正常显示当前值，不影响保存、测试连接和后续操作区。
