/*
 * dk.brics.automaton
 *
 * Copyright (c) 2001-2007 Anders Moeller
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package brics.automaton;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Basic automata operations.
 */
final public class BasicOperations {

	private BasicOperations() {}

	/**
	 * Returns a new automaton that accepts the concatenation of the languages of
	 * the given automata.
	 * <p>
	 * Complexity: linear in number of states.
	 */
	static public Automaton concatenate(Automaton a1, Automaton a2) {
		if (a1.isSingleton() && a2.isSingleton())
			return BasicAutomata.makeString(a1.singleton + a2.singleton);
		a1 = a1.cloneExpanded();
		a2 = a2.cloneExpanded();
		for (State s : a1.getAcceptStates()) {
			s.accept = false;
			s.addEpsilon(a2.initial);
		}
		a1.deterministic = false;
		a1.checkMinimizeAlways();
		return a1;
	}

	/**
	 * Returns a new automaton that accepts the concatenation of the languages of
	 * the given automata.
	 * <p>
	 * Complexity: linear in total number of states.
	 */
	static public Automaton concatenate(List<Automaton> l) {
		if (l.isEmpty())
			return BasicAutomata.makeEmptyString();
		boolean all_singleton = true;
		for (Automaton a : l)
			if (!a.isSingleton()) {
				all_singleton = false;
				break;
			}
		if (all_singleton) {
			StringBuilder b = new StringBuilder();
			for (Automaton a : l)
				b.append(a.singleton);
			return BasicAutomata.makeString(b.toString());
		} else {
			for (Automaton a : l)
				if (a.isEmpty())
					return BasicAutomata.makeEmpty();
			Automaton b = l.get(0).cloneExpanded();
			Set<State> ac = b.getAcceptStates();
			boolean first = true;
			for (Automaton a : l)
				if (first)
					first = false;
				else {
					if (a.isEmptyString())
						continue;
					Automaton aa = a.cloneExpanded();
					Set<State> ns = aa.getAcceptStates();
					for (State s : ac) {
						s.accept = false;
						s.addEpsilon(aa.initial);
						if (s.accept)
							ns.add(s);
					}
					ac = ns;
				}
			b.deterministic = false;
			b.checkMinimizeAlways();
			return b;
		}
	}

	/**
	 * Returns a new automaton that accepts the union of the empty string and the
	 * language of the given automaton.
	 * <p>
	 * Complexity: linear in number of states.
	 */
	static public Automaton optional(Automaton a)
	{
		a = a.cloneExpanded();
		State s = new State();
		s.addEpsilon(a.initial);
		s.accept = true;
		a.initial = s;
		a.deterministic = false;
		a.checkMinimizeAlways();
		return a;
	}

	/**
	 * Returns a new automaton that accepts the Kleene star (zero or more
	 * concatenated repetitions) of the language of the given automaton.
	 * <p>
	 * Complexity: linear in number of states.
	 */
	static public Automaton repeat(Automaton a) {
		a = a.cloneExpanded();
		State s = new State();
		s.accept = true;
		s.addEpsilon(a.initial);
		for (State p : a.getAcceptStates())
			p.addEpsilon(s);
		a.initial = s;
		a.deterministic = false;
		a.checkMinimizeAlways();
		return a;
	}

	/**
	 * Returns a new automaton that accepts <code>min</code> or more
	 * concatenated repetitions of the language of the given automaton.
	 * <p>
	 * Complexity: linear in number of states and in <code>min</code>.
	 */
	static public Automaton repeat(Automaton a, int min) {
		if (min == 0)
			return repeat(a);
		List<Automaton> as = new ArrayList<Automaton>();
		while (min-- > 0)
			as.add(a);
		as.add(repeat(a));
		return concatenate(as);
	}

