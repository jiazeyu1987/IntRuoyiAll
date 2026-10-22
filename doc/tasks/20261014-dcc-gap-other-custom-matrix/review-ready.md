# G03 冻结交付 checkpoint

2026-10-15，实际分支 int_qms。Root 最终软件验收已正式接受：M12有效963 + M14独立45合计1008/1008 PASS，有效47份class报告，11865输入零漂移；F02 38/38、T04类型及B01构建均通过，2429生产输入零漂移。生产/tests继续冻结，工程仅Git提交/推送未授权，不标completed；Root统一清理/报告。

## 最新正式验证

- 最终：M14真实exit0、23 reactor SUCCESS，G48声明18与原父类27独立合计45/45、0 fail/error/skip；合并M12无生产差异的其余45类963 PASS，最终有效47份报告1008/1008 PASS，11865输入零漂移。M12当次exit1/45ERROR保留为历史，不冒称M12当次全通过。
- G03：canonical 23、token 8、Workflow 9、rework 1、native guard 2、类别 4+6，共 53 项全部正式通过。Workflow 9 已实际覆盖最后 direct NEW 同载荷 replay / 异两账号载荷拒绝断言；旧“增量待 M12”限制已解除。
- G06 A34 6 项以及原父类 PreparedExecutionInheritance 42、WorkflowSelectedIteration 38、RoundFlowContract 79 均正式通过。
- M12历史45 ERROR的合法Category fixture问题已补显式PUBLIC_MATRIX，不改生产/schema，并在M14以子类声明方法及原父类独立fixture正式复验通过；不再列为待验。
- F02：四 suite 38/38、0 fail/skip，实际 source 未改；包含 custom Panel 3 项、旧原组/IC21/取消和普通附件清单场景。A36 测试宿主必要依赖已由 G08 处理，G03 未并写。

正式证据：../20261014-dcc-gap-fix-coordination/final-verification.json、m14-verification.json、m14-reactor-receipt.json、m12-verification.json、f02-final-application-node-receipt.json、t04-types-receipt.json、b01-verification.json及其日志；摘要和原始证据SHA见official-verification-receipt.json。T04/B01均exit0，2429生产输入零漂移。未要求的E2E/部署不列软件失败，以下私有历史证据不再承担正式门禁。

## 当前实现

- 正式类别唯一 approvalSourceMode 明确区分 PUBLIC_MATRIX / APPLICATION_CUSTOM；管理端真实创建、更新、读取，上传候选按项返回来源或具体 blocker，坏配置不阻断合法兄弟项，技术错误准确传播。
- 自定义矩阵使用同一 typed 两账号对象和部门名单。PREVIEW 无虚构 file/round，创建后绑定实际 RESERVED 身份；严格 canonical、摘要、HMAC purpose/phase、真实主管组织证据及当前人员资格贯穿预检、保存、恢复、提交。
- 已接真实 Workflow/Resolver/Readiness/快照/Bound：主管仍首先进入既定流程；custom 不借公共 route/version，public 字段为 NULL，来源写入独立 typed runtime JSON；公共分支保留真实版本和原准备定义。
- 原申请保存配置在类别来源变化时保留显示并明确阻断；驳回检入使用真实新轮次重绑原配置。Query/Revision 的 A34 接线由 G06 完成，G03 未并写这些文件。
- 提交绑定服务端普通附件有序 IDs 与 manifest hash；确认等待期间变化会拒绝。Panel 使用实际保存的两账号、部门、分发、培训和生效值；PUBLIC V1/V2 恢复与原分组显示采用正式合同。
- 两份正式迁移已落盘并做静态/依赖政策验证；本线程未执行真实数据库迁移。

## 历史定向验证及限制

