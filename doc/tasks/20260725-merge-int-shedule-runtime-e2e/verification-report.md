# Verification Report

## Result

pass

## Scope

- 当前分支：`int_batch`。
- 融合来源：`origin/int_shedule` 最新提交 `14cc1e66`。
- 合并结果：merge commit `b3edc185`。
- 本次运行端口：前端 `8041`，后端 `48041`。

## Evidence

- Git 基线：上一任务遗留文档基线提交 `c640fad2`。
- Merge：`git merge --no-edit origin/int_shedule`，冲突仅限 `docs/local-runtime.md` 和 `docs/experience-index.md`，已保留双方 Docker 依赖门禁。
- Port guard：`scripts\preflight\branch-runtime-port-guard.ps1` 通过，`int_batch` frontend `8041`、backend `48041`。
- Docker dependencies：`int-ruoyi-mysql` 映射 `23306->3306`，`int-ruoyi-redis` 映射 `26379->6379`。
- Backend build：`mvn.cmd -pl yudao-server -am -DskipTests package` -> BUILD SUCCESS，总耗时 `04:50`。
- Backend runtime：PID `22872`，监听 `48041`，`/actuator/health` 返回 `{"status":"UP"}`。
- Frontend runtime：PID `41280`，监听 `8041`，入口 HTTP `200`。
- Playwright homepage：真实浏览器访问 `http://127.0.0.1:8041/`，HTTP `200`，标题 `瑛泰管理系统 - 登录`，最终 URL `http://127.0.0.1:8041/login?redirect=/user/profile`，console/page error 数量 `0`。
- Screenshot：`.runtime\20260725-merge-int-shedule-runtime-e2e\homepage.png`，文件大小 `1061719` 字节。

## Runtime State

- 前端和后端均保持运行，未执行 closeout cleanup。
- 后端启动使用本机 Docker MySQL `127.0.0.1:23306/ruoyi-vue-pro` 和 Redis `127.0.0.1:26379`。

## Experience Consolidation

- 已检查 `project-experience-consolidation`；本次无新长期经验需要新增。
- 现有 `docs/local-runtime.md`、`docs/e2e-rules.md` 与 `docs/experience-index.md` 已覆盖本次复用门禁。
