# 任务：重新固定后端发布镜像 LTS 基础发行版

## 任务目标

修复“构建发布包”在后端 Docker 镜像构建阶段失败的问题。当前 `Dockerfile.backend` 使用浮动基础镜像 `eclipse-temurin:21-jre`，本机缓存/上游标签已解析到 Ubuntu 26.04 `resolute`，导致 `apt-get update` 拉取 `resolute/universe` 时出现连接失败与 404。修复后后端发布镜像必须固定到稳定 LTS 基础发行版，避免发布构建随上游浮动标签漂移。

## Previous Task Check

- 上一个同服务仓库任务：`doc/tasks/20260604-release-tag-utf8-package-name/task.md`
- 状态：`completed`
- 历史相关任务：`doc/tasks/20260603-backend-dockerfile-lts-base/task.md`
- 状态：`completed`
- 历史回退任务：`doc/tasks/20260603-restore-release-code-before-dockerhub-preflight/task.md`
- 状态：`completed`
- 处理：上一任务已完成；历史回退任务按当时用户要求恢复浮动基础镜像。本次用户提供新的真实构建失败日志，证明浮动基础镜像风险复发，因此重新固定发布 Dockerfile 基础发行版。

## BDD 场景

- BDD: 后端发布镜像基础发行版稳定 -> Given 运行控制台执行“构建发布包” / When 发布脚本构建后端 Docker 镜像 / Then `Dockerfile.backend` 必须使用固定 LTS 标签，不得使用会漂移到 `resolute` 等新发行版的浮动 `eclipse-temurin:21-jre`。
- BDD: apt 依赖安装仍为正式构建前置条件 -> Given 后端容器需要运行 Linux 运维脚本 / When Dockerfile 安装 `python3 docker.io docker-compose-v2` / Then 依赖安装失败必须阻塞构建，不得跳过、mock 或从缓存伪造成功。
- BDD: 容器启动命令消除 Dockerfile 警告 -> Given Docker 构建报告 `JSONArgsRecommended` / When 默认启动命令写入 Dockerfile / Then 使用 JSON exec 形式并通过 `exec java` 启动应用，保留 `JAVA_OPTS` 与 `ARGS` 展开。

## Milestones

- [x] M1：建立任务文档并确认前置任务完成。
- [x] M2：新增 RED 契约测试，锁定浮动基础镜像不可用于发布构建。
- [ ] M3：最小修改 Dockerfile 与对应测试。（blocked：用户要求先设计正式内部基础镜像打包方案）
- [ ] M4：运行目标验证、脚本回归和证据校验。（blocked）
- [ ] M5：运行 task-closeout-cleanup 预览并按策略提交本任务改动。（blocked）

## Expected Verification

- RED/GREEN：`python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py -k publish_dockerfiles -q`
- GREEN：`python -X utf8 -m pytest script\tests\test_runtime_control_ops_scripts.py -k linux_backup_ops_runtime_prerequisites -q`
- GREEN：`python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py script\tests\test_runtime_control_ops_scripts.py -q`
- Optional real external verification：如 Docker Hub 与 Ubuntu apt 源可访问，运行 `docker build --progress=plain --no-cache -t intruoyi-backend:codex-lts-verify -f script\deploy\int-ruoyi-test\Dockerfile.backend D:\ProjectPackage\Int\IntRuoyi`；若外部网络失败，记录为外部前置阻塞，不用 fallback 伪造成功。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。固定基础镜像发行版，不增加镜像源 fallback、不忽略 apt 失败、不使用缓存镜像冒充成功。
- `是否从根因和长期维护角度解决`：是。根因是浮动基础镜像随上游漂移到当前不可稳定取包的 Ubuntu `resolute`；固定到 LTS 标签后发布构建不再受默认标签漂移影响。
- `是否存在临时补丁或绕过`：否。不跳过依赖安装、不删除 Docker CLI/Compose 运行前置、不绕过真实 Docker 构建。

## 当前状态

blocked

## Current Status

blocked

## 验证结果

- VERIFY：上一任务 `doc/tasks/20260604-release-tag-utf8-package-name/task.md` 状态为 `completed`。
- VERIFY：`docker run --rm eclipse-temurin:21-jre sh -lc "cat /etc/os-release"` -> PASS，当前浮动标签解析为 Ubuntu 26.04 `resolute`，与用户构建日志中的 `resolute` apt 源一致。
- RED：`python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py -k publish_dockerfiles -q` -> FAIL，原因：`Dockerfile.backend` 第一行仍为浮动 `FROM eclipse-temurin:21-jre`。

## Blockers

- 用户已明确确认应先设计正式打包方案：建立公司内部固定基础镜像 `intruoyi-backend-runtime-base:<版本>`，发布包构建只复制 jar，不再每次访问 Docker Hub/Ubuntu apt 源。
- 本任务原计划的“直接固定 Dockerfile 到外部 LTS 基础镜像”不再继续，已撤回半成品 Dockerfile/test 改动，后续实现应以新的设计任务为准。

## Cleanup Keep

- `doc/tasks/20260604-backend-dockerfile-lts-base-repin/task.md`
- `doc/tasks/20260604-backend-dockerfile-lts-base-repin/execution-log.md`
- `doc/tasks/20260604-backend-dockerfile-lts-base-repin/bug-regression-evidence.md`
- `doc/tasks/20260604-backend-dockerfile-lts-base-repin/ci-cd-evidence.md`
