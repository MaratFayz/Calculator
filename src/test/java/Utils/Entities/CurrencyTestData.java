package Utils.Entities;

import lombok.Data;

@Data
public class CurrencyTestData {

    Long id;
    String name;
    String short_name;
    String cbrcurrencyCode;
    Long userCode;
    String lastChange;
}
