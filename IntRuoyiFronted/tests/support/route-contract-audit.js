const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')

const FRONTEND_FILE_EXTENSIONS = new Set(['.ts', '.tsx', '.js', '.jsx'])
const REQUEST_METHOD_MAPPING = {
  download: 'GET',
  upload: 'POST'
}

const normalizeRoute = (route) => {
  let normalized = route.trim()
  normalized = normalized.replace(/\$\{[^}]+\}/g, '{}')
  normalized = normalized.replace(/\{[^}/]+\}/g, '{}')
  normalized = normalized.split('?', 1)[0]
  normalized = normalized.replace(/\/+/g, '/')
  if (!normalized.startsWith('/')) {
    normalized = `/${normalized}`
  }
  return normalized.replace(/\/$/, '') || '/'
}

const listFiles = (root, predicate) => {
  const files = []
  const visit = (current) => {
    for (const entry of fs.readdirSync(current, { withFileTypes: true })) {
      const entryPath = path.join(current, entry.name)
      if (entry.isDirectory()) {
        visit(entryPath)
      } else if (entry.isFile() && predicate(entryPath)) {
        files.push(entryPath)
      }
    }
  }
  visit(root)
  return files
}

const getScriptKind = (file) => {
  if (file.endsWith('.tsx')) {
    return ts.ScriptKind.TSX
  }
  if (file.endsWith('.jsx')) {
    return ts.ScriptKind.JSX
  }
  if (file.endsWith('.js')) {
    return ts.ScriptKind.JS
  }
  return ts.ScriptKind.TS
}

const collectTopLevelConstants = (sourceFile) => {
  const constants = new Map()

  for (const statement of sourceFile.statements) {
    if (!ts.isVariableStatement(statement)) {
      continue
    }
    for (const declaration of statement.declarationList.declarations) {
      if (ts.isIdentifier(declaration.name) && declaration.initializer) {
        constants.set(declaration.name.text, declaration.initializer)
      }
    }
  }

  return constants
}

const unwrapExpression = (expression) => {
  let current = expression
  while (
    ts.isParenthesizedExpression(current) ||
    ts.isAsExpression(current) ||
    ts.isTypeAssertionExpression(current) ||
    ts.isNonNullExpression(current) ||
    (ts.isSatisfiesExpression && ts.isSatisfiesExpression(current))
  ) {
    current = current.expression
  }
  return current
}

const resolveRouteExpression = (expression, routeConstants, resolving = new Set()) => {
  const current = unwrapExpression(expression)
  if (ts.isStringLiteral(current) || ts.isNoSubstitutionTemplateLiteral(current)) {
    return current.text
  }
  if (ts.isTemplateExpression(current)) {
    let value = current.head.text
    for (const span of current.templateSpans) {
      value += resolveRouteExpression(span.expression, routeConstants, resolving) ?? '{}'
      value += span.literal.text
    }
    return value
  }
  if (ts.isIdentifier(current)) {
    if (resolving.has(current.text)) {
      return null
    }
    const initializer = routeConstants.get(current.text)
    if (!initializer) {
      return null
    }
    const nextResolving = new Set(resolving)
    nextResolving.add(current.text)
    return resolveRouteExpression(initializer, routeConstants, nextResolving)
  }
  if (
    ts.isBinaryExpression(current) &&
    current.operatorToken.kind === ts.SyntaxKind.PlusToken
  ) {
    const left = resolveRouteExpression(current.left, routeConstants, resolving)
    const right = resolveRouteExpression(current.right, routeConstants, resolving)
    if (left === null && right === null) {
      return null
    }
    return `${left ?? '{}'}${right ?? '{}'}`
  }
  return null
}

const getPropertyName = (property) => {
  if (ts.isIdentifier(property.name) || ts.isStringLiteral(property.name)) {
    return property.name.text
  }
  return null
}

const findUrlExpression = (requestOptions) => {
  if (!ts.isObjectLiteralExpression(requestOptions)) {
    return null
  }
  for (const property of requestOptions.properties) {
    if (ts.isPropertyAssignment(property) && getPropertyName(property) === 'url') {
      return property.initializer
    }
    if (ts.isShorthandPropertyAssignment(property) && property.name.text === 'url') {
      return property.name
    }
  }
  return null
}

const collectFrontendRoutes = (frontendApiRoot) => {
  const routes = []
  const unresolvedRequests = []
  let requestCount = 0
  const supportedMethods = new Set([
    'get',
    'post',
    'put',
    'delete',
    'patch',
    'download',
    'upload'
  ])

  for (const apiFile of listFiles(frontendApiRoot, (file) =>
    FRONTEND_FILE_EXTENSIONS.has(path.extname(file))
  )) {
    const source = fs.readFileSync(apiFile, 'utf8')
    const sourceFile = ts.createSourceFile(
      apiFile,
      source,
      ts.ScriptTarget.Latest,
      true,
      getScriptKind(apiFile)
    )
    const routeConstants = collectTopLevelConstants(sourceFile)
    const relativeFile = path.relative(frontendApiRoot, apiFile).replaceAll('\\', '/')

    const visit = (node) => {
      if (
        ts.isCallExpression(node) &&
        ts.isPropertyAccessExpression(node.expression) &&
        ts.isIdentifier(node.expression.expression) &&
        node.expression.expression.text === 'request' &&
        supportedMethods.has(node.expression.name.text)
      ) {
        requestCount += 1
        const requestMethod = node.expression.name.text
        const method = REQUEST_METHOD_MAPPING[requestMethod] || requestMethod.toUpperCase()
        const urlExpressionNode = node.arguments[0]
          ? findUrlExpression(node.arguments[0])
          : null
        const urlExpression = urlExpressionNode
          ? urlExpressionNode.getText(sourceFile).trim()
          : '<missing url property>'
        const rawRoute = urlExpressionNode
          ? resolveRouteExpression(urlExpressionNode, routeConstants)
          : null

        if (!rawRoute || !rawRoute.startsWith('/')) {
          unresolvedRequests.push({
            method,
            relativeFile,
            urlExpression
          })
        } else {
          routes.push({
            method,
            route: normalizeRoute(rawRoute),
            relativeFile,
            rawRoute
          })
        }
      }
      ts.forEachChild(node, visit)
    }

    visit(sourceFile)
  }

  return {
    requestCount,
    routes,
    unresolvedRequests
  }
}

