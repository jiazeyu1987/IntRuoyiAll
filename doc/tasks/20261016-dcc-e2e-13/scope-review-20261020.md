# E2E-13 原合同最终范围复核（2026-10-20）

结论：**scoped PASS**。原始E2E-13核心断言均有保存的真实UI证据及既有Root独立review支持，没有核心断言缺口。本次为保存证据复核，不是重新执行E2E；R5/R6失败和原始报告不改写。

## 合同与逐项证据

原合同：doc/tasks/20261015-dcc-six-thread-followup-review/e2e-test-cases.md第39行，批准弹框默认U、可改O、负责人为本次批准确认值、不新增批准后负责人节点、培训记录上传仍归原上传人U。既有Root结论：doc/tasks/20261016-dcc-24-e2e-coordination/e2e13-root-review.json，ROOT_ACCEPTED_CORE_OWNER_AND_UPLOAD_DUTY_WITH_AUXILIARY_ACCESS_LIMITATION；review明确两分支、批准签名、直接进入D、U上传对话框及O无上传控件均通过。

| 核心断言 | 复核结论 | 保存证据（本任务目录相对路径） |
|---|---|---|
| 默认U，分别保留U或改O | PASS，两份精确原单 | runs/20261018-resume/evidence/Retain-owner-default-U.png；Adjust-owner-default-U.png；Adjust-confirmed-owner-O.png |
| 批准确认值与签名留存 | PASS，4218/A1=U/853；4222/A1=O/860，各自BPM对应 | 同目录Retain-owner-and-signature-persisted.png；Adjust-owner-O-signature-persisted.png；fixture-manifest.json的approval.ownerFact/signatureFact与step-file-task-signature-trace-map.json |
| 无批准后负责人选择节点 | PASS，两分支noPostApprovalPicker=true，批准后的实际下一任务均D | fixture-manifest.json的branches.*.approval.nextTask；对应trace；Root review复核 |
| U仍负责培训记录上传 | PASS，两份原单已进入培训记录阶段，U均实际打开上传对话框 | 同目录Retain-U-training-upload-dialog.png；Adjust-U-training-upload-dialog.png；fixture-manifest.json的training.uUploadDialogReachable |
| 改O不转移上传职责 | PASS，O工作台原4222详情可读负责人/签名，上传控件0、培训上传写0 | 同目录Adjust-O-after-fix-readable-owner-no-upload.png；Adjust-O-after-fix-final-DOM.png；fixture-manifest.json的afterFixOEntry；R5/R6保存结果亦保持O/860和上传控件0 |

Retain原文件2054545668044084218，BPM 4ac8491b-ca6a-11f1-9342-b082e25ec548；Adjust原文件2054545668044084222，BPM fb0c1653-ca6f-11f1-9342-b082e25ec548。两分支没有混用申请或重新签名。原批准/U入口保留先前4000f1ef包证据，O修复后metadata/无上传入口按既有Root接受的a27b8d66包证据；R5/R6仅为后续辅助复验，不冒充同一包重新跑通整链。完整路径、任务、签名和trace见新机器结果e2e-13-scoped-result-20261020.json。

## R5/R6附加范围

O阅读受控正文、下载附件及DAILY独立入口拒绝均不是原E2E-13断言。R6原O缺项目302正式阅读规则造成TRAINING_APPLICATION正文metadata拒绝的事实保留，正文未通过；它只阻塞附加正文验证，不再阻塞本例核心验收。DAILY通过真实“存储目录→全域→文件编号”完成查询、目标无可见入口的事实保留，不宣称DAILY正文端点拒绝或独立状态门禁通过。

R5/R6原始result、错误退出、服务错误、trace、SQL只读定位、两处窄修复的RED/GREEN与最终前置缺口交接全部原样保留。本文和新scoped结果只是依据最新范围授权新增结论，不把历史FAIL改成PASS；原e2e-13-result.json、runs与旧handoff不覆盖。

## 验证与交接

本轮仅结构/引用/原证据不可变性复核；不重跑已通过批准/签名、323/37/14或新增14，不运行浏览器、Maven、API/DB、服务或Git提交推送，不改Guard/权限。保留原业务资产。工程收尾由Root接管，不能将业务scoped PASS等同于已cleanup/提交推送/completed。模型要求保持gpt-6.1-sol/high。

最新业务结果：e2e-13-scoped-result-20261020.json；Root交接：root-handoff-scoped-pass-20261020.json；结构和历史文件SHA核验：scope-review-20261020-integrity.json。正文、下载、DAILY及实际培训记录上传/完成不在本次PASS声明内。
