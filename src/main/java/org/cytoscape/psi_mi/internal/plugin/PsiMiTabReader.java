package org.cytoscape.psi_mi.internal.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.cytoscape.io.read.CyNetworkReader;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.property.CyProperty;
import org.cytoscape.view.layout.CyLayoutAlgorithm;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;

public class PsiMiTabReader extends AbstractTask implements CyNetworkReader {

	private InputStream inputStream;

	private final CyNetworkViewFactory cyNetworkViewFactory;
	
	private final CyLayoutAlgorithmManager layouts;

	private final PsiMiTabParser parser;
	
	private CyNetwork network;
	
	private TaskMonitor parentTaskMonitor;
	
	private final CyProperty<Properties> prop;

	public PsiMiTabReader(InputStream is,
			CyNetworkViewFactory cyNetworkViewFactory,
			CyNetworkFactory cyNetworkFactory, final CyLayoutAlgorithmManager layouts, final CyProperty<Properties> prop) {
		if (is == null)
			throw new NullPointerException("Input stream is null");
		this.inputStream = is;
		this.cyNetworkViewFactory = cyNetworkViewFactory;
		this.layouts = layouts;
		this.prop = prop;

		parser = new PsiMiTabParser(is, cyNetworkFactory);
	}

	@Override
	public void run(TaskMonitor taskMonitor) throws Exception {
		this.parentTaskMonitor = taskMonitor;
		try {
			createNetwork(taskMonitor);
		} finally {
			if (inputStream != null) {
				inputStream.close();
				inputStream = null;
			}
		}
	}

	private void createNetwork(TaskMonitor taskMonitor) throws IOException {
		
		taskMonitor.setProgress(0.0);
		
		network = parser.parse();

		taskMonitor.setProgress(1.0);

	}

	@Override
	public CyNetwork[] getCyNetworks() {
		return new CyNetwork[] {network};
	}

	@Override
	public CyNetworkView buildCyNetworkView(CyNetwork network) {

		final CyNetworkView view = cyNetworkViewFactory.getNetworkView(network);
		
		String pref = CyLayoutAlgorithmManager.DEFAULT_LAYOUT_NAME;
		if(prop != null) 
			pref = prop.getProperties().getProperty("preferredLayoutAlgorithm", pref);

		final CyLayoutAlgorithm layout = layouts.getLayout(pref);
		layout.setNetworkView(view);
		
		// Force to run this task here to avoid concurrency problem.
		TaskIterator itr = layout.getTaskIterator();
		Task nextTask = itr.next();
		try {
			nextTask.run(parentTaskMonitor);
		} catch (Exception e) {
			throw new RuntimeException("Could not finish layout", e);
		}

		return view;
	}
}
