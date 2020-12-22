<template>
  <div>
    <ShowDataForTwoScenarios
      :url="urlWithEntryRegLD1"
      :stringForNoValues="`Рассчитанные проводки отсутствуют`"
    />
  </div>
</template>

<script>
import { urlWithEntryRegLD1 } from "../../../../generalData.js";
import ShowDataForTwoScenarios from "./Common/ShowDataForTwoScenarios";

export default {
  name: "tabRegLd1",

  components: {
    ShowDataForTwoScenarios,
  },

  data: function () {
    return {
      urlWithEntryRegLD1: urlWithEntryRegLD1,
    };
  },
};
</script>

<style scoped>
</style>