package LD.model.Company;

import LD.model.AbstractModelClass;
import lombok.*;

import javax.persistence.*;

@Entity
@Table(name = "Company")
@Getter
@Setter
@ToString
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class Company extends AbstractModelClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, unique = true)
    private Long id;

    @Column(length = 10, name = "code", nullable = false)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;
}
