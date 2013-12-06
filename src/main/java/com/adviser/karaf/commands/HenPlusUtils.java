package com.adviser.karaf.commands;

import org.apache.felix.service.command.CommandSession;

import jline.console.ConsoleReader;

/**
 * 
 * @author toanvu
 *
 */
public class HenPlusUtils {
	private static final String COMMAND_PROMPT = "henplus";

	/**
	 * get current command line 
	 * @see https://github.com/jline/jline2/blob/master/src/main/java/jline/console/ConsoleReader.java
	 * for more informations about consolereader
	 * @param session of current command
	 * @return the string typed so far to cursor's position
	 */
	public static String getCurrentCommandLine(CommandSession session){
		ConsoleReader consoleReader = (ConsoleReader) session.get(".jline.reader");		 		
 		String partialCommand = consoleReader.getCursorBuffer().buffer.toString().replaceAll(COMMAND_PROMPT+" ", "");
 		return partialCommand;
	}
}
