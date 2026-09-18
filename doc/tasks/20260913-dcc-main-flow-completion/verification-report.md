# DCC主流程闭环修复验证报告

## Bug and Scope

- 任务于2026-09-13开始，2026-09-14完成代码验证；工作区E:/IntRuoyi。
- 用户目标：重点让“项目与模板 → 上传工作稿 → 送审 → 审核会签 → 批准发布 → 浏览”主流程闭合。本轮修复017、019、021、030。
- 当前结论：这四项源码修复完成，423项后端回归、5项前端行为回归、13项历史静态合同与前端类型检查全部通过。
- 培训、分发和撤回重提不属于本轮修复目标。本轮未部署、未重启int_main服务、未运行真实页面E2E。

## Expected

- 有效配置能继续进入上传和审批，无效授权主体/重复环节提前拒绝且不破坏已保存配置。
- CAD工作稿及审批预览使用当前版本配套PDF；改图纸时绑定新PDF，仅改备注时自动复制内容不变的PDF，保持每个版本可独立授权预览。
- 送审先完成后，等待中的检出必须以当前状态拒绝；主流程从创建工作版本到送审始终引用同一个具体版本。
- 普通PDF/Office预览、源件签名摘要、已发布文件浏览以及访问Token/水印防护保持有效。

## Root Cause and Fix

| Bug | 根因 | 本次改动 |
|---|---|---|
| 017 | 只检查主体编号和时间，没有验证组织主体存在/启用 | 保存前调用正式User/Dept/Role/Post校验API，失败先于删除旧权限 |
| 019 | distinct后仅判断1—4，重复环节仍通过 | 保存与运行解析都要求恰好4条非空节点，再验证1—4唯一及固定策略 |
| 021 | CAD预览选源件，检入沿用旧PDF，元数据检入共享文件ID会触发归属歧义 | 授权先于文件元数据读取；CAD选择配套PDF；检入支持新PDF票据及内容校验/绑定；元数据检入复制PDF并校验哈希，重放核对绑定或副本内容 |
| 030 | 加锁前读出的状态一直用到写入 | Master锁后使用版本FOR UPDATE当前读，SQL同时限定可检出状态 |

- 图纸PDF上传加入请求归属检查，切换源件/关闭弹窗后，迟到结果不能覆盖新PDF或清除当前上传状态。
- 多个临时副本失败时逐一尝试清理，清理异常附加到原异常，不伪装成功。
- 更新了与当前规则不一致的旧测试夹具：完整四阶段、正确ANY/ALL策略、预览作用域依赖及独立发布失败服务。权限、Token和状态断言没有降低。

## Reproduction and TDD

- RED: 初始三个核心测试类158项中16项按预期失败：主体校验8、重复环节2、CAD预览5、检出竞态1。
- RED: CAD缺少新PDF仍能检入；前端缺PDF仍发请求、漏传PDF票据；新接口契约缺DTO字段/绑定回读方法，均先记录失败再实现。
- RED: PDF上传迟到响应会清除新源件PDF；元数据检入仍共用旧PDF，且同内容副本不能正常重放。已各自增加失败用例后修复。
- GREEN: 最终15个后端测试类423 tests、0 failures、0 errors、0 skipped；前端5项全部通过。各轮精确失败点、命令及夹具纠正记录见execution-log.md。

## Verification

后端命令在IntRuoyiBackend执行：

```powershell
mvn -pl yudao-module-dcc -am test "-Dtest=DccProjectAccessServiceImplTest,DccProjectFileTemplateServiceImplTest,DccApprovalRouteAdminServiceImplTest,DccControlledFileApprovalRouteAssigneeResolverTest,DccControlledFileQueryServiceTest,DccUploadTicketServiceTest,DccWorkingIterationSubmissionServiceTest,DccControlledFileWorkflowServiceImplTest,DccControlledFileFinalizationServiceImplTest,DccControlledFilePublishServiceTest,DccControlledFileSignatureEvidenceServiceTest,DccBusinessFileAccessProviderTest,DccControlledFilePreviewProtectionTest,DccAtomicPublishReplacementTest,DccControlledFileSourceOwnershipServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dstyle.color=never" -q
```

