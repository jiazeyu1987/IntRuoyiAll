# 执行日志：重新固定后端发布镜像 LTS 基础发行版

- BDD: 后端发布镜像基础发行版稳定 -> Given 运行控制台执行“构建发布包” / When 发布脚本构建后端 Docker 镜像 / Then `Dockerfile.backend` 必须使用固定 LTS 标签，不得使用会漂移到 `resolute` 等新发行版的浮动 `eclipse-temurin:21-jre`。
- BDD: apt 依赖安装仍为正式构建前置条件 -> Given 后端容器需要运行 Linux 运维脚本 / When Dockerfile 安装 `python3 docker.io docker-compose-v2` / Then 依赖安装失败必须阻塞构建，不得跳过、mock 或从缓存伪造成功。
- BDD: 容器启动命令消除 Dockerfile 警告 -> Given Docker 构建报告 `JSONArgsRecommended` / When 默认启动命令写入 Dockerfile / Then 使用 JSON exec 形式并通过 `exec java` 启动应用，保留 `JAVA_OPTS` 与 `ARGS` 展开。

## 记录

- VERIFY：前置任务 `doc/tasks/20260604-release-tag-utf8-package-name/task.md` 为 `completed`。
- REPRO：用户提供的构建日志显示 Dockerfile 第 3 行 `apt-get update && apt-get install -y --no-install-recommends python3 docker.io docker-compose-v2` 访问 Ubuntu `resolute/universe` 包索引时连接失败并返回 404，最终 Docker build exit code 100；同一日志包含 `JSONArgsRecommended` 警告。
- VERIFY：`docker run --rm eclipse-temurin:21-jre sh -lc "cat /etc/os-release"` -> PASS，当前浮动标签解析为 `Ubuntu 26.04 LTS` / `VERSION_CODENAME=resolute`。
- RED: 固定 LTS 基础镜像契约 -> `python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py -k publish_dockerfiles -q` -> FAIL，`Dockerfile.backend` 第一行仍为 `FROM eclipse-temurin:21-jre`。
- BLOCKED: 用户确认应先设计正式内部基础镜像打包方案；本任务不继续直接改 `Dockerfile.backend`，已撤回半成品 Dockerfile/test 改动，转入 `20260604-internal-backend-base-image-packaging-design` 设计任务。
