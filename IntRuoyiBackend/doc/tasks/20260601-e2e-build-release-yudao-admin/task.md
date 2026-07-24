# 任务：使用芋道源码 admin 真实数据 E2E 构建发布包

## 任务目标

- 使用本机前端 `http://localhost:8081`，以 `芋道源码/admin` 真实登录身份进入运行控制台。
- 通过 Playwright 操作真实页面触发一次 `release/ops/build-release` 构建发布包。
- 不使用接口代点、不使用 mock 数据、不绕过运行控制台与发布脚本门禁。

## 前序任务检查

- 已确认上一任务 `doc/tasks/20260601-release-edhr-s3-env-config/task.md` 状态为 `completed`。
- 当前仓库存在其他未提交改动与 `runtime/` 运行产物，本任务只记录本次 E2E 发布构建证据，不提交无关改动或敏感运行配置。

## BDD 场景

- BDD: 芋道源码 admin 真实路径触发构建发布包 -> Given 本机前端可访问且使用 `芋道源码/admin` 登录 / When 在运行控制台点击构建发布包并确认 / Then 后端应创建真实运行控制操作并执行 `publish-int-ruoyi.ps1 -Mode build-release`。
- BDD: 构建发布包门禁真实执行 -> Given 发布进程具备 NAS 与 eDHR S3 Object Lock 配置 / When 构建发布包执行 / Then eDHR verifier、构建、打包、NAS 上传必须真实通过，否则操作状态显示失败并记录真实错误。

## 里程碑

- [x] M1：建立任务文档、BDD 场景与预期验证。
- [x] M2：确认本机前后端、登录身份与发布配置前置条件。
- [x] M3：使用 Playwright 登录并从运行控制台触发构建发布包。
- [x] M4：等待操作完成，记录发布包标签、NAS 路径与操作状态。
- [x] M5：收尾清理预览并提交本任务非敏感记录。

## 预期验证

- Playwright 真实打开 `http://localhost:8081/login?redirect=/index` 并登录。
- 运行控制台操作列表出现本次 `构建发布包` 操作。
- 本次操作最终状态为 `succeeded`，或失败时记录真实失败原因并停止。

## 当前状态

status: completed

## 已完成工作

- 2026-06-01 15:38 左右，Playwright 打开本机前端登录页，页面租户为 `芋道源码`，账号为 `admin`。
- 通过真实菜单路径进入 `基础设施 / 监控中心 / 运行控制台`。
- 在运行控制台点击 `构建发布包`，选择 `带数据发布`，提交发布包 `26-06-01 15:37:24`。
- 后端创建真实操作 `75bfe2fd-25ea-4c9d-8030-7d87479fdc14`，`parameters.publishScope=with-data`。
- 运行控制台操作最终状态为 `succeeded`，摘要为 `构建发布包 completed`。
- 发布包目录：`Backup/ReleasePackage/26-06-01_15-37-24`。
- 本地 manifest 记录 `publishScope=with-data`，共 2806 个 artifact，总体约 13.93GB；其中 MinIO 快照 2760 个文件约 4.03GB，required SQL 4 个。
- task-closeout-cleanup preview 通过：无删除项、无阻塞、无告警。

## 最终验证

- PASS：Playwright 以 `芋道源码/admin` 真实登录路径进入运行控制台并提交构建发布包。
- PASS：运行控制台操作 `75bfe2fd-25ea-4c9d-8030-7d87479fdc14` 最终 `succeeded`。
- PASS：发布脚本日志记录发布包已上传到 `Backup/ReleasePackage/26-06-01_15-37-24`。
- PASS：本地 manifest 确认为 `with-data` 发布包，包含数据库 dump、MinIO 快照、required SQL 与 website artifacts。
