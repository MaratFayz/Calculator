package LD.model.Counterpartner;

import LD.model.AbstractModelClass;
import lombok.*;

import javax.persistence.*;

@Entity
@Table(name = "Counterpartner")
@Getter
@Setter
@ToString
@NoArgsConstructor()
@Builder
@AllArgsConstructor
public class Counterpartner extends AbstractModelClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, unique = true)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;
}