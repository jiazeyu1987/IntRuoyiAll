# 任务：实现内部基础镜像日常发布包构建

## 任务目标

将 IntRuoyi 日常 `build-release` 后端业务镜像构建改为使用公司内部固定后端运行时基础镜像。发布包构建时只加载并校验离线基础镜像 tar，再基于该镜像复制当前 `yudao-server.jar`，不再在日常业务镜像 Dockerfile 中访问 Docker Hub 或 Ubuntu apt 源安装运行时工具。

## Previous Task Check

- 上一个同服务仓库任务：`doc/tasks/20260604-internal-backend-base-image-packaging-design/task.md`
- 状态：`completed`
- 处理：本任务按已完成的设计文档与 ADR 落地日常打包链路改造；上一任务中被阻塞的直接改外部 LTS Dockerfile 方案不再采用。

## BDD 场景

- BDD: 日常发布包使用内部基础镜像 -> Given 内部基础镜像 tar、tar sha256、镜像名、版本和 image id 已配置 / When 运维执行 `publish-int-ruoyi.ps1 -Mode build-release` / Then 脚本先离线加载并校验基础镜像，再用 `--build-arg BACKEND_RUNTIME_BASE_IMAGE` 构建后端业务镜像。
- BDD: 日常后端 Dockerfile 不访问外部 apt 源 -> Given 已存在受控基础镜像构建流程 / When 查看日常 `Dockerfile.backend` / Then 文件只从内部基础镜像复制 jar，不包含 `apt-get`、`docker.io` 或 `docker-compose-v2` 安装步骤。
- BDD: 缺少内部基础镜像前置条件必须失败 -> Given 发布配置缺少任一基础镜像参数或 tar/image 校验不匹配 / When 执行 `build-release` 且包含后端组件 / Then 脚本直接 fail fast，并说明缺少或不匹配的前置条件，不回退到外部源或旧缓存镜像。
- BDD: Runtime Control 使用服务器侧基础镜像配置 -> Given Runtime Control 服务器配置了基础镜像离线参数 / When 运维页面触发“构建发布包” / Then 后端命令拼接自动带上基础镜像参数，不要求前端临时输入。

## Milestones

- [x] M1：创建任务文档并确认上一任务已 completed。
- [x] M2：先更新测试断言并记录 RED。
- [x] M3：实现日常 Dockerfile、基础镜像骨架、发布脚本与 Runtime Control 配置改造。
- [x] M4：运行 Python 与 Java 回归验证并记录 GREEN。
- [x] M5：运行 task-closeout-cleanup 预览，提交本任务改动。

## Expected Verification

- `python -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py script/tests/test_runtime_control_ops_scripts.py`
- `mvn -pl yudao-module-infra -Dtest=RuntimeControlServiceImplTest test`
- `powershell.exe -NoProfile -ExecutionPolicy Bypass -File script/deploy/publish-int-ruoyi.ps1 -Mode build-release -Component backend -ReleaseTag 20260604_internal_base_red -IncludeOnlyOffice -SkipDatabaseSync -SkipMinioSync` 缺少基础镜像配置时应 fail fast。
- `git diff --check -- <本任务变更文件>`

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。基础镜像参数缺失、tar 不存在、tar sha256 不匹配、镜像 image id 不匹配或 Docker 不可用均直接失败。
- `是否从根因和长期维护角度解决`：是。将外部源访问集中到受控基础镜像构建流程，日常业务发布包构建只依赖已验证的内部离线基础镜像。
- `是否存在临时补丁或绕过`：否。本任务不增加外部镜像源替换、旧缓存复用或跳过校验的临时路径。

## 当前状态

completed

## Current Status

completed

## 验证结果

- RED：`python -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py::test_publish_dockerfiles_point_at_current_workspace_artifacts script/tests/test_publish_int_ruoyi_to_test_tooling.py::test_publish_script_loads_and_verifies_internal_backend_runtime_base_image script/tests/test_runtime_control_ops_scripts.py::test_test_server_compose_mounts_linux_backup_ops_runtime_prerequisites` -> FAIL，当前缺少 `Dockerfile.backend-base`、日常 Dockerfile 仍安装 apt 运行时工具、发布脚本缺少内部基础镜像参数和校验函数。
- RED：`mvn -pl yudao-module-infra -Dtest=RuntimeControlServiceImplTest#executeBuildReleaseShouldPassBackendRuntimeBaseConfigFromProperties test` -> FAIL，`RuntimeControlProperties.ReleasePackage` 尚无内部基础镜像配置字段。
- GREEN：PowerShell parse for `script/deploy/publish-int-ruoyi.ps1` -> PASS。
- GREEN：PowerShell parse for `script/deploy/build-backend-runtime-base-image.ps1` -> PASS。
- GREEN：`python -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py script/tests/test_runtime_control_ops_scripts.py` -> PASS，66 passed，包含真实脚本级 E2E：缺少内部基础镜像配置时 `build-release -Component backend` 立即 fail fast。
- GREEN：`powershell.exe -NoProfile -ExecutionPolicy Bypass -File script/deploy/publish-int-ruoyi.ps1 -Mode build-release -Component backend -ReleaseTag 20260604_internal_base_red -IncludeOnlyOffice -SkipDatabaseSync -SkipMinioSync` -> FAIL as expected，输出 `Missing BackendRuntimeBaseMode`，未进入 Maven 或 Docker build，exit code 1。
- GREEN：`mvn -pl yudao-module-infra -Dtest=RuntimeControlServiceImplTest#executeBuildReleaseShouldPassBackendRuntimeBaseConfigFromProperties test` -> PASS。
- GREEN：`mvn -pl yudao-module-infra -Dtest=RuntimeControlServiceImplTest test` -> PASS，43 tests。
- GREEN：Runtime Control container/local config binding -> PASS，`docker-compose.yml` 和 `application-local.yaml` 均显式绑定 `backend-runtime-base-*`，页面触发构建发布包时读取服务器侧配置。
- GREEN：`python C:\Users\BJB110\.codex\skills\ci-cd-environment-delivery\scripts\validate_cicd_environment.py --evidence doc/tasks/20260604-internal-backend-base-image-daily-build-implementation/ci-cd-evidence.md` -> PASS。
- GREEN：`git diff --check -- <本任务变更文件>` -> PASS。
- GREEN：`python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260604-internal-backend-base-image-daily-build-implementation --mode preview` -> PASS，keep 三项，delete/blocked/warnings 均为空。
- NOTE：本任务未执行真实基础镜像 Docker build；缺少正式内部基础镜像 tar、sha256、image id 参数时必须阻断，真实构建需在运维提供这些前置条件后执行。

## Cleanup Keep

- `doc/tasks/20260604-internal-backend-base-image-daily-build-implementation/task.md`
- `doc/tasks/20260604-internal-backend-base-image-daily-build-implementation/execution-log.md`
- `doc/tasks/20260604-internal-backend-base-image-daily-build-implementation/ci-cd-evidence.md`
