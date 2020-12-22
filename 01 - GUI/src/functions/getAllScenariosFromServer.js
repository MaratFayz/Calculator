import getValuesFromServer from "./getValuesFromServer.js";
import { urlWithScenarios } from "../generalData.js";
import { store } from "../store"

export default function getAllScenariosFromServer() {
    console.log("Call")

    let allScenarios = [];
    let allScenariosNames = [];

    // console.log(
    //     "В методе created: => до присвоения значения this.allScenarios = ",
    //     this.allScenarios
    // );

    var promise = getValuesFromServer(urlWithScenarios, null);

    promise.then((scenarios) => {
        console.log("В методе created: => scenarios после запроса = ", scenarios);

        scenarios.forEach((e) => allScenarios.push(e));
        scenarios.forEach((e) => allScenariosNames.push(e.name));

        console.log(
            "В методе created: => после присвоения значения allScenarios = ",
            allScenarios
        );

        store.dispatch('saveAllScenarios', { allScenarios, allScenariosNames });
    });
}