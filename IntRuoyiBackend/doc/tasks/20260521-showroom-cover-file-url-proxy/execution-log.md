# Execution Log: 展厅封面回填改为可访问文件代理地址

BDD: 生成封面后回填可访问代理地址 -> Given 管理员对已发布产品触发真实 AI 封面生成 / When 后端完成图片上传 / Then 返回给前端的 `coverImage` 必须是 `/admin-api/infra/file/{configId}/get/{path}` 代理地址，而不是可能受权限限制的对象存储直链。

BDD: 图片生成仍走真实 Codex CLI -> Given 本次仅修复文件访问链路 / When 后端生成封面 / Then 仍应保持 Codex CLI 原生图片生成逻辑不变，只调整文件保存后的 URL 回填方式。

RED: `mvn --% -pl yudao-module-showroom -Dtest=ShowroomProductCoverImageServiceTest -Dsurefire.failIfNoSpecifiedTests=false surefire:test` -> FAIL，回归测试切换到 `FileService.createFileAndReturnId(...) + getFile(...)` 后，现有服务构造器仍只接收 `FileApi`，无法满足“回填 `/admin-api/infra/file/...` 代理地址”的新契约。

GREEN: `mvn --% -pl yudao-module-showroom -DskipTests -Dmaven.compiler.useIncrementalCompilation=false compile` -> PASS。

GREEN: `mvn --% -pl yudao-module-showroom -Dtest=ShowroomProductCoverImageServiceTest -Dsurefire.failIfNoSpecifiedTests=false surefire:test` -> PASS，4 tests green，已验证：
- 仍走 Codex CLI 生成本地 PNG；
- 文件改为通过 `FileService` 保存；
- 返回值改为 `/admin-api/infra/file/{configId}/get/{path}`；
- 缺失图片文件仍 fail-fast。

GREEN: 真实后端重启后复跑 Playwright -> PASS，`product_001` 点击 `AI生成` 返回：
- `coverImage=/admin-api/infra/file/28/get/showroom/product/cover/20260521/product-product_001-cover.png`
- 浏览器图片状态：`complete=true`, `naturalWidth=1254`, `naturalHeight=1254`

GREEN: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-cover-file-url-proxy\backend-api-evidence.md` -> PASS。

GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260521-showroom-cover-file-url-proxy --mode preview` -> PASS，preview 状态 `ready`。

GREEN: `git commit -m "任务: 修复封面文件代理地址"` with `TDD_TASK_DIR=D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-cover-file-url-proxy` -> PASS，创建 commit `5640297192`。
