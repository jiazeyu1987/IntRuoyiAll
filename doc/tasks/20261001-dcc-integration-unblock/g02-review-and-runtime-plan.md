# G02 Review 与本机运行准备

2026-10-03。源码位于既有 `codex/20261001-dcc-integration`；主目录实际分支 `int_qms`。本记录沿用主管理任务，未创建新总任务。目标仍是全部业务验证后本地合入；本批结果不是完整目标完成。

## 已落实的 Review 修复

1. 作废审批预检：`DccControlledFileWorkflowServiceImpl.getTaskActionReadiness` 原来将独立作废任务套入原上传/升版 BPM 校验。新增只读作废分支，与 `DccWorkflowSignoffAssignmentService.reviewObsolete` 共用真实文件/租户/受控正文/任务/阶段/参与人/已签名指派门禁；预检不产生签名、任务写入或状态推进。正式审批仍重新验证。
2. INITIAL：抽取共同申请字段校验，用明确的 `buildInitialCommand` 构造初始申请；不再用 PARTIAL 加伪受控基线后覆盖类型。属性深拷贝、日期/部门/培训校验、确认后上下文变化拒绝仍保留。
3. 公共生命周期：补 WORKING 与 CONTROLLED_PENDING_EFFECTIVE；显示“工作版本”“受控（待生效）”“受控（已生效）”；培训责任文案为文控，受控处理与生效事实分开。未修改操作权限。

## 验证证据

整合树 `doc/tasks/20261002-dcc-public-backend-completion/`：
- `g02-obsolete-readiness-red.log` 与 `g02-obsolete-readiness-red-refined.log` 保留失败；第二份使用完整原上传身份/锁定 Master 夹具，6 项中2错误/1失败，准确到达 BPM 轮次不一致门禁。
- `g02-obsolete-readiness-green.log`：4 类198执行，零失败/错误/跳过。
- `g02-obsolete-readiness-regression.log`：补跨文件/租户/阶段/正文/实际任务身份负向案例及签名证据回归，最终6类210执行，零失败/错误/跳过。新就绪度9项，原Workflow164、指派15、作废事务13、作废证据8、轮次签名1。
- 这些是服务/隔离H2测试；账号/BPM端口在预检测试中是明确替身，不宣称真实用户页面或运行库通过。

整合树 `doc/tasks/20261002-dcc-detail-integration/`：
- `g02-initial-lifecycle-red.log`：原9项通过，新2项失败（无明确 INITIAL 构造、无 WORKING 展示）。
- `g02-initial-lifecycle-green.log`：详情和三个既有工作流组件测试36项通过。
- `g02-revision-component.log`：既有升版组件18项通过。
- `g02-lint.log`：三个修改生产文件完整 ESLint exit0。
- `g02-types.log`：未携带正式项目8GB堆设置，Node默认4GB堆耗尽，进程7593 exit134；这是检查进程资源失败，非类型结论。
- `g02-types-8gb.log`：按 package.json 相同8GB堆和 tsconfig.relaxed.json 重新执行，进程1686 exit0，类型检查通过；未放宽规则。
- `g02-build-local.log`：正式 env.local 构建进程64769已核实exit0，日志Build successful。它覆盖G02源码；之后G03项目浏览/审核配置改动须另做最终构建，不能沿用此结果证明新源码。

## 迁移准备与影响边界

`g02-prepare-migration-package.py` 调用仓库正式 metadata/manifest/policy 实现，发现当前14项新增DCC迁移，递归依赖40项，拓扑排序且固定SHA256；`g02-migration-package.json` 策略门禁 passed。

这份依赖顺序不是让运行库重跑全部40项的命令清单。执行前必须只读核实实际库、既有迁移账本/哈希、表/索引/生成列与历史BPM版本，确定真正待执行项。旧依赖中可能包含历史初始化，不能不核对直接执行。失败同目标号重提及审核配置新迁移尚待后端交付，最终包需要重新生成并Review。

当前14项包含：生命周期/签名申请轮次字段和表；新增BPM定义版本及作废策略；项目默认属性/模板/逻辑目录/位置/保留草稿及审计意图；名称与版本精确唯一键/INITIAL候选角色键；关联、引用、整改与通知表及通知模板。BPM新定义和策略会影响之后的新流程；既有在途和历史证据须保留。生成列唯一性与新版返工身份必须在最终MySQL迁移合同再次复核；静态policy通过不能证明真实MySQL首次/重放执行成功。

未连接业务数据库，未执行任何迁移、业务SQL、BPM种子、候选审计策略激活或通知配置写入。

## 本机运行恢复方案

- 登记表核对：整合worktree profile=int_qms，slot6，前端8067，后端48067；原任务端口不动，48081不占用。
- 当前MySQL23306、Redis26379及整合服务端口均无监听；Docker Desktop引擎此前未启动。已有共享依赖启动权限问题等待用户答复，不将沉默当授权。
- 获准恢复共享依赖后，先列出现有容器及端口/归属，再只恢复确认匹配的本机测试依赖，不创建另一套库或导入假数据。
- 只读比较实际运行库与本迁移包，准备精确待执行清单/影响/备份和恢复步骤，再向用户报告需要决定的具体数据库事项。
- 源配置 `flowable.database-schema-update=true` 会自动DDL；任务服务必须显式关闭自动schema更新，在正式迁移准备完成之前不能靠启动服务暗中补表。Quartz/任务自动运行也先按本机正式规则核对，防止共享库任务被重复调度。
- 构建任务专属Jar后通过 `scripts/runtime/start-branch-backend.ps1` 和 `start-branch-frontend.ps1` 按slot6启动；启动前核对进程/端口归属、Jar来源、实际数据源和必要配置，不改共享端口配置。
- 真实E2E用AGENTS测试身份和任务自有数据；业务动作全部经Playwright真实页面；接口/DB仅只读辅助。账号密码不写入此交付文档。

提醒配置的正式代码字段为 `dcc.workflow.zone-id` 和 `dcc.workflow.reminder-lead-days`，缺值明确报错。具体业务值和正式通知渠道仍待确认或已有正式配置核实；不在代码中猜固定天数。项目审核人员配置入口由本批后端Owner继续实现，不借admin默认代替。

## 接续

backend_closure 已实际启动并回执，接公共全状态项目目录读模型、失败原目标号重提、后台审核人员配置；Root保留新作废修复后已停止并写后端。完整浏览/关联详情/上传部门选择/项目审核配置前端、运行E2E及最终合入仍未关闭。
