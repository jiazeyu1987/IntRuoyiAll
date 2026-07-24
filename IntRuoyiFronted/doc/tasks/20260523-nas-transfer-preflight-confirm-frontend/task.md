# 任务：NAS转移前增加确认弹框

## Goal

在 `NAS管理` 页中，用户点击真正开始 `NAS转移` 前先弹出确认框；同时因为部分目录可能包含 `10000+` 子文件夹和子文件，确认前不得递归预扫描整棵子树。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\system\nas\index.vue`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\system-nas-management.test.mjs`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260523-nas-transfer-preflight-confirm-frontend\**`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\changes\20260523-nas-transfer-preflight-confirm.md`

## Non-Scope

- 不改后端接口
- 不做子目录/子文件总数预统计
- 不调整转移结果展示结构

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260522-dcc-nas-transfer-controlled-files-frontend\task.md`
- Status before this task: `Blocked on 2026-05-23`
- Impact: 上一任务仅剩 OnlyOffice 实机预览前置阻塞，不影响本次在 `NAS管理` 页补确认弹框。

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- Current state: 仓库存在 showroom 相关无关用户改动
- Impact: 本任务只修改 `NAS管理` 页、对应静态测试与本任务文档

## Milestones

- [x] M1: 记录需求变更与上一任务阻塞状态
- [x] M2: 记录 BDD / RED，锁定“确认前不递归统计”的交互规则
- [x] M3: 实现确认弹框并补静态测试
- [x] M4: 跑前端定向验证并更新证据

## Expected Verification

- `node --test scripts\system-nas-management.test.mjs`
- `pnpm exec eslint src/views/system/nas/index.vue scripts/system-nas-management.test.mjs --format stylish`

## Current Status

Completed on 2026-05-23. 当前 `NAS管理` 页在真正发送转移请求前会先弹出确认框，并明确提示不会为了统计 `10000+` 子项而预先递归扫描整棵子树。

## Blockers And Impact

- Blocker: 当前本地 Node 运行时无法直接 `require('playwright')`
- Impact:
  - 本任务已完成静态测试和定向 `eslint`
  - 真实浏览器自动化验证本轮仅做到了尝试，未形成可复用 Playwright 用例
