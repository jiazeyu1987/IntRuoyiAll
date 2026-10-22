// Read-only PDF text; never creates annotations or persisted edits.
export interface PdfTextPage {
  getTextContent(): Promise<{ items: Array<{ str?: string; hasEOL?: boolean }> }>
  cleanup?(): void
}

export interface PdfTextDocument {
  numPages: number
  getPage(pageNumber: number): Promise<PdfTextPage>
}

export interface PdfTextMatch {
  pageNumber: number
  offset: number
  excerpt: string
}

const normalizePdfText = (text: string) => text.normalize('NFKC').replace(/\s+/gu, ' ').trim()

/** null means the document generation expired, never an empty/failed search. */
export const extractPdfTextPages = async (
  document: PdfTextDocument,
  isCurrent: () => boolean
): Promise<string[] | null> => {
  const pages: string[] = []
  for (let pageNumber = 1; pageNumber <= document.numPages; pageNumber++) {
    if (!isCurrent()) return null
    const page = await document.getPage(pageNumber)
    try {
      if (!isCurrent()) return null
      const content = await page.getTextContent()
      if (!isCurrent()) return null
      pages.push(normalizePdfText(content.items.map(item =>
        typeof item.str === 'string' ? item.str + (item.hasEOL ? '\n' : '') : ''
      ).join('')))
    } finally {
      page.cleanup?.()
    }
  }
  return isCurrent() ? pages : null
}

export const findPdfTextMatches = (pages: readonly string[], query: string): PdfTextMatch[] => {
  const needle = normalizePdfText(query).toLowerCase()
  if (!needle) return []
  const matches: PdfTextMatch[] = []
  pages.forEach((text, pageIndex) => {
    const normalized = normalizePdfText(text)
    const searchable = normalized.toLowerCase()
    let offset = searchable.indexOf(needle)
    while (offset >= 0) {
      matches.push({
        pageNumber: pageIndex + 1,
        offset,
        excerpt: normalized.slice(Math.max(0, offset - 24), offset + needle.length + 48)
      })
      offset = searchable.indexOf(needle, offset + needle.length)
    }
  })
  return matches
}
