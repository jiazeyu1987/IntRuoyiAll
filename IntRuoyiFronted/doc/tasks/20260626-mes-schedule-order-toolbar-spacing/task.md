# 任务：MES 排产工单工具栏排版优化

## 任务目标

- 优化 `/mes/pro/scheduleorder` 排产工单页工具栏排版，避免 `搜索 / 重置 / 同步工单 / 手动重排 / 批量冻结 / 批量解冻 / 批量删除` 按钮挤在一起。
- 将查询动作与页面级批量动作做明确分组，并保证在较窄宽度下可换行、不相互挤压。
- 不修改现有按钮文案、权限门禁、禁用逻辑、接口调用和业务行为。

## 当前状态

COMPLETED

## 上一任务检查

- 上一个 MES frontend 任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260626-mes-replan-protected-task-readable\task.md`
- 状态：`已完成`
- 处理说明：已完成静态合同、类型校验与前端证据校验，不阻塞本次工具栏排版优化。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- 适用强制门禁：
  - 工具栏必须保持 IntPP 运维台紧凑、白底、轻边框、明确分组的操作台风格，不做无关视觉重构。
  - 本轮仅做前端源码、静态合同、类型校验与证据校验，不执行真实登录、真实写入或长链路 E2E。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。只调整工具栏结构与样式，不添加兼容兜底分支。
- `是否从根因和长期维护角度解决`：是。通过稳定的工具栏分组容器和换行布局解决拥挤问题，而不是单纯硬调单个按钮宽度。
- `是否存在临时补丁或绕过`：否。不会通过隐藏按钮、删除文案或只对某个分辨率加一次性补丁来规避问题。

## BDD 场景

- `BDD: 排产工单工具栏分组显示 -> Given 用户打开排产工单列表 / When 页面渲染查询和批量操作按钮 / Then 查询动作与页面级操作动作应按分组展示，不再紧贴成一串按钮。`
- `BDD: 工具栏在较窄宽度下仍保持可读 -> Given 工具栏可见按钮数量较多 / When 可用宽度收窄导致按钮需要换行 / Then 按钮组应允许换行并保留明确间距，不发生相互挤压。`

## 里程碑

1. M1：创建任务文档、补经验门禁和静态 RED 布局合同。`COMPLETED`
2. M2：最小修改排产工单页工具栏结构和样式。`COMPLETED`
3. M3：运行 GREEN 静态验证、类型校验、证据校验和收尾预览。`COMPLETED`

## 预期验证

- `node tests/e2e/mes-pro-schedule-order-toolbar-layout-static.spec.js`
- `node tests/e2e/mes-pro-schedule-order-usability-static.spec.js`
- `node tests/e2e/mes-pro-schedule-order-replan-scope-static.spec.js`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260626-mes-schedule-order-toolbar-spacing\frontend-feature-evidence.md`

## 最终验证结果

- `node tests/e2e/mes-pro-schedule-order-toolbar-layout-static.spec.js` -> PASS
- `node tests/e2e/mes-pro-schedule-order-usability-static.spec.js` -> PASS
- `node tests/e2e/mes-pro-schedule-order-replan-scope-static.spec.js` -> PASS
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> BLOCKED，仓库存在与本任务无关的全局 Pinia/Store 类型错误，首个报错位于 `src/App.vue`
- `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260626-mes-schedule-order-toolbar-spacing\frontend-feature-evidence.md` -> PASS
- `node D:\ProjectPackage\Int\IntRuoyi\scripts\preflight\login-preflight.mjs --base-url http://localhost:8081 --tenant 测试租户 --username aoteman --password 111111 --target-path /mes/pro/scheduleorder --target-text 排产工单` -> BLOCKED，60 秒内未等到目标页关键文本
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260626-mes-schedule-order-toolbar-spacing --mode preview` -> PASS，预览结果 `status=ready`，默认保留 `task.md` 与 `execution-log.md`，`frontend-feature-evidence.md` 被识别为可清理候选。
