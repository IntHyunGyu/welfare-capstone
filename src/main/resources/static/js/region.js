(function () {
    var regions = window.REGIONS || {};
    var sido = document.getElementById('ctpvNm');
    var sgg = document.getElementById('sggNm');
    if (!sido || !sgg) {
        return;
    }
    // "조건 다시 입력"으로 왔을 때 서버가 넘겨준, 이전에 고른 시군구. 처음 한 번만 적용한다.
    var initial = sgg.getAttribute('data-selected') || '';

    function option(value, text) {
        var el = document.createElement('option');
        el.value = value;
        el.textContent = text;
        return el;
    }

    function refresh() {
        var list = regions[sido.value] || [];
        var keep = initial || sgg.value;
        initial = '';
        sgg.replaceChildren();
        if (!sido.value) {
            sgg.appendChild(option('', '시도를 먼저 골라주세요'));
            sgg.disabled = true;
            return;
        }
        sgg.appendChild(option('', list.length ? '전체' : '시군구 구분 없음'));
        list.forEach(function (name) {
            sgg.appendChild(option(name, name));
        });
        sgg.disabled = list.length === 0;
        if (keep && list.indexOf(keep) !== -1) {
            sgg.value = keep;
        }
    }

    sido.addEventListener('change', refresh);
    // 뒤로 가기로 돌아왔을 때 브라우저가 복원한 시도 값에 맞춰 목록을 다시 채운다.
    refresh();
})();
