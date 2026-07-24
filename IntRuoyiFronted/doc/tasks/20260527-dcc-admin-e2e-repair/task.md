# 任务：恢复 DCC 电子签名 admin 测试租户访问入口

## 任务目标

- 在 `芋道源码/admin` 的真实前端路径中恢复访问测试租户的入口，使 DCC 电子签名只读验证可以通过页面选择测试租户。
- 复用已有 `TenantVisit` 组件和后端 `visit-tenant-id` 能力，不新增测试专用入口。
- 入口仅对具备 `system:tenant:visit` 或全权限的用户显示。

## 里程碑

- [x] M1：记录失败 E2E 和 RED 测试。
- [x] M2：在顶栏挂载已有租户访问组件。
- [x] M3：运行静态测试、ESLint/类型检查和真实 Playwright 验证。

## 预期验证

- `node --test scripts\dcc-tenant-visit-header.test.mjs`
- `pnpm exec eslint src/layout/components/ToolHeader.vue scripts/dcc-tenant-visit-header.test.mjs`
- 后端任务脚本：`node doc\tasks\20260527-dcc-admin-e2e-repair\scripts\dcc-admin-readonly-e2e.mjs`

## 当前状态

- 状态：completed
- 当前阶段：M3
- 当前结论：已在顶栏按权限挂载既有租户访问组件，静态 RED/GREEN、ESLint、类型检查、生产构建和测试服 `芋道源码/admin` Playwright 严格只读验证均已通过。最终 E2E 选择 `visitTenantId=122`，DCC 只读验证输出 `YUDAO_ADMIN_DCC_SIGNATURE_PASS`。
- 收尾说明：入口复用既有 `TenantVisit` 和 `visit-tenant-id` 能力；未新增测试专用控件。
- `int_main` 融合：前端 `int_main` 已快进融合 `codex/20260527-dcc-admin-e2e-repair` 至 `fd0d1be9`；后端 `int_main` 已融合至 `4871b0f02c`。
- `int_main` 复验：前端主干 `node --test scripts\dcc-tenant-visit-header.test.mjs` 通过；后端主干触发 `芋道源码/admin` 严格只读 E2E 通过并输出 `YUDAO_ADMIN_DCC_SIGNATURE_PASS`。
- 最终主干复验：后续 `int_main` 又快进融合 NAS 任务后，DCC 前端收口提交仍为当前主干祖先；已在最新 `int_main` 上重新运行前端租户访问测试，并通过后端主干 `芋道源码/admin` 严格只读 E2E。
