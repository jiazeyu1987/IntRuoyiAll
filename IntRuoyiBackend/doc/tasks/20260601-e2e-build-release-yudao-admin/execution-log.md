# 执行日志：使用芋道源码 admin 真实数据 E2E 构建发布包

BDD: 芋道源码 admin 真实路径触发构建发布包 -> Given 本机前端可访问且使用 `芋道源码/admin` 登录 / When 在运行控制台点击构建发布包并确认 / Then 后端应创建真实运行控制操作并执行 `publish-int-ruoyi.ps1 -Mode build-release`。

BDD: 构建发布包门禁真实执行 -> Given 发布进程具备 NAS 与 eDHR S3 Object Lock 配置 / When 构建发布包执行 / Then eDHR verifier、构建、打包、NAS 上传必须真实通过，否则操作状态显示失败并记录真实错误。

GREEN: `Invoke-WebRequest http://localhost:48081/actuator/health` -> PASS，本机后端 HTTP 200。

GREEN: `Invoke-WebRequest http://localhost:8081/login?redirect=/index` -> PASS，本机前端登录页 HTTP 200。

GREEN: Playwright 登录路径 -> PASS，登录页显示租户 `芋道源码`、账号 `admin`，点击 `登录` 后进入 `/index`。

GREEN: Playwright 菜单路径 -> PASS，依次点击 `基础设施`、`监控中心`、`运行控制台`，进入 `/infra/monitors/runtime-control`。

GREEN: Playwright 提交构建发布包 -> PASS，在 `构建发布包` 对话框选择 `带数据发布`，发布包 `26-06-01 15:37:24`，点击 `确认执行`；运行控制台创建操作 `75bfe2fd-25ea-4c9d-8030-7d87479fdc14`。

VERIFY: `runtime/runtime-control/75bfe2fd-25ea-4c9d-8030-7d87479fdc14.json` -> PASS，`status=running`，`parameters.publishScope=with-data`，`parameters.releaseTag=26-06-01 15:37:24`。

GREEN: Runtime Control operation completion -> PASS，`operationId=75bfe2fd-25ea-4c9d-8030-7d87479fdc14`，`status=succeeded`，`summary=构建发布包 completed`。

GREEN: Build release script log -> PASS，日志记录 `Release package uploaded to NAS: Backup/ReleasePackage/26-06-01_15-37-24 (releaseTag=26-06-01 15:37:24)` 与 `Release package built: 26-06-01 15:37:24`。

GREEN: Playwright log dialog refresh -> PASS，前端运行控制台日志弹窗显示本次操作状态 `成功`。

VERIFY: `tmp/publish-int-ruoyi/26-06-01_15-37-24/release-manifest.json` -> PASS，`publishScope=with-data`，`artifactCount=2806`，`totalGB=13.93`，`minioCount=2760`，`minioGB=4.03`，`requiredSqlCount=4`。

GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260601-e2e-build-release-yudao-admin --mode preview` -> PASS，`delete=<none>`，`blocked=<none>`，`warnings=<none>`。
