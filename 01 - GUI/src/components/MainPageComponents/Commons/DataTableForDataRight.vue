<template>
  <v-container fluid>
    <div>
      <v-btn
        color="primary"
        dark
        v-if="showingData.length > 0"
        @click="showAddForm(urlToDetermineName)"
      >
        Добавить значение
      </v-btn>
    </div>
    <div>
      <InputFormForSpravochniki
        :showingKeys="showingKeys"
        :showForm="showFormToAdd"
        :spravochnik_name="spravochnik_name"
        :url="urlToDetermineName.url"
        :method="`POST`"
        :actionName="`Добавление`"
        :dataInForm="{}"
        v-on:hideForm="hideForm()"
        v-on:refreshDataToView="refreshDataRight($event)"
      />
    </div>
    <div>
      <InputFormForSpravochniki
        :showingKeys="showingKeys"
        :showForm="showFormToEdit"
        :spravochnik_name="spravochnik_name"
        :url="urlToDetermineName.url"
        :method="`PUT`"
        :actionName="`Изменение`"
        :dataInForm="showingDataInForm"
        v-on:hideForm="hideForm()"
        v-on:refreshDataToView="refreshDataRight($event)"
      />
    </div>
    <div>
      <DataTable
        :showButtonsForEditAndDelete="true"
        :showingData="showingData"
        :showingKeys="showingKeys"
        :urlToDetermineName="urlToDetermineName"
        :spravochnik_name="spravochnik_name"
        :stringForNoValues="`Для отображения значений выберите справочник. \n  
                                    Если справочник выбран, то значения в справочнике отсутствуют`"
        @updateFormToShow="changeData($event)"
        @refreshDataToView="refreshDataRight($event)"
      />
    </div>
  </v-container>
</template>

<script>
import InputFormForSpravochniki from "./InputFormForSpravochniki";
import DataTable from "./DataTable.vue";
import determineNameSprav from "../../../functions/determineNameSprav.js";

export default {
  name: "DataTableForDataRight",

  components: {
    InputFormForSpravochniki,
    DataTable,
  },

  props: ["showingData", "showingKeys", "urlToDetermineName"],
  data: function () {
    return {
      showFormToAdd: false,
      showFormToEdit: false,
      showingDataInForm: {},
      spravochnik_name: "",
    };
  },
  methods: {
    refreshDataRight: function (url) {
      console.log("Method refreshDataRight");
      this.$emit("refreshDataToView", url);
      console.log("Method refreshDataRight");
    },
    showAddForm: function (urlToDetermineName) {
      console.log(
        "В методе showAddForm, this.showFormToAdd до присвоения значения = " +
          this.showFormToAdd
      );

      this.showFormToAdd = true;

      console.log(
        "В методе showAddForm, this.showFormToAdd после присвоения значения = " +
          this.showFormToAdd
      );

      this.spravochnik_name = determineNameSprav(urlToDetermineName);
    },
    hideForm: function () {
      this.showFormToAdd = false;
      this.showFormToEdit = false;
      this.showingDataInForm = {};
    },
    changeData: function (dataForUpdating) {
      console.log(
        "В методе changeData data = " + JSON.stringify(dataForUpdating.data)
      );
      console.log(
        "В методе changeData showingKeys = " + dataForUpdating.showingKeys
      );

      console.log(
        "В методе changeData, this.showingDataInForm до присвоения значения = " +
          JSON.stringify(this.showingDataInForm)
      );
      this.showingDataInForm = dataForUpdating.data;
      console.log(
        "В методе changeData, this.showingDataInForm после присвоения значения = " +
          JSON.stringify(this.showingDataInForm)
      );

      this.spravochnik_name = determineNameSprav(
        dataForUpdating.urlToDetermineName
      );

      console.log(
        "В методе changeData, this.showFormToEdit до присвоения значения = " +
          JSON.stringify(this.showFormToEdit)
      );
      this.showFormToEdit = true;
      console.log(
        "В методе changeData, this.showFormToEdit после присвоения значения = " +
          JSON.stringify(this.showFormToEdit)
      );
    },
  },
};
</script>

<style scoped>
</style>