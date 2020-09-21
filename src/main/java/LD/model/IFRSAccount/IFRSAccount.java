package LD.model.IFRSAccount;

import LD.model.AbstractModelClass;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name = "ifrs_account")
@NoArgsConstructor
@Data
@AllArgsConstructor
@Builder
public class IFRSAccount extends AbstractModelClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 40)
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
}
