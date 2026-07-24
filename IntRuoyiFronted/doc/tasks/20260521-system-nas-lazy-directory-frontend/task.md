# 任务：NAS 管理页目录懒加载（前端）

## Goal

将 `系统管理 -> NAS管理` 页的目录区域改为按需懒加载：

- 点击“刷新目录”时只同步 NAS 共享第一层目录
- 展开树节点时按当前目录路径继续读取下一层目录
- 避免前端依赖 `/infra/file/nas-tree` 做整棵共享全量递归
- 保留测试连接成功后才能刷新目录、错误显式反馈和已跳过目录提示

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\system\nas\**`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\api\system\nas\**`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\system-nas-management.test.mjs`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-system-nas-lazy-directory-frontend\**`

## Non-Scope

- 不修改后端 NAS 配置保存/测试接口。
- 不新增额外菜单。
- 不在目录读取失败时静默降级为假数据或成功提示。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-system-nas-tree-skip-inaccessible-frontend\task.md`
- Status before this task: `Completed with blockers on 2026-05-21`
- Impact: 前一轮已经补齐 skipped 展示，但页面仍依赖后端全量递归；本轮只处理目录懒加载，不回退该能力。

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- Current state: 仓库存在多处与 showroom 相关的用户改动。
- Impact: 本任务仅修改 NAS 管理 API、页面、脚本和本任务文档，避免混入无关文件。

## Milestones

- [x] M1: 创建任务文档并确认上一同仓 NAS 任务状态。
- [x] M2: 记录 BDD 与 RED，锁定“首层刷新 + 节点懒加载”契约。
- [x] M3: 完成最小前端实现并保留错误/权限提示。
- [x] M4: 跑定向验证并记录 GREEN。
- [x] M5: 运行 closeout preview 并完成收尾。

## Expected Verification

- `node --test scripts\system-nas-management.test.mjs`
- `node --max-old-space-size=8192 node_modules/vue-tsc/bin/vue-tsc.js --noEmit -p tsconfig.relaxed.json`
- `pnpm exec eslint src/api/system/nas/index.ts src/views/system/nas/index.vue scripts/system-nas-management.test.mjs --format stylish`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260521-system-nas-lazy-directory-frontend/frontend-feature-evidence.md`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260521-system-nas-lazy-directory-frontend --mode preview`

## Current Status

Completed on 2026-05-21. 已完成前端懒加载改造、定向验证、真实页面链路验证与 closeout preview。

## Blockers And Impact

- Blocker: none.
- Impact:
  - 页面刷新目录时已改为只读取共享根目录第一层。
  - 展开树节点时已按当前节点 path 再读取下一层目录。
  - 真实页面验证已确认不再触发 `/infra/file/nas-tree`。

## Final Verification Result

- `node --test scripts\system-nas-management.test.mjs` -> PASS，2 tests green。
- `node --max-old-space-size=8192 node_modules\vue-tsc\bin\vue-tsc.js --noEmit -p tsconfig.relaxed.json` -> PASS。
- `pnpm exec eslint src/api/system/nas/index.ts src/views/system/nas/index.vue scripts/system-nas-management.test.mjs --format stylish` -> PASS。
- `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260521-system-nas-lazy-directory-frontend/frontend-feature-evidence.md` -> PASS。
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260521-system-nas-lazy-directory-frontend --mode preview` -> READY，默认 keep `task.md` / `execution-log.md`，preview 计划清理 `frontend-feature-evidence.md`。
- 真实页面验证：
  - 登录 `芋道源码 / admin / admin123` 后打开 `http://127.0.0.1:8081/system/nas`
  - `测试连接` -> PASS，返回 `rootPath=\\\\172.30.30.4\\it共享`、`itemCount=45`
  - `刷新目录` -> PASS，只触发 `GET /admin-api/infra/file/nas-files?path=`，页面同步根层 `31` 个目录
  - 展开首个节点 -> PASS，只再触发 `GET /admin-api/infra/file/nas-files?path=%23recycle`
  - `/admin-api/infra/file/nas-tree` -> 未触发
  - 无权限子目录 `#recycle` -> PASS as explicit error exposure，页面显示“部分目录已跳过 / 已跳过目录 / NAS 读取失败：access denied: #recycle”
