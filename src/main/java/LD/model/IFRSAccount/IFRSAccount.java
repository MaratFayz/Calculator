package LD.model.IFRSAccount;

import LD.config.Security.model.User.User;
import lombok.*;

import javax.persistence.*;
import java.time.ZonedDateTime;

@Entity
@Table(name = "ifrs_account")
@NoArgsConstructor
@Data
@AllArgsConstructor
@Builder
public class IFRSAccount
{
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

	@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	@JoinColumn(nullable = false)
	private User user;

	@Column(name = "DateTime_lastChange", nullable = false)
	private ZonedDateTime lastChange;
}
