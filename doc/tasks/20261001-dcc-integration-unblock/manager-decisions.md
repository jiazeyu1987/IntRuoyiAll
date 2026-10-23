# Manager Decisions MD-20261001

此文件由主管理登记，覆盖旧任务中“B/C/D代码缺失、未收到Review、培训/保留输入未知”的阻塞事实。并非整体Review通过，也不授权Git、业务库、服务、部署或E2E。

## 已确认业务

- 培训：文控上传正式线下培训文件即可完成培训节点；不加逐人系统确认或第二个核验节点。票据、当前文件版本、真实流程轮次及文控权限必须校验，下一步文控审核。
- 作废：保存20年，按实际作废时间起算。独立作废在批准完成时起算；旧版在新版生效自动作废时起算。服务端调用DccObsoleteRetentionService.retain(tenant,master,obsoleteAt)，记录实际时间并保持原编号名称占用。

## 已同步正式依赖

四模块生产代码/schema和共享模型在每个worker均已存在，原Owner交付与Root整合来源见dependency-sync-manifest；这是主管理预置整合层，不应reset或宣称为本模块独立实现。

正式D三个adapter已实现latest/B leader和folder/Query权限，禁止创建第二套或继续假实现。DccApplicationRoundService.bind(project,type,fileId,bpmProcessId)在申请事务持久分配B整数快照轮次；require只读正式绑定，没有记录则报错，不猜1。A/B/D分别保留真正BPM字符串及映射整数，各自身份不能互换。

## 公共请求字段登记

- SubmitReqVO、SubmitIterationReqVO、ObsoleteReqVO：projectAttributes，类型B正式DccProjectAttributes，只携带用户本申请实际值；默认来源只由服务端B服务读取。
- 上述三个请求：selectedSignoffDepartmentIds，类型List<Long>，实际部门集合；服务端验证启用部门、负责人及本动作路线、冻结本轮，不借selectedSignoffUserIds代替。
- SignoffAssignmentReqVO：relationArrangements，类型List<DccRelationContracts.Arrangement>；真实签名事务保存，不能另开无签名HTTP写入口。
- 正式申请ID统一对应controlledFileId。独立作废用所选文件ID作为applicationId，动作OBSOLETE及BPM轮次独立；不会覆盖UPLOAD/REVISION快照。

字段只是已登记可编译合同，A仍须接正式校验和实际事务，未调用不能算功能实现。C幂等载荷应纳入实际意图、属性/部门等正式请求事实，避免同key变载荷重放。

## Review接续

Review状态changes_requested。各模块manager-feedback明确待修和已解除阻塞，继续所属部分并提供真实RED/GREEN；不自行替主管理关闭公共页面门禁。

公共页Root会继续统一接线。各模块先完成自身实际入口调用、API/projection与独立组件；需要Root处理的明确文件/方法/props列integration-notes，不以空泛“等主管理”代替可以执行的服务修复。

仍未知的产品创建审核人、提醒提前量保持单独配置/待确认；不能影响已确定独立开发，更不能猜值或默认通过。
