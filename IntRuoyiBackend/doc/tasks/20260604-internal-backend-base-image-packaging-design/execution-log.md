# 执行日志：设计内部后端基础镜像打包方案

- BDD: 日常发布包构建不访问外部基础镜像源 -> Given 内部基础镜像已由受控流程发布 / When 运维人员执行 `publish-int-ruoyi.ps1 -Mode build-release` / Then 后端业务镜像构建只使用内部基础镜像并复制 jar，不执行 `apt-get install`，不访问 Docker Hub 或 Ubuntu apt 源。
- BDD: 基础镜像构建受控且可追溯 -> Given 需要升级 JRE、Python、Docker CLI 或 Compose / When 基础镜像构建流程运行 / Then 流程必须显式访问外部源、记录版本、生成 digest、运行工具自检，并推送到内部镜像仓库或内部离线镜像包。
- BDD: 缺少内部基础镜像必须 fail fast -> Given 日常发布包构建找不到指定内部基础镜像 digest/tag / When 构建后端业务镜像 / Then 发布脚本必须直接阻塞并提示缺少基础镜像前置条件，不得回退到 Docker Hub、浮动标签或旧缓存镜像。

## 记录

- VERIFY：上一任务 `doc/tasks/20260604-backend-dockerfile-lts-base-repin/task.md` 已标记 `blocked`，半成品 Dockerfile/test 改动已撤回。
- EVIDENCE：现有 `publish-int-ruoyi.ps1` 构建后端镜像后执行 `docker save` 导出 `intruoyi-backend:<tag>` 与 `intruoyi-frontend:<tag>`，远端 `deploy-release` 再执行 `docker load -i '$remoteImageTar'`。
- EVIDENCE：当前 `Dockerfile.backend` 在业务发布包构建阶段执行 `apt-get update && apt-get install python3 docker.io docker-compose-v2`，这是每次打包访问 Ubuntu apt 源的直接原因。
- DESIGN: 新增系统设计文档 `docs/system/internal-backend-base-image-packaging-design.md`。
- ADR: 新增决策记录 `docs/adr/ADR-0001-internal-backend-runtime-base-image.md`。
- EVIDENCE: 新增 CI/CD 证据 `doc/tasks/20260604-internal-backend-base-image-packaging-design/ci-cd-evidence.md`。
- GREEN: ADR validator -> `python C:\Users\BJB110\.codex\skills\architecture-decision-records\scripts\validate_architecture_decision.py --adr docs/adr/ADR-0001-internal-backend-runtime-base-image.md` -> PASS。
- GREEN: CI/CD evidence validator -> `python C:\Users\BJB110\.codex\skills\ci-cd-environment-delivery\scripts\validate_cicd_environment.py --evidence doc/tasks/20260604-internal-backend-base-image-packaging-design/ci-cd-evidence.md` -> PASS。
- GREEN: System design docs validation -> `python C:\Users\BJB110\.codex\skills\system-design-docs\scripts\validate_system_design.py --root .` -> PASS。
- GREEN: UTF-8 readback -> PASS。
- GREEN: `git diff --check` -> PASS。
- GREEN: task-closeout-cleanup preview -> `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260604-internal-backend-base-image-packaging-design --mode preview` -> PASS，keep 五项，delete/blocked/warnings 为空。
- GREEN: task-closeout-cleanup apply -> `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260604-internal-backend-base-image-packaging-design --mode apply` -> PASS，delete/blocked/warnings/deleted_paths 为空。
