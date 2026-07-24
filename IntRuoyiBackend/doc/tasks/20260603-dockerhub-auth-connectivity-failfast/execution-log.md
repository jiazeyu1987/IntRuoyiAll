# 执行记录：Docker Hub 鉴权连接失败诊断与发布前置检查

BDD: Docker Hub 鉴权不可达时提前失败 -> Given 发布流程需要拉取后端基础镜像 / When Docker Desktop 无法访问 `auth.docker.io` 或 `registry-1.docker.io` / Then 发布命令必须在传输大构建上下文前 fail-fast，并提示 Docker Hub 网络/代理前置条件缺失。

BDD: Docker Hub 可达时继续真实构建 -> Given Docker Desktop 可访问基础镜像元数据 / When 发布流程执行构建 / Then 不得切换镜像源、不得使用 mock 镜像、不得静默降级。

## Diagnostics

REPRO: 用户提供的构建日志显示 Docker 构建停在 `load metadata for docker.io/library/eclipse-temurin:21-jre-noble`，失败原因为 `failed to fetch anonymous token`，访问 `https://auth.docker.io/token?...` 超时。该失败发生在 Dockerfile 第 1 行基础镜像元数据解析阶段，尚未进入 `RUN apt-get update`。

VERIFY: Docker Desktop 截图显示左下角 `Engine running`；`docker info` -> PASS，Docker daemon 正常响应，Server 为 Docker Desktop 29.2.1，当前 context 为 `desktop-linux`。

VERIFY: `Test-NetConnection auth.docker.io -Port 443` -> FAIL，TCP 443 连接失败；`curl.exe -I --connect-timeout 20 https://auth.docker.io/token?service=registry.docker.io` -> FAIL，20 秒超时。

VERIFY: `curl.exe -I --connect-timeout 20 https://registry-1.docker.io/v2/` -> FAIL，20 秒超时；`docker manifest inspect eclipse-temurin:21-jre-noble` -> FAIL，`registry-1.docker.io/v2/` context deadline exceeded。

VERIFY: 本机环境变量 `HTTP_PROXY` / `HTTPS_PROXY` 为空，`netsh winhttp show proxy` 显示 Direct access；`docker info` 显示 Docker daemon 侧代理为 `http.docker.internal:3128`。当前失败是 Docker Desktop/主机到 Docker Hub 鉴权与 registry 的网络前置条件未满足。

## RED

RED: `python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py -k dockerhub` -> FAIL，预期原因：发布脚本还没有在本地 Maven/NPM 构建与 Docker build 传输大上下文之前执行基础镜像 metadata 预检。

## GREEN

GREEN: `python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py -k dockerhub` -> PASS，新增发布前 Docker Hub 基础镜像 metadata 预检契约通过。

## REGRESSION

REGRESSION: `python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py` -> PASS，53 passed。

## Blockers

代码侧 fail-fast preflight 无阻塞。外部验证缺口：真实 Docker Hub 拉取仍受外部网络阻塞，当前主机/Docker Desktop 无法稳定访问 `auth.docker.io` / `registry-1.docker.io`。本任务不引入镜像源 fallback、不使用缓存镜像冒充成功；发布脚本改为提前 fail-fast 并输出中文前置条件提示。

## Closeout

CLOSEOUT PREVIEW: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260603-dockerhub-auth-connectivity-failfast --mode preview` -> READY，keep `task.md` / `execution-log.md`，delete `<none>`，blocked `<none>`，warnings `<none>`。

GREEN: `python -X utf8 tool\verify_tdd_compliance.py --task-dir doc\tasks\20260603-dockerhub-auth-connectivity-failfast --all-changed` -> PASS。
