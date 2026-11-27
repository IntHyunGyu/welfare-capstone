(function () {
    var KEY = 'favoriteServIds';

    function load() {
        try {
            var ids = JSON.parse(localStorage.getItem(KEY) || '[]');
            return Array.isArray(ids) ? ids : [];
        } catch (e) {
            return [];
        }
    }

    function save(ids) {
        try {
            localStorage.setItem(KEY, JSON.stringify(ids));
        } catch (e) {
            // 저장소를 쓸 수 없는 브라우저(사생활 보호 모드 등)에서는 즐겨찾기가 이번 화면에만 반영된다.
        }
    }

    function render(button, saved) {
        button.setAttribute('aria-pressed', String(saved));
        button.textContent = saved ? '★ 저장됨' : '☆ 저장';
    }

    var saved = load();
    document.querySelectorAll('[data-fav-id]').forEach(function (button) {
        var id = button.getAttribute('data-fav-id');
        render(button, saved.indexOf(id) !== -1);
        button.addEventListener('click', function () {
            var ids = load();
            var index = ids.indexOf(id);
            if (index === -1) {
                ids.push(id);
            } else {
                ids.splice(index, 1);
            }
            save(ids);
            render(button, index === -1);
        });
    });

    var loader = document.querySelector('[data-fav-load]');
    if (loader) {
        var newestFirst = saved.slice().reverse().join(',');
        location.replace(loader.getAttribute('data-fav-load') + '?ids=' + encodeURIComponent(newestFirst));
    }
})();
