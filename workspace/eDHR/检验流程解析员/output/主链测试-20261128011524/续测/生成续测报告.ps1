$ErrorActionPreference='Stop'
$run=$PSScriptRoot
$previous=Split-Path $run -Parent
$cases=@(Get-Content -LiteralPath (Join-Path $previous '本次测试记录.json') -Raw -Encoding utf8|ConvertFrom-Json)
$screenshots=Join-Path $run '截图'
New-Item -ItemType Directory -Path $screenshots,(Join-Path $run 'report') -Force|Out-Null
foreach($case in $cases){
 Copy-Item -LiteralPath (Join-Path $previous "截图\$($case.Image)") -Destination (Join-Path $screenshots $case.Image) -Force
 $case.Limit='上轮已核验结果沿用，本次未重跑；原始证据日期为2026年11月28日。'
}
function Set-Evidence($case,$name){
 Copy-Item -LiteralPath "C:\Users\BJB110\.agent-browser\tmp\screenshots\screenshot-$name.png" -Destination (Join-Path $screenshots "$name.png") -Force
 $case.Image="$name.png"
 $case.Limit='本次接续复测的实际页面结果；没有重新导入、保存或发布规程。'
}
$history=$cases|Where-Object Node -eq '历史设备显示缺失'
$history.Actual='接续复测仍未通过：ID历史G/11八处设备仍提示待绑定；刷新后当前G/14的八处设备编号正常。撤压项历史未显示C01017。没有改写历史记录，尚不能认定底层记录丢失。'
Set-Evidence $history '1795870070407'
$name=$cases|Where-Object Node -eq '旧关联名称随新版变化'
$name.Actual='接续复测仍存疑：ID历史G/13的大中包装关联仍锁定A/8，但名称仍带本轮1128升版。名称是否应锁定历史版本缺少明确业务口径，不能判定已修复或合格。'
Set-Evidence $name '1795870524085'
$reset=$cases|Where-Object Node -eq '无引用目标成功清空分支'
$reset.Actual='本次再次确认重置后仍未执行：页面明确显示生产订单引用二十六条、PQC任务引用三百九十九条，未删除任何规程或版本。成功清空分支仍缺少无引用目标，不能判定通过或失败。'
$reset.Path='本地 http://localhost:8081，测试1租户，panxin账号，eDHR批记录，检验表单，检验规程，ID当前G/14，测试重置，确认重置，查看引用拒绝提示。需要由业务负责人明确获授权且无引用的目标，才可继续验证成功清空；不删除已有订单和PQC引用。'
Set-Evidence $reset '1795870858813'
$protection=$cases|Where-Object Node -eq '重置引用保护'
$protection.Actual='本次接续复核：二十六条生产订单及三百九十九条PQC任务引用时明确拒绝重置；提示本次未删除规程或版本。关闭提示再刷新，当前仍ID G/14。此项仅证明引用保护生效，不代表成功清空已通过。'
Set-Evidence $protection '1795870858813'
$cases|ConvertTo-Json -Depth 6|Set-Content -LiteralPath (Join-Path $run '本次测试记录.json') -Encoding utf8
$generator=Get-Content -LiteralPath (Join-Path $previous '生成报告.ps1') -Raw -Encoding utf8
$tail=$generator.Substring($generator.IndexOf('$commands=[System.Collections.Generic.List[object]]::new()'))
$tail=$tail.Replace('2026年11月28日本地主链复测，测试1，潘鑫。','2026年11月28日本地主链接续，测试1，潘鑫。沿用原轮已通过结果，仅复核遗留节点。')
& ([scriptblock]::Create($tail))
