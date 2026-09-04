# 注册证上传 E2E 验证报告

## 本轮结论

- 权限补齐：PASS。已通过前端给注册部经理角色 `dcc_registration_certificate_approver` 勾选 `注册证上传审批` 权限，并确认 `chudongchuan` 重新登录后具备该角色和权限。
- 后端重建：PASS。`mvn.cmd -pl yudao-server -am -DskipTests clean package` 通过；E2E 期间 `48097` 由当前 worktree 的 `yudao-server-exec.jar` 进程提供，验证后已停止。
- E2E-1 上传人提交首证上传审批：PASS。
- E2E-2 上传后状态正确：FAIL。审批通过前当前列表查不到目标、上传人待办不出现目标均符合预期；但上传人“我发起”按注册证编号仍查不到目标。
- E2E-3 注册经理待办审批：FAIL。`chudongchuan` 能从审批中心待办按注册证编号找到目标并打开审批详情，但提交审批时后端返回 `1080000151`，提示 DCC 电子签名图片缺失或未启用。
- 后续 E2E-4/E2E-6/E2E-7/E2E-8/E2E-9：BLOCKED，原因是注册经理审批未完成，不能继续验证入库、生产方式回显和下载授权链路。

## 关键数据

- 环境：`D:\IntRuoyiWorktree\20260904-dcc-upload-related-files-e2e-worktree`
- 前端：`http://127.0.0.1:8097`
- 后端：`http://127.0.0.1:48097`
- 测试注册证编号：`E2E-UPLOAD-20260904070732-SELF`
- 上传人：`wanglixuan`
- 注册经理：`chudongchuan`
- 上传文件：`e2e_test/registration/upload/upload_file.pdf`

## 失败场景分析

### E2E-2 我发起按注册证号搜不到

现象：上传接口成功返回后，当前注册证列表按编号查询 total=0，说明审批前未入正式列表；上传人待办不包含目标，说明没有把审批任务派回上传人。这两项符合预期。但进入上传人“我发起”后按注册证编号搜索，页面不显示目标审批。

代码原因：上传业务已经给 BPM 创建请求设置了标题 `注册证上传审批 + 注册证号`，位置在 `DccRegistrationCertificateApprovalService`。但是 BPM 流程创建完成后，`BpmProcessInstanceServiceImpl.processProcessInstanceCreated()` 的 afterCommit 又调用 `generateProcessInstanceName(...)`，并执行 `runtimeService.setProcessInstanceName(...)`，把创建时传入的标题覆盖回流程定义模板生成的标题。审批中心“我发起”查询使用 `processInstanceNameLike` 搜流程实例名称，所以注册证号被覆盖掉后就搜不到。

通俗讲：提交时已经把单子标题写成“注册证上传审批 + 编号”，但流程启动后的自动改名又把编号擦掉了，所以“我发起”按编号搜不到。

### E2E-3 注册经理审批提交失败

现象：后端重建重启后，`chudongchuan` 已能在审批中心待办中按注册证编号找到目标，并打开审批详情；点击确认审核后，请求 HTTP 200，但业务码返回 `1080000151`，提示 DCC 电子签名图片缺失或未启用。

代码原因：审批中心提交审核时，`ApprovalCenterServiceImpl.reviewTask()` 会先校验签名密码，再调用 `signatureRecordService.recordReviewSignature(...)` 生成签名记录。这个签名记录依赖 `ApprovalSignatureRecordServiceImpl` 调用 DCC 的 `requireActiveSnapshot(loginUserId)`。而 `DccElectronicSignatureImageServiceImpl.requireActiveSnapshot()` 要求当前审批人存在启用状态的电子签名图片；没有就抛出 `CONTROLLED_FILE_SIGNATURE_IMAGE_MISSING`，对应业务码 `1080000151`。

通俗讲：密码是对的，人也有审批权限，单子也看到了；但系统要求审批时必须盖上这个人的电子签名图片，当前账号没有可用签名图，所以审批被拦住。

## 证据文件

- `artifacts/registration-upload-ui-only-e2e-result.json`
- `artifacts/self-production-upload-before-submit.png`
- `artifacts/self-production-upload-after-submit.png`
- `artifacts/self-production-current-before-approval.png`
- `artifacts/self-production-approval-detail.png`
- `artifacts/self-production-approval-after.png`

