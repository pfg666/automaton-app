package main;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import de.ls5.jlearn.interfaces.Alphabet;
import de.ls5.jlearn.interfaces.Automaton;
import de.ls5.jlearn.interfaces.State;
import de.ls5.jlearn.interfaces.Symbol;
import de.ls5.jlearn.interfaces.Word;
import de.ls5.jlearn.shared.SymbolImpl;
import de.ls5.jlearn.shared.WordImpl;

public class AutomatonUtils {

	public static List<State> getStatesInBFSOrder(Automaton automaton) {
		List<Symbol> inputs = automaton.getAlphabet().getSymbolList();
		java.util.Collections.sort(inputs);

		Queue<State> statestovisit = new LinkedList<State>();
		List<State> result = new ArrayList<State>();
		HashSet<State> states = new HashSet<State>(); // to check if state is
														// not seen already by
														// other transition

		statestovisit.offer(automaton.getStart());
		result.add(automaton.getStart());
		states.add(automaton.getStart());

		State current = (State) statestovisit.poll();
		while (current != null) {
			for (Symbol input : inputs) {
				State s = current.getTransitionState(input);
				if ((s != null) && (!states.contains(s))) {
					statestovisit.offer(s);
					result.add(s);
					states.add(s);
				}
			}

			if (statestovisit.isEmpty()) {
				break;
			}
			current = (State) statestovisit.poll();
		}

		return result;
	}
	
	
	/**
	 * Gives a minimal length path from the start state to the given state. We build this path based 
	 * on the random value generator. Note, there can be several minimal paths to the given state. We only 
	 * return one. The length is the first order. Traces of equal length are order by the position of the last input
	 * in the alphabet.
	 */
	public static List<Symbol> traceToState(Automaton automaton, State state) {
		Map<State, Set<Tuple2<State, Symbol>>> predMap = new HashMap<>();
		generatePredecessorMap(automaton, predMap);
		return traceToState(automaton, state, predMap);
	}
	
	public static List<Symbol> traceToState(Automaton automaton, State state, Map<State, Set<Tuple2<State, Symbol>>> predMap) {
		Set<State> visited = new HashSet<>();
		Queue<VisitStruct> toVisit = new ArrayDeque<>();
		toVisit.add(new VisitStruct(state, new LinkedList<>()));
		
		while(!toVisit.isEmpty()) {
			VisitStruct crtStruct = toVisit.poll();
			State crtState = crtStruct.getState();
			if (crtState.equals(automaton.getStart())) {
				return crtStruct.getSymbols();
			} else {
				visited.add(crtState);
				for (Tuple2<State, Symbol> trans : predMap.get(crtState)) {
					if (!visited.contains(trans.tuple0)) {
						List<Symbol> symbolsFromState = new LinkedList<>();
						symbolsFromState.add(trans.tuple1);
						symbolsFromState.addAll(crtStruct.getSymbols());
						toVisit.add(new VisitStruct(trans.tuple0, symbolsFromState));
					}
				}
			}
		}
		
		return null;
	}
	
	static class VisitStruct {
		private final State state;
		private final List<Symbol> symbols;
		VisitStruct(State state, List<Symbol> symbols) {
			this.state = state;
			this.symbols = symbols;
		}
		public State getState() {
			return state;
		}
		public List<Symbol> getSymbols() {
			return symbols;
		}
		
	}
	
	public static void generatePredecessorMap(Automaton automaton, Map<State, Set<Tuple2<State, Symbol>>> predMap) {
		List<Symbol> inputs = automaton.getAlphabet().getSymbolList();
		java.util.Collections.sort(inputs);
		for (State state : automaton.getAllStates()) {
			for (Symbol input : inputs) {
				State succState = state.getTransitionState(input);
				Set<Tuple2<State, Symbol>> predTuples = predMap.get(succState); 
				if (predTuples == null) {
					predTuples = new LinkedHashSet<>();
					predMap.put(succState, predTuples);
				}
				predTuples.add(new Tuple2<>(state, input));
			}
		}
		for (State state : automaton.getAllStates()) {
			if (predMap.get(state) == null) {
				throw new RuntimeException(state.getId() + " ");
			}
		}
	}
	
	
	public static List<Symbol> distinguishingSeq(Automaton automaton, State state1, State state2, IdStateMapping map) {
		Map<State, Set<Tuple2<State, Symbol>>> predMap = new LinkedHashMap<>();
		generatePredecessorMap(automaton, predMap);
		Alphabet alpha = automaton.getAlphabet();
		List<Symbol> symbols = alpha.getSymbolList();
		List<Symbol> distSeq = getDistinguishingSeq(automaton, symbols, state1, state2, map, predMap);
		return distSeq;
	}

