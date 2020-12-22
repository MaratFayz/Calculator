let mainUrl = ""

if(process.env.NODE_ENV === "production") {
    mainUrl = process.env.VUE_APP_BASE_URL;
}

let urlWithScenarios = "/scenarios";
let urlWithLeasingDeposits = "/leasingDeposits";
let urlWithLeasingDepositsFor2Scenarios = urlWithLeasingDeposits + "/for2Scenarios";
let urlWithEntryRegLD1 = "/entries/regld1";
let urlWithEntryRegLD2 = "/entries/regld2";
let urlWithEntryRegLD3 = "/entries/regld3";
let urlWithEntryIFRSAcc = "/entriesIFRS";
let urlWithEntryIFRSAccFor2Scenarios = urlWithEntryIFRSAcc + "/forDate";
let urlWithEntryCalculator = mainUrl + "/entries/calculator"
let urlWithDownloadingReportForCalculation = mainUrl + "/excelReports/ld_regld1_2_3"

let GD_tabs_1Level = [{ "id": "generalData", "name": "Справочники" },
{ "id": "Entries", "name": "Проводки по депозитам" }]

let GD_tabs_2Level = [{ "id": "leasingdeposits", "name": "Перечень лизинговых депозитов" },
{ "id": "calculate", "name": "Расчет проводок за период" },
{ "id": "reg_ld_1", "name": "Форма Reg.LD.1" },
{ "id": "reg_ld_2", "name": "Форма Reg.LD.2" },
{ "id": "reg_ld_3", "name": "Форма Reg.LD.3" },
{ "id": "entriesifrs", "name": "Журнал МСФО" }]

let GD_spravochniki = [{ "id": "/currencies", "name": "Валюты" },
{ "id": "/companies", "name": "Компании" },
{ "id": "/counterpartners", "name": "Контрагенты" },
{ "id": "/depositRates", "name": "Ставки депозитов" },
{ "id": "/durations", "name": "Длительности депозитов" },
{ "id": "/endDates", "name": "Даты завершения депозитов" },
{ "id": "/entries", "name": "Проводки" },
{ "id": urlWithEntryIFRSAcc, "name": "Проводки на счетах МСФО" },
{ "id": "/exchangeRates", "name": "Курсы валют" },
{ "id": "/ifrsAccounts", "name": "Счета МСФО" },
{ "id": urlWithLeasingDeposits, "name": "Лизинговые депозиты" },
{ "id": "/periods", "name": "Периоды" },
{ "id": "/periodsClosed", "name": "Закрытие периодов" },
{ "id": urlWithScenarios, "name": "Сценарии" }]

let urlWithCurrencies = "/currencies";
let urlWithUsers = "/users";
let urlWithRoles = "/roles";
let urlWithAutoCreatePeriods = mainUrl + "/periods/autoCreatePeriods";
let urlWithAutoClosingPeriods = mainUrl + "/periodsClosed/autoClosingPeriods";
let urlWithImportExchangeCurrencies = mainUrl + "/exchangeRates/importERFromCBR";

let adminTabs = [{ "id": "users", "name": "Управление пользователями" },
{ "id": "roles", "name": "Управление ролями" },
{ "id": "exImport", "name": "Импорт курсов валют" },
{ "id": "autoCreatePeriods", "name": "Автоматическое создание периодов" },
{ "id": "autoClosingPeriods", "name": "Автоматическое закрытие периодов" }]

let adminSpravochniki = [{ "id": "/user", "name": "Пользователи" },
{ "id": "/roles", "name": "Роли" }]

export {
    GD_tabs_1Level, GD_tabs_2Level, GD_spravochniki,
    urlWithScenarios, urlWithLeasingDepositsFor2Scenarios,
    urlWithEntryRegLD1, urlWithEntryRegLD2, urlWithEntryRegLD3,
    urlWithEntryCalculator, mainUrl, urlWithEntryIFRSAccFor2Scenarios,
    urlWithCurrencies, urlWithUsers, urlWithRoles, urlWithAutoCreatePeriods,
    urlWithAutoClosingPeriods, urlWithImportExchangeCurrencies, adminTabs,
    adminSpravochniki,
    urlWithDownloadingReportForCalculation
}