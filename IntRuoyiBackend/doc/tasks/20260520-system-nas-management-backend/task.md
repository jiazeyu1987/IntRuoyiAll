# 任务：系统管理 NAS 管理页签（后端）

## Goal

为当前系统补齐 NAS 管理页面所需的后端能力：

- 读取已保存的 NAS 连接参数
- 保存 NAS 连接参数
- 使用当前表单参数测试 NAS 连接是否成功
- 让 `GET /infra/file/nas-files` 从已保存参数读取，而不是硬编码常量
- 补一条系统管理菜单 SQL，使前端页面能挂到“系统管理”下

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-infra\src\main\java\cn\iocoder\yudao\module\infra\controller\admin\file\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-infra\src\main\java\cn\iocoder\yudao\module\infra\service\file\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-infra\src\main\java\cn\iocoder\yudao\module\infra\enums\ErrorCodeConstants.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-infra\src\test\java\cn\iocoder\yudao\module\infra\controller\admin\file\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-infra\src\test\java\cn\iocoder\yudao\module\infra\service\file\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\sql\mysql\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260520-system-nas-management-backend\**`

## Non-Scope

- 不处理 NAS 凭据安全治理、加密存储和配置中心迁移。
- 不新增前端 mock 数据或 fallback 参数。
- 不伪造 NAS 连接成功；测试失败必须显式暴露。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260520-infra-nas-credential-browser-api\task.md`
- Status before this task: `Completed on 2026-05-20`
- Impact: 当前仓库已经有带账号密码的 NAS 浏览接口；本次任务在其基础上补配置读写、测试连接和菜单挂接。

## Milestones

- [x] M1: 创建任务文档并确认上一同仓任务完成状态。
- [x] M2: 记录 BDD 场景并写出 RED 测试，锁定 NAS 配置读取/保存/测试契约。
- [x] M3: 实现最小后端接口、配置持久化、菜单 SQL 和浏览服务改造。
- [x] M4: 跑定向测试与后端证据校验，记录 GREEN。
- [x] M5: 运行 task-closeout-cleanup 预览并完成收尾。

## Expected Verification

- `mvn -pl yudao-module-infra "-Dtest=NasSettingsServiceTest,NasBrowserServiceImplTest,FileControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `python -m pytest ruoyi-vue-pro\script\tests\test_system_nas_menu_sql.py -q`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260520-system-nas-management-backend/backend-api-evidence.md`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260520-system-nas-management-backend --mode preview`

## Current Status

Completed on 2026-05-21. 后端接口、配置持久化、菜单 SQL、定向测试、证据校验和 closeout preview 均已完成。

## Blockers And Impact

- Blocker: none.
- Impact: pending implementation result.

## Final Verification Result

- `mvn -pl yudao-module-infra "-Dtest=NasSettingsServiceTest,NasBrowserServiceImplTest,FileControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，13 tests green。
- `python -m pytest ruoyi-vue-pro\script\tests\test_system_nas_menu_sql.py -q` -> PASS，1 test green。
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260520-system-nas-management-backend/backend-api-evidence.md` -> PASS。
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260520-system-nas-management-backend --mode preview` -> READY，默认 keep `task.md` / `execution-log.md`，若 apply 会删除 `backend-api-evidence.md`；本次仅执行 preview。
