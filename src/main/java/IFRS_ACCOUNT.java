import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
@NoArgsConstructor
@Setter
@Getter
@AllArgsConstructor
public class IFRS_ACCOUNT
{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	private String account_code;
	private String account_name;
	private String flow_code;
	private String flow_name;

	private String SH;
	private String PA;
	private String CT;
	private String DR;

	private boolean isInverseSum;

	private String MappingFormAndColumn;
}
