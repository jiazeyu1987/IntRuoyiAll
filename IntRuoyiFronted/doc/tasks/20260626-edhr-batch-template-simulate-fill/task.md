# 任务：eDHR 模板模拟填写页

## 任务目标

- 在 eDHR 批次模板说明页左侧每张模板卡片右上角新增 `模拟填写` 入口。
- 新增单模板模拟填写页，左侧直接在原始模板内模拟填写，右侧展示填写后的表单显示结果。
- 全流程仅做前端内存态模拟，不触发真实保存、真实上传、真实签名或审计链写入。

## 当前状态

已完成。

## 上一任务检查

- 上一个 frontend 任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260626-edhr-batch-template-preview\task.md`
- 状态：`已完成`
- 处理：上一任务文档已完成，不阻塞本次需求。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- 适用强制门禁：
  - 本次仅修改前端源码、静态契约测试与任务文档，不做真实登录写入、服务器操作或租户数据修改。
  - 模拟页与模板页继续遵循 IntPP 紧凑运维台样式，保持蓝白中性色、致密模板工作台、静音行级操作和轻量信息摘要。
  - 不通过 fallback、mock 成功、静默空态或吞异常掩盖 `id/taskId` 缺失、任务缺失、模板布局缺失、规则缺失或接口权限错误；缺前置时必须明确报错。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。查询参数、模板任务、布局或规则缺失时直接明确报错。
- `是否从根因和长期维护角度解决`：是。复用既有模板规则、只读表格和类型映射，新增纯前端模拟层而不复制真实执行页提交链路。
- `是否存在临时补丁或绕过`：否。附件明确不支持上传，不伪造上传结果或后端成功值。

## BDD 场景

- `BDD: 模拟填写入口可见 -> Given 用户位于模板说明页 When 查看左侧模板卡片 Then 每个卡片右上角显示 模拟填写 按钮。`
- `BDD: 模拟页按当前模板打开 -> Given 用户点击某张模板卡片的 模拟填写 When 进入新页面 Then 页面只加载该 taskId 对应模板，不再显示批次模板列表。`
- `BDD: 模拟页左侧按原模板填写 -> Given 用户打开某张模板的模拟页 When 查看左侧区域 Then 看到的是原始模板表格本身，而不是字段列表。`
- `BDD: 左侧模板内单元格可编辑 -> Given 某个模板单元格配置为文字/数字/日期/勾选/签名 When 用户在左侧对应格内输入 Then 该单元格直接显示可编辑控件并接收输入。`
- `BDD: 右侧显示填写结果 -> Given 用户在左侧模板格内输入内容 When 右侧渲染 Then 右侧按只读模板显示填写后的结果。`
- `BDD: 模拟签名实时驱动预览 -> Given 用户填写模拟签名人和签名时间 When 右侧渲染签名单元格 Then 显示签名人和时间；未填写时显示 未签名。`
- `BDD: 附件字段明确不支持上传 -> Given 当前模板字段带附件规则 When 用户查看左侧该格 Then 页面明确说明仅展示规则与占位，不提供真实上传。`
- `BDD: 缺少模板前置时明确报错 -> Given 缺少 id/taskId、找不到任务、缺少 reportId、缺少布局或缺少规则 When 打开模拟页 Then 页面明确报错，不渲染伪造内容。`
- `BDD: 左右工作台等宽 -> Given 用户位于模板模拟填写页 When 同时查看左右两侧 Then 左右工作台按等宽列布局展示。`
- `BDD: 模板宽度适配容器且高度不受限 -> Given 用户位于模板模拟填写页 When 查看左右模板 Then 模板按容器宽度缩放，高度随内容自然展开，不再固定限制高度。`

## 里程碑

1. M1：创建任务文档、更新请求命令记录并补模拟填写静态 RED 用例。
2. M2：实现模板卡片入口、隐藏路由、模拟页与模板内可编辑组件。
3. M3：运行 GREEN 静态验证、类型检查与 frontend evidence 校验。

## 预期验证

- `node tests/e2e/edhr-batch-template-simulate-static.spec.js`
- `node tests/e2e/edhr-batch-template-preview-static.spec.js`
- `node tests/e2e/edhr-batch-history-static.spec.js`
- `node tests/e2e/edhr-inline-signature-cells-static.spec.js`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260626-edhr-batch-template-simulate-fill\frontend-feature-evidence.md`

## 最终验证结果

- `node tests/e2e/edhr-batch-template-simulate-static.spec.js` -> PASS
- `node tests/e2e/edhr-batch-template-preview-static.spec.js` -> PASS
- `node tests/e2e/edhr-batch-history-static.spec.js` -> PASS
- `node tests/e2e/edhr-inline-signature-cells-static.spec.js` -> PASS
- `node tests/e2e/edhr-batch-review-summary-labels-static.spec.js` -> PASS
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260626-edhr-batch-template-simulate-fill\frontend-feature-evidence.md` -> PASS
