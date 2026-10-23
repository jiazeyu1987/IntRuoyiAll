# G11 跨项目目录交付与新业务确认

2026-10-03。主目录int_qms，整合分支codex/20261001-dcc-integration，共同HEAD a801dc8b91579241e221d129ab34343997673f40。前轮是实际UI02/136回归/types/build进展；本轮保留完整目标，不累计全目标阻塞。

## 实际Agent执行

detail_closure已正式followup、实际修改并交FINAL_ANSWER，不凭task文件推断启动。仅写DccFileSelector.vue、独立selector-project-directory.ts和专属/受影响测试，原自动打开单次消费和其它Owner生产资产保留。其他G11分工尚无新执行回执，不能记运行。

## UI03 Root Review

Root读取实际候选导航类和共用选择器：项目搜索/分页使用getProjectDiscoveryPage服务器total，项目逻辑目录使用getProjectFolders、buildProjectFolderTree及正式身份映射。候选项目选择只改导航，不改顶部Source、原已选或引用目标；有folder才构造directory selector查询，项目根仅导航，用户可明确切global。

generation与分页/目录sequence分别保护晚响应；关闭/unmount和Source租户变更清旧上下文，原导航错误局部可见，不恢复旧树或用NAS目录补位；全局搜索继续按正式name/preview授权。currentSource不在项目第一页时按原Source与正式folder读取定位，不从第一页猜默认项目。

子Agent有效RED3→GREEN7、关系Vue25/组件18/浏览Vue14与两源码lint0/0。Root实际五文件组合80 PASS，最终12文件143 PASS/0fail/skip（g11-root-final-combined-ui.log）。正式project types/env.local build session50801已经启动，等待其真实回执；不是E2E。

最终session50801已正式核实exit0/Build successful。g11-ui-receipt.json记录143组合、当前20项生产/测试资产SHA256及mergedfalse/goalCompletefalse；不累加前批136或131为独立场景。当前UI03离线交付 accepted，真实页面仍待验。

## 文件负责人：最新用户确认与真实缺口

用户正式答复“批准人在批准弹框中选择，不新增节点”。适用于上传/升版，不向独立作废增加节点。本轮全模块源码检索与实际ApproveTaskReqVO/Workflow.approveTask/前端approve handler读取确认目前没有fileOwner/documentOwner/文件负责人存储或选择入口，不能把新增文字当已实现。

必须同本次正式MATRIX_APPROVAL通过与电子签名关联验证正式启用同租户账号，保存本轮选择并保留历史；驳回、会签通过、文控审核或独立作废不能借这个字段修改负责人。默认、候选权限和负责人的额外授权不能借项目OWNER/admin推断。已有审批流程保持，不新增BPM节点。

本轮HTML/主任务同步选择位置；实现归属将交后端冻结正式字段/前向迁移/签名证据和候选查询，再交详情Owner接现有批准弹框。仅准备方案，尚无正式续派/源码交付，不关闭此项。

## 运行准备发现

SchedulerManager.isEnabled只判断scheduler!=null；仅spring.quartz.auto-startup=false不阻止JobStartupSyncRunner全量写注册。因此准备无全量Quartz写入的任务启动应排除QuartzAutoConfiguration，建立null scheduler；自动生效job真正启用另须批准及Quartz证据，不能用该无调度运行配置冒称自动生效已验。

实际DccControlledFileBatchRecognitionStartupRecovery的PostConstruct没有沿用已有dcc-batch-recognition-enabled=false，仍跨租户恢复任务；DccUploadTemporaryFileCleanupScheduler也无本机控制条件。主任务已记录两处显式条件修复BDD/范围，实际后端尚未开始，不以方案关闭风险。其余启动/计划任务仍需按真实运行库和归属核对，不宣称这两处足够隔离全系统。

当前MySQL23306/Redis26379及任务8067/48067仍无监听。共享依赖启停许可、提醒配置和实际迁移审批仍待用户/环境；17根/43闭包只准备。无真实DB写入、服务、E2E、Git提交/合入或发布。

仍未实现：引用使用明细前端、批准选择文件负责人的后端/前端与历史、上面任务环境后台控制。完整需求/E2E/int_qms本地合入仍未完成，goal active。
