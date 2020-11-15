package LD.model.Duration;

import LD.model.AbstractModelClass;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name = "Duration")
@Data
@NoArgsConstructor()
@Builder
@AllArgsConstructor
public class Duration extends AbstractModelClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, unique = true)
    private Long id;

    @Column(name = "MIN_MONTH", nullable = false)
    private int MIN_MONTH;

    @Column(name = "MAX_MONTH", nullable = false)
    private int MAX_MONTH;

    @Column(name = "name", nullable = false)
    private String name;
}
