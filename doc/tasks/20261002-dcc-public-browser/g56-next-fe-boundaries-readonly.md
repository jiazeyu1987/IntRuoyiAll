# G56 后续前端边界与R02阶段呈现（只读）

Status: prepared_not_implemented。本记录补充已封审查矩阵，不改变G56已冻结shared组件、BDD或manifest。Root本轮要求先精确边界后授权；此处未改源、跑测试/环境、DB/API/UI/Git。

| 项 | 精确最小文件/入口 | 正式合同与验证边界 |
| --- | --- | --- |
| F02 公共项目目录维护 | browser/ProjectBrowserPanel.vue，必要时project-browser.ts；复用basic-data/components/ProjectFolderEditor.vue既有open/openDelete及get/save/deleteProjectFolder wrapper | 无新CRUD/模板审批/NAS。选中projectID/folderID，不用canReference或负责人等同维护编辑资格；backend已有update+editor/owner守卫。保存只重读同项目，切项目晚结果不污染。RED实际公开handler/renderer缺入口→GREEN复用组件及原维护/删除合同 |
| F03 只读档案/历史入口 | detail/index.vue viewer caller；复用view/presentation.ts buildControlledFileTraceabilityPath | 精确selected fileID→现trace只读route，不传management=1或赋编辑权限；保持正文独立canPreview/正式binary守卫。优先入口，不复制属性/历史平台。RED实际viewer按钮/handler→formaltrace准确ID及原历史/context回归 |
| F04 native进度与职责 | detail/index.vue progress caller、必要shared/approval.ts/handlingSummary，可复用DetailApplicationHistory已验证round投影 | getControlledFileApplicationRounds返回exact file/BPM/applicationType；匹配已核primaryBPM后以UPLOAD/REVISION/OBSOLETE真实snapshots/tasks阶段显示，不能File旧upload key猜独立作废；LEGACY准确身份保旧四级、mapping缺失不假legacy。当前文控培训职责沿实际node/actor，不改流程。RED actualprogress/renderer与旧legacy/context守卫 |

R02实际来源是detail/DetailRelationsPanel.vue.load：selected metadata读通过后无条件listCurrentRelations(selectedID)。BE DccControlledFileRelatedFileServiceImpl.getCurrentRelationView准确读currentset（无初始化写入），尚未受控的新Master没有set即抛DCC_RELATION_CURRENT_SET_NOT_INITIALIZED。G55受控后空集合PASS只证明后阶段，不能关闭未受控阶段问题；矩阵流程7的“读链存在/准确错误”不是该前阶段业务呈现已满足。

当前getControlledFileRelationPermissions/ControlledFileRelationPermissions/DccFileRelationPermissions只有selected.status/controlled/pendingEffect/executable、Master/project/权限，无latest受控source事实。selected.controlled=false可能是WORKING/历史但Master仍有当前受控版，不能据此跳读current；file旧过程/版本号也不能证明没有当前受控。

Root将安排BE同正式metadata新增hasCurrentControlledSource:boolean或精确latestControlledFileId:string|null。建议可同时提供nullable来源身份以对比current result source与Master；该判定必须同租户正式Master及当前受控source，不能仅看selected status。新增缺失/矛盾字段应准确拒绝，不能用未知值假作false。

有正式false时，child显示“无可用受控版本，本次申请关联请查看本版本审批快照”，不GET current、不伪造current空context、不写初始化。selectedSource仍保当前所选file身份，history子组件继续真实listHistoricalRelations，冻结关联不抹掉。有正式true时，现current读取/rowVersion/latest及编辑权限原样；待生效controlled仍可当前关系，不用执行ACTIVE替代。current错误仍显示，不catch造[]。

R02预计唯一FE源边界DetailRelationsPanel.vue与applicationRead.ts DTO/validator，必要阶段呈现state/实际watch/save/render测试；不动300KB父或GETstore。BDD/RED需证明从NEW未受控到有当前受控、WORKING/历史仍有current、真实history保持、未知projection拒、受控current原edit权限和latecontext守卫。Root先给稳定字段合同再实施，sourceOwner仍只已完成shared修复；此记录不是实现或实际页面PASS。
