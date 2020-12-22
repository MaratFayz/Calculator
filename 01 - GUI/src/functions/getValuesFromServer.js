import { mainUrl } from "../generalData";

export default async function getValuesFromServer(url, params) {
    var finalUrl = mainUrl + url;
    console.log("getValuesFromServer => params = " + JSON.stringify(params));

    if (params != null) {
        finalUrl = finalUrl + "?";
        Object.keys(params).forEach(function (keyInParam) {
            console.log("getValuesFromServer => keyInParam = " + JSON.stringify(keyInParam));
            console.log("getValuesFromServer => params[keyInParam] = " + JSON.stringify(params[keyInParam]));
            finalUrl = finalUrl + keyInParam + "=" + params[keyInParam] + "&";
        });

        finalUrl = finalUrl.substring(0, finalUrl.length - 1);
    }

    let response = await fetch(finalUrl);
    console.log("getValuesFromServer => finalUrl = " + finalUrl);

    var commits;

    // axios.get('finalUrl');
    if (response.ok) {
        // если HTTP-статус в диапазоне 200-299
        // получаем тело ответа
        commits = await response.json(); // читаем ответ в формате json

        console.log("Статус ОК: В методе getValuesFromServer commits = ", commits);
    }
    else {
        console.log("Ошибка HTTP: " + response.status);

        commits = [];

        console.log("В методе getValuesFromServer commits = ", commits);
    }

    return commits;
}