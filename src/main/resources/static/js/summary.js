(function () {
    var target = document.querySelector('[data-summary-url]');
    if (!target) {
        return;
    }
    var box = target.closest('.summary-box');
    var headers = {};
    var csrfToken = document.querySelector('meta[name="_csrf"]');
    var csrfHeader = document.querySelector('meta[name="_csrf_header"]');
    if (csrfToken && csrfHeader) {
        headers[csrfHeader.content] = csrfToken.content;
    }

    fetch(target.getAttribute('data-summary-url'), { method: 'POST', headers: headers })
        .then(function (response) {
            if (response.status !== 200) {
                throw new Error('summary unavailable: ' + response.status);
            }
            return response.json();
        })
        .then(function (data) {
            target.textContent = data.summary;
            target.classList.remove('loading');
        })
        .catch(function () {
            // 요약을 못 만들면 박스를 숨긴다 - 아래 원문은 그대로 보인다.
            box.hidden = true;
        });
})();
