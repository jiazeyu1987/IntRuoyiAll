# Task: 展厅封面回填改为可访问文件代理地址

## Goal

修复展厅产品真实点击 `AI生成` 后虽然接口成功，但回填的 `coverImage` 是对象存储直链并返回 `403`，导致前端图片无法实际显示的问题。

本次修复要求：

- 生成封面后回填的 `coverImage` 必须改为与 showroom 其他文件一致的可访问代理地址：
  - `/admin-api/infra/file/{configId}/get/{path}`
- 不改动 Codex CLI 图片生成逻辑本身。
- 不依赖对象存储 public URL 配置，不用 fallback 或前端绕过。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\cover\ShowroomProductCoverImageService.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\test\java\cn\iocoder\yudao\module\showroom\cover\ShowroomProductCoverImageServiceTest.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-cover-file-url-proxy\**`

## Non-Scope

- 不改前端按钮与页面结构。
- 不恢复 SiliconFlow。
- 不处理当前工作树里其他 showroom 在途改动。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-product-cover-default-codex-command\task.md`
- Status before this task: `Completed on 2026-05-21`
- Impact: 上一条封面命令修复已完成，本次继续收口真实图片访问链路。

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Current state: 目标封面服务文件当前干净，但仓库其他目录仍有无关在途改动。
- Impact: 本次只修改封面服务、独立回归测试和当前任务目录。

## Milestones

1. 创建任务文档、执行日志和后端证据骨架。
2. 先补 RED，锁定回填 URL 必须是 `/admin-api/infra/file/...` 代理地址。
3. 最小修复封面服务的文件保存与 URL 回填逻辑。
4. 跑通定向 GREEN、证据校验与 closeout preview。
5. 单独提交本任务范围改动。

## Expected Verification

- `mvn --% -pl yudao-module-showroom -Dtest=ShowroomProductCoverImageServiceTest -Dsurefire.failIfNoSpecifiedTests=false surefire:test`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-cover-file-url-proxy\backend-api-evidence.md`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260521-showroom-cover-file-url-proxy --mode preview`

## Current Status

- Status: In Progress
- Completed work:
  - 已根据真实数据验证确认当前 `coverImage` 对象存储直链返回 `403`。
  - 已锁定修复方向：回填 showroom 一致的 `/admin-api/infra/file/{configId}/get/{path}` 代理地址。
- Remaining blockers:
  - RED/修复/GREEN 尚未完成。

## Milestone Status

### Milestone 1

- Status: Completed
- Completed work:
  - 已创建任务文档、执行日志和后端证据骨架。
  - 已将真实 403 问题收敛到“对象存储直链不可读”。
- Verification evidence:
  - `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-cover-file-url-proxy\task.md`
  - `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-cover-file-url-proxy\execution-log.md`
  - `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-cover-file-url-proxy\backend-api-evidence.md`
- Remaining blockers:
  - 需要完成 RED/修复/GREEN。

### Milestone 2

- Status: Completed
- Completed work:
  - 已将独立回归测试切换为 `FileService` 路径，并锁定 `/admin-api/infra/file/...` 代理 URL 契约。
  - 已执行 RED，确认旧实现仍停留在 `FileApi` 直链模式。
- Verification evidence:
  - `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\test\java\cn\iocoder\yudao\module\showroom\cover\ShowroomProductCoverImageServiceTest.java`
  - `mvn --% -pl yudao-module-showroom -Dtest=ShowroomProductCoverImageServiceTest -Dsurefire.failIfNoSpecifiedTests=false surefire:test`（RED）
- Remaining blockers:
  - 需要完成生产代码修复。

### Milestone 3

- Status: Completed
- Completed work:
  - 已将封面服务改为通过 `FileService.createFileAndReturnId(...)` 保存文件。
  - 已用 `FileService.getFile(...)` 读取 `configId/path` 并组装 `/admin-api/infra/file/{configId}/get/{path}`。
- Verification evidence:
  - `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\cover\ShowroomProductCoverImageService.java`
- Remaining blockers:
  - 待跑通 GREEN 和真实复验。

### Milestone 4

- Status: Completed
- Completed work:
  - 已确认主源码编译通过。
  - 已确认独立回归测试 4/4 通过。
  - 已重新打包 `yudao-server` 并重启本地运行实例。
  - 已复跑真实 Playwright，确认页面图片真实加载成功。
  - 已通过 backend evidence 校验与 closeout preview。
- Verification evidence:
  - `mvn --% -pl yudao-module-showroom -DskipTests -Dmaven.compiler.useIncrementalCompilation=false compile`
  - `mvn --% -pl yudao-module-showroom -Dtest=ShowroomProductCoverImageServiceTest -Dsurefire.failIfNoSpecifiedTests=false surefire:test`
  - `mvn --% -pl yudao-server -am -DskipTests package`
  - `D:\ProjectPackage\Int\IntRuoyi\restart-ruoyi.bat`
  - `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-cover-file-url-proxy\backend-api-evidence.md`
  - `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260521-showroom-cover-file-url-proxy --mode preview`
- Remaining blockers:
  - 待完成任务范围提交。

### Milestone 5

- Status: Completed
- Completed work:
  - 已将变更范围收敛到封面服务、独立回归测试和当前任务目录。
  - 已创建本任务独立 commit `5640297192`。
- Verification evidence:
  - `git status --short -- yudao-module-showroom/src/main/java/cn/iocoder/yudao/module/showroom/cover/ShowroomProductCoverImageService.java yudao-module-showroom/src/test/java/cn/iocoder/yudao/module/showroom/cover/ShowroomProductCoverImageServiceTest.java doc/tasks/20260521-showroom-cover-file-url-proxy`
  - `git commit -m "任务: 修复封面文件代理地址"`
- Remaining blockers:
  - None.

## Final Verification Result

- PASS: `mvn --% -pl yudao-module-showroom -DskipTests -Dmaven.compiler.useIncrementalCompilation=false compile`
- PASS: `mvn --% -pl yudao-module-showroom -Dtest=ShowroomProductCoverImageServiceTest -Dsurefire.failIfNoSpecifiedTests=false surefire:test`
- PASS: `mvn --% -pl yudao-server -am -DskipTests package`
- PASS: `D:\ProjectPackage\Int\IntRuoyi\restart-ruoyi.bat`
- PASS: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-cover-file-url-proxy\backend-api-evidence.md`
- PASS: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260521-showroom-cover-file-url-proxy --mode preview`
- PASS: 真实 Playwright 复验，`coverImage` 返回 `/admin-api/infra/file/28/get/showroom/product/cover/20260521/product-product_001-cover.png`，图片真实加载成功。
- PASS: `git commit -m "任务: 修复封面文件代理地址"` -> `5640297192`

## Current Status

- Status: Completed
- Completed work:
  - 已将封面回填从对象存储直链切换为 `/admin-api/infra/file/{configId}/get/{path}` 代理地址。
  - 已确认真实页面点击 `AI生成` 后图片可正常加载。
  - 已完成独立回归、后端校验、closeout preview 与任务范围提交。
- Remaining blockers:
  - None within this scope.
