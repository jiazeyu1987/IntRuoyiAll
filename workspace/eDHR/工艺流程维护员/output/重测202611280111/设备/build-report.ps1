$ErrorActionPreference='Stop'
$root=$PSScriptRoot
$rows=@'
已通过|车间编码查重|新增前本轮编码不存在|列表5条全部可见，没有WS-MN202611280111|车间设置，检查全部5条及分页|1795800276230
已通过|车间必填拦截|缺少编码名称不能保存|编码和名称均提示不能为空，表单未关闭|车间设置，新增，留空，确定|1795800487699
已通过|车间新增及刷新|专用车间成功保存|WS-MN202611280111，模拟主链202611280111设备车间，开启。刷新详情一致|车间设置，新增，填写本轮编码名称及模拟备注，保存，刷新，详情|1795801336455
已通过|车间取消编辑|取消不保存|临时备注取消，刷新后仍为模拟测试MN202611280111原始备注|车间设置，本轮车间编辑，只改备注，取消，刷新|1795801029102
已通过|车间修改保留字段|仅备注变化|备注为模拟测试MN202611280111修改保留字段验证，编码名称开启状态保留，面积负责人仍为空|车间设置，本轮车间编辑，只改备注，确定，刷新，详情|1795801336455
已通过|车间重复编码|阻止重复新增|页面返回车间编码已存在，取消后总数仍6条。截图是提交后表单，提示原文见原始结果|车间设置，新增，编码WS-MN202611280111，名称模拟重复车间不应新建，确定，取消|1795801700286
已通过|设备编码查重|新增前本轮编码不存在|按完整编码EQ-MN202611280111查询，暂无数据|设备台账，新增筛选条件，设备编码，输入EQ-MN202611280111，查询|1795802142979
已通过|设备必填拦截|缺项不能保存|编码名称类型车间均提示不能为空，默认停机未改|设备台账，新增，留空，确定|1795802323548
已通过|设备新增及刷新详情|专用设备关联专用车间|EQ-MN202611280111，模拟主链202611280111设备，模拟品牌，MN-0111，瑛泰设备，本轮专用车间，产能120，停机，工序为空。刷新详情一致|设备台账，新增，填写本轮资料，选择瑛泰设备及本轮车间，产能120，保存，刷新，详情|1795803387058
已通过|设备取消编辑|取消不保存|备注临时修改并取消，刷新重开仍为模拟测试MN202611280111原始备注，其他字段保留|设备台账，本轮设备编辑，只改备注为模拟测试取消不应保存，取消，刷新，编辑|1795803796432
已通过|重测只改备注零影响提交|备注保存且其他字段不变|影响确认只列备注，受影响路线0条。填写模拟原因确认提交成功；刷新详情备注为模拟测试MN202611280111只改备注验证，其他字段保留，未出现上轮没有该操作权限|设备台账，本轮设备编辑，只改备注，确定，影响确认0条，填写原因模拟测试只修改备注验证设备维护权限及原字段保留，确认并提交，刷新，详情|1795804594032
已通过|设备重复编码|阻止重复新增|页面返回设备编码已存在，取消刷新后同编码仅1条，总数仍60。截图为提交后表单，提示原文见原始结果|设备台账，新增，编码EQ-MN202611280111，名称模拟重复设备不应创建，类型瑛泰设备，车间本轮车间，确定，取消，刷新|1795805686631
已通过|逐工序产能正式入口|可打开维护入口|可打开维护设备工序产能，包含工序、单台标准小时产能、维护说明。仅查看并取消，没有保存关联|设备台账，本轮设备编辑，工序明细，维护工序产能，查看，取消|1795806558950
存疑|工序维护入口及创建未执行|有正式入口维护本轮工序|侧栏仅个人中心审批中心设备管理，菜单搜索工序无数据，未找到维护入口。不代表系统没有功能；未借旧工序、未加权|设备台账，顶部菜单搜索工序，显示无数据。需工序岗位提供本轮专用工序|1795806001246
存疑|设备工序关联双端核验未执行|专用工序关联后双端核对|按任务要求待本轮专用工序具备后续做，尚未收到编号。未保存关联，工序端工作站端及逐工序产能未核验，不判通过|设备台账，本轮设备编辑，工序明细，维护工序产能。待提供本轮专用工序编号，选择该工序填写模拟产能说明，保存刷新，再核对工序端|1795806558950
'@
$cases=@($rows.Trim() -split "`n" | ForEach-Object { $values=$_.Trim() -split '\|'; @{sheet=$values[0];name=$values[1];expected=$values[2];actual=$values[3];path=('本地localhost:8081，测试1，李鑫，设备管理，'+$values[4]);image=$values[5]} })
foreach($case in $cases){Copy-Item -LiteralPath "C:\Users\BJB110\.agent-browser\tmp\screenshots\screenshot-$($case.image).png" -Destination (Join-Path $root "$($case.image).png")}
Copy-Item -LiteralPath 'C:\Users\BJB110\.agent-browser\tmp\screenshots\screenshot-1795803956351.png' -Destination (Join-Path $root 'impact-zero.png')
$cases | ConvertTo-Json -Depth 5 | Set-Content -Encoding utf8 (Join-Path $root 'cases.json')
$report=Join-Path (Resolve-Path (Join-Path $root '..\..\report')) ((Get-Date -Format yyyyMMddHHmmss)+'.xlsx')
officecli create $report --locale zh-CN | Out-Null
if($LASTEXITCODE -ne 0){throw '创建失败'}
$operations=[System.Collections.Generic.List[object]]::new()
$operations.Add(@{command='set';path='/Sheet1';props=@{name='已通过'}})
foreach($sheet in @('未通过','存疑')){$operations.Add(@{command='add';parent='/';type='sheet';props=@{name=$sheet}})}
foreach($sheet in @('已通过','未通过','存疑')){
 $operations.Add(@{command='set';path="/$sheet";props=@{freeze='A2';gridlines='false';zoom='70'}})
 $headers=@('用例','预期结果','实际结果','完整测试路径','证据截图')
 foreach($index in 0..4){$column=[string][char](65+$index);$width=@(28,32,65,72,92)[$index];$operations.Add(@{command='set';path="/$sheet/${column}1";props=@{value=$headers[$index];bold='true';fill='16324F';'font.color'='FFFFFF'}});$operations.Add(@{command='set';path="/$sheet/col[$column]";props=@{width="$width"}})}
 $rowNumber=2
 foreach($case in @($cases | Where-Object sheet -EQ $sheet)){
  $values=@($case.name,$case.expected,$case.actual,$case.path,'')
  foreach($index in 0..4){$column=[string][char](65+$index);$operations.Add(@{command='set';path="/$sheet/$column$rowNumber";props=@{value=$values[$index];wrapText='true';'alignment.vertical'='top';size='11'}})}
  $operations.Add(@{command='set';path="/$sheet/row[$rowNumber]";props=@{height='270'}})
  $nextRow=$rowNumber+1
  $operations.Add(@{command='add';parent="/$sheet";type='picture';props=@{src=(Join-Path $root "$($case.image).png");anchor="E${rowNumber}:F${nextRow}";alt=$case.name}})
  $rowNumber++
 }
 if($rowNumber -eq 2){$operations.Add(@{command='set';path="/$sheet/A2";props=@{value='本轮独立范围未发现未通过项；未执行见存疑表，不代表完整主链通过。';wrapText='true'}})}
}
$batch=Join-Path $root 'report-batch.json'
$operations | ConvertTo-Json -Depth 8 | Set-Content -Encoding utf8 $batch
officecli batch $report --input $batch --json | Set-Content -Encoding utf8 (Join-Path $root 'officecli-result.json')
if($LASTEXITCODE -ne 0){throw '填充失败'}
officecli validate $report | Set-Content -Encoding utf8 (Join-Path $root 'officecli-validate.txt')
if($LASTEXITCODE -ne 0){throw '校验失败'}
$report | Set-Content -Encoding utf8 (Join-Path $root 'report-path.txt')
Write-Output $report
Write-Output '13项通过，0项未通过，2项存疑'
