# DCC 截图需求前端开发设计

## Purpose and Scope

本文为 DCC 截图需求的前端实现设计文档，范围限定在 IntRuoyi 前端现有 DCC、BPM、系统用户、站内信、下载/导出/打印能力基础上的增强设计。后续实现前必须先读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`，并沿用 IntPP 生产订单列表风格：密集、克制、表格优先、蓝/中性色操作台视觉，不另起独立页面体系。

本次仅写设计文档，不修改生产代码、不增加测试专用控件、不提交 Git。

## Evidence Reviewed

- 后端产品草稿：`D:\ProjectPackage\Int\IntRuoyi\worktrees\20260525-dcc-requirements-analysis\ruoyi-vue-pro\docs\product\dcc-screenshot-requirements-prd.md`
- 后端用户流程：`D:\ProjectPackage\Int\IntRuoyi\worktrees\20260525-dcc-requirements-analysis\ruoyi-vue-pro\docs\product\dcc-screenshot-requirements-user-flows.md`
- 后端验收草稿：`D:\ProjectPackage\Int\IntRuoyi\worktrees\20260525-dcc-requirements-analysis\ruoyi-vue-pro\docs\product\dcc-screenshot-requirements-acceptance-criteria.md`
- 当前前端 DCC API：`src/api/dcc/controlledFile/workflow.ts`、`fileCategories.ts`、`approvalRoutes.ts`、`approvalPositions.ts`、`training.ts`、`signatures.ts`、`directories.ts`
- 当前前端页面：`src/views/dcc/controlled-file/upload/index.vue`、`mine/index.vue`、`detail/index.vue`、`approval-tasks/index.vue`、`browser/index.vue`、`distribution/index.vue`、`training/index.vue`、`categories/index.vue`、`routes/index.vue`、`positions/index.vue`、`signatures/index.vue`
- 系统能力：`src/api/system/user/index.ts`、`src/api/system/user/profile.ts`、`src/api/system/notify/message/index.ts`、`src/views/system/user/components/UserSelectV2.vue`、`src/views/system/notify/my/index.vue`、`src/utils/download.ts`、`src/utils/filt.ts`

## Existing Capability Summary

- DCC 受控文件上传已存在：类别、目录、文件名称、文件编号、版本号、生效日期、备注、单文件上传预览、路线预览、提交审批。
- DCC 固定四阶段已存在：文控审核、审核会签、批准、文控批准；详情页已有审批阶段进度、电子签名通过/驳回、签名留痕。
- DCC 下载已存在：`downloadControlledFileWithName`、`triggerControlledFileDownload` 调用 `/dcc/controlled-files/{id}/download` 并触发浏览器下载。
- DCC 培训已存在：类别培训规则、培训执行、我的培训、培训任务预览和确认。
- DCC 发放已存在：类别分发规则、公盘目录/纸质发放配置、详情页分发状态和纸质发放确认。
- DCC 签名授权与签名记录已存在。
- 系统用户能力已存在：用户分页、简单用户列表、创建用户、重置密码、个人修改密码、`UserSelectV2` 弹窗选人。
- 站内信能力已存在：未读消息、小铃铛、我的站内信分页、标记已读。
- BPM 打印模板能力已存在：流程详情打印、模型额外设置中的自定义打印模板，但 DCC 详情页目前只跳 BPM 详情，未在 DCC 页面直接承载导出/打印。

## 页面和组件复用矩阵

| 需求主题 | 复用页面/组件/API | 前端改造点 | 不新增独立体系原则 |
| --- | --- | --- | --- |
| 文件受控审批上传 | `DccControlledFileUpload`、`workflow.ts`、`ProtectedPdfViewer` | 扩展源文件、图纸 PDF 伴随文件、文件类别枚举提示、现行有效版本、14 位产品编号、是否需要培训、会签人选择 | 继续使用 `/dcc/controlled-file/upload`，不新建单独“截图需求上传页” |
| 会签人自选 | `UserSelectV2`、`previewControlledFileRoute`、`submitControlledFile` | 在上传表单按后端契约选择会签人员，路线预览展示用户解析结果 | 复用系统用户选择器，不写新的用户弹窗 |
| 审批动作：回退/转交/加签 | `DccControlledFileApprovalTasks`、`DccControlledFileDetail`、BPM task API、DCC task action API | 在详情页当前任务区新增动作入口和弹窗，按后端权限返回显示 | 留在现有审批待办和详情页，不另起任务中心 |
| 退回申请人重提 | `DccControlledFileMine`、`approval-tasks`、站内信 `notify-message` | 待办/我的文件显示 `有流程回退，需处理`，上传/详情页进入原流程重提模式 | 不重新走发起新流程页，必须复用原流程实例 |
| 主动撤回后删除/重提 | `DccControlledFileDetail`、`DccControlledFileMine` | 撤回后展示删除流程/重新提交动作，依赖后端状态和操作权限 | 继续在我的文件与详情页操作 |
| 培训记录上传 | `DccControlledFileUpload`、`training.ts`、详情页培训状态 | 表单选择是否需要培训；按状态要求申请人上传培训记录 | 复用现有培训模块和详情页，不另造培训系统 |
| 第四节点受控章 PDF 上传 | `DccControlledFileDetail`、`ProtectedPdfViewer`、`workflow.ts` | 文控批准节点新增受控章 PDF 上传字段和提交校验 | 仍在第四节点审批动作区完成 |
| 下载提醒与留痕 | `triggerControlledFileDownload`、`downloadByData`、`message.confirm` | 下载前统一弹窗提示“下载后文件为非受控文件”；成功依赖后端留痕 | 所有 DCC 下载入口调用统一封装 |
| 体系记录所有人下载 | `DccControlledFileBrowser`、`DccControlledFileDetail`、目录权限 | 依据后端 `canDownload` 和记录类型显示下载；不在前端自行放权 | 不通过前端编码猜测绕过权限 |
| 电子发放接收人加签 | `distribution/index.vue`、详情页分发状态、`UserSelectV2` | 接收人处理发放任务时提供加签入口 | 复用 DCC 详情/发放状态，不建新加签页 |
| 纸质发放回收记录 | `distribution/index.vue`、详情页分发状态、`request.download`、`vue3-print-nb` | 增加纸质发放回收记录列表、录入/导出/打印 | 放在 DCC 发放管理内，表格风格对齐 IntPP |
| 流程导出/打印 | DCC 详情页、BPM `PrintDialog`、后端 DCC 导出接口 | DCC 详情页新增导出/打印入口，优先复用 BPM 打印数据/模板 | 不独立开发新的打印模板编辑器，除非后端确认 Word 模板契约 |
| 密码强度/周期 | `UserForm`、用户列表重置密码、`Profile/ResetPwd`、`InputPassword` | 统一 8 位且英文+数字校验；周期强制更新依赖后端策略返回 | 不在前端用默认周期或本地存储伪造策略 |
| 文件“修改中”标识 | `mine`、`browser`、`detail` 状态标签 | 扩展状态/标识字段显示 `修改中` tag | 复用现有状态 tag 体系 |
| 外来文件评审 | 暂无完整现有入口 | 仅保留设计阻塞，待需求补充后决定复用 BPM/DCC 上传链路 | 不先建空页面或占位流程 |

## 截图需求 UI 设计明细

| 序号 | 需求 | UI 入口 | 表单字段/组件 | 交互 | 权限 | 错误提示 |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | 上传可编辑源文件，图纸类同时上传 PDF | `/dcc/controlled-file/upload` | 源文件上传、图纸 PDF 伴随上传；允许类型由后端契约返回或前端常量与后端同步 | 选择源文件后识别扩展名；若为 `dwg/sldprt/sldasm/slddrw`，显示 PDF 必填上传位；提交前前端校验，提交后以后端校验为准 | `dcc:controlled-file:submit` 或现有上传权限 | `图纸类文件必须同时上传 PDF 格式文件`、`源文件上传失败，请查看错误提示后重试` |
| 2 | 密码强度与定期更新 | `/system/user` 新增/重置密码、`/profile` 修改密码、登录后强制修改页或弹窗 | `InputPassword`、密码规则提示 | 输入时校验至少 8 位且含英文和数字；强制更新只根据后端返回状态跳转 | `system:user:create`、`system:user:update-password`、本人修改密码 | `密码至少 8 位，且必须同时包含英文和数字`；周期策略缺失时阻断：`密码更新策略未配置，无法继续` |
| 3 | 下载前非受控提醒、下载留痕 | 我的文件、文件库、详情页、版本历史下载入口 | 统一下载确认弹窗 | 点击下载先确认；确认后调用后端下载接口；后端成功返回 Blob 后再触发保存 | 后端 `canDownload` 控制；前端不自行放权 | `下载失败，请查看错误提示后重试`；留痕失败应由后端失败返回，前端不吞错 |
| 4 | 文件增加“修改中”标识 | 我的文件、文件库、详情页状态区域 | `el-tag` 状态标签 | 列表和详情在后端返回 `modifying` 或新状态时显示 `修改中` | 查看权限 | 状态字段缺失不伪造显示；保持原状态 |
| 5 | 回退、转交、加签、退回申请人提醒、原流程重提、撤回后删除/重提 | `/dcc/controlled-file/approval-tasks`、`/dcc/controlled-file/detail/:id`、我的文件、站内信 | 审批动作区按钮、目标人员选择、原因 textarea、站内信详情跳转 | 当前任务区按后端 action permissions 展示按钮；回退目标可选前一节点/申请人；申请人待办显示 `有流程回退，需处理`；重提必须携带原流程实例 id | 节点负责人权限、申请人权限、后端任务权限 | `当前节点不允许回退/转交/加签`、`请选择处理人`、`请输入处理原因`、`原流程实例缺失，无法重提` |
| 6 | 新增文件类别、现行有效版本、14 位产品编号 | `/dcc/controlled-file/upload`、详情页、我的文件/文件库列 | 文件类别 select、现行有效版本只读、产品编号 input | 类别仍用现有 DCC 类别；选历史文件名带出现行有效版本；产品编号输入 14 位校验 | 上传权限 | `请输入 14 位产品编号`、`现行有效版本读取失败，请确认文件名称和类别` |
| 7 | 外来文件评审流程 | 暂不实现入口 | 无 | 需求不足，不创建空入口 | 待确认 | `外来文件评审流程尚未完成业务定义`，实现前不应出现给用户的半成品入口 |
| 8 | 体系记录所有人下载，编码前六位 `INT/RE` | 文件库、详情页下载入口 | 复用下载按钮 | 前端只根据后端返回 `canDownload` 显示；可展示记录类型 tag | 后端权限和编码规则控制 | 若后端未放权：`当前文件暂无下载权限`；前端不得根据 `INT/RE` 自行绕过 |
| 9 | 需要培训时第四节点前上传培训记录 | 上传页、详情页、我的培训 | 是否需要培训 switch、培训记录上传、培训状态表格 | 申请人提交时选择是否需要培训；流程到第四节点前若需要培训则引导申请人上传记录；详情页显示培训记录和状态 | 申请人、培训任务接收人 | `请选择是否需要培训`、`请先上传培训记录后再提交` |
| 10 | 电子发放回收中接收人可加签 | 详情页分发状态、发放管理 | 接收人列表、加签按钮、`UserSelectV2` | 文控选择接收人；接收人打开任务后可加签；加签人去重由后端校验 | 文控选择接收人、接收人加签权限 | `请选择加签人员`、`当前接收任务不允许加签` |
| 11 | 纸质发放回收记录导出/打印 | `/dcc/controlled-file/distribution` 或新增 tab 于现有发放管理 | 表格列：文件编号、版本、名称、发放人、接收人、发放日期、回收人、回收日期；导出、打印按钮 | 查询记录、录入发放/回收、导出 Excel/PDF、打印当前筛选结果 | `dcc:controlled-file:distribute` 或后端新增细粒度权限 | `请完善文件编号、版本、名称、发放人、接收人、发放日期`、`导出失败，请查看错误提示后重试` |
| 12 | 文件受控审批流程导出与打印，可能支持 Word 模板 | 详情页、BPM 详情页 | 导出按钮、打印按钮、模板选择/只读模板信息 | DCC 详情直接提供打印/导出；打印优先复用 BPM 打印数据；Word 模板上传需后端确认占位符后再做 | 查看/打印权限 | `打印数据加载失败`、`模板缺少必需占位符，无法打印` |
| 13 | 会签节点申请人自选 | `/dcc/controlled-file/upload` 路线预览区 | `UserSelectV2` 多选、路线预览表格 | 申请人在提交前选择会签人员；路线预览显示固定节点和自选人员 | 申请人上传权限；可选人员范围由后端返回 | `请选择会签人员`、`所选会签人员不在允许范围内` |
| 14 | 第四节点文控上传加盖受控章 PDF | 详情页第四节点审批动作区 | PDF 上传、预览、签名密码、意见 | 第四节点处理时先上传盖章 PDF，再电子签名提交；后端确认文件为 PDF | 文控/第四节点任务权限 | `请上传加盖受控章后的 PDF 文件`、`仅支持 PDF 格式` |

## API 调用契约设计

### 扩展现有 `workflow.ts`

1. `ControlledFileSubmitReqVO` 建议扩展：

```ts
interface ControlledFileSubmitReqVO {
  categoryId: number
  directoryId: number
  originalFileId: number
  sourceFileId?: number
  drawingPdfFileId?: number
  trainingRequired?: boolean
  signoffUserIds?: number[]
  fileName: string
  fileNumber: string
  versionNo: string
  currentEffectiveVersionNo?: string
  productCode14?: string
  effectiveDate: string
  remark?: string
}
```

2. 新增或扩展上传接口：

```ts
uploadControlledFileSource(file: File): Promise<ControlledFileUploadRespVO>
uploadControlledFileDrawingPdf(file: File): Promise<ControlledFileUploadRespVO>
uploadControlledFileTrainingRecord(id: number | string, file: File): Promise<boolean>
uploadControlledStampedPdf(id: number | string, file: File): Promise<boolean>
```

若后端继续复用 `/dcc/controlled-files/upload-preview`，前端通过 `purpose` 字段区分 `SOURCE`、`DRAWING_PDF`、`TRAINING_RECORD`、`STAMPED_PDF`；不得静默降级为普通附件。

3. 任务动作建议新增：

```ts
returnControlledFileTask(id, { taskId, targetType, reason, password })
transferControlledFileTask(id, { taskId, assigneeUserId, reason, password })
addSignControlledFileTask(id, { taskId, userIds, reason, password })
resubmitReturnedControlledFile(id, payload)
deleteWithdrawnControlledFile(id)
```

### 下载与留痕

现有 `downloadControlledFileWithName(id)` 保留，但所有调用必须改为统一封装：

```ts
confirmAndDownloadControlledFile(id, fallbackFileName)
```

封装职责：弹出非受控提醒 -> 用户确认 -> 调用后端下载 -> 后端成功返回 Blob -> `downloadByData`。若后端返回 JSON 错误或留痕失败，前端显示错误并不触发下载。

### 纸质发放回收

建议新增 `src/api/dcc/controlledFile/distributionRecords.ts`：

```ts
getPaperDistributionRecordPage(params): Promise<PageResult<PaperDistributionRecordVO[]>>
createPaperDistributionRecord(data): Promise<number>
updatePaperDistributionRecord(id, data): Promise<boolean>
exportPaperDistributionRecords(params): Promise<Blob>
printPaperDistributionRecords(params): Promise<Blob | PrintDataVO>
```

`PaperDistributionRecordVO` 必含：`fileNumber`、`versionNo`、`fileName`、`issuerUserId`、`recipientUserId`、`issuedDate`、`reclaimerUserId`、`reclaimedDate`。

### 密码策略

前端表单只做同步校验，强制更新以接口为准。建议后端提供：

```ts
getPasswordPolicy(): Promise<{ minLength: number; requireLetter: boolean; requireDigit: boolean; expireDays?: number }>
getMyPasswordStatus(): Promise<{ mustChangePassword: boolean; reason?: string }>
```

若策略接口缺失，不在前端默认设置周期；按失败处理并提示策略未配置。

### 站内信/待办

现有 `getMyNotifyMessagePage`、小铃铛未读能力可复用。回退提醒建议后端消息 `templateParams` 包含：

```ts
{
  notifyOpen: 'dccControlledFileReturn',
  controlledFileId: number,
  processInstanceId: string,
  taskId?: string
}
```

前端站内信详情可根据参数跳转 DCC 详情页，并显示 `有流程回退，需处理`。

## State and Data Flow

- 上传页选择类别后，加载目录树、历史文件名、现行有效版本、审批路线预览候选；若任一关键数据加载失败，阻断提交。
- 提交审批时，前端只提交已明确上传成功的文件 id 和表单字段；缺少源文件、图纸 PDF、产品编号、会签人时不提交。
- 审批任务页仍以 BPM 待办为入口，通过 `processInstance.businessKey` 定位 DCC 文件详情；若 businessKey 缺失，保持现有失败提示。
- 详情页按后端 `canPreview/canDownload/canObsolete/canManualRelease/actionPermissions` 控制操作显示；前端不自行推导越权动作。
- 下载成功必须由后端下载接口同时完成留痕；前端不能先触发浏览器下载再异步补记。
- 打印/导出必须从后端拿真实数据或 Blob；不得使用页面当前表格拼装成正式输出，除非后端确认该输出只作前端打印预览。

## Error States

- 必填文件缺失：明确提示缺少源文件、图纸 PDF、培训记录或盖章 PDF。
- 权限不足：显示后端返回消息，不隐藏成“操作成功”。
- 流程实例缺失：阻断重提、打印、审批动作。
- 下载留痕失败：不得触发本地保存。
- 密码策略缺失：不得默认通过或沿用旧 4-16 位规则。
- 外来文件评审需求不足：不得上线空入口、空表单或占位流程。

## 真实用户路径 E2E 入口

本项目统一前端入口：`http://localhost:8081`。E2E 必须使用真实租户、真实账号、真实前后端接口和 Playwright 操作前端；接口仅用于最终校验，不用于绕过页面路径。

