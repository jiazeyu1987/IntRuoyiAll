# G43 槽位6开发运行与维护入口最短路径

2026-10-04；状态：READONLY_CHECKLIST_FOR_ROOT_EXECUTION_NOT_STARTED。用户已取消本机开发阶段质量批准前置，主任务 `g43-development-scope.json` 为依据；本报告不恢复该前置、不补造质量批准记录。Java Owner正在改开发维护门禁，配置 Owner准备准确26操作。Root仍是唯一实际配置/启动/登录/维护执行人。本 Agent没有数据库、浏览器、HTTP、服务、Maven、Git或源码操作，只新增本文。

## 已存在的运行材料

| 材料 | 当前可用事实 |
|---|---|
| 整合运行目录 | `C:/IntRuoyi/20261001-dcc-integration` |
| 正式端口登记表 | `C:/IntRuoyiAll-int_main/.git/intrruoyi-runtime/worktree-ports.json`，精确匹配active entry：int_qms/slot6、frontend8067、backend48067、当前整合分支 |
| 正式后端入口 | 整合目录 `scripts/runtime/start-branch-backend.ps1`；它按登记解析slot，仅用已存在server Jar，不带 `-Build` 时不构建；默认local profile及登记端口，追加 `ExtraArgs` |
| 正式前端入口 | 整合目录 `scripts/runtime/start-branch-frontend.ps1`；slot6生成8067、后端/proxy48067并使用strictPort；不改共享.env |
| 已审安全运行覆盖清单 | 主任务 `g21-runtime-plan.json`；来源于G18/G19实际审查，固定Flowable自动DDL禁用、RAMQuartz、独立scheduler名、关闭startup sync及无关workers、Shanghai/7天。它是计划JSON，不是已经生效配置 |
| 秘密运行配置 | 既有正式本机 `IntRuoyiBackend/script/deploy/restart-int-ruoyi-local.ps1` 与当前本机容器配置是既定来源。本次未找到已生成且已审的本任务专属 `.properties` 文件；不能写成“G18 properties已准备并加载”。不运行main/fullrestart以取得配置 |

后端脚本本身不自动注入本任务23306/26379数据源和DCC签名密钥；直接只用 `-Slot 6` 会沿资源默认值，不足以证明运行前置。Root应从已确认既有配置在任务进程内安全注入所需属性；只记录名称、非空状态/版本和文件哈希，禁止完整配置、密码或密钥原值进入argv、console、日志/本报告。`dcc.signature.evidence.hmac-secret`、`key-version` 由正式 `DccSignatureEvidenceProperties#validateStartupConfig` 强制非空；不新造key、不把未知历史签名版本映射为当前key。OnlyOffice正式配置前缀为 `yudao.dcc.preview.onlyoffice`，base-url/jwt-secret/public-file-base-url按既有来源核对，不能把缺正文依赖改成假成功。

## 最短次序

1. Root先完成本轮准确26个开发操作配置、schema/39原文/历史scope manifest等已授权前置，取得Java Owner冻结的开发维护代码与最新同源Jar。当前读取到的旧 `DccLegacyMaintenanceGate`/command仍含质量资料调用；这是Owner正在修改的源码，不是应继续请求用户批准的理由。运行必须使用修改验证后的版本。
2. 在正式整合工作目录启动普通 `local` 后端和8067前端，维护enabled保持false；数据库为既有源测试库23306，Redis26379。先确认实际48067 PID/health与Jar、有效runtime覆盖，再让真实页面登录。不要从主目录脚本错误解析slot6，不停止48081或其他线程进程。
3. 使用AGENTS确认的租户/现有账号经真实LoginForm自然提交；仅从这一浏览器自然登录响应在内存取得真实accessToken。停普通后端时仅处理已证明属于slot6的PID；保留真实token，**不要先退出登录/调用logout**，因为其可能注销服务端access记录。
4. 以同slot配置启动一次 `local,local-maintenance` 维护进程，明确enabled=true，保护根/请求文件/SHA来自Root已生成的完整清单，把该token只经stdin送入。不能把token放ExtraArgs、环境值日志、临时文件、storageState、HAR或trace；本机维护动作不是通过token直接代办文件上传/批准等业务。
5. 维护runner只在实际登记返回有效scope/event/sequence/hash收据后输出安全JSON并close应用context；失败保留精确状态、只读核实际效果，不能自动重试。Root随后恢复普通local服务，通过真实页面完成主线。维护期间和最初登录期间都关闭非任务自动writer；需要正式每分钟生效job时，后续由真实任务页面单独按实际due范围启用，不能拿最初禁调度启动冒称生效任务已验。

## 非秘密启动参数边界

下面为安全参数骨架，不包含任何秘密、不声称可以忽略数据源/签名配置。Root使用既有backend脚本的 `-Slot 6 -ExtraArgs <这些参数及安全配置引用>`，不使用 `-Build` 重复打包。显式设置 `INTRUOYI_RUNTIME_PROFILE=int_qms`，保留registry归属检查。

