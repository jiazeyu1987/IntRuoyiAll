# 验证报告

## 范围

- 在测试管理中新增项目分类 `工艺路线`。
- 通过真实前端页面新增并回读 4 个工艺路线操作场景测试项。
- 验证前后端项目枚举、页面下拉/筛选/列表标签、后端保存校验和类型检查。

## 运行态

- 前端：`http://127.0.0.1:8082/` -> HTTP 200。
- 后端：`http://127.0.0.1:48082/actuator/health` -> `UP`。
- 后端启动使用本任务 worktree jar 与登记端口 `48082`，JVM 限制参数用于规避本机 Java 21 C2 native 内存崩溃，不切换端口或数据源。

## 结果

- `node doc\tasks\20260726-codex-test-process-route-case\ensure-process-route-codex-test-items.e2e.cjs` -> PASS。
- 新增测试项：
  - `工艺路线基础信息与工序维护闭环`，ID `18`，4 个目标项。
  - `工艺路线复制与产品绑定闭环`，ID `19`，4 个目标项。
  - `工艺路线候选版本编辑发布闭环`，ID `20`，4 个目标项。
  - `工艺路线状态切换与删除约束闭环`，ID `21`，4 个目标项。
- 4 个测试项均回读为 `project=工艺路线`、`defaultExecutionMode=SEQUENTIAL`、`parallelSafe=false`、`status=ENABLE`。
- 只读摘要文件：`doc/tasks/20260726-codex-test-process-route-case/artifacts/process-route-codex-test-items-summary.json`。

## 回归

- `node IntRuoyiFronted\tests\e2e\system-codex-test-management-static.spec.js` -> PASS。
- `node --check doc\tasks\20260726-codex-test-process-route-case\ensure-process-route-codex-test-items.e2e.cjs` -> PASS。
- `pnpm ts:check` -> PASS。
- `mvn.cmd -pl yudao-module-system "-Dtest=CodexTestCaseServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" "-DforkCount=0" test` -> PASS，5 tests，0 failures，0 errors。
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc\tasks\20260726-codex-test-process-route-case\backend-api-evidence.md` -> PASS。
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc\tasks\20260726-codex-test-process-route-case\frontend-feature-evidence.md` -> PASS。
- `project-experience-consolidation` -> 已合并到 `docs/worktree-memory.md`，未新建长期经验文档。

## 注意事项

- `mvn.cmd -pl yudao-module-system -am "-Dtest=CodexTestCaseServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` 在本机 Java 21 fork/surefire 资源环境下曾超时并产生 dumpstream；目标测试历史 `-am` GREEN 已记录，当前改动后使用同模块非 fork 目标测试完成复核。
- 无未解决 blocker。
