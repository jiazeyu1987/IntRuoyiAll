# G56 r3 — native记录阶段显示真实阶段证据

Status: ready_for_closeout_for_Root_review。Root真实R3/039管理页确认六阶段及线下training记录1/1，却看到training/control/distribution落通用“已完成，签名证据待同步/待当前节点签名”。Root唯一授权Parent formatStageSignatureStatus与stage-grid标签；不加签名规则、节点、字段、权限，审批/legacy原逻辑保留。

Given准确当前file+BPM/native scope，Whentraining/CONTROLLED/DISTRIBUTED证据显示，Then按正式trainingRecordAvailable、controlledTime、distributedTime表示线下记录待上传/已上传、受控记录待生成/已生成、文控下发待办理/记录已保存；缺scope/identity不能假事实。不写“下发不需要电子签名”未定决策。

GivenMATRIX/批准/文控审核或legacy真实四级，Then原signatureCount/等待/同步文字保持。本批BDD和实际formatter及grid子树有效RED→GREEN，有限native/round/dialog相关回归及Parentlint。Root最后FE完整types/build，后端包及服务不用因文案逻辑变化重启；Agent无DB/API/browser/Git/环境操作。

实际有效RED4项1PASS/3FAIL/exit1（原approval/legacy签名已PASS，其余旧factNode/缺scope/gridlabel失败）；GREEN与原native/round/dialog共4files31执行PASS/0fail/0skip/exit0，Parent ES lint --max-warnings 0 exit0、日志空(session59067终态)。只有Parent formatter加准确native/currentfile+BPM/read-context的fact-stage分支和grid标签“阶段证据”；不使用stage.isCompleted猜记录存在，training明确needTraining+availabilityboolean，control/distribution看正式时间。unknownscope/legacy/obsolete或上下文错不假fact；matrix/approve原签名方法原样。新r3pin只替代Parent且新增1测试，旧r2全部其它source/test/doc保持。Root后续完整FEtypes/build和真实可见证据另验。
