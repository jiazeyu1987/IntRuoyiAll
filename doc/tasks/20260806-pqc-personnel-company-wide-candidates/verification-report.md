# Verification Report

## Scope

PQC 组长新增检验员时，候选下拉范围调整为全公司系统用户搜索；提交关联校验与候选范围保持一致，不再按当前组长下属过滤。

## Passed Verification

- `node tests/e2e/pqc-leader-personnel-company-wide-candidates-static.spec.js`：PASS，确认 PQC 候选接口复用全公司正式用户搜索，关联方法不含 `getUserListBySubordinate` 或 `PRO_PROCESS_POOL_TEAM_SCOPE_DENIED`。
- `node tests/e2e/pqc-leader-personnel-tab-static.spec.js`：PASS，确认 PQC 人员管理 tab 现有入口和列表契约不回归。
- `mvn -pl yudao-module-mes -am "-Dtest=MesPqcLeaderPersonnelServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：PASS，独立验证 worktree 中 `BUILD SUCCESS`，`Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`。
- `mvn -pl yudao-server -am "-DskipTests" package`：PASS，独立验证 worktree 中 `BUILD SUCCESS`，生成 `yudao-server-exec.jar`。
- `jar tf yudao-server-exec.jar`：PASS，产物包含 `BOOT-INF/lib/yudao-module-mes-2026.04-SNAPSHOT.jar`。
- `jar tf yudao-module-mes-2026.04-SNAPSHOT.jar`：PASS，模块 jar 包含 `MesPqcLeaderPersonnelServiceImpl.class`。
- `git diff --check -- <task paths>`：PASS，无空白错误；仅有 Java 文件 LF/CRLF 规范化提示。
- Worktree cleanup：PASS，`D:\IntRuoyiWorktree\pqc-personnel-company-wide-candidates-20260806` 已删除，`Test-Path=False` 且 Git worktree 登记不存在。

## Blocked Verification

- `pnpm ts:check`：FAIL，当前工作区已有活跃订单类型不一致导致 `TeamLeaderWorkbenchPage.vue(3760,7)` 报 `routeId` 不存在于 `TeamLeaderActiveOrderAddReqVO`。该问题来自当前工作区其他未收敛改动，非 PQC 候选范围逻辑。
- Current runtime refresh：BLOCKED，`48081` 当前运行 `E:\IntRuoyi\output\runtime\int_main\backend-runtime-production-formal-users-20260806.jar`；只读内嵌 MES class 检查仍发现旧 `getUserListBySubordinate` / `PRO_PROCESS_POOL_TEAM_SCOPE_DENIED` 信号。源码与独立构建产物已正确，但未替换或重启当前本地后端。

## No-Fallback Review

- 未引入 fallback、降级、吞异常或静默成功。
- 候选查询和提交关联校验均走正式系统用户能力；重复 PQC scope 仍显式报错。
- 未修改生产组长正式工候选逻辑，未混入生产人员档案表。
