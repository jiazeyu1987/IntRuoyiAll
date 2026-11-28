from pathlib import Path
from reportlab.pdfgen import canvas
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont

output = Path(__file__).parent
pdfmetrics.registerFont(TTFont('Hei', 'C:/Windows/Fonts/simhei.ttf'))
lines = [
    '模拟资料，仅系统测试，非真实注册证或监管批准',
    '测试批次 20261128011419，国内变更说明与批准材料',
    '公司及持证主体 上海七木医疗器械有限公司',
    '项目 RCN20261128011419',
    'DCC产品编码 SIMCN20261128011419，保持不变',
    '原产品名称 软件模拟测试专用国内产品',
    '本次仅变更名称为 软件模拟测试专用国内产品模拟变更',
    '证号 SIM-REG-20261128011419-CN，分类一类',
    '首次获证日期 2026-11-27，生效日期 2026-11-28',
    '本次批准日期 2026-11-28，有效期仍为 2027-01-20',
    '委托生产 否，自行生产 是，其余未变更字段保持原值',
]
document = canvas.Canvas(str(output / 'CN-change-matched.pdf'))
document.setFont('Hei', 11)
for index, text in enumerate(lines):
    document.drawString(30, 800 - index * 30, text)
document.save()
print('已生成国内变更模拟材料，未改变原有效期')