建议路径：

- 上传审批：登录 -> `/dcc/controlled-file/upload` -> 选择类别/目录 -> 上传源文件和必要 PDF -> 选择会签人 -> 提交 -> `/dcc/controlled-file/mine` 或详情页确认。
- 审批动作：登录节点负责人 -> `/dcc/controlled-file/approval-tasks` -> 打开详情 -> 执行回退/转交/加签/通过/驳回。
- 退回重提：登录申请人 -> 我的待办或站内信 -> 看到 `有流程回退，需处理` -> 原流程重提 -> 后端校验同一 processInstanceId。
- 下载留痕：登录用户 -> `/dcc/controlled-file/browser` 或详情页 -> 点击下载 -> 确认非受控提醒 -> 下载成功 -> API 最终校验下载记录含用户 id 和时间。
- 纸质发放：登录文控 -> `/dcc/controlled-file/distribution` -> 纸质发放记录 tab -> 新增记录 -> 导出/打印。
- 密码策略：系统用户新增/重置密码、个人中心修改密码；强制更新路径以后端状态入口为准。

## 无副作用策略

- 不增加测试专用按钮、隐藏入口、URL 参数捷径或 mock 数据。
- 不绕过真实登录、真实租户、真实 DCC 页面路径。
- 不独立新建一套 DCC 页面体系；优先在现有上传、我的文件、审批待办、详情、发放、培训、文件库、用户、站内信页面上扩展。
- 不在前端用编码前缀自行放开下载权限，体系记录下载必须由后端返回 `canDownload`。
- 不吞异常；所有失败显示后端错误或明确的前端校验错误。
- 不引入 fallback 成功路径；缺少流程节点、模板、密码策略、下载留痕、文件 id 时直接阻断。
- 不改写 live 审核矩阵；涉及 DCC 审核矩阵/审批路线必须保持与确认版一致，未经批准不得在 E2E 中覆盖 live 版本。