	/**
	 * Returns a new automaton that accepts between <code>min</code> and
	 * <code>max</code> (including both) concatenated repetitions of the
	 * language of the given automaton.
	 * <p>
	 * Complexity: linear in number of states and in <code>min</code> and
	 * <code>max</code>.
	 */
	static public Automaton repeat(Automaton a, int min, int max) {
		if (min > max)
			return BasicAutomata.makeEmpty();
		max -= min;
		a.expandSingleton();
		if (min == 0)
			a = BasicAutomata.makeEmptyString();
		else if (min == 1)
			a = a.clone();
		else {
			List<Automaton> as = new ArrayList<Automaton>();
			while (min-- > 0)
				as.add(a);
			a = concatenate(as);
		}
		if (max > 0) {
			Automaton d = a.clone();
			while (--max > 0) {
				Automaton c = a.clone();
				for (State p : c.getAcceptStates())
					p.addEpsilon(d.initial);
				d = c;
			}
			for (State p : a.getAcceptStates())
				p.addEpsilon(d.initial);
			a.deterministic = false;
			a.checkMinimizeAlways();
		}
		return a;
	}

	/**
	 * Returns a new (deterministic) automaton that accepts the complement of the
	 * language of the given automaton.
	 * <p>
	 * Complexity: linear in number of states (if already deterministic).
	 */
	static public Automaton complement(Automaton a) {
		a = a.cloneExpanded();
		a.determinize();
		a.totalize();
		for (State p : a.getStates())
			p.accept = !p.accept;
		a.removeDeadTransitions();
		return a;
	}

	/**
	 * Returns a new (deterministic) automaton that accepts the intersection of
	 * the language of <code>a1</code> and the complement of the language of
	 * <code>a2</code>. As a side-effect, the automata may be determinized, if not
	 * already deterministic.
	 * <p>
	 * Complexity: quadratic in number of states (if already deterministic).
	 */
	static public Automaton minus(Automaton a1, Automaton a2) {
		if (a1.isEmpty() || a1 == a2)
			return BasicAutomata.makeEmpty();
		if (a2.isEmpty())
			return a1.clone();
		if (a1.isSingleton()) {
			if (a2.run(a1.singleton))
				return BasicAutomata.makeEmpty();
			else
				return a1.clone();
		}
		return intersection(a1, a2.complement());
	}

	/**
	 * Returns a new (deterministic) automaton that accepts the intersection of
	 * the languages of the given automata. As a side-effect, both
	 * automata are determinized, if not already deterministic.
	 * <p>
	 * Complexity: quadratic in number of states (if already deterministic).
	 */
	static public Automaton intersection(Automaton a1, Automaton a2) {
		if (a1.isSingleton()) {
			if (a2.run(a1.singleton))
				return a1.clone();
			else
				return BasicAutomata.makeEmpty();
		}
		if (a2.isSingleton()) {
			if (a1.run(a2.singleton))
				return a2.clone();
			else
				return BasicAutomata.makeEmpty();
		}
		a1.determinize();
		a2.determinize();
		if (a1 == a2)
			return a1.clone();
		Transition[][] transitions1 = Automaton.getSortedTransitions(a1.getStates());
		Transition[][] transitions2 = Automaton.getSortedTransitions(a2.getStates());
		Automaton c = new Automaton();
		LinkedList<StatePair> worklist = new LinkedList<StatePair>();
		HashMap<StatePair, StatePair> newstates = new HashMap<StatePair, StatePair>();
		State s = new State();
		c.initial = s;
		StatePair p = new StatePair(s, a1.initial, a2.initial);
		worklist.add(p);
		newstates.put(p, p);
		while (worklist.size() > 0) {
			p = worklist.removeFirst();
			p.s.accept = p.s1.accept && p.s2.accept;
			Transition[] t1 = transitions1[p.s1.number];
			Transition[] t2 = transitions2[p.s2.number];
			for (int n1 = 0, n2 = 0; n1 < t1.length && n2 < t2.length;) {
				if (t1[n1].max < t2[n2].min)
					n1++;
				else if (t2[n2].max < t1[n1].min)
					n2++;
				else {
					StatePair q = new StatePair(t1[n1].to, t2[n2].to);
					StatePair r = newstates.get(q);
					if (r == null) {
						q.s = new State();
						worklist.add(q);
						newstates.put(q, q);
						r = q;
					}
					char min = t1[n1].min > t2[n2].min ? t1[n1].min : t2[n2].min;
					char max = t1[n1].max < t2[n2].max ? t1[n1].max : t2[n2].max;
					p.s.transitions.add(new Transition(min, max, r.s));
					if (t1[n1].max < t2[n2].max)
						n1++;
					else
						n2++;
				}
			}
		}
		c.deterministic = true;
		c.removeDeadTransitions();
		c.checkMinimizeAlways();
		return c;
	}

