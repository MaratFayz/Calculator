<template>
  <div>
    <v-simple-table
      border="1"
      v-if="showingData.length > 0"
      align="left"
      valign="top"
    >
      <thead>
        <tr align="left" valign="top">
          <th v-for="key in showingKeys" :key="key">
            {{ key }}
          </th>
        </tr>
      </thead>
      <tbody>
        <tr
          v-for="data in showingData"
          :key="data.id"
          align="left"
          valign="top"
        >
          <td v-for="key1 in showingKeys" :key="key1">
            {{ data[key1] }}
          </td>
          <td v-if="showButtonsForEditAndDelete == true">
            <v-btn
              v-bind:value="data.id"
              class="editButton"
              @click="changeData(data, showingKeys, urlToDetermineName)"
            >
              <v-icon> mdi-pencil </v-icon>
            </v-btn>
          </td>
          <td v-if="showButtonsForEditAndDelete == true">
            <v-btn
              v-bind:value="data.id"
              class="deleteButton"
              @click="deleteData(data)"
            >
              <v-icon> mdi-delete </v-icon>
            </v-btn>
          </td>
        </tr>
      </tbody>
    </v-simple-table>
    <v-simple-table v-else>
      <v-alert type="error">
        {{ stringForNoValues }}
      </v-alert>
    </v-simple-table>
  </div>
</template>

<script>
export default {
  name: "DataTable",

  props: [
    "showButtonsForEditAndDelete",
    "showingData",
    "showingKeys",
    "urlToDetermineName",
    "stringForNoValues",
    "spravochnik_name",
  ],
  methods: {
    changeData: function (data, showingKeys, urlToDetermineName) {
      console.log("В методе changeData data = " + JSON.stringify(data));
      console.log("В методе changeData showingKeys = " + showingKeys);
      console.log(
        "В методе changeData urlToDetermineName = " + urlToDetermineName
      );

      var dataForUpdating = {};
      dataForUpdating.data = data;
      dataForUpdating.urlToDetermineName = urlToDetermineName;
      dataForUpdating.showingKeys = showingKeys;

      console.log(
        "В методе changeData был сформирован dataForUpdating = " +
          JSON.stringify(dataForUpdating)
      );

      this.$emit("updateFormToShow", dataForUpdating);
    },
    deleteData: async function (data) {
      var finalurl = this.urlToDetermineName.url;
      if (data.id != null) finalurl = finalurl + "/" + data.id;
      console.log("В методе deleteData finalurl = " + finalurl);

      await fetch(finalurl, {
        method: "DELETE",
        headers: {
          "Content-Type": "application/json;charset=utf-8",
        },
        body: JSON.stringify(data),
      });

      this.$emit("refreshDataToView", this.urlToDetermineName.url);
    },
  },
};
</script>

<style scoped>
</style>