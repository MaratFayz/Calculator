package LD.model.PeriodsClosed;

import LD.model.AbstractModelClass;
import LD.model.Enums.STATUS_X;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import javax.persistence.*;

@Entity
@Table(name = "PeriodsClosed")
@Data
@NoArgsConstructor()
@Builder
@AllArgsConstructor
public class PeriodsClosed extends AbstractModelClass {

    @EmbeddedId
    private PeriodsClosedID periodsClosedID;

    @Enumerated(value = EnumType.STRING)
    @Type(type = "pgsql_enum")
    private STATUS_X ISCLOSED;
}
