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
  // Nominatim: не более 1 запроса в секунду
  var NOMINATIM_MIN_INTERVAL_MS = 1100;
  var lastGeocodeRequestAt = 0;

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
   * Пауза на указанное время.
   * @param {number} ms
   * @returns {Promise<void>}
   */
  function delay(ms) {
    return new Promise(function (resolve) {
      setTimeout(resolve, ms);
    });
  }

  /**
   * Соблюдение лимита Nominatim: 1 запрос в секунду.
   * @returns {Promise<void>}
   */
  function waitForGeocoderSlot() {
    var now = Date.now();
    var waitMs = NOMINATIM_MIN_INTERVAL_MS - (now - lastGeocodeRequestAt);

    if (waitMs > 0) {
      return delay(waitMs);
    }

    return Promise.resolve();
  }

  /**
   * Нормализация введённого адреса.
   * @param {string} address
   * @returns {string}
   */
  function normalizeAddress(address) {
    return address
      .replace(/\s+/g, " ")
      .replace(/\s*,\s*/g, ", ")
      .trim();
  }

  /**
   * Проверка, похож ли ввод на координаты (широта, долгота).
   * @param {string} query
   * @returns {{lat: number, lon: number}|null}
   */
  function parseCoordinates(query) {
    var match = query.match(/^(-?\d+(?:\.\d+)?)\s*[,;]\s*(-?\d+(?:\.\d+)?)$/);

    if (!match) {
      return null;
    }

    var lat = parseFloat(match[1]);
    var lon = parseFloat(match[2]);

    if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
      return null;
    }

    return { lat: lat, lon: lon };
  }

  /**
   * Варианты запроса для повышения шанса нахождения адреса.
   * @param {string} address
   * @returns {string[]}
   */
  function buildSearchQueries(address) {
    var queries = [address];
    var lower = address.toLowerCase();

    if (lower.indexOf("россия") === -1 && lower.indexOf("russia") === -1) {
      queries.push(address + ", Россия");
    }

    // Без запятых иногда ищется лучше для коротких адресов
    if (address.indexOf(",") !== -1) {
      queries.push(address.replace(/,\s*/g, " "));
    }

    // Убираем дубликаты, сохраняя порядок
    return queries.filter(function (item, index, list) {
      return list.indexOf(item) === index;
    });
  }

  /**
   * Один запрос к Nominatim.
   * @param {string} query
   * @returns {Promise<Array>}
   */
  function fetchNominatim(query) {
    var params = new URLSearchParams({
      format: "json",
      q: query,
      limit: "5",
      addressdetails: "1",
      "accept-language": "ru",
      countrycodes: "ru"
    });

    var url = NOMINATIM_URL + "?" + params.toString();

    return waitForGeocoderSlot().then(function () {
      lastGeocodeRequestAt = Date.now();

      return fetch(url, {
        method: "GET",
        headers: {
          Accept: "application/json"
        }
      });
    }).then(function (response) {
      if (response.status === 429) {
        throw new Error(
          "Слишком много запросов к геокодеру. Подождите пару секунд и попробуйте снова."
        );
      }

      if (!response.ok) {
        throw new Error("Сервер геокодера вернул ошибку: " + response.status);
      }

      return response.json();
    }).then(function (data) {
      if (!Array.isArray(data)) {
        return [];
      }

      return data;
    });
  }

  /**
   * Выбор наиболее подходящего результата из списка.
   * @param {Array} results
   * @param {string} originalQuery
   * @returns {object|null}
   */
  function pickBestResult(results, originalQuery) {
    if (!results.length) {
      return null;
    }

    var queryLower = originalQuery.toLowerCase();

    // Предпочитаем здания и адреса, а не только регион/город целиком
    var ranked = results
      .map(function (place) {
        var score = place.importance || 0;
        var type = place.type || "";
        var className = place.class || "";
        var displayName = (place.display_name || "").toLowerCase();

        if (className === "building" || type === "house" || type === "commercial") {
          score += 0.2;
        }

        if (className === "highway" || type === "residential") {
          score += 0.05;
        }

        if (className === "boundary" && (type === "administrative" || type === "state")) {
          score -= 0.15;
        }

        if (queryLower && displayName.indexOf(queryLower.split(",")[0].trim()) !== -1) {
          score += 0.1;
        }

        return { place: place, score: score };
      })
      .sort(function (a, b) {
        return b.score - a.score;
      });

    return ranked[0].place;
  }

  /**
   * Запрос к геокодеру Nominatim по текстовому адресу.
   * @param {string} query — адрес для поиска
   * @returns {Promise<{lat: number, lon: number, displayName: string}>}
   */
  function geocodeAddress(query) {
    var normalized = normalizeAddress(query);
    var coordinates = parseCoordinates(normalized);

    if (coordinates) {
      return Promise.resolve({
        lat: coordinates.lat,
        lon: coordinates.lon,
        displayName: coordinates.lat.toFixed(6) + ", " + coordinates.lon.toFixed(6)
      });
    }

    var queries = buildSearchQueries(normalized);

    function tryQuery(index) {
      if (index >= queries.length) {
        throw new Error(
          "Адрес не найден. Укажите город и улицу, например: «Москва, Тверская улица, 1»."
        );
      }

      return fetchNominatim(queries[index]).then(function (results) {
        var place = pickBestResult(results, normalized);

        if (place) {
          return {
            lat: parseFloat(place.lat),
            lon: parseFloat(place.lon),
            displayName: place.display_name || normalized
          };
        }

        // Пустой ответ часто означает лимит запросов — повторяем ту же фразу
        return delay(NOMINATIM_MIN_INTERVAL_MS).then(function () {
          return fetchNominatim(queries[index]);
        }).then(function (retryResults) {
          place = pickBestResult(retryResults, normalized);

          if (place) {
            return {
              lat: parseFloat(place.lat),
              lon: parseFloat(place.lon),
              displayName: place.display_name || normalized
            };
          }

          return tryQuery(index + 1);
        });
      });
    }

    return tryQuery(0);
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
