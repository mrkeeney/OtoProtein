package my.vaadin.ffx;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.activation.CommandMap;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.activation.MailcapCommandMap;
import javax.mail.Authenticator;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.servlet.annotation.WebServlet;

import org.apache.commons.configuration.CompositeConfiguration;

import com.vaadin.annotations.JavaScript;
import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.annotations.Widgetset;
import com.vaadin.server.Page;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.shared.Position;
import com.vaadin.shared.communication.PushMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;

import ffx.potential.MolecularAssembly;
import ffx.potential.parameters.ForceField;
import ffx.potential.parsers.ForceFieldFilter;
import ffx.potential.parsers.PDBFilter;
import ffx.utilities.Keyword;

/**
 * This UI is the application entry point. A UI may either represent a browser
 * window (or tab) or some part of a html page where a Vaadin application is
 * embedded.
 * <p>
 * The UI is initialized using {@link #init(VaadinRequest)}. This method is
 * intended to be overridden to add component to the user interface and
 * initialize non-component functionality.
 */
@Theme("mytheme")
@Widgetset("my.vaadin.ffx.MyAppWidgetset")
@JavaScript("bio-pv.min.js")
@Push(PushMode.AUTOMATIC)
public class MyUI extends UI {

	private JobForm form = new JobForm();
	VerticalLayout subContent = new VerticalLayout();

	@Override
	protected void init(final VaadinRequest vaadinRequest) {
		NotificationSender.setUi(this);
		final VerticalLayout fullLayout = new VerticalLayout();
		fullLayout.setWidth("100%");
		final Label title = new Label("OtoProtein: A Computational Tool For Genetic Variant Analysis");
		title.setStyleName(ValoTheme.LABEL_H1);
		title.setHeight("75px");
		fullLayout.addComponent(title);
		fullLayout.setComponentAlignment(title, Alignment.TOP_CENTER);

		final GridLayout contentGrid = new GridLayout(3, 4);

		// Title for form
		contentGrid.setMargin(true);

		// Add JobForm to grid
		contentGrid.addComponent(form, 0, 1);

		final ProteinViewer pv = ProteinViewer.getInstance();
		pv.setVisible(false);
		contentGrid.addComponent(pv, 1, 1, 1, 2);

		// Add a view controller for the protein viewer.
		final ViewController vc = ViewController.getInstance();
		vc.setVisible(false);
		contentGrid.addComponent(vc, 2, 1);

		// When save button is pressed, save user input into Job BeanItem
		Button save = new Button("Submit");

		save.addClickListener(e -> {
			final String errorResult = isFormValid();
			if (errorResult.isEmpty()) {
				save.setEnabled(true);
				try {
					NotificationSender.sendNotification("Thank you! Your job has been submitted.",
							Type.TRAY_NOTIFICATION);
					ActionDriver.beginActions(form);

					// Retrieving energy?
					// if (command.equalsIgnoreCase("Energy")) {
					// double energy =
					// Main.mainPanel.getHierarchy().getActive().getPotentialEnergy().getTotalEnergy();
					// Label resultLabel = new Label("Total Potential Energy:");
					// Label energyResult = new Label(energy + "kcal/mol");
					// subContent.addComponent(resultLabel);
					// subContent.addComponent(energyResult);
					// subContent.setWidth("300px");
					// subContent.setHeight("100px");
					// subContent.setMargin(true);
					// resultWindow.setContent(subContent);
				} catch (final Exception e1) {
					throw new RuntimeException(e1);
				}
			} else {
				NotificationSender.sendNotification("Job submission failed. " + errorResult, Type.ERROR_MESSAGE);
			}
		});

		contentGrid.addComponent(save, 0, 3);
		contentGrid.setSpacing(true);
		fullLayout.addComponent(contentGrid);

		setContent(fullLayout);
	}

	@WebServlet(urlPatterns = "/*", name = "MyUIServlet", asyncSupported = true)
	@VaadinServletConfiguration(ui = MyUI.class, productionMode = false)
	public static class MyUIServlet extends VaadinServlet {
	}

	private String isFormValid() {
		if (!isSet(form.getJobName())) {
			return "Please include your desired job name.";
		} else if (!isSet(form.getEmail())) {
			return "Please include your email where results should be sent to.";
		} else if (!form.isEmailValid()) {
			return "Please enter a valid email address to send results.";
		} else if (!isSet(form.getGeneName())) {
			return "Please select a gene name from the list of DVD genes.";
		} else if (!isSet(form.getResidueRange())) {
			return "Please select a residue range for the supplied gene.";
		} else if (!isSet(form.getChainId())) {
			return "Please select a chain ID for the supplied residue range.";
		} else if (!isSet(form.getMutationPosition())) {
			return "Please select the mutation position within the supplied residue range.";
		} else if (!isSet(form.getNewAminoAcid())) {
			return "Please select the amino acid you wish to mutate to.";
		} else {
			return "";
		}
	}

	private boolean isSet(final String stringToTest) {
		return (stringToTest != null) && (!stringToTest.isEmpty()) && (!stringToTest.equals("null"));
	}
}
