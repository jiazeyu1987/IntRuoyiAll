# 任务：系统管理 NAS 管理页签目录树（后端）

## Goal

为 `NAS管理` 页面补齐一个基于已保存 NAS 参数的目录树同步接口，使前端在“测试连接成功”后点击“刷新目录”时，能够一次性获得 NAS 的目录结构并渲染到页面中。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-infra\src\main\java\cn\iocoder\yudao\module\infra\controller\admin\file\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-infra\src\main\java\cn\iocoder\yudao\module\infra\service\file\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-infra\src\test\java\cn\iocoder\yudao\module\infra\controller\admin\file\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-infra\src\test\java\cn\iocoder\yudao\module\infra\service\file\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-system-nas-directory-tree-backend\**`

## Non-Scope

- 不调整 NAS 参数保存逻辑本身。
- 不改菜单 SQL。
- 不引入 fallback 树、分页树或 mock 目录结果。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-system-nas-menu-garbled-fix\task.md`
- Status before this task: `Completed on 2026-05-21`
- Impact: NAS 管理菜单已正常显示，本任务在其基础上补目录树同步能力。

## Milestones

- [x] M1: 创建任务文档并确认上一同仓任务状态。
- [x] M2: 记录 BDD 并写出 RED 测试，锁定目录树接口契约。
- [x] M3: 实现最小后端目录树接口与服务逻辑。
- [x] M4: 跑定向测试与证据校验，记录 GREEN。
- [x] M5: 运行 closeout preview 并完成收尾。

## Expected Verification

- `mvn -pl yudao-module-infra "-Dtest=NasBrowserServiceImplTest,FileControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260521-system-nas-directory-tree-backend/backend-api-evidence.md`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260521-system-nas-directory-tree-backend --mode preview`

## Current Status

Completed on 2026-05-21. 目录树接口、回归测试、证据校验和 closeout preview 均已完成。

## Blockers And Impact

- Blocker: none.
- Impact: pending implementation result.

## Final Verification Result

- `mvn -pl yudao-module-infra "-Dtest=NasBrowserServiceImplTest,FileControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，12 tests green。
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260521-system-nas-directory-tree-backend/backend-api-evidence.md` -> PASS。
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260521-system-nas-directory-tree-backend --mode preview` -> READY，默认 keep `task.md` / `execution-log.md`，若 apply 会删除 `backend-api-evidence.md`；本次仅执行 preview。
