# 任务：Docker Hub 鉴权连接失败诊断与发布前置检查

## 任务目标

处理发布构建在拉取 `eclipse-temurin:21-jre-noble` 元数据时无法访问 Docker Hub 鉴权服务的问题。优先确认本机 Docker Desktop 到 `auth.docker.io` / `registry-1.docker.io` 的网络前置条件；如无法在本机修通网络，则为发布流程增加 fail-fast 前置检查和清晰错误提示，避免非代码用户等待完整构建后才看到底层网络错误。

## Previous Task Check

- 上一个后端任务：`doc/tasks/20260603-restore-data-test-only-safety/task.md`
- 状态：`completed`，提交 `ce3aa59274 任务: 禁止恢复数据覆盖正式服`
- 影响：上一个任务已完成；本任务只处理 Docker Hub 鉴权连接/发布前置检查，不修改恢复数据逻辑。

## BDD 场景

- BDD: Docker Hub 鉴权不可达时提前失败 -> Given 发布流程需要拉取后端基础镜像 / When Docker Desktop 无法访问 `auth.docker.io` 或 `registry-1.docker.io` / Then 发布命令必须在传输大构建上下文前 fail-fast，并提示 Docker Hub 网络/代理前置条件缺失。
- BDD: Docker Hub 可达时继续真实构建 -> Given Docker Desktop 可访问基础镜像元数据 / When 发布流程执行构建 / Then 不得切换镜像源、不得使用 mock 镜像、不得静默降级。

## Milestones

- [x] M1：确认上一任务已完成并提交。
- [x] M2：复现 Docker Hub 鉴权连接失败并记录诊断。
- [x] M3：补充 RED 契约测试。
- [x] M4：实现发布前 fail-fast Docker Hub 前置检查。
- [x] M5：运行 GREEN/回归验证并执行 closeout 预览。

## Expected Verification

- `docker pull eclipse-temurin:21-jre-noble` 用于真实网络验证；若失败，记录外部网络前置阻塞。
- `python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py -k dockerhub`
- `python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py`

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。不自动切换基础镜像、不使用缓存镜像冒充成功、不改用非官方镜像源。
- `是否从根因和长期维护角度解决`：是。根因是 Docker Desktop 到 Docker Hub 鉴权/registry 的网络前置条件缺失；发布流程应提前暴露该前置条件，并保留真实构建。
- `是否存在临时补丁或绕过`：否。若需要代理、内部镜像仓库或镜像 tar 预加载，必须作为显式前置配置，不做静默绕过。

## 当前状态

completed

## 已完成工作

- 确认 Docker Desktop 已启动，Docker daemon 正常响应；失败不是 Engine 未启动。
- 复现 `auth.docker.io` 与 `registry-1.docker.io` 访问超时，确认失败发生在 Docker Hub 鉴权/registry 网络前置条件。
- 为发布脚本新增 `Get-DockerfileBaseImages`，从 Dockerfile `FROM` 行解析基础镜像。
- 为发布脚本新增 `Assert-DockerBaseImageMetadataAvailable`，在本地 Maven/NPM 构建和 Docker build 传输大上下文前执行 `docker manifest inspect`。
- 当 Docker Hub 不可达时，发布脚本会 fail-fast 输出中文提示：检查 Docker Desktop 代理、DNS 或公司网络是否允许访问 `auth.docker.io / registry-1.docker.io`。

## 验证结果

- RED：`python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py -k dockerhub` -> FAIL，预期原因为发布脚本缺少基础镜像 metadata 预检。
- GREEN：`python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py -k dockerhub` -> PASS。
- REGRESSION：`python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py` -> PASS，53 passed。
- VERIFY：`docker info` -> PASS，Docker Desktop Engine 正常运行。
- VERIFY：`Test-NetConnection auth.docker.io -Port 443` -> FAIL，TCP 443 不通。
- VERIFY：`curl.exe -I --connect-timeout 20 https://auth.docker.io/token?service=registry.docker.io` -> FAIL，连接超时。
- VERIFY：`docker manifest inspect eclipse-temurin:21-jre-noble` -> FAIL，`registry-1.docker.io/v2/` context deadline exceeded。
- CLOSEOUT PREVIEW：`python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260603-dockerhub-auth-connectivity-failfast --mode preview` -> READY，keep `task.md` / `execution-log.md`，delete `<none>`，blocked `<none>`，warnings `<none>`。
- TDD GATE：`python -X utf8 tool\verify_tdd_compliance.py --task-dir doc\tasks\20260603-dockerhub-auth-connectivity-failfast --all-changed` -> PASS。

## Blockers

- none for code-side fail-fast preflight.
- External verification gap: 真实 Docker Hub 拉取仍受外部网络阻塞，当前主机/Docker Desktop 无法稳定访问 `auth.docker.io` / `registry-1.docker.io`。本任务不引入镜像源 fallback、不使用缓存镜像冒充成功。
