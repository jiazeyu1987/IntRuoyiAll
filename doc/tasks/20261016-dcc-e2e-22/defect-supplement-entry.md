# E2E-22 / R22-01：负责人指派页面缺少后补固定关联入口

## 实际结论

2026-10-18，真实前端复现。业务分支 FAIL；完整 E2E-22 未通过。这里只记录问题，未修改生产源码。

环境：int_qms，前端8061/PID24564，后端48061/PID34440；运行Jar SHA256为264efbd4ee539115f1a7d5b43941029a2fd7a03f03b7df9d9e8964d8cf3ffeb5，Root运行态证明为browser-replacement-runtime-proof.json。

## 可恢复现场

- 租户芋道源码，办理人DCC241016C22L；账号、部门、岗位、签名及本例类别查看矩阵均通过真实UI准备。
- Main：DCC241016C22MAIN，A/1，文件2054545668044084249。
- BPM：1842903f-cb00-11f1-ad52-b082e25ec548。
- 当前L待办：3481664b-cb00-11f1-ad52-b082e25ec548，待会签审核、0/1完成；尚未签名指派。
- 原上传关联：Rel1文件2054545668044084243，A/1，页面显示“原上传选择”。
- 待补充目标：Rel2文件2054545668044084248，A/1，已在受控浏览确认“受控（已生效）”，与Main属于本例P1项目。
- 不重建、不重新提交Main。修复后新独立L context真实登录，从该待办续跑。

## 真实页面 RED

Playwright从L本人审批中心待办打开上述Main，定位含“先签名指派本部门人员”的可见section。页面只有会签人下拉、指派意见、当前账号签名密码及“签名确认指派”。该section内“补充/后补/关联”按钮数量为0，预期存在正式后补关联操作。

脚本：scripts/business-steps.cjs的inspect-supplement-entry阶段。断言原文：E2E-22 requires a real UI action to supplement fixed related files before signed assignment；Expected 1 / Received 0。

证据目录：e2e-artifacts/fixture-1792329547050/。

- Main-L-assignment-missing-supplement.png：完整指派区，密码控件已遮蔽；没有后补关联入口。
- dom-48.json：最终只读页面，包含原文件、BPM、节点、冻结Rel1 A/1和原上传来源。
- fixture-manifest.json中的supplementEntryInspection：原task、办理人、按钮与输入控件清单、supplementEntryCount=0。
- trace-1.zip至trace-29.zip：本轮脱敏trace；签名凭据输入时停止追踪。

在同一页面已真实打开“查看固定版本”，非空PDF canvas、正文搜索DCC241016C22_REL1_A1_FIXED命中、DCC241016C22_REL1_B1_LATEST不命中。Main-original-fixed-Rel1-A1.png保留这一局部正向证据。Rel1尚未升级，因此这不是“源文件升级后仍固定旧版”的完整通过证据。

## 直接代码依据

- IntRuoyiFronted/src/views/dcc/controlled-file/workflow/SignoffAssignmentPanel.vue：模板第3—12行只渲染会签人、意见、密码、整改slot和签名按钮；submit构造的指派请求没有采集supplementalRelatedControlledFileIds。
- IntRuoyiFronted/src/views/dcc/controlled-file/detail/DetailSignoffAssignment.vue：第16—24行slot仅在revision流程渲染既有关系的整改安排，没有可选新增固定版本的控件或文件选择器；saveAssignment直接转发指派请求。
- IntRuoyiFronted/src/views/dcc/controlled-file/workflow/workflow-actions.ts:36已经声明supplementalRelatedControlledFileIds。
- IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/vo/DccSignoffAssignmentReqVO.java:16接收同名字段；DccWorkflowSignoffAssignmentService.assign调用selectedSupplementalRelations并在原签名事务prepare/freeze阶段处理。

源码指纹见supplement-source-fingerprints.json。上述为直接静态证据；没有通过API或SQL补造后补记录。独立VIEW矩阵此前缺失导致的canPreview拒绝已通过本例正式UI配置并回读解决，不是当前缺入口的原因。

## 修复验收 BDD（交Root唯一源码写入者）

Given Main A/1原上传冻结Rel1 A/1、Rel2 A/1在同项目已生效，L打开Main当前正式指派待办；When L通过可见正式入口选择Rel2 A/1为本次后补关联，再本人签名指派C；Then 同一签名事务保存后补固定版本，保留原上传Rel1 A/1，后补来源、签名、节点/BPM和所选版本在该轮只读证据中一致，重读不追随最新版。

整改安排仍遵守revision流程的正式边界，不能把初始上传指派扩展为不允许的整改提交。Main后续revision再覆盖整改冻结及跨版本隔离。

修复不得改前端跳过canPreview、使用API/SQL造关系、补假签名、删除本例断言或重建Main规避原待办。入口补齐后继续原E2E-22全部参数，当前两版本/时间/引用去重/后补与整改完整链路均不能写PASS。

## 运行器与残留

本轮runner47716、Chrome根进程21488均正常关闭，handle17809退出码0，归属进程核查为空。保留本例三个正式文件、八个用户及配置，供原对象续跑；未执行业务清理。Root review前不执行cleanup apply、不删除证据。没有Git、构建、服务或生产源码写入。
