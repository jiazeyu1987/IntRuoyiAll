# 任务：固定后端发布镜像基础发行版

## 任务目标

修复 `Dockerfile.backend` 构建阶段 `apt-get update` 解析到非固定 Ubuntu 发行版仓库后失败的问题，使测试服发布后端镜像使用固定 LTS 基础发行版，并消除 Dockerfile `CMD` shell-form 警告。

## Previous Task Check

- 上一个后端任务：`doc/tasks/20260603-prod-backup-restore-to-test/task.md`
- 状态：`completed`
- 影响：上一个任务已完成；本任务只修改后端发布 Dockerfile、相关契约测试与本任务文档，不接管 runtime 运行态文件。

## BDD 场景

- BDD: 后端发布镜像基础发行版固定 -> Given 发布流程构建后端镜像 / When Docker 解析基础镜像 / Then 基础镜像必须固定到已选 LTS 发行版标签，不得使用会随上游漂移到新发行版的浮动标签。
- BDD: 后端容器启动命令可传递信号 -> Given 后端容器由发布镜像启动 / When Docker 执行默认启动命令 / Then Dockerfile 必须使用 JSON exec 形式并通过 `exec java` 启动应用，避免 shell-form `CMD` 警告。

## Milestones

- [x] M1：确认构建失败根因与受影响 Dockerfile。
- [x] M2：补充 RED 契约测试。
- [x] M3：最小修改 Dockerfile。
- [x] M4：运行 GREEN 与回归验证。
- [x] M5：执行 task-closeout-cleanup 预览并提交本任务改动。

## Expected Verification

- `python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py -k publish_dockerfiles`
- `python -X utf8 -m pytest script\tests\test_runtime_control_ops_scripts.py -k linux_backup_ops_runtime_prerequisites`
- `docker manifest inspect eclipse-temurin:21-jre-noble` 如 Docker Hub 可访问则验证标签存在；若外部网络不可访问，记录为外部前置阻塞，不以 mock 或跳过伪造成功。
- 任务日志必须包含 BDD、RED、GREEN/REGRESSION 证据。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。固定基础镜像发行版，不增加镜像源兜底、不忽略 apt 失败、不吞构建错误。
- `是否从根因和长期维护角度解决`：是。根因是浮动基础镜像漂移到当前不可稳定获取的 Ubuntu 发行版仓库；固定到 LTS 发行版标签后发布构建不再随上游默认标签漂移。
- `是否存在临时补丁或绕过`：否。不跳过依赖安装、不改用 mock 包、不绕过 Docker 构建。

## 当前状态

completed

## 已完成工作

- 已确认受影响文件为 `script/deploy/int-ruoyi-test/Dockerfile.backend`。
- 已补充发布 Dockerfile 契约测试，要求基础镜像固定为 `eclipse-temurin:21-jre-noble`，并要求 `CMD` 使用 JSON exec 形式。
- 已将后端发布基础镜像从浮动 `eclipse-temurin:21-jre` 固定为 `eclipse-temurin:21-jre-noble`。
- 已将 shell-form `CMD` 改为 JSON exec 形式，并通过 `exec java` 启动应用。
- 已补齐 bug 回归证据，记录根因、RED、GREEN、REGRESSION 与外部网络前置条件。
- 已执行 task-closeout-cleanup 预览，确认只保留本任务 `task.md` 与 `execution-log.md`，无待删除产物。

## 验证结果

- RED：`python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py -k publish_dockerfiles` -> FAIL，预期原因为 Dockerfile 仍使用浮动基础镜像与 shell-form `CMD`。
- GREEN：`python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py -k publish_dockerfiles` -> PASS。
- GREEN：`python -X utf8 -m pytest script\tests\test_runtime_control_ops_scripts.py -k linux_backup_ops_runtime_prerequisites` -> PASS。
- REGRESSION：`python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py` -> PASS，52 passed。
- REGRESSION：`python -X utf8 -m pytest script\tests\test_runtime_control_ops_scripts.py` -> PASS，5 passed。
- GREEN：`python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260603-backend-dockerfile-lts-base\execution-log.md` -> PASS。
- VERIFY：Docker Hub 官方标签页可查询到 `eclipse-temurin:21-jre-noble`；Ubuntu Packages 确认 noble 的 `docker-compose-v2` 包存在。
- VERIFY：`docker pull eclipse-temurin:21-jre-noble` -> FAIL，Docker daemon 已解析到 manifest 摘要，但访问 `registry-1.docker.io` 时 `TLS handshake timeout`。
- VERIFY：`docker manifest inspect eclipse-temurin:21-jre-noble --verbose` -> FAIL，Docker CLI 访问 `registry-1.docker.io` 被拒绝。
- CLOSEOUT PREVIEW：`python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260603-backend-dockerfile-lts-base --mode preview` -> READY，keep `task.md` / `execution-log.md`，delete `<none>`，blocked `<none>`，warnings `<none>`。

## Blockers

- none for code-side contract and regression verification.
- External verification gap: Docker CLI 当前无法稳定连接 `registry-1.docker.io`，`docker manifest inspect`、`docker pull` 与完整 Docker build 暂未能完成；该项是外部网络前置条件，不以 fallback 或跳过伪造成功。
