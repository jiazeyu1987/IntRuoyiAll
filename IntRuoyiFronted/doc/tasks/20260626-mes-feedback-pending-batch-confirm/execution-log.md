# 执行日志：MES 待归属页单页归属与整批确认前端改造

BDD: 当前批次待归属工作台 -> Given 用户刚完成一次 Excel 导入或模拟报工 / When 页面切到待归属页 / Then 列表必须按本次 importRecordIds 锁定，并在顶部展示来源文件、条数、已归属数、未归属数和可确认草稿数。
BDD: 已归属行在待归属页内补齐草稿字段 -> Given 某条导入记录已归属真实工序并生成 PREPARE 草稿 / When 用户查看待归属列表 / Then 行内直接显示报工人、报工时间、当前审批人和备注编辑控件，不再跳到正式报工页。
BDD: 批量确认阻断未归属或漏填 -> Given 当前批次仍有 PENDING 行或已归属真实工序行缺少报工人/报工时间/当前审批人 / When 用户点击确认报工 / Then 页面必须给出整批阻断清单，且不允许部分提交。
BDD: 其他订单行跳过确认 -> Given 当前批次中存在归属到其他订单的记录 / When 用户查看待归属列表并执行确认报工 / Then 该行显示为“其他订单/本批跳过”，不展示草稿编辑字段，也不阻断真实工序草稿整批提交。
BDD: 待归属页不再引导单条正式报工 -> Given 用户查看已归属记录 / When 页面渲染操作列和正式报工列表入口 / Then 待归属行仅显示选择归属或修改归属，来源于导入记录的 PREPARE 草稿不再暴露单条提交入口。

READONLY: 已读取 `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`，命中 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`；本轮尚未进入真实登录或写入型 E2E。

RED: `node tests/e2e/mes-feedback-pending-batch-confirm-static.spec.js` -> FAIL，预期失败；当前前后端仍缺 `confirm-batch` / `batch-summary` 合同文件与待归属页批量确认结构。
RED: `node tests/e2e/mes-feedback-import-diagnostics-hidden-static.spec.js` -> FAIL，预期失败；当前待归属页尚未出现行内 `备注` 编辑与顶部 `确认报工`。
RED: `node tests/e2e/mes-feedback-tracking-static.spec.js` -> FAIL，预期失败；当前追踪链路尚未返回 `generatedFeedbackDraft` / `linkedFeedbackStatus`，正式报工详情也未阻止导入草稿单条提交。
GREEN: `node tests/e2e/mes-feedback-pending-batch-confirm-static.spec.js` -> PASS，待归属页已接入批次摘要、整批确认与其他订单跳过合同。
GREEN: `node tests/e2e/mes-feedback-import-diagnostics-hidden-static.spec.js` -> PASS，待归属页已收敛为业务工作台，不再渲染导入诊断列。
GREEN: `node tests/e2e/mes-feedback-tracking-static.spec.js` -> PASS，追踪字段、修改归属入口与导入草稿单条提交封口均已落地。
RED: `node tests/e2e/mes-feedback-pending-batch-confirm-static.spec.js` -> FAIL，补充需求要求确认报工成功后弹框“报工成功”并切回正式报工 tab，旧实现仍停留在待归属页且成功提示文案不符。
