import { determineScenarioId } from "./determineIdSprav.js"
import { mainUrl } from "../generalData";

export default async function getFirstOpenDateInScenario(selectedScenario) {
  console.log("getFirstOpenDateInScenario => selectedScenario = " + JSON.stringify(selectedScenario));
  let selectedScenario_id;

  if (selectedScenario != null) {
    selectedScenario_id = determineScenarioId(selectedScenario);

    console.log("'/periodsClosed' + selectedScenario_id = " + '/periodsClosed/' + selectedScenario_id);

    let url = mainUrl + '/periodsClosed/' + selectedScenario_id;

    var response = await fetch(url);
    var closedPeriod = "";

    if (response.ok) { // если HTTP-статус в диапазоне 200-299
      // получаем тело ответа (см. про этот метод ниже)
      closedPeriod = await response.text(); // читаем ответ в формате text
      console.log("closedPeriod: " + closedPeriod);

    }
    else {
      console.log("Ошибка HTTP: " + response.status);
    }

    console.log("closedPeriod = " + closedPeriod);

    return closedPeriod;
  }
}