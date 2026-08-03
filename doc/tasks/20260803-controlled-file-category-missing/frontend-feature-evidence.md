# Frontend Feature Evidence

## Feature Goal

受控文件提交页中，“文件类别”自动显示所选“文件分类”路径的叶子节点，不再允许用户手工选择。正式提交、预览、目录加载仍使用后端正式 DCC 类别 `categoryId`，该 ID 只能由当前叶子节点唯一绑定的可上传类别自动解析。

## Non-Goals

- 不修改后端 API 合同。
- 不把 `fileTypeTaxonomyId` 当作 `categoryId` 提交。
- 不新增 fallback、默认类别、空值通过或吞异常。
- 不改变外来文件评审页仍需手工选择正式类别的现有流程。

## Requirements

- REQ-1: 受控文件提交页“文件类别”只读显示文件分类叶子节点。
- REQ-2: 受控文件提交页不得展示可手选的文件类别下拉。
- REQ-3: 当前叶子节点唯一绑定一个启用、可上传、已绑定提交目录的正式 DCC 类别时，自动写入 `formData.categoryId` 并加载提交目录。
- REQ-4: 当前叶子节点缺少可上传正式类别、缺提交目录、无上传权限或存在多个可上传类别时，页面明确阻塞并提示管理员修配置。
- REQ-5: 外来文件评审页保留原手选类别流程。

## UI Entry Points And Owned Files

- Route/page: DCC 受控文件提交页。
- Component: `IntRuoyiFronted/src/views/dcc/controlled-file/upload/index.vue`。
- Static contracts: `IntRuoyiFronted/tests/e2e/dcc-upload-category-taxonomy-binding-static.spec.js` and adjacent upload static contracts.

## API Contracts And Data States

- `getFileTypeTaxonomyList()` 提供文件分类树，页面叶子节点显示来自该树路径最后一级。
- `getFileCategoryList()` 提供正式 DCC 类别，页面只用当前叶子节点 `fileTypeTaxonomyId` 精确匹配类别。
- `getControlledFileUploadDirectoryTree(categoryId)` 仍只接收正式 DCC `categoryId`。
- `uploadControlledFilePreview` / `submitControlledFile` 继续使用正式 DCC `categoryId`，不得使用 taxonomy id 代替。

## BDD Scenarios

- BDD: 文件类别只读显示叶子节点 -> Given 用户选择“技术文档 / 设计和开发输入阶段 / 专利检索与分析报告” When 页面展示“文件类别” Then 文件类别显示“专利检索与分析报告”，不可下拉、不可输入，正式 DCC `categoryId` 由该叶子节点唯一绑定的可上传类别自动解析。
- BDD: 文件分类缺正式类别绑定 -> Given 用户选择的文件分类叶子节点没有唯一可上传正式 DCC 类别 When 用户准备上传或提交 Then 页面明确提示该文件分类尚未配置唯一可上传文件类别和提交目录，不用其它分类、空值或 taxonomy id 替代。

## RED

- RED: 待运行 `node tests/e2e/dcc-upload-category-taxonomy-binding-static.spec.js` -> FAIL, expected reason: 当前旧页面仍展示受控上传页文件类别下拉，尚未只读显示文件分类叶子节点。

## GREEN

- 待实现后记录。

## Checks

- Responsive/layout: 保持现有表单布局宽度，不扩大整页设计。
- Accessibility: 只读显示使用明确文本区域和提示，不伪装成可编辑下拉。
- Loading/empty/error: 分类缺绑定、缺目录、无上传权限、多绑定均显示明确阻塞文案。
- Permission: 仍以正式类别 `canUpload` 作为上传权限来源。

## Blockers

- 当前工作区存在非本任务脏改动；本轮仅处理目标文件和任务证据，不宽泛暂存或提交。
