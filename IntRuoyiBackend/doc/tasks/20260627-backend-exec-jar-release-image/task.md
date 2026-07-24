# 20260627 后端发布镜像改用可执行 Jar

## Task Goal

修正测试服后端发布镜像入口，使镜像复制 Spring Boot repackage 生成的可执行 `yudao-server-exec.jar`，避免测试服容器启动时报 `no main manifest attribute, in app.jar`。

## Scope

- 后端发布镜像入口文件：
  - `script/deploy/int-ruoyi-test/Dockerfile.backend`
  - `yudao-server/Dockerfile`
- 任务证据：
  - `doc/tasks/20260627-backend-exec-jar-release-image/execution-log.md`

## Non-Scope

- 不修改业务代码、SQL、接口逻辑。
- 不直接在本任务内执行正式服或备份服发布。

## BDD

BDD: 后端发布镜像必须复制可执行 jar -> Given `yudao-server` 使用 `spring-boot-maven-plugin` 且配置 `classifier=exec` / When 发布镜像构建复制后端 jar / Then Dockerfile 必须复制 `yudao-server-exec.jar`，这样容器内 `app.jar` 才具有 `Main-Class` 和 `Start-Class`。

## Milestones

1. 创建任务目录并记录真实 blocker。`COMPLETED`
2. 修正后端发布镜像复制目标。`COMPLETED`
3. 记录验证证据并提交本任务改动。`IN_PROGRESS`

## Expected Verification

- `yudao-server/pom.xml` 仍保留 `spring-boot-maven-plugin` 的 `classifier=exec` 配置。
- `yudao-server/target/yudao-server-exec.jar` 的 `MANIFEST.MF` 包含 `Main-Class` 和 `Start-Class`。
- 两个 Dockerfile 都改为复制 `yudao-server-exec.jar`。

## Current Status

IN_PROGRESS：已根据测试服真实失败 `no main manifest attribute, in app.jar` 收敛出根因，并完成 Dockerfile 修正，待按仓库 TDD 门禁提交本任务改动。
