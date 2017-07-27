/*
 * tuProlog - Copyright (C) 2001-2002  aliCE team at deis.unibo.it
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package alice.tuprolog;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;

/**
 * This class represents a variable term.
 * Variables are identified by a name (which must starts with
 * an upper case letter) or the anonymous ('_') name.
 *
 * @see Term
 */
public class Var extends Term {

    private static final long serialVersionUID = 1L;
    final static String ANY = "_";
    // the name identifying the var
    private String name;
    private StringBuilder completeName;     /* Reviewed by Paolo Contessi: String -> StringBuilder */
    private Term link;            /* link is used for unification process */
    private long timestamp;        /* timestamp is used for fix vars order */
    private int id;            /* id of ExecCtx owners of this var util for renaming*/


    /**
     * Creates a variable identified by a name.
     * <p>
     * The name must starts with an upper case letter or the underscore. If an underscore is
     * specified as a name, the variable is anonymous.
     *
     * @param n is the name
     * @throws InvalidTermException if n is not a valid Prolog variable name
     */
    public Var(String n) {
        link = null;
        id = -1; //no execCtx owners
        if (n.equals(ANY)) {
            name = null;
            completeName = new StringBuilder();
        } else if (Character.isUpperCase(n.charAt(0)) ||
                (n.startsWith(ANY))) {
            name = n;
            completeName = new StringBuilder(n);
        } else {
            throw new InvalidTermException("Illegal variable name: " + n);
        }
    }


    /**
     * Creates an anonymous variable
     * <p>
     * This is equivalent to builder a variable with name _
     */
    public Var() {
        name = null;
        completeName = new StringBuilder();
        link = null;
        id = ORIGINAL;
        timestamp = 0;
    }


    /**
     * Creates a internal engine variable.
     *
     * @param n     is the name
     * @param id    is the id of ExecCtx
     * @param alias code to discriminate external vars
     * @param time  is timestamp
     */
    private Var(String n, int id, int alias, long time) {
        name = n;
        completeName = new StringBuilder();
        timestamp = time;
        link = null;
        if (id < 0) id = ORIGINAL;
        rename(id, alias);
    }

    /* Identify kind of renaming */
    final static int ORIGINAL = -1;
    final static int PROGRESSIVE = -2;

    /**
     * Rename variable (assign completeName)
     */
    void rename(int idExecCtx, int count) { /* Reviewed by Paolo Contessi: String -> StringBuilder */
        id = idExecCtx;

        StringBuilder completeName = this.completeName;
        completeName.delete(0, completeName.length());

        if (id > -1) {
            //completeName = name + "_e" + idExecCtx;

            this.completeName = completeName
                    .append(name).append("_e").append(id);
        } else if (id == ORIGINAL) { //completeName = name;
            this.completeName = completeName
                    .append(name);
        } else if (id == PROGRESSIVE) { //completeName = "_"+count;
            this.completeName = completeName
                    .append('_').append(count);
        }
    }


    /**
     * Gets a copy of this variable.
     * <p>
     * if the variable is not present in the list passed as argument,
     * a copy of this variable is returned and added to the list. If instead
     * a variable with the same time identifier is found in the list,
     * then the variable in the list is returned.
     */
    @Override
    Term copy(AbstractMap<Var, Var> vMap, int idExecCtx) {
        Term tt = term();
        if (tt == this) {
            //No occurence of v before
            return vMap.computeIfAbsent(this, k -> new Var(name, idExecCtx, 0, timestamp));
        } else {
            return tt.copy(vMap, idExecCtx);
        }
    }


    /**
     * Gets a copy of this variable.
     */
    @Override
    Term copy(AbstractMap<Var, Var> vMap, AbstractMap<Term, Var> substMap) {
        Var v;
        Object temp = vMap.get(this);
        if (temp == null) {
            v = new Var(null, Var.PROGRESSIVE, vMap.size(), timestamp);//name,Var.PROGRESSIVE,vMap.size(),timestamp);
            vMap.put(this, v);
        } else {
            v = (Var) temp;
        }
        Term t = term();
        if (t instanceof Var) {
            Object tt = substMap.get(t);
            if (tt == null) {
                substMap.put(t, v);
                v.link = null;
            } else {
                v.link = (tt != v) ? (Var) tt : null;
            }
        }
        if (t instanceof Struct) {
            v.link = t.copy(vMap, substMap);
        }
        if (t instanceof Number) v.link = t;
        return v;
    }


