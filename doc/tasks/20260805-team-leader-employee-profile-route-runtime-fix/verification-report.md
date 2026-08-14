# Verification Report

## Summary

本机 `int_main` 后端 `48081` 已修复生产人员档案列表接口运行态。根因是旧运行 Jar 的内嵌 MES 模块未包含 `employee-profile/list` 路由；已从干净 worktree 构建 MES 模块并生成 hotpatch Jar 启动。

## Evidence

| Check | Result |
| --- | --- |
| 源码路由 | PASS：`MesProcessPoolTeamLeaderController.java` 包含 `@GetMapping("/employee-profile/list")`，前端 wrapper 调用相同业务路径。 |
| 旧运行 Jar | RED：旧 Jar 内 Controller class 不包含 `employee-profile/list` 常量。 |
| clean worktree build | PASS：`mvn.cmd -pl yudao-module-mes -am "-DskipTests" package` 成功。 |
| hotpatch Jar | PASS：SHA256 `F114FA94AAB6FA7645729960BF17C1C8900B4BCA7B00F781AE3F7309F21FB629`，内嵌 MES module 未压缩存储且包含目标路由。 |
| 运行态 | PASS：`48081` listener PID `46768` 使用 hotpatch Jar，`/actuator/health` HTTP `200`。 |
| 登录态接口 | PASS：默认本机账号登录业务 `code=0`；`/admin-api/mes/pro/process-pool/team-leader/employee-profile/list` HTTP `200`、业务 `code=0`、返回数组。 |

## Residual Notes

- 当前任务未修改业务源码；只刷新本机运行态。
- 构建 worktree `D:\IntRuoyiWorktree\20260805-team-leader-route-runtime-build` 已移除。
- `task-closeout-cleanup` apply 已删除任务临时脚本，仅保留正式任务记录。
- 非 Git 临时 staging 目录 `E:\IntRuoyi\output\runtime\int_main\team-leader-hotpatch-staging-20260805-203537` 删除被本地策略拦截；不影响 `48081` hotpatch Jar。
- 任务证据未提交，原因是主工作区存在大量非本任务脏改动且分支已 ahead；为避免混入并行任务，未执行提交/推送。
