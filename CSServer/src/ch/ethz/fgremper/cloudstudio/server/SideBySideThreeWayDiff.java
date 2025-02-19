package ch.ethz.fgremper.cloudstudio.server;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import ch.ethz.fgremper.cloudstudio.common.ProcessWithTimeout;

/**
 * 
 * Utility class to provide side-by-side comparison of three files
 * 
 * @author Fabian Gremper
 * 
 */
public class SideBySideThreeWayDiff {

	private static final Logger log = LogManager.getLogger(SideBySideThreeWayDiff.class);
	
	/**
	 * 
	 * Find the number of conflicting blocks in a three way file comparison
	 * 
	 * @param fileName1 my file
	 * @param fileName2 base file
	 * @param fileName3 their file
	 * 
	 * @return number of conflict blocks
	 * 
	 */
	public static int countConflicts(String fileName1, String fileName2, String fileName3) throws Exception {
		
		// Count
		int count = 0;

		// Compile pattern: a merge conflict occurs if all 3 files or only the base file differs
		Pattern pattern = Pattern.compile("^(====|====2)$");

		log.debug("Running diff: " + fileName1 + " " + fileName2 + " " + fileName3);
		
		// Run diff3
		Process p = Runtime.getRuntime().exec(new String[]{"diff3", fileName1, fileName2, fileName3});
		ProcessWithTimeout processWithTimeout = new ProcessWithTimeout(p);
		processWithTimeout.waitForProcess(1000);

		// Read the output
		BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
		String line;
		while ((line = reader.readLine()) != null) {

			// If we match the pattern, there's a conflicting merge block
			Matcher m = pattern.matcher(line);
			if (m.matches()) {
				count++;
			}
		}
		
		// Return counter
		return count;
		
	}

