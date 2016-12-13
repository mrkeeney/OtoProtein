package my.vaadin.ffx;

import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Authenticator;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import com.vaadin.ui.Notification.Type;

import edu.rit.pj.Comm;

import java.io.File;
import java.io.IOException;

import ffx.algorithms.Minimize;
import ffx.algorithms.RotamerOptimization;
import ffx.algorithms.RotamerOptimization.Algorithm;
import ffx.numerics.Potential;
import ffx.potential.ForceFieldEnergy;
import ffx.potential.MolecularAssembly;
import ffx.potential.bonded.Residue;
import ffx.potential.bonded.Rotamer;
import ffx.potential.bonded.RotamerLibrary;
import ffx.potential.bonded.RotamerLibrary.ProteinLibrary;
import ffx.potential.parameters.ForceField;
import ffx.potential.parsers.PDBFilter;
import ffx.potential.parsers.ForceFieldFilter;
import ffx.utilities.Keyword;

import org.apache.commons.configuration.CompositeConfiguration;

public final class ActionDriver {

	public static void beginActions(final JobForm form) {
		final ExecutorService executor = Executors.newSingleThreadExecutor();
		executor.submit(() -> {
			// Sleep for just a moment to let the user read the submission
			// notification.
			try {
				TimeUnit.SECONDS.sleep(3);

				// Tell the user that the job has started.
				NotificationSender.sendNotification("Your job '" + form.getJobName() + "' has begun.",
						Type.TRAY_NOTIFICATION);

				System.out.println("Querying FFX now.");
				// Get the PDB from GitHub and write it to temp storage.
				final File structure = DataHandler.writeCurrentPdbToTemp();
				final String fileName = structure.getName();
				final String nameNoExtension = fileName.substring(0, fileName.lastIndexOf("."));

				final double beginningEnergy;
				final double endingEnergy;

				// Create a molecular assembly to read the pdb contents and do
				// an initial minimization.
				{
					final MolecularAssembly molecularAssembly = new MolecularAssembly(nameNoExtension + "_1");
					molecularAssembly.setFile(structure);
					final CompositeConfiguration properties = Keyword.loadProperties(structure);
					final ForceFieldFilter forceFieldFilter = new ForceFieldFilter(properties);
					final ForceField forceField = forceFieldFilter.parse();
					molecularAssembly.setForceField(forceField);

					// Create the PDB filter to manipulate the PDB data.
					final PDBFilter pdbFilter = new PDBFilter(structure, molecularAssembly, forceField, properties);

					// Get the energy before mutating?
					pdbFilter.readFile();
					molecularAssembly.finalize(true, forceField);
					ForceFieldEnergy forceFieldEnergy = new ForceFieldEnergy(molecularAssembly);
					molecularAssembly.setPotential(forceFieldEnergy);

					// Minimize the structure.
					final Minimize m = new Minimize(molecularAssembly, null);
					final Potential p = m.minimize();
					beginningEnergy = p.getTotalEnergy();
					System.out.println("Beginning energy: " + beginningEnergy);
				}

				// Create the new molecular assembly.
				{
					final MolecularAssembly molecularAssembly = new MolecularAssembly(nameNoExtension + "_2");
					molecularAssembly.setFile(structure);
					final CompositeConfiguration properties = Keyword.loadProperties(structure);
					final ForceFieldFilter forceFieldFilter = new ForceFieldFilter(properties);
					final ForceField forceField = forceFieldFilter.parse();
					molecularAssembly.setForceField(forceField);

					// Create the PDB filter to manipulate the PDB data.
					final PDBFilter pdbFilter = new PDBFilter(structure, molecularAssembly, forceField, properties);

					// Get the energy before mutating?
					final int resId = Integer.parseInt(form.getMutationPosition());
					pdbFilter.mutate(form.getChainId().charAt(0), resId, form.getNewAminoAcid());
					pdbFilter.readFile();
					molecularAssembly.finalize(true, forceField);
					ForceFieldEnergy forceFieldEnergy = new ForceFieldEnergy(molecularAssembly);
					molecularAssembly.setPotential(forceFieldEnergy);

					// Minimize the mutated structure.
					final Minimize m = new Minimize(molecularAssembly, null);
					final Potential p = m.minimize();
					endingEnergy = p.getTotalEnergy();
					System.out.println("Final energy: " + endingEnergy);

					// Write the output result.
					pdbFilter.writeFile(structure, false);
				}

				// Get the energy difference.
				final double energyDifference = Math.abs(beginningEnergy - endingEnergy);
				System.out.println("Difference in energy: " + energyDifference);

				// Get the file from temp storage and attach it to an email
				// response.
				final File newStructure = new File(structure.getAbsolutePath() + "_2");

				System.out.println("Sending email to user.");
				// Send an email to the user.
				final String username = "otoprotein.no.reply@gmail.com";
				final String password = "otoprotein";

				final Properties props = new Properties();
				props.put("mail.smtp.auth", "true");
				props.put("mail.smtp.starttls.enable", "true");
				props.put("mail.smtp.host", "smtp.gmail.com");
				props.put("mail.smtp.port", "587");

				final Session session = Session.getInstance(props, new Authenticator() {
					protected PasswordAuthentication getPasswordAuthentication() {
						return new PasswordAuthentication(username, password);
					}
				});

				final Message message = new MimeMessage(session);
				message.setFrom(new InternetAddress("otoprotein.no.reply@gmail.com"));
				message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(form.getEmail()));
				message.setSubject("OtoProtein Results: " + form.getJobName());
				final Multipart multipart = new MimeMultipart();

				BodyPart messageBodyPart = new MimeBodyPart();
				messageBodyPart
						.setContent("The attached file contains the results of your OtoProtein query in the PDB format.<br>"
								+ "The absolute value of the energy change between the original structure and the mutated structure was <b>"
								+ String.format("%.3f", energyDifference) + " kcal/mol</b>.<br><br>"
								+ "Below are some general energy cut-offs that can be used for relative pathogenicity predition:<br>"
								+ "Benign: 0.0 - 15.0 kcal/mol<br>"
								+ "Likely Benign: 15.0 - 30.0 kcal/mol<br>"
								+ "Likely Pathogenic: 30.0 - 45.0 kcal/mol<br>"
								+ "Pathogenic: 45.0 + kcal/mol<br><br>"
								+ "Thank you for using OtoProtein. We hope that you found this tool useful!<br><br>"
								+ "Sincerely,<br>"
								+ "The OtoProtein Team", "text/html; charset=utf-8");
				multipart.addBodyPart(messageBodyPart);
				messageBodyPart = new MimeBodyPart();
				DataSource source = new FileDataSource(newStructure);
				messageBodyPart.setDataHandler(new javax.activation.DataHandler(source));
				messageBodyPart.setFileName(DataHandler.getPdbBaseName() + "_NEW_STRUCT.pdb");
				multipart.addBodyPart(messageBodyPart);

				// Send the message.
				message.setContent(multipart);
				Thread.currentThread().setContextClassLoader(ActionDriver.class.getClassLoader());
				Transport.send(message);

				// Delete the temporary files.
				structure.delete();
				newStructure.delete();

				NotificationSender.sendNotification("Successfully sent results to: " + form.getEmail(),
						Type.TRAY_NOTIFICATION);
			} catch (final Exception e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		});
		executor.shutdown();
	}

	public static void main() throws Exception {
		Comm.init(new String[] { "Empty", "Properties" });
		Comm.world();
	}

}