    /**
     * De-unify the variable
     */
    @Override
    public void free() {
        link = null;
    }


    /**
     * De-unify the variables of list
     */
    public static void free(List<Var> varsUnified) {
        varsUnified.forEach(Term::free);
        //varsUnified.clear();
    }


    /**
     * Gets the name of the variable
     */
    public String getName() {
        return name != null ? completeName.toString() : ANY;
    }

    /**
     * Gets the name of the variable
     */
    public String getOriginalName() {
        return name != null ? name : ANY + hashCode();
    }


    /**
     * Gets the term which is referred by the variable.
     * <p>
     * For unbound variable it is the variable itself, while
     * for bound variable it is the bound term.
     */
    @Override
    public Term term() {
        Term tt = this;
        Term t = link;
        while (t != null) {
            tt = t;
            if (t instanceof Var) {
                t = ((Var) t).link;
            } else {
                break;
            }
        }
        return tt;
    }


    /**
     * Gets the term which is direct referred by the variable.
     */
    public Term getLink() {
        return link;
    }

    /**
     * Set the term which is direct bound
     */
    void setLink(Term l) {
        link = l;
    }

    /**
     * Set the timestamp
     */
    void setTimestamp(long t) {
        timestamp = t;
    }

    //

    @Override
    public boolean isNumber() {
        return false;
    }

    @Override
    public boolean isStruct() {
        return false;
    }

    @Override
    public boolean isVar() {
        return true;
    }

    @Override
    public boolean isEmptyList() {
        Term t = term();
        return t != this && t.isEmptyList();
    }

    @Override
    public boolean isAtomic() {
        Term t = term();
        return t != this && t.isAtomic();
    }

    @Override
    public boolean isCompound() {
        Term t = term();
        return t != this && t.isCompound();
    }

    @Override
    public boolean isAtom() {
        Term t = term();
        return t != this && t.isAtom();
    }

    @Override
    public boolean isList() {
        Term t = term();
        return t != this && t.isList();
    }

    @Override
    public boolean isGround() {
        Term t = term();
        return t != this && t.isGround();
    }

    //

    /**
     * Tests if this variable is ANY
     */
    public boolean isAnonymous() {
        return name == null;
    }

    /**
     * Tests if this variable is bound
     */
    public boolean isBound() {
        return link != null;
    }


    /**
     * finds var occurence in a Struct, doing occur-check.
     * (era una findIn)
     *
     * @param vl TODO
     */
    private boolean occurCheck(List<Var> vl, Struct t) {
        int arity = t.getArity();
        for (int c = 0; c < arity; c++) {
            Term at = t.getTerm(c);
            if (at instanceof Struct) {
                if (occurCheck(vl, (Struct) at)) {
                    return true;
                }
            } else if (at instanceof Var) {
                Var v = (Var) at;
                if (v.link == null) {
                    vl.add(v);
                }
                if (this == v) {
                    return true;
                }
            }
        }
        return false;

    }

    //

    /**
     * Resolve the occurence of variables in a Term
     */
    @Override
    long resolveTerm(long count) {
        Term tt = term();
        if (tt != this) {
            return tt.resolveTerm(count);
        } else {
            timestamp = count;
            return count++;
        }
    }


    //

