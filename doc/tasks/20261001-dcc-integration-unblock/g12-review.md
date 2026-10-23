# G12 引用使用明细与任务后台条件

2026-10-03。主目录int_qms、整合分支codex/20261001-dcc-integration，共同HEAD a801dc8b91579241e221d129ab34343997673f40。前轮有实际UI03/143回归/types/build进度，非全目标阻塞。本轮已登记负责人后端/详情和usage前端Owner，但尚无新正式Agent续派回执；Root按主任务显式临时范围完成下列独立工作，不假称Agent正在执行。

## 引用明细真实前端接线

source列表和已保存reference行均提供“使用明细”，独立DccReferenceUsageDialog接已交付的正式usage-page。源文件与目的项目/文件夹分开显示，保留服务器distinct全局/可见项目计数、目录引用total/分页和detailsRestricted。受限说明明确，不将不可见项目伪造为空或全局计数改成当前页项目数。

新增reference-usage.ts严格校验精确Long、tenant/source/Master、真实分页/行数、引用ID与目的folder唯一性、固定selectedControlledFileId/版本/状态、独立canPreview。APIwrapper请求前校验合法ID/page，不用客户端筛选/去重/补齐total。当前两目录同项目计1项目明细2行、所用A/1与A/2保持原身份；作废选定版保留历史状态，不自动跟latest。

正文点击重新读取实际selectedControlledFileId的正式relation-permissions，source项目与目的项目不能混淆；权限撤销/身份不符不打开窗口。关闭/换列表context/分页/卸载使旧明细/正文响应失效；错误局部显示，不渲染0success；不加取消/编辑资格。

实际公共SFC入口RED1缺使用明细按钮，GREEN15后扩展错误/关闭/scope/unmount/撤权/已保存引用入口以及validator/API行为，共44定向PASS。最终13文件组合154 PASS，0fail/skip/cancel（g12-final-combined-ui.log）。五生产文件lint0/0；正式项目type/env.local build session14511已经核实exit0/Build successful。测试使用真实转译组件/正式wrapper和显式transportfixture，非真实页面/E2E。

## 后台任务显式关闭条件

Root在主任务记录临时唯一Java/Maven范围后，补真实ApplicationContextRunner四场景，原两处显式false仍注册bean的有效RED2/4（g12-background-conditions-red.log）。批识别startup recovery新增ConditionalOnProperty复用已有dcc-batch-recognition-enabled；临时上传清理scheduler新增独立dcc-upload-temporary-cleanup-enabled，未配置/true保留既有行为，任务运行须显式false。未改恢复/清理算法、共享yaml或其它scheduler。

首次GREEN22次/4类及主应用compile session14204 exit0。该命令有一项误写不存在的DccControlledFileUploadNamePreflightDatabaseTest，Surefire允许未匹配，不能称这项已运行。随后准确用DccUploadNamePreflightDatabaseTest补最终五类受影响回归session51311，已核实37/5类PASS，0fail/error/skip且BUILD SUCCESS，包含15实际源名预检用例。原命令保留，不把未执行项计入通过数，不将22与37相加冒充独立场景。

测试证明真实条件注册与正常startup/cleanup端口调用，但端口为明确替身、不连接运行库。正式Flowable自动DDL/Quartz全量注册和其它任务启动副作用仍须单独关闭/核验；不能据此称整个应用已隔离。

## 下一步仍需实施/验收

批准选择文件负责人仍未实施（已确认在批准弹框、不新增节点），后端正式签名/字段/迁移与前端/历史需冻结接线；其余已确认需求仍按完整验收矩阵逐项审计，不以这两个工作缩目标。

共享MySQL23306/Redis26379仍不可用，恢复共享依赖许可、提醒业务值和迁移授权未答，17根/43闭包仅准备。无实际DB写入、服务/主服务/48081操作、E2E、Git提交/合入或发布。持续目标仍active。
