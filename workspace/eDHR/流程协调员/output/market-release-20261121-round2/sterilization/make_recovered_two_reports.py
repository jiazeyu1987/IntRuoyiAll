from pathlib import Path
import hashlib
import json

import fitz
from pypdf import PdfReader
from reportlab.lib import colors
from reportlab.lib.styles import ParagraphStyle
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer


folder = Path(__file__).resolve().parent
pdfmetrics.registerFont(TTFont('SimChinese', 'C:/Windows/Fonts/msyh.ttc', subfontIndex=0))
title = ParagraphStyle('Title', fontName='SimChinese', fontSize=19, leading=29, textColor=colors.HexColor('#A02020'), spaceAfter=16)
body = ParagraphStyle('Body', fontName='SimChinese', fontSize=11, leading=20, spaceAfter=12)
orders = [
    ('881MO100292', '1009200650', 'K202608015', '120', '900000001353', '3120', '344', '螺杆240与按钮120属于不同部件，最终大包装120；原超限+106.800；组装螺杆检验损耗1及整体不合格保留。'),
    ('881MO097127', '1009200651', '34126060002', '9823', '900000001354', '3125', '345', '粗洗生产损耗110、组装Ⅰ检验损耗1及整体不合格保留。'),
]
for order, active_id, batch, quantity, execution, task, application, exceptions in orders:
    target = folder / f'SIM-{active_id}-{order}-STERILIZATION-round2.pdf'
    if target.exists():
        raise FileExistsError(target)
    document = SimpleDocTemplate(str(target), pagesize=(595, 842), leftMargin=48, rightMargin=48, topMargin=40, bottomMargin=40)
    contents = [
        Paragraph('灭菌报告｜软件模拟附件', title),
        Paragraph('非真实实测／非真实质量批准', title),
        Paragraph(f'资料类别：灭菌报告<br/>生产订单：{order}<br/>活跃订单标识：{active_id}<br/>生产批号：{batch}<br/>计划数量：{quantity}件（非实际灭菌数量）<br/>批次执行：{execution}<br/>本人正式资料任务：{task}；PQC申请：{application}<br/>生产周期：2026-11-21；模拟轮次：20261121B 第二轮', body),
        Paragraph('本文件仅用于测试1租户已授权的软件模拟资料上传、保存及关联验证。不证明已经实际实施灭菌，不含真实灭菌参数、实测数据、合格结论、机构签章或真实质量批准。', body),
        Paragraph('异常披露（来源：本轮协调及PQC恢复办理回执）：' + exceptions + '本岗不修改原始记录，不认定异常已获真实处置批准。', body),
        Paragraph('结论：未作真实灭菌有效性、无菌或产品合格判定；不可作为真实生产、销售、临床使用或质量放行依据。资料上传不替代责任人审核、上市放行和归档。', body),
    ]
    document.build(contents)
    reader = PdfReader(target, strict=True)
    text = ''.join(''.join(page.extract_text().split()) for page in reader.pages)
    assert len(reader.pages) == 1
    for expected in [order, active_id, batch, execution, task, '软件模拟', '非真实', '整体不合格']:
        assert expected in text, expected
    with fitz.open(target) as rendered:
        rendered[0].get_pixmap(matrix=fitz.Matrix(1.2, 1.2)).save(target.with_suffix('.png'))
    print(json.dumps({'file': str(target), 'pages': len(reader.pages), 'bytes': target.stat().st_size, 'sha256': hashlib.sha256(target.read_bytes()).hexdigest()}, ensure_ascii=False))
