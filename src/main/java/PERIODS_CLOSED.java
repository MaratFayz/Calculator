import lombok.Builder;
import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Table;

@Data
@Builder(toBuilder = true)
@Entity
@Table(name = "PERIODS_CLOSED")
public class PERIODS_CLOSED
{
}
