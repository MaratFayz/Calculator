<template>
  <v-container fluid>
    <v-subheader> Выберите файл ПСД для загрузки в систему </v-subheader>
    <v-col class="d-flex" cols="12" sm="4">
      <v-file-input
        small-chips
        counter
        show-size
        truncate-length="50"
        v-model="fileToUpload"
        accept=".xls,.xlsx,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet,application/vnd.ms-excel"
      ></v-file-input>
    </v-col>
    <v-btn color="blue-grey" class="ma-2 white--text" @click="clickUpload">
      Загрузить
      <v-icon right dark> mdi-cloud-upload </v-icon>
    </v-btn>
  </v-container>
</template>

<script>
import { mainUrl } from "../../generalData.js";
import axios from "axios";

export default {
  name: "FileExcelUploadPsd",
  data: function () {
    return {
      fileToUpload: {},
    };
  },
  methods: {
    clickUpload: function () {
      console.log("Clicked");
      console.log(this.fileToUpload);

      let formData = new FormData();
      formData.append("file", this.fileToUpload);

      axios
        .post(mainUrl + "/upload", formData, {
          headers: {
            "Content-Type": "multipart/form-data; boundary=END---boundary",
          },
        })
        .then((response) => {
          console.log("Success!");
          console.log({ response });
        })
        .catch((error) => {
          console.log({ error });
        });
    },
  },
};
</script>

<style scoped>
</style>