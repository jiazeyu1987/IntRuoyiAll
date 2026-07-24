# 执行日志：DCC 未保存目录初始仅显示 QA 规则

- BDD: 未保存目录初始仅显示一条 QA 规则 -> Given 用户从新增目录树选择一个不在左侧列表中的目录 / When 页面切换到该未保存目录 / Then 右侧规则表仅初始化一条主体类型为部门且授权对象为 QA 的规则。
- BDD: 已绑定目录继续显示真实保存规则 -> Given 用户点击左侧已绑定目录 / When 页面加载规则 / Then 右侧继续展示该目录真实保存的全部规则。
- BDD: 用户仍可手动补充更多规则 -> Given 未保存目录已初始化单条 QA 规则 / When 用户点击新增规则 / Then 页面在现有 QA 规则基础上继续新增可编辑规则行。

- INFO: task-created -> 已创建前端任务文档，准备先补“未保存目录初始只给一条 QA 规则”的 RED 静态回归。
- RED: draft-default-qa-static -> FAIL，`node tests/e2e/dcc-access-rule-draft-default-qa-static.spec.js` 初次失败：页面尚未定义默认 `QA` 草稿规则，未保存目录仍直接通过 `loadRules(directoryId)` 加载整组继承规则。
- GREEN: draft-default-qa-static -> PASS，`node tests/e2e/dcc-access-rule-draft-default-qa-static.spec.js` 通过；确认未保存目录改为通过 `loadDraftRules` 初始化单条 `部门/QA` 规则。
- GREEN: bound-directory-regression-static -> PASS，`node tests/e2e/dcc-access-rule-bound-directory-list-static.spec.js` 通过；确认左侧手动目录列表与草稿态识别口径保持不变。
- GREEN: save-validation-regression-static -> PASS，`node tests/e2e/dcc-access-rule-save-validation-static.spec.js` 通过；确认保存校验与草稿态保存后的收口逻辑未回归。
- GREEN: header-context-regression-static -> PASS，`node tests/e2e/dcc-access-rule-header-context-static.spec.js` 通过；确认标题区仍正确显示未保存目录上下文。
- GREEN: ts-check -> PASS，`$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` 通过。