	/**
	 * Returns true if the language of <code>a1</code> is a subset of the
	 * language of <code>a2</code>.
	 */
	public static boolean subsetOf(Automaton a1, Automaton a2) {
		if (a1 == a2)
			return true;
		if (a1.isSingleton()) {
			if (a2.isSingleton())
				return a1.singleton.equals(a2.singleton);
			return a2.run(a1.singleton);
		}
		a2.determinize();
		Transition[][] transitions1 = Automaton.getSortedTransitions(a1.getStates());
		Transition[][] transitions2 = Automaton.getSortedTransitions(a2.getStates());
		LinkedList<StatePair> worklist = new LinkedList<StatePair>();
		HashSet<StatePair> visited = new HashSet<StatePair>();
		StatePair p = new StatePair(a1.initial, a2.initial);
		worklist.add(p);
		visited.add(p);
		while (worklist.size() > 0) {
			p = worklist.removeFirst();
			Transition[] t1 = transitions1[p.s1.number];
			Transition[] t2 = transitions2[p.s2.number];
			int n1 = 0, n2 = 0;
			while (n1 < t1.length && n2 < t2.length) {
				if (t1[n1].max < t2[n2].min)
					return false;
				else if (t2[n2].max < t1[n1].min)
					n2++;
				else {
					StatePair q = new StatePair(t1[n1].to, t2[n2].to);
					if (!visited.contains(q)) {
						if (q.s1.accept && !q.s2.accept)
							return false;
						worklist.add(q);
						visited.add(q);
					}
					if (t1[n1].max <= t2[n2].max)
						n1++;
					else
						n2++;
				}
			}
			if (n1 < t1.length && !(n2 < t2.length))
				return false;
		}
		return true;
	}

	/**
	 * Returns a new automaton that accepts the union of the languages of the given automata.
	 * <p>
	 * Complexity: linear in number of states.
	 */
	public static Automaton union(Automaton a1, Automaton a2) {
		if ((a1.isSingleton() && a2.isSingleton() && a1.singleton.equals(a2.singleton)) || a1 == a2)
			return a1.clone();
		a1 = a1.cloneExpanded();
		a2 = a2.cloneExpanded();
		State s = new State();
		s.addEpsilon(a1.initial);
		s.addEpsilon(a2.initial);
		a1.initial = s;
		a1.deterministic = false;
		a1.checkMinimizeAlways();
		return a1;
	}

	/**
	 * Returns a new automaton that accepts the union of the languages of the given automata.
	 * <p>
	 * Complexity: linear in number of states.
	 */
	public static Automaton union(Collection<Automaton> l) {
		State s = new State();
		for (Automaton b : l) {
			if (b.isEmpty())
				continue;
			Automaton bb = b.cloneExpanded();
			s.addEpsilon(bb.initial);
		}
		Automaton a = new Automaton();
		a.initial = s;
		a.deterministic = false;
		a.checkMinimizeAlways();
		return a;
	}

