package com.adviser.karaf.commands;

import henplus.HenPlus;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.felix.gogo.commands.basic.AbstractCommand;
import org.apache.felix.gogo.runtime.CommandProxy;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Function;
import org.apache.karaf.shell.console.Completer;
import org.apache.karaf.shell.console.jline.CommandSessionHolder;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * custom completer. 
 * @author toanvu
 * 
 */
public class HenPlusCompleter implements Completer {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(HenPlusCompleter.class);

	private CommandSession session;
	private HenPlus henplus;

	public HenPlusCompleter(HenPlus henplus) {
		this(CommandSessionHolder.getSession());
		this.henplus = henplus;
	}

	public HenPlusCompleter(CommandSession session) {
		this.session = session;
	}

	@Override
	public int complete(String buffer, int cursor, List<String> candidates) {
		if (session == null) {
			session = CommandSessionHolder.getSession();
		}
		SortedSet<String> hpCandidates = convert(hpCompleter());

		if (buffer == null) {
			buffer = "";
		}

		SortedSet<String> matches = hpCandidates.tailSet(buffer);
		for (String match : matches) {
			String s = match.toLowerCase();
			if (!s.startsWith(buffer)) {
				break;
			}

			// noinspection unchecked
			candidates.add(match);
		}

		if (candidates.size() == 1) {
			// noinspection unchecked
			candidates.set(0, candidates.get(0) + " ");
		}

		// set all posibilities if there is no match for given buffer
		if (candidates.size() == 0) {
			for (String leftOverCandidate : hpCandidates) {
				candidates.add(leftOverCandidate);
			}
		}

		Collections.sort(candidates);
		return candidates.isEmpty() ? -1 : 0;
	}

	/**
	 * use consolereader to get currentline and get suggestion from henplus 
	 * base on this currentline
	 * @return List<String> list of posibilites 
	 */
	protected synchronized List<String> hpCompleter() {
		try {
			String partialCommand = HenPlusUtils.getCurrentCommandLine(session);
			String[] words = partialCommand.split(" ");
			String lastWord = "";
			if (words.length > 0) {
				lastWord = words[words.length - 1];
			}
			if (partialCommand.endsWith(" ") && words.length < 2) {
				lastWord = "";
			}

			Iterator it = null;
			if (partialCommand.contains(" ")) {
				it = henplus.getDispatcher().completeCommand(partialCommand,
						lastWord);
			} else {
				it = henplus.getDispatcher().getRegisteredCommandNames();
			}
			
			List<String> candidates = new ArrayList<String>();

			if (it != null) {
				while (it.hasNext()) {
					candidates.add((String) it.next());
				}
			}

			return candidates;

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return new ArrayList<String>();
		}
	}

	/**
	 * convert list to sortedset
	 * 
	 * @param list
	 * @return SortedSet<Stirng>
	 */
	private SortedSet<String> convert(List<String> list) {
		SortedSet set = new TreeSet<String>();
		if (list != null) {
			for (String element : list) {
				set.add(element);
			}
		}
		return set;
	}
}
