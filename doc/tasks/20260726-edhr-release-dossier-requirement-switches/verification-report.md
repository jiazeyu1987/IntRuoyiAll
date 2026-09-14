# Verification Report

## Passed

- `node tests\e2e\edhr-release-dossier-requirement-setting-static.spec.js` -> PASS。
- `pnpm ts:check` -> PASS。
- `node tests\e2e\edhr-release-check-result-chinese-static.spec.js` -> PASS。
- `node tests\e2e\edhr-release-dialog-copy-cleanup-static.spec.js` -> PASS。
- `mvn.cmd -pl yudao-module-mes -am "-DskipTests" compile` -> PASS，后端生产代码 reactor 编译通过。
- `git diff --check` -> PASS，仅 CRLF 工作区提示，无 whitespace error。
- `node scripts/preflight/login-preflight.mjs --base-url http://127.0.0.1:8081 --target-path /user/profile --target-text 个人工作台` -> PASS，本机默认身份标签 `芋道源码/admin` 登录个人中心成功。
- `node --check tests\e2e\edhr-release-dossier-requirement-setting-real.e2e.js` -> PASS。
- `mvn.cmd -pl yudao-server -am "-DskipTests" package`（`D:\IntRuoyiWorktree\edhr-release-dossier-e2e-20260726\IntRuoyiBackend`）-> PASS，生成包含 `MesProEdhrReleaseSettingController` 的隔离 jar。
- `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProEdhrReleaseServiceImplTest,MesProEdhrReleasePrecheckContractTest,MesProEdhrReleaseDossierRequirementSettingServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`（隔离 worktree）-> PASS，24 tests / 0 failures / 0 errors。
- `pnpm install --frozen-lockfile`（隔离 worktree 前端）-> PASS，补齐 `node_modules/.bin/vite`，未修改 lockfile。
- `http://127.0.0.1:48086/actuator/health` -> UP，PID 52792，命令行指向隔离 worktree `yudao-server-exec.jar`。
- `http://127.0.0.1:8086/` -> HTTP 200，Vite 进程命令行指向隔离 worktree，代理后端 48086。
- 本机 Docker MySQL `infra_config` seed -> PASS，插入缺失配置键 `mes.edhr.release.dossier.requirements`，默认 JSON 全 false；执行前校验表存在、重复键、JSON 和 4 个布尔字段。
- `node tests\e2e\edhr-release-dossier-requirement-setting-real.e2e.js`（`EDHR_RELEASE_DOSSIER_E2E_BASE_URL=http://127.0.0.1:8086`，`EDHR_RELEASE_DOSSIER_E2E_BACKEND_URL=http://127.0.0.1:48086`）-> PASS。
- E2E 恢复复验 -> PASS，UI 恢复 `incomingInspectionReportRequired=false`，独立 DB 复验 4 个开关均为 false。
- 授权后的 `48081` 运行态替换 -> PASS，旧 `codex-test-run-monitor-runtime` PID `18212` 已停止，新 PID `57744` 指向任务 Jar，`http://127.0.0.1:48081/actuator/health` 返回 `UP`。
- `http://127.0.0.1:48081/admin-api/mes/pro/edhr-release-setting/dossier-requirements` 未登录探针 -> PASS，返回业务 `401 账号未登录`，证明路由已加载且不再是 `请求地址不存在`。
- `node tests\e2e\edhr-release-dossier-requirement-setting-real.e2e.js`（默认 int_main `http://127.0.0.1:8081` / `http://127.0.0.1:48081`，使用用户本次指定的 `芋道源码/admin` 登录凭据，未记录明文密码）-> PASS。
- int_main E2E 后独立 DB 恢复复验 -> PASS，`infra_config` 中 `mes.edhr.release.dossier.requirements` 仍为四个字段全 false。
- 隔离运行态停止 -> PASS，停止 task-owned PID 52792 / 26488，复验 8086/48086 均已释放。

## Resolved Blockers

- `48081` 旧 jar 不含新增 Controller 导致业务 404：已通过 slot 5 隔离运行态 8086/48086 复验，未停止并行 48081。
- 用户授权后已替换 `48081` 旧 jar；当前 `8081/48081` 用户路径真实 E2E 已 PASS。
- 缺少 `mes.edhr.release.dossier.requirements` 配置导致 fail-fast：已执行既有 seed，在本机测试库插入默认全 false 配置。
- 前端 worktree 缺少 `vite`：已执行 `pnpm install --frozen-lockfile` 补齐依赖。
- E2E hash 断言等待不足：已修正脚本等待配置 hash 文案异步渲染后再断言。

## Remaining Closeout

- 实现和验证已完成；收尾 cleanup、经验沉淀、提交与推送尚未执行。
- 主工作区仍存在大量并行脏改动和 ahead 状态，提交前必须按项目 dirty-worktree baseline / 选择性暂存规则处理。
- 任务状态更新为 `ready_for_closeout`，不得在 closeout 完成前标记 `completed`。
