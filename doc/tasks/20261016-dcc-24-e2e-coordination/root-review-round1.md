# Root首轮脚本Review

当前为静态Review，不是真实E2E结果。24个线程均交付prepared.json及独立脚本；Root于本次检查亲跑24份node --check，24/24语法通过。之后线程可能按反馈继续修改，最终运行前须重新核对指纹。

## E2E-11 — 要求返修

Root静态review E2E-11需要返修（不是真实E2E FAIL）：supervisorPage.tracing不存在，tracing属于BrowserContext；应在登录完成后context.tracing.start，签名密码填入阶段暂停/脱敏trace，不能把凭据存进trace。当前取消只断言弹窗关闭，须刷新同一主管待办并确认仍在主管节点、会签负责人未提前收到任务；通过后需验证实际签名主体/时间/申请和负责人指派待办，不能用任意会签人直接出现代替部门负责人指派。finally目前仅把cleanupAttempted=true但无清理动作，属于不实回执，须实际页面清理/恢复或明确残留与未尝试。源码入口/按钮还需对照真实组件，审批中心review不应猜为通用弹框。修改仅本例脚本资产，node --check不算E2E通过，保持prepare_only，交Root重新review。

## E2E-13 — 要求返修

Root静态review E2E-13需返修：两个场景目前均openApproval(EXPECTED_PREFIX).first，可能重复选同一申请；必须分别绑定两份精确文件编号/版本/待办，并核对默认U及选中O。需要保证真实页面已推进到培训记录单阶段后再判断U可上传/O不可上传，不能批准刚结束就要求培训待办。缺电子签名trace/screenshot与真实页面收尾；不要凭弹框关闭即批准PASS，核对本次实际负责人及签名事实、无多余负责人节点。脚本无清理动作时必须明确residual及cleanupAttempted=false。仅改本例脚本、保持prepare_only；前端角色授权仍待用户答复。

## E2E-24 — 要求返修

Root静态review E2E-24需返修：格式允许只传一个任意item就总PASS；必须与prepared六种格式逐项比对并列未覆盖，不让小集合代表全格式。assertOffice仅iframe存在不能正文ready/内容已加载，需真实frame正文标记和只读事实；PDF需验证正文标记、翻页实际内容变化，当前只查canvas且搜索excerpt存在不够。失焦计时仅assert秒数不回退不能证明停止计时；需可见页面累计值在失焦期间不增长/恢复后增长，统计明确误差边界。重开不变目前仅画布尺寸，需可辨识正文事实。登录/签名trace避免凭据，监控request URL不能保存token查询原值，只记录path/method/status。BLOCKED整体状态应非0并明确未覆盖。只改本例资产，保持prepare_only，重新node --check后交Rootreview。

## E2E-20 — 要求返修

Root静态review E2E-20：第93行等待protected-preview-error/reading-error隐藏后catch空吞异常，会把真实预览错误忽略；请保留具体断言失败，只有明确预期拒绝分支单独断言拒绝。核对完整入口和旧页撤权后行为，不改生产/不做权限写入；准备中保持prepare_only。

## E2E-03 — 要求返修

Root静态review E2E-03：存在confirmation隐藏、截图、tracing.stop、browser.close的多个catch空吞异常。请分别保留业务断言错误、证据采集错误与cleanupError，不能静默忽略或覆盖原失败；截图/trace缺失不能当完整验收，上传失败/保存失败分支若无真实触发保持BLOCKED。保持prepare_only，不改生产或调用API补数据。

## E2E-16 — 要求返修

Root静态review E2E-16：finally contexts.close().catch空吞异常，需记录每个context收口错误及残留/清理结果，不能无声丢失。保持prepare_only；培训及签名阶段必须跟精确申请/版本/原作者，不用owner替代。

## 共享环境

用户已明确授权更新并重启int_qms。完整reactor package实际exit0；新Jar来源绑定关键类存在。初次启动未完整复用原环境，随后恢复使用已有g56-local-runtime.py加载器及原矩阵签名key/定义转换，均只在进程内加载。新包仍实际暴露Delayed服务@Resource按字段名误注入infra FileMapper，已交E2E-15线程真实Spring注入测试，Root独占Maven RED/GREEN。正式E2E业务执行仍未放行。

## 待review范围

其余18例只有语法/独立归属/准备资产检查，不代表完整脚本审查通过；后续继续逐例核对覆盖、真实入口、trace凭据保护和清理。
