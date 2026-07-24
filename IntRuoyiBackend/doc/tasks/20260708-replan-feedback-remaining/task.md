# 任务：排产重排支持已报工剩余量

## 任务目标

将排产重排中的报工保护从“整条任务冻结”调整为“已报工任务保留，工序剩余数量继续参与重排”。例如工序计划 1000 个、已报工 200 个、剩余 800 个时，重排预览和应用应保留已报工任务，并为 800 个剩余量生成新的重排任务。

## 经验门禁

- 已读取 `docs/powershell-memory.md`：PowerShell 命令、中文文档读写和 Maven 参数必须显式 UTF-8 / 参数安全。
- 已读取 `docs/experience-index.md` 与 `docs/agent-memory/project-error-prevention.md`：排产重排联动必须区分 `FEEDBACK` 保护与“按剩余量重排”，不能把保护通过等同剩余量算法完成。
- 已读取 `bug-regression-fix-loop` 与 `backend-api-delivery`：本次行为修复必须先补失败回归测试，再最小实现，并记录 RED/GREEN 证据。
- 当前后端仓库存在其他任务脏改；本任务只修改自动排产服务、自动排产测试和本任务文档，不回退、不暂存无关改动。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；直接修正重排数量来源，按排产工序快照的 `remainingQuantity` 生成剩余任务。
- 是否存在临时补丁或绕过：否。

## 里程碑

- [x] M1：确认当前 `FEEDBACK` 报工保护和剩余量重排边界。
- [x] M2：补充 “1000 已报工 200 剩余 800 参与重排” 回归测试。
- [x] M3：实现剩余量重排数量口径。
- [x] M4：运行自动排产目标回归并记录 GREEN；整类回归受无关新增测试编译错误阻塞，已记录。
- [x] M5：执行任务收尾预览；混合工作区存在无关脏改，本任务不混入无关改动。

## BDD 场景

- BDD: 已报工工序剩余量参与重排 -> Given 排产工序计划数量 1000、已报工 200、剩余 800 且已有非草稿报工任务 / When 用户发起重排预览 / Then 预览保留已报工任务，并生成数量为 800 的新重排任务。
- BDD: 无剩余量的已报工工序不重复排产 -> Given 排产工序无剩余数量且已有非草稿报工任务 / When 用户发起重排预览 / Then 仅显示报工保护任务，不生成额外任务。

## 预期验证

- `mvn -pl yudao-module-system,yudao-framework/yudao-spring-boot-starter-excel -am "-DskipTests" install`
- `mvn -pl yudao-module-mes "-Dtest=MesProAutoScheduleServiceImplTest#replanPreview_shouldScheduleRemainingQuantityWhenFeedbackTaskProtected" test`
- `mvn -pl yudao-module-mes "-Dtest=MesProAutoScheduleServiceImplTest" test`
- `node tests/e2e/mes-replan-feedback-remaining-readonly.e2e.js`

## 验证结果

- `mvn -pl yudao-module-system,yudao-framework/yudao-spring-boot-starter-excel -am "-DskipTests" install`：PASS，用于刷新本地依赖产物，避免 unrelated 导出列改动导致 MES 单模块编译缺少新 ExcelUtils 签名。
- `mvn -pl yudao-module-mes "-Dtest=MesProAutoScheduleServiceImplTest#replanPreview_shouldScheduleRemainingQuantityWhenFeedbackTaskProtected" test`：PASS，确认报工保护任务保留且剩余 800 生成新预览任务。
- `mvn -pl yudao-module-mes "-Dtest=MesProAutoScheduleServiceImplTest" test`：BLOCKED，`MesProRouteProductServiceImplTest` 引用缺失的 `MesProRouteProductCopyReqVO`，属于当前混合工作区内其他未完成任务新增测试，不属于本任务改动。
- `task-closeout-cleanup --task-id 20260708-replan-feedback-remaining --mode preview --worktree-closeout off`：PASS，预览仅建议保留 `task.md`、`execution-log.md`，可删除额外 evidence 文件。
- `node tests/e2e/mes-replan-feedback-remaining-readonly.e2e.js`：BLOCKED，真实登录和本机运行态通过，但测试租户现有已报工剩余量工单均被工艺路线缺失/禁用阻塞，无法在真实页面生成剩余量重排预览。
- `PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH="C:\Program Files\Google\Chrome\Application\chrome.exe"; node tests/e2e/mes-replan-feedback-remaining-readonly.e2e.js`：BLOCKED，系统 Chrome 真实浏览器已启动并进入页面扫描，当前测试租户未找到可用于只读重排预览的真实已报工剩余量排产工单；已检查候选列表中仅 `SCH-CODexERP20260610E-20260610-0001` 存在报工进度，但不满足可预览剩余量重排条件。

## Current Status

completed

## 当前状态

`COMPLETED_WITH_E2E_BLOCKER`：已修正报工保护下的剩余量重排口径；目标回归通过；真实数据 E2E 的登录、运行态、系统 Chrome 页面扫描已执行，但测试租户现有真实数据没有可用于只读预览的“已报工且剩余量可重排”候选，无法完成页面预览验收。
