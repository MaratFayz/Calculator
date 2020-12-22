<template>
  <div>
    <ShowDataForTwoScenarios
      :url="urlWithEntryRegLD3"
      :stringForNoValues="`Рассчитанные проводки отсутствуют`"
    />
  </div>
</template>

<script>
import { urlWithEntryRegLD3 } from "../../../../generalData.js";
import ShowDataForTwoScenarios from "./Common/ShowDataForTwoScenarios";

export default {
  name: "tabRegLd3",

  components: {
    ShowDataForTwoScenarios,
  },

  data: function () {
    return {
      urlWithEntryRegLD3: urlWithEntryRegLD3
    };
  },
};
</script>

<style scoped>
</style>