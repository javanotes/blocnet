package org.reactiveminds.blocnet.api;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;

import org.reactiveminds.blocnet.api.FileStorageService.FileContent;
import org.reactiveminds.blocnet.dto.AddRequest;
import org.reactiveminds.blocnet.dto.AddTxnResponse;
import org.reactiveminds.blocnet.dto.GetBlockResponse;
import org.reactiveminds.blocnet.dto.GetRawResponse;
import org.reactiveminds.blocnet.dto.GetTxnResponse;
import org.reactiveminds.blocnet.dto.Response;
import org.reactiveminds.blocnet.dto.TxnRequest;
import org.reactiveminds.blocnet.utils.SerdeUtil;
import org.reactiveminds.blocnet.utils.err.IllegalLinkException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@ConditionalOnProperty(name = "blocnet.miner", havingValue="false", matchIfMissing=true)
@RestController
public class RestApi {

	public static final String DEFAULT_CHAIN = "blocnet";
	private static Logger log = LoggerFactory.getLogger("RestApi");
	@Autowired
	BlocService service;
	@Autowired
	FileStorageService fileStore;
	
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
		rep.setChainId(name);
		try 
		{
			TxnRequest txn = service.fetchTransaction(name, txnid);
			resp.setStatus(HttpStatus.NOT_FOUND.value());
			rep.setStatus(Response.NOT_FOUND);
			
			if(!txn.isRaw() && StringUtils.hasText(txn.getRequest())) {
				resp.setStatus(HttpStatus.OK.value());
				rep.setBody(txn.getRequest());
				rep.setChainId(name);
				rep.setStatus(Response.OK);
				rep.setTxnId(txn.getTxnId());
			}
			
		} catch (IllegalLinkException e) {
			log.error(e.toString());
			resp.setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
			rep.setStatus(Response.INVALID_BLOCKCHAIN);
		}
		//rep.setBody(SerdeUtil.toJson(rep.getBody()));
		return rep;
	}
	@GetMapping("/chain/{name}/{txnid}/raw")
	public GetRawResponse getTransactionRaw(@PathVariable String name, @PathVariable String txnid, HttpServletResponse resp) {
		GetRawResponse rep = new GetRawResponse();
		rep.setTxnId(txnid);
		
		try 
		{
			getRaw(rep, name, txnid);
			
		} catch (IllegalLinkException e) {
			log.error(e.toString());
			resp.setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
			rep.setStatus(Response.INVALID_BLOCKCHAIN);
		}
		
		return rep;
	}
	private GetRawResponse getRaw(final GetRawResponse rep, String name, String txnid) {
		try 
		{
			TxnRequest txn = service.fetchTransaction(name, txnid);
			rep.setTxnId(txn.getTxnId());
			rep.setStatus(Response.NOT_FOUND);
			
			if(txn.isRaw()) {
				rep.setBody(txn.getRequestRaw());
				rep.setChainId(name);
				rep.setStatus(Response.OK);
			}
			
		} catch (IllegalLinkException e) {
			throw e;
		}
		return rep;
	}
	@GetMapping("/chain/{name}/{txnid}/file")
	public ResponseEntity<Resource> getTransactionFile(@PathVariable String name, @PathVariable String txnid, HttpServletResponse resp) {
		GetRawResponse rep = new GetRawResponse();
		rep.setTxnId(txnid);
		
		try 
		{
			getRaw(rep, name, txnid);
			
			if (rep.getStatus() == Response.OK) {
				FileContent content = FileContent.decode(rep.getBody());
				return ResponseEntity.ok().contentType(MediaType.parseMediaType(content.contentType))
						.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + content.name + "\"")
						.body(new ByteArrayResource(content.bytes));
			}
			
		} catch (IllegalLinkException e) {
			log.error(e.toString());
			rep.setStatus(Response.INVALID_BLOCKCHAIN);
		}
		catch (Exception e) {
			log.error(e.toString(), e);
			rep.setStatus(Response.ERROR);
		}

		return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ByteArrayResource(rep.getStatus().name().getBytes(StandardCharsets.UTF_8)));
    }
	@PostMapping("/chain/{name}")
	public AddTxnResponse postTransaction(@PathVariable String name, @RequestBody String payload, HttpServletResponse resp) {
		String txnid = service.addTransaction(new AddRequest(SerdeUtil.toJson(payload), name));
		AddTxnResponse trep = new AddTxnResponse();
		trep.setChainId(name);
		trep.setStatus(Response.TXN_ACCEPT);
		trep.setTxnId(txnid);
		return trep;
	}
	@PostMapping("/chain/{name}/raw")
	public AddTxnResponse postTransactionRaw(@PathVariable String name, @RequestBody byte[] payload, HttpServletResponse resp) {
		String txnid = service.addTransaction(payload, name);
		AddTxnResponse trep = new AddTxnResponse();
		trep.setChainId(name);
		trep.setStatus(Response.TXN_ACCEPT);
		trep.setTxnId(txnid);
		return trep;
	}
	
	//@PostMapping("/chain/{name}/file")
	public AddTxnResponse postTransactionFile(@PathVariable String name, @RequestParam("file") MultipartFile file, HttpServletResponse resp) {
		
		FileContent bytes = fileStore.getContent(file);
		byte[] encoded = bytes.encode();
		AddTxnResponse trep = postTransactionRaw(name, encoded, resp);
		
		log.info(bytes+", compressed: "+encoded.length);
		return trep;
		
	}
	@PostMapping("/chain")
	public AddTxnResponse postTransaction(@RequestBody String payload, HttpServletResponse resp) {
		return postTransaction(DEFAULT_CHAIN, payload, resp);
	}
}
