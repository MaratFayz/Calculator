package Utils.Entities;

import lombok.Data;

@Data
public class IfrsAccountTestData {

    private Long id;

    private String account_code;
    private String account_name;
    private String flow_code;
    private String flow_name;

    private String sh;
    private String pa;
    private String ct;
    private String dr;

    private boolean isInverseSum;

    private String mappingFormAndColumn;

    private Long userCode;
    private String lastChange;
}
