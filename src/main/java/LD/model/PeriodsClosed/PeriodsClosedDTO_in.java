package LD.model.PeriodsClosed;

import LD.model.Enums.STATUS_X;
import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PeriodsClosedDTO_in
{
	private Long scenario;
	private Long period;

	private STATUS_X ISCLOSED;
}

