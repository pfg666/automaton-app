package main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Scanner;

public class DotHandler {

	
	public static void main (String[] args) {
		if (args.length == 1) {
			String dotString = "";
			try {
				File dotFile = new File(args[0]);
				Scanner dotScanner = new Scanner(dotFile);
				
				while (dotScanner.hasNextLine()) {
					dotString += dotScanner.nextLine() + "\n";
				}
				
				dotScanner.close();

				String stripped = dotString.
		                replaceAll(
		                        "<<table border=\"0\" cellpadding=\"1\" cellspacing=\"0\"><tr><td>",
		                        "\"").replaceAll("</td><td>", " ")
		                .replaceAll("</td></tr></table>>", "\"");
				
				PrintWriter out = new PrintWriter("strippedDot.dot");
				out.print(stripped);
				out.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	}
}
