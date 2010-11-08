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

import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/* Class invariants:
 *
 * - An automaton is either represented explicitly (with State and Transition objects)
 *   or with a singleton string in case the automaton accepts exactly one string.
 * - Automata are always reduced (see reduce())
 *   and have no transitions to dead states (see removeDeadTransitions()).
 * - If an automaton is nondeterministic, then isDeterministic() returns false (but
 *   the converse is not required).
 */

/**
 * Finite-state automaton with regular expression operations.
 * <p>
 * Automata are represented using {@link State} and {@link Transition} objects.
 * Implicitly, all states and transitions of an automaton are reachable from its initial state.
 * If the states or transitions are manipulated manually, the {@link #restoreInvariant()}
 * and {@link #setDeterministic(boolean)} methods should be used afterwards to restore
 * certain representation invariants that are assumed by the built-in automata operations.
 * @author Anders M&oslash;ller &lt;<a href="mailto:amoeller@brics.dk">amoeller@brics.dk</a>&gt;
 */
public class Automaton implements Serializable, Cloneable {

	static final long serialVersionUID = 10001;

	/**
	 * Minimize using Huffman's O(n<sup>2</sup>) algorithm.
	 * This is the standard text-book algorithm.
	 * @see #setMinimization(int)
	 */
	public static final int MINIMIZE_HUFFMAN = 0;

	/**
	 * Minimize using Brzozowski's O(2<sup>n</sup>) algorithm.
	 * This algorithm uses the reverse-determinize-reverse-determinize trick, which has a bad
	 * worst-case behavior but often works very well in practice
	 * (even better than Hopcroft's!).
	 * @see #setMinimization(int)
	 */
	public static final int MINIMIZE_BRZOZOWSKI = 1;

	/**
	 * Minimize using Hopcroft's O(n log n) algorithm.
	 * This is regarded as one of the most generally efficient algorithms that exist.
	 * @see #setMinimization(int)
	 */
	public static final int MINIMIZE_HOPCROFT = 2;

	/** Selects minimization algorithm (default: <code>MINIMIZE_HOPCROFT</code>). */
	static int minimization = MINIMIZE_HOPCROFT;

	/** Initial state of this automaton. */
	State initial;

	/** If true, then this automaton is definitely deterministic
	 (i.e., there are no choices for any run, but a run may crash). */
	boolean deterministic;

	/** Extra data associated with this automaton. */
	Object info;

	/** Hash code. Recomputed by {@link #minimize()}. */
	int hash_code;

	/** Singleton string. Null if not applicable. */
	String singleton;

	/** Minimize always flag. */
	static boolean minimize_always;

	/**
	 * Constructs a new automaton that accepts the empty language.
	 * Using this constructor, automata can be constructed manually from
	 * {@link State} and {@link Transition} objects.
	 * @see #setInitialState(State)
	 * @see State
	 * @see Transition
	 */
	public Automaton() {
		initial = new State();
		deterministic = true;
		singleton = null;
	}

	/**
	 * Selects minimization algorithm (default: <code>MINIMIZE_HOPCROFT</code>).
	 * @param algorithm minimization algorithm
	 */
	static public void setMinimization(int algorithm) {
		minimization = algorithm;
	}

	/**
	 * Sets or resets minimize always flag.
	 * If this flag is set, then {@link #minimize()} will automatically
	 * be invoked after all operations that otherwise may produce non-minimal automata.
	 * By default, the flag is not set.
	 * @param flag if true, the flag is set
	 */
	static public void setMinimizeAlways(boolean flag) {
		minimize_always = flag;
	}

	void checkMinimizeAlways() {
		if (minimize_always)
			minimize();
	}

	boolean isSingleton() {
		return singleton!=null;
	}

	/**
	 * Returns the singleton string for this automaton.
	 * An automaton that accepts exactly one string <i>may</i> be represented
	 * in singleton mode. In that case, this method may be used to obtain the string.
	 * @return string, null if this automaton is not in singleton mode.
	 */
	public String getSingleton() {
		return singleton;
	}

	/**
	 * Sets initial state.
	 * @param s state
	 */
	public void setInitialState(State s) {
		initial = s;
		singleton = null;
	}

	/**
	 * Gets initial state.
	 * @return state
	 */
	public State getInitialState() {
		expandSingleton();
		return initial;
	}

