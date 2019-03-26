package org.reactiveminds.blocnet.api;

import org.reactiveminds.blocnet.model.BlockData;
@FunctionalInterface
public interface BlocCommitListener {
	void onCommit(BlockData block);
}
