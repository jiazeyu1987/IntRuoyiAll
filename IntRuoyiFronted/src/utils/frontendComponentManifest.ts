const viewModules = import.meta.glob('../views/**/*.{vue,tsx}')

const normalizeViewPath = (path: string) => {
  return path
    .replace(/\\/g, '/')
    .replace(/^\.\.\/views\//, '')
    .replace(/\.(vue|tsx)$/, '')
    .replace(/^\/+/, '')
}

export const getFrontendComponentPaths = () => {
  return Object.keys(viewModules).map(normalizeViewPath).sort()
}
