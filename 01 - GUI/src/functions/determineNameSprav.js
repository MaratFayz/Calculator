import { GD_spravochniki } from "../generalData.js"

export default function determineNameSprav(urlToDetermineName) {
    var index;
    var SpravName;
    for (index = 0; index < GD_spravochniki.length; ++index) {
        console.log("GD_spravochniki.id = " + GD_spravochniki[index].id);
        console.log("GD_spravochniki.name = " + GD_spravochniki[index].name);

        console.log("urlToDetermineName.url = " + urlToDetermineName.url);

        if (urlToDetermineName.url == GD_spravochniki[index].id)
            SpravName = GD_spravochniki[index].name;
    }

    return SpravName;
}