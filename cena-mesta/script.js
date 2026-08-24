/**
 * «Цена места» — анализ коммерческой локации по адресу.
 * Карта: Leaflet + OpenStreetMap
 * Геокодер: Nominatim
 */

(function () {
  "use strict";

  // ===== Константы =====
  var MOSCOW_CENTER = [55.7558, 37.6173];
  var DEFAULT_ZOOM = 12;
  var RESULT_ZOOM = 16;
  var NOMINATIM_URL = "https://nominatim.openstreetmap.org/search";
  var STUB_LOADING_DELAY_MS = 900;

  // ===== DOM-элементы =====
  var form = document.getElementById("search-form");
  var addressInput = document.getElementById("address-input");
  var analyzeBtn = document.getElementById("analyze-btn");
  var geolocationBtn = document.getElementById("geolocation-btn");
  var errorMessage = document.getElementById("error-message");
  var resultsHint = document.getElementById("results-hint");
  var resultsGrid = document.getElementById("results-grid");

  var valueTraffic = document.getElementById("value-traffic");
  var valueRent = document.getElementById("value-rent");
  var valueCompetitors = document.getElementById("value-competitors");
  var valueCadastre = document.getElementById("value-cadastre");

  var cards = document.querySelectorAll(".card");

  // ===== Состояние карты =====
  var map = null;
  var marker = null;

  /**
   * Инициализация карты Leaflet с центром в Москве.
   */
  function initMap() {
    map = L.map("map", {
      center: MOSCOW_CENTER,
      zoom: DEFAULT_ZOOM,
      scrollWheelZoom: true
    });

    L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
      attribution:
        '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
      maxZoom: 19
    }).addTo(map);
  }

  /**
   * Показать сообщение об ошибке пользователю.
   * @param {string} text — текст ошибки
   */
  function showError(text) {
    errorMessage.textContent = text;
    errorMessage.hidden = false;
  }

  /**
   * Скрыть сообщение об ошибке.
   */
  function hideError() {
    errorMessage.textContent = "";
    errorMessage.hidden = true;
  }

  /**
   * Установить состояние загрузки для кнопок.
   * @param {boolean} isLoading
   */
  function setLoading(isLoading) {
    analyzeBtn.disabled = isLoading;
    geolocationBtn.disabled = isLoading;
    analyzeBtn.textContent = isLoading ? "Поиск…" : "Анализировать";
  }

  /**
   * Переместить карту и поставить метку в указанных координатах.
   * @param {number} lat
   * @param {number} lon
   * @param {string} label — подпись для всплывающей подсказки
   */
  function setMapLocation(lat, lon, label) {
    var latLng = [lat, lon];

    if (marker) {
      marker.setLatLng(latLng);
      marker.setPopupContent(label);
    } else {
      marker = L.marker(latLng).addTo(map);
      marker.bindPopup(label).openPopup();
    }

    map.setView(latLng, RESULT_ZOOM, { animate: true });
  }

  /**
   * Запрос к геокодеру Nominatim по текстовому адресу.
   * @param {string} query — адрес для поиска
   * @returns {Promise<{lat: number, lon: number, displayName: string}>}
   */
  function geocodeAddress(query) {
    var params = new URLSearchParams({
      format: "json",
      q: query,
      limit: "1",
      addressdetails: "0"
    });

    var url = NOMINATIM_URL + "?" + params.toString();

    return fetch(url, {
      method: "GET",
      headers: {
        Accept: "application/json"
      }
    })
      .then(function (response) {
        if (!response.ok) {
          throw new Error("Сервер геокодера вернул ошибку: " + response.status);
        }
        return response.json();
      })
      .then(function (data) {
        if (!Array.isArray(data) || data.length === 0) {
          throw new Error("Адрес не найден. Проверьте правильность написания.");
        }

        var place = data[0];
        return {
          lat: parseFloat(place.lat),
          lon: parseFloat(place.lon),
          displayName: place.display_name || query
        };
      });
  }

  /**
   * Псевдослучайное число на основе координат (детерминированные заглушки).
   * @param {number} lat
   * @param {number} lon
   * @param {number} seed — дополнительное смещение
   * @returns {number} значение от 0 до 1
   */
  function pseudoRandom(lat, lon, seed) {
    var x = Math.sin(lat * 12.9898 + lon * 78.233 + seed * 43.758) * 43758.5453;
    return x - Math.floor(x);
  }

  /**
   * Форматирование числа с разделителями тысяч.
   * @param {number} num
   * @returns {string}
   */
  function formatNumber(num) {
    return new Intl.NumberFormat("ru-RU").format(Math.round(num));
  }

  /**
   * Показать состояние «загрузка» в карточках результатов.
   */
  function showResultsLoading() {
    resultsHint.hidden = true;
    resultsGrid.hidden = false;

    var loadingTexts = {
      traffic: "Трафик: загрузка…",
      rent: "Аренда: загрузка…",
      competitors: "Конкуренты: загрузка…",
      cadastre: "Кадастровая стоимость: загрузка…"
    };

    valueTraffic.textContent = loadingTexts.traffic;
    valueRent.textContent = loadingTexts.rent;
    valueCompetitors.textContent = loadingTexts.competitors;
    valueCadastre.textContent = loadingTexts.cadastre;

    [valueTraffic, valueRent, valueCompetitors, valueCadastre].forEach(function (el) {
      el.classList.add("card__value--loading");
    });

    cards.forEach(function (card) {
      card.classList.remove("card--loaded");
      card.classList.add("card--loading");
    });
  }

  /**
   * Заполнить панель результатов демонстрационными данными-заглушками.
   * @param {number} lat
   * @param {number} lon
   */
  function fillStubResults(lat, lon) {
    // Генерируем правдоподобные значения на основе координат
    var trafficBase = 800 + pseudoRandom(lat, lon, 1) * 4200;
    var rentBase = 1200 + pseudoRandom(lat, lon, 2) * 3800;
    var competitorsCount = Math.floor(3 + pseudoRandom(lat, lon, 3) * 18);
    var cadastreBase = 8 + pseudoRandom(lat, lon, 4) * 42;

    var trafficLevel =
      trafficBase > 3500 ? "Высокий" : trafficBase > 2000 ? "Средний" : "Низкий";

    valueTraffic.textContent = formatNumber(trafficBase) + " чел./сутки (" + trafficLevel + ")";
    valueRent.textContent = formatNumber(rentBase) + " ₽";
    valueCompetitors.textContent = competitorsCount + " заведений";
    valueCadastre.textContent = formatNumber(cadastreBase) + " млн ₽";

    [valueTraffic, valueRent, valueCompetitors, valueCadastre].forEach(function (el) {
      el.classList.remove("card__value--loading");
    });

    cards.forEach(function (card) {
      card.classList.remove("card--loading");
      card.classList.add("card--loaded");
    });
  }

  /**
   * Запустить анализ локации: геокодирование + обновление карты и панели.
   * @param {string} address
   */
  function analyzeLocation(address) {
    hideError();
    setLoading(true);
    showResultsLoading();

    geocodeAddress(address)
      .then(function (place) {
        setMapLocation(place.lat, place.lon, place.displayName);

        // Имитация задержки получения аналитики с сервера
        setTimeout(function () {
          fillStubResults(place.lat, place.lon);
          setLoading(false);
        }, STUB_LOADING_DELAY_MS);
      })
      .catch(function (error) {
        setLoading(false);
        resultsGrid.hidden = true;
        resultsHint.hidden = false;

        if (error instanceof TypeError) {
          // Сетевая ошибка или блокировка CORS (часто при file://)
          showError(
            "Не удалось выполнить запрос к геокодеру. Проверьте подключение к интернету. " +
              "Если открываете файл напрямую (file://), попробуйте запустить локальный сервер."
          );
        } else {
          showError(error.message || "Произошла неизвестная ошибка.");
        }
      });
  }

  /**
   * Обработка отправки формы с адресом.
   * @param {Event} event
   */
  function handleFormSubmit(event) {
    event.preventDefault();

    var address = addressInput.value.trim();

    if (!address) {
      showError("Введите адрес для анализа.");
      addressInput.focus();
      return;
    }

    analyzeLocation(address);
  }

  /**
   * Центрирование карты на текущем местоположении пользователя.
   */
  function handleGeolocation() {
    hideError();

    if (!navigator.geolocation) {
      showError("Геолокация не поддерживается вашим браузером.");
      return;
    }

    setLoading(true);
    showResultsLoading();

    navigator.geolocation.getCurrentPosition(
      function (position) {
        var lat = position.coords.latitude;
        var lon = position.coords.longitude;
        var label = "Ваше местоположение";

        setMapLocation(lat, lon, label);
        addressInput.value = lat.toFixed(6) + ", " + lon.toFixed(6);

        setTimeout(function () {
          fillStubResults(lat, lon);
          setLoading(false);
        }, STUB_LOADING_DELAY_MS);
      },
      function (geoError) {
        setLoading(false);
        resultsGrid.hidden = true;
        resultsHint.hidden = false;

        var messages = {
          1: "Доступ к геолокации запрещён. Разрешите определение местоположения в настройках браузера.",
          2: "Не удалось определить местоположение. Попробуйте ещё раз.",
          3: "Время ожидания геолокации истекло."
        };

        showError(messages[geoError.code] || "Ошибка геолокации.");
      },
      {
        enableHighAccuracy: true,
        timeout: 10000,
        maximumAge: 60000
      }
    );
  }

  // ===== Инициализация =====
  initMap();
  form.addEventListener("submit", handleFormSubmit);
  geolocationBtn.addEventListener("click", handleGeolocation);
})();
