# 执行日志：实现内部基础镜像日常发布包构建

## BDD

- BDD: 日常发布包使用内部基础镜像 -> Given 内部基础镜像 tar、tar sha256、镜像名、版本和 image id 已配置 / When 运维执行 `publish-int-ruoyi.ps1 -Mode build-release` / Then 脚本先离线加载并校验基础镜像，再用 `--build-arg BACKEND_RUNTIME_BASE_IMAGE` 构建后端业务镜像。
- BDD: 日常后端 Dockerfile 不访问外部 apt 源 -> Given 已存在受控基础镜像构建流程 / When 查看日常 `Dockerfile.backend` / Then 文件只从内部基础镜像复制 jar，不包含 `apt-get`、`docker.io` 或 `docker-compose-v2` 安装步骤。
- BDD: 缺少内部基础镜像前置条件必须失败 -> Given 发布配置缺少任一基础镜像参数或 tar/image 校验不匹配 / When 执行 `build-release` 且包含后端组件 / Then 脚本直接 fail fast，并说明缺少或不匹配的前置条件，不回退到外部源或旧缓存镜像。
- BDD: Runtime Control 使用服务器侧基础镜像配置 -> Given Runtime Control 服务器配置了基础镜像离线参数 / When 运维页面触发“构建发布包” / Then 后端命令拼接自动带上基础镜像参数，不要求前端临时输入。

## TDD Evidence

- RED: `python -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py::test_publish_dockerfiles_point_at_current_workspace_artifacts script/tests/test_publish_int_ruoyi_to_test_tooling.py::test_publish_script_loads_and_verifies_internal_backend_runtime_base_image script/tests/test_runtime_control_ops_scripts.py::test_test_server_compose_mounts_linux_backup_ops_runtime_prerequisites` -> FAIL, expected reason: 当前缺少 `Dockerfile.backend-base`、日常 Dockerfile 仍包含 apt 工具安装、发布脚本缺少内部基础镜像参数和离线校验函数。
- RED: `mvn -pl yudao-module-infra -Dtest=RuntimeControlServiceImplTest#executeBuildReleaseShouldPassBackendRuntimeBaseConfigFromProperties test` -> FAIL, expected reason: `RuntimeControlProperties.ReleasePackage` 尚无 `BackendRuntimeBase*` 配置字段。
- GREEN: PowerShell parse for `script/deploy/publish-int-ruoyi.ps1` -> PASS。
- GREEN: PowerShell parse for `script/deploy/build-backend-runtime-base-image.ps1` -> PASS。
- GREEN: `python -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py::test_build_release_backend_e2e_fails_fast_without_internal_backend_runtime_base_config -q` -> PASS，真实调用发布脚本并验证缺少内部基础镜像配置时 fail fast。
- GREEN: `python -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py script/tests/test_runtime_control_ops_scripts.py` -> PASS，66 passed。
- GREEN: `powershell.exe -NoProfile -ExecutionPolicy Bypass -File script/deploy/publish-int-ruoyi.ps1 -Mode build-release -Component backend -ReleaseTag 20260604_internal_base_red -SkipDatabaseSync -SkipMinioSync` -> FAIL as expected，缺少 `BackendRuntimeBaseMode` 时直接阻塞，exit code 1。
- GREEN: `mvn -pl yudao-module-infra -Dtest=RuntimeControlServiceImplTest#executeBuildReleaseShouldPassBackendRuntimeBaseConfigFromProperties test` -> PASS。
- GREEN: `mvn -pl yudao-module-infra -Dtest=RuntimeControlServiceImplTest test` -> PASS，43 tests。
- GREEN: Runtime Control container/local config binding -> PASS，`docker-compose.yml` 和 `application-local.yaml` 均显式绑定 `backend-runtime-base-*`，页面触发构建发布包时读取服务器侧配置。
- GREEN: `python -X utf8 -m pytest script/tests/test_runtime_control_ops_scripts.py::test_test_server_compose_mounts_linux_backup_ops_runtime_prerequisites -q` -> PASS，覆盖 compose 与 `application-local.yaml` 的基础镜像配置绑定。
- GREEN: `mvn -pl yudao-module-infra -Dtest=RuntimeControlServiceImplTest#defaultRuntimeControlPropertiesShouldSeparateReleaseAndBackupNasRoots test` -> PASS，覆盖 `ReleasePackage` 基础镜像配置字段默认不伪造值。
- GREEN: `python C:\Users\BJB110\.codex\skills\ci-cd-environment-delivery\scripts\validate_cicd_environment.py --evidence doc/tasks/20260604-internal-backend-base-image-daily-build-implementation/ci-cd-evidence.md` -> PASS。
- GREEN: `git diff --check -- <本任务变更文件>` -> PASS。
- GREEN: `python -X utf8 tool/verify_tdd_compliance.py --task-dir doc/tasks/20260604-internal-backend-base-image-daily-build-implementation --paths ...` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260604-internal-backend-base-image-daily-build-implementation --mode preview` -> PASS，keep 三项，delete/blocked/warnings 均为空。
- NOTE: 未执行真实基础镜像 Docker build；缺少正式内部基础镜像 tar、sha256、image id 参数时本任务要求 fail fast，真实构建待运维提供前置条件。
