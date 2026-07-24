# 执行日志：恢复发布构建代码到 DockerHub 前置检查之前

BDD: 发布构建恢复到前置检查之前 -> Given 运维人员点击“构建发布包”且 Docker daemon 可用 / When 发布脚本进入本地构建阶段 / Then 脚本不应额外执行 `docker manifest inspect` metadata 前置检查，而应回到此前由真实 `docker build` 暴露基础镜像或 apt 源问题的行为。

BDD: 后端发布 Dockerfile 恢复旧基础镜像定义 -> Given 发布脚本构建后端镜像 / When 读取 `script/deploy/int-ruoyi-test/Dockerfile.backend` / Then 基础镜像恢复为 `eclipse-temurin:21-jre`，CMD 恢复为原 shell form。

VERIFY: 上一个同服务仓库任务 `doc/tasks/20260603-dockerhub-preflight-message-encoding/task.md` 当前状态为 `completed`。

RED: `python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -k "publish_dockerfiles_point_at_current_workspace_artifacts or publish_script_does_not_preflight_dockerhub_base_image_metadata" -q` -> FAIL，预期原因：当前代码仍包含 `FROM eclipse-temurin:21-jre-noble`、JSON CMD 与 `docker manifest inspect` metadata 前置检查。

CHANGE: `script/deploy/int-ruoyi-test/Dockerfile.backend` 已恢复为 `FROM eclipse-temurin:21-jre` 与 `CMD java ${JAVA_OPTS} -jar app.jar $ARGS`。

CHANGE: `script/deploy/publish-int-ruoyi.ps1` 已移除 `Get-DockerfileBaseImages`、`Assert-DockerBaseImageMetadataAvailable`、`docker manifest inspect` 调用、`DOCKERHUB_PREFLIGHT_FAILED` 提示，以及该前置检查调用块。

CHANGE: `script/tests/test_publish_int_ruoyi_to_test_tooling.py` 已更新为断言发布脚本不包含 Docker Hub metadata preflight，并断言 Dockerfile 恢复旧基础镜像与 CMD。

GREEN: `python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -k "publish_dockerfiles_point_at_current_workspace_artifacts or publish_script_does_not_preflight_dockerhub_base_image_metadata" -q` -> PASS，`2 passed, 51 deselected`。

GREEN: `python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -q` -> PASS，`53 passed`。

VERIFY: `rg -n "DOCKERHUB_PREFLIGHT_FAILED|Cannot read Docker base image metadata|Assert-DockerBaseImageMetadataAvailable|Get-DockerfileBaseImages|manifest', 'inspect|docker manifest inspect" script/deploy/publish-int-ruoyi.ps1 script/deploy/int-ruoyi-test/Dockerfile.backend` -> PASS，无残留匹配。

VERIFY: `git diff --check -- script/deploy/publish-int-ruoyi.ps1 script/deploy/int-ruoyi-test/Dockerfile.backend script/tests/test_publish_int_ruoyi_to_test_tooling.py doc/tasks/20260603-restore-release-code-before-dockerhub-preflight` -> PASS，仅提示 Git 将在下次触碰时把 LF 替换为 CRLF，无 whitespace error。

CLOSEOUT PREVIEW: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260603-restore-release-code-before-dockerhub-preflight --mode preview` -> READY，keep `task.md` / `execution-log.md` / `bug-regression-evidence.md`，delete `<none>`，blocked `<none>`，warnings `<none>`。
