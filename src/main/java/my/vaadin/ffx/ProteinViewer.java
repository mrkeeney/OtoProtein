package my.vaadin.ffx;

import com.vaadin.annotations.JavaScript;
import com.vaadin.shared.ui.JavaScriptComponentState;
import com.vaadin.ui.AbstractJavaScriptComponent;

import my.vaadin.ffx.ViewController.DisplayOption;

@JavaScript("pv-connector.js")
public final class ProteinViewer extends AbstractJavaScriptComponent {
	
	private static ProteinViewer INSTANCE = null;
	
	private ProteinViewer() {
	}
	
	public static ProteinViewer getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new ProteinViewer();
			INSTANCE.setWidth("700px");
			INSTANCE.setHeight("750px");
			INSTANCE.setId("proteinViewer");
		}
		return INSTANCE;
	}
	
	private static final long serialVersionUID = 1L;

	public void viewPdb(final String geneName, final int beginningResidue, final int endingResidue) {
		getState().setUrl(geneName, beginningResidue, endingResidue);
	}
	
	public void setDisplayMode(final DisplayOption displayOption) {
		getState().displayMode = displayOption.getLogicName();
	}
	
	public String getPdbUrl() {
		return getState().url.replace(".pdb", "_FFX.pdb");
	}

	@Override
	public ProteinViewerState getState() {
		return (ProteinViewerState) super.getState();
	}
	
	public static class ProteinViewerState extends JavaScriptComponentState {
		private static final long serialVersionUID = 1L;
		
		private static final String ROOT_URL = "https://raw.githubusercontent.com/wtollefson/dvd-structures/master/";
		public String url;
		// Default to cartoon when the viewer is first initialized before any user interaction with the display type.
		public String displayMode = "cartoon";

		public void setUrl(final String geneName, final int beginningResidue, final int endingResidue) {
			final String residueRange = beginningResidue + "-" + endingResidue;
			// Build a string like "ROOT_URL/GENE_NAME/RES_RANGE/GENE_NAME_RES_RANGE.pdb
			final StringBuilder sb = new StringBuilder();
			sb.append(ROOT_URL);
			sb.append(geneName).append("/");
			sb.append(residueRange).append("/");
			sb.append(geneName).append("_").append(residueRange).append(".pdb");
			url = sb.toString();
		}
	}

}