	/**
	 * 
	 * Create side-by-side-by-side displayable view for three files
	 * @param fileName1 my file
	 * @param fileName2 base file
	 * @param fileName3 their file
	 * 
	 * @return JSON array of lines, where every line is an object with the keys myContent,
	 * baseContent, theirContent, myType, baseType, theirType
	 * 
	 */
	public static JSONArray diff(String fileName1, String fileName2, String fileName3) throws Exception {

		// File content (0: me, 1: base, 2: them)
		Vector<List<String>> fileContent = new Vector<List<String>>(3);
		fileContent.add(fileToLines(fileName1));
		fileContent.add(fileToLines(fileName2));
		fileContent.add(fileToLines(fileName3));

		// File type (0: me, 1: base, 2: them)
		Vector<List<String>> fileContentType = new Vector<List<String>>(3);
	    for (int i = 0; i < 3; i++) {
			fileContentType.add(new LinkedList<String>());
	    	for (int j = 0; j < fileContent.get(i).size(); j++) {
	    		fileContentType.get(i).add("UNCHANGED");
	    	}
	    }

		// Run diff3
		Process p = Runtime.getRuntime().exec(new String[]{"diff3", fileName1, fileName2, fileName3});
		ProcessWithTimeout processWithTimeout = new ProcessWithTimeout(p);
		processWithTimeout.waitForProcess(1000);

		// Read the output
		BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
		String line;
		
		// Pattern to watch: it's always repetitions of triplets with all 3 files
		Pattern pattern = Pattern.compile("^([1-3]):([0-9]+)(,([0-9]+))?([ac])$");
		Pattern conflictPattern = Pattern.compile("^====(.?)$");

		// Init block and offset
		Vector<Integer> blockStart = new Vector<Integer>(3);
		Vector<Integer> blockEnd = new Vector<Integer>(3);
		Vector<String> blockType = new Vector<String>(3);
		Vector<Integer> fileOffset = new Vector<Integer>(3);
		for (int i = 0; i < 3; i++) {
			blockStart.add(0);
			blockEnd.add(0);
			blockType.add("");
			fileOffset.add(0);
		}
		int maxLength = 0;
		
		int blocksCounter = 0;
		
		int conflictType = -1;
		
		// Go through the diff3 output
		while ((line = reader.readLine()) != null) {
			
			System.out.println(">>> " + line);

			Matcher conflictMatch = conflictPattern.matcher(line);
			if (conflictMatch.matches()) {
				String conflictTypeString = conflictMatch.group(1);
				conflictType = -1;
				if (conflictTypeString.equals("1")) conflictType = 0;
				if (conflictTypeString.equals("2")) conflictType = 1;
				if (conflictTypeString.equals("3")) conflictType = 2;
			}
			
			// Do we match the pattern=
			Matcher m = pattern.matcher(line);
			if (m.matches()) {
				
				// Which file?
				int fileNum = Integer.parseInt(m.group(1)) - 1;
				
				// First file? Reset the max length, so we can get the max block length for this block
				if (fileNum == 0) {
					maxLength = 0;
				}
				
				// Set start, end, type and max length of block
				blockStart.set(fileNum, Integer.parseInt(m.group(2)) - 1);
				blockEnd.set(fileNum, m.group(4) != null ? (Integer.parseInt(m.group(4)) - 1) : (Integer.parseInt(m.group(2)) - 1));
				maxLength = Math.max(maxLength, (m.group(4) != null ? (Integer.parseInt(m.group(4)) - Integer.parseInt(m.group(2)) + 1) : 1));
				blockType.set(fileNum, m.group(5));
				
				blocksCounter++;
				
				// Last file? End of block, time to process
				if (blocksCounter == 3) {
					
					// For all files, process the block information, meaning set type to modified, pad
					// or modifiedpad where necessary and inserting empty lines to the content where
					// necessary
					
					boolean isConflict = (conflictType == -1 || conflictType == 1);
							
					for (int i = 0; i < 3; i++) {
						
						// Change block?
						if (blockType.get(i).equals("c")) {
							int length = maxLength;
							
							// Go through all the lines and set their type to modified
							for (int j = blockStart.get(i); j <= blockEnd.get(i); j++) {
								fileContentType.get(i).set(j + fileOffset.get(i), isConflict ? "CONFLICT" : "MODIFIED");
								length--;
							}
							
							// Still some difference to the max length, so we insert some padding
							while (length > 0) {
								fileContentType.get(i).add(blockEnd.get(i) + fileOffset.get(i) + 1, isConflict ? "CONFLICT_PAD" : "MODIFIED_PAD");
								fileContent.get(i).add(blockEnd.get(i) + fileOffset.get(i) + 1, "");
								fileOffset.set(i, fileOffset.get(i) + 1);
								length--;
							}
						}
						
						// Add block?
						else if (blockType.get(i).equals("a")) {
							int length = maxLength;
							while (length > 0) {
								fileContentType.get(i).add(blockEnd.get(i) + fileOffset.get(i) + 1, isConflict ? "CONFLICT_PAD" : "PAD");
								fileContent.get(i).add(blockEnd.get(i) + fileOffset.get(i) + 1, "");
								fileOffset.set(i, fileOffset.get(i) + 1);
								length--;
							}
						}
					}
					blocksCounter = 0;
				}
				
			}
		}

		// Build JSON array with the diff
	    JSONArray lineArray = new JSONArray();
	    for (int i = 0; i < fileContent.get(0).size(); i++) {
	    	JSONObject lineObject = new JSONObject();
	    	lineObject.put("myContent", fileContent.get(0).get(i));
	    	lineObject.put("myType", fileContentType.get(0).get(i));
	    	lineObject.put("baseContent", fileContent.get(1).get(i));
	    	lineObject.put("baseType", fileContentType.get(1).get(i));
	    	lineObject.put("theirContent", fileContent.get(2).get(i));
	    	lineObject.put("theirType", fileContentType.get(2).get(i));
	    	lineArray.put(lineObject);
	    }
    
	    // Return it
    	return lineArray;
    	
	}
	
	/**
	 * 
	 * Reads a file into a list of lines
	 * 
	 * @param filename filename of the file to read
	 * 
	 * @return list of lines
	 * 
	 */
	public static List<String> fileToLines(String filename) {
        List<String> lines = new LinkedList<String>();
        String line = "";
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(
                new FileInputStream(filename), "UTF8"));
            while ((line = in.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // ignore ... any errors should already have been
                    // reported via an IOException from the final flush.
                }
            }
        }
        return lines;
	}
	
}
