package br.edu.atitus.product_service.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Fallback;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.edu.atitus.product_service.clients.CurrencyClient;
import br.edu.atitus.product_service.clients.CurrencyResponse;
import br.edu.atitus.product_service.entities.ProductEntity;
import br.edu.atitus.product_service.repositories.ProductRepository;

@RestController
@RequestMapping("products")
public class OpenProductController {
	
	private final ProductRepository repository;
	private final CurrencyClient currencyClient;
	private final CacheManager cacheManager;

	public OpenProductController(ProductRepository repository, CurrencyClient currencyClient, CacheManager cacheManager) {
		super();
		this.repository = repository;
		this.currencyClient = currencyClient;
		this.cacheManager = cacheManager;
	}
	
	@Value("${server.port}")
	private int serverPort;
	
	@GetMapping("/{idProduct}/{targetCurrency}")
	public ResponseEntity<ProductEntity> getProduct(
			@PathVariable Long idProduct,
			@PathVariable String targetCurrency
			) throws Exception{
		
		String nameCache = "Product";
		String keyCache = idProduct + targetCurrency;
		
		ProductEntity product = cacheManager.getCache(nameCache).get(keyCache, ProductEntity.class);
		
        String baseEnvironment = "Product-service running on port: " + serverPort;
		
		if (product != null) {
            product.setEnvironment(baseEnvironment + " - Origin: Cache");
		} else {
			product = repository.findById(idProduct)
                    .orElseThrow(() -> new Exception("Product Unsupported"));
			if (product.getCurrency().equals(targetCurrency)) {
				product.setConvertedPrice(product.getPrice());
                product.setEnvironment(baseEnvironment + " - No conversion needed");
				cacheManager.getCache(nameCache).put(keyCache, product);
			} else {
				CurrencyResponse currency = currencyClient.getCurrency(product.getPrice(),product.getCurrency(),targetCurrency);
				if (currency.getConvertedValue() != -1) {
					product.setConvertedPrice(currency.getConvertedValue());
                    product.setEnvironment(baseEnvironment + " - Converted by " + currency.getEnviroment());
    				cacheManager.getCache(nameCache).put(keyCache, product);
				} else {
					product.setConvertedPrice(product.getPrice());
                    product.setEnvironment(baseEnvironment + " - Conversion Failed/Fallback");
				}
				
			}
		}
		
		return ResponseEntity.ok(product);
	}

}
