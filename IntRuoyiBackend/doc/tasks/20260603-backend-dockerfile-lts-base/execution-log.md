# 执行日志：固定后端发布镜像基础发行版

BDD: 后端发布镜像基础发行版固定 -> Given 发布流程构建后端镜像 / When Docker 解析基础镜像 / Then 基础镜像必须固定到已选 LTS 发行版标签，不得使用会随上游漂移到新发行版的浮动标签。

BDD: 后端容器启动命令可传递信号 -> Given 后端容器由发布镜像启动 / When Docker 执行默认启动命令 / Then Dockerfile 必须使用 JSON exec 形式并通过 `exec java` 启动应用，避免 shell-form `CMD` 警告。

## Bug

后端发布镜像构建在 `Dockerfile.backend` 的 `apt-get update && apt-get install ...` 阶段失败，日志显示基础镜像解析到 Ubuntu `resolute` 仓库后，`resolute-updates InRelease` 返回 `502 Bad Gateway`，`resolute/universe amd64 Packages` 出现连接失败与 `404 Not Found`，最终 Docker build exit code 100。

## Expected

发布后端镜像应使用固定 LTS 基础发行版，避免浮动标签随上游默认发行版漂移；容器默认启动命令应使用 JSON exec 形式，避免 shell-form `CMD` 信号处理警告。

## Reproduction

REPRO: 用户提供的构建日志显示 `Dockerfile.backend` 第 3 行 `apt-get update && apt-get install ...` 解析到 Ubuntu `resolute` 仓库后，`resolute-updates InRelease` 返回 `502 Bad Gateway`，`resolute/universe amd64 Packages` 进一步出现连接失败与 `404 Not Found`，导致 Docker build exit code 100；同一构建还报告 `JSONArgsRecommended`，指向 Dockerfile 第 18 行 shell-form `CMD`。

## Root Cause

`Dockerfile.backend` 使用 `FROM eclipse-temurin:21-jre` 浮动标签。该标签会随上游基础发行版漂移，当前已解析到构建日志中的 Ubuntu `resolute` 仓库，使发布构建受上游新发行版仓库状态影响。Dockerfile 还使用 shell-form `CMD`，触发 Docker 的 JSON args 警告。

RED: `python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py -k publish_dockerfiles` -> FAIL，预期原因：`Dockerfile.backend` 仍使用 `FROM eclipse-temurin:21-jre`，缺少固定 LTS 标签与 JSON exec 形式 `CMD`。

GREEN: `python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py -k publish_dockerfiles` -> PASS。

GREEN: `python -X utf8 -m pytest script\tests\test_runtime_control_ops_scripts.py -k linux_backup_ops_runtime_prerequisites` -> PASS。

REGRESSION: `python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py` -> PASS，52 passed。

REGRESSION: `python -X utf8 -m pytest script\tests\test_runtime_control_ops_scripts.py` -> PASS，5 passed。

VERIFY: Docker Hub 官方 `eclipse-temurin` 标签页可查询到 `21-jre-noble`，并显示对应 `docker pull eclipse-temurin:21-jre-noble` 命令；Ubuntu Packages 的 noble 包索引确认 `docker-compose-v2` 存在于 Ubuntu 24.04 LTS noble/universe。

VERIFY: `docker images --format "{{.Repository}}:{{.Tag}} {{.ID}} {{.CreatedSince}}" | Select-String -Pattern "eclipse-temurin|temurin"` -> PASS，本机仅有缓存 `eclipse-temurin:21-jre` 和 `maven:3.9.9-eclipse-temurin-21`，没有 `eclipse-temurin:21-jre-noble` 缓存。

VERIFY: `docker run --rm eclipse-temurin:21-jre sh -c "cat /etc/os-release; apt-cache policy docker-compose-v2 python3 docker.io | sed -n '1,80p'"` -> PASS，缓存的浮动 `21-jre` 镜像确认为 Ubuntu 26.04 `resolute`，与用户构建日志中的 apt 源一致，说明旧浮动标签/缓存确实会把构建带到 `resolute`。

VERIFY: `docker manifest inspect eclipse-temurin:21-jre-noble` -> FAIL，Docker CLI 访问 `registry-1.docker.io` 超时，错误为 `request canceled while waiting for connection (Client.Timeout exceeded while awaiting headers)`。

VERIFY: `docker pull eclipse-temurin:21-jre-noble` -> FAIL，Docker daemon 已解析到 `library/eclipse-temurin` 的 manifest 摘要，但后续读取 registry 数据时发生 `TLS handshake timeout`。

VERIFY: `docker manifest inspect eclipse-temurin:21-jre-noble --verbose` -> FAIL，Docker CLI 访问 `registry-1.docker.io` 被拒绝，错误为 `connectex: No connection could be made because the target machine actively refused it`。

## Verification

已通过发布 Dockerfile 契约测试与运行控制发布依赖契约测试。`docker manifest inspect eclipse-temurin:21-jre-noble` 当前因 Docker Hub 连接超时未能完成，错误为 `request canceled while waiting for connection (Client.Timeout exceeded while awaiting headers)`；该项是外部网络前置条件，未用 mock、跳过或镜像源 fallback 伪造成功。

## Blockers

代码侧契约与回归验证无阻塞。外部验证缺口：Docker CLI 当前无法稳定连接 `registry-1.docker.io`，因此不能在本机完成真实 manifest inspect、pull 或完整 Docker build；未用 fallback、跳过或缓存镜像伪造成功。

CLOSEOUT PREVIEW: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260603-backend-dockerfile-lts-base --mode preview` -> READY，keep `task.md` / `execution-log.md`，delete `<none>`，blocked `<none>`，warnings `<none>`。
