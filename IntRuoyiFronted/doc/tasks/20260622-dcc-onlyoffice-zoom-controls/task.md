# 任务：DCC OnlyOffice 预览增加缩放按钮与滚轮缩放

## 任务目标

- 在 DCC 受控预览的 OnlyOffice 只读查看器中增加明确可见的“放大 / 缩小 / 重置”按钮。
- 支持用户在 OnlyOffice 预览区域使用滚轮执行放大缩小。
- 保持现有受控阅读边界，不放开编辑、下载、打印、复制等受控权限。

## 上一任务检查

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260622-dcc-preview-file-name-recognition\task.md`
- 状态：`COMPLETED`
- 处理：上一任务已完整收尾，不阻塞本次 OnlyOffice 缩放交互增强。

## 用户要求与执行边界

- 用户要求：
  - `增加滚轮可以放大缩小，有放大缩小按钮可以放大缩小`
- 本任务边界：
  - 只修改 DCC Office 受控预览组件及对应静态合同测试。
  - 不修改后端接口、预览地址签名、权限模型和下载/打印/copy 受控边界。
  - 不做无关 UI 重构，保持现有 Int 运营台风格。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 命中文档：
    - `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- 本任务强制门禁摘录：
  - 交互增强必须保持蓝/中性运营台风格，不做营销化改版。
  - 不得以移除受控限制为代价换取缩放能力；编辑、下载、打印、复制等只读限制必须保留。
  - 先补 RED 测试，再做最小实现，最后补回归验证。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。直接在共享 OnlyOffice 只读预览组件中增加统一缩放控制，不在页面外层加临时补丁。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 用户可通过按钮缩放 OnlyOffice 预览 -> Given 用户进入 DCC Office 受控预览 When 点击放大、缩小或重置按钮 Then 预览缩放比例应按预设步进变化并保留在受控只读模式。`
- `BDD: 用户可通过滚轮缩放 OnlyOffice 预览 -> Given 用户正在查看 DCC Office 受控预览 When 在预览区域使用滚轮进行缩放手势 Then 预览缩放比例应随滚轮方向变化且不放开编辑权限。`
- `BDD: 缩放增强不破坏受控边界 -> Given OnlyOffice 预览仍属于受控浏览 When 用户使用缩放交互 Then 编辑、下载、打印、复制禁用状态必须保持不变。`

## 里程碑

1. 建立任务文档并记录门禁。`DONE`
2. 补 RED 静态合同测试，要求 OnlyOffice 预览暴露缩放按钮、缩放状态和滚轮绑定。`DONE`
3. 最小修改 `OnlyOfficeReadOnlyViewer.vue` 实现缩放交互。`DONE`
4. 运行目标静态验证并补证据。`DONE`

## 预期验证

- `node scripts/dcc-onlyoffice-zoom-controls.test.mjs`
- `node scripts/dcc-onlyoffice-readonly-config.test.mjs`
- `pnpm ts:check`

## 当前状态

COMPLETED

## 当前结论

- 已在共享 `OnlyOfficeReadOnlyViewer.vue` 中增加缩小、重置、放大按钮与缩放百分比显示。
- 已支持 `Ctrl + 滚轮` 缩放；为兼容 OnlyOffice iframe 场景，缩放手势激活时会临时让外层容器接收滚轮事件。
- 受控只读权限未变，原有 `copy/download/print/edit/comment/review=false` 仍保持。
