package org.logicng.knowledgecompilation.dnnf.datastructures.dtree;

import org.logicng.formulas.Formula;

import java.util.ArrayList;

public class SimpleDTreeGenerator extends EliminatingOrderDTreeGenerator {

    @Override
    public DTree generate(final Formula cnf) {
        return generateWithEliminatingOrder(cnf, new ArrayList<>(cnf.variables()));
    }
}
