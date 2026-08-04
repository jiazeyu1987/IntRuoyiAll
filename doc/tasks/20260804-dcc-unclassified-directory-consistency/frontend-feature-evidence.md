# Frontend Feature Evidence

## Feature

让用户侧 DCC 提交/转移/维护入口不再要求手工选择或绑定文件类别目录；类别未绑定时只显示系统自动落位到未分类目录。

## Scope

- `IntRuoyiFronted/src/views/system/nas/index.vue`
- `IntRuoyiFronted/src/views/dcc/controlled-file/shared/ControlledFileMetadataDialog.vue`
- `IntRuoyiFronted/src/api/dcc/controlledFile/workflow.ts`

## Acceptance

- NAS 转移/本地导入选择未绑定模板类别时，不再前端阻塞。
- 元数据编辑选择未绑定类别时，不再提示“当前类别未绑定受控目录”，目录显示为系统自动落位未分类。
- 元数据更新 API 请求允许 `directoryId?: number | null`，由后端解析最终目录。
- 上传页相邻契约继续保证“文件类别自动取文件分类叶子节点、未绑定提交目录自动未分类”。

## BDD

- BDD: NAS 转移未绑定模板类别 -> Given 模板类别启用但无目录绑定, When 用户在 NAS 管理页选择该类别, Then 页面显示自动落位未分类并允许继续填写其它业务字段。
- BDD: 元数据编辑未绑定类别 -> Given 用户打开可编辑的受控文件元数据弹窗, When 选择未绑定文件类别, Then 受控目录字段显示自动落位未分类提示且不要求目录选择。
- BDD: 上传页相邻链路保持 -> Given 用户选择文件分类叶子节点, When 叶子节点绑定的类别没有提交目录, Then 页面自动显示未分类提交目录，不显示旧阻塞文案。

## RED

- RED: 前端静态合同在旧源码下会命中旧阻塞常量/函数或要求 `directoryId` 必填，阻止未绑定类别提交。

## GREEN

- GREEN: `node scripts\system-nas-management.test.mjs` -> PASS，2 tests。
- GREEN: `node scripts\dcc-controlled-file-metadata-edit.test.mjs` -> PASS，5 tests。
- GREEN: `node tests\e2e\dcc-upload-category-permission-static.spec.js` -> PASS。
- GREEN: `pnpm e2e:dcc:upload-category-taxonomy-binding:static` -> PASS。
- GREEN: `pnpm e2e:dcc:upload-project-taxonomy-revision:static` -> PASS。
- GREEN: `pnpm e2e:dcc:upload-onlyoffice-document-url:static` -> PASS。
- GREEN: `node tests\e2e\dcc-upload-category-leaf-real.e2e.js` -> PASS，真实页面只读 E2E。
- GREEN: `node doc\tasks\20260804-dcc-unclassified-directory-consistency\nas-unclassified-dialog-readonly.e2e.cjs` -> PASS，真实 NAS 管理页只读 E2E。

## Verification

- 旧阻塞文案和 `validateTransferCategoryDirectoryBinding` 在运行时源码中无残留。
- NAS 弹窗真实页面能显示未绑定模板类别“自动落位未分类”提示，且只读验证没有触发 DCC 写请求。
- 上传页真实页面证据显示 `bindingDirectoryPath=未分类`、`defaultUnclassified=true`，无 console/page error。

## Blockers

- `node doc\tasks\20260804-dcc-unclassified-directory-consistency\metadata-unclassified-dialog-readonly.e2e.cjs` 当前被本机账号缺 `doc_control` 编辑入口阻塞；页面按权限隔离不渲染“修改基础信息”，未用 API-only 替代。
- `pnpm ts:check` 被既有 LocalDateTime 类型问题阻塞，未指向本任务修改文件。