	/**
	 * Returns deterministic flag for this automaton.
	 * @return true if the automaton is definitely deterministic, false if the automaton
	 *         may be nondeterministic
	 */
	public boolean isDeterministic() {
		return deterministic;
	}

	/**
	 * Sets deterministic flag for this automaton.
	 * This method should (only) be used if automata are constructed manually.
	 * @param deterministic true if the automaton is definitely deterministic, false if the automaton
	 *                      may be nondeterministic
	 */
	public void setDeterministic(boolean deterministic) {
		this.deterministic = deterministic;
	}

	/**
	 * Associates extra information with this automaton.
	 * @param info extra information
	 */
	public void setInfo(Object info) {
		this.info = info;
	}

	/**
	 * Returns extra information associated with this automaton.
	 * @return extra information
	 * @see #setInfo(Object)
	 */
	public Object getInfo()	{
		return info;
	}

	/**
	 * Returns the set of states that are reachable from the initial state.
	 * @return set of {@link State} objects
	 */
	public Set<State> getStates() {
		expandSingleton();
		HashSet<State> visited = new HashSet<State>();
		LinkedList<State> worklist = new LinkedList<State>();
		worklist.add(initial);
		visited.add(initial);
		while (worklist.size() > 0) {
			State s = worklist.removeFirst();
			for (Transition t : s.transitions)
				if (!visited.contains(t.to)) {
					visited.add(t.to);
					worklist.add(t.to);
				}
		}
		return visited;
	}

	/**
	 * Returns the set of reachable accept states.
	 * @return set of {@link State} objects
	 */
	public Set<State> getAcceptStates() {
		expandSingleton();
		HashSet<State> accepts = new HashSet<State>();
		HashSet<State> visited = new HashSet<State>();
		LinkedList<State> worklist = new LinkedList<State>();
		worklist.add(initial);
		visited.add(initial);
		while (worklist.size() > 0) {
			State s = worklist.removeFirst();
			if (s.accept)
				accepts.add(s);
			for (Transition t : s.transitions)
				if (!visited.contains(t.to)) {
					visited.add(t.to);
					worklist.add(t.to);
				}
		}
		return accepts;
	}

	/**
	 * Assigns consecutive numbers to the given states.
	 */
	static void setStateNumbers(Set<State> states) {
		int number = 0;
		for (State s : states)
			s.number = number++;
	}

	/**
	 * Adds transitions to explicit crash state to ensure that transition function is total.
	 */
	void totalize() {
		State s = new State();
		s.transitions.add(new Transition(Character.MIN_VALUE, Character.MAX_VALUE, s));
		for (State p : getStates()) {
			int maxi = Character.MIN_VALUE;
			for (Transition t : p.getSortedTransitions(false)) {
				if (t.min > maxi)
					p.transitions.add(new Transition((char)maxi, (char)(t.min - 1), s));
				if (t.max + 1 > maxi)
					maxi = t.max + 1;
			}
			if (maxi <= Character.MAX_VALUE)
				p.transitions.add(new Transition((char)maxi, Character.MAX_VALUE, s));
		}
	}

	/**
	 * Restores representation invariant.
	 * This method must be invoked before any built-in automata operation is performed
	 * if automaton states or transitions are manipulated manually.
	 * @see #setDeterministic(boolean)
	 */
	public void restoreInvariant() {
		removeDeadTransitions();
		hash_code = 0;
	}

	/**
	 * Reduces this automaton.
	 * An automaton is "reduced" by combining overlapping and adjacent edge intervals with same destination.
	 */
	public void reduce() {
		if (isSingleton())
			return;
		Set<State> states = getStates();
		setStateNumbers(states);
		for (State s : states) {
			List<Transition> st = s.getSortedTransitions(true);
			s.resetTransitions();
			State p = null;
			int min = -1, max = -1;
			for (Transition t : st) {
				if (p == t.to) {
					if (t.min <= max + 1) {
						if (t.max > max)
							max = t.max;
					} else {
						if (p != null)
							s.transitions.add(new Transition((char)min, (char)max, p));
						min = t.min;
						max = t.max;
					}
				} else {
					if (p != null)
						s.transitions.add(new Transition((char)min, (char)max, p));
					p = t.to;
					min = t.min;
					max = t.max;
				}
			}
			if (p != null)
				s.transitions.add(new Transition((char)min, (char)max, p));
		}
	}