## BDD 场景摘要

- BDD: 图纸源文件要求 PDF -> Given 申请人在文件受控审批上传图纸类源文件 / When 未上传 PDF 并提交 / Then 前端阻断并提示缺少 PDF。
- BDD: 上传扩展字段 -> Given 申请人填写文件受控审批 / When 文件类别、现行有效版本或 14 位产品编号缺失 / Then 不能提交并提示具体字段。
- BDD: 会签人自选 -> Given 申请人提交受控文件 / When 到达会签节点前选择会签人员 / Then 路线预览和提交载荷包含所选人员。
- BDD: 回退申请人待办 -> Given 节点负责人回退至申请人 / When 申请人打开待办或站内信 / Then 看到 `有流程回退，需处理` 并可在原流程重提。
- BDD: 第四节点盖章 PDF -> Given 文控处理第四节点 / When 未上传加盖受控章 PDF / Then 节点不可完成。
- BDD: 下载非受控提醒与留痕 -> Given 用户点击下载 / When 确认非受控提醒且后端返回成功 / Then 文件下载并产生下载人 id、下载时间记录。
- BDD: 纸质发放回收 -> Given 文控维护纸质发放回收记录 / When 导出或打印 / Then 输出包含截图要求字段。
- BDD: 密码强度 -> Given 用户创建、重置或修改密码 / When 密码少于 8 位或缺少英文/数字 / Then 表单拒绝保存。

