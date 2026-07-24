# 任务：Infra 增加 NAS 目录读取接口

## Goal

在 `ruoyi-vue-pro` 当前系统中新增一个受权限控制的 NAS 目录读取接口，使管理端能够提交一个服务器可访问的本地路径或 UNC/NAS 路径，并读取返回对应目录结构，用于确认当前系统具备读取 NAS 目录结构的后端能力。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-infra\src\main\java\cn\iocoder\yudao\module\infra\controller\admin\file\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-infra\src\main\java\cn\iocoder\yudao\module\infra\service\file\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-infra\src\main\java\cn\iocoder\yudao\module\infra\enums\ErrorCodeConstants.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-infra\src\test\java\cn\iocoder\yudao\module\infra\controller\admin\file\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-infra\src\test\java\cn\iocoder\yudao\module\infra\service\file\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260520-infra-nas-directory-read-api\**`

## Non-Scope

- 不增加前端页面或额外前端控件。
- 不接入数据库 schema、菜单 seed、权限 seed 或租户隔离改造。
- 不增加 fallback 路径、缓存副本、假目录树或 mock 成功返回。
- 不假装完成真实 NAS 联调；如缺少可访问 NAS 路径，必须明确记为验证前置条件缺失。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260520-showroom-publicity-product-assignment\task.md`
- Status before this task: `Blocked on 2026-05-20`
- Reported blocker: `yudao-module-showroom` 存量 `testCompile` 错误阻断了该展厅任务的集成验证。
- Impact on this task: 本次 NAS 目录读取接口落在 `yudao-module-infra`，不并入或回滚 showroom 相关脏改动；验证仅跑 infra 定向测试。

## Milestones

- [x] M1: 确认上一同仓任务状态并创建本任务文档、执行日志、后端证据骨架。
- [x] M2: 记录 BDD 场景并写出 NAS 目录读取的 RED 测试。
- [x] M3: 实现最小后端接口、VO、服务与错误码。
- [x] M4: 跑定向测试与后端证据校验，记录 GREEN。
- [x] M5: 尝试确认本机是否存在可访问真实 NAS 路径；若缺失则记录阻塞和影响。
- [x] M6: 运行 task-closeout-cleanup 预览并完成本任务收尾。

## Expected Verification

- `mvn -pl yudao-module-infra "-Dtest=NasDirectoryServiceImplTest,FileControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260520-infra-nas-directory-read-api/backend-api-evidence.md`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260520-infra-nas-directory-read-api --mode preview`

## Current Status

Completed with blockers on 2026-05-20. `yudao-module-infra` 已新增 `POST /infra/file/nas-directory-tree`、请求/响应 VO、独立 NAS 目录读取服务和显式错误码；定向测试、后端证据校验与 closeout preview 已完成。

## Blockers And Impact

- Blocker: 当前尚未发现本机可直接访问的真实 NAS 挂载盘或 `net use` 映射。
- Impact: 可以完成“接口实现 + 单测验证 + 对本地/临时目录结构的真实读取验证”，但若没有真实 NAS 路径，最终不能把“真实 NAS 目录读取成功”标记为已验证。

## Final Verification Result

- `mvn -pl yudao-module-infra "-Dtest=NasDirectoryServiceImplTest,FileControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，5 tests green。
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260520-infra-nas-directory-read-api/backend-api-evidence.md` -> PASS。
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260520-infra-nas-directory-read-api --mode preview` -> READY，默认 keep `task.md` / `execution-log.md`，若 apply 会删除 `backend-api-evidence.md`；本次仅按基线执行 preview，不做 apply。
