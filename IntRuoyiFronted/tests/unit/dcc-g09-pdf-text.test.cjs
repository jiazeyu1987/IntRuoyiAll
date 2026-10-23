const test = require('node:test')
const assert = require('node:assert/strict')
const { pathToFileURL } = require('node:url')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const viewerRoot = path.resolve(__dirname, '../../src/views/dcc/controlled-file/view')
function tools() {
  const c = { exports: {}, Error }
  vm.runInNewContext(ts.transpileModule(fs.readFileSync(path.join(viewerRoot, 'pdf-tools.ts'), 'utf8'), {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  }).outputText, c)
  return c.exports
}

// Valid, task-owned PDF bytes with genuine page text or embedded raster scan pages.
function pdfFixture(texts) {
  const objects = ['', '<< /Type /Catalog /Pages 2 0 R >>', '', '<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>']
  const pageIds = []
  for (const text of texts) {
    const page = objects.length
    pageIds.push(page)
    const raster = text === null
    objects.push(`<< /Type /Page /Parent 2 0 R /MediaBox [0 0 400 600] /Resources << /Font << /F1 3 0 R >> ${raster ? `/XObject << /Im1 ${page + 2} 0 R >>` : ''} >> /Contents ${page + 1} 0 R >>`)
    const stream = raster ? 'q 350 0 0 550 20 20 cm /Im1 Do Q' : text ? `BT /F1 12 Tf 30 540 Td (${text}) Tj ET` : '0.8 g 20 20 350 550 re f'
    objects.push(`<< /Length ${Buffer.byteLength(stream)} >>\nstream\n${stream}\nendstream`)
    if (raster) {
      const image = 'ffffff000000000000ffffff>'
      objects.push(`<< /Type /XObject /Subtype /Image /Width 2 /Height 2 /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /ASCIIHexDecode /Length ${image.length} >>\nstream\n${image}\nendstream`)
    }
  }
  objects[2] = `<< /Type /Pages /Kids [${pageIds.map(id => id + ' 0 R').join(' ')}] /Count ${texts.length} >>`
  let output = '%PDF-1.4\n'
  const offsets = [0]
  for (let id = 1; id < objects.length; id++) {
    offsets.push(Buffer.byteLength(output))
    output += `${id} 0 obj\n${objects[id]}\nendobj\n`
  }
  const xref = Buffer.byteLength(output)
  output += `xref\n0 ${objects.length}\n0000000000 65535 f \n`
  for (const offset of offsets.slice(1)) output += `${String(offset).padStart(10, '0')} 00000 n \n`
  output += `trailer\n<< /Size ${objects.length} /Root 1 0 R >>\nstartxref\n${xref}\n%%EOF\n`
  return new Uint8Array(Buffer.from(output))
}

async function parsePdf(bytes) {
  assert.ok(process.env.G09_CANVAS_MODULE, 'set G09_CANVAS_MODULE to the installed real @napi-rs/canvas module')
  const { DOMMatrix, ImageData, Path2D } = require(process.env.G09_CANVAS_MODULE)
  Object.assign(globalThis, { DOMMatrix, ImageData, Path2D })
  const pdf = await import(pathToFileURL(path.join(viewerRoot, 'vendor/pdf.min.mjs')).href)
  pdf.GlobalWorkerOptions.workerSrc = pathToFileURL(path.join(viewerRoot, 'vendor/pdf.worker.mjs')).href
  return pdf.getDocument({ data: bytes.slice(), useSystemFonts: true, isEvalSupported: false, stopAtErrors: true }).promise
}

test('genuine PDF.js text extraction finds multiple page occurrences without modifying the PDF', async () => {
  const { extractPdfTextPages, findPdfTextMatches } = tools()
  const bytes = pdfFixture(['First controlled paragraph', 'Second CONTROLLED and controlled', 'End'])
  const original = Buffer.from(bytes)
  const document = await parsePdf(bytes)
  try {
    const pages = await extractPdfTextPages(document, () => true)
    assert.equal(pages.length, 3)
    assert.match(pages[0], /First controlled paragraph/)
    const matches = findPdfTextMatches(pages, ' controlled ')
    assert.deepEqual(Array.from(matches, x => x.pageNumber), [1, 2, 2])
    assert.equal(Buffer.compare(Buffer.from(bytes), original), 0)
  } finally { await document.destroy() }
})

test('genuine non-text PDF has empty text pages and no fabricated watermark matches', async () => {
  const { extractPdfTextPages, findPdfTextMatches } = tools()
  const document = await parsePdf(pdfFixture([null, null]))
  try {
    const pages = await extractPdfTextPages(document, () => true)
    assert.equal(pages.every(page => page === ''), true)
    assert.equal(findPdfTextMatches(pages, '受控').length, 0)
  } finally { await document.destroy() }
})

test('text extraction observes the active generation after each await and surfaces genuine failures', async () => {
  const { extractPdfTextPages, findPdfTextMatches } = tools()
  let current = true, calls = 0
  const switched = await extractPdfTextPages({ numPages: 2, getPage: async () => {
    calls++
    return { getTextContent: async () => { current = false; return { items: [{ str: 'old-version' }] } }, cleanup() {} }
  } }, () => current)
  assert.equal(switched, null)
  assert.equal(calls, 1)
  await assert.rejects(extractPdfTextPages({ numPages: 1, getPage: async () => ({
    getTextContent: async () => { throw new Error('PDF_TEXT_READ_FAILED') }, cleanup() {}
  }) }, () => true), /PDF_TEXT_READ_FAILED/)
  assert.deepEqual(Array.from(findPdfTextMatches(['split\n text  受控'], 'split text'), x => x.pageNumber), [1])
  assert.equal(findPdfTextMatches(['literal a+b [x]'], 'a+b').length, 1)
  assert.equal(findPdfTextMatches(['text'], '').length, 0)
})
