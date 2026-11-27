import type { RouteLocationNormalizedLoaded, RouteMeta } from 'vue-router'

// A tag is navigation data, never a clone of Vue Router's live object graph.
const metaKeys = [
  'hidden',
  'alwaysShow',
  'title',
  'titleSuffix',
  'tagsViewKey',
  'tagsViewTitle',
  'tagsViewKeyMode',
  'keepAliveName',
  'icon',
  'noCache',
  'breadcrumb',
  'breadcrumbTitle',
  'affix',
  'activeMenu',
  'noTagsView',
  'hideFooter',
  'followAuth',
  'canTo',
  'approvalTodoBadge',
  'personalWorkbenchTodoBadge'
] as const

const copyMeta = (meta: RouteMeta): RouteMeta => {
  const result: RouteMeta = {}
  for (const key of metaKeys) {
    const value = meta[key]
    if (
      value === undefined ||
      value === null ||
      ['string', 'number', 'boolean'].includes(typeof value)
    ) {
      result[key] = value as never
    }
  }
  return result
}

export const snapshotTagsView = (
  view: RouteLocationNormalizedLoaded
): RouteLocationNormalizedLoaded =>
  ({
    name: view.name,
    path: view.path,
    fullPath: view.fullPath,
    hash: view.hash,
    query: Object.fromEntries(
      Object.entries(view.query).map(([key, value]) => [
        key,
        Array.isArray(value) ? [...value] : value
      ])
    ),
    params: Object.fromEntries(
      Object.entries(view.params).map(([key, value]) => [
        key,
        Array.isArray(value) ? [...value] : value
      ])
    ),
    meta: copyMeta(view.meta),
    matched: view.matched.map((record) => ({
      name: record.name,
      path: record.path,
      meta: copyMeta(record.meta)
    })),
    redirectedFrom: undefined
  }) as RouteLocationNormalizedLoaded
