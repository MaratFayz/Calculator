package LD.model.Period;

import LD.model.AbstractModelClass;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "period")
@Data
@NoArgsConstructor()
@Builder(toBuilder = true)
@AllArgsConstructor
public class Period extends AbstractModelClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, unique = true)
    private Long id;

    @Column(name = "date", nullable = false, unique = true)
    private LocalDate date;
}