## 2026-09-04 继续修复后的最终结论

- 修复后最终前端 E2E：PASS。
- 结果文件：`artifacts/upload-front-only-20260904082602.json`
- 测试注册证编号：`E2E-UPLOAD-20260904082602`
- 覆盖结果：
  - PREP：`chudongchuan` 电子签名图片已启用，PASS。
  - E2E-1：`wanglixuan` 通过真实前端上传注册证并提交审批，HTTP 200 / code 0，PASS。
  - E2E-3：`chudongchuan` 通过真实前端审批中心待办按注册证编号找到目标任务，PASS。
  - E2E-3-approve：`chudongchuan` 通过真实前端完成电子签名审核，HTTP 200 / code 0，PASS。
  - E2E-4：`wanglixuan` 通过真实前端在当前注册证列表按编号查到已入库 CURRENT 记录，PASS。

## 本轮新增失败场景分析

### 前端仍提示“User has no enabled company scope”

现象：上传弹框前，当前注册证列表请求返回业务码 `1081001002`，页面随后出现公司/项目候选加载异常。

根因：不是前端操作问题，也不是账号权限没配好；是可执行 jar 里嵌入的 DCC 模块还是旧字节码。源码已把注册证列表改成不依赖授权公司，但之前只打包了 server，没有先安装 DCC 模块，导致运行包仍从本地 Maven 仓库拿旧的 DCC jar。

通俗讲：代码文件看起来已经改了，但真正跑起来的后端包还是旧的，所以页面还按老规矩查“授权公司”。

处理：先安装 `yudao-module-dcc`，再重新打包 `yudao-server`，并抽取可执行 jar 验证 `scopedCompanyIds()` 已经只返回空列表，不再调用授权公司接口。

### 审批通过时报“未配置注册证提醒任务”

现象：上传成功、经理待办可见、电子签名也可用；但经理点击确认审核后，审批接口返回业务码 `1080000275`，提示 `未配置注册证提醒任务`。

根因：注册证业务事件通知还在读取旧的 Quartz 任务参数 `registrationCertificateReminderDailyJob.handler_param`，要求里面有 `roleIds` 和 `permission`。但新的迁移已经把提醒接收人迁到租户级配置表 `dcc_registration_certificate_reminder_config.threshold_recipient_user_ids_json`，并把 job 参数改成只保留 `actorId`。代码和数据库迁移口径不一致，导致审批正式化后的通知阶段失败并回滚审批。

通俗讲：审批本身已经过了，卡在“审批成功后给谁发通知”。数据库已经换了新通讯录，代码还去旧通讯录找人，找不到就报错。

处理：业务事件通知改为读取新的租户级提醒接收人配置，并按明确用户列表发送，同时保留通知失败继续明确失败的规则；没有通过吞异常或跳过通知来冒充成功。

## 本轮验证命令

- `mvn.cmd -pl yudao-module-dcc -Dtest=DccRegistrationCertificateUploadServiceTest#listOwnerCompaniesReturnsTenantOwnedCandidatesWithoutCompanyScope test` -> PASS
- `mvn.cmd -pl yudao-module-dcc -Dtest=DccRegistrationCertificateQueryServiceTest#pageListsTenantCurrentCertificatesWithoutCompanyScopeAndAuditsReturnedObjects test` -> PASS
- `mvn.cmd -pl yudao-module-dcc -Dtest=DccRegistrationCertificateBusinessEventNotificationConfigServiceTest test` -> PASS
- `mvn.cmd -pl yudao-module-dcc -Dtest=DccRegistrationCertificateBusinessEventNotificationTest test` -> PASS
- `mvn.cmd -pl yudao-module-dcc -DskipTests install` -> PASS
- `mvn.cmd -pl yudao-server -DskipTests package` -> PASS
- 前端 Playwright E2E `upload-front-only.cjs` -> PASS

## 2026-09-04 完整上传验收复跑结论

最终完整脚本结果文件：`artifacts/registration-upload-ui-only-e2e-result.json`，run key `20260904084729`。