```text
--flowable.database-schema-update=false
--spring.quartz.auto-startup=false
--spring.quartz.job-store-type=memory
--spring.quartz.scheduler-name=dcc-integration-slot6
--spring.quartz.properties.org.quartz.scheduler.instanceName=dcc-integration-slot6
--spring.quartz.properties.org.quartz.jobStore.class=org.quartz.simpl.RAMJobStore
--spring.quartz.properties.org.quartz.jobStore.isClustered=false
--yudao.local-job-control.startup-sync-enabled=false
--yudao.local-job-control.dcc-batch-recognition-enabled=false
--yudao.local-job-control.dcc-nas-transfer-enabled=false
--yudao.local-job-control.dcc-nas-permission-restore-enabled=false
--yudao.local-job-control.dcc-nas-control-audit-enabled=false
--yudao.local-job-control.dcc-upload-temporary-cleanup-enabled=false
--yudao.local-job-control.showroom-release-auto-publish-enabled=false
--yudao.local-job-control.showroom-product-cover-batch-resume-enabled=false
--yudao.local-job-control.showroom-product-batch-narration-audio-auto-check-enabled=false
--yudao.local-job-control.showroom-product-batch-narration-script-auto-check-enabled=false
--dcc.workflow.zone-id=Asia/Shanghai
--dcc.workflow.reminder-lead-days=7
```

只设置Quartz auto-startup=false不够：`SchedulerManager#isEnabled`只看scheduler非null，startup sync会写调度注册；新正式 `JobStartupSyncRunner` 以startup-sync-enabled=false关闭bean。批识别startup recovery与temporary cleanup均有独立ConditionalOnProperty，需要上述false；本机resource默认Quartz为auto=true/JDBC，必须核实际覆盖RAM/disabled。这些flag并非证明全系统永远无writer，Root沿既有preflight核本次有效启动和任务归属。

维护阶段另加以下非秘密参数（路径和SHA必须为Root实际材料，不写伪造占位文件）：

```text
--spring.profiles.active=local,local-maintenance
--yudao.dcc.legacy-registration-maintenance.enabled=true
--yudao.dcc.legacy-registration-maintenance.protected-root=<实际保护根>
--yudao.dcc.legacy-registration-maintenance.request-file=<实际请求文件>
--yudao.dcc.legacy-registration-maintenance.request-sha256=<该文件实际SHA256>
--mybatis-plus.configuration.log-impl=org.apache.ibatis.logging.slf4j.Slf4jImpl
```

当前runner profile限定local-maintenance且排除prod/production/backup/test/unit-test；它用System.in，不是HTTP新业务入口。生产Java门禁另由Owner按用户本机开发授权修改；不要用任意正式profile加载开发豁免。

令牌入口正式防日志门禁还要求下列 logger/其子logger有效级别OFF，MyBatis accessToken mapper实际statementLog必须Slf4jImpl。Root在**普通登录与维护启动两次**都使用相同安全log覆盖；不要仅关闭根logger后保留显式DEBUG子logger：

```text
cn.iocoder.yudao.module.system.service.oauth2
cn.iocoder.yudao.module.system.dal.mysql.oauth2
cn.iocoder.yudao.module.system.dal.redis.oauth2
org.springframework.data.redis
io.lettuce.core.protocol
org.mybatis.spring
org.apache.ibatis
org.springframework.jdbc.core
```

通过 `--logging.level.<上述名称>=OFF` 配置即可表达非秘密覆盖；以实际运行有效配置为准，不把文档名单当已关闭证明。Root如准备受保护配置引用，只输出引用路径和其受控哈希，不读回含secret正文。

## 真实登录与维护账号条件

`LoginForm#completeLogin` 调用自然 `/system/auth/login` 响应并 `setToken`；维护需要的是真实accessToken，不能直接API登录、create token、读取库里已有token、传refreshToken或伪造admin LoginUser。Playwright可以等待此页面自然POST响应，解析返回的accessToken只赋内存变量，既不tool text输出也不write文件；登录阶段不启动trace/HAR/网络正文日志、不保存storageState，密码填入仅在该会话内，之后清除。真实业务阶段照常页面操作并保留安全证据，不能用已取token调用接口代替页面动作。

AuthAdapter检查access记录真实存在、正式checkAccessToken结果、scope/身份/expiry与当前数据库行一致，tenant1、ADMIN类型、启用账号及用户名/昵称一致；refresh兼容、撤销/过期token和tenant-ignore均拒绝。内部注册service另明确 `doc_control` 精确角色及 `dcc:controlled-file:update` 权限。能以admin登录或具有超管permission，不证明有doc_control；后者没有super_admin旁路。

stdin按token原始UTF-8 bytes一次写入并关闭管道，**不加换行**；command的有界readNBytes需要EOF，AuthAdapter禁止控制字符。普通 `echo token` 会同时暴露token或附加换行，不适用。使用Root已有进程启动/内存管道方式，既不打印进程stdin也不把令牌留在脚本参数文本中。

本次已有安全材料没有admin到doc_control的实际active绑定证明，G18/G21 schema/BPM收据也不承担这个事实。Root可用已有安全SQL/真实账号权限页核ID/username/role code/status和所需permission；本 Agent不执行DB。若当前账号不符，准确报告缺实际角色/权限，不复制相似角色、不猜密码、不直接更改账号授权。本目标已有本机依赖/任务服务授权，不新增泛permission询问。

正式页面主线的项目reviewer、部门leader、实际会签人、批准人仍来源实际配置/岗位/签名readiness，不能因开发取消质量批准就省略业务签名、借admin假候选或预设mock成功。业务账号最小前置详见现有 `g34-runtime-account-readiness.md`，本报告不扩负向矩阵。

## 本次读取限制

只读定位标准restart参数时有一次rg误匹配password行，工具输出越出允许字段范围；已停止该读取方式并告知Root，未复制原值到报告/任务资产，随后仅提取property/env名称。本次没有访问真实凭据或使用这些值。没有重复GET原件、没有写质量批准行、没有启用调度或获取实际token。

本文只证明最短路径的实际代码和现有登记材料可审查；secret注入/26配置/开发Java最终freeze、实际账号身份、runtime health、维护收据与真实页面主线均仍由Root以当前事实执行验证。
