# 执行日志：DCC 审阅矩阵部门选择器对齐查看矩阵

BDD: 审阅矩阵选择部门首层直接显示部门 -> Given 管理员编辑审阅矩阵中的 DEPT 规则 When 打开对应部门选择器 Then 首层节点直接是部门而不是公司根节点。
BDD: 公司上下文无法唯一推断时不混合多公司 -> Given 当前规则无法唯一定位公司上下文 When 打开对应部门选择器 Then 不显示公司根节点，也不混合多家公司部门。
BDD: 查看矩阵复用共享 helper 后行为不回归 -> Given 管理员打开查看矩阵维护弹窗 When 查看对应部门选择器 Then 仍只显示当前公司下一层部门，且不显示公司根节点。

INFO: task-created -> 前端任务文档已创建，开始补审阅矩阵部门树 RED 断言。
RED: `node tests/e2e/dcc-review-matrix-tab-static.spec.js` -> FAIL, 审阅矩阵编辑器仍直接绑定整棵 `departmentTree`，未按规则裁剪掉公司根节点。
GREEN: experience-preflight -> PASS，已按 `docs/login-access.md` 使用本机 `http://localhost:8081`、`测试租户/aoteman` 跑通官方登录前置并进入 `审阅矩阵` 页面。
GREEN: `node tests/e2e/dcc-review-matrix-tab-static.spec.js` -> PASS
GREEN: `node tests/e2e/dcc-view-matrix-independent-source-static.spec.js` -> PASS
GREEN: `pnpm ts:check` -> PASS
GREEN: `node tests/e2e/dcc-review-matrix-tab-real.e2e.js` -> PASS，真实样本部门首层不再显示公司根节点，目标部门可正常选择、预览、保存与回读。