	/**
	 * Determinizes the given automaton.
	 * <p>
	 * Complexity: exponential in number of states.
	 */
	public static void determinize(Automaton a) {
		if (a.deterministic || a.isSingleton())
			return;
		Set<State> initialset = new HashSet<State>();
		initialset.add(a.initial);
		determinize(a, initialset);
	}

	/**
	 * Determinizes the given automaton using the given set of initial states.
	 */
	static void determinize(Automaton a, Set<State> initialset) {
		char[] points = a.getStartPoints();
		// subset construction
		Map<Set<State>, Set<State>> sets = new HashMap<Set<State>, Set<State>>();
		LinkedList<Set<State>> worklist = new LinkedList<Set<State>>();
		Map<Set<State>, State> newstate = new HashMap<Set<State>, State>();
		sets.put(initialset, initialset);
		worklist.add(initialset);
		a.initial = new State();
		newstate.put(initialset, a.initial);
		while (worklist.size() > 0) {
			Set<State> s = worklist.removeFirst();
			State r = newstate.get(s);
			for (State q : s){
				if (q.accept) {
					r.accept = true;
					if(q.getActor()!=null){
						r.addActor(q.getActor());
					}
				}
			}
			for (int n = 0; n < points.length; n++) {
				Set<State> p = new HashSet<State>();
				for (State q : s)
					for (Transition t : q.transitions)
						if (t.min <= points[n] && points[n] <= t.max)
							p.add(t.to);
				if (!sets.containsKey(p)) {
					sets.put(p, p);
					worklist.add(p);
					newstate.put(p, new State());
				}
				State q = newstate.get(p);
				char min = points[n];
				char max;
				if (n + 1 < points.length)
					max = (char)(points[n + 1] - 1);
				else
					max = Character.MAX_VALUE;
				r.transitions.add(new Transition(min, max, q));
			}
		}
		a.deterministic = true;
		a.removeDeadTransitions();
	}


	/**
	 * Adds epsilon transitions to the given automaton.
	 * This method adds extra character interval transitions that are equivalent to the given
	 * set of epsilon transitions.
	 * @param pairs collection of {@link StatePair} objects representing pairs of source/destination states
	 *        where epsilon transitions should be added
	 */
	public static void addEpsilons(Automaton a, Collection<StatePair> pairs) {
		a.expandSingleton();
		HashMap<State, HashSet<State>> forward = new HashMap<State, HashSet<State>>();
		HashMap<State, HashSet<State>> back = new HashMap<State, HashSet<State>>();
		for (StatePair p : pairs) {
			HashSet<State> to = forward.get(p.s1);
			if (to == null) {
				to = new HashSet<State>();
				forward.put(p.s1, to);
			}
			to.add(p.s2);
			HashSet<State> from = back.get(p.s2);
			if (from == null) {
				from = new HashSet<State>();
				back.put(p.s2, from);
			}
			from.add(p.s1);
		}
		// calculate epsilon closure
		LinkedList<StatePair> worklist = new LinkedList<StatePair>(pairs);
		HashSet<StatePair> workset = new HashSet<StatePair>(pairs);
		while (!worklist.isEmpty()) {
			StatePair p = worklist.removeFirst();
			workset.remove(p);
			HashSet<State> to = forward.get(p.s2);
			HashSet<State> from = back.get(p.s1);
			if (to != null) {
				for (State s : to) {
					StatePair pp = new StatePair(p.s1, s);
					if (!pairs.contains(pp)) {
						pairs.add(pp);
						forward.get(p.s1).add(s);
						back.get(s).add(p.s1);
						worklist.add(pp);
						workset.add(pp);
						if (from != null) {
							for (State q : from) {
								StatePair qq = new StatePair(q, p.s1);
								if (!workset.contains(qq)) {
									worklist.add(qq);
									workset.add(qq);
								}
							}
						}
					}
				}
			}
		}
		// add transitions
		for (StatePair p : pairs)
			p.s1.addEpsilon(p.s2);
		a.deterministic = false;
		a.checkMinimizeAlways();
	}

