$ErrorActionPreference='Stop'
$root=(Get-Location).Path
$destination=Join-Path $root 'output\流程说明书-20261125'
$sources=Get-Content (Join-Path $destination '截图来源清单.json') -Raw | ConvertFrom-Json
function Paragraph([string]$text,[hashtable]$extra=@{}) {
 $props=@{text=$text;font='微软雅黑';size='11pt';color='#222222';spaceAfter='6pt';lineSpacing='1.25x';keepLines='true'}
 foreach($key in $extra.Keys){$props[$key]=$extra[$key]}
 $script:commands.Add(@{command='add';parent='/body';type='paragraph';props=$props});$script:paragraphNumber++
}
function Heading([string]$text,[bool]$newPage=$true){Paragraph $text @{style='Heading1';size='14pt';bold='true';color='#17365D';spaceBefore='4pt';spaceAfter='8pt';keepNext='true';pageBreakBefore=$newPage.ToString().ToLower()}}
function Step([string]$text){Paragraph $text}
function Picture([int]$number,[string]$caption,[double]$width=16.8){
 $imagePath=Join-Path (Join-Path $root 'output') $sources[$number-1]
 Add-Type -AssemblyName System.Drawing
 $bitmap=[System.Drawing.Image]::FromFile($imagePath);$height=$width*$bitmap.Height/$bitmap.Width;$bitmap.Dispose()
 Paragraph '' @{align='center';spaceAfter='3pt';keepNext='true'}
 $script:commands.Add(@{command='add';parent="/body/p[$script:paragraphNumber]";type='picture';props=@{src=$imagePath;width=('{0:0.###}cm' -f $width);height=('{0:0.###}cm' -f $height);alt=$caption}})
 Paragraph $caption @{size='9pt';color='#595959';spaceAfter='8pt'}
}
function Start-Manual([string]$title,[string]$scope){
 $script:commands=[System.Collections.Generic.List[object]]::new();$script:paragraphNumber=0
 $script:commands.Add(@{command='set';path='/section[1]';props=@{pageWidth='21cm';pageHeight='29.7cm';marginTop='2cm';marginBottom='2cm';marginLeft='2cm';marginRight='2cm'}})
 Paragraph $title @{style='Title';size='18pt';bold='true';color='#17365D';spaceAfter='5pt';keepNext='true'}
 Paragraph '适用：eDHR业务操作｜编写日期：2026-11-25｜版本：V1' @{size='9pt';color='#595959'}
 Paragraph $scope
 Paragraph '截图为2026-11-23—25的历史测试界面，包含不同租户、订单及周期，仅说明操作位置，不是同一订单的连续记录，也不代表本次重新实测。实际办理以当前租户、本人正式任务和页面要求为准。' @{size='9pt';color='#595959'}
}
function Save-Manual([string]$name,[string]$footer){
 $file=Join-Path $destination ($name+'.docx')
 $script:commands.Add(@{command='add';parent='/';type='footer';props=@{text=($footer+' V1 · ');field='page';align='center';font='微软雅黑';size='9pt';color='#17365D'}})
 $batch=Join-Path $destination ($name+'.batch.json')
 ConvertTo-Json -InputObject $script:commands.ToArray() -Depth 12 | Set-Content -LiteralPath $batch -Encoding utf8
 & officecli create $file --locale zh-CN
 if($LASTEXITCODE -ne 0){throw '创建失败'}
 & officecli batch $file --input $batch | Out-File (Join-Path $destination ($name+'.制作日志.txt')) -Encoding utf8
 if($LASTEXITCODE -ne 0){throw ('写入失败：'+$name)}
 & officecli close $file
 if($LASTEXITCODE -ne 0){throw '保存失败'}
}
. (Join-Path $PSScriptRoot '正文-01.ps1')
. (Join-Path $PSScriptRoot '正文-02.ps1')
. (Join-Path $PSScriptRoot '正文-03.ps1')