| 测试类 | 通过数 |
|---|---:|
| DccProjectAccessServiceImplTest | 17 |
| DccProjectFileTemplateServiceImplTest | 6 |
| DccApprovalRouteAdminServiceImplTest | 20 |
| DccControlledFileApprovalRouteAssigneeResolverTest | 6 |
| DccControlledFileQueryServiceTest | 130 |
| DccUploadTicketServiceTest | 24 |
| DccWorkingIterationSubmissionServiceTest | 7 |
| DccControlledFileWorkflowServiceImplTest | 136 |
| DccControlledFileFinalizationServiceImplTest | 38 |
| DccControlledFilePublishServiceTest | 9 |
| DccControlledFileSignatureEvidenceServiceTest | 5 |
| DccBusinessFileAccessProviderTest | 10 |
| DccControlledFilePreviewProtectionTest | 8 |
| DccAtomicPublishReplacementTest | 1 |
| DccControlledFileSourceOwnershipServiceTest | 6 |
| 合计 | 423 |

- GREEN: pnpm exec node --test src/views/dcc/controlled-file/browser/checkin-main-flow.spec.cjs -> PASS，5/5。直接执行实际页面提交/上传处理器及现有校验函数，网络端口为测试替身，未冒充E2E。
- GREEN: pnpm ts:check -> PASS，exit 0，使用最终前端源码。
- GREEN: 001/002/003/006/007/008/009/010/013/014/015/023/025共13个既有Node合同 -> PASS，最终Java调整后已复核。
- GREEN: 本次修改范围git diff --check -> PASS；仅有仓库预设LF/CRLF转换提示。
- GREEN: bug-regression-fix-loop证据校验器 -> PASS；文档结构、15类测试/423总数、17份源码指纹、代码行号、33条bug索引和4项状态、任务UTF-8/JSON校验 -> PASS。
- 尚待task-closeout-cleanup。

## Main-flow Coverage

| 主流程环节 | 证据及边界 |
|---|---|
| 项目/模板准备 | 正式授权主体校验17项、模板选择/保存6项；项目基础创建沿原实现静态复核 |
| 上传后创建工作版本再送审 | 新增mainFlowCreatesWorkingVersionThenSubmitsThatSameVersion串联调用实际服务，断言只创建一条A/1、送审绑定同一ID并生成4份环节快照 |
| 审核会签与批准 | 路线、工作版本提交、Workflow服务和签名证据回归；角色/签名不完整仍被拒绝，NEW与REVISION审批完成进入待发布 |
| 发布与版本替代 | 发布服务及Finalization回归验证待发布门禁、正式指针、旧版替代、批准时间保留和失败状态持久化 |
| 浏览/预览 | Query与受控访问/Token/水印回归，含普通文件和CAD配套PDF、未授权拒绝与版本来源 |

- 以上是代码及分层/串联服务测试证据。BPM、对象存储、组织API等外部端口在部分单测中使用替身；没有验证实际环境人员、路线、目录、签名图片、PDF或服务版本是否配置齐全。

## Code Evidence

- 正式主体校验：`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/projectcode/access/DccProjectAccessServiceImpl.java:173`。
- 唯一四环节：`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/route/DccFixedApprovalRoutePolicy.java:43`。
- 锁后当前读：`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java:463`。
- 新图纸/PDF绑定与元数据副本：`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java:568`。
- 绑定票据回读：`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/upload/DccUploadTicketServiceImpl.java:203`。
- 前端配套PDF入口与校验：`IntRuoyiFronted/src/views/dcc/controlled-file/browser/index.vue:983`。
- 上传工作稿到送审的串联回归：`IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileWorkflowServiceImplTest.java:580`。

## Source Fingerprints