## TDD/组件/E2E 命令草案

RED 命令草案：

```powershell
pnpm exec vitest run src/views/dcc/controlled-file/upload/__tests__/dcc-upload-requirements.spec.ts
pnpm exec vitest run src/views/dcc/controlled-file/detail/__tests__/dcc-task-actions.spec.ts
pnpm exec vitest run src/views/dcc/controlled-file/shared/__tests__/download-warning.spec.ts
pnpm exec vitest run src/views/system/user/__tests__/password-policy.spec.ts
pnpm exec eslint src/api/dcc/controlledFile src/views/dcc/controlled-file src/views/system/user src/views/Profile
npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-screenshot-requirements-e2e run-code --filename doc/tasks/20260525-dcc-requirements-analysis/scripts/verify-dcc-screenshot-requirements-e2e.mjs
```

RED 预期：

- 上传页未实现图纸 PDF、14 位产品编号、会签人选择时组件测试失败。
- 详情页未实现回退/转交/加签/盖章 PDF 时组件测试失败。
- 下载未弹非受控提醒或留痕失败仍下载时测试失败。
- 密码仍按 4-16 位规则时测试失败。
- E2E 找不到真实入口或后端前置数据不足时失败并记录前置条件。

GREEN 命令草案：

```powershell
pnpm exec vitest run src/views/dcc/controlled-file/upload/__tests__/dcc-upload-requirements.spec.ts
pnpm exec vitest run src/views/dcc/controlled-file/detail/__tests__/dcc-task-actions.spec.ts
pnpm exec vitest run src/views/dcc/controlled-file/shared/__tests__/download-warning.spec.ts
pnpm exec vitest run src/views/system/user/__tests__/password-policy.spec.ts
pnpm exec eslint src/api/dcc/controlledFile src/views/dcc/controlled-file src/views/system/user src/views/Profile
npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-screenshot-requirements-e2e run-code --filename doc/tasks/20260525-dcc-requirements-analysis/scripts/verify-dcc-screenshot-requirements-e2e.mjs
```

