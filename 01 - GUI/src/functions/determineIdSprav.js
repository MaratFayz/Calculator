import { store } from "../store"

function determineIdSprav(value, allValues) {
    let value_id;
    let index;

    for (index = 0; index < allValues.length; ++index) {
        console.log("value.id = " + allValues[index].id);
        console.log("value = ")
        console.log("value.name = " + allValues[index].name);
        console.log(value)

        if (value == allValues[index].name)
            value_id = allValues[index].id;
    }

    console.log("value_id = " + value_id)
    return value_id;
}

export function determineScenarioId(scenario) {
    let allScenarios = store.getters.getScenarios;

    let scenarioId = determineIdSprav(scenario, allScenarios);
    return scenarioId
}