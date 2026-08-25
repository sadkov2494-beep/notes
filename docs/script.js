/**
 * «Цена места» — анализ коммерческой локации с учётом типа бизнеса.
 * Геокодер: Nominatim · POI: Overpass API · Карта: OpenLayers (OSGeo)
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
  var STORAGE_KEY = "cena-mesta-saved-v2";
  var MAX_SAVED = 10;

  var FINANCE_PRESETS = {
    hairdresser: { avgCheck: 1200, conversion: 1.2, cogs: 25, staff: 3, salary: 50000, renovation: 500000, equipment: 400000 },
    cafe: { avgCheck: 450, conversion: 3.0, cogs: 38, staff: 4, salary: 52000, renovation: 700000, equipment: 900000 },
    restaurant: { avgCheck: 1500, conversion: 1.5, cogs: 42, staff: 8, salary: 55000, renovation: 1500000, equipment: 2000000 },
    pharmacy: { avgCheck: 850, conversion: 1.2, cogs: 62, staff: 3, salary: 52000, renovation: 500000, equipment: 1100000 },
    grocery: { avgCheck: 600, conversion: 2.0, cogs: 55, staff: 3, salary: 48000, renovation: 600000, equipment: 800000 },
    fitness: { avgCheck: 3500, conversion: 0.4, cogs: 15, staff: 6, salary: 58000, renovation: 2500000, equipment: 3500000 },
    dentist: { avgCheck: 4500, conversion: 0.5, cogs: 20, staff: 4, salary: 65000, renovation: 1200000, equipment: 2500000 },
    bank: { avgCheck: 0, conversion: 0.5, cogs: 10, staff: 5, salary: 60000, renovation: 1000000, equipment: 500000 },
    clothes: { avgCheck: 3500, conversion: 0.8, cogs: 50, staff: 3, salary: 48000, renovation: 600000, equipment: 500000 },
    auto: { avgCheck: 5000, conversion: 0.3, cogs: 45, staff: 4, salary: 55000, renovation: 800000, equipment: 1500000 },
    flowers: { avgCheck: 2000, conversion: 1.5, cogs: 40, staff: 2, salary: 45000, renovation: 300000, equipment: 200000 },
    kids: { avgCheck: 2500, conversion: 0.6, cogs: 20, staff: 5, salary: 50000, renovation: 900000, equipment: 600000 }
  };

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
  var mapPopup = document.getElementById("map-popup");

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

  var calculatorSection = document.getElementById("calculator-section");
  var reportSection = document.getElementById("report-section");
  var compareSection = document.getElementById("compare-section");
  var businessReport = document.getElementById("business-report");
  var savedList = document.getElementById("saved-list");
  var compareWrap = document.getElementById("compare-wrap");
  var compareThead = document.getElementById("compare-thead");
  var compareTbody = document.getElementById("compare-tbody");
  var sourcesContent = document.getElementById("sources-content");
  var saveCalcBtn = document.getElementById("save-calc-btn");
  var exportExcelBtn = document.getElementById("export-excel-btn");
  var exportCompareBtn = document.getElementById("export-compare-btn");
  var clearSavedBtn = document.getElementById("clear-saved-btn");
  var printReportBtn = document.getElementById("print-report-btn");
  var copyReportBtn = document.getElementById("copy-report-btn");

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

  var currentAnalysis = null;
  var savedAnalyses = [];
  var lastAddress = "";

  // ===== Состояние карты (OpenLayers) =====
  var map = null;
  var popupOverlay = null;
  var targetSource = null;
  var circleSource = null;
  var competitorSource = null;
  var transitSource = null;

  /**
   * Получить выбранный тип бизнеса с проверкой.
   */
  function getBusinessKey() {
    var key = businessTypeSelect.value;
    if (!key || !BUSINESS_TYPES[key]) {
      return "hairdresser";
    }
    return key;
  }

  /**
   * Инициализация карты OpenLayers.
   */
  function initMap() {
    targetSource = new ol.source.Vector();
    circleSource = new ol.source.Vector();
    competitorSource = new ol.source.Vector();
    transitSource = new ol.source.Vector();

    map = new ol.Map({
      target: "map",
      layers: [
        new ol.layer.Tile({ source: new ol.source.OSM() }),
        new ol.layer.Vector({ source: circleSource, zIndex: 1 }),
        new ol.layer.Vector({ source: transitSource, zIndex: 2 }),
        new ol.layer.Vector({ source: competitorSource, zIndex: 3 }),
        new ol.layer.Vector({ source: targetSource, zIndex: 4 })
      ],
      view: new ol.View({
        center: ol.proj.fromLonLat([MOSCOW_CENTER[1], MOSCOW_CENTER[0]]),
        zoom: DEFAULT_ZOOM
      })
    });

    popupOverlay = new ol.Overlay({
      element: mapPopup,
      autoPan: true,
      positioning: "bottom-center",
      offset: [0, -12]
    });
    map.addOverlay(popupOverlay);

    map.on("click", function (evt) {
      var feature = map.forEachFeatureAtPixel(evt.pixel, function (f) { return f; });
      if (feature && feature.get("title")) {
        mapPopup.innerHTML = feature.get("title");
        mapPopup.hidden = false;
        popupOverlay.setPosition(evt.coordinate);
      } else {
        mapPopup.hidden = true;
        popupOverlay.setPosition(undefined);
      }
    });
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

  function formatMoney(num) {
    return formatNumber(num) + " ₽";
  }

  function formatRadiusLabel(meters) {
    return meters >= 1000 ? meters / 1000 + " км" : meters + " м";
  }

  function escapeHtml(text) {
    var div = document.createElement("div");
    div.textContent = text;
    return div.innerHTML;
  }

  function readCalcInput(el) {
    return parseFloat(el.value) || 0;
  }

  function readAllCalcValues() {
    var values = {};
    Object.keys(calcInputs).forEach(function (key) {
      values[key] = readCalcInput(calcInputs[key]);
    });
    return values;
  }

  function writeCalcValues(values) {
    Object.keys(values).forEach(function (key) {
      if (calcInputs[key]) calcInputs[key].value = values[key];
    });
  }

  function applyFinancePreset(businessKey, metrics) {
    var preset = FINANCE_PRESETS[businessKey] || FINANCE_PRESETS.cafe;
    calcInputs.area.value = metrics.area;
    calcInputs.rentRate.value = metrics.rent;
    calcInputs.avgCheck.value = preset.avgCheck;
    calcInputs.conversion.value = preset.conversion;
    calcInputs.staff.value = preset.staff;
    calcInputs.salary.value = preset.salary;
    calcInputs.cogs.value = preset.cogs;
    calcInputs.renovation.value = preset.renovation;
    calcInputs.equipment.value = preset.equipment;
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
    var monthlyRevenue = dailyCustomers * workDays * avgCheck;
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
    var breakevenTraffic = conversion > 0 && contributionMargin > 0
      ? fixedCosts / (contributionMargin * workDays) / conversion : 0;
    var paybackMonths = monthlyProfit > 0 ? totalInvestment / monthlyProfit : null;
    var annualProfit = monthlyProfit * 12;
    var roi = totalInvestment > 0 ? (annualProfit / totalInvestment) * 100 : 0;

    return {
      dailyCustomers: dailyCustomers,
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
    finOutputs.payback.textContent = finances.paybackMonths === null ? "Не окупится" :
      finances.paybackMonths > 120 ? "> 10 лет" : finances.paybackMonths.toFixed(1) + " мес.";
    finOutputs.roi.textContent = finances.roi.toFixed(1) + "%";
    finOutputs.roi.className = "finance-card__value" + (finances.roi >= 0 ? " finance-card__value--positive" : " finance-card__value--negative");
  }

  function generateBusinessReport(data, finances) {
    var m = data.metrics;
    var radiusLabel = formatRadiusLabel(data.radius);
    var profitVerdict = finances.monthlyProfit > 0
      ? "Проект выходит в плюс на " + formatMoney(finances.monthlyProfit) + " в месяц."
      : "Проект убыточен на " + formatMoney(Math.abs(finances.monthlyProfit)) + " в месяц.";

    return (
      "<h3>1. Резюме</h3><p><strong>Адрес:</strong> " + escapeHtml(lastAddress) + "</p>" +
      "<p><strong>Формат:</strong> " + escapeHtml(data.business.label) + "</p>" +
      "<p><strong>Зона анализа:</strong> " + radiusLabel + "</p>" +
      "<p><strong>Оценка:</strong> " + data.score + "/100</p><p>" + escapeHtml(data.recommendation) + "</p>" +
      "<h3>2. Локация</h3><ul>" +
      "<li>Конкуренты: " + data.competitors.length + " (" + data.competition.level + ")</li>" +
      "<li>Трафик: " + formatNumber(m.traffic) + " чел./сут (" + m.trafficLevel + ")</li>" +
      "<li>Аренда: " + formatMoney(m.rent) + "/м² · Площадь: " + m.area + " м²</li>" +
      "<li>Остановки: " + data.transit.length + " · Парковки: " + data.parking.length + "</li>" +
      "</ul>" +
      "<h3>3. Финансы (месяц)</h3><ul>" +
      "<li>Выручка: " + formatMoney(finances.monthlyRevenue) + "</li>" +
      "<li>Расходы: " + formatMoney(finances.monthlyOpex) + "</li>" +
      "<li><strong>Прибыль: " + formatMoney(finances.monthlyProfit) + "</strong> (маржа " + finances.margin.toFixed(1) + "%)</li>" +
      "</ul>" +
      "<h3>4. Вложения</h3><p><strong>Итого:</strong> " + formatMoney(finances.totalInvestment) + "</p>" +
      "<h3>5. Показатели</h3><ul>" +
      "<li>Безубыточность: " + formatNumber(finances.breakevenTraffic) + " чел./день</li>" +
      "<li>ROI за год: " + finances.roi.toFixed(1) + "%</li></ul>" +
      "<p class=\"report__verdict\"><strong>Вывод:</strong> " + profitVerdict + "</p>" +
      "<p class=\"report__meta\">Сформировано " + new Date().toLocaleString("ru-RU") + ". Не является инвестиционной рекомендацией.</p>"
    );
  }

  function refreshCalculations() {
    if (!currentAnalysis) return;
    var finances = calculateFinances(currentAnalysis.metrics.traffic);
    currentAnalysis.finances = finances;
    updateFinanceDisplay(finances);
    businessReport.innerHTML = generateBusinessReport(currentAnalysis, finances);
  }

  function enableAnalysisActions(enabled) {
    saveCalcBtn.disabled = !enabled;
    exportExcelBtn.disabled = !enabled;
  }

  function afterAnalysisComplete(data, address) {
    lastAddress = address;
    currentAnalysis = data;
    calculatorSection.hidden = false;
    reportSection.hidden = false;
    applyFinancePreset(getBusinessKey(), data.metrics);
    refreshCalculations();
    enableAnalysisActions(true);
  }

  function csvEscape(value) {
    var str = value == null ? "" : String(value);
    if (str.indexOf(";") !== -1 || str.indexOf("\"") !== -1) return "\"" + str.replace(/"/g, "\"\"") + "\"";
    return str;
  }

  function downloadCsv(filename, rows) {
    var blob = new Blob(["\uFEFF" + rows.map(function (r) { return r.map(csvEscape).join(";"); }).join("\n")], { type: "text/csv;charset=utf-8;" });
    var link = document.createElement("a");
    link.href = URL.createObjectURL(blob);
    link.download = filename;
    link.click();
    URL.revokeObjectURL(link.href);
  }

  function exportCurrentToExcel() {
    if (!currentAnalysis) return;
    var f = currentAnalysis.finances;
    var m = currentAnalysis.metrics;
    downloadCsv("cena-mesta-" + Date.now() + ".csv", [
      ["Параметр", "Значение"],
      ["Адрес", lastAddress],
      ["Тип бизнеса", currentAnalysis.business.label],
      ["Радиус", formatRadiusLabel(currentAnalysis.radius)],
      ["Оценка", currentAnalysis.score + "/100"],
      ["Конкуренты", currentAnalysis.competitors.length],
      ["Трафик", Math.round(m.traffic)],
      ["Аренда ₽/м²", Math.round(m.rent)],
      ["Выручка / мес", Math.round(f.monthlyRevenue)],
      ["Прибыль / мес", Math.round(f.monthlyProfit)],
      ["Вложения", Math.round(f.totalInvestment)],
      ["ROI %", f.roi.toFixed(1)]
    ]);
  }

  function getCompareRows() {
    return [
      { key: "score", label: "Оценка", format: function (v) { return v + "/100"; }, higherBetter: true },
      { key: "competitors", label: "Конкуренты", format: function (v) { return v + " шт."; }, higherBetter: false },
      { key: "traffic", label: "Трафик", format: function (v) { return formatNumber(v); }, higherBetter: true },
      { key: "rent", label: "Аренда ₽/м²", format: function (v) { return formatMoney(v); }, higherBetter: false },
      { key: "revenue", label: "Выручка / мес", format: function (v) { return formatMoney(v); }, higherBetter: true },
      { key: "profit", label: "Прибыль / мес", format: function (v) { return formatMoney(v); }, higherBetter: true },
      { key: "investment", label: "Вложения", format: function (v) { return formatMoney(v); }, higherBetter: false },
      { key: "roi", label: "ROI %", format: function (v) { return v.toFixed(1) + "%"; }, higherBetter: true }
    ];
  }

  function getCompareValue(item, key) {
    if (key === "score") return item.score;
    if (key === "competitors") return item.competitors;
    if (key === "traffic") return item.metrics.traffic;
    if (key === "rent") return item.metrics.rent;
    if (key === "revenue") return item.finances.monthlyRevenue;
    if (key === "profit") return item.finances.monthlyProfit;
    if (key === "investment") return item.finances.totalInvestment;
    if (key === "roi") return item.finances.roi;
    return null;
  }

  function renderSavedList() {
    var hasItems = savedAnalyses.length > 0;
    compareSection.hidden = !hasItems && !currentAnalysis;
    exportCompareBtn.disabled = !hasItems;
    clearSavedBtn.disabled = !hasItems;
    if (!hasItems) {
      savedList.innerHTML = '<p class="saved-list__empty">Нет сохранённых расчётов.</p>';
      compareWrap.hidden = true;
      return;
    }
    savedList.innerHTML = savedAnalyses.map(function (item) {
      return '<article class="saved-item"><div class="saved-item__info"><p class="saved-item__title">' +
        escapeHtml(item.address) + '</p><p class="saved-item__meta">' + escapeHtml(item.businessLabel) +
        ' · ' + formatRadiusLabel(item.radius) + ' · ' + item.score + '/100</p></div>' +
        '<div class="saved-item__actions">' +
        '<button class="btn btn--secondary btn--small" data-action="load" data-id="' + item.id + '">Открыть</button>' +
        '<button class="btn btn--danger btn--small" data-action="delete" data-id="' + item.id + '">Удалить</button>' +
        '</div></article>';
    }).join("");
    savedList.querySelectorAll("button").forEach(function (btn) {
      btn.addEventListener("click", function () {
        var id = btn.getAttribute("data-id");
        if (btn.getAttribute("data-action") === "load") handleLoadSaved(id);
        else handleDeleteSaved(id);
      });
    });
    renderComparisonTable();
  }

  function renderComparisonTable() {
    if (!savedAnalyses.length) { compareWrap.hidden = true; return; }
    compareWrap.hidden = false;
    compareThead.innerHTML = '<tr><th>Показатель</th>' + savedAnalyses.map(function (item, i) {
      return '<th>#' + (i + 1) + '<br><small>' + escapeHtml(item.address.split(",")[0]) + '</small></th>';
    }).join("") + '</tr>';
    compareTbody.innerHTML = getCompareRows().map(function (row) {
      var values = savedAnalyses.map(function (item) { return getCompareValue(item, row.key); });
      var numeric = values.filter(function (v) { return typeof v === "number"; });
      var best = numeric.length ? (row.higherBetter ? Math.max.apply(null, numeric) : Math.min.apply(null, numeric)) : null;
      return '<tr><th class="compare-table__metric">' + row.label + '</th>' + values.map(function (val) {
        var isBest = typeof val === "number" && best !== null && val === best && savedAnalyses.length > 1;
        return '<td' + (isBest ? ' class="compare-table__best"' : '') + '>' + row.format(val) + '</td>';
      }).join("") + '</tr>';
    }).join("");
  }

  function buildSnapshot() {
    if (!currentAnalysis) return null;
    return {
      id: Date.now().toString(36),
      savedAt: new Date().toISOString(),
      address: lastAddress,
      businessKey: getBusinessKey(),
      businessLabel: currentAnalysis.business.label,
      radius: currentAnalysis.radius,
      score: currentAnalysis.score,
      competitors: currentAnalysis.competitors.length,
      metrics: currentAnalysis.metrics,
      finances: currentAnalysis.finances,
      calc: readAllCalcValues()
    };
  }

  function handleSaveCalculation() {
    if (!currentAnalysis) return;
    var snapshot = buildSnapshot();
    var idx = savedAnalyses.findIndex(function (s) {
      return s.address === snapshot.address && s.businessKey === snapshot.businessKey && s.radius === snapshot.radius;
    });
    if (idx >= 0) savedAnalyses[idx] = snapshot; else {
      savedAnalyses.unshift(snapshot);
      if (savedAnalyses.length > MAX_SAVED) savedAnalyses = savedAnalyses.slice(0, MAX_SAVED);
    }
    localStorage.setItem(STORAGE_KEY, JSON.stringify(savedAnalyses));
    renderSavedList();
    compareSection.hidden = false;
    saveCalcBtn.textContent = "Сохранено!";
    setTimeout(function () { saveCalcBtn.textContent = "Сохранить расчёт"; }, 1500);
  }

  function handleLoadSaved(id) {
    var item = savedAnalyses.find(function (s) { return s.id === id; });
    if (!item) return;
    addressInput.value = item.address;
    businessTypeSelect.value = item.businessKey;
    radiusSelect.value = String(item.radius);
    writeCalcValues(item.calc);
    lastAddress = item.address;
    analyzeLocation(item.address);
  }

  function handleDeleteSaved(id) {
    savedAnalyses = savedAnalyses.filter(function (s) { return s.id !== id; });
    localStorage.setItem(STORAGE_KEY, JSON.stringify(savedAnalyses));
    renderSavedList();
  }

  function handleClearSaved() {
    if (!savedAnalyses.length || !window.confirm("Удалить все сохранённые расчёты?")) return;
    savedAnalyses = [];
    localStorage.removeItem(STORAGE_KEY);
    renderSavedList();
    compareSection.hidden = !currentAnalysis;
  }

  function exportComparisonToExcel() {
    if (!savedAnalyses.length) return;
    var rows = [["Показатель"].concat(savedAnalyses.map(function (item, i) { return "#" + (i + 1) + " " + item.address; }))];
    getCompareRows().forEach(function (row) {
      rows.push([row.label].concat(savedAnalyses.map(function (item) { return row.format(getCompareValue(item, row.key)); })));
    });
    downloadCsv("cena-mesta-sravnenie-" + Date.now() + ".csv", rows);
  }

  function loadSavedFromStorage() {
    try {
      savedAnalyses = JSON.parse(localStorage.getItem(STORAGE_KEY) || "[]");
      if (!Array.isArray(savedAnalyses)) savedAnalyses = [];
    } catch (e) { savedAnalyses = []; }
    renderSavedList();
  }

  function renderApiSourcesGuide() {
    sourcesContent.innerHTML =
      '<p class="sources-note">Сайт на GitHub Pages работает без сервера. Конкуренты — реальные (OSM). ' +
      'Трафик, аренда и кадастр — оценки, потому что эти данные в РФ платные и требуют бэкенд с API-ключами.</p>' +
      '<h3>Стоимость подключения</h3><table class="sources-table"><thead><tr><th>Данные</th><th>Провайдер</th><th>Цена</th></tr></thead><tbody>' +
      '<tr><td>Кадастр</td><td><a href="https://dadata.ru/api/cadastre-clean/" target="_blank" rel="noopener">DaData</a></td><td>~0,20 ₽/запрос</td></tr>' +
      '<tr><td>Адрес</td><td>DaData</td><td>бесплатно до 10k/день</td></tr>' +
      '<tr><td>Трафик</td><td><a href="https://yandex.ru/geoanalytics/" target="_blank" rel="noopener">Яндекс Геоаналитика</a></td><td>0 ₽ в вебе, API — B2B</td></tr>' +
      '<tr><td>Аренда</td><td>ЦИАН / агентства</td><td>от ~30 000 ₽/мес</td></tr>' +
      '<tr><td>Конкуренты</td><td>OSM</td><td>0 ₽ (уже работает)</td></tr>' +
      '</tbody></table>' +
      '<p><strong>MVP:</strong> 0–3 000 ₽/мес · <strong>Базовый:</strong> 5 000–15 000 ₽/мес · <strong>Полный:</strong> 50 000+ ₽/мес</p>';
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

  // ===== Карта: метки и круг радиуса (OpenLayers) =====

  function makePointStyle(fillColor, label) {
    return new ol.style.Style({
      image: new ol.style.Circle({
        radius: 13,
        fill: new ol.style.Fill({ color: fillColor }),
        stroke: new ol.style.Stroke({ color: "#ffffff", width: 2 })
      }),
      text: new ol.style.Text({
        text: label,
        font: "bold 11px sans-serif",
        fill: new ol.style.Fill({ color: "#ffffff" })
      })
    });
  }

  function addPointFeature(source, lat, lon, style, title) {
    var feature = new ol.Feature({
      geometry: new ol.geom.Point(ol.proj.fromLonLat([lon, lat])),
      title: title
    });
    feature.setStyle(style);
    source.addFeature(feature);
    return feature;
  }

  function clearMapLayers() {
    targetSource.clear();
    circleSource.clear();
    competitorSource.clear();
    transitSource.clear();
    mapPopup.hidden = true;
    if (popupOverlay) popupOverlay.setPosition(undefined);
  }

  function showOnMap(lat, lon, label, radius, competitors, transit) {
    clearMapLayers();

    addPointFeature(
      targetSource, lat, lon,
      makePointStyle("#3b82f6", "★"),
      "<strong>Ваша точка</strong><br>" + label
    );

    var circlePolygon = ol.geom.Polygon.fromCircle(
      new ol.geom.Circle(ol.proj.fromLonLat([lon, lat]), radius),
      64
    );
    var circleFeature = new ol.Feature({ geometry: circlePolygon });
    circleFeature.setStyle(new ol.style.Style({
      stroke: new ol.style.Stroke({ color: "#3b82f6", width: 2, lineDash: [6, 4] }),
      fill: new ol.style.Fill({ color: "rgba(59, 130, 246, 0.1)" })
    }));
    circleSource.addFeature(circleFeature);

    competitors.forEach(function (c, i) {
      addPointFeature(
        competitorSource, c.lat, c.lon,
        makePointStyle("#ef4444", String(i + 1)),
        "<strong>" + c.name + "</strong><br>" + c.distance + " м от вас"
      );
    });

    transit.slice(0, 15).forEach(function (t) {
      addPointFeature(
        transitSource, t.lat, t.lon,
        makePointStyle("#22c55e", "•"),
        t.name
      );
    });

    var coords = [[lon, lat]];
    competitors.forEach(function (c) { coords.push([c.lon, c.lat]); });
    var extent = ol.extent.boundingExtent(coords.map(function (c) {
      return ol.proj.fromLonLat(c);
    }));
    map.getView().fit(extent, { padding: [60, 60, 60, 60], maxZoom: 17, duration: 400 });
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
    var radiusLabel = formatRadiusLabel(radius);

    if (score >= 75) {
      return "Отличная локация для «" + business.label + "». Конкуренция " +
        comp.level.toLowerCase() + " (" + count + " " + business.plural + " в " + radiusLabel + "). " +
        "Ближайший конкурент — " + nearest + ".";
    }
    if (score >= 55) {
      return "Локация подходит с оговорками. В радиусе " + radiusLabel + " найдено " +
        count + " " + business.plural + " (" + comp.level.toLowerCase() + " конкуренция). " +
        "Изучите ценовую политику соседей и проходимость улицы.";
    }
    return "Высокая конкуренция: " + count + " " + business.plural + " в " + radiusLabel +
      ". Ближайший — " + nearest + ". Рассмотрите другой адрес или уникальное позиционирование.";
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
    resultsBusiness.textContent = business.icon + " " + business.label + " · радиус " + formatRadiusLabel(radius);
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
    descCompetitors.textContent = business.plural + " в радиусе " + formatRadiusLabel(data.radius);

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
        "<li class='competitors-list__empty'>В радиусе " + formatRadiusLabel(data.radius) +
        " конкурентов не найдено в OpenStreetMap. Это может быть хорошим знаком!</li>";
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
    if (!BUSINESS_TYPES[businessKey]) {
      setLoading(false);
      showError("Выберите тип бизнеса из списка.");
      return Promise.resolve();
    }

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
      afterAnalysisComplete(data, displayName);
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

    var businessKey = getBusinessKey();
    var radius = parseInt(radiusSelect.value, 10) || 3000;

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
    if (!businessTypeSelect.value) {
      showError("Выберите тип бизнеса.");
      businessTypeSelect.focus();
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
    var businessKey = getBusinessKey();
    var radius = parseInt(radiusSelect.value, 10) || 3000;

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
  initMap();
  renderApiSourcesGuide();
  loadSavedFromStorage();
  Object.keys(calcInputs).forEach(function (key) {
    calcInputs[key].addEventListener("input", refreshCalculations);
  });
  form.addEventListener("submit", handleFormSubmit);
  geolocationBtn.addEventListener("click", handleGeolocation);
  saveCalcBtn.addEventListener("click", handleSaveCalculation);
  exportExcelBtn.addEventListener("click", exportCurrentToExcel);
  exportCompareBtn.addEventListener("click", exportComparisonToExcel);
  clearSavedBtn.addEventListener("click", handleClearSaved);
  printReportBtn.addEventListener("click", function () { window.print(); });
  copyReportBtn.addEventListener("click", function () {
    if (!currentAnalysis) return;
    navigator.clipboard.writeText(businessReport.innerText).then(function () {
      copyReportBtn.textContent = "Скопировано!";
      setTimeout(function () { copyReportBtn.textContent = "Копировать текст"; }, 2000);
    });
  });
})();
