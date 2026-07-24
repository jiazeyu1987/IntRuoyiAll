# Execution Log: DCC 受控预览右侧详情面板

BDD: 预览页显示右侧详情 -> Given 用户打开 `/dcc/controlled-file/detail/:id?viewer=1&from=detail` / When 受控预览加载 / Then 页面左侧显示文件预览，右侧显示文件类别、文件名称、产品名称、受控目录、培训要求等基础信息。

BDD: 文控可在预览详情修改 -> Given 当前登录用户角色包含 `doc_control` / When 在预览页右侧详情点击“修改”并保存 / Then 前端调用现有 `PUT /dcc/controlled-files/{id}/metadata`，保存后刷新预览页右侧详情。

BDD: 非文控和超管不可修改 -> Given 当前登录用户角色不包含 `doc_control`，即使包含 `super_admin` / When 打开详情页或预览页 / Then 不显示“修改基础信息”“修改”和产品名称“识别”入口。

BDD: 保存失败明确暴露 -> Given 后端拒绝基础信息保存 / When 文控保存 / Then 弹窗保留错误，不关闭，不伪造成功。

RED: node scripts/dcc-controlled-file-preview-detail-panel.test.mjs -> FAIL, expected missing preview split layout, shared basic info panel, viewer edit selector, and removal of super_admin metadata edit branch.

GREEN: node scripts/dcc-controlled-file-preview-detail-panel.test.mjs -> PASS, 4 tests.

GREEN: node scripts/dcc-controlled-file-metadata-edit.test.mjs -> PASS, 4 tests.

RED: pnpm ts:check -> FAIL, Node default heap OOM at roughly 4GB; no TypeScript diagnostic was produced.

GREEN: $env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check -> PASS.

GREEN: node doc/tasks/20260607-dcc-preview-detail-panel/verify-preview-detail-panel.e2e.mjs -> PASS, real Playwright at http://localhost:8081. Positive path used 测试租户/aoteman, synchronized doc_control with the formal test-tenant role assignment endpoint, saved productName E2E预览详情1780789763468 from the viewer right-side detail panel, restored productName to blank, and restored the original role set reported by the permission API. Negative path used 芋道源码/admin and confirmed adminRoles common/super_admin/showroom_publicity with adminEditButtonCount 0.

GREEN: docker exec int-ruoyi-mysql mysql --default-character-set=utf8mb4 -uroot -p123456 -D ruoyi-vue-pro -e "DELETE ... code='doc_control'; SELECT ..." -> PASS, cleaned leftover test-tenant user_role duplicates for user 113 and confirmed roles showroom_publicity,tenant_admin.

GREEN: python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260607-dcc-preview-detail-panel/frontend-feature-evidence.md -> PASS.

GREEN: python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260607-dcc-preview-detail-panel --mode preview -> PASS, delete none, blocked none.
