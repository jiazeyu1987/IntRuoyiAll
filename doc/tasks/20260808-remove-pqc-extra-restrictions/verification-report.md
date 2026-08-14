# Verification Report

## Scope

- 用户最新要求：一线 PQC 提交只保留两个业务阻断：电子签名密码非空、检验数量大于 0。
- 本轮调整：`productionSubmitEventId`、活跃订单强校验、任务身份严格匹配、任务状态、逐件项目明细、设备、损耗说明、模板预校验等均不再作为提交前置；可用上下文仍作为追溯字段写入。
- 保留前提：提交目标仍来自当前一线 PQC 页面选择的正式 `pqcTaskId`，后端以该任务补齐可追溯上下文。

## Passed Verification

- `node tests/e2e/frontline-pqc-extra-restrictions-removed-static.spec.cjs`：PASS。
- `node tests/e2e/frontline-pqc-final-inspection-switch-static.spec.cjs`：PASS。
- `node tests/e2e/pqc-item-equipment-standard-method-static.spec.js`：PASS。
- `node tests/e2e/pqc-inspection-tabs-layout-static.spec.js`：PASS。
- `node tests/e2e/pqc-tab-item-name-display-static.spec.cjs`：PASS。
- `node tests/e2e/pqc-active-title-method-display-static.spec.cjs`：PASS。
- `node tests/e2e/pqc-requirement-alignment-static.spec.cjs`：PASS。
- `pnpm ts:check`：PASS。
- `mvn -pl yudao-module-mes -DskipTests compile`：PASS。
- `mvn -pl yudao-module-mes "-Dtest=MesFrontlinePqcContextServiceTest#shouldSubmitPqcInspectionWithOnlySignatureAndPositiveQuantity" test`：PASS，1 个用例通过。
- 残留扫描：PASS，未命中生产提交事件强制、PQC 正式上下文断言、任务身份/任务选项强制、模板预校验、设备强制或样本数量强制等提交阻断。
- `git diff --check`：PASS；仅输出工作区 CRLF 提示，无 whitespace error。

## Notes

- 首次后端定向 Maven 在 `testCompile` 阶段遇到同模块 `target\classes` 大量 class 文件缺失；按既有 Maven target 门禁先运行主代码编译重建目标目录，再复跑目标 JUnit，最终 Surefire 到达并通过。

## 2026-08-09 Reverification

- `node tests/e2e/frontline-pqc-extra-restrictions-removed-static.spec.cjs`：PASS。
- `node tests/e2e/pqc-requirement-alignment-static.spec.cjs`：PASS。
- `node tests/e2e/frontline-pqc-final-inspection-switch-static.spec.cjs`：PASS。
- `node tests/e2e/pqc-item-equipment-standard-method-static.spec.js`：PASS。
- `node tests/e2e/pqc-inspection-tabs-layout-static.spec.js`：PASS。
- `pnpm ts:check`：PASS。
- `mvn -pl yudao-module-mes "-Dtest=MesFrontlinePqcContextServiceTest#shouldSubmitPqcInspectionWithOnlySignatureAndPositiveQuantity" test`：PASS，1 个用例通过。
- 残留扫描：PASS，未命中生产提交事件强制、PQC 正式上下文断言、任务身份/任务选项强制、模板预校验、设备强制或样本数量强制等提交阻断。
- `git diff --check`：PASS；仅输出工作区 CRLF 提示，无 whitespace error。
## Result

- 本轮功能实现、静态契约、前端类型检查、后端主编译、后端目标 JUnit 和 whitespace 检查均通过。
- 当前无阻塞。
