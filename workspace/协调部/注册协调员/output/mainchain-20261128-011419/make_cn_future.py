from pathlib import Path
from reportlab.pdfgen import canvas
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont

output = Path(__file__).parent
pdfmetrics.registerFont(TTFont('Hei', 'C:/Windows/Fonts/simhei.ttf'))
lines = [
    '模拟资料，仅系统测试，非真实注册证或监管批准',
    '测试批次 20261128011419，国内未来生效延续材料',
    '公司及持证主体 上海七木医疗器械有限公司',
    '项目 RCN20261128011419，DCC产品编码 SIMCN20261128011419',
    '证载产品名称 软件模拟测试专用国内产品模拟变更',
    '证号 SIM-REG-20261128011419-CN，分类一类',
    '首次获证日期 2026-11-27，批准日期 2026-11-28',
    '本次生效日期 2026-11-29，有效期至 2029-01-20',
    '委托生产 否，自行生产 是，其他内容不变',
]
document = canvas.Canvas(str(output / 'CN-future-matched.pdf'))
document.setFont('Hei', 11)
for index, text in enumerate(lines):
    document.drawString(30, 800 - index * 30, text)
document.save()
print('已生成匹配当前证载名称的未来延续模拟材料')
