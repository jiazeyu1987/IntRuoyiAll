# 任务：提交 ERP/MES 可闭环前端代码补充批次

- Task ID: `20260630-commit-erp-mes-followup-frontend`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- Created: `2026-06-30`
- Current Status: `completed`

## Task Goal

在前端仓库中仅提交当前仍留在工作区、且已具备 `completed` 状态与验证证据、文件边界清晰的 ERP/MES 改动。本批目标只包含：

- `20260630-erp-production-order-material-list-bidirectional-link`
- `20260630-erp-material-list-workorder-link-label-fix`
- `20260630-test-server-zhaojie-replan-preview-permission-fix`

`20260630-showroom-hall-config-package`、`20260630-dcc-admin-full-config-package`、测试服 DCC 跟进以及混在 `showroom-admin`/`dcc` 共享文件中的未收口内容继续保留在工作区。

## Previous Task Check

- 上一个前端提交任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260630-commit-committable-followup-frontend\task.md`
- 状态：`completed`
- 处理说明：上一批前端补充提交已完成工作台全量包收口；本次进入新的 ERP/MES 提交批次。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 命中 `docs\powershell-memory.md` 与 `docs\worktree-memory.md`。
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - 中文日志与任务文档显式 UTF-8。
- `D:\ProjectPackage\Int\IntRuoyi\docs\worktree-memory.md`
  - 共享页面混入未完成 hunk 时不得整文件暂存；只能提交边界清晰的文件组。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；只提交已闭环并已验证的正式前端代码。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 已完成 ERP/MES 前端任务可独立提交 -> Given 前端工作区存在生产用料清单与生产工单双向跳转、主表文案修正和手动重排权限门禁改动 / When 本次补充提交收口 / Then 只提交这些已具备 GREEN 证据的前端文件组。`
- `BDD: Showroom/DCC 共享页面混入未完成内容时继续留在工作区 -> Given showroom-admin、dcc browser 等共享页面仍混有 blocked 或 in_progress hunk / When 评估提交范围 / Then 这些共享文件不纳入本批。`

## Milestones

1. M1：建立本轮前端 ERP/MES 提交任务并锁定候选文件组。`completed`
2. M2：补核候选任务 GREEN 证据与共享文件边界。`completed`
3. M3：按任务边界提交前端代码。`completed`
4. M4：记录剩余未提交范围并完成收尾预览。`completed`

## Expected Verification

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\erp-production-order-material-link-static.spec.js`
- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\erp-production-material-list-static.spec.js`
- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-pro-schedule-order-pool-static.spec.js`
- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-pro-schedule-order-usability-static.spec.js`
- `git -C D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 diff --cached --name-only`
- `git -C D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 diff --cached --check`

## Final Verification Result

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\erp-production-order-material-link-static.spec.js` -> PASS
- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\erp-production-material-list-static.spec.js` -> PASS
- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-pro-schedule-order-pool-static.spec.js` -> PASS
- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-pro-schedule-order-usability-static.spec.js` -> PASS
- `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260630-erp-production-order-material-list-bidirectional-link\frontend-feature-evidence.md` -> PASS
- `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260630-erp-material-list-workorder-link-label-fix\frontend-feature-evidence.md` -> PASS
- `git -C D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 commit -m "任务: 提交ERP生产用料清单与排产权限前端收口"` -> PASS，创建 commit `6b5aed411`
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260630-commit-erp-mes-followup-frontend --mode preview` -> PASS

## Current Blockers

- 无新的提交阻塞；剩余改动属于进行中/阻塞任务或边界不清文件，继续保留在工作区。
