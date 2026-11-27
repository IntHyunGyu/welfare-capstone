(function () {
    var regions = window.REGIONS || {};
    var sido = document.getElementById('ctpvNm');
    var sgg = document.getElementById('sggNm');
    if (!sido || !sgg) {
        return;
    }

    function option(value, text) {
        var el = document.createElement('option');
        el.value = value;
        el.textContent = text;
        return el;
    }

    function refresh() {
        var list = regions[sido.value] || [];
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
    }

    sido.addEventListener('change', refresh);
    // 뒤로 가기로 돌아왔을 때 브라우저가 복원한 시도 값에 맞춰 목록을 다시 채운다.
    refresh();
})();