| 用例 | 最终结果 | 证据摘要 |
| --- | --- | --- |
| E2E-1 上传人提交首证上传审批 | PASS | 自产 `E2E-UPLOAD-20260904084729-SELF`、委托 `E2E-UPLOAD-20260904084729-ENTR` 均由真实前端提交，上传请求 HTTP 200 / code 0。 |
| E2E-2 上传后状态正确 | PASS | 审批前当前列表 total=0；上传人待办无目标行；上传人“我发起”有目标行。 |
| E2E-3 注册经理待办审批 | PASS | `chudongchuan` 从真实审批中心待办按注册证编号找到目标并审核通过，review HTTP 200 / code 0。 |
| E2E-4 审批后注册证入库 | PASS | 审批后详情页显示目标注册证，并能看到上传文件。 |
| E2E-6 合法生产方式入库展示 | PASS | 自产和委托两组详情生产方式均按上传表单值回显。 |
| E2E-7 注册部经理直接下载附件 | PASS | 注册经理在详情页点击下载，本次自然下载请求 HTTP 200。 |
| E2E-8 普通用户申请下载并 3 天内下载 | BLOCKED | 缺少可通过前端确认并登录的同租户普通用户 C 凭据；不能用 API/DB 或其它账号替代。 |
| E2E-9 超过 3 天后重新申请下载 | BLOCKED | 依赖 E2E-8 授权通过，且需要自然超过 3 天或产品认可的业务日期推进入口；当前不允许直接改库/API 推进时间。 |

### 完整复跑中额外发现的脚本问题

旧 `registration-upload-ui-only-e2e.mjs` 不是产品失败，而是脚本落后于当前前端：它把关键词拼到 URL，并把筛选框里显示的关键词当成表格行证据；详情页也没有等待数据加载完成，截图停在骨架屏。已改成真实 `TableMultiFilter` 控件路径、等待带关键词的自然分页请求、按表格行断言，并等待详情页出现目标注册证编号后再截图。

### 仍需业务补充的前置条件

要继续 E2E-8/E2E-9，需要提供或确认一个同租户普通用户 C：能登录、能进入注册证详情、没有注册证文件直接下载特权，并有下载申请入口。E2E-9 还需要自然等待超过 3 天，或提供产品认可的业务日期推进入口；否则按当前“只能前端操作”约束无法验证过期重申。

## 2026-09-04 最新完整复跑结论

最终完整脚本结果文件：`artifacts/registration-upload-ui-only-e2e-result.json`，run key `20260904101823`。

| 用例 | 最新结果 | 证据摘要 |
| --- | --- | --- |
| E2E-1 上传人提交首证上传审批 | PASS | 自产 `E2E-UPLOAD-20260904101823-SELF`、委托 `E2E-UPLOAD-20260904101823-ENTR` 均由真实前端提交，上传请求 HTTP 200 / code 0。 |
| E2E-2 上传后状态正确 | PASS | 审批前当前列表 total=0；上传人待办无目标行；上传人“我发起”有目标行。 |
| E2E-3 注册经理待办审批 | PASS | `chudongchuan` 从真实审批中心待办按注册证编号找到目标并审核通过，review HTTP 200 / code 0。 |
| E2E-4 审批后注册证入库 | PASS | 审批后详情页显示目标注册证，并能看到上传文件。 |
| E2E-6 合法生产方式入库展示 | PASS | 自产和委托两组详情生产方式均按上传表单值回显。 |
| E2E-7 注册部经理直接下载附件 | PASS | 注册经理在详情页点击下载，本次自然下载请求 HTTP 200。 |
| E2E-8 普通用户申请下载并 3 天内下载 | BLOCKED | 缺少可通过前端确认并登录的同租户普通用户 C 凭据；不能用 API/DB 或其它账号替代。 |
| E2E-9 超过 3 天后重新申请下载 | BLOCKED | 依赖 E2E-8 授权通过，且需要自然超过 3 天或产品认可的业务日期推进入口；当前不允许直接改库/API 推进时间。 |

### 最新脚本稳定性修复

- 当前注册证列表的 `详细` 按钮改为按真实可见按钮定位，避免固定操作列 DOM 导致误判。
- 审批中心 `审核` 按钮改为使用页面稳定属性 `data-approval-action="review"`。
- 审批搜索只把审批表格内容作为命中证据，不再把筛选输入框里的关键词误算成目标行。
- 上传人待办这种预期不存在的负向检查只做短等待；经理待办和我发起这种预期存在的正向检查才重试。
