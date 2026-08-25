/**
 * «Цена места» — анализ коммерческой локации, финансовая модель и бизнес-план.
 * Карта: Leaflet + OpenStreetMap
 * Геокодер: Nominatim
 * Конкуренты: Overpass API (OSM)
 */

(function () {
  "use strict";

  // ===== Константы =====
  var MOSCOW_CENTER = [55.7558, 37.6173];
  var DEFAULT_ZOOM = 12;
  var NOMINATIM_URL = "https://nominatim.openstreetmap.org/search";
  var OVERPASS_URL = "https://overpass-api.de/api/interpreter";
  var STUB_LOADING_DELAY_MS = 700;
  var NOMINATIM_MIN_INTERVAL_MS = 1100;
  var lastGeocodeRequestAt = 0;

  var BUSINESS_PRESETS = {
    cafe: {
      label: "Кафе / ресторан",
      avgCheck: 650,
      conversion: 2.8,
      cogs: 38,
      staff: 5,
      salary: 55000,
      area: 90,
      renovation: 900000,
      equipment: 1400000
    },
    retail: {
      label: "Розничный магазин",
      avgCheck: 1200,
      conversion: 1.5,
      cogs: 55,
      staff: 3,
      salary: 48000,
      area: 70,
      renovation: 600000,
      equipment: 800000
    },
    beauty: {
      label: "Салон красоты",
      avgCheck: 2800,
      conversion: 0.8,
      cogs: 25,
      staff: 4,
      salary: 60000,
      area: 60,
      renovation: 700000,
      equipment: 500000
    },
    pharmacy: {
      label: "Аптека",
      avgCheck: 850,
      conversion: 1.2,
      cogs: 62,
      staff: 3,
      salary: 52000,
      area: 55,
      renovation: 500000,
      equipment: 1100000
    },
    fitness: {
      label: "Фитнес-клуб",
      avgCheck: 3500,
      conversion: 0.4,
      cogs: 15,
      staff: 6,
      salary: 58000,
      area: 250,
      renovation: 2500000,
      equipment: 3500000
    },
    services: {
      label: "Услуги",
      avgCheck: 1800,
      conversion: 1.0,
      cogs: 20,
      staff: 3,
      salary: 50000,
      area: 50,
      renovation: 400000,
      equipment: 300000
    },
    other: {
      label: "Другое",
      avgCheck: 1000,
      conversion: 2.0,
      cogs: 40,
      staff: 3,
      salary: 50000,
      area: 80,
      renovation: 600000,
      equipment: 700000
    }
  };

  var OVERPASS_FILTERS = {
    cafe: [
      'node["amenity"~"^(cafe|restaurant|fast_food|bar|bakery)$"]',
      'way["amenity"~"^(cafe|restaurant|fast_food|bar|bakery)$"]'
    ],
    retail: [
      'node["shop"]',
      'way["shop"]'
    ],
    beauty: [
      'node["shop"~"^(beauty|hairdresser)$"]',
      'node["amenity"~"^(hairdresser|beauty)$"]',
      'way["shop"~"^(beauty|hairdresser)$"]'
    ],
    pharmacy: [
      'node["amenity"="pharmacy"]',
      'way["amenity"="pharmacy"]'
    ],
    fitness: [
      'node["leisure"="fitness_centre"]',
      'way["leisure"="fitness_centre"]',
      'node["sport"="fitness"]'
    ],
    services: [
      'node["office"]',
      'way["office"]',
      'node["craft"]',
      'way["craft"]'
    ],
    other: [
      'node["shop"]',
      'way["shop"]',
      'node["amenity"]',
      'way["amenity"]'
    ]
  };

  // ===== DOM =====
  var form = document.getElementById("search-form");
  var addressInput = document.getElementById("address-input");
  var businessTypeSelect = document.getElementById("business-type");
  var radiusSelect = document.getElementById("radius-select");
  var analyzeBtn = document.getElementById("analyze-btn");
  var geolocationBtn = document.getElementById("geolocation-btn");
  var errorMessage = document.getElementById("error-message");
  var resultsSection = document.getElementById("results-section");
  var resultsAddress = document.getElementById("results-address");
  var resultsGrid = document.getElementById("results-grid");
  var locationScore = document.getElementById("location-score");
  var scoreValue = document.getElementById("score-value");
  var calculatorSection = document.getElementById("calculator-section");
  var reportSection = document.getElementById("report-section");
  var businessReport = document.getElementById("business-report");
  var printReportBtn = document.getElementById("print-report-btn");
  var copyReportBtn = document.getElementById("copy-report-btn");

  var valueTraffic = document.getElementById("value-traffic");
  var valueRent = document.getElementById("value-rent");
  var valueCompetitors = document.getElementById("value-competitors");
  var valueCadastre = document.getElementById("value-cadastre");
  var valueDensity = document.getElementById("value-density");
  var valueParking = document.getElementById("value-parking");
  var descCompetitors = document.getElementById("desc-competitors");

  var calcInputs = {
    area: document.getElementById("calc-area"),
    rentRate: document.getElementById("calc-rent-rate"),
    depositMonths: document.getElementById("calc-deposit-months"),
    avgCheck: document.getElementById("calc-avg-check"),
    conversion: document.getElementById("calc-conversion"),
    workDays: document.getElementById("calc-work-days"),
    staff: document.getElementById("calc-staff"),
    salary: document.getElementById("calc-salary"),
    utilities: document.getElementById("calc-utilities"),
    marketing: document.getElementById("calc-marketing"),
    cogs: document.getElementById("calc-cogs"),
    renovation: document.getElementById("calc-renovation"),
    equipment: document.getElementById("calc-equipment"),
    licenses: document.getElementById("calc-licenses"),
    otherStartup: document.getElementById("calc-other-startup")
  };

  var finOutputs = {
    revenue: document.getElementById("fin-revenue"),
    expenses: document.getElementById("fin-expenses"),
    profit: document.getElementById("fin-profit"),
    margin: document.getElementById("fin-margin"),
    breakeven: document.getElementById("fin-breakeven"),
    investment: document.getElementById("fin-investment"),
    payback: document.getElementById("fin-payback"),
    roi: document.getElementById("fin-roi")
  };

  var cards = document.querySelectorAll(".card");

  // ===== Состояние =====
  var map = null;
  var marker = null;
  var radiusCircle = null;
  var competitorLayer = null;
  var currentAnalysis = null;

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

    competitorLayer = L.layerGroup().addTo(map);
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
    analyzeBtn.textContent = isLoading ? "Анализ…" : "Анализировать";
  }

  function delay(ms) {
    return new Promise(function (resolve) {
      setTimeout(resolve, ms);
    });
  }

  function waitForGeocoderSlot() {
    var now = Date.now();
    var waitMs = NOMINATIM_MIN_INTERVAL_MS - (now - lastGeocodeRequestAt);
    return waitMs > 0 ? delay(waitMs) : Promise.resolve();
  }

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
    return queries.filter(function (item, index, list) {
      return list.indexOf(item) === index;
    });
  }

  function fetchNominatim(query) {
    var params = new URLSearchParams({
      format: "json",
      q: query,
      limit: "5",
      addressdetails: "1",
      "accept-language": "ru",
      countrycodes: "ru"
    });

    return waitForGeocoderSlot().then(function () {
      lastGeocodeRequestAt = Date.now();
      return fetch(NOMINATIM_URL + "?" + params.toString(), {
        method: "GET",
        headers: { Accept: "application/json" }
      });
    }).then(function (response) {
      if (response.status === 429) {
        throw new Error("Слишком много запросов к геокодеру. Подождите пару секунд.");
      }
      if (!response.ok) {
        throw new Error("Сервер геокодера вернул ошибку: " + response.status);
      }
      return response.json();
    }).then(function (data) {
      return Array.isArray(data) ? data : [];
    });
  }

  function pickBestResult(results, originalQuery) {
    if (!results.length) return null;
    var queryLower = originalQuery.toLowerCase();
    var ranked = results.map(function (place) {
      var score = place.importance || 0;
      var type = place.type || "";
      var className = place.class || "";
      var displayName = (place.display_name || "").toLowerCase();
      if (className === "building" || type === "house" || type === "commercial") score += 0.2;
      if (className === "highway" || type === "residential") score += 0.05;
      if (className === "boundary" && (type === "administrative" || type === "state")) score -= 0.15;
      if (queryLower && displayName.indexOf(queryLower.split(",")[0].trim()) !== -1) score += 0.1;
      return { place: place, score: score };
    }).sort(function (a, b) { return b.score - a.score; });
    return ranked[0].place;
  }

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
        throw new Error("Адрес не найден. Укажите город и улицу, например: «Москва, Тверская улица, 1».");
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

  function pseudoRandom(lat, lon, seed) {
    var x = Math.sin(lat * 12.9898 + lon * 78.233 + seed * 43.758) * 43758.5453;
    return x - Math.floor(x);
  }

  function formatNumber(num) {
    return new Intl.NumberFormat("ru-RU").format(Math.round(num));
  }

  function formatMoney(num) {
    return formatNumber(num) + " ₽";
  }

  function formatRadiusLabel(meters) {
    if (meters >= 1000) {
      return meters / 1000 + " км";
    }
    return meters + " м";
  }

  function getRadiusMeters() {
    return parseInt(radiusSelect.value, 10) || 3000;
  }

  function getBusinessType() {
    return businessTypeSelect.value || "cafe";
  }

  function getZoomForRadius(radiusMeters) {
    if (radiusMeters >= 10000) return 12;
    if (radiusMeters >= 5000) return 13;
    if (radiusMeters >= 3000) return 14;
    return 15;
  }

  function setMapLocation(lat, lon, label, radiusMeters) {
    var latLng = [lat, lon];

    if (marker) {
      marker.setLatLng(latLng);
      marker.setPopupContent(label);
    } else {
      marker = L.marker(latLng).addTo(map);
      marker.bindPopup(label).openPopup();
    }

    if (radiusCircle) {
      map.removeLayer(radiusCircle);
    }

    radiusCircle = L.circle(latLng, {
      radius: radiusMeters,
      color: "#3b82f6",
      fillColor: "#3b82f6",
      fillOpacity: 0.08,
      weight: 2,
      dashArray: "6 4"
    }).addTo(map);

    map.fitBounds(radiusCircle.getBounds(), { padding: [24, 24], animate: true });
  }

  function clearCompetitorMarkers() {
    competitorLayer.clearLayers();
  }

  function buildOverpassQuery(lat, lon, radiusMeters, businessType) {
    var filters = OVERPASS_FILTERS[businessType] || OVERPASS_FILTERS.other;
    var around = "(around:" + radiusMeters + "," + lat + "," + lon + ")";
    var parts = filters.map(function (filter) {
      return filter + around + ";";
    }).join("\n  ");

    return "[out:json][timeout:25];\n(\n  " + parts + "\n);\nout center 200;";
  }

  function fetchCompetitors(lat, lon, radiusMeters, businessType) {
    var query = buildOverpassQuery(lat, lon, radiusMeters, businessType);

    return fetch(OVERPASS_URL, {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: "data=" + encodeURIComponent(query)
    }).then(function (response) {
      if (!response.ok) {
        throw new Error("Overpass API: " + response.status);
      }
      return response.json();
    }).then(function (data) {
      var elements = Array.isArray(data.elements) ? data.elements : [];
      var seen = {};
      var competitors = [];

      elements.forEach(function (el) {
        var latP = el.lat || (el.center && el.center.lat);
        var lonP = el.lon || (el.center && el.center.lon);
        if (!latP || !lonP) return;

        var key = Math.round(latP * 10000) + "_" + Math.round(lonP * 10000);
        if (seen[key]) return;
        seen[key] = true;

        var tags = el.tags || {};
        var name = tags.name || tags.brand || tags.shop || tags.amenity || "Без названия";
        competitors.push({ lat: latP, lon: lonP, name: name, tags: tags });
      });

      return competitors;
    });
  }

  function showCompetitorsOnMap(competitors) {
    clearCompetitorMarkers();
    competitors.forEach(function (c) {
      L.circleMarker([c.lat, c.lon], {
        radius: 5,
        color: "#f97316",
        fillColor: "#fb923c",
        fillOpacity: 0.85,
        weight: 1
      }).bindPopup(c.name).addTo(competitorLayer);
    });
  }

  function estimateStubCompetitors(lat, lon, radiusMeters, businessType) {
    var base = 3 + pseudoRandom(lat, lon, 3) * 18;
    var radiusFactor = radiusMeters / 1000;
    var typeFactor = businessType === "fitness" ? 0.4 : businessType === "pharmacy" ? 0.6 : 1;
    return Math.max(1, Math.round(base * radiusFactor * typeFactor));
  }

  function generateLocationMetrics(lat, lon, radiusMeters, businessType, competitorCount) {
    var radiusKm = radiusMeters / 1000;
    var trafficBase = 800 + pseudoRandom(lat, lon, 1) * 4200;
    var rentBase = 1200 + pseudoRandom(lat, lon, 2) * 3800;
    var cadastreBase = 8 + pseudoRandom(lat, lon, 4) * 42;
    var densityBase = Math.round((5000 + pseudoRandom(lat, lon, 5) * 45000) * Math.pow(radiusKm, 1.4));
    var parkingBase = Math.round((5 + pseudoRandom(lat, lon, 6) * 40) * Math.pow(radiusKm, 1.2));

    var trafficLevel = trafficBase > 3500 ? "Высокий" : trafficBase > 2000 ? "Средний" : "Низкий";
    var densityLevel = densityBase > 30000 ? "Высокая" : densityBase > 12000 ? "Средняя" : "Низкая";

    var competitionDensity = competitorCount / Math.max(radiusKm * radiusKm * Math.PI, 0.1);
    var competitionLabel =
      competitionDensity > 8 ? "Высокая конкуренция" :
      competitionDensity > 3 ? "Умеренная конкуренция" : "Низкая конкуренция";

    return {
      traffic: trafficBase,
      trafficLevel: trafficLevel,
      rent: rentBase,
      competitors: competitorCount,
      cadastre: cadastreBase,
      density: densityBase,
      densityLevel: densityLevel,
      parking: parkingBase,
      competitionDensity: competitionDensity,
      competitionLabel: competitionLabel,
      businessType: businessType,
      businessLabel: (BUSINESS_PRESETS[businessType] || BUSINESS_PRESETS.other).label,
      radiusMeters: radiusMeters
    };
  }

  function calculateLocationScore(metrics) {
    var trafficScore = Math.min(100, (metrics.traffic / 5000) * 100) * 0.3;
    var rentScore = Math.max(0, 100 - (metrics.rent / 5000) * 100) * 0.15;
    var competitionScore = Math.max(0, 100 - metrics.competitionDensity * 8) * 0.25;
    var densityScore = Math.min(100, (metrics.density / 50000) * 100) * 0.2;
    var parkingScore = Math.min(100, (metrics.parking / 80) * 100) * 0.1;
    var total = trafficScore + rentScore + competitionScore + densityScore + parkingScore;
    return Math.round(Math.max(10, Math.min(98, total)));
  }

  function scoreLabel(score) {
    if (score >= 80) return "Отличная";
    if (score >= 65) return "Хорошая";
    if (score >= 50) return "Средняя";
    if (score >= 35) return "Слабая";
    return "Рискованная";
  }

  function showResultsLoading(radiusMeters) {
    resultsSection.hidden = false;
    calculatorSection.hidden = false;
    reportSection.hidden = false;

    descCompetitors.textContent = "Похожие точки в радиусе " + formatRadiusLabel(radiusMeters);

    var loading = "Загрузка…";
    valueTraffic.textContent = loading;
    valueRent.textContent = loading;
    valueCompetitors.textContent = loading;
    valueCadastre.textContent = loading;
    valueDensity.textContent = loading;
    valueParking.textContent = loading;

    [valueTraffic, valueRent, valueCompetitors, valueCadastre, valueDensity, valueParking].forEach(function (el) {
      el.classList.add("card__value--loading");
    });

    cards.forEach(function (card) {
      card.classList.remove("card--loaded");
      card.classList.add("card--loading");
    });

    locationScore.hidden = true;
  }

  function fillResults(metrics, score) {
    valueTraffic.textContent = formatNumber(metrics.traffic) + " чел./сутки (" + metrics.trafficLevel + ")";
    valueRent.textContent = formatMoney(metrics.rent);
    valueCompetitors.textContent = metrics.competitors + " заведений";
    valueCadastre.textContent = formatNumber(metrics.cadastre) + " млн ₽";
    valueDensity.textContent = formatNumber(metrics.density) + " чел. (" + metrics.densityLevel + ")";
    valueParking.textContent = metrics.parking + " мест";

    descCompetitors.textContent =
      metrics.competitors + " похожих точек в радиусе " + formatRadiusLabel(metrics.radiusMeters) +
      " · " + metrics.competitionLabel;

    [valueTraffic, valueRent, valueCompetitors, valueCadastre, valueDensity, valueParking].forEach(function (el) {
      el.classList.remove("card__value--loading");
    });

    cards.forEach(function (card) {
      card.classList.remove("card--loading");
      card.classList.add("card--loaded");
    });

    scoreValue.textContent = score + "/100 · " + scoreLabel(score);
    locationScore.hidden = false;
    locationScore.className = "score-badge score-badge--" + (
      score >= 80 ? "great" : score >= 65 ? "good" : score >= 50 ? "medium" : "low"
    );
  }

  function applyBusinessPreset(businessType, rentRate) {
    var preset = BUSINESS_PRESETS[businessType] || BUSINESS_PRESETS.other;
    calcInputs.area.value = preset.area;
    if (rentRate) {
      calcInputs.rentRate.value = Math.round(rentRate);
    }
    calcInputs.avgCheck.value = preset.avgCheck;
    calcInputs.conversion.value = preset.conversion;
    calcInputs.staff.value = preset.staff;
    calcInputs.salary.value = preset.salary;
    calcInputs.cogs.value = preset.cogs;
    calcInputs.renovation.value = preset.renovation;
    calcInputs.equipment.value = preset.equipment;
  }

  function readCalcInput(el) {
    return parseFloat(el.value) || 0;
  }

  function calculateFinances(traffic) {
    var area = readCalcInput(calcInputs.area);
    var rentRate = readCalcInput(calcInputs.rentRate);
    var depositMonths = readCalcInput(calcInputs.depositMonths);
    var avgCheck = readCalcInput(calcInputs.avgCheck);
    var conversion = readCalcInput(calcInputs.conversion) / 100;
    var workDays = readCalcInput(calcInputs.workDays);
    var staff = readCalcInput(calcInputs.staff);
    var salary = readCalcInput(calcInputs.salary);
    var utilities = readCalcInput(calcInputs.utilities);
    var marketing = readCalcInput(calcInputs.marketing);
    var cogsPercent = readCalcInput(calcInputs.cogs) / 100;
    var renovation = readCalcInput(calcInputs.renovation);
    var equipment = readCalcInput(calcInputs.equipment);
    var licenses = readCalcInput(calcInputs.licenses);
    var otherStartup = readCalcInput(calcInputs.otherStartup);

    var dailyCustomers = traffic * conversion;
    var monthlyCustomers = dailyCustomers * workDays;
    var monthlyRevenue = monthlyCustomers * avgCheck;

    var monthlyRent = area * rentRate;
    var monthlyPayroll = staff * salary;
    var monthlyCogs = monthlyRevenue * cogsPercent;
    var monthlyOpex = monthlyRent + monthlyPayroll + utilities + marketing + monthlyCogs;
    var monthlyProfit = monthlyRevenue - monthlyOpex;
    var margin = monthlyRevenue > 0 ? (monthlyProfit / monthlyRevenue) * 100 : 0;

    var deposit = monthlyRent * depositMonths;
    var totalInvestment = renovation + equipment + licenses + otherStartup + deposit;

    var fixedCosts = monthlyRent + monthlyPayroll + utilities + marketing;
    var contributionMargin = avgCheck * (1 - cogsPercent);
    var breakevenCustomersPerDay = contributionMargin > 0
      ? fixedCosts / (contributionMargin * workDays)
      : 0;
    var breakevenTraffic = conversion > 0 ? breakevenCustomersPerDay / conversion : 0;

    var paybackMonths = monthlyProfit > 0 ? totalInvestment / monthlyProfit : null;
    var annualProfit = monthlyProfit * 12;
    var roi = totalInvestment > 0 ? (annualProfit / totalInvestment) * 100 : 0;

    return {
      dailyCustomers: dailyCustomers,
      monthlyCustomers: monthlyCustomers,
      monthlyRevenue: monthlyRevenue,
      monthlyRent: monthlyRent,
      monthlyPayroll: monthlyPayroll,
      monthlyCogs: monthlyCogs,
      monthlyOpex: monthlyOpex,
      monthlyProfit: monthlyProfit,
      margin: margin,
      totalInvestment: totalInvestment,
      deposit: deposit,
      breakevenTraffic: breakevenTraffic,
      breakevenCustomersPerDay: breakevenCustomersPerDay,
      paybackMonths: paybackMonths,
      roi: roi,
      annualProfit: annualProfit
    };
  }

  function updateFinanceDisplay(finances) {
    finOutputs.revenue.textContent = formatMoney(finances.monthlyRevenue);
    finOutputs.expenses.textContent = formatMoney(finances.monthlyOpex);
    finOutputs.profit.textContent = formatMoney(finances.monthlyProfit);
    finOutputs.profit.className = "finance-card__value" + (finances.monthlyProfit >= 0 ? " finance-card__value--positive" : " finance-card__value--negative");
    finOutputs.margin.textContent = finances.margin.toFixed(1) + "%";
    finOutputs.breakeven.textContent = formatNumber(finances.breakevenTraffic) + " чел./день";
    finOutputs.investment.textContent = formatMoney(finances.totalInvestment);

    if (finances.paybackMonths === null) {
      finOutputs.payback.textContent = "Не окупится";
    } else if (finances.paybackMonths > 120) {
      finOutputs.payback.textContent = "> 10 лет";
    } else {
      finOutputs.payback.textContent = finances.paybackMonths.toFixed(1) + " мес.";
    }

    finOutputs.roi.textContent = finances.roi.toFixed(1) + "%";
    finOutputs.roi.className = "finance-card__value" + (finances.roi >= 0 ? " finance-card__value--positive" : " finance-card__value--negative");
  }

  function generateBusinessReport(place, metrics, score, finances) {
    var radiusLabel = formatRadiusLabel(metrics.radiusMeters);
    var verdict =
      score >= 80 ? "Локация выглядит перспективной для выбранного формата." :
      score >= 65 ? "Локация подходит при грамотной финансовой модели." :
      score >= 50 ? "Локация средняя — требуется детальная проверка конкурентов и аренды." :
      "Локация рискованная — рекомендуется рассмотреть альтернативные адреса.";

    var profitVerdict =
      finances.monthlyProfit > 0
        ? "При заданных параметрах проект выходит в плюс на " + formatMoney(finances.monthlyProfit) + " в месяц."
        : "При текущих параметрах проект убыточен на " + formatMoney(Math.abs(finances.monthlyProfit)) + " в месяц.";

    var paybackText =
      finances.paybackMonths === null
        ? "Срок окупаемости не рассчитывается — отрицательная прибыль."
        : finances.paybackMonths > 120
          ? "Срок окупаемости превышает 10 лет."
          : "Ориентировочный срок окупаемости: " + finances.paybackMonths.toFixed(1) + " месяцев.";

    return (
      "<h3>1. Резюме</h3>" +
      "<p><strong>Адрес:</strong> " + escapeHtml(place.displayName) + "</p>" +
      "<p><strong>Формат:</strong> " + escapeHtml(metrics.businessLabel) + "</p>" +
      "<p><strong>Зона анализа:</strong> " + radiusLabel + "</p>" +
      "<p><strong>Оценка локации:</strong> " + score + "/100 (" + scoreLabel(score) + ")</p>" +
      "<p>" + verdict + "</p>" +

      "<h3>2. Анализ локации</h3>" +
      "<ul>" +
      "<li>Пешеходный трафик: " + formatNumber(metrics.traffic) + " чел./сутки (" + metrics.trafficLevel + ")</li>" +
      "<li>Конкуренты: " + metrics.competitors + " в радиусе " + radiusLabel + " (" + metrics.competitionLabel + ")</li>" +
      "<li>Аренда: " + formatMoney(metrics.rent) + "/м²</li>" +
      "<li>Плотность застройки: " + formatNumber(metrics.density) + " чел. (" + metrics.densityLevel + ")</li>" +
      "<li>Парковки: " + metrics.parking + " мест</li>" +
      "<li>Кадастровая стоимость: ~" + formatNumber(metrics.cadastre) + " млн ₽</li>" +
      "</ul>" +

      "<h3>3. Финансовая модель (месяц)</h3>" +
      "<ul>" +
      "<li>Площадь: " + readCalcInput(calcInputs.area) + " м²</li>" +
      "<li>Клиентов в день (оценка): " + finances.dailyCustomers.toFixed(1) + "</li>" +
      "<li>Выручка: " + formatMoney(finances.monthlyRevenue) + "</li>" +
      "<li>Аренда: " + formatMoney(finances.monthlyRent) + "</li>" +
      "<li>ФОТ: " + formatMoney(finances.monthlyPayroll) + "</li>" +
      "<li>Себестоимость: " + formatMoney(finances.monthlyCogs) + "</li>" +
      "<li>Прочие расходы: " + formatMoney(readCalcInput(calcInputs.utilities) + readCalcInput(calcInputs.marketing)) + "</li>" +
      "<li><strong>Чистая прибыль:</strong> " + formatMoney(finances.monthlyProfit) + " (маржа " + finances.margin.toFixed(1) + "%)</li>" +
      "</ul>" +

      "<h3>4. Стартовые вложения</h3>" +
      "<ul>" +
      "<li>Ремонт: " + formatMoney(readCalcInput(calcInputs.renovation)) + "</li>" +
      "<li>Оборудование: " + formatMoney(readCalcInput(calcInputs.equipment)) + "</li>" +
      "<li>Лицензии: " + formatMoney(readCalcInput(calcInputs.licenses)) + "</li>" +
      "<li>Депозит: " + formatMoney(finances.deposit) + "</li>" +
      "<li>Прочие: " + formatMoney(readCalcInput(calcInputs.otherStartup)) + "</li>" +
      "<li><strong>Итого вложений:</strong> " + formatMoney(finances.totalInvestment) + "</li>" +
      "</ul>" +

      "<h3>5. Ключевые показатели</h3>" +
      "<ul>" +
      "<li>Точка безубыточности: " + formatNumber(finances.breakevenTraffic) + " чел. трафика/день</li>" +
      "<li>" + paybackText + "</li>" +
      "<li>ROI за 1 год: " + finances.roi.toFixed(1) + "%</li>" +
      "<li>Годовая прибыль (прогноз): " + formatMoney(finances.annualProfit) + "</li>" +
      "</ul>" +

      "<h3>6. Риски и рекомендации</h3>" +
      "<ul>" +
      (metrics.competitionDensity > 8 ? "<li>Высокая конкуренция — нужна сильная УТП и маркетинг.</li>" : "") +
      (metrics.traffic < 1500 ? "<li>Низкий трафик — рассмотрите доставку и онлайн-каналы.</li>" : "") +
      (metrics.rent > 4000 ? "<li>Высокая аренда — пересмотрите площадь или формат.</li>" : "") +
      (finances.monthlyProfit <= 0 ? "<li>Отрицательная прибыль — снизьте расходы или повысьте конверсию/средний чек.</li>" : "") +
      "<li>Проведите ручной аудит конкурентов на карте и уточните аренду у собственника.</li>" +
      "<li>Заложите резерв 15–20% на непредвиденные расходы.</li>" +
      "</ul>" +

      "<p class=\"report__verdict\"><strong>Вывод:</strong> " + profitVerdict + " " + paybackText + "</p>" +
      "<p class=\"report__meta\">Отчёт сформирован автоматически " + new Date().toLocaleString("ru-RU") + ". Не является инвестиционной рекомендацией.</p>"
    );
  }

  function escapeHtml(text) {
    var div = document.createElement("div");
    div.textContent = text;
    return div.innerHTML;
  }

  function refreshCalculations() {
    if (!currentAnalysis) return;
    var finances = calculateFinances(currentAnalysis.metrics.traffic);
    currentAnalysis.finances = finances;
    updateFinanceDisplay(finances);
    businessReport.innerHTML = generateBusinessReport(
      currentAnalysis.place,
      currentAnalysis.metrics,
      currentAnalysis.score,
      finances
    );
  }

  function runAnalysis(lat, lon, displayName) {
    var radiusMeters = getRadiusMeters();
    var businessType = getBusinessType();

    setMapLocation(lat, lon, displayName, radiusMeters);
    showResultsLoading(radiusMeters);
    resultsAddress.textContent = displayName;

    var place = { lat: lat, lon: lon, displayName: displayName };

    return delay(STUB_LOADING_DELAY_MS).then(function () {
      return fetchCompetitors(lat, lon, radiusMeters, businessType).catch(function () {
        return null;
      });
    }).then(function (competitors) {
      var competitorCount;
      var usedRealData = false;

      if (competitors && competitors.length >= 0) {
        competitorCount = competitors.length;
        showCompetitorsOnMap(competitors);
        usedRealData = true;
      } else {
        competitorCount = estimateStubCompetitors(lat, lon, radiusMeters, businessType);
        clearCompetitorMarkers();
      }

      var metrics = generateLocationMetrics(lat, lon, radiusMeters, businessType, competitorCount);
      var score = calculateLocationScore(metrics);

      applyBusinessPreset(businessType, metrics.rent);
      calcInputs.rentRate.value = Math.round(metrics.rent);

      fillResults(metrics, score);

      currentAnalysis = {
        place: place,
        metrics: metrics,
        score: score,
        usedRealCompetitors: usedRealData
      };

      refreshCalculations();
      return currentAnalysis;
    });
  }

  function analyzeLocation(address) {
    hideError();
    setLoading(true);

    geocodeAddress(address)
      .then(function (place) {
        return runAnalysis(place.lat, place.lon, place.displayName);
      })
      .then(function () {
        setLoading(false);
      })
      .catch(function (error) {
        setLoading(false);
        resultsSection.hidden = true;
        calculatorSection.hidden = true;
        reportSection.hidden = true;

        if (error instanceof TypeError) {
          showError(
            "Не удалось выполнить запрос. Проверьте интернет. " +
            "При открытии через file:// запустите локальный сервер."
          );
        } else {
          showError(error.message || "Произошла неизвестная ошибка.");
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
      showError("Геолокация не поддерживается вашим браузером.");
      return;
    }

    setLoading(true);

    navigator.geolocation.getCurrentPosition(
      function (position) {
        var lat = position.coords.latitude;
        var lon = position.coords.longitude;
        var label = "Ваше местоположение";
        addressInput.value = lat.toFixed(6) + ", " + lon.toFixed(6);

        runAnalysis(lat, lon, label)
          .then(function () { setLoading(false); })
          .catch(function (error) {
            setLoading(false);
            showError(error.message || "Ошибка анализа.");
          });
      },
      function (geoError) {
        setLoading(false);
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

  function handlePrintReport() {
    window.print();
  }

  function handleCopyReport() {
    if (!currentAnalysis) return;
    var text = businessReport.innerText;
    navigator.clipboard.writeText(text).then(function () {
      copyReportBtn.textContent = "Скопировано!";
      setTimeout(function () {
        copyReportBtn.textContent = "Копировать текст";
      }, 2000);
    }).catch(function () {
      showError("Не удалось скопировать текст.");
    });
  }

  function bindCalcListeners() {
    Object.keys(calcInputs).forEach(function (key) {
      calcInputs[key].addEventListener("input", refreshCalculations);
    });
    businessTypeSelect.addEventListener("change", function () {
      if (currentAnalysis) {
        applyBusinessPreset(getBusinessType(), currentAnalysis.metrics.rent);
        refreshCalculations();
      }
    });
    radiusSelect.addEventListener("change", function () {
      if (currentAnalysis && addressInput.value.trim()) {
        analyzeLocation(addressInput.value.trim());
      }
    });
  }

  initMap();
  bindCalcListeners();
  form.addEventListener("submit", handleFormSubmit);
  geolocationBtn.addEventListener("click", handleGeolocation);
  printReportBtn.addEventListener("click", handlePrintReport);
  copyReportBtn.addEventListener("click", handleCopyReport);
})();
