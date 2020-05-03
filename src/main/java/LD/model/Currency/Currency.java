package LD.model.Currency;

import lombok.*;

import javax.persistence.*;

@Table(name = "Currency")
@Entity
@Getter
@Setter
@ToString
@NoArgsConstructor()
@Builder
@AllArgsConstructor
public class Currency
{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false, unique = true)
	private Long id;

	@Column(name = "short_name", nullable = false)
	private String short_name;
}