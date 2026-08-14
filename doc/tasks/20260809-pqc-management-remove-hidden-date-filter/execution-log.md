# Execution Log

## User Intent

- 用户明确要求修改：PQC 管理默认显示无筛选条件时不得执行隐藏的时间过滤。

## Rule And Skill Evidence

- 已读取 `bug-regression-fix-loop`、`backend-api-delivery`、`frontend-feature-delivery` 技能及其证据合同。
- 已读取项目任务、后端、前端、E2E、登录和本机运行规则；未授权远端访问或业务数据写入。

## BDD

- BDD: PQC 管理默认展示历史提交 -> Given 当前 PQC 组长负责范围内存在跨多个提交日期的正式 PQC 事件 / When 用户进入 `PQC管理` 且页面显示“暂无筛选条件” / Then 请求不携带 `submitDate`，列表按正式分页返回负责范围内全部历史提交。
- BDD: PQC 管理显式按提交日期筛选 -> Given 用户明确选择提交日期 / When 应用筛选 / Then 请求携带所选日期并只返回该日事件。
- BDD: 空日期不是默认当天 -> Given API 未收到日期 / When 查询提交列表 / Then 后端不生成时间窗口、不报日期缺失且保留其它正式过滤条件。

## Root Cause

- 前端 `queryParams.submitDate` 初始化和重置为当天，`buildSubmissionParams()` 又强制要求并发送该值；与此同时初始化逻辑清除了可见日期条件，造成 UI 与请求不一致。
- 后端 `ProcessPoolTimelineServiceImpl` 强制要求 `submitDate`，Mapper 无条件应用 `submittedAtStart/submittedAtEnd`，促使前端使用隐藏默认日期规避后端失败。

## Milestone Updates

- M1: completed。根因、BDD、非目标范围和验证路径已确认。
- RED: `node tests/e2e/pqc-leader-management-default-submit-date-static.spec.cjs` -> FAIL，预期原因为前端 API 仍声明 `submitDate: string`。
- RED: `node tests/e2e/pqc-leader-standard-list-template-static.spec.js` -> FAIL，预期原因为 PQC 首屏仍初始化默认当天日期。
- RED: `node yudao-module-mes/src/test/js/process-pool-timeline-mapper-static.spec.cjs` -> FAIL，预期原因为 Mapper 未用动态条件包裹时间窗口。
- RED: Java 聚焦测试通过任务自有 `javac/java @argfile` 执行 -> FAIL，`shouldQueryAllDatesWhenSubmitDateMissing` 在 `ProcessPoolTimelineServiceImpl.prepareSubmitDateWindow` 抛出原有日期必填异常。
- M2: completed。前后端目标行为均已通过失败测试锁定。
- GREEN: `node tests/e2e/pqc-leader-management-default-submit-date-static.spec.cjs` -> PASS。
- GREEN: `node tests/e2e/pqc-leader-standard-list-template-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/team-leader-report-default-filter-empty-static.spec.cjs` -> PASS。
- GREEN: `node tests/e2e/team-leader-report-nearest-submit-date-static.spec.cjs` -> PASS。
- GREEN: `node yudao-module-mes/src/test/js/process-pool-timeline-mapper-static.spec.cjs` -> PASS。
- GREEN: Java 聚焦测试通过任务自有 `javac/java @argfile` 执行 -> PASS，覆盖空日期查询全部历史、显式日期窗口和仅返回所选日期。
- M3: completed。前端仅对 PQC 默认省略日期，生产组长原有默认日期保持不变；后端日期契约改为可选且 Mapper 仅在完整时间窗口存在时过滤。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `mvn.cmd -pl yudao-module-mes -am "-Dtest=ProcessPoolTimelineDateFilterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，3 tests，0 failures/errors/skips，reactor `BUILD SUCCESS`。
- GREEN: 前端 5 个相关静态回归与后端 Mapper 静态合同 -> PASS。
- GREEN: `git diff --check -- <task-owned paths>` -> PASS，仅出现仓库既有 LF/CRLF 提示，无空白错误。
- RUNTIME RED: 首次真实页面请求已正确省略 `submitDate`，但当时 `48081` 仍运行旧 Jar，业务码 `500`，后端日志明确为 `工序池时间轴查询必须提供提交日期`。
- RUNTIME GATE: 热补丁始终以当前运行 Jar 为底；并行任务切换运行 Jar、共享 `target/classes` 变化和旧进程退出延迟均触发 fail-fast，未覆盖并行运行态。最终改用任务隔离 class 输出打包。
- GREEN: 运行 Jar `backend-runtime-control-20260809-pqc-management-no-hidden-date-v4.jar` SHA256 `D3613793981997F46FE752FA2CF4A80316D8F8271B5DA580A07BB60D9872931E`；MES 嵌套模块未压缩，5 个 class 哈希一致，Mapper 条件已核验。
- GREEN: 本机后端 PID `51896` 监听 `48081`，`/actuator/health` -> `UP`。
- GREEN: Playwright 真实只读路径 `/mes/pro/process-pool/pqc-leader` -> `PQC管理`：默认请求无 `submitDate`、页面显示“暂无筛选条件”、业务码 `0`、`total=82`、当前页 10 条；显式选择 `2026-08-08` 后请求携带日期、业务码 `0`、`total=5`、当前页 5 条；MES 写请求 0，pageerror 0。
- GREEN: `validate_bug_regression.py`、`validate_backend_api.py`、`validate_frontend_feature.py` -> PASS。
- EXPERIENCE: 已将“无筛选即不限制结果范围时，前端请求省略参数且后端契约同步可选”合并到 `docs/frontend-development.md#统一列表复合工具栏布局门禁`，并更新 `docs/experience-index.md` 路由关键词。
- M4: completed。默认历史与显式日期两条真实路径均通过。
- CLEANUP: `task-closeout-cleanup --mode preview` -> PASS，无 blocked/warnings；保留核心任务文档、Playwright 验收证据和生效 v4 运行包。
- CLEANUP: `task-closeout-cleanup --mode apply` -> PASS；已删除任务自有聚焦测试中间文件、辅助脚本、热补丁暂存目录和失败的 v1-v3 运行包。
- GREEN: 清理后 `result.json`、验收截图和 v4 运行包仍存在；PID `51896` 的后端 `/actuator/health` -> `UP`。
- M5: completed。证据校验、经验沉淀和任务清理完成；未执行未经授权的 Git 操作。

## Verification History

- 初次标准 Maven 测试曾被并行任务未完成源码阻断；并行源码完成后已重试并正式通过，旧 blocker 已解除。
- `pqc-leader-module-tabs-static.spec.js` 与 `pqc-leader-personnel-tab-static.spec.js` 仍使用不含既有 `history` 页签的旧正则合同，失败与本任务日期行为无关；本任务未修改这些并行/既有契约。

## Blockers

- None。