    /**
     * var unification.
     * <p>
     * First, verify the Term eventually already unified with the same Var
     * if the Term exist, unify var with that term, in order to handle situation
     * as (A = p(X) , A = p(1)) which must produce X/1.
     * <p>
     * If instead the var is not already unified, then:
     * <p>
     * if the Term is a var bound to X, then try unification with X
     * so for example if A=1, B=A then B is unified to 1 and not to A
     * (note that it's coherent with chronological backtracking:
     * the eventually backtracked A unification is always after
     * backtracking of B unification.
     * <p>
     * if are the same Var, unification must succeed, but without any new
     * bindings (to avoid cycles for extends in A = B, B = A)
     * <p>
     * if the term is a number, then it's a success and new link is created
     * (retractable by means of a code)
     * <p>
     * if the term is a compound, then occur check test is executed:
     * the var must not appear in the compound ( avoid X=p(X),
     * or p(X,X)=p(Y,f(Y)) ); if occur check is ok
     * then it's success and a new link is created (retractable by a code)
     */
    @Override
    boolean unify(List<Var> vl1, List<Var> vl2, Term t) {
        Term tt = term();
        if (tt == this) {
            t = t.term();
            if (t instanceof Var) {
                if (this == t) {
                    //try{
                    vl1.add(this);
                    //} catch(NullPointerException e) {
                    ///* vl1==null mean nothing intresting for the caller */
                    //throw new RuntimeException(e); //TODO trap this make sure stack trace not generated routinely
                    //}
                    return true;
                }
            } else if (t instanceof Struct) {
                // occur-check
                if (occurCheck(vl2, (Struct) t)) {
                    return false;
                }
            } else if (!(t instanceof Number) && !(t instanceof AbstractSocket)) {
                return false;
            }
            link = t;
            //try {
            vl1.add(this);
            //} catch(NullPointerException e) {
            ///* vl1==null mean nothing intresting for the caller */
            //throw new RuntimeException(e); //TODO trap this make sure stack trace not generated routinely
            //}
            //System.out.println("VAR "+name+" BOUND to "+link+" - time: "+time+" - mark: "+mark);
            return true;
        } else {
            return (tt.unify(vl1, vl2, t));
        }
    }


    /**
     * Gets a copy of this variable
     */
     /*    public Term copy(int idExecCtx) {
     Term tt = getTerm();
     if(tt == this) {
     if(idExecCtx > 0 && id > 0) thisCopy++;
     return new Var(name,idExecCtx,thisCopy,antialias,timestamp);
     } else {
     return (tt.copy(idExecCtx));
     }
     }
	  */
    @Override
    public boolean isGreater(Term t) {
        Term tt = term();
        if (tt == this) {
            t = t.term();
            return t instanceof Var && timestamp > ((Var) t).timestamp;
        } else {
            return tt.isGreater(t);
        }
    }

    @Override
    public boolean isGreaterRelink(Term t, ArrayList<String> vorder) {
        Term tt = term();
        if (tt == this) {
            t = t.term();
            if (!(t instanceof Var)) return false;
            //System.out.println("Compare di tt "+tt+" con t "+t);
            //System.out.println("vorder "+vorder);
            //System.out.println("indice tt "+vorder.indexOf(((Var)tt).getName())+" indice t "+vorder.indexOf(((Var)t).getName()));
            //return timestamp > ((Var)t).timestamp;
            return vorder.indexOf(((Var) tt).getName()) > vorder.indexOf(((Var) t).getName());
        } else {
            return tt.isGreaterRelink(t, vorder);
        }
    }

    @Override
    public boolean isEqual(Term t) {
        Term tt = term();
        if (tt == this) {
            t = t.term();
            return (t instanceof Var && timestamp == ((Var) t).timestamp);
        } else {
            return tt.isEqual(t);
        }
    }

    public void setName(String s) {
        this.name = s;
    }

    /**
     * Gets the string representation of this variable.
     * <p>
     * For bounded variables, the string is <Var Name>/<bound Term>.
     */
    @Override
    public String toString() {
        Term tt = term();
        if (name != null) {
            return tt == this ? completeName.toString() : completeName + " / " + tt;
        } else {
            return tt == this ? ANY + hashCode() : tt.toString();
        }
    }


    /**
     * Gets the string representation of this variable, providing
     * the string representation of the linked term in the case of
     * bound variable
     */
    public String toStringFlattened() {
        Term tt = term();
        if (name != null) {
            return tt == this ? completeName.toString() : tt.toString();
        } else {
            return tt == this ? ANY + hashCode() : tt.toString();
        }
    }

    /*Castagna 06/2011*/
    @Override
    public final void accept(TermVisitor tv) {
        tv.visit(this);
    }
	 /**/

}