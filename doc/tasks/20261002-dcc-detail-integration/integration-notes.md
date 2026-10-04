# 详情接线主管理 / 后端协调

- 新增 DetailApplicationPanel（INITIAL、局部、换版）、DetailObsoleteApplication（正式 obsolete API）、DetailSignoffAssignment + 同签名整改、WorkflowDistributionPanel 已挂公共 detail；审批中心模式也能显示指派和申请证据。
- 草稿 GET 使用后端最终 controlledFileId；restore 接 Boolean 后正式 GET。selectedSignoffDepartmentIds=null 时展示未保存并要求重新明确选择；needTraining=null 时必须选择。
- 新申请默认实际属性均从项目读取；原草稿 actual/defaultSource 保留分离；正式升版使用 selectedIterationId 调用 /submit，后端负责其基线校验。
- 较早小版无前端 latest 门禁，组件按 revision-options 实际 canPartial/canReplacement。
- 接线单元 9 PASS，相关 API 合计 16 PASS，既有 workflow 25 PASS，局部 lint 无 error/warning。全量 type/build 由 Root 单实例执行。

## 需要后端/Root Review
1. getTaskActionReadiness 目前只调用 resolveNativeProcessDefinitionKey，再 validateTaskAction；独立作废的 taskId/BPM 不属于 file 的 UPLOAD/REVISION 原流程，详情审批提交会先 readiness 因此作废会签/批准会被此接口挡住。需要后端读取真实 obsolete task 时与 reviewObsolete 同样验证后返回正式 readiness，不能由前端默认为 ready。
2. DccWorkingApplicationAttributes.effectiveDate 已由前端正式支持 Java LocalDate 数组并校验真实日期，不默认今天。9 项单元包含数组及非法日期。
3. RelatedFileRespVO.masterId/controlledFileId 目前无 Long 字符串注解；前端拒绝不安全 number，后端请核实全局 Long 序列化。
4. 新 ControlledFileStatus CONTROLLED_PENDING_EFFECTIVE 不在 shared/lifecycle.ts 的状态枚举，详情旧状态描述可能仍显示原码；已在详情独立受控事实区明确显示待生效和预设日期，但全局状态需 Root 统一处理。
5. 浏览跳详情应 path=/dcc/controlled-file/detail/{id}，query mode=manage 可选；不要 viewer=1/traceability=1。详情已处理 route.params.id。DccFileRelations current/history 主展示请浏览执行者接；详情指派只读本次历史关联快照。
6. 旧已结束作废的轮次没有列表只读入口可选；当前详情显示活动作废申请及原 UPLOAD/REVISION 冻结快照，不能从当前项目默认或签名串反推历史作废类型。
