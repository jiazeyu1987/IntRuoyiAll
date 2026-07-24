# 任务：eDHR 模板说明缺少布局回归修复

## 任务目标

- 修复批次模板说明页提示“缺少电子批记录模板布局，无法显示模板说明”的回归。
- 确保前端继续 fail-fast 暴露真实缺失原因，不生成默认模板、不静默降级。
- 若后端/Jimu 报表已有有效模板结构，接口必须返回 `sheetLayoutJson` 供模板说明页渲染。

## 当前状态

COMPLETED

## 上一任务检查

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260626-edhr-signature-cell-electronic-signature\task.md`
- 状态：`COMPLETED`
- 处理说明：上一任务已完成并提交；本次只处理 eDHR 模板布局缺失回归，不回退当前工作区其他无关脏改。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- 适用强制门禁：
  - 前端页面保持 IntPP 运维台风格：白底、轻边框、紧凑控制，不做营销式重构。
  - 本轮默认做本机源码、静态测试和后端单元测试；如进入真实 Playwright 登录写入验证，必须先运行登录预检并记录 `GREEN: experience-preflight -> PASS`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。缺少真实模板布局仍明确报错；不生成默认表格。
- `是否从根因和长期维护角度解决`：是。优先修正后端/Jimu JSON 布局解析与接口返回合同。
- `是否存在临时补丁或绕过`：否。不通过前端硬编码、mock 布局或接口绕过掩盖问题。

## BDD 场景

- `BDD: 模板说明页显示已有布局 -> Given 批记录报表的 Jimu JSON 中存在有效模板 rows / When 用户打开 eDHR 批次模板说明页 / Then 页面能收到 sheetLayoutJson 并渲染模板说明，不显示缺少布局错误。`
- `BDD: 真正缺少布局继续 fail-fast -> Given 报表 JSON 确实没有可识别模板 rows / When 用户打开模板说明页 / Then 页面显示明确布局缺失错误，不展示默认模板。`

## 里程碑

1. M1：创建任务文档和 RED 复现。`COMPLETED`
2. M2：修复模板布局解析或接口返回合同。`COMPLETED`
3. M3：运行前后端目标验证。`COMPLETED`
4. M4：收尾预览并按验证结果提交。`COMPLETED`

## 预期验证

- `node tests/e2e/edhr-batch-template-preview-static.spec.js`
- 视根因补充后端 MES 目标单测。

## 最终验证结果

- `node tests/e2e/edhr-batch-template-preview-static.spec.js` -> PASS。
- `node tests/e2e/edhr-batch-template-simulate-static.spec.js` -> PASS。
- 后端 `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordReportServiceImplDbTest,MesProBatchRecordExecutionServiceImplTest" test` -> PASS。
- 结论：前端继续 fail-fast，不新增默认/兜底布局；本次根因修复在后端执行详情和报表布局接口返回合同。
