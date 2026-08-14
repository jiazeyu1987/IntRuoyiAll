# Execution Log

## User Intent

- 用户要求按已确认方案完成一线 PQC 正式提交的设计、开发和验证。
- 正式提交不得通过解除禁用、URL 注入签名编号、默认生产事件或空检验项目成功实现。

## BDD Scenarios

- BDD: 正式 PQC 提交形成可追溯回执 -> Given 当前登录 PQC 人员选择待执行任务、明确绑定生产提交事件、完成发布态 QA 规程全部逐件检验并通过电子签名 / When 点击提交 / Then 系统在一个事务中保存签名、逐件明细和 PQC 工序池事件，将任务标记为 SUBMITTED，并返回正式回执。
- BDD: 缺少正式检验项目时明确阻塞 -> Given 当前订单或工序缺少发布态 QA 检验项目或待执行 PQC 任务 / When 用户点击提交 / Then 页面显示正式前置缺失原因，不调用提交接口，也不显示默认成功。
- BDD: 多个生产提交事件要求明确选择 -> Given 当前 PQC 任务对应多个可绑定的生产提交事件 / When 用户尚未选择目标事件并点击提交 / Then 页面明确要求选择生产提交，不自动使用第一条、最后一条或 URL 参数。
- BDD: 电子签名由当前提交生成 -> Given 用户填写完成但尚未完成电子签名 / When 点击提交并输入电子签名密码 / Then 后端验证当前登录人并生成本次提交的签名证据，不接受客户端提供既有签名编号冒充本次签名。
- BDD: 正式结果由服务端计算 -> Given 逐件检验存在不合格值或损耗数量大于零 / When 提交 / Then 服务端计算结果为不合格并要求非空不良说明，客户端不能把结果改写为合格。
- BDD: 重复正式提交保持唯一 -> Given 同一 PQC 任务已成功提交 / When 客户端因重复点击或网络重试再次提交同一幂等令牌 / Then 系统返回同一正式结果，不新增第二条签名、逐件明细或 PQC 事件。

## TDD Evidence

- RED: `node tests\e2e\frontline-pqc-formal-submit-static.spec.js` -> FAIL，前端尚无 `FrontlinePqcProductionSubmitCandidateVO`、正式签名口令请求契约和正式回执状态。
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，后端尚无生产提交候选、服务端签名生成、结构化损耗和正式回执契约；同时 Maven 全量 `testCompile` 暴露当前任务范围外的既存编译错误，涉及 `MesProcessPoolTeamLeaderControllerTest`、`MesFrontlineDeviceAccountContextServiceTest` 和 `MesProcessPoolProductionReportCorrectionServiceTest`，后续回归需单独复核该基线阻塞。
- RED: `node tests\e2e\frontline-pqc-formal-submit-static.spec.js` -> FAIL，提交前置失败会逃逸到原生事件处理器，页面没有稳定错误反馈。
- GREEN: `mvn -pl yudao-module-mes -Pmes-frontline-pqc-formal-submit-targeted-tests test` -> PASS，35 tests，0 failures，0 errors，覆盖正式请求/回执 schema、生产提交候选、签名、服务端判定、事务写入、回执和幂等。
- GREEN: `node tests\e2e\frontline-pqc-formal-submit-static.spec.js` -> PASS，前置失败通过页面消息显式暴露，不再逃逸为未处理事件异常。
- REGRESSION: `pnpm ts:check`、6 组受影响前端静态合同、Maven 生产编译和定向 JUnit 全部通过。

## Milestone Updates

