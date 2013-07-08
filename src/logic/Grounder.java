package logic;

import com.google.common.base.Preconditions;
import com.google.common.collect.*;
import propnet.util.FilteringCartesianIterator;
import rekkura.logic.algorithm.Terra;
import rekkura.logic.algorithm.Unifier;
import rekkura.logic.model.Atom;
import rekkura.logic.model.Dob;
import rekkura.logic.model.Rule;
import rekkura.logic.structure.Cachet;
import rekkura.logic.structure.Pool;
import rekkura.util.Colut;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * User: david
 * Date: 6/14/13
 * Time: 11:11 AM
 * Description:
 *      For a set of rules, generates all the valid groundings. Makes sure to cache them in the
 *      pool and the cachet.
 *
 *      This really doesn't belong in the propnet dir.
 */
public class Grounder {

    private static boolean debug = false;
    private static long MAX_PROPS = 300000000;

    public static SetMultimap<Dob,Set<Atom>> getValidGroundings(List<Rule> rules, final Pool pool,
                                                                Cachet cachet) {
        SetMultimap<Dob, Set<Atom>> groundings = HashMultimap.create(); // head -> all atoms

        // now process the rules
        for (Rule rule : rules) {
            // check if exceeded limit
            if (groundings.size() > MAX_PROPS) {
                System.out.println("[ERROR] PropNet is too large! Returning null");
                return null;
            }

            if (debug)
                System.out.println("[DEBUG] Processing rule: " + rule);

            // already grounded?
            if (rule.vars.isEmpty()) {
                Dob head = rule.head.dob;
                Preconditions.checkArgument(pool.dobs.cache.containsKey(head.toString()));
                for (Atom body : rule.body) {
                    Preconditions.checkArgument(pool.atoms.cache.containsKey(body.toString()));
                    Preconditions.checkArgument(pool.dobs.cache.containsKey(body.dob.toString()));
                }

                groundings.put(head, Sets.newHashSet(rule.body));
                continue;
            }


            // now ground the rule
            List<Atom> body = rule.body;

            // what should cachet be expected to have??
            ListMultimap<Atom,Dob> bodySpace = Terra.getBodySpace(rule, cachet);
            if (debug)
                System.out.println("[DEBUG] body space: " + bodySpace);
            List<Atom> posBodyTerms = Atom.filterPositives(body);

            // get the positive body space as lists in order they appear in body
            List<List<Dob>> posBodySpace = Lists.newArrayList();
            for (Atom bodyTerm : posBodyTerms)
                posBodySpace.add(bodySpace.get(bodyTerm));

            // unification of positive body
            SetMultimap<Dob,Dob> unitedUnification = unitedUnification(rule, posBodySpace);
            final List<Dob> vars = Lists.newArrayList();
            for (Dob v : rule.vars)
                if (unitedUnification.containsKey(v))
                    vars.add(v);

            List<List<Dob>> unitedAsList = Lists.newArrayList();
            for (Dob var : vars)
                unitedAsList.add(Lists.newArrayList(unitedUnification.get(var)));

            final SetMultimap<Dob,Dob> groundedBy = getGroundedBy(rule, vars);

            final Rule fr = rule;
            FilteringCartesianIterator.FilterFn<Dob> unificationFilter = new FilteringCartesianIterator.FilterFn<Dob>() {
                // IMPORTANT! current must be in the same order as vars.
                @Override
                public boolean pred(List<Dob> current, Dob x) {
                    // no assignment to filter by
                    if (current.isEmpty())
                        return true;
                    // get old variable assignments
                    Map<Dob,Dob> unify = Maps.newHashMap();
                    for (int i=0; i<current.size(); i++)
                        unify.put(vars.get(i), current.get(i));
                    // new assignment
                    Dob lastVar = vars.get(current.size());
                    unify.put(lastVar, x);

                    // verify unification
                    if (!validDistinct(unify, fr, fr.vars))
                        return false;

                    /*if (debug) {
                        System.out.println("[DEBUG] filtering for " + fr);
                        System.out.println("[DEBUG] unify: " + unify);
                    }*/

                    Set<Dob> bodiesToGround = groundedBy.get(lastVar);
                    for (Dob bodyToGround : bodiesToGround) {
                        Dob grounded = Unifier.replace(bodyToGround, unify);
                        if (!(pool.dobs.cache.containsKey(grounded.toString()))) {
                            return false;
                        }
                    }

                    return true;
                }
            };

            Iterable<List<Dob>> filteredUnifications = new FilteringCartesianIterator.FilteringCartesianIterable<Dob>(
                    unitedAsList, unificationFilter);

            int count = 0;
            for (List<Dob> unifyAsList : filteredUnifications) {
                count++;
                // check if exceeded limit
                if (groundings.size() > MAX_PROPS) {
                    System.out.println("[ERROR] PropNet is too large! Returning null");
                    return null;
                }
                if (debug)
                    if (count % 10000 == 0)
                        System.out.println("[DEBUG] Done processing " + count + " unifications");
                Preconditions.checkArgument(unifyAsList.size() == vars.size());
                // create unification
                Map<Dob,Dob> unify = Maps.newHashMap();
                for (int i=0; i<vars.size(); i++)
                    unify.put(vars.get(i), unifyAsList.get(i));

                Rule grounded = Unifier.replace(rule, unify, Sets.<Dob>newHashSet());
                Preconditions.checkArgument(grounded.vars.isEmpty());
                Dob head = getSubmergedGround(grounded.head.dob, pool, cachet);
                List<Atom> submergedBodies = Lists.newArrayList();
                // submerge the atom dobs and make new bodies
                for (Atom b : grounded.body) {
                    Dob bd = getSubmergedGround(b.dob, pool, cachet);
                    Atom newbody = getSubmergedAtom(new Atom(bd, b.truth), pool);
                    Preconditions.checkNotNull(newbody);
                    submergedBodies.add(newbody);
                }
                if (debug) {
                    System.out.println("[DEBUG] adding head: " + head);
                    System.out.println("[DEBUG] \twith bodies: " + submergedBodies);
                }
                groundings.put(head, Sets.newHashSet(submergedBodies));
            }

            if (debug)
                System.out.println();
        }
        return groundings;
    }
    /**
     * Returns the submerged version if there is one. Otherwise, submerges the dob
     * and returns it.
     * @param dob
     * @param pool
     * @param cachet
     * @return
     */
    public static Dob getSubmergedGround(Dob dob, Pool pool, Cachet cachet) {
        cachet.storeGround(dob);
        return pool.dobs.submerge(dob);
    }

