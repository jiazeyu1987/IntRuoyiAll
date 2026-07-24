# 执行日志：配置本机内部后端基础镜像参数

## BDD

- BDD: 本机基础镜像参数可被自动配置 -> Given Docker 可用且基础镜像构建脚本可运行 / When 执行本任务配置 / Then 本机生成基础镜像 tar、sha256、image id，并写入 `INTRUOYI_BACKEND_RUNTIME_BASE_*` 用户环境变量。
- BDD: 发布脚本读取真实配置 -> Given 本机环境变量已配置 / When 执行 `publish-int-ruoyi.ps1 -Mode build-release -Component backend` / Then 脚本不再因缺少基础镜像参数失败，而是加载并校验离线 tar 后继续进入后续构建前置检查。
- BDD: 基础镜像构建失败必须阻塞 -> Given Docker Hub 或 Ubuntu apt 源不可访问 / When 构建内部基础镜像 / Then 任务必须报告缺失前置条件，不写入假参数，不用旧缓存或外部 fallback。

## TDD Evidence

- AUTH: 2026-06-05 用户明确授权“在运行控制台点击构建发布包”，允许通过本机运行控制台真实触发 `build-release` 并上传 NAS 发布包。
- RED: `powershell.exe -NoProfile -ExecutionPolicy Bypass -File script/deploy/build-backend-runtime-base-image.ps1 -Version 2026.06.05-jre21-noble-docker29.2.1` -> FAIL, Ubuntu apt 源下载 `runc`、`containerd`、`docker-compose-v2`、`docker.io` 时返回 `502 Bad Gateway` 与 `Connection failed`。
- VERIFY: HEAD 检查确认 `docker.io_29.1.3-0ubuntu3~24.04.2_amd64.deb` 在 `security.ubuntu.com` 失败，但同路径在 `archive.ubuntu.com`、阿里云、华为云可达。
- RED: 基础镜像 Dockerfile 尚未显式把 `security.ubuntu.com` 固定到可达的 `archive.ubuntu.com`，也未声明 apt 重试策略。
- GREEN: `python -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py::test_publish_dockerfiles_point_at_current_workspace_artifacts -q` -> PASS。
- RED: `python -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py::test_publish_dockerfiles_point_at_current_workspace_artifacts -q` -> FAIL, 基础镜像 Dockerfile 尚未声明可配置的 `APT_MIRROR`。
- GREEN: `python -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py::test_publish_dockerfiles_point_at_current_workspace_artifacts -q` -> PASS。
- GREEN: `powershell.exe -NoProfile -ExecutionPolicy Bypass -File script/deploy/build-backend-runtime-base-image.ps1 -Version 2026.06.05-jre21-noble-docker29.2.1` -> PASS, 生成离线 tar 与 manifest。
- GREEN: 写入本机用户环境变量 `INTRUOYI_BACKEND_RUNTIME_BASE_*` 与 `RUNTIME_CONTROL_BACKEND_RUNTIME_BASE_*` -> PASS。
- GREEN: `docker load -i D:\ProjectPackage\Int\BaseImages\intruoyi-backend-runtime-base-2026.06.05-jre21-noble-docker29.2.1.tar` + `docker image inspect intruoyi-backend-runtime-base:2026.06.05-jre21-noble-docker29.2.1 --format '{{.Id}}'` -> PASS, `imageId=sha256:b4f7d85f325665c3b372379e6d352c3c8be2d3a08add68ac0dc151f720160be7`。
- GREEN: `python -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py script/tests/test_runtime_control_ops_scripts.py -q` -> PASS, 66 passed。
- GREEN: `mvn -pl yudao-module-infra -Dtest=RuntimeControlServiceImplTest test` -> PASS, 43 tests passed。
- GREEN: `git diff --check -- script/deploy/int-ruoyi-test/Dockerfile.backend-base script/tests/test_publish_int_ruoyi_to_test_tooling.py doc/tasks/20260605-backend-runtime-base-local-config/task.md doc/tasks/20260605-backend-runtime-base-local-config/execution-log.md` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\ci-cd-environment-delivery\scripts\validate_cicd_environment.py --evidence docs\environments\ci-cd-evidence.md` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260605-backend-runtime-base-local-config --mode preview` -> PASS, delete none, blocked none。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260605-backend-runtime-base-local-config --mode apply` -> PASS, deleted none, blocked none。
- GREEN: completion audit `python -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py script/tests/test_runtime_control_ops_scripts.py -q` -> PASS, 66 passed。
- GREEN: completion audit `mvn -pl yudao-module-infra -Dtest=RuntimeControlServiceImplTest test` -> PASS, 43 tests passed。
- GREEN: completion audit env/tar/image identity check -> PASS, `tarSha256=5bcd568b46ba9f28bfa7ac8ee67e283aea95bd585a89842a14cc40827fa71603`, `imageId=sha256:b4f7d85f325665c3b372379e6d352c3c8be2d3a08add68ac0dc151f720160be7`。
- GREEN: completion audit `docker build --no-cache --build-arg BACKEND_RUNTIME_BASE_IMAGE=intruoyi-backend-runtime-base:2026.06.05-jre21-noble-docker29.2.1 -t intruoyi-backend:codex-internal-base-verify -f script/deploy/int-ruoyi-test/Dockerfile.backend D:\ProjectPackage\Int\IntRuoyi` -> PASS, 后端业务镜像构建只执行 `mkdir/WORKDIR/COPY`，临时验证镜像已删除。

## Artifact Evidence

- `image`: `intruoyi-backend-runtime-base:2026.06.05-jre21-noble-docker29.2.1`
- `version`: `2026.06.05-jre21-noble-docker29.2.1`
- `digest`: `sha256:b4f7d85f325665c3b372379e6d352c3c8be2d3a08add68ac0dc151f720160be7`
- `tarPath`: `D:\ProjectPackage\Int\BaseImages\intruoyi-backend-runtime-base-2026.06.05-jre21-noble-docker29.2.1.tar`
- `tarSha256`: `5bcd568b46ba9f28bfa7ac8ee67e283aea95bd585a89842a14cc40827fa71603`
