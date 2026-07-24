# 执行日志

## BDD

- BDD: 同名导入确认升版 -> Given 测试租户批记录名称和路线已存在 / When 用户再次选择同名 Word 导入 / Then 页面先提示是否升版，确认后才以 `upgrade=true` 调用真实导入接口。
- BDD: 同名导入选择否放弃任务 -> Given 页面提示同名批记录是否升版 / When 用户选择否或关闭确认框 / Then 页面清理本次导入状态并返回，不调用 `recognizeUploadedRoute`，不产生写入。

## RED

- RED: `node scripts\electronic-batch-record-word-import.test.mjs; node scripts\edhr-batch-version-phase1-contract.test.mjs` -> FAIL，新增契约要求确认框包含“是否升版 / 升版 / 否，放弃本次导入”；同时发现该旧静态测试受当前工作区其它未提交页面改动影响，存在 `accept=".doc,.docx"`、`targetReportId` 等无关失败。

## GREEN

- GREEN: `node scripts\edhr-duplicate-name-upgrade-confirm.test.mjs` -> PASS，确认同名导入会先提示是否升版，取消文案为“否，放弃本次导入”，并且确认发生在写入接口之前。
- GREEN: `node scripts\edhr-batch-version-phase1-contract.test.mjs` -> PASS，阶段一升版语义契约通过。
- TODO: `node scripts\electronic-batch-record-word-import.test.mjs` 已补齐当前页面 `.doc,.docx` 与 `targetReportId` 查询结构断言，等待复跑。
- RED: duplicate-name-cancel-real-e2e -> `node tests\e2e\edhr-duplicate-name-upgrade-cancel-real-flow.e2e.js` -> FAIL，`message.confirm` 封装固定渲染“确定/取消”，未显示“否，放弃本次导入”。
- GREEN: experience-preflight -> PASS，官方登录预检使用系统 Chrome 进入本机 `http://127.0.0.1:8096/mes/pro/batch-record-template`，租户 `测试租户/aoteman`。
- GREEN: duplicate-name-contracts -> PASS，`node scripts\edhr-duplicate-name-upgrade-confirm.test.mjs`、`node scripts\edhr-batch-version-phase1-contract.test.mjs`、`node scripts\electronic-batch-record-word-import.test.mjs` 均通过。
- GREEN: duplicate-name-cancel-real-e2e -> PASS，`node tests\e2e\edhr-duplicate-name-upgrade-cancel-real-flow.e2e.js` 使用真实测试租户数据 `E2E-PHASE2-1783564189622` 触发同名确认，选择“否，放弃本次导入”后 `recognize-uploaded` 写请求数为 `0`。
- GREEN: frontend-evidence-validator -> PASS，`frontend-feature-evidence.md` 结构校验通过。
