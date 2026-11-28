from pathlib import Path
from reportlab.pdfgen import canvas
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.cidfonts import UnicodeCIDFont
import fitz

output = Path(__file__).parent
pdfmetrics.registerFont(UnicodeCIDFont('STSong-Light'))
orders = [
    ('881MO101355', '1009200654', 'SIM20261121B0654', '900000001357', '2020'),
    ('881MO102665', '1009200653', '2609052', '900000001356', '520'),
]
for order, active, batch, execution, quantity in orders:
    filename = f'SIM-R2-{active}-{order}-来料检验报告-软件模拟.pdf'
    document = canvas.Canvas(str(output / filename), pagesize=(595, 842))
    document.setTitle(f'软件模拟来料检验报告 - {order}')
    document.setFillColorRGB(0.75, 0.05, 0.05)
    document.setFont('STSong-Light', 21)
    document.drawString(42, 790, '软件模拟 / 非真实报告')
    document.setFillColorRGB(0, 0, 0)
    document.setFont('STSong-Light', 17)
    document.drawString(42, 752, '第二轮流程测试 - 来料检验报告')
    lines = [
        '用途：仅验证来料资料任务的文件上传、保存及关联流程。',
        '环境：localhost:8081 / 测试1；资料类别：来料检验报告。',
        f'生产订单：{order}',
        f'活跃实例：{active}',
        f'生产批号：{batch}',
        f'批次执行：{execution}；计划数量：{quantity}',
        '本附件不是物料实测报告，不载入虚构检验值或合格判定。',
        '来料实测、检验标准及质量结论：本模拟附件不提供。',
        '不代表真人亲签、真实实测、质量批准或上市放行。',
        '不得用于实际质量或上市依据；无真实签章。',
        '本轮新制测试附件，不复用前轮文件或签章。',
    ]
    if order == '881MO102665':
        lines += ['保留既有检验损耗1及整体不合格，不作改判或处置批准。']
    lines += ['软件模拟，非真实实测 / 质量批准。']
    document.setFont('STSong-Light', 12)
    for index, text in enumerate(lines):
        document.drawString(42, 705 - index * 31, text)
    document.save()
    pdf = fitz.open(output / filename)
    assert len(pdf) == 1
    assert order in pdf[0].get_text()
    pdf[0].get_pixmap(matrix=fitz.Matrix(1.2, 1.2)).save(output / f'{order}-来料报告预览.png')
    print(filename, 'valid one-page PDF')
