# 任务：配置本机内部后端基础镜像参数

## 任务目标

为 IntRuoyi 本机发布包构建配置真实的内部后端运行时基础镜像参数。生成 `intruoyi-backend-runtime-base:<version>` 离线 tar、tar sha256、Docker image id，并把这些值写入本机用户环境变量，使 Runtime Control 和 `publish-int-ruoyi.ps1 -Mode build-release` 能读取服务器侧配置，不需要用户手工填写代码参数。

## Previous Task Check

- 上一个同服务仓库任务：`doc/tasks/20260604-internal-backend-base-image-daily-build-implementation/task.md`
- 状态：`completed`
- 处理：上一任务已实现配置入口、Dockerfile、发布脚本和 Runtime Control 参数拼接；本任务只落地本机配置值，不改远程服务器。

## BDD 场景

- BDD: 本机基础镜像参数可被自动配置 -> Given Docker 可用且基础镜像构建脚本可运行 / When 执行本任务配置 / Then 本机生成基础镜像 tar、sha256、image id，并写入 `INTRUOYI_BACKEND_RUNTIME_BASE_*` 用户环境变量。
- BDD: 发布脚本读取真实配置 -> Given 本机环境变量已配置 / When 执行 `publish-int-ruoyi.ps1 -Mode build-release -Component backend` / Then 脚本不再因缺少基础镜像参数失败，而是加载并校验离线 tar 后继续进入后续构建前置检查。
- BDD: 基础镜像构建失败必须阻塞 -> Given Docker Hub 或 Ubuntu apt 源不可访问 / When 构建内部基础镜像 / Then 任务必须报告缺失前置条件，不写入假参数，不用旧缓存或外部 fallback。

## Milestones

- [x] M1：确认上一任务 completed，确认配置入口已存在。
- [x] M2：构建或发现本机内部基础镜像 tar 与 manifest。
- [x] M3：写入本机用户环境变量并验证读取。
- [x] M4：运行发布脚本基础镜像校验路径和配置证据验证。
- [x] M5：记录收尾、cleanup 预览并提交任务记录。

## Expected Verification

- `powershell.exe -NoProfile -ExecutionPolicy Bypass -File script/deploy/build-backend-runtime-base-image.ps1 -Version <version>`
- `powershell.exe -NoProfile -ExecutionPolicy Bypass -File script/deploy/publish-int-ruoyi.ps1 -Mode build-release -Component backend -ReleaseTag <tag> -SkipDatabaseSync -SkipMinioSync`
- `python -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py::test_build_release_backend_e2e_fails_fast_without_internal_backend_runtime_base_config`
- `git diff --check -- <本任务文档>`

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。构建失败或配置值缺失时直接阻塞。
- `是否从根因和长期维护角度解决`：是。用正式基础镜像构建脚本生成可追溯配置值，并写入本机环境变量。
- `是否存在临时补丁或绕过`：否。不写假 digest、不跳过 tar sha256、不使用旧缓存镜像冒充正式基础镜像。

## 当前状态

completed

## Current Status

completed

## 验证结果

