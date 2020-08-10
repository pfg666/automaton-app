package main;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import de.ls5.jlearn.interfaces.Automaton;
import de.ls5.jlearn.interfaces.State;
import de.ls5.jlearn.interfaces.Symbol;

public class AutomatonApp {
	private BufferedReader in;
	private PrintStream out;
	private Deque<String> commands;
	
	public AutomatonApp(BufferedReader in, PrintStream out) {
		this.in = in;
		this.out = out;
		this.commands = new ArrayDeque<String>();
	}
	
	public AutomatonApp() {
		this(new BufferedReader(new InputStreamReader(System.in)), System.out);
	}
	
	public void bufferCommands(Collection<String> commands) {
		this.commands.addAll(commands);
	}
	
	private String ask(String msg) throws IOException{
		out.println(msg);
		if (!commands.isEmpty()) {
			return commands.remove();
		}
		String newCommands; 
		while ( (newCommands =  in.readLine().trim()).isEmpty());
		String[] commandSplit = newCommands.split("\\s");
		if (commandSplit.length > 1) {
			Arrays.stream(commandSplit, 1, commandSplit.length).forEach(cmd -> commands.add(cmd));
		}
		return commandSplit[0];
	}
	
	public List<String> readTrace(String PATH) throws IOException {
		List<String> trace;
		trace = Files.readAllLines(Paths.get(PATH), StandardCharsets.US_ASCII);
		ListIterator<String> it = trace.listIterator();
		while(it.hasNext()) {
			String line = it.next();
			if (line.startsWith("#") || line.startsWith("!")) {
				it.remove();
			} else {
				 if ( line.isEmpty()) {
					 it.remove();
					 while (it.hasNext()) {
						 it.next();
						 it.remove();
					 } 
				 } 
			}
		}
		return trace;
	}
	
	public Collection<String> getRoute(Automaton automaton, State startingState, List<Symbol> inputSeq, IdStateMapping map) {
		Deque<String> strings = new ArrayDeque<String>();
		State currentState = startingState;
		for (Symbol input : inputSeq) {
			strings.add(input.toString() +"\\"+ currentState.getTransitionOutput(input) + " (" + map.getIdForState(currentState.getTransitionState(input)) + ") " );
			if (inputSeq.size() > 5) {
				strings.add("\n");
			}
			currentState = currentState.getTransitionState(input);
		}
		return strings;
	}
	
	private void displayWelcome() {
		out.println("Welcome to the hyp assistant. ");
		out.println("The assistant provides functionality for analyzing models in the form of a list of commands.");
		out.println("Commands/command parameters can be provided one at a time or in batch-mode using space separation.");
	}
	
	private void displayCommands() {
		out.println("Available commands (1 to 7) with associated parameters behind []: " +
				"\n 1. Load a new hypothesis [hypFile] \n " +
				"2. Get trace to state [stateId] \n " +
				"3. Get distinguishing seq between two states [stateId1, stateId2] \n " +
				"4. Run test [testFile] \n " +
				"5. Get an access sequence for every state [] \n " +
				"6. Display welcome text [] \n " +
				"7. Quit []");
	}
	
	public void play() throws IOException {
		Automaton loadedHyp = null;
		IdStateMapping map = null;
		Map<State, Set<Tuple2<State, Symbol>>> predMap = null;
		
		displayWelcome();
		while (true) {
			displayCommands();
			
			String command = ask("Command:");
			switch(command) {
			case "1":
				String hyp = ask("Hypothesis:");
				Tuple2<Automaton, IdStateMapping> result = Dot.readDotFile(hyp);
				loadedHyp = result.tuple0;
				map = result.tuple1;
				if (loadedHyp != null) {
					out.println("Loaded successfully");
				}
				predMap = new LinkedHashMap<>();
				AutomatonUtils.generatePredecessorMap(loadedHyp, predMap);
				break;
			case "2":
				if (loadedHyp == null) {
					out.println("Load a hyp first!");
				} else {
					int stateId = Integer.valueOf(ask("State ID:"));
					if (map.getStateWithId(stateId) == null) {
						out.println("State ID " + stateId + " is invalid");
					} else {
						List<Symbol> traceToState = AutomatonUtils.traceToState(loadedHyp, map.getStateWithId(stateId));
						Collection<String> strings = getRoute(loadedHyp, loadedHyp.getStart(), traceToState, map);
						out.println("Trace to state " + stateId + ": " + traceToState); 
						out.println("Trace to state " + stateId + " (full):  " + strings);
					}
				}
				break;

			case "3":
				if (loadedHyp == null) {
					out.println("Load a hyp first!");
				} else {
					int stateId1 = Integer.valueOf(ask("State ID1:"));
					int stateId2 = Integer.valueOf(ask("State ID2:"));
					Integer invStateId = null;
					if (map.getStateWithId(stateId1) == null) {
						invStateId = stateId1;
					} else {
						if (map.getStateWithId(stateId2) == null) {
							invStateId = stateId2;
						}
					}
					if (invStateId != null) {
						out.println("State ID " + invStateId + " is invalid");
					} else { 
						List<Symbol> distSeq = AutomatonUtils.distinguishingSeq(loadedHyp, map.getStateWithId(stateId1), map.getStateWithId(stateId2), map);
						out.println("Distinguishing trace: " + distSeq); 
						Collection<String> strings1 = getRoute(loadedHyp, map.getStateWithId(stateId1), distSeq, map);
						Collection<String> strings2 = getRoute(loadedHyp, map.getStateWithId(stateId2), distSeq, map);
						out.println("Trace from state " + stateId1 + ": " + strings1);
						out.println("Trace from state " + stateId2 + ": " + strings2);
					}
				}
				break;
				
			case "4":
				if (loadedHyp == null) {
					out.println("Load a hyp first!");
				} else {
					String traceFile = ask("Test file:");
					if (!new File(traceFile).isFile()) {
						out.println("Invalid file supplied");
					} else {
						Collection<String> trace = readTrace(traceFile);
						List<Symbol> traceSymbols = AutomatonUtils.buildSymbols(trace);
						Set<Symbol> traceSymbolSet = new LinkedHashSet<>(traceSymbols);
						traceSymbolSet.removeAll(loadedHyp.getAlphabet().getSymbolList());
						if (!traceSymbolSet.isEmpty()) {
							out.println("Inputs " + traceSymbolSet + " are not present in alphabet");
						} else {
							Collection<String> strings = getRoute(loadedHyp, loadedHyp.getStart(), traceSymbols, map);
							out.println("Test run:" + strings);
						}
					}
				}
				break;
			case "5":
				if (loadedHyp == null) {
					out.println("Load a hyp first!");
				} else {
					for (Integer stateId=0; stateId<loadedHyp.getAllStates().size(); stateId++) {
						List<Symbol> traceToState = AutomatonUtils.traceToState(loadedHyp, map.getStateWithId(stateId), predMap);
						Collection<String> strings = getRoute(loadedHyp, loadedHyp.getStart(), traceToState, map);
						out.println("Trace to state " + stateId + ": " + traceToState); 
						out.println("Trace to state " + stateId + " (full):  " + strings);
					}
				}
				
			case "6":
				displayWelcome();
				break ;

			case "7":
				out.println("Byee");
				return ;
			}
			
		}
	}



	public static void main(String args[]) throws IOException {
		AutomatonApp app = new AutomatonApp();
		if (args.length > 0) {
			app.bufferCommands(Arrays.asList(args));
		}
		app.play();
	}
	
}