    /**
     * Same as getSubmergedGround, but for atoms
     * @param atom
     * @param pool
     * @return
     */
    public static Atom getSubmergedAtom(Atom atom, Pool pool) {
        Preconditions.checkArgument(pool.dobs.cache.containsKey(atom.dob.toString()));
        String key = atom.toString();
        if (pool.atoms.cache.containsKey(key))
            return pool.atoms.cache.get(key);
        return pool.atoms.submerge(atom);
    }

    /**
     * Returns a multimap where the key is a body term in rule.  The corresponding values
     * is are the variables v_j such that if variables v_1...v_j are assigned, the key
     * is grounded. Note that if v_j is a value, v_k for k>j is also in there.
     * @param rule
     * @param vars
     *      variables of rule in order v_1...v_n
     * @return
     */
    public static SetMultimap<Dob,Dob> getGroundedBy(Rule rule, List<Dob> vars) {
        SetMultimap<Dob,Dob> ret = HashMultimap.create();
        Set<Dob> processedVars = Sets.newHashSet();
        Set<Dob> allVars = Sets.newHashSet(vars);
        for (Dob var : vars) {
            processedVars.add(var);
            for (Atom bodyTerm : rule.body) {
                boolean grounded = true;
                for (Dob bodyTermDob : bodyTerm.dob.fullIterable())
                    if (!processedVars.contains(bodyTermDob) && allVars.contains(bodyTermDob)) {
                        grounded = false;
                        break;
                    }
                if (grounded)
                    ret.put(var, bodyTerm.dob);
            }
        }
        return ret;
    }

    // united unifications
    // key: variable, value: grounding
    public static SetMultimap<Dob,Dob> unitedUnification(Rule rule, List<List<Dob>> posBodySpace) {
        SetMultimap<Dob,Dob> ret = HashMultimap.create();
        List<Dob> posBodies = posBodies(rule);
        Preconditions.checkArgument(posBodies.size() == posBodySpace.size());
        for (int i=0; i<posBodies.size(); i++) {
            Dob ungrounded = posBodies.get(i);
            for (Dob ground : posBodySpace.get(i)) {
                Map<Dob,Dob> unification = Unifier.unify(ungrounded, ground);
                if (unification == null)
                    continue;
                for (Dob var : unification.keySet())
                    if (var.name.startsWith("?"))
                        ret.put(var, unification.get(var));
            }
        }
        return ret;
    }

    public static List<Dob> posBodies(Rule rule) {
        List<Dob> ret = Lists.newArrayList();
        for (Atom body : Atom.filterPositives(rule.body))
            ret.add(body.dob);
        return ret;
    }


    // fuck forgot to handle the one variable case
    public static boolean validDistinct(Map<Dob,Dob> unify, Rule rule, Collection<Dob> vars) {
        List<Rule.Distinct> distinct = rule.distinct;
        if (distinct.size() == 0)
            return true;
        for (Rule.Distinct entry : distinct) {
            Dob first = entry.first;
            Dob second = entry.second;

            // two variable case
            if (Colut.contains(vars, first) &&
                unify.containsKey(first) &&
                Colut.contains(vars, second) &&
                unify.containsKey(second)) {
                first = unify.get(first);
                second = unify.get(second);

                if (first == second)
                    return false;
                continue;
            }

            // one variable case
            if (Colut.contains(vars, first) || Colut.contains(vars, second)) {
                Dob var = Colut.contains(vars, first) ? first : second;
                Dob diff = Colut.contains(vars, first) ? second : first;
                if (!unify.containsKey(var))
                    continue;
                Dob val = unify.get(var);
                if (val == diff)
                    return false;
                continue;
            }
        }
        return true;
    }
}
