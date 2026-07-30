# Execution Log

## 2026-07-30

- User intent: 在 eDHR 批记录页面级页签中新增 `生产填写` 与 `PQC填写`，接入真实 Vue 前端，不嵌入静态 HTML/PNG，不改后端契约。
- Baseline: `4158334f chore: baseline dirty workspace before edhr frontline tabs`，用于隔离本任务前已有脏工作区。
- BDD: eDHR 页签入口 -> Given 用户进入 eDHR 批记录页签区域 When 查看页签栏 Then 能看到 `批次执行`、`历史批记录`、`生产填写`、`PQC填写`，且四个页签跳转稳定。
- BDD: 生产一线填写 -> Given 用户打开 `生产填写` When 页面渲染 Then 页面只显示工序、员工、主页、数量、最多三个设备参数和提交，不显示工单或生产订单。
- BDD: PQC 一线填写 -> Given 用户打开 `PQC填写` When 页面渲染 Then 页面显示生产订单、工序、员工、主页、可输入检验内容、首检/巡检/末检、检验数量和损耗数量，不显示检验方法、成功/失败或巡检摘要。
- BDD: 固定模板模式 -> Given 员工切换后后端返回模板类型 When 模板类型与当前页签模式不一致 Then 页面显式阻塞提交，不自动切换到另一套 UI。
- RED: `node tests/e2e/edhr-frontline-fill-tabs-static.spec.cjs` -> FAIL, expected reason: `BatchProductionFillPage.vue must exist.`
