# E2E-14 第二次完整重跑第三轮

状态：blocked，已完成本轮执行、只读核验与证据关闭检查，REVIEW_REQUIRED_OPEN_ANOMALIES；HOLD，不启动第四轮。runId为rerun-20261020-r2-round3，历史PASS不计本轮。D真实终态已确认，首次签名界面报错OPEN，U追加只读浏览失败；工程Git/cleanup未授权。见同名前缀report与handoff。

Given 本轮int_qms、统一新proof及原U/S/L/C/A/D真实账号，When UI建立新立即生效申请并完成主管、负责人指派、会签、批准、文控取消/重试/本人签名，Then 取消和空值不推进，签名后记录真实生效时间并到达日常受控终态，正文非空可读。适用分支14-A/B/C/D和空密码/空意见共6项。

里程碑：本地运行PID/Jar/proof/execute及runtimeReady门禁 → 本人登录、新申请单次提交 → 全前置审批 → D取消/空值/单次签名 → 新context终态及日常PDF → 证据隐私/CRC和全部浏览器/context/runner关闭回执 → HOLD。

约束：不API/DB/JSON oracle/mock，不改产品/共享配置/schema，不Maven/构建/重启/Git/子Agent/cleanup。14是立即生效，不使用5628、不设未来T，不推进旧文件；本次source名称与文件编号独立。首次失败独立保留，仅必要定位校准，不无限重跑。共享协调目录只读。
