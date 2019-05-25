package org.logicng.dnnf.dtree;

import org.logicng.formulas.Formula;

public interface DTreeGenerator {

    DTree generate(final Formula cnf);
}
