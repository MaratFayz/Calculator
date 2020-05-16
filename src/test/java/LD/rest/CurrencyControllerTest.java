package LD.rest;

import LD.config.Security.model.Authority.ALL_AUTHORITIES;
import LD.model.Currency.Currency_out;
import LD.service.CurrencyService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(CurrencyController.class)
public class CurrencyControllerTest
{
	@Autowired
	private MockMvc mvc;
	@MockBean
	private UserDetailsService userDetailsService;
	@Autowired
	private WebApplicationContext context;
	@MockBean
	private CurrencyService currencyService;

	private ArrayList<Currency_out> currencyList;
	Currency_out RUB;
	Currency_out USD;
	Currency_out EUR;

	@Before
	public void setup() {
		mvc = MockMvcBuilders
				.webAppContextSetup(context)
				.apply(springSecurity())
				.build();
	}

	@Before
	public void createListOfValues()
	{
		this.currencyList = new ArrayList<>();
		RUB = Currency_out.builder().short_name("RUB").build();
		USD = Currency_out.builder().short_name("USD").build();
		EUR = Currency_out.builder().short_name("EUR").build();

		this.currencyList.add(RUB);
		this.currencyList.add(USD);
		this.currencyList.add(EUR);
	}

	@Test
	@WithMockUser(authorities = "CURRENCY_READER")
	public void return_AllValues_when_getRequest() throws Exception
	{
		when(currencyService.getAllCurrencies()).thenReturn(this.currencyList);

		mvc.perform(MockMvcRequestBuilders.get("/currencies")
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(3)));
//				.andExpect((ResultMatcher) jsonPath("$[0].short_name", is(RUB.getShort_name())))
//				.andExpect((ResultMatcher) jsonPath("$[1].short_name", is(USD.getShort_name())))
//				.andExpect((ResultMatcher) jsonPath("$[2].short_name", is(EUR.getShort_name())));
	}

	@Test
	@WithMockUser(authorities = "CURRENCY_READER")
	public void return_access_denied_when_deleteRequest() throws Exception
	{
		mvc.perform(MockMvcRequestBuilders.delete("/currencies/1")
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().is(404));
	}
}
