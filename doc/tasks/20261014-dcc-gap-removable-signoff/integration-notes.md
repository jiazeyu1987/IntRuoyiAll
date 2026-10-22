# G04 最终接入说明

## A32协助CUSTOM上传：G03公共接线注意（2026-10-15，当前实施中）
- 当前G04只持upload/index.vue、submitter.ts、approval-matrix.ts；BE/公共workflow.ts/applicationRead.ts/DetailApplicationPanel.vue仍G03，不并写。
- UploadApprovalMatrixState现在结构核验正式applicationCustomMatrixCanonical：PREVIEW/RESERVED格式、租户/项目/类别/类型/contextKey/申请类型/精确prepared定义、两账号与唯一selectedSignoffDepartmentIds；不计算/生成或声称验证服务器HMAC。正式token/digest/主管proof仅取响应并原样送正式API，服务器仍复核资格与签名/HMAC。
- 为防payload使用旧部门票据，matrix.selection(SAVE/SUBMIT)现在返回ScopedApprovalMatrixSelection（局部扩展既有正式request字段selectedSignoffDepartmentIds，类型string[]|null），与同次配置token关联。不是新端点/平行名单；最终payload仍用唯一draft.selectedSignoffDepartmentIds，经requireApprovalMatrixSelection第三参数核对两账号/部门一致再发。G03 Panel如自行拼Selection，应保留state.selection返回的该字段，不能重造不含名单的CUSTOM selection。
- ApprovalMatrixScope新增可选requesterId、attributeRound，NEW upload传当前真实账号requesterId；G03 DetailPanel displaySavedCustom创建state时建议透传当前正式saved编制人ID（或已证明的当前办理requester）及saved.attributeRound，用于额外结构相等校验。scope.controlledFileId已有则强制RESERVED并核canonical file/正数round；scope无file则强制PREVIEW且canonical不得混file/round。不代替server授权。
- 上传页为NEW入口，不新增草稿恢复端点；草稿保存后导航既有精确detail。恢复真实saved null/两账号/下发/保留context/round并只走CUSTOM preview的正式路径仍由G03 DetailPanel/applicationRead负责。本线程有RESERVED state恢复软件合同，不能冒称已跑真实Panel恢复或后端freeze/rework。
- 若公共API类型需要显示ScopedSelection.selectedSignoffDepartmentIds，最小type hunk为ApprovalMatrixConfirmationSelection新增可选selectedSignoffDepartmentIds?: string[] | null（同正式请求字段）；只由G03按Root授权接入，当前本线程用局部扩展不改公共文件。

## 当前合同
- 公共 LEGACY 矩阵原节点、marker、required、候选CSV及摘要不改变；本次 selectedSignoffDepartmentIds 是唯一实际会签部门名单。
- null 取正式类型全部默认会签部门（含原可选来源），R02真实RED后已修复；显式空数组不能回填公共默认，前端提示“请至少选择一个会签部门”。完整默认与显式编辑路径已开发测试。
- 原来源 RuleGroup.applied 可以为 false（包括原 required/● 会签组）；源 required/marker 仍为真实原事实。批准和文控组的强制参与/组内规则不改变。
- 派生 MATRIX_REVIEW 仍为 DEPT + ALL + 100 + requireAllApprovals=true，B/C 各保留一个部门身份；同负责人不能合并义务。

## 共享文件复核及必要验收（不在本线程写入范围）
- FE upload/index.vue：已读 import/route refresh/submit confirmation 均调用同一个 signoff-departments helper，没有固定必选部门禁删控件。当前未见必须接线差异；Root/G12/G01/G03 后续接入必须保持该 helper 和精确本次 selectedSignoffDepartmentIds，不从公共原组反填删掉的部门。
- BE DccControlledFileWorkflowServiceImpl.java，persistSignoffSelection/saveStageObligationSnapshots/buildDccTaskObligationIdMap：当前从已解析 candidateSourceIds 保存选择、逐部门义务；需 Root 最终集成验证删除 A 后 A 无任务，B+C 两项义务均完成且通过才进批准。未申请写入。
- PUBLIC runtime/source JSON 和 native supervisor token：实际名单包含增补部门；公共来源组只记录来源，不可把 required 再解释为本次不可删除限制。原 source digest 和 proof 的精确租户/申请/名单身份不可变更或绕过。
- G03 接收路线派生前须 Root 复核归还。本任务未实现“其他类型自定义矩阵”，不新增并行 API。

## 验证边界
前端26/26、BE GREEN39/39、相邻回归76/76，0skip；默认optional RED39项4FAIL已真实执行。当前Workflow/Token/Readiness/Frozen门禁只读编译进入任务私有目录并执行，其他依赖仍现有classpath，非完整工程当前源码PASS。Maven、共享target写入、E2E、服务、真实业务库均未操作。

## 归还
G04已在review-ready.md申请Root复核接管，从该报告起冻结已授生产/测试；Resolver供G01接线、路线派生供G03随后接线，顺序由Root登记。最终hash见final-source-manifest.json。
