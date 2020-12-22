<template>
  <div>
    <ShowDataForTwoScenarios
      :url="urlWithEntryIFRSAccFor2Scenarios"
      :stringForNoValues="`Рассчитанные проводки отсутствуют`"
    />
  </div>
</template>

<script>
import { urlWithEntryIFRSAccFor2Scenarios } from "../../../../generalData.js";
import ShowDataForTwoScenarios from "./Common/ShowDataForTwoScenarios";

export default {
  name: "tabEntriesIfrs",

  components: {
    ShowDataForTwoScenarios,
  },

  data: function () {
    return {
      urlWithEntryIFRSAccFor2Scenarios: urlWithEntryIFRSAccFor2Scenarios,
    };
  },
};
</script>

<style scoped>
</style>