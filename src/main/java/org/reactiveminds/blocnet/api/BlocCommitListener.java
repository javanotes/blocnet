package org.reactiveminds.blocnet.api;

import org.reactiveminds.blocnet.model.BlockData;
/**
 * Callback interface for block commit event
 * @author Sutanu_Dalui
 *
 */
@FunctionalInterface
public interface BlocCommitListener {
	void onCommit(BlockData block);
}
