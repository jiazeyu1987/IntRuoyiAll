# 20260608-e2e-no-website-release-test-backup

## 任务目标

使用本机运行控制台 E2E 操作，从当前本地 `int_main` 代码构建发布包，明确不包含 Website/展厅构筑包、不包含 OnlyOffice；然后继续通过运行控制台 E2E 操作将该发布包部署到测试服务器 `172.30.30.58` 和备份服务器 `172.30.30.59`。全程禁止访问或修改正式服务器代码和数据。

## 前置任务状态

- 已完成 `20260608-runtime-build-release-showroom-option`：运行控制台支持默认不发布展厅构筑包，并将组件范围写入 manifest。
- 已完成 `20260608-runtime-console-overview-all-error`：本机 `8081/48081` 已恢复为主仓库 `int_main`，本机状态脚本返回 running。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；发布包必须通过 manifest 证明 `component=intruoyi`、`includeShowroomBuildPackage=false`、`onlyOfficeIncluded=false`。
- `是否从根因和长期维护角度解决`：是；所有远端动作通过运行控制台和既有发布脚本执行，使用现有门禁和 manifest。
- `是否存在临时补丁或绕过`：否；不直接调用发布脚本绕过前端 E2E，不直接访问正式服务器。

## BDD 场景

- BDD: E2E 构建不含展厅和 OnlyOffice 的发布包 -> Given 本机运行控制台加载当前 `int_main` 代码 / When 运维点击“构建发布包”且保持“发布展厅构筑包”和“发布 OnlyOffice”未选中 / Then 构建命令使用 `-Component intruoyi`，发布包 manifest 记录 `includeShowroomBuildPackage=false` 和 `onlyOfficeIncluded=false`。
- BDD: E2E 部署发布包到测试服务器 -> Given 发布包构建成功且 manifest 完整 / When 运维通过运行控制台执行“部署发布包到测试服” / Then 测试服健康检查通过，部署记录显示成功。
- BDD: E2E 标记测试通过并部署到备份服务器 -> Given 测试服部署成功 / When 运维通过运行控制台标记测试通过并执行“上线备份服务器” / Then 备份服务器健康检查通过，部署记录显示成功，未访问正式服务器。
- BDD: 恢复候选接口有界返回 -> Given NAS 中存在大量历史备份点 / When 运行控制台加载恢复候选用于“标记测试通过” / Then 后端只扫描最近 5 个备份点，接口快速返回可用或阻断原因，不因旧备份无限枚举导致前端超时。
- BDD: 运行控制台 E2E 上线备份服 -> Given 发布包已部署测试服且标记测试通过 / When 运维通过运行控制台点击“上线备份服务器” / Then 备份服后端与管理前端容器使用同一 releaseTag，且发布包仍不包含 Website/展厅构筑包和 OnlyOffice。

## 里程碑

- [x] M0：解除非正式 eDHR protected storage 配置阻塞，确保测试/备份发布不继承正式 S3 endpoint。
- [x] M1：通过 E2E 预览确认当前构建请求不含 Website/OnlyOffice。
- [x] M1.1：恢复候选接口只扫描最近 5 个备份点，避免运行控制台加载历史恢复点时超时。
- [x] M2：通过 E2E 执行构建发布包并记录 releaseTag。
- [x] M3：读取发布包 manifest，确认不含 Website 和 OnlyOffice。
- [x] M4：通过 E2E 部署到测试服务器并验证健康。
- [x] M5：通过 E2E 标记测试通过并部署到备份服务器，验证健康。

## 预期验证

- Playwright 操作 `http://localhost:8081/infra/monitors/runtime-control`。
- NAS ReleasePackage manifest：`component=intruoyi`、`includeShowroomBuildPackage=false`、`onlyOfficeIncluded=false`。
- `http://172.30.30.58:48081/actuator/health` 返回 UP。
- `http://172.30.30.59:48081/actuator/health` 返回 UP。

## 当前状态

completed: 已通过运行控制台 E2E 构建并复用发布包 `26-06-08 16:11:25` / `26-06-08_16-11-25`，manifest 确认 `component=intruoyi`、`includeShowroomBuildPackage=false`、`onlyOfficeIncluded=false`。测试服与备份服均已部署该 releaseTag，后端与管理前端健康检查通过。

最终验证：E2E 结果文件 `runtime-control-e2e-release-test-backup-result.json` 记录 `BACKUP_DEPLOY_OPERATION=104f6a98-62ef-44d0-b494-9550e873b4a5` 且 `logStatus=succeeded`；测试服 `172.30.30.58` 与备份服 `172.30.30.59` 的 `/opt/intruoyi/runtime/.env` 均为 `IMAGE_TAG=26-06-08_16-11-25`，`intruoyi-backend` 与 `intruoyi-frontend` 容器镜像均为该 tag。

## Current Status

completed: 已完成；详见“当前状态”和 `execution-log.md`。

## 阻塞项

- 当前无阻塞项。

## unblock 方案

- 已实施：发布脚本增加目标环境绑定变量解析，优先读取 `EDHR_S3_TEST_*`、`EDHR_S3_BACKUP_*`、`EDHR_S3_PROD_*`，再回退通用 `EDHR_S3_*`。
- 已实施：`runtime-env/test.env`、`runtime-env/backup.env` 写入对应目标环境 eDHR 配置；`deploy-release` 继续从 manifest 包内目标 env 读取，不绕过门禁。
- 已验证：测试服/备份服只使用各自非正式 MinIO / Object Lock bucket 通过 verifier，禁止使用 `172.30.30.57:9000`。