GREEN 预期：

- 组件测试通过，前端校验和 API payload 与契约一致。
- Playwright 真实路径通过，并用最终 API 校验流程实例、下载记录、纸质记录或密码策略结果。
- 未创建测试专用控件，未使用 mock 数据，未覆盖 live 审核矩阵。

## Subagent-driven 前端实现拆分建议

- Worker-FE-Upload：负责上传页表单字段、图纸 PDF、产品编号、会签人选择、路线预览 payload；输出组件测试和上传路径 E2E。
- Worker-FE-Workflow-Actions：负责详情页/审批待办的回退、转交、加签、退回重提、撤回后删除/重提、第四节点盖章 PDF；输出动作组件测试和审批路径 E2E。
- Worker-FE-Download-Distribution：负责下载提醒统一封装、纸质发放回收记录列表/导出/打印、电子发放接收人加签入口；输出下载/发放测试。
- Worker-FE-System-Password-Notify：负责密码规则表单统一、强制更新入口对接、站内信跳转和退回提醒展示；输出系统用户/个人中心/通知测试。
- Reviewer-FE：只审查设计一致性、无副作用、接口契约、真实路径 E2E 和是否复用现有 DCC/BPM/System 能力。

## Design Blockers

1. 必须确认当前文件受控审批第四节点的后端任务定义、角色和“文控”权限映射，否则盖章 PDF 上传入口无法准确绑定。
2. 必须确认回退、转交、加签的后端任务 action 契约和权限返回字段，否则前端不能安全展示按钮。
3. 必须确认图纸 PDF 是逐源文件绑定还是审批包级绑定；当前上传页单文件模型需要后端明确是否扩展为多附件。
4. 必须确认文件类别是否使用现有 DCC 类别，还是新增截图中的业务枚举；若两者不同，需要后端映射字段。
5. 必须确认现行有效版本读取来源和 14 位产品编号校验来源；不能在前端硬编码外部系统规则。
6. 必须确认 `INT/RE` 体系记录完整编码规则和后端授权策略；前端不得自行用前六位放权。
7. 必须确认下载留痕失败时后端是否以错误响应阻断 Blob；前端依赖该契约避免先下载后补记。
8. 必须确认密码强制更新周期、历史账号上线策略和强制修改入口；前端不能默认周期。
9. 必须确认 Word 打印模板上传、占位符、权限和版本规则；在确认前仅可复用现有 BPM 打印模板能力。
10. 外来文件评审缺少字段、节点、参与人、结论和输出物，不能进入前端实现。

## Reviewer 放行结论

该设计可在当前系统基础上实现截图需求的大部分目标：上传、审批动作、下载留痕、培训、发放、打印、密码和通知均有可复用页面与 API 基础。不可放行直接实现的部分集中在后端契约和业务规则未确认项，尤其是第四节点、流程动作、体系记录下载、密码周期、Word 模板和外来文件评审。后续实现必须按 BDD + 严格 TDD + subagent-driven 执行，并在真实用户路径 E2E 通过后再进入提交。
