# E2E-10 返工矩阵实测问题（Root 定向复核）

## C10-C：同类型驳回返工错误走当前公共矩阵确认

Given C01 文件2054545668044084194已在T1矩阵914026/v2（批准人910443，DCC241016C10文控审核人）下实际提交，主管真实签名802驳回；When 仅将自有公共T1恢复为914028/v3（批准人910442，DCC241016C10批准人），申请人不改类型，真实检出C01、上传同原名但不同内容PDF、检入新工作稿2054545668044084205并点击准备本次申请/核对并提交初始申请；Then 应沿用原驳回轮矩阵并检查原参与人可用性，不应要求接纳新公共矩阵。

实际：准备面板批准人为当前公共910442，实际点击提交后弹出“公共审批配置已变化，请确认本次使用的配置”，完整列出v2→v3及910443→910442。已截图并点击取消，未明确确认错误的新公共配置，未声称重提成功。此为实际检出/上传/检入/点击重提后的业务FAIL，非仅静态预览。

证据：runs/acceptance-2026-10-17T14-27-29-581Z 的 branch-progress.json / session.json，C-actual-rework-matrix-mismatch.png、C-resubmit-unexpected-public-reconfirmation.png、rework-checked-in-C.png、author-rejected-readback-C.png；原source与返工source页面历史哈希 aaf0a00ed2fe→777a3eabdabd。原驳回轮BPM a1191fa3-ca3d-11f1-ac5a-b082e25ec548。

Root后续复核已缩小准确根因：Query检入通过executionContracts.readInheritedDraftFacts已经从原执行snapshot保存正确旧draftApprovalMatrixJson；不能把“返工前驱字段未记录”直接当成主根因或给Query硬塞身份。真正丢失口径在DccApprovalMatrixBaselineService.preview/confirm无条件调用sources.read/readForUpdate最新公共矩阵，返回after/current并要求确认。本次以真实source_application_id/source_attribute_round逐级证明已执行驳回来源，在Baseline预检与最终confirm选择该原执行canonical；首次新申请及受控后新REVISION仍选择最新公共比较。

Root已唯一分配BaselineService及必要ExecutionContract复用小方法/对应测试；不改Query或Workflow。不得改共享矩阵、直接写库补身份、删除版本/申请历史或放松冻结合同。精确计划见fix-plan.json，生产改动须在Root实际RED后开始。

回归应先RED：旧驳回轮保留910443、公共变为910442，真实检入后准备/确认/实际提交均保持910443并进入原主管新轮待办；原910443不可用时明确阻止，不能用910442替换。

## C10-D：原申请人返工页缺主动换类型入口

Given D01原申请2054545668044084197在T1/v2真实提交并由主管签名驳回；When 原申请人从正式检出/检入入口上传同名不同内容正文并生成工作稿2054545668044084206，打开本次文件申请及更多操作；Then 应允许明确选择自有T2类型、看到新矩阵差异、确认后重提。

实际本次申请表仅有生效方式、会签/下发部门、批准人只读摘要、培训、属性、说明、保存和提交；文件类型/分类选择控件均0。更多操作未提供原申请人主动换类型入口。停在真实返工工作稿，未用新建另一申请、改DB或借文控高角色模拟该分支，结果BLOCKED（缺正式入口）。证据：session操作D-real-checkout-upload-checkin、D-real-rework-type-change-entry-inspection、D-type-change-entry-terminal-evidence及D-type-change-no-formal-entry.png。

静态依据：DetailApplicationPanel.vue没有类型选择/变类型确认；详情index.vue的修改基础信息要求doc_control角色，而DccControlledFileMetadataUpdateServiceImpl.updateMetadata只接受ACTIVE版本，无法作为REJECTED/WORKING主动换类型的正式替代。WorkflowServiceImpl.readNativeReworkRoute要求原类别/类型完全相同，另一路明确报“失败重提变更类型必须有正式主动选择和确认合同，不能自动重匹配矩阵”。因此不能仅补一个前端下拉或放松身份校验。Root须确定主动变类型的正式持久化/审批配置确认合同后，再安排最小接口与页面实现及RED/GREEN；本线程不扩写生产功能。
