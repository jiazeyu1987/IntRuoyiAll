# Execution Log

## User Intent

- 用户指出生产组长报工管理的“修改报工内容”弹窗不是给人使用的，要求修复该界面。
- 用户截图显示页面要求手工填写用户 ID、签名 ID、payload JSON、签名快照 JSON和字段变更 JSON。
- 用户进一步明确：修改必须有日志记录，且日志本身也必须符合业务人员阅读习惯。

## BDD Scenarios

- BDD: 组长修改写错的生产报工 -> Given 生产组长打开一条待复核或复核不正确的生产报工 / When 在业务表单中修改完成数量、损耗明细或设备参数，填写修改原因和本人电子签名密码并确认 / Then 系统生成结构化字段差异，以当前登录人生成新的电子签名，保存修订日志并刷新报工列表。
- BDD: 内部协议字段不暴露 -> Given 生产组长打开修改弹窗 / When 页面渲染 / Then 页面展示订单、工序、员工、提交时间和可修改业务字段，不展示 payload JSON、字段变更 JSON、用户 ID、签名 ID或签名快照 JSON。
- BDD: 无实际变化禁止提交 -> Given 生产组长没有改变任何业务字段 / When 点击确认修改 / Then 页面明确提示至少修改一项，不发起修订请求。
- BDD: 签名身份由服务端确定 -> Given 已登录生产组长输入本人电子签名密码 / When 提交修订 / Then 服务端使用当前登录用户校验密码并生成唯一修订签名，客户端不能指定修改人或签名用户身份。
- BDD: 数量片段与报工内容一致 -> Given 被退回且尚未分配的生产报工 / When 组长修改完成数量 / Then 系统同步原始报工 payload、字段差异日志和对应可分配数量片段。
- BDD: 业务人员查看修改记录 -> Given 一条生产报工已有一次或多次有效修改 / When 生产组长在报工列表点击“修改记录” / Then 系统按最新修改在前展示修改人、修改时间、修改原因、电子签名状态和每个业务字段的修改前后值。
- BDD: 修改记录不暴露内部协议 -> Given 生产组长打开修改记录 / When 日志加载完成 / Then 页面不展示事件号、修订号、用户号、签名号、字段代码、原始 payload 或签名快照 JSON。
- BDD: 尚无修改记录 -> Given 生产报工从未修改 / When 打开修改记录 / Then 页面明确显示“暂无修改记录”，不伪造默认日志。
- BDD: 修改记录加载失败 -> Given 正式日志接口返回错误 / When 页面加载修改记录 / Then 页面保留报工上下文并显示失败原因和“重新加载”操作，不以空记录掩盖失败。
- BDD: 无权限查看他组报工日志 -> Given 当前组长不在该报工员工的负责范围 / When 请求修改记录 / Then 服务端拒绝查询，不返回任何修订内容。

## Command And Evidence Log

- 2026-08-07: 已读取 `bug-regression-fix-loop`、`frontend-feature-delivery` 及其证据合同，并读取前端、E2E、编码和任务收尾规则。
- 2026-08-07: 截图复现定位到 `TeamLeaderWorkbenchPage.vue` 的 `correctionVisible` 弹窗；现状把 `ProcessPoolEventRevisionUpdateReqVO` 内部字段逐项暴露为输入控件。
- 2026-08-07: 根因确认：前端没有业务字段编辑模型和差异生成器；后端要求客户端传入 `modifiedByUserId/revisionSignatureId/revisionSignatureUserId/revisionSignatureSnapshot`，未形成面向当前登录人的签名命令。
- 2026-08-07: 路线版本策略确认：修改对象绑定报工事件的 `routeProcessId` 和原始 payload 快照，不跟随最新路线版本漂移；后续路线发布新版本时，历史报工仍按原快照展示和补正，已审核通过记录保持只读。
- 2026-08-07: 前端完成业务化弹窗：报工上下文、完成数量、损耗明细、设备参数、变更预览、修改原因和当前用户签名密码；移除所有内部 ID/JSON 输入。
- 2026-08-07: 后端新增专用业务接口 `/mes/pro/process-pool/event-revision/correct-production-report`；控制器注入当前登录人，服务端校验生产组长员工范围、签名密码和签名身份，自动生成 payload、字段差异和审计快照。
- 2026-08-07: 保留通用 `update-original` 的“必须基于退回复核”约束；专用生产报工接口允许待复核/退回记录，明确阻断已审核通过记录，并继续执行 FIFO 数量片段锁校验。
- 2026-08-07: 移动端真实截图初次发现弹窗底部超出 430x932 视口；改为弹窗根容器纵向 flex、正文独立滚动和固定底部操作区后复验通过。
- 2026-08-07: 用户指出“日志已记录”但缺少人类可读查看入口；根因是修订表和字段差异表只有写链路，控制器、查询服务和前端均未提供正式日志读取能力。

## RED / GREEN / REGRESSION

