# G44 当前代码与HTML逻辑差异主管理核对

对照：当前整合worktree codex/20261001-dcc-integration，需求HTML v1.6／27AC及最新用户确认；尚未合入int_qms。用户当前问题是逻辑差异，未做真实E2E不能代替逻辑缺失判断。

| 编号 | HTML目标 | 当前确凿差异 | 证据 |
|---|---|---|---|
| LD01 | 项目产品批准创建后可进入上传 | jointwriter只建DCC catalog及relation，没有upload要求的MDMproductMasterId；实际新项目270批准COMPLETED、正式submit被拒file0。upload还用projectCode填productCode | g44-project-upload-identity-gap.json；Workflow.prepareSubmitContext2276/resolveDccProductFromProjectCode2829；upload1486-87 |
| LD02 | 选择项目／存储文件夹／文件类型／实际文件 | 另必须配置project文件模板阶段／类型／预设文件名白名单并三级classification映射，否则合法类型与文件也不能上传 | backendchild g44-requirement-logic-delta-review.md / upload99/126/259、Workflow.validateUploadSelection2255 |
| LD03 | 按项目存储文件夹归档 | 项目逻辑folder之外，还要求选category独立绑定子树最后叶子directoryId；project创建模板不会提供该额外目录 | upload63/206/2543；Workflow.validateSelectedDirectory2282 |
| LD04 | 审核人收到申请、批准人进入待办、驳回通知申请人 | 当前项目申请在产品目录records弹框手动查询办理，无对应receivedtask/notificationcaller | ProjectProductCreateService/create/review及State.markApprovalDecision；ProductCatalogTabPanel records；DccApprovalTaskAdapter目前只有文件 |

批准仍固定用户名admin是当前限制，HTML D02明确批准人配置／本人兼任未定，不把它误判成已确认配置要求的违背。引用固定实际选定版本，HTML引用跟最新只是建议，当前关联跟最新是另一独立已实现合同；不能混报。初始版本仅letter/1相对A/3建议属于未确定政策，不乱定为confirmedbug。

当前正式upload/revision signoff→approve→trainingoptional→docreview→control→distribution；obsolete批准结束、20年保留、futurecontrol旧版等生效、关联最新与历史独立、三属性默认／actual／history、owner在批准选、reference仅项目leader、partial/replacement/selectedolderbody/reworktarget守卫已有当前实现及定向证据，不能从旧HTMLsource注释宣称仍没有。真实完整业务尚未验收。每分钟activationjob运行默认off，这是实际部署／验证缺口，与代码没有日期切换逻辑区分。

Rootactual主要进展：projectrequest6/270完整UIreviewapprove、leaderUSEROWNER/folder/tree/template、PDF真实上传preview／二次确认取消；sourceFDA vsdefaultCE再真实submit失败准确记录。审计identity缺陷已finiteTDD184/11／独立Review/package并新UIevent205实际currentusername matches/旧6hash unchanged；blocker显示有限fix已4renderer/27related/lint/fulltypesPASS，actualsameerrorwhoDOM核验尚pending。当前不修改MDM产品编号政策、不用catalogID充MDM、不直接库repair、不自动重提失败业务。