	/**
	 * Returns true if the given automaton accepts the empty string and nothing else.
	 */
	public static boolean isEmptyString(Automaton a) {
		if (a.isSingleton())
			return a.singleton.length() == 0;
		else
			return a.initial.accept && a.initial.transitions.isEmpty();
	}

	/**
	 * Returns true if the given automaton accepts no strings.
	 */
	public static boolean isEmpty(Automaton a) {
		if (a.isSingleton())
			return false;
		return !a.initial.accept && a.initial.transitions.isEmpty();
	}

	/**
	 * Returns true if the given automaton accepts all strings.
	 */
	public static boolean isTotal(Automaton a) {
		if (a.isSingleton())
			return false;
		if (a.initial.accept && a.initial.transitions.size() == 1) {
			Transition t = a.initial.transitions.iterator().next();
			return t.to == a.initial && t.min == Character.MIN_VALUE && t.max == Character.MAX_VALUE;
		}
		return false;
	}

	/**
	 * Returns a shortest accepted/rejected string. If more than one string is
	 * found, the lexicographically first is returned.
	 * @param accepted if true, look for accepted strings; otherwise, look for rejected strings
	 * @return the string, null if none found
	 */
	public static String getShortestExample(Automaton a, boolean accepted) {
		if (a.isSingleton()) {
			if (accepted)
				return a.singleton;
			else if (a.singleton.length() > 0)
				return "";
			else
				return "\u0000";

		}
		return getShortestExample(a.initial, accepted);
	}

	static String getShortestExample(State s, boolean accepted) {
		return getShortestExample(s, accepted, new HashMap<State, String>());
	}

	private static String getShortestExample(State s, boolean accepted, Map<State, String> map) {
		if (s.accept == accepted)
			return "";
		if (map.containsKey(s))
			return map.get(s);
		map.put(s, null);
		String best = null;
		for (Transition t : s.transitions) {
			String b = getShortestExample(t.to, accepted, map);
			if (b != null) {
				b = t.min + b;
				if (best == null || b.length() < best.length() || (b.length() == best.length() && b.compareTo(best) < 0))
					best = b;
			}
		}
		map.put(s, best);
		return best;
	}

	/**
	 * Returns true if the given string is accepted by the automaton.
	 * <p>
	 * Complexity: linear in the length of the string.
	 * <p>
	 * <b>Note:</b> for full performance, use the {@link RunAutomaton} class.
	 */
	public static boolean run(Automaton a, String s) {
		if (a.isSingleton())
			return s.equals(a.singleton);
		if (a.deterministic) {
			State p = a.initial;
			for (int i = 0; i < s.length(); i++) {
				State q = p.step(s.charAt(i));
				if (q == null)
					return false;
				p = q;
			}
			return p.accept;
		} else {
			Set<State> states = a.getStates();
			Automaton.setStateNumbers(states);
			LinkedList<State> pp = new LinkedList<State>();
			LinkedList<State> pp_other = new LinkedList<State>();
			BitSet bb = new BitSet(states.size());
			BitSet bb_other = new BitSet(states.size());
			pp.add(a.initial);
			ArrayList<State> dest = new ArrayList<State>();
			boolean accept = a.initial.accept;
			for (int i = 0; i < s.length(); i++) {
				char c = s.charAt(i);
				accept = false;
				pp_other.clear();
				bb_other.clear();
				for (State p : pp) {
					dest.clear();
					p.step(c, dest);
					for (State q : dest) {
						if (q.accept)
							accept = true;
						if (!bb_other.get(q.number)) {
							bb_other.set(q.number);
							pp_other.add(q);
						}
					}
				}
				LinkedList<State> tp = pp;
				pp = pp_other;
				pp_other = tp;
				BitSet tb = bb;
				bb = bb_other;
				bb_other = tb;
			}
			return accept;
		}
	}
}
