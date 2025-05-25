package br.edu.atitus.product_service.clients;

import org.springframework.stereotype.Component;

@Component
public class CurrencyFallback implements CurrencyClient{

	@Override
	public CurrencyResponse getCurrency(double value, String source, String target) {
		CurrencyResponse fallback = new CurrencyResponse();
		fallback.setConvertedValue(-1);
		return fallback;
	}

}
