package LD.model.Currency;

import LD.model.AbstractModelClass;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Table(name = "Currency")
@Entity
@Data
@NoArgsConstructor()
@Builder
@AllArgsConstructor
public class Currency extends AbstractModelClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, unique = true)
    private Long id;

    @Column(name = "short_name", nullable = false, length = 3)
    private String short_name;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "CBRCurrencyCode", length = 6)
    private String CBRCurrencyCode;
}