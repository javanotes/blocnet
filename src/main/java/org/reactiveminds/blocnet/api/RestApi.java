package org.reactiveminds.blocnet.api;

import java.util.Collections;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;

import org.reactiveminds.blocnet.dto.AddRequest;
import org.reactiveminds.blocnet.dto.AddTxnResponse;
import org.reactiveminds.blocnet.dto.GetBlockResponse;
import org.reactiveminds.blocnet.dto.GetTxnResponse;
import org.reactiveminds.blocnet.dto.Response;
import org.reactiveminds.blocnet.dto.TxnRequest;
import org.reactiveminds.blocnet.utils.err.IllegalLinkException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@ConditionalOnProperty(name = "blocnet.miner", havingValue="false", matchIfMissing=true)
@RestController
public class RestApi {

	public static final String DEFAULT_CHAIN = "blocnet";
	private static Logger log = LoggerFactory.getLogger("RestApi");
	@Autowired
	BlocService service;
	@PostConstruct
	private void onStart() {
		log.debug("rest api loaded");
	}
	
	@GetMapping("/chain/{name}")
	public GetBlockResponse getChain(@PathVariable String name, HttpServletResponse resp) {
		try {
			return service.getChain(name);
		} catch (IllegalLinkException e) {
			log.error(e.toString());
			resp.setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
			GetBlockResponse get = new GetBlockResponse(Collections.emptyList());
			get.setStatus(Response.INVALID_BLOCKCHAIN);
			return get;
		}
	}
	@GetMapping("/chain")
	public GetBlockResponse getChain(HttpServletResponse resp) {
		return getChain(DEFAULT_CHAIN, resp);
	}
	@GetMapping("/chain/{name}/{txnid}")
	public GetTxnResponse getTransaction(@PathVariable String name, @PathVariable String txnid, HttpServletResponse resp) {
		GetTxnResponse rep = new GetTxnResponse();
		rep.setTxnId(txnid);
		
		try 
		{
			TxnRequest txn = service.fetchTransaction(name, txnid);
			resp.setStatus(HttpStatus.NOT_FOUND.value());
			rep.setTxnId(txn.getTxnId());
			rep.setStatus(Response.NOT_FOUND);
			
			if(StringUtils.hasText(txn.getRequest())) {
				resp.setStatus(HttpStatus.OK.value());
				rep.setBody(txn.getRequest());
				rep.setChainId(name);
				rep.setStatus(Response.OK);
			}
			
		} catch (IllegalLinkException e) {
			log.error(e.toString());
			resp.setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
			rep.setStatus(Response.INVALID_BLOCKCHAIN);
		}
		
		return rep;
	}
	@PostMapping("/chain/{name}")
	public AddTxnResponse postTransaction(@PathVariable String name, @RequestBody String payload, HttpServletResponse resp) {
		String txnid = service.addTransaction(new AddRequest(payload, name));
		AddTxnResponse trep = new AddTxnResponse();
		trep.setChainId(name);
		trep.setStatus(Response.TXN_ACCEPT);
		trep.setTxnId(txnid);
		return trep;
	}
	@PostMapping("/chain")
	public AddTxnResponse postTransaction(@RequestBody String payload, HttpServletResponse resp) {
		return postTransaction(DEFAULT_CHAIN, payload, resp);
	}
}