	/**
	 * Gives a minimal distinguishing sequence. Be warned that the implementation is inefficient.
	 */
	public static List<Symbol> distinguishingSeq(Automaton automaton, State state1, State state2, IdStateMapping map, Map<State, Set<Tuple2<State, Symbol>>> predMap) {
		Alphabet alpha = automaton.getAlphabet();
		List<Symbol> symbols = alpha.getSymbolList();
		List<Symbol> distSeq = getDistinguishingSeq(automaton, symbols, state1, state2, map, predMap);
		return distSeq;
	}
	
	public static Word createWordFromSymbols(Collection<Symbol> symbols) {
		Word word = new WordImpl();
		for (Symbol symbol : symbols) {
			word.addSymbol(symbol);
		}
		return word;
	}
	
	public static List<Symbol> buildSymbols(Collection<String> trace) {
		List<Symbol> traceSymbols = new ArrayList<Symbol>();
		for (String str : trace) {
			traceSymbols.add(new SymbolImpl(str));
		}
		return traceSymbols;
	}

	private static List<Symbol> getDistinguishingSeq(Automaton automaton,
		List<Symbol> symbols, State state1, State state2, IdStateMapping map, Map<State, Set<Tuple2<State, Symbol>>> predMap) {
		List<List<Symbol>> middleParts = new ArrayList<List<Symbol>>();
		List<Symbol> traceToState1 = traceToState(automaton, state1, predMap);
		List<Symbol> traceToState2 = traceToState(automaton, state2, predMap);
		List<Tuple2<State,State>> reachedSatePairs =new ArrayList<>();

		List<Symbol> selectedMiddlePart = new ArrayList<Symbol>();
		middleParts.add(selectedMiddlePart);
		while(true) {
			List<Symbol> traceFromState1 = new ArrayList<Symbol>();
			traceFromState1.addAll(traceToState1);
			traceFromState1.addAll(selectedMiddlePart);
			List<Symbol> traceFromState2 = new ArrayList<Symbol>();
			traceFromState2.addAll(traceToState2);
			traceFromState2.addAll(selectedMiddlePart);
			State reachedStateFrom1 = getState(automaton, traceFromState1);
			State reachedStateFrom2 = getState(automaton, traceFromState2);
			
			boolean diffFound = false;
			
			for (Symbol input : symbols) {
				if (!reachedStateFrom1.getTransitionOutput(input).equals(reachedStateFrom2
						.getTransitionOutput(input))) {
					System.out.println(
							"(s" + map.getIdForState(reachedStateFrom1) + ") " + input + "/" + reachedStateFrom1.getTransitionOutput(input) 
							+  " (s" + map.getIdForState(reachedStateFrom1.getTransitionState(input)) + ") " +
							" != (s" + map.getIdForState(reachedStateFrom2) + ") " + input  + "/" + reachedStateFrom2.getTransitionOutput(input) 
							+  " (s" + map.getIdForState(reachedStateFrom2.getTransitionState(input)) + ") ");
					selectedMiddlePart.add(input);
					diffFound = true;
					break;
				}
			}
			if (diffFound) {
				break;
			}
			
			Tuple2<State,State> reachedStatePair = new Tuple2<>(reachedStateFrom1, reachedStateFrom2);
			reachedSatePairs.add(reachedStatePair);
			
			middleParts.remove(0);
			for (Symbol input : symbols) {
				State stateReachedAfterSymFromState1 = reachedStateFrom1.getTransitionState(input);
				State stateReachedAfterSymFromState2 = reachedStateFrom2.getTransitionState(input);
				Tuple2<State, State> statePairAfterSymbol = new Tuple2<State,State>(stateReachedAfterSymFromState1, stateReachedAfterSymFromState2);
				if (!reachedSatePairs.contains(statePairAfterSymbol)) {
					List<Symbol> newMiddlePart = new ArrayList<Symbol>(selectedMiddlePart);
					newMiddlePart.add(input);
					middleParts.add(newMiddlePart);
				}
			}
			
			selectedMiddlePart = middleParts.get(0);
		}
		
		return selectedMiddlePart;
	}
	
	
	
	public static State getState(Automaton automaton, List<Symbol> symbols ) {
		State currentState = automaton.getStart();
		for(Symbol symbol : symbols) {
			currentState = currentState.getTransitionState(symbol);
		}
		return currentState;
	}
}