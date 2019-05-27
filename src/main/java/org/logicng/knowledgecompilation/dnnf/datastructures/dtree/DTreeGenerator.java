package org.logicng.knowledgecompilation.dnnf.datastructures.dtree;

import org.logicng.formulas.Formula;

public interface DTreeGenerator {

    DTree generate(final Formula cnf);
}
