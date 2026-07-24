# 执行日志：DCC 访问规则保存校验误报回归修复

- BDD: 已绑定目录不应显示未保存目录状态 -> Given 后端已返回当前目录的访问规则且该目录属于已绑定目录列表 / When 页面完成初始化 / Then 标题区不得显示“未保存目录”，左侧对应目录项应保持选中。
- BDD: 已有完整规则点击保存不应误报授权对象缺失 -> Given 当前目录所有规则都带真实 subjectType 与 subjectId / When 用户直接点击保存规则 / Then 页面不得提示“请完善授权对象后再保存”。
- BDD: 新增空白规则仍阻止保存 -> Given 用户新增一条默认规则且没有选择授权对象 / When 点击保存规则 / Then 页面继续提示“请完善授权对象后再保存”，防止提交空规则。

- INFO: task-created -> 已创建前端缺陷任务台账，准备补只读复现证据与 RED 静态合同。
- GREEN: experience-preflight -> PASS，本轮仅执行本机真实登录最小路径与只读页面复现，不做保存写入。
- RED: `node tests/e2e/dcc-access-rule-save-validation-static.spec.js` -> FAIL, `access-rule page must isolate invalid-row detection in a helper`，说明当前保存校验仍只给出泛化告警，无法定位空白规则行。
- GREEN: `node tests/e2e/dcc-access-rule-save-validation-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/dcc-access-rule-header-context-static.spec.js` -> PASS。
- INFO: `node tests/e2e/dcc-access-rule-bound-directory-list-static.spec.js` 当前失败于 `package.json must expose the access-rule bound directory list static script`；该失败在本次修复前已存在，属于当前工作区未收口的既有状态，不作为本缺陷回归结论。
