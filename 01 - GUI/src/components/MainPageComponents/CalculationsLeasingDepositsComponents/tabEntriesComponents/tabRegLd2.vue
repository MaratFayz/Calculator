<template>
  <div>
    <ShowDataForTwoScenarios
      :url="urlWithEntryRegLD2"
      :stringForNoValues="`Рассчитанные проводки отсутствуют`"
    />
  </div>
</template>

<script>
import { urlWithEntryRegLD2 } from "../../../../generalData.js";
import ShowDataForTwoScenarios from "./Common/ShowDataForTwoScenarios";

export default {
  name: "tabRegLd2",

  components: {
    ShowDataForTwoScenarios,
  },

  data: function () {
    return {
      urlWithEntryRegLD2: urlWithEntryRegLD2,
    };
  },
};
</script>

<style scoped>
</style>