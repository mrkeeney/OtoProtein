package my.vaadin.ffx;

import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.rmi.CORBA.ValueHandlerMultiFormat;

import com.vaadin.annotations.JavaScript;
import com.vaadin.shared.ui.JavaScriptComponentState;
import com.vaadin.ui.AbstractJavaScriptComponent;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.OptionGroup;

public final class ViewController extends GridLayout {

	private static final long serialVersionUID = -5718191666359270835L;
	private static final ViewController INSTANCE = new ViewController();

	public enum DisplayOption {
		CARTOON("Cartoon", "cartoon"),
		LINES("Lines", "lines"),
		TRACE("Trace", "trace"),
		LINE_TRACE("Line Trace", "lineTrace"),
		SMOOTH_LINE("Smooth Line Trace", "sline"),
		TUBE("Tube", "tube"),
		SPHERES("Spheres", "spheres"),
		BALL_STICKS("Balls and Sticks", "ballsAndSticks");

		private final String displayName;
		private final String logicName;

		DisplayOption(final String displayName, final String logicName) {
			this.displayName = displayName;
			this.logicName = logicName;
		}

		public String getDisplayName() {
			return this.displayName;
		}

		public String getLogicName() {
			return this.logicName;
		}
	}

	public static ViewController getInstance() {
		return INSTANCE;
	}

	private ViewController() {
		final OptionGroup group = new OptionGroup("Choose View Style");
		group.addItems(Stream.of(DisplayOption.values()).map(displayEnum -> displayEnum.getDisplayName())
				.collect(Collectors.toList()));
		group.setNullSelectionAllowed(false);
		group.select(DisplayOption.values()[0].getDisplayName());
		group.addValueChangeListener(l -> {
			final String selectedValue = String.valueOf(group.getValue());
			ProteinViewer.getInstance().setDisplayMode(getEnumOption(selectedValue));
		});
		addComponent(group);
	}

	private DisplayOption getEnumOption(final String valueFromGui) {
		return Stream.of(DisplayOption.values())
				.filter(displayEnum -> displayEnum.getDisplayName().equals(valueFromGui)).findAny().get();
	}

}
