# 任务：Infra 增加带账号密码的 NAS 浏览接口

## Goal

在 `ruoyi-vue-pro` 当前系统中新增一个可直接连接 NAS 共享的后端接口，采用 `D:\ProjectPackage\RagflowAuth` 当前 `nas_browser_service.py` 的方法：后端持有 NAS 服务器、共享名、用户名和密码，通过 SMB 会话连接 NAS 并返回目录列表。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-infra\pom.xml`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-infra\src\main\java\cn\iocoder\yudao\module\infra\controller\admin\file\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-infra\src\main\java\cn\iocoder\yudao\module\infra\service\file\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-infra\src\main\java\cn\iocoder\yudao\module\infra\enums\ErrorCodeConstants.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-infra\src\test\java\cn\iocoder\yudao\module\infra\controller\admin\file\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-infra\src\test\java\cn\iocoder\yudao\module\infra\service\file\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260520-infra-nas-credential-browser-api\**`

## Non-Scope

- 不增加前端页面或前端调试入口。
- 不改数据库 schema、菜单 seed、权限 seed。
- 不做 NAS 凭据的安全治理、加密存储或配置中心迁移；本任务按用户要求直接复用 RagflowAuth 的显式凭据方法。
- 不伪造 NAS 成功连接；如果依赖缺失或 SMB 客户端行为不通，必须显式失败。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260520-nas-folder-locate-qms-dhf-dmr\task.md`
- Status before this task: `Blocked on 2026-05-20`
- Reported blocker: 当前机器无活动 NAS 映射盘，也缺少具体 UNC / 共享根路径，无法可靠定位目标文件夹。
- Impact on this task: 本次任务通过新增“带账号密码的 NAS 浏览接口”来补足当前系统的 NAS 访问能力，但不覆盖旧任务的定位结论。

## Milestones

- [x] M1: 创建任务文档并确认上一同仓任务阻塞状态。
- [x] M2: 记录 BDD 场景并写出 RED 测试，锁定 NAS 浏览接口契约和 SMB 会话行为。
- [x] M3: 实现最小后端接口、VO、错误码、SMB 连接服务与依赖。
- [x] M4: 跑定向测试与后端证据校验，记录 GREEN。
- [x] M5: 运行 task-closeout-cleanup 预览并完成收尾。

## Expected Verification

- `mvn -pl yudao-module-infra "-Dtest=NasBrowserServiceImplTest,FileControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260520-infra-nas-credential-browser-api/backend-api-evidence.md`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260520-infra-nas-credential-browser-api --mode preview`

## Current Status

Completed on 2026-05-20. `yudao-module-infra` 已新增带账号密码的 NAS 浏览接口、SMBJ 依赖、服务实现、VO 和错误码；定向测试、后端证据校验和 closeout preview 均已完成。

## Blockers And Impact

- Blocker: none.
- Impact:
  - 当前系统已具备直接用后端内置凭据建立 SMB 会话并浏览 NAS 的能力。
  - 按用户要求，本任务未处理凭据安全治理，只复用 RagflowAuth 风格的显式凭据方案。

## Final Verification Result

- `mvn -pl yudao-module-infra "-Dtest=NasBrowserServiceImplTest,FileControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，6 tests green。
- `Test-NetConnection 172.30.30.4 -Port 445` -> PASS，`TcpTestSucceeded=True`。
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260520-infra-nas-credential-browser-api/backend-api-evidence.md` -> PASS。
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260520-infra-nas-credential-browser-api --mode preview` -> READY，默认 keep `task.md` / `execution-log.md`，若 apply 会删除 `backend-api-evidence.md`；本次仅执行 preview。