| 范围 | 已有结果 | 准确边界 |
| --- | --- | --- |
| actual custom Workflow / public V2 邻接 | regression-custom-13-public-v2：9/9，0 skip/abort，runner 当次 source stable | 该旧证据之后的 Resolver / Execution 与 NEW replay 增量现已由 M12 当前源 9/9 正式覆盖。 |
| actual rejected→检入→候选重提 | rework-10-stable：编译成功，JUnit 1/1，runner exit 0 / SOURCE_STABLE | 真实 Query/Revision/SourceOwnership/继承/Flowable；前置 terminal REJECTED 是 fixture，不宣称电子签名拒绝动作或 UI 验收。 |
| RF11-02 public native source guard | rf11-red：1 fail/1 pass → rf11-green：2/2，source stable | 实际 current round、冻结 native proof 和 BPM 变量；旧无 native 登记的历史分支保持原规则。 |
| 纯 custom canonical / preview / reserved | 23/23 | 私有当前源编译，摘要合同不等于签名或工程联合验证。 |
| custom HMAC / 原 PUBLIC 签名向量 | 8/8 | 旧 PUBLIC 源码移除新增 custom 区块后与改前快照逐字一致（规范化 LF）；当前 token 8、Workflow 9、rework 1 已由 M12 正式通过。 |
| 类别管理 / projection | 4+6 = 10/10 | 私有真实被测源码与显式外部端口/H2；不冒称完整 tenant/security 拦截器验收。 |
| Panel、附件清单、类别编辑入口、时间 parser | 最新四文件 12/12 | 实际 SFC handler / 源方法，外部 UI/网络端口显式隔离；不是 Playwright E2E。 |
| 迁移 | 类别 SQL 6、custom SQL 4 静态测试；依赖闭包政策 3/13 项通过 | 没有真实 MySQL 执行或部署验证。 |

private runner 将当前明确列出的被测 DCC 源码编译到任务私有目录，其他模块和框架依赖使用既有 classpath；不代替正式 Maven -am。runner 目前精确复制共享 create_tables，不再补列或补假表；明确附件 fixture 在自有测试中，Maven/private 同源。新编译诊断使用英文及 UTF-8；历史乱码不猜测或改写。

rework-9-initializer 虽 JUnit 1/1、Java exit 0，但 Node 在 G06 源码变化时检测到 drift，不计稳定 PASS；稳定替代证据为 rework-10-stable。较早 rework 编译、端口接线、fixture 或断言失败保留为历史，不能作为最终产品 RED 或当前失败状态。

历史只读比较原始 sources.json：旧私有主链 9 项证据与冻结源有 3 处差异（Resolver、Execution、WorkflowTest）；rework-10 与冻结源仅 WorkflowTest 不同，且该类并非其执行目标。现有当前源正式 PASS 依据为 M12，不是该旧 9/9。

相邻旧 dcc-review-matrix-tab-static.spec.js 曾因未修改 Dialog 的 label="标记" 断言失败；没有放宽该断言或声称所有旧静态测试通过。

## M12 输入与归属

- final-checkpoint-manifest.json：54 个当前文件原始 SHA-256，另列 Root/G04 上传三文件及 G06 Query/Revision 只读依赖指纹。共享文件的完整 diff 不因此全部归 G03。
- declared-test-methods.md：7 个自有 Java 类共 53 个声明 @Test；三份继承 fixture 子类必须只选声明方法，原父类由 Root 独立完整回归。此为选择清单，未新增执行 PASS。
- upload/index.vue、upload/submitter.ts、upload/approval-matrix.ts 已 A32 归还 Root/G04；G04 更新的 upload state/page tests 同属冻结输入，G03不再写。
- 本次整理没有修改任何生产、测试、runner 或共享资源；不运行 private Java/Node 重复套件、Maven、全类型、构建、E2E、服务、真实数据库或 Git 提交/推送。
- F01/A36：旧 dcc-detail-application-ic05 宿主缺少实际 isPublicMatrixExecution 声明依赖，由 G08 只补必要依赖并保留原组/IC21/取消断言；F02 38/38 已通过，G03 未并写或提前改生产。

Root M12范围已实际执行，自有53项、A34 6项及各原父类通过，含最后NEW replay新断言。M14补齐独立fixture的45/45后，全软件域正式接收；工程仅Git收尾未授权，由Root统一清理/报告，本线程不提交或跨线程发消息。

## 交付文件

- task.md：范围、BDD、冻结授权、blocked工程状态（仅Git未授权）和Cleanup Keep。
- verification-report.md / frozen-checkpoint-verification.md：已有实际结果、源码差异、未验证增量和正式门禁。
- changed-files.md：当前文件路径和归属边界。
- final-checkpoint-manifest.json / freeze-source-check.json：当前 SHA 与旧 PUBLIC 原文不变证据。
- declared-test-methods.md：Root 最终正式选择的声明方法。

本页是当前交付入口。历史阶段和原始失败保留于execution-log.md、verification-report.md及证据目录；最新软件验收已接受。工程仅Git收尾未授权，不标completed；Root统一办理清理/报告，不要求追加E2E或部署。
