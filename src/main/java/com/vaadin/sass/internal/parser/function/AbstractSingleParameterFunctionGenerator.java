/*
 * Copyright 2000-2014 Vaadin Ltd.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.sass.internal.parser.function;

import com.vaadin.sass.internal.parser.LexicalUnitImpl;
import com.vaadin.sass.internal.parser.ParseException;
import com.vaadin.sass.internal.parser.SassList;
import com.vaadin.sass.internal.tree.Node.BuildStringStrategy;

/**
 * AbstractSingleParameterFunctionGenerator is used as a base class for
 * implementing Sass functions that take exactly one single value parameter. A
 * single value parameter is of type LexicalUnitImpl.
 * 
 * @author Vaadin
 * 
 */
public abstract class AbstractSingleParameterFunctionGenerator implements
        SCSSFunctionGenerator {

    private final String functionName;

    public AbstractSingleParameterFunctionGenerator(String functionName) {
        this.functionName = functionName;
    }

    @Override
    public String getFunctionName() {
        return functionName;
    }

    @Override
    public String printState(LexicalUnitImpl function,
            BuildStringStrategy strategy) {
        SassList params = function.getParameterList();
        if (params.size() != 1 || !(params.get(0) instanceof LexicalUnitImpl)) {
            throw new ParseException("Function " + getFunctionName()
                    + " must have exactly one single value parameter", function);
        }
        LexicalUnitImpl firstParam = (LexicalUnitImpl) params.get(0);
        return strategy.build(compute(firstParam));
    }

    /**
     * Compute the value of the function.
     * 
     * This method must not modify firstParam. If necessary, the implementation
     * should copy the parameter before making modifications.
     * 
     * @param firstParam
     * @return
     */
    protected abstract LexicalUnitImpl compute(LexicalUnitImpl firstParam);

}
