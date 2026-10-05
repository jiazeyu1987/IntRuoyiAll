# G59 — 独立文控旧工作台摘要权限只读核对

Status: read_only_review_complete。Root真实R9/016独立文控native培训队列接口有权限、页面显示0，但旧工作台加载错误。这里只读源码，未读取账号运行态权限或请求明细，不能指定首个被拒端点，也不把native0当培训上传E2E PASS。

当前没有单一“workbench summary”接口。`workbench/index.vue:loadWorkbench` 用Promise.all组合下列三组；任一拒绝清空全部旧rows，并把所有旧指标显式改0，显示全局loadErrorMessage。这会将未加载/失败误显示无待办。native `loadOfflineTrainingTodos` 是独立loading/error/rows，与旧组合无调用依赖，保留该架构。

| 现区块 / caller | 正式接口 | Controller ACL / 业务范围 |
| --- | --- | --- |
| 我的审批待办 / buildTaskRows | GET /bpm/task/todo-page，各原DCC process key | BpmTaskController.getTaskTodoPage：bpm:task:query；查询当前用户任务 |
| 有审批任务时装配 | GET /bpm/process-instance/get；/bpm/task/list-by-process-instance-id；/dcc/controlled-files/{id} | process:get需bpm:process-instance:query并assertCanReadProcessInstance；task:list需bpm:task:query；DCC详情需认证且QueryService.canAccessDetail文件级事实 |
| 旧600秒培训确认 / loadTrainingTodos | GET /dcc/training-tasks/my-page，两原状态 | DccTrainingTaskController.getMyPage：dcc:controlled-file:training:mine；服务按当前userId的原progress，不是新线下上传资格 |
| 生效失败 / loadFilePageByStatus | GET /dcc/controlled-files/browser-page?status=FINALIZATION_FAILED | DccControlledFileController.browserPage：dcc:controlled-file:query；QueryService.getControlledFileBrowserPage使用目录/文件可见性过滤，并非类别APPROVE维护能力 |
| 新线下培训队列 / loadOfflineTrainingTodos | GET /approval-center/tasks/page，DCC TODO全分页精筛原native类型 | ApprovalCenterController.getTaskPage：bpm:task:query；既有provider再判真实doc_control/独立TRAINING_RECORD类别规则/文件范围，FE不授资格 |

源码锚点：`IntRuoyiFronted/src/views/dcc/controlled-file/workbench/index.vue` 的buildTaskRows/loadTrainingTodos/loadFilePageByStatus/loadWorkbench/canReadOfflineTrainingTodos；API training.ts:getMyTrainingTaskPage、workflow.ts:getControlledFileBrowserPage；上表Controller分别位于后端bpm controller/admin/task、bpm controller/admin/approval和dcc controller/admin/training/file。`workbench/presentation.ts:buildDccWorkbenchMetricItems` 当前count类型number，缺数据默认0；本批不改。

最小方案应基于现正式菜单权限分区，未授权旧600秒区不调用training/my-page且明确“无此培训确认入口权限”，隐藏该区和其指标或显示无权限，绝不记0成功；生效失败区依dcc:controlled-file:query，审批装配依bpm:task:query+bpm:process-instance:query。DCC文件/流程业务范围即使菜单够也可能拒绝，应在本区保留正式错误，不能转空列表。原native线下队列独立保留，无需增加training:mine、process-instance:query、super_admin或宽泛类别APPROVE。

三旧区应各自pending/error/loaded事实与count；只在正式读取成功后显示0。授权区HTTP/业务错误仍显示，某区失败不能清空另一已成功区或native队列。全局旧加载错误可以概括失败区，但不可吞其详情。这需要Root后续明确授有限workbench/index.vue+presentation/count投影及原有test，当前只有计划，无源码修改。

验收BDD：独立文控仅native许可时，native正式查询照常，旧training无菜单零GET且无0KPI；完整旧菜单用户各区正常实际值，已授权区403/网络故障显示该区错误和未知count，其他已成功区保留；actor/tenant切换旧响应不污染新身份。Root先查真实网络首拒URL与fresh permission，再决定对应区gate，不借角色名字推菜单。

本报告不是实际账号ACL PASS，没有新增授权或调度、数据库/API/浏览器/服务/Git/Maven/构建操作；G59 repair修复代码已封存不变。