- AUTH：2026-06-05 用户明确授权“在运行控制台点击构建发布包”，允许通过本机运行控制台真实触发 `build-release` 并上传 NAS 发布包。
- RED：`powershell.exe -NoProfile -ExecutionPolicy Bypass -File script/deploy/build-backend-runtime-base-image.ps1 -Version 2026.06.05-jre21-noble-docker29.2.1` -> FAIL，Ubuntu apt 源下载 `runc`、`containerd`、`docker-compose-v2`、`docker.io` 时返回 `502 Bad Gateway` 与 `Connection failed`。
- VERIFY：HEAD 检查确认 `docker.io_29.1.3-0ubuntu3~24.04.2_amd64.deb` 在 `security.ubuntu.com` 失败，但同路径在 `archive.ubuntu.com`、阿里云、华为云可达。
- RED：基础镜像 Dockerfile 尚未显式把 `security.ubuntu.com` 固定到可达的 `archive.ubuntu.com`，也未声明 apt 重试策略。
- GREEN：`python -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py::test_publish_dockerfiles_point_at_current_workspace_artifacts -q` -> PASS，基础镜像 Dockerfile 已显式固定 `archive.ubuntu.com` 并声明 apt 重试策略。
- RED：`python -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py::test_publish_dockerfiles_point_at_current_workspace_artifacts -q` -> FAIL，基础镜像 Dockerfile 尚未声明可配置的 `APT_MIRROR`，无法稳定避开 Docker 构建中的 `archive.ubuntu.com` 长连接失败。
- GREEN：`python -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py::test_publish_dockerfiles_point_at_current_workspace_artifacts -q` -> PASS，基础镜像 Dockerfile 已声明 `APT_MIRROR=http://mirrors.aliyun.com/ubuntu`，并同时替换 `security.ubuntu.com` 与 `archive.ubuntu.com`。
- GREEN：`powershell.exe -NoProfile -ExecutionPolicy Bypass -File script/deploy/build-backend-runtime-base-image.ps1 -Version 2026.06.05-jre21-noble-docker29.2.1` -> PASS，生成 `D:\ProjectPackage\Int\BaseImages\intruoyi-backend-runtime-base-2026.06.05-jre21-noble-docker29.2.1.tar` 与 manifest。
- GREEN：本机用户环境变量已写入 `INTRUOYI_BACKEND_RUNTIME_BASE_*` 与 `RUNTIME_CONTROL_BACKEND_RUNTIME_BASE_*` 两组配置。
- GREEN：离线 tar 校验通过，`tarSha256=5bcd568b46ba9f28bfa7ac8ee67e283aea95bd585a89842a14cc40827fa71603`，`imageId=sha256:b4f7d85f325665c3b372379e6d352c3c8be2d3a08add68ac0dc151f720160be7`。
- GREEN：`python -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py script/tests/test_runtime_control_ops_scripts.py -q` -> PASS，66 passed。
- GREEN：`mvn -pl yudao-module-infra -Dtest=RuntimeControlServiceImplTest test` -> PASS，43 tests passed。
- GREEN：`git diff --check -- script/deploy/int-ruoyi-test/Dockerfile.backend-base script/tests/test_publish_int_ruoyi_to_test_tooling.py doc/tasks/20260605-backend-runtime-base-local-config/task.md doc/tasks/20260605-backend-runtime-base-local-config/execution-log.md` -> PASS。
- GREEN：`python C:\Users\BJB110\.codex\skills\ci-cd-environment-delivery\scripts\validate_cicd_environment.py --evidence docs\environments\ci-cd-evidence.md` -> PASS。
- GREEN：`python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260605-backend-runtime-base-local-config --mode preview` -> PASS，delete none，blocked none。
- GREEN：`python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260605-backend-runtime-base-local-config --mode apply` -> PASS，deleted none，blocked none。
- GREEN：完成审计复跑 `python -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py script/tests/test_runtime_control_ops_scripts.py -q` -> PASS，66 passed。
- GREEN：完成审计复跑 `mvn -pl yudao-module-infra -Dtest=RuntimeControlServiceImplTest test` -> PASS，43 tests passed。
- GREEN：完成审计复核本机 tar、用户环境变量与 Docker image id -> PASS，当前 `tarSha256=5bcd568b46ba9f28bfa7ac8ee67e283aea95bd585a89842a14cc40827fa71603`，`imageId=sha256:b4f7d85f325665c3b372379e6d352c3c8be2d3a08add68ac0dc151f720160be7`。
- GREEN：完成审计执行 `docker build --no-cache --build-arg BACKEND_RUNTIME_BASE_IMAGE=intruoyi-backend-runtime-base:2026.06.05-jre21-noble-docker29.2.1 -t intruoyi-backend:codex-internal-base-verify -f script/deploy/int-ruoyi-test/Dockerfile.backend D:\ProjectPackage\Int\IntRuoyi` -> PASS，业务后端镜像只执行 `mkdir/WORKDIR/COPY`，临时验证镜像已删除。

## 配置结果

- `INTRUOYI_BACKEND_RUNTIME_BASE_MODE=offline-tar`
- `INTRUOYI_BACKEND_RUNTIME_BASE_TAR=D:\ProjectPackage\Int\BaseImages\intruoyi-backend-runtime-base-2026.06.05-jre21-noble-docker29.2.1.tar`
- `INTRUOYI_BACKEND_RUNTIME_BASE_TAR_SHA256=5bcd568b46ba9f28bfa7ac8ee67e283aea95bd585a89842a14cc40827fa71603`
- `INTRUOYI_BACKEND_RUNTIME_BASE_IMAGE=intruoyi-backend-runtime-base:2026.06.05-jre21-noble-docker29.2.1`
- `INTRUOYI_BACKEND_RUNTIME_BASE_DIGEST=sha256:b4f7d85f325665c3b372379e6d352c3c8be2d3a08add68ac0dc151f720160be7`
- `INTRUOYI_BACKEND_RUNTIME_BASE_VERSION=2026.06.05-jre21-noble-docker29.2.1`
- `RUNTIME_CONTROL_BACKEND_RUNTIME_BASE_*` 已写入同一组值，供 Runtime Control 容器命令读取。
- 真实 `build-release` 仍需要 NAS 配置与发布授权；本任务未操作 NAS 共享盘。

## Cleanup Keep

- `doc/tasks/20260605-backend-runtime-base-local-config/task.md`
- `doc/tasks/20260605-backend-runtime-base-local-config/execution-log.md`
