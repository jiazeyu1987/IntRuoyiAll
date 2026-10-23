# E2E-14 第三轮真实重跑

状态：业务本轮PASS，6/6已真实重跑，浏览器/context/runner均关闭，待Root review；工程blocked（Git/cleanup未授权）。授权来源为Root线程01a13ad1-10c0-72a3-b271-f192b6058114本轮放行；遵守rerun-four-rounds-20261020.md，只收集结果，不修产品。完整结果见rerun-20261020-round3-report.md及独立本次handoff。

目标：使用原U/S/L/C/A/D真实账号、独立本次文件编号与源文件名，UI新建立即生效申请，完成主管、会签指派、会签、批准及文控链；重新验证14-A取消、空密码/空意见不推进、14-B本人签名、14-C实际生效时间与签名一致（仅页面可见精度）、14-D日常目录当前已生效版与非空PDF。

Given 本轮int_qms运行环境8061/48061及任务原账号，When 从UI建立唯一新申请并逐人办理，Then 未完成文控签名不能生效，取消/空值不推进，签名后到达真实生效终态且日常可读。不得用历史PASS替代。

里程碑：规则/进程/运行Jar校验 → U本人新申请 → S/L/C/A本人办理 → D取消及空值分支、本人签名 → 终态/日常/PDF → 关闭全部context/browser/runner与交接。

约束：业务仅Playwright页面及DOM，不API/DB/响应JSON/mock；不改产品、公共配置、schema、服务、系统时钟、Git或Agent。不改变共享基线；若本例独占夹具必须变更，先UI记录原值且finally恢复回读。缺真实前置具体BLOCKED，真实失败先留证再继续可执行分支。历史证据不覆盖，新增证据仅本例独立目录。Git/cleanup未授权，不apply，不启动下一轮。
