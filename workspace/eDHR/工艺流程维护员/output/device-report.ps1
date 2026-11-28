$ErrorActionPreference='Stop'
$stamp=Get-Date -Format yyyyMMddHHmmss
$dir=Join-Path $PSScriptRoot "设备主链$stamp"
$report=Join-Path $PSScriptRoot report
New-Item -ItemType Directory -Path $dir,$report -Force | Out-Null
$images=@{车间必填='1795762715315';车间基线='1795763242095';车间重复='1795763713019';车间取消='1795764410888';车间详情='1795764734811';设备查重='1795765560480';设备必填='1795765826277';设备创建='1795767903990';设备取消='1795768454634';修改预览='1795769357092';修改拒绝='1795769729567';工序缺少='1795770301555';设备重复='1795772965298';设备最终='1795773524555'}
foreach($key in $images.Keys){Copy-Item -LiteralPath "C:\Users\BJB110\.agent-browser\tmp\screenshots\screenshot-$($images[$key]).png" -Destination (Join-Path $dir "$key.png")}
$cases=@(
 @('已通过','车间01','新建前查重','查不到目标编码才能新增','重新登录恢复后完整列表4条，无WS-MN202611271323，下一页禁用；未把500空列表作为查重证据。','车间设置；清空筛选；核对全部记录和分页','车间基线'),
 @('已通过','车间02','必填验证','缺少编码和名称不能保存','空表单确定，显示车间编码不能为空、车间名称不能为空。','车间设置；新增；保持空表单；确定','车间必填'),
 @('已通过','车间03','新增及刷新详情','专用车间保存且状态开启','WS-MN202611271323，模拟主链202611271323设备车间，开启；新增后共5条，刷新和详情核对存在。','车间设置；新增；填本条编码名称；保持开启；确定；刷新；点编码','车间详情'),
 @('已通过','车间04','重复编码验证','重复不能保存','同编码、名称模拟主链202611271323重复验证提交，页面返回车间编码已存在；取消后5条。截图为表单，短暂错误提示已消失。','车间设置；新增；编码WS-MN202611271323；重复验证名称；确定；取消','车间重复'),
 @('已通过','车间05','取消编辑','取消不改变原名称','临时改名模拟主链202611271323取消不保存，取消并刷新后原名称保留。','车间设置；本次车间编辑；改名称；取消；刷新','车间取消'),
 @('已通过','车间06','只改备注保留其他字段','仅备注变更','备注保存为模拟主链202611271323修改保留字段验证；刷新详情，编码名称状态不变，面积和负责人仍空。','车间设置；本次车间编辑；只改备注；确定；刷新；点编码','车间详情'),
 @('已通过','设备01','按完整编码查重','创建前查不到','筛选设备编码EQ-MN202611271323并查询，显示暂无数据。','设备台账；新增筛选条件；设备编码；EQ-MN202611271323；查询','设备查重'),
 @('已通过','设备02','必填验证','缺少必填不能保存','空表单提交分别显示编码名称类型所属车间不能为空；默认状态停机。','设备台账；新增；保持空表单；确定','设备必填'),
 @('已通过','设备03','新增及车间产能状态','保存后资料准确','EQ-MN202611271323，模拟主链202611271323设备，模拟品牌，MN-1323，类型瑛泰设备，本次专用车间；模拟主数据标准小时产能120，停机。最终刷新详情一致。','设备台账；新增；填本条资料；类型瑛泰设备；车间WS-MN202611271323；产能120；保持停机；确定；刷新；点编码','设备最终'),
 @('已通过','设备04','取消编辑','取消后原名称保留','改为模拟主链202611271323取消不保存后取消，重新查询和最终详情仍为原名称，其他字段不变。','设备台账；本次设备编辑；改名称；取消；查询；点编码','设备最终'),
 @('已通过','设备05','重复设备编码','重复不能保存','同编码及本次重复验证名称，类型瑛泰设备、本次车间提交，页面错误设备编码已存在；取消刷新仍只有原对象。截图为表单，短暂错误提示已消失。','设备台账；新增；编码EQ-MN202611271323；本次重复验证名称；选择类型车间；确定；取消；刷新','设备重复'),
 @('未通过','设备06','修改及未改字段保留','备注保存，其他资料不变','只改备注，影响确认显示仅备注变更、受影响路线0条；填写模拟原因并确认提交，明确显示没有该操作权限。最终刷新详情备注仍为空，其他字段未变。不能算修改成功。','设备台账；本次设备编辑；备注填模拟主链202611271323只改备注，其他字段保留；点确定，不是工艺流程统一替换；影响确认填原因；确认并提交','修改拒绝'),
 @('存疑','关联01','专用工序及逐工序关联','专用工序可选并保存产能','lixin无工序新建菜单；设备编辑有维护工序产能入口，搜索模拟主链202611271323无匹配数据。未借用既有工序，未提交关联，设备工序明细为空。需有权限岗位先建专用工序。','设备台账；本次设备编辑；工序明细；维护工序产能；搜索模拟主链202611271323','工序缺少'),
 @('存疑','产能01','主数据产能后续修改','分别验证主数据和逐工序产能','新增主数据120已核实；编辑中主数据标准小时产能禁用，只有维护工序产能入口。未用其他入口代替，未声称主数据修改通过。','设备台账；本次设备编辑；查看主数据产能控件禁用及维护工序产能入口','工序缺少'),
 @('存疑','环境01','500及页面意外跳转','页面稳定','初始车间列表及未读消息500；正常退出重登lixin后恢复。一次新增填写重复设备编码时弹窗消失并转个人中心，原因未确认；重新进入后完成重复验证。截图仅为恢复列表，不是500画面。','初始进入车间设置；退出重登恢复；新增填编码时曾转个人中心，完整触发条件未知','车间基线')
)
$book=Join-Path $report "$stamp.xlsx"
officecli create $book | Out-Null
if($LASTEXITCODE -ne 0){throw '创建失败'}
$cmds=[System.Collections.Generic.List[object]]::new()
$cmds.Add(@{command='set';path='/Sheet1';props=@{name='已通过'}})
foreach($sheet in @('未通过','存疑')){$cmds.Add(@{command='add';path='/';type='sheet';props=@{name=$sheet}})}
$headers=@('用例编号','检查内容','应该出现的结果','实际结果','完整操作路径','证据截图')
foreach($sheet in @('已通过','未通过','存疑')){
 $cmds.Add(@{command='set';path="/$sheet";props=@{freeze='A2'}})
 $widths=@(12,25,28,58,62,90)
 for($col=0;$col -lt 6;$col++){
  $letter=[string][char](65+$col)
  $cmds.Add(@{command='set';path="/$sheet/col[$letter]";props=@{width=[string]$widths[$col]}})
  $cmds.Add(@{command='set';path="/$sheet/${letter}1";props=@{value=$headers[$col];'font.bold'='true';fill='155E63';'font.color'='FFFFFF'}})
 }
 $row=2
 foreach($case in $cases){
  if($case[0] -ne $sheet){continue}
  $values=@($case[1],$case[2],$case[3],$case[4],('本地localhost:8081，测试1，lixin；设备管理；'+$case[5]),'')
  for($col=0;$col -lt 6;$col++){
   $letter=[string][char](65+$col)
   $cmds.Add(@{command='set';path="/$sheet/$letter$row";props=@{value=[string]$values[$col];'alignment.wrapText'='true';'alignment.vertical'='top';'font.size'='11pt'}})
  }
  $cmds.Add(@{command='set';path="/$sheet/row[$row]";props=@{height='260'}})
  $cmds.Add(@{command='add';path="/$sheet";type='picture';props=@{src=(Join-Path $dir "$($case[6]).png");anchor="F${row}:G$($row+1)";alt=$case[2]}})
  $row++
 }
}
$batch=Join-Path $dir '报告生成记录.json'
$cmds | ConvertTo-Json -Depth 10 | Set-Content $batch -Encoding utf8
officecli batch $book --input $batch --json | Set-Content (Join-Path $dir '报告生成结果.json') -Encoding utf8
if($LASTEXITCODE -ne 0){throw '写入失败'}
officecli close $book
officecli validate $book
if($LASTEXITCODE -ne 0){throw '校验失败'}
$cases | ConvertTo-Json -Depth 5 | Set-Content (Join-Path $dir '用例原始记录.json') -Encoding utf8
Write-Output "REPORT=$book"
Write-Output "EVIDENCE=$dir"