- M1 completed：确认后端已有事务提交主体，但前端生产提交事件和签名编号来自路由；PQC 空态与加载/错误/未选择混为“暂无检验项目”；提交结果只返回任务 ID。
- Experience gate loaded：正式提交继续以发布态 QA 规程和结构化 `itemResults[]` 为权威，真实路径必须通过整条活跃路线前置校验，不得用 API-only、raw payload 或默认项目替代。
- M2 completed：以 6 个 Given/When/Then 场景固定正式回执、前置阻塞、多来源显式选择、本次签名、服务端结论和幂等行为；前后端 RED 均按预期失败。
- M3 completed：后端返回逐工序生产提交候选；控制器只接收签名密码和结构化损耗；服务层在事务内生成 `PQC_SUBMIT` 签名、按发布态 QA 标准判定逐件结果、CAS 提交任务、落逐件明细和工序池事件，并从正式事件及 PQC 记录组装回执。
- M4 completed：前端移除 PQC 路由签名/生产事件依赖，单候选自动选择、多候选强制选择；提交点击先显式校验，再打开密码签名弹窗；成功后展示事件、记录、签名、服务端结论和时间回执并锁定重复提交。
- M5 completed：增加正式 Maven 定向测试 profile，后端 35 个测试通过；前端类型检查和 6 组静态合同通过；Playwright 登录真实页面并验证缺正式前置时提交按钮可点击、页面显示明确错误且未产生写入。成功写入型 E2E 因当前正式测试数据缺少待执行任务或发布态规程而按规则阻塞。
- M6 completed：将“命令按钮失败必须终止在可见错误边界”合并到 `docs/frontend-development.md` 并更新 `docs/experience-index.md`；`task-closeout-cleanup` preview 无 blocked/warnings，apply 仅删除本任务设计草稿、临时测试工程、截图、定向产物和本任务 Playwright 快照，保留三份核心任务记录。
- Skill evidence validation：`validate_backend_api.py` -> PASS；`validate_frontend_feature.py` -> PASS。结果已归档到 `verification-report.md`，证据中间文件进入最终 cleanup 删除集。

## Verification Evidence

- `node tests\e2e\frontline-pqc-formal-submit-static.spec.js` -> PASS。
- `node tests\e2e\mes-frontline-pqc-submit-to-leader-chain-static.spec.js` -> PASS。
- `node tests\e2e\p0-production-execution-loop-static.spec.cjs` -> PASS。
- `node tests\e2e\pqc-production-source-context-static.spec.cjs` -> PASS。
- `node tests\e2e\frontline-formal-submit-static.spec.cjs` -> PASS。
- `node tests\e2e\edhr-frontline-pqc-fullscreen-toggle-static.spec.cjs` -> PASS。
- `pnpm ts:check` -> PASS。
- `mvn -pl yudao-module-mes -am -DskipTests compile` -> PASS。
- `mvn -pl yudao-module-mes -Pmes-frontline-pqc-formal-submit-targeted-tests test` -> PASS；35 tests，0 failures，0 errors，0 skipped。
- `git diff --check -- <task-owned paths>` -> PASS；仅有 Windows 行尾提示，无空白错误。
- 后端 `http://127.0.0.1:48081/actuator/health` -> `UP`。
- Playwright `http://127.0.0.1:8081/mes/pro/feedback/edhr-batch-pqc-fill` -> 登录并打开真实一线 PQC 页面；1920x1080 下订单选择和提交入口可操作。
- Playwright 缺前置提交 -> PASS；点击提交显示“当前工序缺少待执行PQC任务或发布态QA规程快照”，修复后没有新增未处理事件异常，也没有调用正式提交写接口。

## Blockers

- 成功写入型真实 E2E 缺少正式测试数据前置。当前可见 `CODX-PQC-20260807-SP-WO-*` 订单缺少待执行 PQC 检验任务；`PQC-E2E-FS-20260804` 缺少已发布 QA 检验规程。按项目门禁未通过 API-only、mock、默认项目或伪造规程绕过。
- 仓库全量 `testCompile` 仍存在本任务范围外的大量缺失测试类型；本任务沿用仓库现有定向 profile 模式执行了真实 35 个 JUnit，不把全量基线错误计入本任务改动。
- 非本任务阻塞观察：现有订单选择器在约 1280px 宽窗口布局拥挤并可能拦截点击；1920x1080 目标工作台尺寸可正常操作，本任务未扩展为响应式重构。

## Closeout Evidence

- `project-experience-consolidation`：经验合并到现有 `docs/frontend-development.md#前端命令按钮失败必须终止在可见错误边界门禁`，未新建长期经验文档。
- `task-closeout-cleanup preview`：status `ready`，blocked/warnings 均为空。
- `task-closeout-cleanup apply`：status `applied`；仅清理本任务临时产物，保留 `task.md`、`execution-log.md`、`verification-report.md`。
- backend/frontend delivery evidence validators：均 PASS，归档结果后按收尾门禁清理中间 evidence 文件。
- 最终 `task-closeout-cleanup`：preview `ready`、apply `applied`，blocked/warnings 均为空；已删除两份中间 evidence 文件，保留三份核心记录。
- Git：用户未要求 staging、commit、merge 或 push，本任务未执行 Git 写操作。
