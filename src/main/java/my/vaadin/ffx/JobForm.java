package my.vaadin.ffx;

import java.util.Arrays;
import java.util.List;

import com.vaadin.data.validator.EmailValidator;
import com.vaadin.data.validator.NullValidator;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.NativeSelect;
import com.vaadin.ui.TextField;
import com.vaadin.ui.themes.BaseTheme;
import com.vaadin.ui.themes.ValoTheme;

import my.vaadin.ffx.MyUI;

public final class JobForm extends GridLayout {
	private MyUI myUI;
	private TextField jobName = new TextField("Job Name");
	private TextField email = new TextField("Email");
	NativeSelect geneName = new NativeSelect("Gene Name");
	NativeSelect residueRange = new NativeSelect("Residue Range");
	NativeSelect chainId = new NativeSelect("Chain ID");
	NativeSelect mutationPosition = new NativeSelect("Mutation Position");
	NativeSelect newAA = new NativeSelect("New Amino Acid");
	// private MutatePDBForm mutatePDBForm = new MutatePDBForm();
	private Button jobNameHelp = new Button();
	private Button emailHelp = new Button();
	private Button geneNameHelp = new Button();
	private Button residueRangeHelp = new Button();
	private Button chainIdHelp = new Button();
	private Button mutationPositionHelp = new Button();
	private Button newAaHelp = new Button();
	private Button save = new Button("Save");
	private Label wtRes = new Label();
	public String commandSelection;
	private List<String> allAminoAcids = Arrays.asList("ALA", "ARG", "ASN", "ASP", "CYS", "GLU", "GLN", "GLY", "HIS",
			"ILE", "LEU", "LYS", "MET", "PHE", "PRO", "SER", "THR", "TRY", "TYR", "VAL");

