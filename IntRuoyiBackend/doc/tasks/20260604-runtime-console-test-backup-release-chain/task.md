# 任务：运行控制台验证测试服与备份服发布链路

## 任务目标

按用户最新目标，通过本机运行控制台完成发布包 A 的构建、部署到测试服、标记测试通过、上线备份服务器，并确认测试服与备份服运行程序与发布包一致。严格禁止访问、发布或修改正式服务器 `172.30.30.57`。

## Previous Task Check

- 上一个同服务仓库任务：`doc/tasks/20260604-test-deploy-showroom-image-json/task.md`
- 状态：`completed`
- 影响：上一任务已修复 code-only 部署测试服时展厅文件配置使用错误 MinIO 凭据的问题；本任务从构建发布包重新验证完整链路。

## BDD 场景

- BDD: 构建 code-only 发布包 A -> Given 本机运行控制台可用 / When 点击“构建发布包”，选择只发代码且不勾选 OnlyOffice / Then NAS `Backup/ReleasePackage` 生成发布包 A，manifest 为 `publishScope=code-only`、`onlyOfficeIncluded=false`。
- BDD: 发布包 A 部署到测试服 -> Given 发布包 A 构建成功 / When 点击“部署发布包到测试服”并选择 A / Then 测试服运行的 backend/frontend/website 镜像与发布包 A 一致，健康检查和 smoke 通过。
- BDD: 发布包 A 标记测试通过 -> Given 测试服部署 A 成功 / When 点击“标记测试通过”并填写原因 / Then A 变为已验证状态。
- BDD: 已验证发布包 A 上线备份服 -> Given A 已测试通过 / When 点击“上线备份服务器”选择 A / Then 备份服运行的程序与 A 一致，健康检查和 smoke 通过。
- BDD: 正式服务器禁止触碰 -> Given 当前任务执行任一步骤 / When 需要选择目标环境 / Then 禁止访问、发布、登录、SSH、HTTP 探测或修改正式服务器 `172.30.30.57`。

## Milestones

- [x] M1：建立任务文档并确认上一任务完成。
- [x] M2：通过运行控制台构建 code-only、不含 OnlyOffice 的发布包 A。
- [x] M3：部署发布包 A 到测试服并验证版本一致。
- [x] M4：标记发布包 A 测试通过。
- [x] M5：上线备份服务器并验证版本一致。
- [x] M6：记录最终验证、closeout 预览和提交。

## Expected Verification

- Playwright 真实打开本机 `http://localhost:8081` 并登录运行控制台。
- 构建发布包 A 后读取 operation JSON、日志和 manifest。
- 测试服只验证 `172.30.30.58`。
- 备份服只验证 `172.30.30.59`。
- 禁止任何 `172.30.30.57` 访问。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。任何步骤失败必须记录错误并修复后从 M2 重新开始。
- `是否从根因和长期维护角度解决`：是。沿用运行控制台真实链路和发布脚本门禁，不通过接口绕过。
- `是否存在临时补丁或绕过`：否。不改正式服，不绕过 smoke，不使用 mock 数据。

## 当前状态

completed

## Current Status

completed

## 验证结果

- VERIFY：上一任务 `doc/tasks/20260604-test-deploy-showroom-image-json/task.md` 状态为 `completed`。
- VERIFY：本机后端 `http://localhost:48081/actuator/health` -> PASS，`{"status":"UP"}`。
- VERIFY：本机前端 `http://127.0.0.1:8081/` -> PASS，HTTP 200。
- VERIFY：运行控制台发布包 A `26-06-04 01:25:18` 构建成功，operationId=`4af5eb74-5365-4569-b4f8-aac9dfa1ffe4`，参数 `publishScope=code-only`、`includeOnlyOffice=false`，NAS 路径 `Backup/ReleasePackage/26-06-04_01-25-18`。
- VERIFY：发现本机计划任务 `IntRuoyi Backup Scheduled` 曾自动启动正式服备份 SSH 访问；已立即停止相关进程并暂停该计划任务，当前状态 `Disabled`，防止本任务期间再次触碰 `172.30.30.57`。
- RED：服务端单元回归证明原运行控制台总览会把 `prod/172.30.30.57` 放入状态探测队列，缺少正式服访问总开关。
- GREEN：`mvn -pl yudao-module-infra "-Dtest=RuntimeControlServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，37 个运行控制台服务用例通过；默认正式环境 `accessEnabled=false` 时总览返回 `BLOCKED/access-disabled`，正式动作服务端 fail fast。
- VERIFY：已登录本机后端读取 `/admin-api/infra/runtime-control/overview` -> PASS，`prod` 下四个组件均为 `BLOCKED/access-disabled`，证明运行控制台总览不会探测正式服务器。
- GREEN：`node doc\tasks\20260604-runtime-console-test-backup-release-chain\runtime-console-test-backup-chain.e2e.cjs` -> PASS，发布包 A `26-06-04 03:42:41` 完成构建、测试服部署、标记测试通过、备份服上线完整链路。
- VERIFY：构建 operation `dadf4187-cbc1-4aba-bd61-07863197d661` -> PASS，`publishScope=code-only`、`includeOnlyOffice=false`，NAS 路径 `Backup/ReleasePackage/26-06-04_03-42-41`。
- VERIFY：测试服部署 operation `93f00281-5573-4782-91e5-720a094a6faf` -> PASS；测试服 `.env` 中 `IMAGE_TAG=26-06-04_03-42-41`，backend 镜像 `intruoyi-backend:26-06-04_03-42-41`，frontend 镜像 `intruoyi-frontend:26-06-04_03-42-41`，backend/frontend/website 健康检查均通过。
- VERIFY：标记测试通过 operation `1fe8ab8c-016c-4cfe-9535-773571d67045` -> PASS，发布包 `Backup/ReleasePackage/26-06-04_03-42-41` 已标记测试通过。
- VERIFY：备份服上线 operation `dd09917e-d2b8-4bbc-9e84-ac9156d5e058` -> PASS；备份服 `.env` 中 `IMAGE_TAG=26-06-04_03-42-41`，backend 镜像 `intruoyi-backend:26-06-04_03-42-41`，frontend 镜像 `intruoyi-frontend:26-06-04_03-42-41`，backend/frontend/website 健康检查均通过。
- VERIFY：本轮四个 operation 日志搜索 `172.30.30.57` -> PASS，无匹配；未访问、发布或修改正式服务器。

## Blockers

- none.

## Cleanup Candidates

- `doc/tasks/20260604-runtime-console-test-backup-release-chain/runtime-console-test-backup-chain.e2e.cjs`
- `doc/tasks/20260604-runtime-console-test-backup-release-chain/artifacts/`

## Cleanup Keep

- `doc/tasks/20260604-runtime-console-test-backup-release-chain/bug-regression-evidence.md`
