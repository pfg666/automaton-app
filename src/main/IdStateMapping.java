package main;

import java.util.HashMap;
import java.util.Map;

import de.ls5.jlearn.interfaces.State;

public class IdStateMapping {
	private Map<Integer, State> map;
	private Map<State, Integer> revMap;

	public IdStateMapping(Map<Integer, State> map) {
		this.map = map;
		this.revMap = new HashMap<>(map.size());
		this.map.forEach((id,s) -> {
			revMap.put(s, id);
			}
		);
	}
	
	public State getStateWithId(Integer id) {
		return map.get(id);
	}
	
	public Integer getIdForState(State state) {
		return revMap.get(state);
	}
}
