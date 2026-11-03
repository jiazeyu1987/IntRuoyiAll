# 执行记录

- RED: 展厅6场景定向测试 -> 3条NeverWantedButInvoked行为失败（无任务/WAITING无RUNNING子项/仅子项），另3条因原实现未读取新增候选查询而出现严格Mockito UnnecessaryStubbing错误；未把它们概称6项行为失败。最小实现先读取已有未完成任务，无任务返回，仅真实RUNNING任务/子项调用原恢复，无异常吞掉或配置更改。

- 用户明确批准展厅封面批任务同类启动空写入修复。BDD: Given无候选，When启动恢复，Then仅查询不UPDATE；Given WAITING无RUNNING子项，Then也不空写；Given真实RUNNING任务/子项，Then原恢复并传播失败。先RED再GREEN，不改产品配置/媒体或业务行。

- 后续DCC Java实现提交3bf63eee8ebefefa9ee48441c434bea481a5ac71，仅5文件，hook菜单与端口守卫PASS；未合并/推送。测试服原程序显式恢复及真实登录验收PASS，等待DCC以外展厅同类问题的范围确认后形成新发布目标；未宣称后端补丁→全量完成。

- GREEN/REGRESSION: DCC识别/NAS传输/受控审计/启动包装及调度条件共118测试PASS，0失败/错误/跳过。仅5个DCC Java实现和回归文件准备提交；真实发布尚未重新执行，不将单测冒称只读启动验收。

- RED: NAS传输及受控审计两项真实Maven无工作测试 -> 2测试2失败，均精确验证到不应发送的UPDATE，0错误/跳过。
- 实际DESCRIBE及只读计数：三服NAS传输任务、子项、受控审计RUNNING均0。最小实现沿用未完成任务查询/租户过滤，补齐实际RUNNING候选再写；不修改调度器或启动配置。

- BDD: NAS启动无恢复工作 -> Given无未完成传输任务/无RUNNING审计任务，When启动恢复，Then不发UPDATE；WAITING传输无RUNNING子项也不写；存在RUNNING任务/子项保留原恢复及异常传播。
- 真实补丁2026-11-02-23-56-34测试只读启动被NAS传输无条件UPDATE阻止；失败闭锁保留，正式/备份未变，维护仓原任务JOB-740d813a0b。

- BDD: 无恢复工作 -> Given终态占用0且RUNNING任务为空，When启动恢复，Then不发送DELETE/UPDATE；有实际工作保留恢复语义、写入失败原样抛出。
- RED: Maven -pl yudao-module-dcc -am，DccControlledFileBatchRecognitionServiceTest#recoverInterruptedTasksOnStartupDoesNotWriteWhenNoWorkExists -> FAIL，1测试明确验证到不应调用的DELETE。
- GREEN/REGRESSION: Maven同模块及依赖，DccControlledFileBatchRecognitionServiceTest,DccControlledFileBatchRecognitionStartupRecoveryTest -> PASS，47测试，0失败/跳过。
- 首次回归新增测试缺少assertSame导入/使用未导入doThrow，testCompile失败；修复测试编译后实际回归通过，不以编译错误充当行为RED。
- 实现提交da3851751451ea75ce82f4ecb9e0364000505909，仅3个Java文件；日志提交e5934b12e为祖先。hook菜单/端口门禁通过。当前文档未进入补丁源码提交，避免超出Java-only合同；最终统一收口后独立提交记录。
- 发布必须新建已提交目标的干净临时worktree。当前开发副本只用于开发与单测，不复制target给发布器；真实Spug及远端证据见维护仓主任务，尚未宣称发布成功。

- GREEN/REGRESSION: Maven -pl yudao-module-showroom -am -Dtest=ShowroomProductCoverStartupRecoveryTest,ShowroomProductCoverBatchTaskServiceTest -Dsurefire.failIfNoSpecifiedTests=false test -> PASS；新增6项、既有9项，共15项，0失败/错误/跳过，BUILD SUCCESS。无候选不写、真实恢复保留、异常继续传播。

## Quartz startup same-class repair
用户已批准同类修复，实际Quartz无条件删除重建导致JOB-6aa7b1fac9失败。原程序JOB-1746423a99恢复成功，JAR/schema/ledger/readOnly0/0及真实登录/GxP关闭PASS。实施范围：任务定义及状态一致仅查询；有差异仍原同步；错误传播。正式服auto-startup有效值待核对，不关闭调度器。BDD见维护仓quartz-startup-noop.md。

- GREEN/REGRESSION: Maven Quartz definition/startup + existing infra/maintenance/job tests -> PASS，63项，0失败/错误/跳过；真实内存Quartz保留原start/next fire时间。当前未修改三服Quartz配置或放宽只读保护。

# 最终发布及收尾
已完成后端补丁2026-11-03-10-33-18三服发布及验收，随后同源码全量2026-11-03-12-15-42按测试→正式→备份闭环完成。三服实际JAR/前后端镜像相同、885张表及13段结构一致、各自迁移历史保持，旧补丁程序挂载为空。真实Playwright维护/开放登录、版本变更说明、本地及三服GxP关闭、健康UP/HTTP200通过，正式Quartz原有15秒心跳推进。清理最终JOB-763953cdce三服成功，原失败保持；合计删除旧文件36638007347字节（约34.12GiB），四组证据不足旧资料整组保留。全部本轮构建副本已删除，未产生构建提交；稳定普通执行器PID14264路径已恢复、无服务/活动任务/远端锁、数据库临时保护0/0，私密配置保持。实现先提交，最终记录独立提交；不自动合并/推送int_main。
