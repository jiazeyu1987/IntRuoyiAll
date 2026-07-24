# 任务：恢复发布构建代码到 DockerHub 前置检查之前

## 任务目标

按用户要求，将“构建发布包”相关代码恢复到后端提交 `de99474db6`（`2026-06-03 21:09`）之前的相关状态。范围仅限发布构建 Dockerfile、发布脚本 Docker Hub metadata 前置检查及对应测试；不回退运行控制台、NAS、DCC、恢复数据等无关改动。

## Previous Task Check

- 上一个同服务仓库任务：`doc/tasks/20260603-dockerhub-preflight-message-encoding/task.md`
- 状态：`completed`
- 影响：上一任务已完成；本任务按用户明确要求回退其相关发布前置检查改动。

## BDD 场景

- BDD: 发布构建恢复到前置检查之前 -> Given 运维人员点击“构建发布包”且 Docker daemon 可用 / When 发布脚本进入本地构建阶段 / Then 脚本不应额外执行 `docker manifest inspect` metadata 前置检查，而应回到此前由真实 `docker build` 暴露基础镜像或 apt 源问题的行为。
- BDD: 后端发布 Dockerfile 恢复旧基础镜像定义 -> Given 发布脚本构建后端镜像 / When 读取 `script/deploy/int-ruoyi-test/Dockerfile.backend` / Then 基础镜像恢复为 `eclipse-temurin:21-jre`，CMD 恢复为原 shell form。

## Milestones

- [x] M1：建立任务文档并确认上一任务完成。
- [x] M2：补充失败回归测试，证明当前代码仍包含 `de99474db6` 之后的发布构建改动。
- [x] M3：按用户要求回退相关生产代码与对应测试。
- [x] M4：运行目标测试并记录 RED/GREEN 证据。
- [x] M5：closeout 预览并提交本任务改动。

## Expected Verification

- `python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -k "publish_dockerfiles_point_at_current_workspace_artifacts or publish_script_does_not_preflight_dockerhub_base_image_metadata" -q`
- 静态核对发布脚本不再包含 `DOCKERHUB_PREFLIGHT_FAILED`、`Assert-DockerBaseImageMetadataAvailable`、`docker manifest inspect` 前置检查。
- 静态核对后端 Dockerfile 恢复 `FROM eclipse-temurin:21-jre`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。本任务为用户明确要求的代码恢复，不新增镜像源 fallback、不吞异常、不使用缓存镜像伪造成功。
- `是否从根因和长期维护角度解决`：是。移除与真实 Docker 构建能力不一致的额外 metadata 前置检查，恢复到此前构建路径直接暴露失败的行为。
- `是否存在临时补丁或绕过`：否。只回退指定提交及之后的相关改动，不新增临时绕过。

## 当前状态

completed

## 验证结果

- VERIFY：上一任务 `doc/tasks/20260603-dockerhub-preflight-message-encoding/task.md` 状态为 `completed`。
- RED：`python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -k "publish_dockerfiles_point_at_current_workspace_artifacts or publish_script_does_not_preflight_dockerhub_base_image_metadata" -q` -> FAIL，预期原因：当前代码仍包含 `FROM eclipse-temurin:21-jre-noble`、JSON CMD 与 `docker manifest inspect` metadata 前置检查。
- GREEN：`python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -k "publish_dockerfiles_point_at_current_workspace_artifacts or publish_script_does_not_preflight_dockerhub_base_image_metadata" -q` -> PASS，`2 passed, 51 deselected`。
- GREEN：`python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -q` -> PASS，`53 passed`。
- VERIFY：`rg -n "DOCKERHUB_PREFLIGHT_FAILED|Cannot read Docker base image metadata|Assert-DockerBaseImageMetadataAvailable|Get-DockerfileBaseImages|manifest', 'inspect|docker manifest inspect" script/deploy/publish-int-ruoyi.ps1 script/deploy/int-ruoyi-test/Dockerfile.backend` -> PASS，无残留匹配。
- VERIFY：`git diff --check -- <相关文件>` -> PASS，仅提示 Git 将在下次触碰时把 LF 替换为 CRLF，无 whitespace error。
- CLOSEOUT PREVIEW：`python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260603-restore-release-code-before-dockerhub-preflight --mode preview` -> READY，keep `task.md` / `execution-log.md` / `bug-regression-evidence.md`，delete `<none>`，blocked `<none>`，warnings `<none>`。

## Blockers

- none.

## Cleanup Keep

- `doc/tasks/20260603-restore-release-code-before-dockerhub-preflight/bug-regression-evidence.md`
