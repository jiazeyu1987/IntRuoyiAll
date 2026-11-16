const fieldSelector = 'form, input, textarea, select'

// Apply to native fields as well as fields rendered by Element Plus, form-create and dialogs.
export const setupBrowserAutocomplete = () => {
  const passwordFields = new WeakSet<Element>()
  const applyToField = (element: Element) => {
    if (!element.matches(fieldSelector)) return
    if (element instanceof HTMLInputElement && element.type === 'password') {
      passwordFields.add(element)
    }
    // Keep password fields out of saved-login suggestions, including while revealing the password.
    const autocomplete = passwordFields.has(element) ? 'new-password' : 'off'
    if (element.getAttribute('autocomplete') !== autocomplete) {
      element.setAttribute('autocomplete', autocomplete)
    }
  }
  const applyToTree = (element: Element) => {
    applyToField(element)
    element.querySelectorAll(fieldSelector).forEach(applyToField)
  }
  const observer = new MutationObserver((records) => {
    for (const record of records) {
      if (record.type === 'attributes') {
        applyToField(record.target as Element)
      } else {
        record.addedNodes.forEach((node) => {
          if (node instanceof Element) applyToTree(node)
        })
      }
    }
  })
  applyToTree(document.documentElement)
  observer.observe(document.documentElement, {
    childList: true,
    subtree: true,
    attributes: true,
    attributeFilter: ['autocomplete', 'type']
  })
  // A newly mounted field can receive focus before the observer's microtask runs.
  const onFocus = (event: FocusEvent) => {
    if (event.target instanceof Element) applyToField(event.target)
  }
  document.addEventListener('focusin', onFocus, true)
  return () => {
    observer.disconnect()
    document.removeEventListener('focusin', onFocus, true)
  }
}
