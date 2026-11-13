/* Only read and navigate the current document; never modify its content. */
(function () {
  const origin = location.origin;
  const send = data => window.top.postMessage({ type: 'dcc-office-navigation', ...data }, origin);
  window.Asc.plugin.init = function () { send({ ready: true }); };
  window.addEventListener('message', function (event) {
    if (event.origin !== origin || event.source !== window.top || event.data?.type !== 'dcc-office-command') return;
    const request = event.data;
    window.Asc.scope.navigation = request;
    window.Asc.plugin.callCommand(function () {
      try {
        const request = Asc.scope.navigation;
        const doc = Api.GetDocument();
        const total = doc.GetPageCount();
        let current = doc.GetCurrentPage() + 1;
        let count = 0;
        let index = -1;
        if (request.action === 'page') {
          if (!Number.isInteger(request.page) || request.page < 1 || request.page > total) throw new Error('页码超出范围');
          if (!doc.GoToPage(request.page - 1)) throw new Error('无法跳转到指定页');
          current = request.page;
        } else if (request.action === 'search') {
          const matches = doc.Search(request.query, false);
          count = matches.length;
          if (count) {
            index = ((request.index % count) + count) % count;
            matches[index].Select();
            current = matches[index].GetStartPage() + 1;
          }
        } else if (request.action !== 'state') throw new Error('不支持的正文导航操作');
        return { total, current, count, index };
      } catch (error) { return { error: error.message }; }
    }, false, false, function (result) { send({ id: request.id, action: request.action, result }); });
  });
})();
