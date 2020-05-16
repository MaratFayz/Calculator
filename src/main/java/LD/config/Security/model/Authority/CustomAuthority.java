package LD.config.Security.model.Authority;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;

import javax.persistence.*;

@Entity
@Table(name = "authorities")
@NoArgsConstructor
@Data
@AllArgsConstructor
@Builder
public class CustomAuthority implements GrantedAuthority
{
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	@Column(nullable = false, unique = true)
	private String name;

	@Override
	public String getAuthority()
	{
		return this.name;
	}

	@Transient
	public static CustomAuthority[] customAuthorityPredefinedList =
	{
		CustomAuthority.builder().name(ALL_AUTHORITIES.USER_ADDER.name()).build(),
		CustomAuthority.builder().name(ALL_AUTHORITIES.USER_EDITOR.name()).build(),
		CustomAuthority.builder().name(ALL_AUTHORITIES.USER_DELETER.name()).build(),
		CustomAuthority.builder().name(ALL_AUTHORITIES.USER_READER.name()).build(),

		CustomAuthority.builder().name(ALL_AUTHORITIES.COMPANY_ADDER.name()).build(),
		CustomAuthority.builder().name(ALL_AUTHORITIES.COMPANY_EDITOR.name()).build(),
		CustomAuthority.builder().name(ALL_AUTHORITIES.COMPANY_DELETER.name()).build(),
		CustomAuthority.builder().name(ALL_AUTHORITIES.COMPANY_READER.name()).build(),

		CustomAuthority.builder().name(ALL_AUTHORITIES.COUNTERPARTNER_ADDER.name()).build(),
		CustomAuthority.builder().name(ALL_AUTHORITIES.COUNTERPARTNER_EDITOR.name()).build(),
		CustomAuthority.builder().name(ALL_AUTHORITIES.COUNTERPARTNER_DELETER.name()).build(),
		CustomAuthority.builder().name(ALL_AUTHORITIES.COUNTERPARTNER_READER.name()).build(),

		CustomAuthority.builder().name(ALL_AUTHORITIES.CURRENCY_ADDER.name()).build(),
		CustomAuthority.builder().name(ALL_AUTHORITIES.CURRENCY_EDITOR.name()).build(),
		CustomAuthority.builder().name(ALL_AUTHORITIES.CURRENCY_DELETER.name()).build(),
		CustomAuthority.builder().name(ALL_AUTHORITIES.CURRENCY_READER.name()).build(),

		CustomAuthority.builder().name(ALL_AUTHORITIES.DEPOSIT_RATES_ADDER.name()).build(),
		CustomAuthority.builder().name(ALL_AUTHORITIES.DEPOSIT_RATES_EDITOR.name()).build(),
		CustomAuthority.builder().name(ALL_AUTHORITIES.DEPOSIT_RATES_DELETER.name()).build(),
		CustomAuthority.builder().name(ALL_AUTHORITIES.DEPOSIT_RATES_READER.name()).build(),

		CustomAuthority.builder().name(ALL_AUTHORITIES.DURATION_ADDER.name()).build(),
		CustomAuthority.builder().name(ALL_AUTHORITIES.DURATION_EDITOR.name()).build(),
		CustomAuthority.builder().name(ALL_AUTHORITIES.DURATION_DELETER.name()).build(),
		CustomAuthority.builder().name(ALL_AUTHORITIES.DURATION_READER.name()).build(),

		CustomAuthority.builder().name(ALL_AUTHORITIES.END_DATE_ADDER.name()).build(),
		CustomAuthority.builder().name(ALL_AUTHORITIES.END_DATE_EDITOR.name()).build(),
		CustomAuthority.builder().name(ALL_AUTHORITIES.END_DATE_DELETER.name()).build(),
		CustomAuthority.builder().name(ALL_AUTHORITIES.END_DATE_READER.name()).build(),

		CustomAuthority.builder().name(ALL_AUTHORITIES.ENTRY_ADDER.name()).build(),
		CustomAuthority.builder().name(ALL_AUTHORITIES.ENTRY_EDITOR.name()).build(),
		CustomAuthority.builder().name(ALL_AUTHORITIES.ENTRY_DELETER.name()).build(),
		CustomAuthority.builder().name(ALL_AUTHORITIES.ENTRY_READER.name()).build(),

		CustomAuthority.builder().name(ALL_AUTHORITIES.ENTRY_IFRS_ADDER.name()).build(),
		CustomAuthority.builder().name(ALL_AUTHORITIES.ENTRY_IFRS_EDITOR.name()).build(),
		CustomAuthority.builder().name(ALL_AUTHORITIES.ENTRY_IFRS_DELETER.name()).build(),
		CustomAuthority.builder().name(ALL_AUTHORITIES.ENTRY_IFRS_READER.name()).build(),

		CustomAuthority.builder().name(ALL_AUTHORITIES.EXCHANGE_RATE_ADDER.name()).build(),
		CustomAuthority.builder().name(ALL_AUTHORITIES.EXCHANGE_RATE_EDITOR.name()).build(),
		CustomAuthority.builder().name(ALL_AUTHORITIES.EXCHANGE_RATE_DELETER.name()).build(),
		CustomAuthority.builder().name(ALL_AUTHORITIES.EXCHANGE_RATE_READER.name()).build(),

		CustomAuthority.builder().name(ALL_AUTHORITIES.IFRS_ACC_ADDER.name()).build(),
		CustomAuthority.builder().name(ALL_AUTHORITIES.IFRS_ACC_EDITOR.name()).build(),
		CustomAuthority.builder().name(ALL_AUTHORITIES.IFRS_ACC_DELETER.name()).build(),
		CustomAuthority.builder().name(ALL_AUTHORITIES.IFRS_ACC_READER.name()).build(),

		CustomAuthority.builder().name(ALL_AUTHORITIES.LEASING_DEPOSIT_ADDER.name()).build(),
		CustomAuthority.builder().name(ALL_AUTHORITIES.LEASING_DEPOSIT_EDITOR.name()).build(),
		CustomAuthority.builder().name(ALL_AUTHORITIES.LEASING_DEPOSIT_DELETER.name()).build(),
		CustomAuthority.builder().name(ALL_AUTHORITIES.LEASING_DEPOSIT_READER.name()).build(),

		CustomAuthority.builder().name(ALL_AUTHORITIES.PERIOD_ADDER.name()).build(),
		CustomAuthority.builder().name(ALL_AUTHORITIES.PERIOD_EDITOR.name()).build(),
		CustomAuthority.builder().name(ALL_AUTHORITIES.PERIOD_DELETER.name()).build(),
		CustomAuthority.builder().name(ALL_AUTHORITIES.PERIOD_READER.name()).build(),

		CustomAuthority.builder().name(ALL_AUTHORITIES.PERIODS_CLOSED_ADDER.name()).build(),
		CustomAuthority.builder().name(ALL_AUTHORITIES.PERIODS_CLOSED_EDITOR.name()).build(),
		CustomAuthority.builder().name(ALL_AUTHORITIES.PERIODS_CLOSED_DELETER.name()).build(),
		CustomAuthority.builder().name(ALL_AUTHORITIES.PERIODS_CLOSED_READER.name()).build(),

		CustomAuthority.builder().name(ALL_AUTHORITIES.SCENARIO_ADDER.name()).build(),
		CustomAuthority.builder().name(ALL_AUTHORITIES.SCENARIO_EDITOR.name()).build(),
		CustomAuthority.builder().name(ALL_AUTHORITIES.SCENARIO_DELETER.name()).build(),
		CustomAuthority.builder().name(ALL_AUTHORITIES.SCENARIO_READER.name()).build(),

		CustomAuthority.builder().name(ALL_AUTHORITIES.LOAD_EXCHANGE_RATE_FROM_CBR.name()).build(),
		CustomAuthority.builder().name(ALL_AUTHORITIES.AUTO_ADDING_PERIODS.name()).build(),
		CustomAuthority.builder().name(ALL_AUTHORITIES.AUTO_CLOSING_PERIODS.name()).build(),
		CustomAuthority.builder().name(ALL_AUTHORITIES.CALCULATE.name()).build()

	};
}