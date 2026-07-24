# 任务：批记录签名格改为电子签名

## 任务目标

- 将 eDHR 批记录模板内签名格从手动输入签名人、签名时间改为电子签名入口。
- 签名格不得作为普通字段值保存，必须复用现有密码电子签名链路生成签名记录。
- 模拟填写页只展示签名状态与入口，不伪造本地签名记录。

## 当前状态

COMPLETED

## 上一任务检查

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260626-electronic-batch-record-master-detail-layout\task.md`
- 状态：`COMPLETED`
- 处理说明：上一任务已完成；本次只修改 eDHR 签名格相关组件、静态测试和任务文档，不回退工作区其他无关改动。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- 适用强制门禁：
  - 前端页面必须沿用 IntPP 运维台样式：白底、轻边框、紧凑控制、稳定尺寸，不做营销式视觉重构。
  - 本轮默认仅做本机源码、静态测试和单元测试；如进入真实 Playwright E2E 或登录后写入验证，必须先运行官方登录预检并记录 `GREEN: experience-preflight -> PASS`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。签名格不能手填，也不生成模拟签名成功。
- `是否从根因和长期维护角度解决`：是。前端移除手填入口，后端另在字段审计写入通道拒绝 `SIGNATURE` 普通字段值。
- `是否存在临时补丁或绕过`：否。不新增 mock 签名、不增加默认成功、不绕过现有电子签名链路。

## BDD 场景

- `BDD: 签名格只能电子签名 -> Given 批记录模板含复核人/日期签名格 / When 用户进入模板内填写 / Then 签名格显示电子签名入口，不出现签名人姓名或签名时间手填输入框。`
- `BDD: 模拟页不伪造签名记录 -> Given 用户在模拟填写页查看签名格 / When 未完成真实电子签名 / Then 右侧表单显示未签名，不从本地输入生成签名记录。`
- `BDD: 已签名记录回填模板格 -> Given 执行记录已有真实签名记录 / When 表单预览或历史页展示模板 / Then 签名格显示真实签名人和签名时间。`

## 里程碑

1. M1：创建任务文档、执行日志和 RED 静态测试。`COMPLETED`
2. M2：实现签名格电子签名入口与模拟页签名值剔除。`COMPLETED`
3. M3：运行前端目标验证。`COMPLETED`
4. M4：回写证据、收尾预览并按验证结果提交。`COMPLETED`

## 预期验证

- `node tests/e2e/edhr-batch-template-simulate-static.spec.js`
- `node tests/e2e/edhr-inline-signature-cells-static.spec.js`

## 最终验证结果

- `node tests/e2e/edhr-batch-template-simulate-static.spec.js`：PASS。
- `node tests/e2e/edhr-inline-signature-cells-static.spec.js`：PASS。
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check`：PASS。
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc\tasks\20260626-edhr-signature-cell-electronic-signature\frontend-feature-evidence.md`：PASS。
- 真实登录写入 E2E：本轮未执行；未触发登录后写入、服务器、数据库或发布类高风险动作。

## Cleanup Keep

- `doc/tasks/20260626-edhr-signature-cell-electronic-signature/frontend-feature-evidence.md`

## 阻塞与影响

- 已解除：用户已通过 `继续` 恢复本任务，本轮完成前端实现、验证和证据回写。