	/**
	 * Returns sorted array of all interval start points.
	 */
	public char[] getStartPoints() {
		Set<Character> pointset = new HashSet<Character>();
		for (State s : getStates()) {
			pointset.add(Character.MIN_VALUE);
			for (Transition t : s.transitions) {
				pointset.add(t.min);
				if (t.max < Character.MAX_VALUE)
					pointset.add((char)(t.max + 1));
			}
		}
		char[] points = new char[pointset.size()];
		int n = 0;
		for (Character m : pointset)
			points[n++] = m;
		Arrays.sort(points);
		return points;
	}

	/**
	 * Returns the set of live states. A state is "live" if an accept state is reachable from it.
	 * @return set of {@link State} objects
	 */
	public Set<State> getLiveStates() {
		expandSingleton();
		return getLiveStates(getStates());
	}

	private Set<State> getLiveStates(Set<State> states) {
		HashMap<State, Set<State>> map = new HashMap<State, Set<State>>();
		for (State s : states)
			map.put(s, new HashSet<State>());
		for (State s : states)
			for (Transition t : s.transitions)
				map.get(t.to).add(s);
		Set<State> live = new HashSet<State>(getAcceptStates());
		LinkedList<State> worklist = new LinkedList<State>(live);
		while (worklist.size() > 0) {
			State s = worklist.removeFirst();
			for (State p : map.get(s))
				if (!live.contains(p)) {
					live.add(p);
					worklist.add(p);
				}
		}
		return live;
	}

	/**
	 * Removes transitions to dead states and calls {@link #reduce()}.
	 * (A state is "dead" if no accept state is reachable from it.)
	 */
	public void removeDeadTransitions() {
		if (isSingleton())
			return;
		Set<State> states = getStates();
		Set<State> live = getLiveStates(states);
		for (State s : states) {
			Set<Transition> st = s.transitions;
			s.resetTransitions();
			for (Transition t : st)
				if (live.contains(t.to))
					s.transitions.add(t);
		}
		reduce();
	}

	/**
	 * Returns a sorted array of transitions for each state (and sets state numbers).
	 */
	static Transition[][] getSortedTransitions(Set<State> states) {
		setStateNumbers(states);
		Transition[][] transitions = new Transition[states.size()][];
		for (State s : states)
			transitions[s.number] = s.getSortedTransitionArray(false);
		return transitions;
	}

	/**
	 * Expands singleton representation to normal representation.
	 */
	void expandSingleton() {
		if (isSingleton()) {
			State p = new State();
			initial = p;
			for (int i = 0; i < singleton.length(); i++) {
				State q = new State();
				p.transitions.add(new Transition(singleton.charAt(i), q));
				p = q;
			}
			p.accept = true;
			deterministic = true;
			singleton = null;
		}
	}

	/**
	 * Returns the number of states in this automaton.
	 */
	public int getNumberOfStates() {
		if (isSingleton())
			return singleton.length() + 1;
		return getStates().size();
	}

	/**
	 * Returns the number of transitions in this automaton. This number is counted
	 * as the total number of edges, where one edge may be a character interval.
	 */
	public int getNumberOfTransitions() {
		if (isSingleton())
			return singleton.length();
		int c = 0;
		for (State s : getStates())
			c += s.transitions.size();
		return c;
	}

	/**
	 * Returns true if the language of this automaton is equal to the language
	 * of the given automaton. Implemented using <code>hashCode</code> and
	 * <code>subsetOf</code>.
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (!(obj instanceof Automaton))
			return false;
		Automaton a = (Automaton)obj;
		if (isSingleton() && a.isSingleton())
			return singleton.equals(a.singleton);
		return hashCode() == a.hashCode() && subsetOf(a) && a.subsetOf(this);
	}

	/**
	 * Returns hash code for this automaton. The hash code is based on the
	 * number of states and transitions in the minimized automaton.
	 */
	@Override
	public int hashCode() {
		if (hash_code == 0)
			minimize();
		return hash_code;
	}

