# Frontend Feature Evidence

## Feature Goal And Non-Goals

- Goal: QA 业务工序在批记录表单尚未识别绑定时仍继续显示，并进入正式保存/发布载荷。
- Goal: 保留已识别路线工序的逐工序正式身份。
- Non-goal: 不猜测未识别业务工序对应组装Ⅰ/Ⅱ、光固Ⅰ/Ⅱ等路线工序；不修改后端 API 或数据库。

## Requirements And Acceptance IDs

- AC-CR1: 未识别批记录绑定的 QA 业务工序不阻断保存/发布，并保留页面原始工序名称。
- AC-CR2: 未匹配项目归入页面已正式解析的 QA 质检工序载荷，批记录绑定摘要保持空。
- AC-CR3: 已匹配业务工序仍按正式路线工序身份独立发布。

## UI Entry Points And Owned Files

- Route: `/mes/pro/process-pool/qa-regulation`。
- Page: `IntRuoyiFronted/src/views/mes/pro/processpool/QaRegulationPage.vue`。
- Test: `IntRuoyiFronted/tests/e2e/qa-regulation-unbound-process-visible-static.spec.cjs`。

## API Contracts And Data States

- 保持 `QaInspectionRegulationSaveReqVO` 不变，继续要求正式 `routeProcessId/processId`。
- 已匹配状态：使用匹配路线工序身份和对应批记录摘要。
- 未匹配状态：使用页面已经解析的正式 QA 质检工序身份，不构造虚假批记录摘要。
- 错误状态：正式 QA 质检工序身份本身缺失时仍 fail fast。

## BDD Scenarios

- BDD: 未识别业务工序继续显示 -> Given 未识别批记录绑定的 QA 业务工序 When 构建保存载荷 Then 原业务工序继续显示，项目进入正式 QA 质检工序载荷且不因名称未匹配报错。
- BDD: 已识别业务工序保持正式分组 -> Given 可唯一匹配的 QA 业务工序 When 构建保存载荷 Then 继续按匹配的正式路线工序分组。

## RED Evidence

- RED: `node tests/e2e/qa-regulation-unbound-process-visible-static.spec.cjs` -> FAIL。
- Expected failure: `QA_UNBOUND_BATCH_RECORD_PROCESS_NAMES_BY_PROJECT_CODE` 不存在，当前零匹配分支仍抛出“未匹配激活路线版本”错误。

## GREEN Evidence

- GREEN: `node tests/e2e/qa-regulation-unbound-process-visible-static.spec.cjs` -> PASS。
- GREEN: `node tests/e2e/qa-regulation-process-scoped-publish-static.spec.cjs` -> PASS。
- GREEN: `node tests/e2e/frontline-pqc-sampling-equipment-dialog-static.spec.cjs` -> PASS。
- GREEN: `node tests/e2e/qa-regulation-applicable-types-derived-static.spec.js` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: 任务范围 `git diff --check` -> PASS，仅有换行转换 warning。
- REGRESSION: IDI 完整项目、逐页截图、QA standalone role-matrix 合同 -> PASS。
- Known adjacent failure: `qa-regulation-route-checkflag-fallback-static.spec.cjs` 仍断言已淘汰的 Promise 解构结构，与本次分支无关。

## UI State Checks

- Responsive: 不改变布局。
- Accessibility: 不改变控件与语义结构。
- Loading/empty/error: 保留现有正式路线加载状态；只有业务工序名称未匹配时不再作为错误。
- Permission: 保持原 QA 菜单与保存/发布权限。

## E2E Path

- 使用 Playwright 登录本机“芋道源码”，进入 QA 页面选择 IDI，确认完整业务工序仍显示并能越过原未匹配校验；一线详情弹窗继续验证抽样方案和检验器具及设备。
- Real read-only result: PASS。选择 `IDI / 按压式球囊扩充压力泵 / 1` 后，真实表格可见“组装螺杆八组件”“光固外套四组件”“检验器具及设备”“抽样方案”；`qaWriteRequests=[]`、`consoleErrors=[]`、`pageErrors=[]`。
- Publish boundary: “发布规程”当前会直接写入不可变版本，没有只读预检查确认框，因此本轮未点击；未把写入规程当作只读验证。

## Verification

- AC-CR1: PASS，静态合同与真实 QA 表格均证明未识别工序继续显示。
- AC-CR2: PASS，静态合同锁定未匹配项目使用正式 QA 质检工序身份且不生成批记录绑定摘要。
- AC-CR3: PASS，原逐路线工序发布合同保持通过。
- Overall original task: BLOCKED，仅因一线 PQC 弹窗完整真实路径缺少可清理的已发布 QA 数据。

## Blockers And Follow-Up Skills

- 本次前端行为无实现 blocker，真实只读验证通过。
- 原任务完整一线弹窗真实路径仍缺可通过同一页面清理的已发布 QA 数据；不可用任务规程永久残留、API 或 SQL 清理替代。
- Follow-up: `independent-verification-gate`、`project-experience-consolidation`。
