# G46 四类主流程差异按HTML顺序修复

Status: in_progress。用户明确HTML业务需求是authority，已本地merge至int_qms ce88a18。唯一开发源码目录C:/IntRuoyiAll-int_main，不继续原ABCD或integration。

1. 批准项目产品→上传：统一正式产品identity来源。既有MDM绑定保真实MDM规则；新DCC批准目录须凭同租户COMPLETEDrequest＋ACTIVErelation真实catalog建立正式来源和snapshot，productMasterId不混catalogID。不把旧14DCCcode强加HTML普通productCode、不生成假产品。不通过兼容fallback或移除productrequired误判正常上传。
2. 自由实际文件上传：去除项目预设文件名强制白名单、项目文件模板可辅助；正式文件类型/全名binary占用/项目权限/源ticket继续真实校验。
3. 一个项目文件夹：用户只选projectFolderId，后台从正式folder/category/storage配置解析真实落位；不以客户端其他directory或默认第一个叶子猜定位。必要mapping由正式有权限后台管理，不增加第二次上传目录选择。
4. 项目审核批准待办／驳回通知：沿用现行合法配置审核人/现行批准资格（HTML尚未确认批准人配置），给精确人生成可跳转当前request真实待办/通知，状态办理和主line txn一致/重复不重复通知；不用新平行业务平台。

开发顺序以1→2→3→4；子Agent第一产品后端实现、Root界面合同Review/最终验证/数据库/服务/Git，frontend得到新正式合同后接1，不同时在共享Workflow乱写。每项先BDD和必要RED/GREEN，主line正向验收优先，细节后补。归并baseline不是上述四修复完成，也不是前全部27AC已PASS。
