export interface ProjectRowBounds { top: number; height: number }

// Size the viewport to whole variable-height rows, including a single unusually tall row.
export function completeProjectWindow(rows: ProjectRowBounds[], scrollTop: number, preferredHeight = 280) {
  if (!rows.length) return { top: 0, height: 100 }
  let start = 0
  for (let i = 1; i < rows.length; i++) {
    if (Math.abs(rows[i].top - scrollTop) < Math.abs(rows[start].top - scrollTop)) start = i
  }
  const top = rows[start].top
  let bottom = top + rows[start].height
  for (let i = start + 1; i < rows.length; i++) {
    const nextBottom = rows[i].top + rows[i].height
    if (nextBottom - top > preferredHeight) break
    bottom = nextBottom
  }
  return { top, height: bottom - top }
}
