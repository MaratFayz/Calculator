import FileSaver from "file-saver";

export default async function getFileFromServer(url, params) {
    var finalUrl = url;
    console.log("getFileFromServer => params = " + JSON.stringify(params));

    if (params != null) {
        finalUrl = finalUrl + "?";
        Object.keys(params).forEach(function (keyInParam) {
            console.log("getFileFromServer => keyInParam = " + JSON.stringify(keyInParam));
            console.log("getFileFromServer => params[keyInParam] = " + JSON.stringify(params[keyInParam]));
            finalUrl = finalUrl + keyInParam + "=" + params[keyInParam] + "&";
        });

        finalUrl = finalUrl.substring(0, finalUrl.length - 1);
    }

    console.log("getFileFromServer => finalUrl = " + finalUrl);

    let response = await fetch(finalUrl);

    console.log("response => " + response);
    console.log(response);

    if (response.ok) {
        let fileName = response.headers.get("FileName")
        console.log("fileName => " + fileName);

        let fileAsBlob = await response.blob();
        console.log(fileAsBlob)
        FileSaver.saveAs(fileAsBlob, fileName);
    }
    else {
        console.log("Ошибка HTTP: " + response.status);
    }

    return response;
}