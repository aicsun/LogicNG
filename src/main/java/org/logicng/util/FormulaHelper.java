package org.logicng.util;

import org.logicng.formulas.Formula;
import org.logicng.formulas.Literal;
import org.logicng.formulas.Variable;

import java.util.Arrays;
import java.util.Collection;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * A class which contains utility methods for {@link Formula} objects.
 * @version 1.5.1
 * @since 1.5.1
 */
public class FormulaHelper {

    /**
     * Private empty constructor.  Class only contains static utility methods.
     */
    private FormulaHelper() {
        // Intentionally left empty
    }

    /**
     * Returns all variables occurring in the given formulas.
     * @param formulas formulas
     * @return all variables occurring in the given formulas
     */
    public static SortedSet<Variable> variables(final Formula... formulas) {
        return Arrays.stream(formulas).map(Formula::variables).flatMap(Collection::stream).collect(Collectors.toCollection(TreeSet::new));
    }

    /**
     * Returns all variables occurring in the given formulas.
     * @param formulas formulas
     * @return all variables occurring in the given formulas
     */
    public static SortedSet<Variable> variables(final Collection<? extends Formula> formulas) {
        return formulas.stream().map(Formula::variables).flatMap(Collection::stream).collect(Collectors.toCollection(TreeSet::new));
    }

    /**
     * Returns all literals occurring in the given formulas.
     * @param formulas formulas
     * @return all literals occurring in the given formulas
     */
    public static SortedSet<Literal> literals(final Formula... formulas) {
        return Arrays.stream(formulas).map(Formula::literals).flatMap(Collection::stream).collect(Collectors.toCollection(TreeSet::new));
    }

    /**
     * Returns all literals occurring in the given formulas.
     * @param formulas formulas
     * @return all literals occurring in the given formulas
     */
    public static SortedSet<Literal> literals(final Collection<? extends Formula> formulas) {
        return formulas.stream().map(Formula::literals).flatMap(Collection::stream).collect(Collectors.toCollection(TreeSet::new));
    }
}