- RED: `node --test tests/e2e/production-report-correction-human-ui-static.spec.cjs` -> FAIL，5 项中新增 2 项失败，预期原因：页面尚无“修改记录”入口、可读日志弹窗和正式查询 API。
- RED: `mvn -pl yudao-module-mes '-Dtest=MesProcessPoolProductionReportRevisionLogServiceTest,MesProcessPoolProductionReportRevisionLogContractTest' test` -> FAIL，预期原因：可读日志查询服务和响应 VO 尚不存在。
- RED: 聚焦 `javac` 后执行 `mvn -pl yudao-module-mes surefire:test '-Dtest=MesProcessPoolProductionReportRevisionLogServiceTest,MesProcessPoolProductionReportRevisionLogContractTest,MesProcessPoolProductionReportCorrectionServiceTest'` -> FAIL，9 项中 1 项失败；新实现按要求增加“损耗原因”逐项日志，原测试仍只预期损耗合计和设备参数两项，需要更新为新的可读日志合同。
- RED: `mvn -pl yudao-module-mes surefire:test '-Dtest=MesProcessPoolProductionReportRevisionLogContractTest'` -> FAIL，2 项中 1 项失败；日志查询接口仍要求修改权限，无法满足“有查看权限即可阅读日志”的权限合同。
- RED: `node --test tests/e2e/production-report-correction-human-ui-static.spec.cjs` -> FAIL，预期原因：旧弹窗缺少业务字段、稳定定位标记和当前用户密码签名请求。
- RED: `mvn -pl yudao-module-mes '-Dtest=MesProcessPoolProductionReportCorrectionServiceTest,MesProcessPoolProductionReportCorrectionContractTest' test` -> FAIL，预期原因：`MesProcessPoolProductionReportCorrectionService`、命令和请求 VO 尚不存在；同次 testCompile 还报告了并行任务尚未落地的 Mapper，记录为非本任务噪声，不作为本任务降级依据。
- RED: `node --test tests/e2e/production-leader-report-row-modify-action-static.spec.cjs` -> FAIL，预期原因：修改入口被误收紧为仅 `REJECTED`，违反既有“生产组长可直接修改待复核生产报工”合同。
- RED: `mvn -pl yudao-module-mes '-Dtest=MesProcessPoolProductionReportCorrectionServiceTest,MesProcessPoolProductionReportRevisionPolicyTest' test` -> FAIL，预期原因：专用生产报工修订策略、服务端 actor 和组长范围校验尚未实现。
- RED: `node doc/tasks/20260807-production-report-correction-human-ui/production-report-correction-readonly.e2e.cjs` -> FAIL，预期原因：移动端弹窗底部坐标 `1129.765625` 超出 932px 视口。
- GREEN: `node --test tests/e2e/production-report-correction-human-ui-static.spec.cjs tests/e2e/production-leader-report-row-modify-action-static.spec.cjs` -> PASS，4/4。
- GREEN: 聚焦 `javac` 编译 6 个本任务后端主源码及 3 个测试源码 -> PASS；显式 UTF-8、Java 17，使用任务自有输出目录。
- GREEN: `mvn -pl yudao-module-mes surefire:test '-Dtest=MesProcessPoolProductionReportCorrectionServiceTest,MesProcessPoolProductionReportRevisionPolicyTest,MesProcessPoolProductionReportCorrectionContractTest'` -> PASS，6/6。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `node doc/tasks/20260807-production-report-correction-human-ui/production-report-correction-readonly.e2e.cjs` -> PASS，真实页面事件 `176`，桌面与 430x932 移动端无横向/纵向越界，签名输入不被底部操作区遮挡，`mesWriteRequests=[]`。
- REGRESSION: `production-leader-function-tabs-static`、`production-leader-report-row-modify-action-static`、`production-report-correction-human-ui-static`、`team-leader-production-report-history-tab-static` 共 6 个断言通过。
- REGRESSION NOTE: `team-leader-pqc-review-gate-static.spec.js` 仍失败于其旧断言要求 `canReviewSubmission` 不包含历史页签只读门禁；该失败未命中本任务修改入口/弹窗/接口代码，且 `team-leader-production-report-history-tab-static` 已证明当前历史页签只读行为通过，本任务未回退历史页签门禁。

## Blockers

- 无本任务功能阻塞。
- 写入型 Playwright 未执行：当前仅授权本机默认 `芋道源码/admin`，`docs/e2e-rules.md` 禁止在 admin 基线创建或修改 MES 数据；提交链路由 6 个聚焦后端测试覆盖。
- 本机 48081 运行态仍会对并发任务新增的只读 `/team-device/list` 请求返回 404 通知；该接口不属于本任务，真实验收等待通知自然关闭后截图，未修改并发任务或重启共享后端。
- EXPERIENCE: `project-experience-consolidation` 将“业务 UI 不暴露内部 JSON/ID，审计身份和派生差异由服务端生成”合并到 `docs/frontend-development.md#业务运行记录用户可读展示门禁` 与 `docs/backend-development.md#业务修订审计身份服务端归属门禁`，并更新 `docs/experience-index.md`；未新建长期经验文档。
- CLOSEOUT: `task-closeout-cleanup` preview -> PASS，无 blocked/warnings；明确保留三个核心任务文档和三张桌面/移动端验收截图。
- CLOSEOUT: `task-closeout-cleanup` apply -> PASS，清理聚焦编译 class/arg/classpath、一次性 Playwright 脚本/result 和技能中间 evidence；未删除正式源码、正式测试、截图或其它任务产物。
- FINAL: 任务状态更新为 `completed`，未执行 Git stage/commit/push。