const collectBackendRoutes = (backendRoot) => {
  const routes = new Set()
  const mappingPattern =
    /@(?:[\w.]+\.)?(Get|Post|Put|Delete|Patch|Request)Mapping\s*(?:\((.*?)\))?/gs
  const stringPattern = /['"]([^'"]*)['"]/g

  for (const controllerFile of listFiles(backendRoot, (file) =>
    file.endsWith('Controller.java')
  )) {
    const source = fs.readFileSync(controllerFile, 'utf8')
    const classMatch = source.match(/\bclass\s+\w+/)
    if (!classMatch || classMatch.index === undefined) {
      continue
    }

    const classPosition = classMatch.index
    let routePrefix = ''

    for (const match of source.slice(0, classPosition).matchAll(mappingPattern)) {
      if (match[1] !== 'Request') {
        continue
      }
      const values = Array.from((match[2] || '').matchAll(stringPattern), (value) => value[1])
      routePrefix = values[0] || ''
    }

    for (const match of source.slice(classPosition).matchAll(mappingPattern)) {
      const mappingKind = match[1]
      const argumentsSource = match[2] || ''
      const routeSuffixes = Array.from(argumentsSource.matchAll(stringPattern), (value) => value[1])
      const resolvedSuffixes = routeSuffixes.length > 0 ? routeSuffixes : ['']
      const methods =
        mappingKind === 'Request'
          ? Array.from(argumentsSource.matchAll(/RequestMethod\.([A-Z]+)/g), (value) => value[1])
          : [mappingKind.toUpperCase()]
      const resolvedMethods = methods.length > 0 ? methods : ['ANY']

      for (const routeSuffix of resolvedSuffixes) {
        const fullRoute = normalizeRoute(
          [routePrefix.replace(/\/$/, ''), routeSuffix.replace(/^\//, '')].join('/')
        )
        for (const method of resolvedMethods) {
          routes.add(`${method} ${fullRoute}`)
        }
      }
    }
  }

  return routes
}

const getRequestIdentity = ({ method, relativeFile, urlExpression }) =>
  `${method}\u0000${relativeFile}\u0000${urlExpression}`

const auditRouteContracts = ({
  frontendApiRoot,
  backendRoot,
  dynamicRequestClassifications = []
}) => {
  if (!fs.existsSync(frontendApiRoot)) {
    throw new Error(`Frontend API root does not exist: ${frontendApiRoot}`)
  }
  if (!fs.existsSync(backendRoot)) {
    throw new Error(`Backend root does not exist: ${backendRoot}`)
  }

  const frontendAudit = collectFrontendRoutes(frontendApiRoot)
  const frontendRoutes = frontendAudit.routes
  const backendRoutes = collectBackendRoutes(backendRoot)
  const uniqueFrontendRoutes = new Map()

  for (const route of frontendRoutes) {
    uniqueFrontendRoutes.set(`${route.method} ${route.route}`, route)
  }

  const missingRoutes = Array.from(uniqueFrontendRoutes.values())
    .filter(
      ({ method, route }) =>
        !backendRoutes.has(`${method} ${route}`) && !backendRoutes.has(`ANY ${route}`)
    )
    .sort((left, right) =>
      `${left.method} ${left.route}`.localeCompare(`${right.method} ${right.route}`)
    )
  const classificationByRequest = new Map()

  for (const classification of dynamicRequestClassifications) {
    if (!classification.reason || !classification.reason.trim()) {
      throw new Error(
        `Dynamic request classification requires a reason: ${getRequestIdentity(classification)}`
      )
    }
    const identity = getRequestIdentity(classification)
    if (classificationByRequest.has(identity)) {
      throw new Error(`Duplicate dynamic request classification: ${identity}`)
    }
    classificationByRequest.set(identity, classification)
  }

  const classifiedDynamicRequests = []
  const unclassifiedRequests = []
  const unresolvedRequestIdentities = new Set()

  for (const unresolvedRequest of frontendAudit.unresolvedRequests) {
    const identity = getRequestIdentity(unresolvedRequest)
    unresolvedRequestIdentities.add(identity)
    const classification = classificationByRequest.get(identity)
    if (classification) {
      classifiedDynamicRequests.push({
        ...unresolvedRequest,
        reason: classification.reason
      })
    } else {
      unclassifiedRequests.push(unresolvedRequest)
    }
  }

  const unusedDynamicRequestClassifications = dynamicRequestClassifications.filter(
    (classification) => !unresolvedRequestIdentities.has(getRequestIdentity(classification))
  )

  return {
    frontendRequestCount: frontendAudit.requestCount,
    frontendRouteCount: uniqueFrontendRoutes.size,
    backendRouteCount: backendRoutes.size,
    missingRoutes,
    unresolvedRequests: frontendAudit.unresolvedRequests,
    classifiedDynamicRequests,
    unclassifiedRequests,
    unusedDynamicRequestClassifications
  }
}

module.exports = {
  auditRouteContracts,
  normalizeRoute
}
