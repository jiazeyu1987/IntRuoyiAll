from pathlib import Path
import fitz
from reportlab.lib.pagesizes import A4
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.cidfonts import UnicodeCIDFont
from reportlab.pdfgen import canvas

output_dir = Path(__file__).resolve().parent
pdfmetrics.registerFont(UnicodeCIDFont('STSong-Light'))
for revision in (1, 2):
    target_path = output_dir / f'dcc_flow_20261125_ab111cae_v{revision}.pdf'
    document = canvas.Canvas(str(target_path), pagesize=A4)
    document.setTitle(f'DCC TEST ab111cae revision {revision}')
    document.setFont('STSong-Light', 19)
    document.drawString(55, 780, '文控流程模拟测试文件 - 不用于真实业务')
    document.setFont('STSong-Light', 12)
    lines = [
        '测试标识：DCC-TEST-20261125-ab111cae',
        '指定环境：本地 localhost:8081 / 测试1租户',
        f'内容修订：{revision}',
        '用途：验证上传、审批、生效、下载及工作版本正式更新。',
        '本文件不包含真实产品参数，不构成真实质量批准。',
        '签名仅为本次获授权的测试身份验证代操作。',
        '版本1内容：模拟文件的初次上传与受控生效。'
        if revision == 1 else '版本2内容：已修改本段，用于检入与正式变更验证。',
        '未经真实业务审核，不得用于生产、检验或产品放行。',
    ]
    for line_index, line_text in enumerate(lines):
        document.drawString(55, 730 - line_index * 36, line_text)
    document.setFont('Helvetica', 10)
    document.drawString(55, 60, f'TEST ONLY | ab111cae | content revision {revision} | page 1 of 1')
    document.save()
    with fitz.open(target_path) as rendered_document:
        rendered_document[0].get_pixmap(matrix=fitz.Matrix(1.2, 1.2)).save(
            output_dir / f'dcc_flow_20261125_ab111cae_v{revision}.png'
        )
    print(target_path)
