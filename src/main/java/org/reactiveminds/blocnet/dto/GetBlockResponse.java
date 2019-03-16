package org.reactiveminds.blocnet.dto;

import java.util.ArrayList;
import java.util.List;

import org.reactiveminds.blocnet.model.Block;

public class GetBlockResponse {
	
	private List<Block> blocks;
	private Response status = Response.OK;
	public GetBlockResponse(List<Block> blocks) {
		this.blocks = new ArrayList<>(blocks.size());
		this.blocks.addAll(blocks);
	}

	public List<Block> getBlocks() {
		return blocks;
	}

	public void setBlocks(List<Block> blocks) {
		this.blocks = blocks;
	}

	public Response getStatus() {
		return status;
	}

	public void setStatus(Response status) {
		this.status = status;
	}

}