	/**
	 * Returns a string representation of this automaton.
	 */
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		if (isSingleton()) {
			b.append("singleton: ");
			for (char c : singleton.toCharArray())
				Transition.appendCharString(c, b);
			b.append("\n");
		} else {
			Set<State> states = getStates();
			setStateNumbers(states);
			b.append("initial state: ").append(initial.number).append("\n");
			for (State s : states)
				b.append(s.toString());
		}
		return b.toString();
	}

	/**
	 * Returns <a href="http://www.research.att.com/sw/tools/graphviz/" target="_top">Graphviz Dot</a>
	 * representation of this automaton.
	 */
	public String toDot() {
		StringBuilder b = new StringBuilder("digraph Automaton {\n");
		b.append("  rankdir = LR;\n");
		Set<State> states = getStates();
		setStateNumbers(states);
		for (State s : states) {
			b.append("  ").append(s.number);
			if (s.accept)
				b.append(" [shape=doublecircle,label=\"\"];\n");
			else
				b.append(" [shape=circle,label=\"\"];\n");
			if (s == initial) {
				b.append("  initial [shape=plaintext,label=\"\"];\n");
				b.append("  initial -> ").append(s.number).append("\n");
			}
			for (Transition t : s.transitions) {
				b.append("  ").append(s.number);
				t.appendDot(b);
			}
		}
		return b.append("}\n").toString();
	}

	/**
	 * Returns a clone of this automaton, expands if singleton.
	 */
	Automaton cloneExpanded() {
		Automaton a = clone();
		a.expandSingleton();
		return a;
	}

	/**
	 * Returns a clone of this automaton.
	 */
	@Override
	public Automaton clone() {
		try {
			Automaton a = (Automaton)super.clone();
			if (!isSingleton()) {
				HashMap<State, State> m = new HashMap<State, State>();
				Set<State> states = getStates();
				for (State s : states)
					m.put(s, new State());
				for (State s : states) {
					State p = m.get(s);
					p.setActor(s.getActor());
					p.accept = s.accept;
					if (s == initial)
						a.initial = p;
					p.transitions = new HashSet<Transition>();
					for (Transition t : s.transitions)
						p.transitions.add(new Transition(t.min, t.max, m.get(t.to)));
				}
			}
			return a;
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Retrieves a serialized <code>Automaton</code> located by a URL.
	 * @param url URL of serialized automaton
	 * @exception IOException if input/output related exception occurs
	 * @exception OptionalDataException if the data is not a serialized object
	 * @exception InvalidClassException if the class serial number does not match
	 * @exception ClassCastException if the data is not a serialized <code>Automaton</code>
	 * @exception ClassNotFoundException if the class of the serialized object cannot be found
	 */
	public static Automaton load(URL url) throws IOException, OptionalDataException, ClassCastException,
	                                             ClassNotFoundException, InvalidClassException {
		return load(url.openStream());
	}

	/**
	 * Retrieves a serialized <code>Automaton</code> from a stream.
	 * @param stream input stream with serialized automaton
	 * @exception IOException if input/output related exception occurs
	 * @exception OptionalDataException if the data is not a serialized object
	 * @exception InvalidClassException if the class serial number does not match
	 * @exception ClassCastException if the data is not a serialized <code>Automaton</code>
	 * @exception ClassNotFoundException if the class of the serialized object cannot be found
	 */
	public static Automaton load(InputStream stream) throws IOException, OptionalDataException, ClassCastException,
	                                                        ClassNotFoundException, InvalidClassException {
		ObjectInputStream s = new ObjectInputStream(stream);
		return (Automaton)s.readObject();
	}

	/**
	 * Writes this <code>Automaton</code> to the given stream.
	 * @param stream output stream for serialized automaton
	 * @exception IOException if input/output related exception occurs
	 */
	public void store(OutputStream stream) throws IOException {
		ObjectOutputStream s = new ObjectOutputStream(stream);
		s.writeObject(this);
		s.flush();
	}

	/**
	 * Returns a new (deterministic) automaton with the empty language.
	 */
	public static Automaton makeEmpty()	{
		return BasicAutomata.makeEmpty();
	}

	/**
	 * Returns a new (deterministic) automaton that accepts only the empty string.
	 */
	public static Automaton makeEmptyString() {
		return BasicAutomata.makeEmptyString();
	}

	/**
	 * Returns a new (deterministic) automaton that accepts all strings.
	 */
	public static Automaton makeAnyString()	{
		return BasicAutomata.makeAnyString();
	}

	/**
	 * Returns a new (deterministic) automaton that accepts any single character.
	 */
	public static Automaton makeAnyChar() {
		return BasicAutomata.makeAnyChar();
	}

	/**
	 * Returns a new (deterministic) automaton that accepts a single character of the given value.
	 */
	public static Automaton makeChar(char c) {
		return BasicAutomata.makeChar(c);
	}

	/**
	 * Returns a new (deterministic) automaton that accepts a single char
	 * whose value is in the given interval (including both end points).
	 */
	public static Automaton makeCharRange(char min, char max) {
		return BasicAutomata.makeCharRange(min, max);
	}

	/**
	 * Returns a new (deterministic) automaton that accepts a single character in the given set.
	 */
	public static Automaton makeCharSet(String set) {
		return BasicAutomata.makeCharSet(set);
	}

	/**
	 * Returns a new automaton that accepts strings representing
	 * decimal non-negative integers in the given interval.
	 * @param min minimal value of interval
	 * @param max maximal value of inverval (both end points are included in the interval)
	 * @param digits if >0, use fixed number of digits (strings must be prefixed
	 *               by 0's to obtain the right length) -
	 *               otherwise, the number of digits is not fixed
	 * @exception IllegalArgumentException if min>max or if numbers in the interval cannot be expressed
	 *                                     with the given fixed number of digits
	 */
	public static Automaton makeInterval(int min, int max, int digits) throws IllegalArgumentException {
		return BasicAutomata.makeInterval(min, max, digits);
	}

	/**
	 * Returns a new (deterministic) automaton that accepts the single given string.
	 * <p>
	 * Complexity: constant.
	 */
	public static Automaton makeString(String s) {
		return BasicAutomata.makeString(s);
	}

	/**
	 * Returns a new automaton that accepts the concatenation of the languages of
	 * this and the given automaton.
	 * <p>
	 * Complexity: linear in number of states.
	 */
	public Automaton concatenate(Automaton a) {
		return BasicOperations.concatenate(this, a);
	}

	/**
	 * Returns a new automaton that accepts the concatenation of the languages of
	 * the given automata.
	 * <p>
	 * Complexity: linear in total number of states.
	 */
	static public Automaton concatenate(List<Automaton> l) {
		return BasicOperations.concatenate(l);
	}

	/**
	 * Returns a new automaton that accepts the union of the empty string and the
	 * language of this automaton.
	 * <p>
	 * Complexity: linear in number of states.
	 */
	public Automaton optional() {
		return BasicOperations.optional(this);
	}

	/**
	 * Returns a new automaton that accepts the Kleene star (zero or more
	 * concatenated repetitions) of the language of this automaton.
	 * <p>
	 * Complexity: linear in number of states.
	 */
	public Automaton repeat() {
		return BasicOperations.repeat(this);
	}

	/**
	 * Returns a new automaton that accepts <code>min</code> or more
	 * concatenated repetitions of the language of this automaton.
	 * <p>
	 * Complexity: linear in number of states and in <code>min</code>.
	 */
	public Automaton repeat(int min) {
		return BasicOperations.repeat(this, min);
	}

	/**
	 * Returns a new automaton that accepts between <code>min</code> and
	 * <code>max</code> (including both) concatenated repetitions of the
	 * language of this automaton.
	 * <p>
	 * Complexity: linear in number of states and in <code>min</code> and
	 * <code>max</code>.
	 */
	public Automaton repeat(int min, int max) {
		return BasicOperations.repeat(this, min, max);
	}

	/**
	 * Returns a new (deterministic) automaton that accepts the complement of the
	 * language of this automaton.
	 * <p>
	 * Complexity: linear in number of states (if already deterministic).
	 */
	public Automaton complement() {
		return BasicOperations.complement(this);
	}

	/**
	 * Returns a new (deterministic) automaton that accepts the intersection of
	 * the language of this automaton and the complement of the language of the
	 * given automaton. As a side-effect, this automaton may be determinized, if not
	 * already deterministic.
	 * <p>
	 * Complexity: quadratic in number of states (if already deterministic).
	 */
	public Automaton minus(Automaton a) {
		return BasicOperations.minus(this, a);
	}

	/**
	 * Returns a new (deterministic) automaton that accepts the intersection of
	 * the languages of this and the given automaton. As a side-effect, both
	 * this and the given automaton are determinized, if not already
	 * deterministic.
	 * <p>
	 * Complexity: quadratic in number of states (if already deterministic).
	 */
	public Automaton intersection(Automaton a) {
		return BasicOperations.intersection(this, a);
	}

	/**
	 * Returns true if the language of this automaton is a subset of the
	 * language of the given automaton.
	 */
	public boolean subsetOf(Automaton a) {
		return BasicOperations.subsetOf(this, a);
	}

	/**
	 * Returns a new automaton that accepts the union of the languages of this and the given automaton.
	 * <p>
	 * Complexity: linear in number of states.
	 */
	public Automaton union(Automaton a) {
		return BasicOperations.union(this, a);
	}

	/**
	 * Returns a new automaton that accepts the union of the languages of the given automata.
	 * <p>
	 * Complexity: linear in number of states.
	 */
	static public Automaton union(Collection<Automaton> l) {
		return BasicOperations.union(l);
	}

	/**
	 * Determinizes this automaton.
	 * <p>
	 * Complexity: exponential in number of states.
	 */
	public void determinize() {
		BasicOperations.determinize(this);
	}

	/**
	 * Adds epsilon transitions to this automaton.
	 * This method adds extra character interval transitions that are equivalent to the given
	 * set of epsilon transitions.
	 * @param pairs collection of {@link StatePair} objects representing pairs of source/destination states
	 *        where epsilon transitions should be added
	 */
	public void addEpsilons(Collection<StatePair> pairs) {
		BasicOperations.addEpsilons(this, pairs);
	}

	/**
	 * Returns true if this automaton accepts the empty string and nothing else.
	 */
	public boolean isEmptyString() {
		return BasicOperations.isEmptyString(this);
	}

	/**
	 * Returns true if this automaton accepts no strings.
	 */
	public boolean isEmpty() {
		return BasicOperations.isEmpty(this);
	}

	/**
	 * Returns true if this automaton accepts all strings.
	 */
	public boolean isTotal() {
		return BasicOperations.isTotal(this);
	}

	/**
	 * Returns a shortest accepted/rejected string. If more than one string is
	 * found, the lexicographically first is returned.
	 * @param accepted if true, look for accepted strings; otherwise, look for rejected strings
	 * @return the string, null if none found
	 */
	public String getShortestExample(boolean accepted) {
		return BasicOperations.getShortestExample(this, accepted);
	}

	/**
	 * Returns true if the given string is accepted by this automaton. As a
	 * side-effect, this automaton is determinized if not already deterministic.
	 * <p>
	 * Complexity: linear in length of string (if automaton is already
	 * deterministic) and in number of transitions.
	 * <p>
	 * <b>Note:</b> to obtain maximum speed, use the {@link RunAutomaton} class.
	 */
	public boolean run(String s) {
		return BasicOperations.run(this, s);
	}

	/**
	 * Minimizes (and determinizes if not already deterministic) this automaton.
	 * @see #setMinimization(int)
	 */
	public void minimize() {
		MinimizationOperations.minimize(this);
	}

	/**
	 * Returns a new automaton that accepts the strings that in more than one way can be split into
	 * a left part being accepted by this automaton and a right part being accepted by
	 * the given automaton.
	 */
	public Automaton overlap(Automaton a) {
		return SpecialOperations.overlap(this, a);
	}

	/**
	 * Returns a new automaton that accepts the single chars that occur
	 * in strings that are accepted by this automaton.
	 */
	public Automaton singleChars() {
		return SpecialOperations.singleChars(this);
	}

	/**
	 * Returns a new automaton that accepts the trimmed language of this
	 * automaton. The resulting automaton is constructed as follows: 1) Whenever
	 * a <code>c</code> character is allowed in the original automaton, one or
	 * more <code>set</code> characters are allowed in the new automaton. 2)
	 * The automaton is prefixed and postfixed with any number of
	 * <code>set</code> characters.
	 * @param set set of characters to be trimmed
	 * @param c canonical trim character (assumed to be in <code>set</code>)
	 */
	public Automaton trim(String set, char c) {
		return SpecialOperations.trim(this, set, c);
	}

	/**
	 * Returns a new automaton that accepts the compressed language of this
	 * automaton. Whenever a <code>c</code> character is allowed in the
	 * original automaton, one or more <code>set</code> characters are allowed
	 * in the new automaton.
	 * @param set set of characters to be compressed
	 * @param c canonical compress character (assumed to be in <code>set</code>)
	 */
	public Automaton compress(String set, char c) {
		return SpecialOperations.compress(this, set, c);
	}

	/**
	 * Returns a new automaton where all transition labels have been substituted.
	 * <p>
	 * Each transition labeled <code>c</code> is changed to a set of
	 * transitions, one for each character in <code>map(c)</code>. If
	 * <code>map(c)</code> is null, then the transition is unchanged.
	 * @param map map from characters to sets of characters (where characters
	 *            are <code>Character</code> objects)
	 */
	public Automaton subst(Map<Character,Set<Character>> map) {
		return SpecialOperations.subst(this, map);
	}

	/**
	 * Returns a new automaton where all transitions of the given char are replaced by a string.
	 * @param c char
	 * @param s string
	 * @return new automaton
	 */
	public Automaton subst(char c, String s) {
		return SpecialOperations.subst(this, c, s);
	}

	/**
	 * Returns a new automaton accepting the homomorphic image of this automaton
	 * using the given function.
	 * <p>
	 * This method maps each transition label to a new value.
	 * <code>source</code> and <code>dest</code> are assumed to be arrays of
	 * same length, and <code>source</code> must be sorted in increasing order
	 * and contain no duplicates. <code>source</code> defines the starting
	 * points of char intervals, and the corresponding entries in
	 * <code>dest</code> define the starting points of corresponding new
	 * intervals.
	 */
	public Automaton homomorph(char[] source, char[] dest) {
		return SpecialOperations.homomorph(this, source, dest);
	}

	/**
	 * Returns a new automaton with projected alphabet. The new automaton accepts
	 * all strings that are projections of strings accepted by this automaton
	 * onto the given characters (represented by <code>Character</code>). If
	 * <code>null</code> is in the set, it abbreviates the intervals
	 * u0000-uDFFF and uF900-uFFFF (i.e., the non-private code points). It is
	 * assumed that all other characters from <code>chars</code> are in the
	 * interval uE000-uF8FF.
	 */
	public Automaton projectChars(Set<Character> chars) {
		return SpecialOperations.projectChars(this, chars);
	}

	/**
	 * Returns true if the language of this automaton is finite.
	 */
	public boolean isFinite() {
		return SpecialOperations.isFinite(this);
	}

	/**
	 * Returns the set of accepted strings, assuming this automaton has a finite
	 * language. If the language is not finite, null is returned.
	 */
	public Set<String> getFiniteStrings() {
		return SpecialOperations.getFiniteStrings(this);
	}

	/**
	 * Returns the set of accepted strings, assuming that at most <code>limit</code>
	 * strings are accepted. If more than <code>limit</code> strings are
	 * accepted, null is returned. If <code>limit</code>&lt;0, then this
	 * methods works like {@link #getFiniteStrings()}.
	 */
	public Set<String> getFiniteStrings(int limit) {
		return SpecialOperations.getFiniteStrings(this, limit);
	}

	/**
	 * Returns the longest string that is a prefix of all accepted strings and
	 * visits each state at most once.
	 * @return common prefix
	 */
	public String getCommonPrefix() {
		return SpecialOperations.getCommonPrefix(this);
	}

	/**
	 * Returns a string that is an interleaving of strings that are accepted by
	 * <code>ca</code> but not by <code>a</code>. If no such string
	 * exists, null is returned. As a side-effect, <code>a</code> is determinized,
	 * if not already deterministic. Only interleavings that respect
	 * the suspend/resume markers (two BMP private code points) are considered if the markers are non-null.
	 * Also, interleavings never split surrogate pairs.
	 * <p>
	 * Complexity: proportional to the product of the numbers of states (if <code>a</code>
	 * is already deterministic).
	 */
	public static String shuffleSubsetOf(Collection<Automaton> ca, Automaton a, Character suspend_shuffle, Character resume_shuffle) {
		return ShuffleOperations.shuffleSubsetOf(ca, a, suspend_shuffle, resume_shuffle);
	}

	/**
	 * Returns a new automaton that accepts the shuffle (interleaving) of
	 * the languages of this and the given automaton.
	 * As a side-effect, both automata are determinized, if not already deterministic.
	 * <p>
	 * Complexity: quadratic in number of states (if already deterministic).
	 * <p>
	 * <dl><dt><b>Author:</b></dt><dd>Torben Ruby
	 * &lt;<a href="mailto:ruby@daimi.au.dk">ruby@daimi.au.dk</a>&gt;</dd></dl>
	 */
	public Automaton shuffle(Automaton a) {
		return ShuffleOperations.shuffle(this, a);
	}
}
