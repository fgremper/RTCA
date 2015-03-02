package ch.ethz.fgremper.rtca;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONObject;

public class SideBySideThreeWayDiff {

	public static int countConflicts(String fileName1, String fileName2, String fileName3) throws Exception {
		int count = 0;

		Pattern pattern = Pattern.compile("^(====|====2)$");

		Process p = Runtime.getRuntime().exec(new String[]{"diff3", fileName1, fileName2, fileName3});
		p.waitFor();

		BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

		String line;

		while ((line = reader.readLine()) != null) {

			Matcher m = pattern.matcher(line);
			if (m.matches()) {
				count++;
			}
		}
		
		return count;
	}
	
	public static JSONObject diff(String fileName1, String fileName2, String fileName3) throws Exception {

		Vector<List<String>> fileContent = new Vector<List<String>>(3);
		fileContent.add(fileToLines(fileName1));
		fileContent.add(fileToLines(fileName2));
		fileContent.add(fileToLines(fileName3));

		Vector<List<String>> fileContentType = new Vector<List<String>>(3);
	    for (int i = 0; i < 3; i++) {
			fileContentType.add(new LinkedList<String>());
	    	for (int j = 0; j < fileContent.get(i).size(); j++) {
	    		fileContentType.get(i).add("unchanged");
	    	}
	    }
	    
	    
	    
	    
		Process p = Runtime.getRuntime().exec(new String[]{"diff3", fileName1, fileName2, fileName3});
		p.waitFor();

		BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

		String line;
		
		Pattern pattern = Pattern.compile("^([1-3]):([0-9]+)(,([0-9]+))?([ac])$");

		Vector<Integer> fileStart = new Vector<Integer>(3);
		Vector<Integer> fileEnd = new Vector<Integer>(3);
		Vector<String> fileType = new Vector<String>(3);
		Vector<Integer> offset = new Vector<Integer>(3);
		for (int i = 0; i < 3; i++) {
			fileStart.add(0);
			fileEnd.add(0);
			fileType.add("");
			offset.add(0);
		}
		int maxLength = 0;
		
		while ((line = reader.readLine()) != null) {
			Matcher m = pattern.matcher(line);
			if (m.matches()) {
				
				System.out.println("[TestGitHelper] Console: " + line);
				
				
				int fileNum = Integer.parseInt(m.group(1)) - 1;
				
				if (fileNum == 0) {
					maxLength = 0;
				}
				
				fileStart.set(fileNum, Integer.parseInt(m.group(2)) - 1);
				fileEnd.set(fileNum, m.group(4) != null ? (Integer.parseInt(m.group(4)) - 1) : (Integer.parseInt(m.group(2)) - 1));
				maxLength = Math.max(maxLength, (m.group(4) != null ? (Integer.parseInt(m.group(4)) - Integer.parseInt(m.group(2)) + 1) : 1));
				fileType.set(fileNum, m.group(5));
				

				if (fileNum == 2) {
					for (int i = 0; i < 3; i++) {
						//System.out.println("file " + i + " type + " + fileType.get(i));
						if (fileType.get(i).equals("c")) {
							int length = maxLength;
							for (int j = fileStart.get(i); j <= fileEnd.get(i); j++) {
								System.out.println(" changing " + j);
								fileContentType.get(i).set(j + offset.get(i), "modified");
								length--;
							}
							while (length > 0) {
								fileContentType.get(i).add(fileEnd.get(i) + offset.get(i) + 1, "modifiedpad");
								fileContent.get(i).add(fileEnd.get(i) + offset.get(i) + 1, "");
								offset.set(i, offset.get(i) + 1);
								length--;
							}
						}
						else if (fileType.get(i).equals("a")) {
							int length = maxLength;
							while (length > 0) {
								System.out.println("file " + i + " pad ");
								fileContentType.get(i).add(fileEnd.get(i) + offset.get(i) + 1, "pad");
								fileContent.get(i).add(fileEnd.get(i) + offset.get(i) + 1, "");
								offset.set(i, offset.get(i) + 1);
								length--;
							}
						}
					}
				}
				
				//System.out.println(m.groupCount());
				//System.out.println("[TestGitHelper]  >> " + fileNum + " " + fileStart + " " + fileEnd + " " + fileType);
			}
		}

	    
		for (int i = 0; i < 3; i++) {
			System.out.println("File " + i);
			for (int j = 0; j < fileContent.get(i).size(); j++) {
				System.out.println((j + 1) + " " + fileContentType.get(i).get(j) + " " + fileContent.get(i).get(j));
			}
		}
		
		return null;
	}
	
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