| 本任务改动源码/测试文件 | 行数 | SHA-256 |
|---|---|---|
| `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/vo/DccControlledFileCheckinReqVO.java` | 19 | `a95eb47304d40cc8e4529375f7baea71f99dae29fb5d10f83cee92f19d30e695` |
| `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/mysql/file/DccControlledFileMapper.java` | 813 | `521b693a8b962fd4b870231cff2d0e454a9424e1cff3686a1df1ea19ba8fef87` |
| `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java` | 3320 | `10a98d3bc33de47d6767f76d96c664018aeaa9b5a6c5a4c0ee7d2e2a7588eef6` |
| `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/projectcode/access/DccProjectAccessServiceImpl.java` | 207 | `f920f7e5aa29c3c904b63574c44ad3737c861890b501109311fe5406d231e206` |
| `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/route/DccFixedApprovalRoutePolicy.java` | 96 | `e205423839a71f7b40df398f5b3b0cd3b52db265274e880ac788927d87667fb1` |
| `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/upload/DccUploadTicketService.java` | 26 | `7985db164060a03f9933eef8bf8e97dfdc2812ef8c1b2f4820ea538361ee172b` |
| `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/upload/DccUploadTicketServiceImpl.java` | 502 | `16154da54652e50055a27bb9de594f2542843a1e69f0a7f0ab70a7f1abe03151` |
| `IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccAtomicPublishReplacementTest.java` | 29 | `ffdbb40ab1d57820a50bfda963d947a434ae9171a3f5809827d2e95d31845d9d` |
| `IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFilePreviewProtectionTest.java` | 385 | `956a9ab2a452b19d22e3a0a5a2b8cd961b6497868ba289217741f87b3b4b72a1` |
| `IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceTest.java` | 4773 | `89673f2a4622ed911ef364f34493e83170c6414f593b382997eb21959a18e2d5` |
| `IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileWorkflowServiceImplTest.java` | 4533 | `b541c61300302fea16b902010c05392a25621bfaa52aaa73396e649ebf4d0f51` |
| `IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/projectcode/access/DccProjectAccessServiceImplTest.java` | 225 | `6706408beb95fd3ee95adc4db5d3e58f4782cc9dd56111d29b2433688b83db98` |
| `IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/route/DccApprovalRouteAdminServiceImplTest.java` | 589 | `ff2a1b8b7d19b6a93cea18440b3a555afb2afb89f9da5e2cc3eabcc7b59da485` |
| `IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/upload/DccUploadTicketServiceTest.java` | 547 | `a1a0a72850e10ff8c4b4bbaeddba0064a9637cdd8db6854f0625b623812512a3` |
| `IntRuoyiFronted/src/api/dcc/controlledFile/workflow.ts` | 2519 | `2af7a5384a5ba99ce43d10c069fb09e15ccd6b9aa179f4edb04b1a4f990739cd` |
| `IntRuoyiFronted/src/views/dcc/controlled-file/browser/checkin-main-flow.spec.cjs` | 130 | `f7820e689e88b788b101675ebbd6a42f952d0668edb73162e1f0d372c24874b2` |
| `IntRuoyiFronted/src/views/dcc/controlled-file/browser/index.vue` | 4115 | `39bbe3404a3733dd65e4802a41a300c177bb455ca83173f3aa6d353c9056c261` |

## Blockers, Risks and Closeout

- 无未解决的本轮源码或测试失败。真实页面E2E与部署NOT_RUN，按用户给定AGENTS须当轮明确授权，本次未执行。
- 不包含运行库迁移，本次没有新增数据库列；新检入PDF沿现有上传票据、文件ID和版本字段保存。
- 既有非法主体或重复路线今后保存/新送审会明确报错，需要按正式配置修正；不伪造负责人或补默认审批人。
- 既有历史数据修复、培训、分发、撤回重提及其它已登记问题未纳入本轮，不据此宣称全部33项已修复。
- 未获Git提交/推送授权，不执行；下级默认提交规则不扩张用户授权。
- 工作区已有其它改动，Git总diff不等于本任务全部贡献；仅沿本任务记录的文件和局部变更收尾，不回滚并行资产。
- 经验归入既有docs/backend-development.md，清理前先标记ready_for_closeout，仅保留当前任务核心记录。
