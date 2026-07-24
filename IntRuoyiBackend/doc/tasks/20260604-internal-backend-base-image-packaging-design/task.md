# 任务：设计内部后端基础镜像打包方案

## 任务目标

设计一个正式、稳定、可验证的 IntRuoyi 发布包构建方案：将“构建后端运行基础镜像”和“构建业务发布包”解耦。后端运行基础镜像由受控流程预先构建、验证并发布为公司内部固定镜像 `intruoyi-backend-runtime-base:<version>`；日常 `build-release` 只基于内部基础镜像复制当前 jar，不再每次访问 Docker Hub 或 Ubuntu apt 源。

## Previous Task Check

- 上一个同服务仓库任务：`doc/tasks/20260604-backend-dockerfile-lts-base-repin/task.md`
- 状态：`blocked`
- 处理：上一任务原计划直接改 `Dockerfile.backend` 固定外部 LTS 基础镜像；用户确认应先设计正式内部基础镜像打包方案，因此上一任务已阻塞并撤回半成品代码改动。本任务仅产出设计、ADR 与 CI/CD 证据。

## BDD 场景

- BDD: 日常发布包构建不访问外部基础镜像源 -> Given 内部基础镜像已由受控流程发布 / When 运维人员执行 `publish-int-ruoyi.ps1 -Mode build-release` / Then 后端业务镜像构建只使用内部基础镜像并复制 jar，不执行 `apt-get install`，不访问 Docker Hub 或 Ubuntu apt 源。
- BDD: 基础镜像构建受控且可追溯 -> Given 需要升级 JRE、Python、Docker CLI 或 Compose / When 基础镜像构建流程运行 / Then 流程必须显式访问外部源、记录版本、生成 digest、运行工具自检，并推送到内部镜像仓库或内部离线镜像包。
- BDD: 缺少内部基础镜像必须 fail fast -> Given 日常发布包构建找不到指定内部基础镜像 digest/tag / When 构建后端业务镜像 / Then 发布脚本必须直接阻塞并提示缺少基础镜像前置条件，不得回退到 Docker Hub、浮动标签或旧缓存镜像。

## Milestones

- [x] M1：建立任务文档并确认上一任务 blocked。
- [x] M2：梳理现有发布链路证据和失败根因。
- [x] M3：新增系统设计文档与 ADR。
- [x] M4：新增 CI/CD 证据并运行文档校验。
- [x] M5：运行 task-closeout-cleanup 预览并提交本设计任务文档。

## Expected Verification

- `python C:\Users\BJB110\.codex\skills\architecture-decision-records\scripts\validate_architecture_decision.py --adr docs/adr/ADR-0001-internal-backend-runtime-base-image.md`
- `python C:\Users\BJB110\.codex\skills\ci-cd-environment-delivery\scripts\validate_cicd_environment.py --evidence doc/tasks/20260604-internal-backend-base-image-packaging-design/ci-cd-evidence.md`
- UTF-8 readback for all new Chinese Markdown files.
- `git diff --check -- <设计相关文件>`

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。方案要求内部基础镜像缺失时 fail fast，不回退外部 Docker Hub、不使用旧缓存、不跳过工具依赖。
- `是否从根因和长期维护角度解决`：是。根因是日常发布包构建每次重建运行基础层并依赖外部 Docker Hub/Ubuntu apt；新方案把外部依赖集中到受控基础镜像发布流程。
- `是否存在临时补丁或绕过`：否。本任务只设计正式方案，不实施临时镜像源、缓存绕过或直接改 Dockerfile。

## 当前状态

completed

## Current Status

completed

## 验证结果

- VERIFY：上一任务 `doc/tasks/20260604-backend-dockerfile-lts-base-repin/task.md` 已标记 `blocked`，半成品 Dockerfile/test 改动已撤回。
- DESIGN：新增 `docs/system/internal-backend-base-image-packaging-design.md`，定义内部固定基础镜像、日常发布包构建、fail-fast 前置条件与实施边界。
- ADR：新增 `docs/adr/ADR-0001-internal-backend-runtime-base-image.md`，记录选择内部固定后端运行基础镜像的决策、取舍、风险与回滚条件。
- EVIDENCE：新增 `doc/tasks/20260604-internal-backend-base-image-packaging-design/ci-cd-evidence.md`。
- GREEN：ADR validator -> PASS。
- GREEN：CI/CD evidence validator -> PASS。
- GREEN：System design docs validation -> PASS。
- GREEN：UTF-8 readback -> PASS。
- GREEN：`git diff --check` -> PASS。
- GREEN：task-closeout-cleanup preview -> PASS，keep 五项，delete/blocked/warnings 均为空。
- GREEN：task-closeout-cleanup apply -> PASS，delete/blocked/warnings/deleted_paths 均为空。

## Cleanup Keep

- `doc/tasks/20260604-internal-backend-base-image-packaging-design/task.md`
- `doc/tasks/20260604-internal-backend-base-image-packaging-design/execution-log.md`
- `doc/tasks/20260604-internal-backend-base-image-packaging-design/ci-cd-evidence.md`
- `docs/system/internal-backend-base-image-packaging-design.md`
- `docs/adr/ADR-0001-internal-backend-runtime-base-image.md`
