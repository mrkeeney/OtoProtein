package my.vaadin.ffx;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.json.JSONArray;

import javax.net.ssl.HttpsURLConnection;

public final class DataHandler {

	private static List<String> geneNames = null;

	private static Map<String, List<String>> residueRangeMap = new HashMap<>();

	private static Map<String, Map<String, String>> chainIdMap = new HashMap<>();

	private static final String BASE_URL = "https://api.github.com/repos/wtollefson/dvd-structures/contents";

	private DataHandler() {
	}

	public static List<String> getGithubGenes() throws Exception {
		// Check if the list of genes has been initialized yet.
		if (geneNames == null) {
			final List<String> results = getContentList(BASE_URL);
			// Gene names must be alpha-numeric only.
			geneNames = results.stream().filter(entry -> entry.matches("[A-Z0-9]+")).collect(Collectors.toList());
		}
		return geneNames;
	}

	public static List<String> getResidueRangesForGene(final String geneName) throws Exception {
		if (!residueRangeMap.containsKey(geneName)) {
			if (!getGithubGenes().contains(geneName)) {
				throw new IllegalArgumentException("Gene name is invalid.");
			}
			final List<String> results = getContentList(BASE_URL + "/" + geneName);
			// Residue ranges must be numbers followed by a "-" character, and
			// then more numbers.
			residueRangeMap.put(geneName,
					results.stream().filter(entry -> entry.matches("[0-9]+\\-[0-9]+")).collect(Collectors.toList()));
		}
		return residueRangeMap.get(geneName);
	}

	public static List<String> getChainIdsForGeneResidueRange(final String geneName, final String residueRange)
			throws Exception {
		// The key of this map is the geneName/residueRange (e.g. "GJB2/2-217")
		final String mapKey = geneName + "/" + residueRange;
		if (!chainIdMap.containsKey(mapKey)) {
			if (!getGithubGenes().contains(geneName)) {
				throw new IllegalArgumentException("Gene name is invalid.");
			}
			final List<String> pdbData = getCurrentPdb();
			final Map<String, String> chainIdInfo = new HashMap<>();
			int beginRange = -1;
			int endRange = -1;
			int prevPosition = -1;
			String currentChainId = null;
			String firstChainId = null;
			boolean firstChainAdded = false;
			for (final String s : pdbData) {
				if (s.startsWith("ATOM")) {
					final int currentPosition = getResiduePositionForAtom(s);
					currentChainId = getChainForAtom(s);
					if (!chainIdInfo.containsKey(currentChainId)) {
						if (beginRange != -1) {
							// This is a new chain, make sure its not the first.
							if (!currentChainId.equals(firstChainId)) {
								endRange = prevPosition;
								if (!firstChainAdded) {
									chainIdInfo.put(firstChainId, beginRange + "-" + endRange);
									firstChainAdded = true;
								}
								chainIdInfo.put(currentChainId, beginRange + "-" + endRange);
							}
						} else {
							// This is the first chain.
							firstChainId = currentChainId;
							beginRange = currentPosition;
						}
						if (firstChainAdded) {
							beginRange = currentPosition;
						}
					}
					prevPosition = currentPosition;
				}
			}
			// If the endRange was never set, there was a single chainId for the
			// entire residue range.
			if (endRange == -1) {
				chainIdInfo.put(currentChainId, residueRange);
			}

			// The value of the map is itself a map containing the chain ID and
			// associated residue range.
			chainIdMap.put(mapKey, chainIdInfo);
		}
		final List<String> result = new ArrayList<>();
		result.addAll(chainIdMap.get(mapKey).keySet());
		return result;
	}

	public static String getResRangeForChainId(final String geneName, final String residueRange, final String chainId) {
		final String chainIdMapKey = geneName + "/" + residueRange;
		if (!chainIdMap.containsKey(chainIdMapKey)) {
			throw new IllegalArgumentException("Gene name and residue range don't exist in internal map.");
		}
		final Map<String, String> temp = chainIdMap.get(chainIdMapKey);
		if (!temp.containsKey(chainId)) {
			throw new IllegalArgumentException("Chain ID doesn't exist for the given residue range and gene name.");
		}
		return temp.get(chainId);
	}

	private static List<String> getContentList(final String urlToRead) throws Exception {
		final JSONArray jsonResult = new JSONArray(readRawDataToString(urlToRead));
		final int jsonLen = jsonResult.length();
		final List<String> resultList = new ArrayList<>(jsonLen);
		for (int i = 0; i < jsonLen; i++) {
			resultList.add(jsonResult.getJSONObject(i).getString("name"));
		}
		return resultList;
	}

	private static String readRawDataToString(final String urlToRead) throws Exception {
		final List<String> results = readRawData(urlToRead);
		final StringBuilder sb = new StringBuilder();
		for (final String s : results) {
			sb.append(s + "\n");
		}
		return sb.toString();
	}

	private static List<String> readRawData(final String urlToRead) throws Exception {
		final List<String> result = new ArrayList<>();
		final URL url = new URL(urlToRead);
		final HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
		conn.setRequestMethod("GET");
		final BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		String line;
		while ((line = rd.readLine()) != null) {
			result.add(line + "\n");
		}
		rd.close();
		return result;
	}

	public static String getPdbBaseName() {
		final String pdbUrl = getPdbUrl();
		return pdbUrl.substring(pdbUrl.lastIndexOf("/") + 1, pdbUrl.lastIndexOf("."));
	}

	private static String getPdbUrl() {
		return ProteinViewer.getInstance().getPdbUrl();
	}

	private static List<String> getCurrentPdb() throws Exception {
		final String pdbUrl = getPdbUrl();
		if (pdbUrl.isEmpty()) {
			throw new IllegalArgumentException("No pdb is currently being viewed.");
		}
		return readRawData(pdbUrl);
	}

	private static String getChainForAtom(final String atomEntry) {
		return atomEntry.substring(20, 22).trim();
	}

	private static final int getResiduePositionForAtom(final String atomEntry) {
		return Integer.parseInt(atomEntry.substring(22, 27).trim());
	}

	public static String getWtResAtPosition(final String geneName, final String chainId, final int position)
			throws Exception {
		final List<String> pdbData = getCurrentPdb();

		for (final String s : pdbData) {
			// Only look at the corresponding substring if it begins with
			// "ATOM".
			if (s.startsWith("ATOM")) {
				// Check if this is the right chain.
				if (getChainForAtom(s).equals(chainId)) {
					// Check if this is the right position.
					if (getResiduePositionForAtom(s) == position) {
						return s.substring(17, 20);
					}
				}
			}
		}
		throw new IOException("Could not find resiude number in specified PDB.");
	}

	public static File writeCurrentPdbToTemp() throws Exception {
		final File tempFile = File.createTempFile(getPdbBaseName(), ".pdb");
		final List<String> currentPdb = getCurrentPdb();
		final BufferedWriter outputWriter = new BufferedWriter(new FileWriter(tempFile));
		for (final String line : currentPdb) {
			outputWriter.write(line);
			outputWriter.newLine();
		}
		outputWriter.flush();
		outputWriter.close();
		return tempFile;
	}

}
