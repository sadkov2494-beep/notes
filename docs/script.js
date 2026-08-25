/**
 * «Цена места» — анализ коммерческой локации с учётом типа бизнеса.
 * Геокодер: Nominatim · POI: Overpass API · Карта: Leaflet
 */

(function () {
  "use strict";

  // ===== Константы =====
  var MOSCOW_CENTER = [55.7558, 37.6173];
  var DEFAULT_ZOOM = 12;
  var RESULT_ZOOM = 15;
  var NOMINATIM_URL = "https://nominatim.openstreetmap.org/search";
  var OVERPASS_URL = "https://overpass-api.de/api/interpreter";
  var NOMINATIM_MIN_INTERVAL_MS = 1100;
  var lastGeocodeRequestAt = 0;

  // Типы бизнеса и теги OpenStreetMap для поиска конкурентов
  var BUSINESS_TYPES = {
    hairdresser: {
      label: "Парикмахерская / барбершоп",
      icon: "💇",
      plural: "парикмахерских",
      tags: [
        { shop: "hairdresser" },
        { shop: "beauty" },
        { amenity: "hairdresser" }
      ],
      rent: [1800, 5200],
      area: [35, 90],
      competition: { low: 3, high: 10 }
    },
    cafe: {
      label: "Кафе / кофейня",
      icon: "☕",
      plural: "кафе",
      tags: [{ amenity: "cafe" }],
      rent: [2200, 6500],
      area: [40, 120],
      competition: { low: 4, high: 14 }
    },
    restaurant: {
      label: "Ресторан",
      icon: "🍽️",
      plural: "ресторанов",
      tags: [{ amenity: "restaurant" }, { amenity: "fast_food" }],
      rent: [2500, 8000],
      area: [80, 300],
      competition: { low: 3, high: 12 }
    },
    pharmacy: {
      label: "Аптека",
      icon: "💊",
      plural: "аптек",
      tags: [{ amenity: "pharmacy" }],
      rent: [2000, 5500],
      area: [40, 100],
      competition: { low: 2, high: 6 }
    },
    grocery: {
      label: "Продуктовый магазин",
      icon: "🛒",
      plural: "магазинов",
      tags: [
        { shop: "supermarket" },
        { shop: "convenience" },
        { shop: "greengrocer" }
      ],
      rent: [1500, 4500],
      area: [50, 200],
      competition: { low: 2, high: 8 }
    },
    fitness: {
      label: "Фитнес-клуб",
      icon: "🏋️",
      plural: "фитнес-клубов",
      tags: [{ leisure: "fitness_centre" }, { leisure: "sports_centre" }],
      rent: [1200, 3500],
      area: [200, 800],
      competition: { low: 2, high: 5 }
    },
    dentist: {
      label: "Стоматология",
      icon: "🦷",
      plural: "стоматологий",
      tags: [{ amenity: "dentist" }],
      rent: [2000, 6000],
      area: [60, 180],
      competition: { low: 2, high: 7 }
    },
    bank: {
      label: "Банк / отделение",
      icon: "🏦",
      plural: "отделений",
      tags: [{ amenity: "bank" }, { amenity: "atm" }],
      rent: [2500, 7000],
      area: [50, 150],
      competition: { low: 3, high: 10 }
    },
    clothes: {
      label: "Магазин одежды",
      icon: "👗",
      plural: "магазинов одежды",
      tags: [{ shop: "clothes" }, { shop: "boutique" }, { shop: "shoes" }],
      rent: [2200, 7500],
      area: [50, 200],
      competition: { low: 3, high: 12 }
    },
    auto: {
      label: "Автосервис",
      icon: "🔧",
      plural: "автосервисов",
      tags: [{ shop: "car_repair" }, { amenity: "car_wash" }],
      rent: [800, 2500],
      area: [100, 400],
      competition: { low: 2, high: 6 }
    },
    flowers: {
      label: "Цветочный магазин",
      icon: "💐",
      plural: "цветочных",
      tags: [{ shop: "florist" }],
      rent: [1800, 5000],
      area: [20, 60],
      competition: { low: 2, high: 8 }
    },
    kids: {
      label: "Детский центр",
      icon: "🧸",
      plural: "детских центров",
      tags: [{ amenity: "kindergarten" }, { leisure: "playground" }],
      rent: [1500, 4000],
      area: [80, 250],
      competition: { low: 2, high: 6 }
    }
  };

  // ===== DOM =====
  var form = document.getElementById("search-form");
  var addressInput = document.getElementById("address-input");
  var businessTypeSelect = document.getElementById("business-type");
  var radiusSelect = document.getElementById("search-radius");
  var analyzeBtn = document.getElementById("analyze-btn");
  var geolocationBtn = document.getElementById("geolocation-btn");
  var errorMessage = document.getElementById("error-message");
  var resultsHint = document.getElementById("results-hint");
  var resultsBusiness = document.getElementById("results-business");
  var resultsGrid = document.getElementById("results-grid");
  var summaryBlock = document.getElementById("summary-block");
  var summaryScore = document.getElementById("summary-score");
  var summaryText = document.getElementById("summary-text");
  var competitorsList = document.getElementById("competitors-list");
  var competitorsItems = document.getElementById("competitors-items");
  var mapLegend = document.getElementById("map-legend");

  var valueEls = {
    competitors: document.getElementById("value-competitors"),
    nearest: document.getElementById("value-nearest"),
    competition: document.getElementById("value-competition"),
    rent: document.getElementById("value-rent"),
    area: document.getElementById("value-area"),
    budget: document.getElementById("value-budget"),
    traffic: document.getElementById("value-traffic"),
    transit: document.getElementById("value-transit"),
    parking: document.getElementById("value-parking"),
    cadastre: document.getElementById("value-cadastre")
  };

  var descCompetitors = document.getElementById("desc-competitors");

  // ===== Состояние карты =====
  var map = null;
  var targetMarker = null;
  var radiusCircle = null;
  var competitorLayer = null;
  var transitLayer = null;

  /**
   * Заполнить выпадающий список типов бизнеса.
   */
  function initBusinessTypes() {
    Object.keys(BUSINESS_TYPES).forEach(function (key) {
      var type = BUSINESS_TYPES[key];
      var option = document.createElement("option");
      option.value = key;
      option.textContent = type.icon + " " + type.label;
      businessTypeSelect.appendChild(option);
    });
  }

  /**
   * Инициализация карты Leaflet.
   */
  function initMap() {
    map = L.map("map", {
      center: MOSCOW_CENTER,
      zoom: DEFAULT_ZOOM,
      scrollWheelZoom: true
    });

    L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
      maxZoom: 19
    }).addTo(map);

    competitorLayer = L.layerGroup().addTo(map);
    transitLayer = L.layerGroup().addTo(map);
  }

  function showError(text) {
    errorMessage.textContent = text;
    errorMessage.hidden = false;
  }

  function hideError() {
    errorMessage.textContent = "";
    errorMessage.hidden = true;
  }

  function setLoading(isLoading) {
    analyzeBtn.disabled = isLoading;
    geolocationBtn.disabled = isLoading;
    businessTypeSelect.disabled = isLoading;
    radiusSelect.disabled = isLoading;
    analyzeBtn.textContent = isLoading ? "Анализ…" : "Анализировать";
  }

  function delay(ms) {
    return new Promise(function (resolve) {
      setTimeout(resolve, ms);
    });
  }

  function formatNumber(num) {
    return new Intl.NumberFormat("ru-RU").format(Math.round(num));
  }

  function pseudoRandom(lat, lon, seed) {
    var x = Math.sin(lat * 12.9898 + lon * 78.233 + seed * 43.758) * 43758.5453;
    return x - Math.floor(x);
  }

  // ===== Геокодирование (Nominatim) =====

  function normalizeAddress(address) {
    return address.replace(/\s+/g, " ").replace(/\s*,\s*/g, ", ").trim();
  }

  function parseCoordinates(query) {
    var match = query.match(/^(-?\d+(?:\.\d+)?)\s*[,;]\s*(-?\d+(?:\.\d+)?)$/);
    if (!match) return null;
    var lat = parseFloat(match[1]);
    var lon = parseFloat(match[2]);
    if (lat < -90 || lat > 90 || lon < -180 || lon > 180) return null;
    return { lat: lat, lon: lon };
  }

  function buildSearchQueries(address) {
    var queries = [address];
    var lower = address.toLowerCase();
    if (lower.indexOf("россия") === -1 && lower.indexOf("russia") === -1) {
      queries.push(address + ", Россия");
    }
    if (address.indexOf(",") !== -1) {
      queries.push(address.replace(/,\s*/g, " "));
    }
    return queries.filter(function (item, i, list) { return list.indexOf(item) === i; });
  }

  function waitForGeocoderSlot() {
    var waitMs = NOMINATIM_MIN_INTERVAL_MS - (Date.now() - lastGeocodeRequestAt);
    return waitMs > 0 ? delay(waitMs) : Promise.resolve();
  }

  function fetchNominatim(query) {
    var params = new URLSearchParams({
      format: "json", q: query, limit: "5",
      addressdetails: "1", "accept-language": "ru", countrycodes: "ru"
    });

    return waitForGeocoderSlot().then(function () {
      lastGeocodeRequestAt = Date.now();
      return fetch(NOMINATIM_URL + "?" + params.toString(), {
        headers: { Accept: "application/json" }
      });
    }).then(function (response) {
      if (response.status === 429) {
        throw new Error("Слишком много запросов. Подождите пару секунд.");
      }
      if (!response.ok) throw new Error("Ошибка геокодера: " + response.status);
      return response.json();
    }).then(function (data) {
      return Array.isArray(data) ? data : [];
    });
  }

  function pickBestResult(results, originalQuery) {
    if (!results.length) return null;
    var queryLower = originalQuery.toLowerCase();
    return results.map(function (place) {
      var score = place.importance || 0;
      if (place.class === "building" || place.type === "house") score += 0.2;
      if (place.class === "boundary") score -= 0.15;
      if (queryLower && (place.display_name || "").toLowerCase().indexOf(queryLower.split(",")[0].trim()) !== -1) {
        score += 0.1;
      }
      return { place: place, score: score };
    }).sort(function (a, b) { return b.score - a.score; })[0].place;
  }

  function geocodeAddress(query) {
    var normalized = normalizeAddress(query);
    var coords = parseCoordinates(normalized);
    if (coords) {
      return Promise.resolve({
        lat: coords.lat, lon: coords.lon,
        displayName: coords.lat.toFixed(6) + ", " + coords.lon.toFixed(6)
      });
    }

    var queries = buildSearchQueries(normalized);

    function tryQuery(index) {
      if (index >= queries.length) {
        throw new Error("Адрес не найден. Укажите город и улицу.");
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
        return delay(NOMINATIM_MIN_INTERVAL_MS).then(function () {
          return fetchNominatim(queries[index]);
        }).then(function (retry) {
          place = pickBestResult(retry, normalized);
          if (place) {
            return {
              lat: parseFloat(place.lat), lon: parseFloat(place.lon),
              displayName: place.display_name || normalized
            };
          }
          return tryQuery(index + 1);
        });
      });
    }

    return tryQuery(0);
  }

  // ===== Overpass: конкуренты и инфраструктура =====

  /**
   * Расстояние между двумя точками в метрах (формула гаверсинуса).
   */
  function getDistanceMeters(lat1, lon1, lat2, lon2) {
    var R = 6371000;
    var dLat = (lat2 - lat1) * Math.PI / 180;
    var dLon = (lon2 - lon1) * Math.PI / 180;
    var a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
      Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
      Math.sin(dLon / 2) * Math.sin(dLon / 2);
    return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  }

  /**
   * Построить Overpass-запрос для поиска POI по тегам.
   */
  function buildOverpassQuery(lat, lon, radius, tagFilters) {
    var parts = [];
    tagFilters.forEach(function (tag) {
      var key = Object.keys(tag)[0];
      var value = tag[key];
      parts.push('node["' + key + '"="' + value + '"](around:' + radius + ',' + lat + ',' + lon + ');');
      parts.push('way["' + key + '"="' + value + '"](around:' + radius + ',' + lat + ',' + lon + ');');
    });
    return "[out:json][timeout:30];(" + parts.join("") + ");out center tags;";
  }

  function fetchOverpass(query) {
    return fetch(OVERPASS_URL, {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: "data=" + encodeURIComponent(query)
    }).then(function (response) {
      if (!response.ok) throw new Error("Ошибка Overpass API: " + response.status);
      return response.json();
    });
  }

  /**
   * Извлечь координаты и имя из элемента OSM.
   */
  function parseOsmElement(element) {
    var lat = element.lat || (element.center && element.center.lat);
    var lon = element.lon || (element.center && element.center.lon);
    if (!lat || !lon) return null;

    var tags = element.tags || {};
    var name = tags.name || tags.brand || tags.operator || "Без названия";

    return { id: element.id, lat: lat, lon: lon, name: name, tags: tags };
  }

  /**
   * Найти конкурентов выбранного типа бизнеса.
   */
  function findCompetitors(lat, lon, radius, businessKey) {
    var business = BUSINESS_TYPES[businessKey];
    var query = buildOverpassQuery(lat, lon, radius, business.tags);

    return fetchOverpass(query).then(function (data) {
      var seen = {};
      var competitors = [];

      (data.elements || []).forEach(function (el) {
        var parsed = parseOsmElement(el);
        if (!parsed) return;

        // Исключаем точки слишком близко к целевой (менее 30 м — вероятно то же здание)
        var dist = getDistanceMeters(lat, lon, parsed.lat, parsed.lon);
        if (dist < 30) return;

        var dedupeKey = parsed.name + "|" + parsed.lat.toFixed(4) + "|" + parsed.lon.toFixed(4);
        if (seen[dedupeKey]) return;
        seen[dedupeKey] = true;

        competitors.push({
          name: parsed.name,
          lat: parsed.lat,
          lon: parsed.lon,
          distance: Math.round(dist)
        });
      });

      competitors.sort(function (a, b) { return a.distance - b.distance; });
      return competitors;
    });
  }

  /**
   * Найти остановки общественного транспорта.
   */
  function findTransit(lat, lon, radius) {
    var query = "[out:json][timeout:25];(" +
      'node["highway"="bus_stop"](around:' + radius + ',' + lat + ',' + lon + ");" +
      'node["public_transport"="stop_position"](around:' + radius + ',' + lat + ',' + lon + ");" +
      'node["railway"="tram_stop"](around:' + radius + ',' + lat + ',' + lon + ");" +
      ");out;";

    return fetchOverpass(query).then(function (data) {
      return (data.elements || []).map(parseOsmElement).filter(Boolean);
    });
  }

  /**
   * Найти парковки.
   */
  function findParking(lat, lon, radius) {
    var query = "[out:json][timeout:25];(" +
      'node["amenity"="parking"](around:' + radius + ',' + lat + ',' + lon + ");" +
      'way["amenity"="parking"](around:' + radius + ',' + lat + ',' + lon + ");" +
      ");out center;";

    return fetchOverpass(query).then(function (data) {
      return (data.elements || []).map(parseOsmElement).filter(Boolean);
    });
  }

  // ===== Карта: метки и круг радиуса =====

  function createIcon(color, label) {
    return L.divIcon({
      className: "map-pin map-pin--" + color,
      html: '<span>' + label + "</span>",
      iconSize: [28, 28],
      iconAnchor: [14, 14]
    });
  }

  function clearMapLayers() {
    competitorLayer.clearLayers();
    transitLayer.clearLayers();
    if (radiusCircle) {
      map.removeLayer(radiusCircle);
      radiusCircle = null;
    }
  }

  function showOnMap(lat, lon, label, radius, competitors, transit) {
    clearMapLayers();

    if (targetMarker) {
      targetMarker.setLatLng([lat, lon]);
      targetMarker.setPopupContent("<strong>Ваша точка</strong><br>" + label);
    } else {
      targetMarker = L.marker([lat, lon], { icon: createIcon("target", "★") })
        .addTo(map)
        .bindPopup("<strong>Ваша точка</strong><br>" + label)
        .openPopup();
    }

    radiusCircle = L.circle([lat, lon], {
      radius: radius,
      color: "#3b82f6",
      fillColor: "#3b82f6",
      fillOpacity: 0.08,
      weight: 2,
      dashArray: "6 4"
    }).addTo(map);

    competitors.forEach(function (c, i) {
      L.marker([c.lat, c.lon], { icon: createIcon("competitor", String(i + 1)) })
        .addTo(competitorLayer)
        .bindPopup("<strong>" + c.name + "</strong><br>" + c.distance + " м от вас");
    });

    transit.slice(0, 15).forEach(function (t) {
      L.marker([t.lat, t.lon], { icon: createIcon("transit", "🚌") })
        .addTo(transitLayer)
        .bindPopup(t.name);
    });

    var bounds = L.latLngBounds([[lat, lon]]);
    competitors.forEach(function (c) { bounds.extend([c.lat, c.lon]); });
    map.fitBounds(bounds.pad(0.15));
    mapLegend.hidden = false;
  }

  // ===== Аналитика и оценка =====

  function getCompetitionLevel(count, business) {
    if (count <= business.competition.low) return { level: "Низкая", class: "good" };
    if (count <= business.competition.high) return { level: "Средняя", class: "medium" };
    return { level: "Высокая", class: "bad" };
  }

  function getLocationScore(competitors, transit, parking, business, lat, lon) {
    var score = 50;
    var count = competitors.length;
    var comp = getCompetitionLevel(count, business);

    if (comp.class === "good") score += 25;
    else if (comp.class === "medium") score += 10;
    else score -= 15;

    if (count > 0 && competitors[0].distance > 200) score += 10;
    if (transit.length >= 3) score += 10;
    if (parking.length >= 1) score += 5;

    var traffic = 800 + pseudoRandom(lat, lon, 1) * 4200;
    if (traffic > 3000) score += 10;

    return Math.max(10, Math.min(95, Math.round(score)));
  }

  function buildRecommendation(score, competitors, business, radius) {
    var count = competitors.length;
    var comp = getCompetitionLevel(count, business);
    var nearest = count ? competitors[0].distance + " м" : "нет рядом";

    if (score >= 75) {
      return "Отличная локация для «" + business.label + "». Конкуренция " +
        comp.level.toLowerCase() + " (" + count + " " + business.plural + " в " + radius + " м). " +
        "Ближайший конкурент — " + nearest + ".";
    }
    if (score >= 55) {
      return "Локация подходит с оговорками. В радиусе " + radius + " м найдено " +
        count + " " + business.plural + " (" + comp.level.toLowerCase() + " конкуренция). " +
        "Изучите ценовую политику соседей и проходимость улицы.";
    }
    return "Высокая конкуренция: " + count + " " + business.plural + " в " + radius +
      " м. Ближайший — " + nearest + ". Рассмотрите другой адрес или уникальное позиционирование.";
  }

  function calculateStubMetrics(lat, lon, business) {
    var rand = pseudoRandom(lat, lon, business.label.length);
    var rent = business.rent[0] + rand * (business.rent[1] - business.rent[0]);
    var area = Math.round(business.area[0] + rand * (business.area[1] - business.area[0]));
    var traffic = Math.round(800 + pseudoRandom(lat, lon, 1) * 4200);
    var cadastre = 8 + pseudoRandom(lat, lon, 4) * 42;
    var trafficLevel = traffic > 3500 ? "Высокий" : traffic > 2000 ? "Средний" : "Низкий";

    return {
      rent: Math.round(rent),
      area: area,
      budget: Math.round(rent * area),
      traffic: traffic,
      trafficLevel: trafficLevel,
      cadastre: cadastre
    };
  }

  // ===== UI: результаты =====

  function showLoading(business, radius) {
    resultsHint.hidden = true;
    resultsBusiness.hidden = false;
    resultsBusiness.textContent = business.icon + " " + business.label + " · радиус " + radius + " м";
    resultsGrid.hidden = false;
    summaryBlock.hidden = false;
    competitorsList.hidden = false;
    summaryScore.textContent = "…";
    summaryText.textContent = "Собираем данные о конкурентах и инфраструктуре…";

    Object.keys(valueEls).forEach(function (key) {
      valueEls[key].textContent = "Загрузка…";
      valueEls[key].classList.add("card__value--loading");
    });

    competitorsItems.innerHTML = "<li class='competitors-list__loading'>Поиск на карте…</li>";
  }

  function renderResults(data) {
    var business = data.business;
    var metrics = data.metrics;
    var comp = data.competition;

    summaryScore.textContent = data.score + "/100";
    summaryScore.className = "summary__score summary__score--" +
      (data.score >= 75 ? "good" : data.score >= 55 ? "medium" : "bad");
    summaryText.textContent = data.recommendation;

    valueEls.competitors.textContent = data.competitors.length;
    descCompetitors.textContent = business.plural + " в радиусе " + data.radius + " м";

    valueEls.nearest.textContent = data.competitors.length
      ? data.competitors[0].distance + " м (" + data.competitors[0].name + ")"
      : "Не найдено";

    valueEls.competition.textContent = comp.level;
    valueEls.competition.className = "card__value card__value--" + comp.class;

    valueEls.rent.textContent = formatNumber(metrics.rent) + " ₽/м²";
    valueEls.area.textContent = metrics.area + " м²";
    valueEls.budget.textContent = formatNumber(metrics.budget) + " ₽/мес";
    valueEls.traffic.textContent = formatNumber(metrics.traffic) + " чел./сут (" + metrics.trafficLevel + ")";
    valueEls.transit.textContent = data.transit.length + " остановок";
    valueEls.parking.textContent = data.parking.length
      ? data.parking.length + " парковок"
      : "Не найдено";
    valueEls.cadastre.textContent = formatNumber(metrics.cadastre) + " млн ₽";

    Object.keys(valueEls).forEach(function (key) {
      valueEls[key].classList.remove("card__value--loading");
    });

    if (data.competitors.length) {
      competitorsItems.innerHTML = data.competitors.map(function (c, i) {
        return "<li><span class='competitors-list__num'>" + (i + 1) + ".</span> " +
          "<strong>" + c.name + "</strong> — " + c.distance + " м</li>";
      }).join("");
    } else {
      competitorsItems.innerHTML =
        "<li class='competitors-list__empty'>В радиусе " + data.radius +
        " м конкурентов не найдено в OpenStreetMap. Это может быть хорошим знаком!</li>";
    }
  }

  function resetResults() {
    resultsGrid.hidden = true;
    summaryBlock.hidden = true;
    competitorsList.hidden = true;
    resultsBusiness.hidden = true;
    resultsHint.hidden = false;
    mapLegend.hidden = true;
    clearMapLayers();
  }

  // ===== Основной анализ =====

  function runAnalysis(lat, lon, displayName, businessKey, radius) {
    var business = BUSINESS_TYPES[businessKey];
    showLoading(business, radius);
    showOnMap(lat, lon, displayName, radius, [], []);

    return Promise.all([
      findCompetitors(lat, lon, radius, businessKey),
      findTransit(lat, lon, radius),
      findParking(lat, lon, radius)
    ]).then(function (results) {
      var competitors = results[0];
      var transit = results[1];
      var parking = results[2];
      var metrics = calculateStubMetrics(lat, lon, business);
      var competition = getCompetitionLevel(competitors.length, business);
      var score = getLocationScore(competitors, transit, parking, business, lat, lon);
      var recommendation = buildRecommendation(score, competitors, business, radius);

      showOnMap(lat, lon, displayName, radius, competitors, transit);

      return {
        business: business,
        radius: radius,
        competitors: competitors,
        transit: transit,
        parking: parking,
        metrics: metrics,
        competition: competition,
        score: score,
        recommendation: recommendation
      };
    }).then(function (data) {
      renderResults(data);
      setLoading(false);
    }).catch(function (error) {
      setLoading(false);
      resetResults();
      if (error instanceof TypeError) {
        showError("Ошибка сети. Проверьте подключение к интернету.");
      } else {
        showError(error.message || "Ошибка анализа.");
      }
    });
  }

  function analyzeLocation(address) {
    hideError();
    setLoading(true);

    var businessKey = businessTypeSelect.value;
    var radius = parseInt(radiusSelect.value, 10);

    geocodeAddress(address)
      .then(function (place) {
        return runAnalysis(place.lat, place.lon, place.displayName, businessKey, radius);
      })
      .catch(function (error) {
        setLoading(false);
        resetResults();
        if (error instanceof TypeError) {
          showError("Не удалось выполнить запрос к геокодеру. Проверьте интернет.");
        } else {
          showError(error.message || "Произошла ошибка.");
        }
      });
  }

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

  function handleGeolocation() {
    hideError();
    if (!navigator.geolocation) {
      showError("Геолокация не поддерживается браузером.");
      return;
    }

    setLoading(true);
    var businessKey = businessTypeSelect.value;
    var radius = parseInt(radiusSelect.value, 10);

    navigator.geolocation.getCurrentPosition(
      function (position) {
        var lat = position.coords.latitude;
        var lon = position.coords.longitude;
        addressInput.value = lat.toFixed(6) + ", " + lon.toFixed(6);
        runAnalysis(lat, lon, "Ваше местоположение", businessKey, radius);
      },
      function (geoError) {
        setLoading(false);
        resetResults();
        var messages = {
          1: "Доступ к геолокации запрещён.",
          2: "Не удалось определить местоположение.",
          3: "Время ожидания геолокации истекло."
        };
        showError(messages[geoError.code] || "Ошибка геолокации.");
      },
      { enableHighAccuracy: true, timeout: 10000, maximumAge: 60000 }
    );
  }

  // ===== Инициализация =====
  initBusinessTypes();
  initMap();
  form.addEventListener("submit", handleFormSubmit);
  geolocationBtn.addEventListener("click", handleGeolocation);
})();
