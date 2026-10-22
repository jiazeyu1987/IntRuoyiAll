# E2E-18 升版申请与历史附件验证

业务验收 PASS：局部变更、换版变更共2个参数均完成真实页面闭环。最新核对见latest-acceptance-review.json；完整原始页面事实见root-integrated-result.json。2026-10-19本轮仅核对已有证据、修正过期记录，没有重新提交、检入、绑定培训或签名。

| 参数 | 来源受控版 | 选用草稿 | 目标终态 | 流程 |
|---|---|---|---|---|
| PARTIAL 局部变更 | A/1，4232 | 4246 | A/2，4252，受控已生效 | f3d5e4b9-cb41-11f1-856e-b082e25ec548 |
| REPLACEMENT 换版变更 | A/2，4252 | 4255 | B/1，4256，受控已生效 | 1415e10e-cb56-11f1-856e-b082e25ec548 |

表中文件ID为完整ID的末4位；完整身份：A/1=2054545668044084232、局部草稿=2054545668044084246、A/2=2054545668044084252、换版草稿=2054545668044084255、B/1=2054545668044084256。同一逻辑文件编号DCC241016C18PARTIAL，原项目DCC241016C18P1。

## 实际通过范围

- 两参数均经U编制、S主管、L指派、C会签、A批准、D文控及U本版培训记录完成；确认页与详情展示实际变更类型、精确来源受控版、选用草稿及独立说明，未停在提交或候选状态。
- 文件basename保持DCC241016C18-PARTIAL-OLD-BODY.pdf；不同正文由内容标记、画布、版本身份及来源ID证明。A/1、A/2、B/1逐版实际预览，对侧版本标记无匹配。
- 各版2份普通附件和1份培训记录逐个实际打开预览；文件名、内容标记、用途、上传人、时间及对应BPM均核对，旧版证据不串入新版。草稿显示为“草稿”。
- 原始业务证据运行SHA为3b2de8ff5852f181eed80c8a3cb8c6d2bd810f4da29d3f09b9f9467213f8a9fd，不用后续运行身份覆盖历史。
- 本轮结构核对PASS：17张截图文件存在且PNG可解析；两阶段28份trace索引可读，脱敏报告PASS且networkPayloadsRetained=false。结构核对不冒充新E2E或新的逐页视觉复验。

## R28后续结论

R28已由Root真实页面复核通过，依据../20261016-dcc-24-e2e-coordination/e2e28-reading-close-root-review.json。验证在E2E-22原文件2054545668044084258的C/A/D本人签名后完成：瞬时错误通知和closeConsoleFailures为空，reading-session/stop自然POST返回200。先前GET、预检通知等错误诊断及E2E-18原失败证据均保留，当前不再列“R28待复验”。本例没有重放已成功签名来制造复验样本。

## 验证与收尾边界

- 本轮未运行Maven、构建、服务操作、API/SQL、Git或新的业务E2E；Query与QueryTest写权仍归Root指定的23，本例未改生产。
- 当前dispatch曾处于RESTORING_AFTER_HOST_REBOOT/runtimeVerified=false，因此本轮不启动浏览器，不将环境恢复计作业务失败，也不重开已通过参数。
- 工程状态blocked：当轮未授权Git提交/推送，尚未完成cleanup apply。保留任务证据及自有业务对象供Root复核；不得标记completed或自动删除。清理preview的实际结果见closeout-preview-20261019.txt。
- 旧verification-report全文保存在verification-report-before-final-status-20261019.md，仅作历史；其中prepare_only、4246未提交和R28未关闭等状态已被本报告及最新证据取代。