	public JobForm() {

		super(3, 9);

		jobName.setIcon(FontAwesome.USER);
		jobName.setRequired(true);
		jobName.addValidator(new NullValidator("Must be given", false));

		email.setIcon(FontAwesome.ENVELOPE);
		email.setRequired(true);
		email.addValidator(new EmailValidator("Please enter a valid email address."));

		// Set Gene Name dropdown.
		List<String> geneNames;
		try {
			geneNames = DataHandler.getGithubGenes();
		} catch (final Exception e) {
			geneNames = Arrays.asList("Error retrieving gene list.");
		}
		for (int i = 0; i < geneNames.size(); i++) {
			geneName.addItem(geneNames.get(i));
			geneName.setItemCaption(i, geneNames.get(i));
		}

		geneName.setNullSelectionAllowed(false);
		geneName.setRequired(true);
		geneName.addValidator(new NullValidator("Must be given", false));
		geneName.addValueChangeListener(e -> {
			List<String> residueRanges;
			try {
				residueRanges = DataHandler.getResidueRangesForGene(geneName.getValue().toString());
			} catch (final Exception exception) {
				residueRanges = Arrays.asList("Error retrieving residue ranges.");
			}

			// Clear out any existing items and populate new residue ranges.
			residueRange.clear();
			residueRange.removeAllItems();
			chainId.clear();
			chainId.removeAllItems();
			mutationPosition.clear();
			mutationPosition.removeAllItems();
			newAA.setVisible(false);
			newAaHelp.setVisible(false);
			for (int i = 0; i < residueRanges.size(); i++) {
				residueRange.addItem(residueRanges.get(i));
				residueRange.setItemCaption(i, residueRanges.get(i));
			}
		});

		residueRange.setNullSelectionAllowed(false);
		residueRange.setRequired(true);
		residueRange.addValidator(new NullValidator("Must be given", false));
		residueRange.addValueChangeListener(e -> {
			final Object geneName = this.geneName.getValue();
			if (geneName != null) {
				final Object residueRange = this.residueRange.getValue();
				if (residueRange != null && !residueRange.toString().isEmpty()) {
					final String range = residueRange.toString();
					final String geneNameStr = geneName.toString();
					final int beginningResidue = Integer.parseInt(range.substring(0, range.indexOf("-")));
					final int endingResidue = Integer.parseInt(range.substring(range.indexOf("-") + 1, range.length()));
					final ProteinViewer pv = ProteinViewer.getInstance();
					pv.setVisible(true);
					pv.viewPdb(geneNameStr, beginningResidue, endingResidue);
					ViewController.getInstance().setVisible(true);

					// Determine the list of chain IDs for the selected residue
					// range.
					chainId.clear();
					chainId.removeAllItems();

					List<String> chainIds;
					try {
						chainIds = DataHandler.getChainIdsForGeneResidueRange(geneNameStr, range);
					} catch (final Exception exception) {
						chainIds = Arrays.asList("Error retrieving chain IDs.");
					}

					for (int i = 0; i < chainIds.size(); i++) {
						chainId.addItem(chainIds.get(i));
						chainId.setItemCaption(i, chainIds.get(i));
					}
					mutationPosition.clear();
					mutationPosition.removeAllItems();
					newAA.setVisible(false);
					newAaHelp.setVisible(false);
				}
			}
		});

		chainId.setNullSelectionAllowed(false);
		chainId.setRequired(true);
		chainId.addValidator(new NullValidator("Must be given", false));
		chainId.addValueChangeListener(e -> {
			final Object currentChainId = this.chainId.getValue();
			if (currentChainId != null) {
				// Determine the beginning and end residues of the current
				// chain.

				// Populate all of the mutation positions based on the residues
				// that are in the current chain.
				mutationPosition.clear();
				mutationPosition.removeAllItems();
				newAA.setVisible(false);
				newAaHelp.setVisible(false);

				final String chainIdResRange = DataHandler.getResRangeForChainId(this.geneName.getValue().toString(),
						this.residueRange.getValue().toString(), this.chainId.getValue().toString());
				final int beginningResidue = Integer.parseInt(chainIdResRange.substring(0, chainIdResRange.indexOf("-")));
				final int endingResidue = Integer.parseInt(chainIdResRange.substring(chainIdResRange.indexOf("-") + 1, chainIdResRange.length()));
				for (int i = beginningResidue; i <= endingResidue; i++) {
					final String currentPosition = String.valueOf(i);
					mutationPosition.addItem(currentPosition);
					mutationPosition.setItemCaption(i, currentPosition);
				}
			}
		});

		mutationPosition.setNullSelectionAllowed(false);
		mutationPosition.setRequired(true);
		mutationPosition.addValidator(new NullValidator("Must be given", false));
		mutationPosition.addValueChangeListener(e -> {
			final Object currentPosition = mutationPosition.getValue();
			if (currentPosition != null) {
				wtRes.setVisible(true);
				final int position = Integer.parseInt(mutationPosition.getValue().toString());
				String labelText;
				try {
					final String resAtPosition = DataHandler.getWtResAtPosition(geneName.getValue().toString(), chainId.getValue().toString(),
							position);
					labelText = "Wild type residue: " + resAtPosition;
					newAA.removeItem(resAtPosition);
					newAA.clear();
					newAA.removeAllItems();
					for (int i = 0; i < allAminoAcids.size(); i++) {
						final String currentAA = allAminoAcids.get(i);

						// Only add the amino acid to this list if it isn't the
						// WT.
						if (!currentAA.equals(resAtPosition)) {
							newAA.addItem(currentAA);
							newAA.setItemCaption(i, currentAA);
						}
					}
					newAA.setVisible(true);
					newAaHelp.setVisible(true);
				} catch (final Exception exception) {
					System.out.println(exception.getMessage());
					labelText = "Error retrieving WT residue.";
				}
				wtRes.setCaption(labelText);

			} else {
				wtRes.setVisible(false);
				newAA.setVisible(false);
				newAaHelp.setVisible(false);
			}
		});

		// The wild-type residue label should remain invisible until the
		// mutation position has been selected.
		wtRes.setVisible(false);

		newAA.setNullSelectionAllowed(false);
		newAA.setRequired(true);
		newAA.addValidator(new NullValidator("Must be given", false));

		save.setStyleName(ValoTheme.BUTTON_PRIMARY);
		save.setClickShortcut(KeyCode.ENTER);

		// Informational icons
		helpButtonGenerator(jobNameHelp, "Please enter a job name using letters, numbers, dashes, or underscores.");
		helpButtonGenerator(emailHelp, "Please enter an email address to which the results can be sent.");
		helpButtonGenerator(geneNameHelp, "Please select a Gene Name from the list of FFX-refined genes.");
		helpButtonGenerator(residueRangeHelp,
				"Please select a residue range of the selected gene respresenting an available structure.");
		helpButtonGenerator(chainIdHelp, "Please select a chain ID within the given residue range.");
		helpButtonGenerator(mutationPositionHelp, "Please select the residue number to be mutated.");
		helpButtonGenerator(newAaHelp, "Please select the amino acid to mutate to.");

		// Job Name.
		{
			// setSpacing(true);
			addComponent(jobName, 0, 1);
			setSpacing(true);
			addComponent(jobNameHelp, 1, 1);
			setSpacing(true);
			setComponentAlignment(jobNameHelp, Alignment.BOTTOM_LEFT);
		}
		// Email.
		{
			addComponent(email, 0, 2);
			setSpacing(true);
			addComponent(emailHelp, 1, 2);
			setSpacing(true);
			setComponentAlignment(emailHelp, Alignment.BOTTOM_LEFT);
		}
		// Gene name.
		{
			addComponent(geneName, 0, 3);
			setSpacing(true);
			geneName.setWidth("100%");
			addComponent(geneNameHelp, 1, 3);
			setSpacing(true);
			setComponentAlignment(geneNameHelp, Alignment.BOTTOM_LEFT);
		}
		// Residue Range.
		{
			addComponent(residueRange, 0, 4);
			setSpacing(true);
			residueRange.setWidth("100%");
			addComponent(residueRangeHelp, 1, 4);
			setSpacing(true);
			setComponentAlignment(residueRangeHelp, Alignment.BOTTOM_LEFT);
		}
		// Chain ID.
		{
			addComponent(chainId, 0, 5);
			setSpacing(true);
			chainId.setWidth("100%");
			addComponent(chainIdHelp, 1, 5);
			setSpacing(true);
			setComponentAlignment(chainIdHelp, Alignment.BOTTOM_LEFT);
		}
		// Mutation Position.
		{
			addComponent(mutationPosition, 0, 6);
			setSpacing(true);
			mutationPosition.setWidth("100%");
			addComponent(mutationPositionHelp, 1, 6);
			setSpacing(true);
			setComponentAlignment(mutationPositionHelp, Alignment.BOTTOM_LEFT);
		}
		// WT residue label.
		{
			addComponent(wtRes, 0, 7);
			setSpacing(true);
			wtRes.setWidth("100%");
			setComponentAlignment(mutationPositionHelp, Alignment.BOTTOM_LEFT);
		}
		// New Amino Acid.
		{
			newAA.setVisible(false);
			newAaHelp.setVisible(false);
			addComponent(newAA, 0, 8);
			setSpacing(true);
			newAA.setWidth("100%");
			addComponent(newAaHelp, 1, 8);
			setSpacing(true);
			setComponentAlignment(mutationPositionHelp, Alignment.BOTTOM_LEFT);
		}
	}

	private void helpButtonGenerator(final Button button, final String prompt) {
		button.setIcon(FontAwesome.INFO_CIRCLE);
		button.setStyleName(BaseTheme.BUTTON_LINK);
		button.setDescription(prompt);
	}

	public String getJobName() {
		return jobName.getValue();
	}

	public String getEmail() {
		return email.getValue();
	}

	public boolean isEmailValid() {
		return email.isValid();
	}

	public String getGeneName() {
		return String.valueOf(geneName.getValue());
	}

	public String getResidueRange() {
		return String.valueOf(residueRange.getValue());
	}

	public String getChainId() {
		return String.valueOf(chainId.getValue());
	}
	
	public String getMutationPosition() {
		return String.valueOf(mutationPosition.getValue());
	}

	public String getNewAminoAcid() {
		return String.valueOf(newAA.getValue());
	}
}
