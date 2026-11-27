(function () {
    var KEY = 'favoriteServIds';
    // 로그인 회원이면 head에 fav-api 메타가 있다 → DB(API)에 저장. 없으면 이 브라우저(localStorage)에 저장.
    var apiMeta = document.querySelector('meta[name="fav-api"]');
    var api = apiMeta ? apiMeta.content : null;
    var onFavoritesPage = document.querySelector('[data-fav-page]') !== null;

    function loadLocal() {
        try {
            var ids = JSON.parse(localStorage.getItem(KEY) || '[]');
            return Array.isArray(ids) ? ids : [];
        } catch (e) {
            return [];
        }
    }

    function saveLocal(ids) {
        try {
            localStorage.setItem(KEY, JSON.stringify(ids));
        } catch (e) {
            // 저장소를 쓸 수 없는 브라우저(사생활 보호 모드 등)에서는 즐겨찾기가 이번 화면에만 반영된다.
        }
    }

    function csrfHeaders() {
        var headers = {};
        var token = document.querySelector('meta[name="_csrf"]');
        var header = document.querySelector('meta[name="_csrf_header"]');
        if (token && header) {
            headers[header.content] = token.content;
        }
        return headers;
    }

    function savedIds() {
        if (!api) {
            return Promise.resolve(loadLocal());
        }
        return fetch(api, { headers: { Accept: 'application/json' } })
            .then(function (response) { return response.ok ? response.json() : []; })
            .catch(function () { return []; });
    }

    /** 저장/해제를 반영하고, 실제로 반영됐는지(true/false)를 돌려준다. */
    function setSaved(id, saved) {
        if (!api) {
            var ids = loadLocal().filter(function (each) { return each !== id; });
            if (saved) {
                ids.push(id);
            }
            saveLocal(ids);
            return Promise.resolve(true);
        }
        return fetch(api + '/' + encodeURIComponent(id), {
            method: saved ? 'POST' : 'DELETE',
            headers: csrfHeaders()
        })
            .then(function (response) { return response.ok; })
            .catch(function () { return false; });
    }

    function render(button, saved) {
        button.setAttribute('aria-pressed', String(saved));
        button.querySelector('.fav-label').textContent = saved ? '저장됨' : '저장';
    }

    savedIds().then(function (saved) {
        document.querySelectorAll('[data-fav-id]').forEach(function (button) {
            var id = button.getAttribute('data-fav-id');
            render(button, saved.indexOf(id) !== -1);
            button.addEventListener('click', function () {
                var next = button.getAttribute('aria-pressed') !== 'true';
                button.disabled = true;
                setSaved(id, next).then(function (ok) {
                    button.disabled = false;
                    if (!ok) {
                        return;
                    }
                    render(button, next);
                    // 저장한 혜택 화면에서 해제하면 카드를 바로 없애지 않고 흐리게 둔다 — 다시 누르면 되살아난다(실수 되돌리기).
                    if (onFavoritesPage) {
                        var card = button.closest('.result-card');
                        if (card) {
                            card.classList.toggle('is-unsaved', !next);
                        }
                    }
                });
            });
        });
    });

    // 비회원의 저장한 혜택 화면: 목록이 브라우저에만 있으니 servId를 붙여 다시 요청한다 (로그인 회원은 서버가 바로 그림).
    var loader = document.querySelector('[data-fav-load]');
    if (loader && !api) {
        var newestFirst = loadLocal().slice().reverse().join(',');
        location.replace(loader.getAttribute('data-fav-load') + '?ids=' + encodeURIComponent(newestFirst));
    }
})();
